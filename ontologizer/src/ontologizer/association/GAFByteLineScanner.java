package ontologizer.association;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import ontologizer.go.IParserInput;
import ontologizer.go.PrefixPool;
import ontologizer.go.Term;
import ontologizer.go.TermID;
import ontologizer.go.TermMap;
import ontologizer.linescanner.AbstractByteLineScanner;
import ontologizer.types.ByteString;

/**
 * A GAF Line scanner.
 *
 * @author Sebastian Bauer
 */
class GAFByteLineScanner extends AbstractByteLineScanner
{
	private static Logger logger = Logger.getLogger(GAFByteLineScanner.class.getName());

	/** The wrapped input */
	private IParserInput input;

	/** Contains all items whose associations should gathered or null if all should be gathered */
	private Set<ByteString> names;

	/** All known terms */
	private TermMap terms;

	/** Set of evidences that shall be considered or null if all should be considered */
	private Set<ByteString> evidences;

	/** Monitor progress */
	private IAssociationParserProgress progress;

	private int lineno = 0;
	private long millis = 0;
	public int good = 0;
	public int bad = 0;
	public int skipped = 0;
	public int nots = 0;
	public int evidenceMismatch = 0;
	public int kept = 0;
	public int obsolete = 0;
	private int symbolWarnings = 0;
	private int dbObjectWarnings = 0;

	/** Mapping from gene (or gene product) names to Association objects */
	private ArrayList<Association> associations = new ArrayList<Association>();

	/** Our prefix pool */
	private PrefixPool prefixPool = new PrefixPool();

	/** Items as identified by the object symbol to the list of associations */
	private HashMap<ByteString, ArrayList<Association>> gene2Associations = new HashMap<ByteString, ArrayList<Association>>();

	/** key: synonym, value: main gene name (dbObject_Symbol) */
	private HashMap<ByteString, ByteString> synonym2gene = new HashMap<ByteString,ByteString>();

	/** key: dbObjectID, value: main gene name (dbObject_Symbol) */
	private HashMap<ByteString, ByteString> dbObjectID2gene = new HashMap<ByteString,ByteString>();

	private HashMap<ByteString,ByteString> dbObject2ObjectSymbol = new HashMap<ByteString,ByteString>();
	private HashMap<ByteString,ByteString> objectSymbol2dbObject = new HashMap<ByteString,ByteString>();
	private HashMap<TermID, Term> altTermID2Term = null;
	private HashSet<TermID> usedGoTerms = new HashSet<TermID>();

	public GAFByteLineScanner(IParserInput input, byte [] head, Set<ByteString> names, TermMap terms, Set<ByteString> evidences, IAssociationParserProgress progress)
	{
		super(input.inputStream());

		push(head);

		this.input = input;
		this.names = names;
		this.terms = terms;
		this.evidences = evidences;
		this.progress = progress;
	}

	@Override
	public boolean newLine(byte[] buf, int start, int len)
	{
		/* Progress stuff */
		if (progress != null)
		{
			long newMillis = System.currentTimeMillis();
			if (newMillis - millis > 250)
			{
				progress.update(input.getPosition());
				millis = newMillis;
			}
		}

		lineno++;

		/* Ignore comments */
		if (len < 1 || buf[start]=='!')
			return true;

		Association assoc = Association.createFromGAFLine(buf,start,len,prefixPool);

		try
		{
			TermID currentTermID = assoc.getTermID();

			Term currentTerm;

			good++;

			if (assoc.hasNotQualifier())
			{
				skipped++;
				nots++;
				return true;
			}

			if (evidences != null)
			{
				/*
				 * Skip if evidence of the annotation was not supplied as
				 * argument
				 */
				if (!evidences.contains(assoc.getEvidence()))
				{
					skipped++;
					evidenceMismatch++;
					return true;
				}
			}

			currentTerm = terms.get(currentTermID);
			if (currentTerm == null)
			{
				if (altTermID2Term == null)
				{
					/* Create the alternative ID to Term map */
					altTermID2Term = new HashMap<TermID, Term>();

					for (Term t : terms)
						for (TermID altID : t.getAlternatives())
							altTermID2Term.put(altID, t);
				}

				/* Try to find the term among the alternative terms before giving up. */
				currentTerm = altTermID2Term.get(currentTermID);
				if (currentTerm == null)
				{
					System.err.println("Skipping association of item \"" + assoc.getObjectSymbol() + "\" to " + currentTermID + " because the term was not found!");
					System.err.println("(Are the obo file and the association " + "file both up-to-date?)");
					skipped++;
					return true;
				} else
				{
					/* Okay, found, so set the new attributes */
					currentTermID = currentTerm.getID();
					assoc.setTermID(currentTermID);
				}
			} else
			{
				/* Reset the term id so a unique id is used */
				currentTermID = currentTerm.getID();
				assoc.setTermID(currentTermID);
			}

			usedGoTerms.add(currentTermID);

			if (currentTerm.isObsolete())
			{
				System.err.println("Skipping association of item \"" + assoc.getObjectSymbol() + "\" to " + currentTermID + " because term is obsolete!");
				System.err.println("(Are the obo file and the association file in sync?)");
				skipped++;
				obsolete++;
				return true;
			}

			ByteString[] synonyms;

			/* populate synonym string field */
			if (assoc.getSynonym() != null && assoc.getSynonym().length() > 2)
			{
				/* Note that there can be multiple synonyms, separated by a pipe */
				synonyms = assoc.getSynonym().splitBySingleChar('|');
			} else
				synonyms = null;

			if (names != null)
			{
				/* We are only interested in associations to given genes */
				boolean keep = false;

				/* Check if synonyms are contained */
				if (synonyms != null)
				{
					for (int i = 0; i < synonyms.length; i++)
					{
						if (names.contains(synonyms[i]))
						{
							keep = true;
							break;
						}
					}
				}

				if (keep || names.contains(assoc.getObjectSymbol()) || names.contains(assoc.getDB_Object()))
				{
					kept++;
				} else
				{
					skipped++;
					return true;
				}
			} else
			{
				kept++;
			}

			if (synonyms != null)
			{
				for (int i = 0; i < synonyms.length; i++)
					synonym2gene.put(synonyms[i], assoc.getObjectSymbol());
			}

			{
				/* Check if db object id and object symbol are really bijective */
				ByteString dbObject = objectSymbol2dbObject.get(assoc.getObjectSymbol());
				if (dbObject == null) objectSymbol2dbObject.put(assoc.getObjectSymbol(),assoc.getDB_Object());
				else
				{
					if (!dbObject.equals(assoc.getDB_Object()))
					{
						symbolWarnings++;
						if (symbolWarnings < 1000)
						{
							logger.warning("Line " + lineno + ": Expected that symbol \"" + assoc.getObjectSymbol() + "\" maps to \"" + dbObject + "\" but it maps to \"" + assoc.getDB_Object() + "\"");
						}
					}

				}

				ByteString objectSymbol = dbObject2ObjectSymbol.get(assoc.getDB_Object());
				if (objectSymbol == null) dbObject2ObjectSymbol.put(assoc.getDB_Object(),assoc.getObjectSymbol());
				else
				{
					if (!objectSymbol.equals(assoc.getObjectSymbol()))
					{
						dbObjectWarnings++;
						if (dbObjectWarnings < 1000)
						{
							logger.warning("Line " + lineno + ": Expected that dbObject \"" + assoc.getDB_Object() + "\" maps to symbol \"" + objectSymbol + "\" but it maps to \"" + assoc.getObjectSymbol() + "\"");
						}
					}

				}

			}

			/* Add the Association to ArrayList */
			associations.add(assoc);

			ArrayList<Association> gassociations = gene2Associations.get(assoc.getObjectSymbol());
			if (gassociations == null)
			{
				gassociations = new ArrayList<Association>();
				gene2Associations.put(assoc.getObjectSymbol(),gassociations);
			}
			gassociations.add(assoc);

			/* dbObject2Gene has a mapping from dbObjects to gene names */
			dbObjectID2gene.put(assoc.getDB_Object(), assoc.getObjectSymbol());
		} catch (Exception ex) {
			ex.printStackTrace();
			bad++;
			System.err.println("Nonfatal error: "
					+ "malformed line in association file \n"
					+ /* associationFile + */"\nCould not parse line "
					+ lineno + "\n" + ex.getMessage() + "\n\"" + buf
					+ "\"\n");
		}


		return true;
	}

	/**
	 * @return the number of terms used by the import.
	 */
	public int getNumberOfUsedTerms()
	{
		return usedGoTerms.size();
	}

	public ArrayList<Association> getAssociations()
	{
		return associations;
	}

	public HashMap<ByteString, ByteString> getSynonym2Gene()
	{
		return synonym2gene;
	}

	public HashMap<ByteString, ByteString> getDbObjectID2Gene()
	{
		return dbObjectID2gene;
	}
};

