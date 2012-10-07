package ontologizer.association;

import java.io.*;
import java.nio.channels.FileChannel;
import java.util.*;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import ontologizer.go.PrefixPool;
import ontologizer.go.Term;
import ontologizer.go.TermContainer;
import ontologizer.go.TermID;
import ontologizer.types.ByteString;

/**
 * This class is responsible for parsing GO association files. One object is
 * made for each entry; since genes can have 0,1, >1 synonyms, we also parse the
 * synonym field and create a mapping from each synonym to the gene-association
 * object. Therefore, if the user enters the synonym of a gene, we may still be
 * able to identify it.
 * 
 * @author Peter Robinson, Sebastian Bauer
 * @see Association
 * @see <A HREF="http://www.geneontology.org">www.geneontology.org</A>
 */

public class AssociationParser
{
	private static Logger logger = Logger.getLogger(AssociationParser.class.getCanonicalName());

	enum Type
	{
		UNKNOWN,
		GAF,
		IDS,
		AFFYMETRIX
	};

	/** Mapping from gene (or gene product) names to Association objects */
	private ArrayList<Association> associations;

	/** key: synonym, value: main gene name (dbObject_Symbol) */
	private HashMap<ByteString, ByteString> synonym2gene;

	/** key: dbObjectID, value: main gene name (dbObject_Symbol) */
	private HashMap<ByteString, ByteString> dbObjectID2gene;

	/** Our prefix pool */
	private PrefixPool prefixPool = new PrefixPool();
	
	/** The file type of the association file which was parsed */ 
	private Type fileType = Type.UNKNOWN;

	/** Predefined three slashes string */
	private static ByteString THREE_SLASHES = new ByteString("///");

	/** Counts the symbol warnings */
	private int symbolWarnings;
	
	/** Counts the dbObject warnings */
	private int dbObjectWarnings;
		
	/**
	 * Construct the association parser object. The given file name will
	 * parsed. Convenience constructor when not using progress monitor.
	 * 
	 * @param filename
	 * @param terms
	 * @throws IOException 
	 */
	public AssociationParser(String filename, TermContainer terms) throws IOException
	{
		this(filename,terms,null);
	}

	
	/**
	 * Construct the association parser object. The given file name will
	 * parsed. Convenience constructor when not using progress monitor.
	 * 
	 * @param filename
	 * @param terms
	 * @param names
	 * @throws IOException 
	 */
	public AssociationParser(String filename, TermContainer terms, HashSet<ByteString> names) throws IOException
	{
		this(filename,terms,names,null);
	}

	/**
	 * Construct the association parser object. The given file name will
	 * parsed.
	 * 
	 * @param filename specifies the association file which contains
	 *                 the association of genes to GO terms.
	 * @param terms the container of the GO terms
	 * @param names list of genes from which the associations should be gathered.
	 *        If null all associations are taken,
	 * @param progress
	 * @throws IOException 
	 */
	public AssociationParser(String filename, TermContainer terms, HashSet<ByteString> names, IAssociationParserProgress progress) throws IOException
	{
		this(filename,terms,names,null,progress);
	}

	
	/**
	 * Construct the association parser object. The given file name will
	 * parsed.
	 * 
	 * @param filename specifies the association file which contains
	 *                 the association of genes to GO terms.
	 * @param terms the container of the GO terms
	 * @param names list of genes from which the associations should be gathered.
	 *        If null all associations are taken,
	 * @param evidence keep only the annotation whose evidence match the given ones. If null, all annotations are used.
	 *        Note that this field is currently used when the filenames referes to a GAF file.
	 * @param progress
	 * @throws IOException 
	 */
	public AssociationParser(String filename, TermContainer terms, HashSet<ByteString> names, Collection<String> evidences, IAssociationParserProgress progress) throws IOException
	{
		associations = new ArrayList<Association>();
		synonym2gene = new HashMap<ByteString, ByteString>();
		dbObjectID2gene = new HashMap<ByteString, ByteString>();

		if (filename.endsWith(".ids"))
		{
			importIDSAssociation(filename,terms,progress);
			fileType = Type.IDS;
		} else
		{
			/* We support compressed association files */
			FileInputStream fis = new FileInputStream(filename);
			InputStream is;
			try
			{
				is = new GZIPInputStream(fis);
			} catch (IOException exp)
			{
				fis = new FileInputStream(filename);
				is = fis;
			}
			BufferedInputStream bis = new BufferedInputStream(is);
			bis.mark(4096);

			BufferedReader in = new BufferedReader(new InputStreamReader(bis));

			/* Skip affy comments */
			String line;
			while ((line = in.readLine()) != null)
				if (!line.startsWith("#")) break;
			
			if (line != null)
			{
				bis.reset();

				if (line.startsWith("\"Probe Set ID\",\"GeneChip Array\""))
				{
					importAffyFile(new BufferedReader(new InputStreamReader(bis)),fis,names,terms,progress);
					fileType = Type.AFFYMETRIX;
				} else
				{
					importAssociationFile(bis,fis,names,terms,evidences,progress);
					fileType = Type.GAF;
				}
			}
		}
	}

	/**
	 * Import the annotation from a file generated by GOStat.
	 * 
	 * @param filename
	 */
	private void importIDSAssociation(String filename, TermContainer terms, IAssociationParserProgress progress)
	{
		try
		{
			BufferedReader is = new BufferedReader(new FileReader(filename));
			String line;

			while ((line = is.readLine()) != null)
			{
				if (line.equalsIgnoreCase("GoStat IDs Format Version 1.0"))
					continue;

				String [] fields = line.split("\t",2);
				
				if (fields.length != 2) continue;

				String [] annotatedTerms = fields[1].split(",");

				for (int i = 0; i <annotatedTerms.length; i++)
				{

					TermID tid;

					try
					{
						tid = new TermID(annotatedTerms[i]);
					} catch (IllegalArgumentException ex)
					{
						int id = new Integer(annotatedTerms[i]);
						tid = new TermID(TermID.DEFAULT_PREFIX,id);
					}
					
					if (terms.get(tid) != null)
					{
						Association assoc = new Association(new ByteString(fields[0]),tid.toString());
						associations.add(assoc);
					} else
					{
						logger.warning(tid.toString() + " which annotates " + fields[0] + " not found");
					}
				}
			}
		} catch (FileNotFoundException e)
		{
		} catch (IOException e)
		{
		}
	}

	/**
	 * Do the actual parsing work.
	 * 
	 * @param names
	 * @param terms
	 * @param evidences specifies which annotations to take.
	 * @throws IOException 
	 */
	@SuppressWarnings("unused")
	private void importAssociationFile(BufferedInputStream is, FileInputStream fis, final HashSet<ByteString> names, final TermContainer terms, Collection<String> evidences, final IAssociationParserProgress progress) throws IOException
	{
		final HashSet<ByteString> myEvidences; /* Evidences converted to ByteString */
		if (evidences != null)
		{
			myEvidences = new HashSet<ByteString>();
			for (String e : evidences)
				myEvidences.add(new ByteString(e));
		} else
			myEvidences = null;

		final HashSet<TermID> usedGoTerms = new HashSet<TermID>();

		/* Items as identified by the object symbol to the list of associations */
		final HashMap<ByteString, ArrayList<Association>> gene2Associations = new HashMap<ByteString, ArrayList<Association>>();
		
		final HashMap<ByteString,ByteString> dbObject2ObjectSymbol = new HashMap<ByteString,ByteString>();
		final HashMap<ByteString,ByteString> objectSymbol2dbObject = new HashMap<ByteString,ByteString>();

		final FileChannel fc = fis.getChannel();

		if (progress != null)
			progress.init((int) fc.size());

		class GAFByteLineScanner extends AbstractByteLineScanner
		{
			int lineno = 0;
			long millis = 0;
			int good = 0;
			int bad = 0;
			int skipped = 0;
			int nots = 0;
			int evidenceMismatch = 0;
			int kept = 0;
			int obsolete = 0;

			HashMap<TermID, Term> altTermID2Term = null;

			public GAFByteLineScanner(InputStream is)
			{
				super(is);
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
						try {
							progress.update((int) fc.position());
						} catch (IOException e) { }
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
	
					if (myEvidences != null)
					{
						/*
						 * Skip if evidence of the annotation was not supplied as
						 * argument
						 */
						if (!myEvidences.contains(assoc.getEvidence()))
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
	
						/* Check if synoyms are contained */
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
					bad++;
					System.err.println("Nonfatal error: "
							+ "malformed line in association file \n"
							+ /* associationFile + */"\nCould not parse line "
							+ lineno + "\n" + ex.getMessage() + "\n\"" + buf
							+ "\"\n");
				}


				return true;
			}
		};

		GAFByteLineScanner ls = new GAFByteLineScanner(is);
		ls.scan();
		
		if (progress != null)
			progress.update((int) fc.size());

		is.close();

		logger.info(ls.good + " associations parsed, " + ls.kept
				+ " of which were kept while " + ls.bad
				+ " malformed lines had to be ignored.");
		logger.info("A further " + ls.skipped
				+ " associations were skipped due to various reasons whereas "
				+ ls.nots + " of those where explicitly qualified with NOT, " +
				+ ls.obsolete + " referred to obsolete terms and "
				+ ls.evidenceMismatch + " didn't"
				+ " match the requested evidence codes");
		logger.info("A total of " + usedGoTerms.size()
				+ " terms are directly associated to " + dbObjectID2gene.size()
				+ " items.");
		
		if (symbolWarnings >= 1000)
			logger.warning("The symbols of a total of " + symbolWarnings + " entries mapped ambiguously");
		if (dbObjectWarnings >= 1000)
			logger.warning("The objects of a  total of " + dbObjectWarnings + " entries mapped ambiguously");

		/*
		 * Code is disabled for now. The problem with approach above is that if
		 * a synonym of a gene is entered that also stand for a object symbol
		 * then both genes are not filtered.
		 */
		if (false && names != null) {
			for (ByteString name : names) {
				ByteString objectSymbol = synonym2gene.get(name);

				if (objectSymbol != null && !objectSymbol.equals(name)
						&& !names.contains(objectSymbol)) {
					/*
					 * Now check whether there is any object symbol with the
					 * same name. If so, we remove the evidence of that gene.
					 */
					if (gene2Associations.containsKey(name)) {
						/*
						 * Here, we know that we have names referring to
						 * different objects. As we give precedence over object
						 * symbols, we remove the evidence for the other one.
						 */
						System.out.println("nn " + name);
						ArrayList<Association> gassociations = gene2Associations
								.get(name);
						for (Association a : gassociations)
							associations.remove(a);
					}
				}
			}
		}
	}

	/**
	 *
	 * 
	 * @param names
	 * @param terms
	 * @param progress
	 * @throws IOException 
	 */
	private void importAffyFile(BufferedReader in, FileInputStream fis, HashSet<ByteString> names, TermContainer terms, IAssociationParserProgress progress) throws IOException
	{
		/* This represents the affymetrix annotation format as of
		 * May 15th, 2006. The code uses the following to check that the
		 * headers have stayed the same. If anything has changed, then it
		 * is worthwhile checking the code again to make sure the code is
		 * doing what it thinks it is doing. Therefore, throw an error
		 * if something is amiss.
		 */
		String [] annot =
		{
			"Probe Set ID",   /* 0 */
			"GeneChip Array",
			"Species Scientific Name",
			"Annotation Date",
			"Sequence Type",
			"Sequence Source",
			"Transcript ID(Array Design)",
			"Target Description",
			"Representative Public ID",
			"Archival UniGene Cluster",
			"UniGene ID",      /* 10 */
			"Genome Version",
			"Alignments",
			"Gene Title",
			"Gene Symbol",
			"Chromosomal Location",
			"Unigene Cluster Type",
			"Ensembl",
			"Entrez Gene",
			"SwissProt", /* 19 */
			"EC",  /* 20 */
			"OMIM",
			"RefSeq Protein ID",
			"RefSeq Transcript ID",
			"FlyBase",
			"AGI",
			"WormBase",
			"MGI Name",
			"RGD Name",
			"SGD accession number",
			"Gene Ontology Biological Process", /* 30 */
			"Gene Ontology Cellular Component", /* 31 */
			"Gene Ontology Molecular Function", /* 32 */
			"Pathway",
			"Protein Families",
			"Protein Domains",
			"InterPro",
			"Trans Membrane",
			"QTL",
			"Annotation Description",
			"Annotation Transcript Cluster",
			"Transcript Assignments",
			"Annotation Notes",
		};

		FileChannel fc = fis.getChannel();

		if (progress != null)
			progress.init((int)fc.size());

		int skipped = 0;
		long millis = 0;
		
		String line;
		
		/* Skip comments */
		do
		{
			line = in.readLine();
		} while (line.startsWith("#"));

		/* Check header */
		boolean headerFailure = false;
		String fields[];
		String delim = ",";
		fields = line.split(delim);
		for (int i=0;i<33/*fields.length*/;i++){ // we don't need to read all columns
			String item = fields[i];
			int x,y; // first and last index of quotation mark
			x = item.indexOf('"')+1;
			y = item.lastIndexOf('"');
			if (x == 0 && y == (item.length() - 1)) System.out.print("OK");
			item = item.substring(x,y);

			if (!item.equals(annot[i]))
			{
				logger.severe("Found column header \"" + item + "\" but expected \"" + annot[i] + "\"");
				headerFailure = true;
				break;
			}
		}

		if (!headerFailure)
		{
			SwissProtAffyAnnotaionSet annotationSet = new SwissProtAffyAnnotaionSet();

			/* Header is fine */
			while ((line = in.readLine()) != null)
			{
				/* Progress stuff */
				if (progress != null)
				{
					long newMillis = System.currentTimeMillis();
					if (newMillis - millis > 250)
					{
						progress.update((int)fc.position());
						millis = newMillis;
					}
				}

				/* Evaluate the current line, store results within the following
				 * variables */
				ByteString probeid = null,swiss = null;
				LinkedList<TermID> termList = new LinkedList<TermID>();

				int len = line.length();
				int x,y;
				int idx;
				x = -1;
				idx = 0;

				for (int i=0;i<len;++i)
				{
					if (line.charAt(i) == '\"')
					{
						if (x==-1) x = i;
						else
						{
							y = i;

							if (y > x)
							{
								if (idx == 0)
								{
									probeid = new ByteString(line.substring(x+1,y));
								} else
								{
									if (idx == 14) /* gene symbol */
									{
										String s = line.substring(x+1,y);
										if (s.startsWith("---"))
											swiss = null;
										else
										{
											swiss = new ByteString(s);
											int sepIndex = swiss.indexOf(THREE_SLASHES);
											if (sepIndex != -1)
												swiss = swiss.trimmedSubstring(0,sepIndex);
										}
									} else
									if (idx == 30 || idx == 31 || idx == 32) /* GO */
									{
										String [] ids = line.substring(x+1,y).split("///");
										if (ids != null)
										{
											int j;
											for (j=0;j<ids.length;j++)
											{
												String number;
												if (ids[j].contains("/"))
												{
													number = ids[j].substring(0,ids[j].indexOf('/')).trim();
												} else number = ids[j].trim();

												try
												{
													int goId = Integer.parseInt(number);
													TermID id = new TermID(goId);

													if (terms.get(id) != null)
														termList.add(id);
													else skipped++;
												} catch (NumberFormatException ex)
												{}
											}
										}
									}
								}

								idx++;
								x = -1;
							}
						}
					
					}
				}

				/* Add the annotation to our annotation set */
				if (swiss != null && swiss.length() > 0)
				{
					annotationSet.add(swiss,probeid,termList);
				}
				else
				{
					if (termList.size() > 0)
					{
						annotationSet.add(probeid,probeid,termList);
					}
				}

			} /* while (line != null) */

			for (SwissProtAffyAnnotation swissAnno : annotationSet)
			{
				ByteString swissID = swissAnno.getSwissProtID();
				for (TermID goID : swissAnno.getGOIDs())
				{
					Association assoc = new Association(swissID,goID);
					associations.add(assoc);
				}

				for (ByteString affy : swissAnno.getAffyIDs())
				{
					synonym2gene.put(affy,swissID);
				}
			}
		}

		System.err.println("Skipped " + skipped + " annotations");
	}

	public ArrayList<Association> getAssociations()
	{
		return associations;
	}

	public HashMap<ByteString, ByteString> getSynonym2gene()
	{
		return synonym2gene;
	}

	public HashMap<ByteString, ByteString> getDbObject2gene()
	{
		return dbObjectID2gene;
	}

	/**
	 * Returns the list of object symbols of all associations.
	 * 
	 * @return
	 */
	public List<ByteString> getListOfObjectSymbols()
	{
		ArrayList<ByteString> arrayList = new ArrayList<ByteString>();
		
		for (Association assoc : associations)
			arrayList.add(assoc.getObjectSymbol());

		return arrayList;
	}

	/**
	 * Returns the file type of the associations.
	 * 
	 * @return
	 */
	public Type getFileType()
	{
		return fileType;
	}
}

/**
 * Set containing all swiss prot ids linked to affy ids and annotaions.
 * 
 * @author sba
 */
class SwissProtAffyAnnotaionSet implements Iterable<SwissProtAffyAnnotation>
{
	private HashMap<ByteString,SwissProtAffyAnnotation> map;
	
	public SwissProtAffyAnnotaionSet()
	{
		map = new HashMap<ByteString,SwissProtAffyAnnotation>();
	}

	/**
	 * Add a new swissprot id -> affyID -> goID mappping.
	 * 
	 * @param swissProtID
	 * @param affyID
	 * @param goIDs
	 */
	public void add(ByteString swissProtID, ByteString affyID, List<TermID> goIDs)
	{
		SwissProtAffyAnnotation an = map.get(swissProtID);
		if (an == null)
		{
			an = new SwissProtAffyAnnotation(swissProtID);
			map.put(swissProtID,an);
		}
		an.addAffyID(affyID);
		for (TermID id : goIDs)
			an.addTermID(id);
	}

	public Iterator<SwissProtAffyAnnotation> iterator()
	{
		return map.values().iterator();
	}
}

/**
 * This class stores a single swiss prot id it's affymetrix probes
 * and their GO annotations.
 * 
 * @author sba
 *
 */
class SwissProtAffyAnnotation
{
	private ByteString swissProtID;
	private HashSet<ByteString> affyIDs;
	private HashSet<TermID> goIDs;
	
	public SwissProtAffyAnnotation(ByteString newSwissProtID)
	{
		swissProtID = newSwissProtID;
		affyIDs = new HashSet<ByteString>();
		goIDs = new HashSet<TermID>();
	}

	public void addAffyID(ByteString affyID)
	{
		affyIDs.add(affyID);
	}
	
	public void addTermID(TermID id)
	{
		goIDs.add(id);
	}

	public ByteString getSwissProtID()
	{
		return swissProtID;
	}

	public Collection<TermID> getGOIDs()
	{
		return goIDs;
	}
	
	public Collection<ByteString> getAffyIDs()
	{
		return affyIDs;
	}
}