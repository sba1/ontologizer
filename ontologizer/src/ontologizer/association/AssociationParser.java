package ontologizer.association;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PushbackInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import ontologizer.linescanner.AbstractByteLineScanner;
import ontologizer.ontology.IParserInput;
import ontologizer.ontology.TermID;
import ontologizer.ontology.TermMap;
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
	private static Logger logger = Logger.getLogger(AssociationParser.class.getName());

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
	 * @param input
	 * @param terms
	 * @throws IOException
	 */
	public AssociationParser(IParserInput input, TermMap terms) throws IOException
	{
		this(input,terms,null);
	}


	/**
	 * Construct the association parser object. The given file name will
	 * parsed. Convenience constructor when not using progress monitor.
	 *
	 * @param input
	 * @param terms
	 * @param names
	 * @throws IOException
	 */
	public AssociationParser(IParserInput input, TermMap terms, HashSet<ByteString> names) throws IOException
	{
		this(input,terms,names,null);
	}

	/**
	 * Construct the association parser object. The given file name will
	 * parsed.
	 *
	 * @param input specifies wrapping input that contains association of genes to GO terms.
	 * @param terms the container of the GO terms
	 * @param names list of genes from which the associations should be gathered.
	 *        If null all associations are taken,
	 * @param progress
	 * @throws IOException
	 */
	public AssociationParser(IParserInput input, TermMap terms, HashSet<ByteString> names, IAssociationParserProgress progress) throws IOException
	{
		this(input,terms,names,null,progress);
	}


	/**
	 * Construct the association parser object. The given file name will
	 * parsed.
	 *
	 * @param input specifies wrapping input that contains association of genes to GO terms.
	 * @param terms the container of the GO terms
	 * @param names list of genes from which the associations should be gathered.
	 *        If null all associations are taken,
	 * @param evidences keep only the annotation whose evidence match the given ones. If null, all annotations are used.
	 *        Note that this field is currently used when the filenames referes to a GAF file.
	 * @param progress
	 * @throws IOException
	 */
	public AssociationParser(IParserInput input, TermMap terms, HashSet<ByteString> names, Collection<String> evidences, IAssociationParserProgress progress) throws IOException
	{
		associations = new ArrayList<Association>();
		synonym2gene = new HashMap<ByteString, ByteString>();
		dbObjectID2gene = new HashMap<ByteString, ByteString>();

		if (input.getFilename().endsWith(".ids"))
		{
			importIDSAssociation(input,terms,progress);
			fileType = Type.IDS;
		} else
		{
			/* First, skip headers */
			final List<byte[]> lines = new ArrayList<byte[]>();
			AbstractByteLineScanner abls = new AbstractByteLineScanner(input.inputStream()) {
				@Override
				public boolean newLine(byte[] buf, int start, int len)
				{
					if (len > 0 && buf[start] != '#')
					{
						byte [] b = new byte[len];
						System.arraycopy(buf, start, b, 0, len);
						lines.add(b);
						return false;
					}
					return true;
				}
			};
			abls.scan();

			if (lines.size() == 0)
				return;

			byte [] head = merge(lines.get(0), abls.availableBuffer());

			if (new String(head).startsWith("\"Probe Set ID\",\"GeneChip Array\""))
			{
				importAffyFile(input,head,names,terms,progress);
				fileType = Type.AFFYMETRIX;
			} else
			{
				importAssociationFile(input,head,names,terms,evidences,progress);
				fileType = Type.GAF;
			}
		}
	}

	/**
	 * Import the annotation from a file generated by GOStat.
	 *
	 * @param input
	 * @param
	 */
	private void importIDSAssociation(IParserInput input, TermMap terms, IAssociationParserProgress progress)
	{
		try
		{
			BufferedReader is = new BufferedReader(new InputStreamReader(input.inputStream()));
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
		} catch (IOException e)
		{
		}
	}

	/**
	 * Get from a collection of strings a ByteString set.
	 *
	 * @param strings
	 * @return
	 */
	private static Set<ByteString> getByteStringSetFromStringCollection(Collection<String> strings)
	{
		Set<ByteString> byteStrings; /* Evidences converted to ByteString */

		if (strings != null)
		{
			byteStrings = new HashSet<ByteString>();
			for (String e : strings)
				byteStrings.add(new ByteString(e));
		} else
		{
			byteStrings = null;
		}
		return byteStrings;
	}

	/**
	 * Import GAF.
	 *
	 * @param input the wrapped input.
	 * @param head the header of the file. Basically, the beginning of the text until the current position of the input.
	 * @param names names of items that are interesting or null if annotations of them should be considered
	 * @param terms all known terms
	 * @param evidences specifies which annotations to take.
	 * @param progress used for monitoring progress.
	 * @throws IOException
	 */
	private void importAssociationFile(IParserInput input, byte [] head, HashSet<ByteString> names, TermMap terms, Collection<String> evidences, IAssociationParserProgress progress) throws IOException
	{
		if (progress != null)
			progress.init(input.getSize());

		GAFByteLineScanner ls = new GAFByteLineScanner(input, head, names, terms,getByteStringSetFromStringCollection(evidences), progress);
		ls.scan();

		if (progress != null)
			progress.update(input.getSize());

		logger.log(Level.INFO, ls.good + " associations parsed, " + ls.kept
				+ " of which were kept while " + ls.bad
				+ " malformed lines had to be ignored.");
		logger.log(Level.INFO, "A further " + ls.skipped
				+ " associations were skipped due to various reasons whereas "
				+ ls.nots + " of those where explicitly qualified with NOT, " +
				+ ls.obsolete + " referred to obsolete terms and "
				+ ls.evidenceMismatch + " didn't"
				+ " match the requested evidence codes");
		logger.log(Level.INFO, "A total of " + ls.getNumberOfUsedTerms()
				+ " terms are directly associated to " + dbObjectID2gene.size()
				+ " items.");

		associations = ls.getAssociations();
		synonym2gene = ls.getSynonym2Gene();
		dbObjectID2gene = ls.getDbObjectID2Gene();


		if (symbolWarnings >= 1000)
			logger.warning("The symbols of a total of " + symbolWarnings + " entries mapped ambiguously");
		if (dbObjectWarnings >= 1000)
			logger.warning("The objects of a  total of " + dbObjectWarnings + " entries mapped ambiguously");
	}

	/**
	 *
	 *
	 * @param names
	 * @param terms
	 * @param progress
	 * @throws IOException
	 */
	private void importAffyFile(IParserInput input, byte [] head, HashSet<ByteString> names, TermMap terms, IAssociationParserProgress progress) throws IOException
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

		if (progress != null)
			progress.init(input.getSize());

		int skipped = 0;
		long millis = 0;

		String line;

		PushbackInputStream pis = new PushbackInputStream(input.inputStream());
		pis.unread(head);

		BufferedReader in = new BufferedReader(new InputStreamReader(pis));

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
						progress.update(input.getPosition());
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
													TermID id = new TermID(TermID.DEFAULT_PREFIX,goId);

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
	 * @return the list of object symbols of all associations.
	 */
	public List<ByteString> getListOfObjectSymbols()
	{
		ArrayList<ByteString> arrayList = new ArrayList<ByteString>();

		for (Association assoc : associations)
			arrayList.add(assoc.getObjectSymbol());

		return arrayList;
	}

	/**
	 * @return the file type of the associations.
	 */
	public Type getFileType()
	{
		return fileType;
	}

	/**
	 * Merge two byte arrays.
	 *
	 * @param a1
	 * @param a2
	 * @return
	 */
	private static byte [] merge(byte [] a1, byte [] a2)
	{
		byte [] b = new byte[a1.length + a2.length];
		System.arraycopy(a1, 0, b, 0, a1.length);
		System.arraycopy(a2, 0, b, a1.length,a2.length);
		return b;
	}
}
