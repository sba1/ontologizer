package ontologizer.association;

import static ontologizer.types.ByteString.b;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PushbackInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.logging.Logger;

import ontologizer.ontology.IParserInput;
import ontologizer.ontology.TermID;
import ontologizer.ontology.TermMap;
import ontologizer.types.ByteString;

/**
 * Parser of affymetrix annotation files. Unsupported and unmaintained.
 *
 * @author Sebastian Bauer
 */
public class AffyParser
{
	/** Predefined three slashes string */
	private static ByteString THREE_SLASHES = b("///");

	private static Logger logger = Logger.getLogger(AffyParser.class.getName());

	private ArrayList<Association> associations = new ArrayList<Association>();
	private HashMap<ByteString,ByteString> synonym2Symbol = new HashMap<ByteString,ByteString>();

	public void parse(IParserInput input, byte [] head, HashSet<ByteString> names, TermMap terms, IAssociationParserProgress progress) throws IOException
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
			SwissProtAffyAnnotationSet annotationSet = new SwissProtAffyAnnotationSet();

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
					synonym2Symbol.put(affy,swissID);
				}
			}
		}

		System.err.println("Skipped " + skipped + " annotations");
	}

	public ArrayList<Association> getAssociations()
	{
		return associations;
	}

	public HashMap<ByteString, ByteString> getSynonym2Symbol()
	{
		return synonym2Symbol;
	}
}
