package ontologizer.sampling;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Formatter;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ontologizer.association.AssociationContainer;
import ontologizer.association.AssociationParser;
import ontologizer.calculation.CalculationRegistry;
import ontologizer.calculation.EnrichedGOTermsResult;
import ontologizer.calculation.ICalculation;
import ontologizer.calculation.TermForTermGOTermProperties;
import ontologizer.enumeration.GOTermCounter;
import ontologizer.enumeration.GOTermEnumerator;
import ontologizer.enumeration.GOTermEnumerator.GOTermAnnotatedGenes;
import ontologizer.go.Ontology;
import ontologizer.go.OBOParser;
import ontologizer.go.TermContainer;
import ontologizer.go.TermID;
import ontologizer.go.Namespace;
import ontologizer.go.Namespace.NamespaceEnum;
import ontologizer.set.PopulationSet;
import ontologizer.set.StudySet;
import ontologizer.statistics.AbstractTestCorrection;
import ontologizer.statistics.PValue;
import ontologizer.statistics.TestCorrectionRegistry;
import ontologizer.types.ByteString;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.Parser;

public class SetConstructor
{

	/**
	 * The required argument stuff of the jakarta cli didn't work as expected,
	 * so we have to do this manually. If specified argument is not found an
	 * appropriate error message is written the program exited.
	 * 
	 * @param cmd
	 *            the command line to parse
	 * @param name
	 *            the name of the argument to check for
	 * @return the value of the argument as a string
	 */
	private static String getRequiredOptionValue(CommandLine cmd, String name)
	{
		String val = cmd.getOptionValue(name);
		if (val == null)
		{
			System.err.println("Aborting because the required argument \"-"
					+ name + "\" wasn't specified! Use the -h for more help.");
			System.exit(-1);
		}
		return val;
	}

	/**
	 * Recursively deletes the directory specified.
	 * 
	 * @param path
	 *            the directory to delete
	 * @return a boolean telling success or failure
	 */
	static public boolean deleteDirectory(File path)
	{
		if (path.exists())
		{
			File[] files = path.listFiles();
			for (int i = 0; i < files.length; i++)
			{
				if (files[i].isDirectory())
				{
					deleteDirectory(files[i]);
				} else
				{
					files[i].delete();
				}
			}
		}
		return (path.delete());
	}

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception
	{
		String oboFileName;
		String assocFileName;
		String baseSampleFileName;
		String outDirName;
		boolean forceOutDirDelete;
		boolean listAllTerms;
		boolean completeSet;
		boolean wantSubTermMatrix;
		boolean listAllAnnotations;
		String enrichSingle;
		boolean enrichMany;
		int popSize;
		int nSamples;

		try
		{
			// Reading options
			Options options = new Options();

			options.addOption("h", "help", false, "Shows this help");
			options.addOption("g", "go", true, "File containig GO terminology and structure (.obo format). Required.");
			options.addOption("a", "association", true, "File containing associations from genes to GO terms. Required.");
			options.addOption("p", "popsize", true, "Size of population set to create. Optional, defaults to 1000.");
			options.addOption("s", "samplefile", true,
							  "Name of the sample file to be created. "
							+ "A trailing '.txt' will be appended, other suffixes are removed. "
							+ "Optional, defaults to 'sampled_genes'.");
			options.addOption("l", "listallterms", false,
					          "Creates for every GO term a file listing its associated genes if set. "
							+ "Files are written to a directory with the name specified by the '-s' option with a trailing '.tld'.");
			options.addOption("la", "listallannotations", false,
					          "Writes out a file named \"annotations.txt\" containing a list of go terms with their (direct) annotations.");
			options.addOption("o", "outdir", true, "Directory to hold results. Optional, defaults to '.'");
			options.addOption("f", "force", false, "Forces a delete of the outdir if set.");
			options.addOption("c", "complete", false, "The sampled set consists of all annotated genes if set. Ignores -p and -n.");
			options.addOption("n", "nsamples", true, "Number of samples to generate. Samples are numbered if necessary. Defaults to 1");
			options.addOption("sm", "subTermMatrix", false, "If specified, the output is a matrix in which element (i,j) is set to 1, only if term i is a subterm of term j.");
			options.addOption("es", "enrichSingle", true,
							  "Specify a term enrichment strategy for the sampling of a single term enriched set. "
							+ "A term enrichment strategy has to be specified in the following way: "
							+ "For each term to enrich, specify first the GO ID of the term in the form GO:XXXXXXX "
							+ "and then, separated by a comma, an integer value between 0 and 100 "
							+ "specifying how many percent of the term's genes should end up in the sampled file. "
							+ "Finally, add another integer value between 0 and 100 specifying the percentage of genes "
							+ "from the rest to put into the sample. "
							+ "Ignores -p, -n and -c");
			options.addOption("em", "enrichMany", false,
							  "Flag telling that term over-represented sets should be created for a large number of terms. "
							+ "This requires that at least the options -emc, -emp and -emr are specified. "
							+ "The option -emn is optional.");
			options.addOption("emc","enrichManyCalc",true,
							  "Specifies which calculation methods should be considered, "
							+ "when choosing terms to overrepresent. "
							+ "Specify the full names of the calculation methods as Term-For-Term, Parent-Child, etc. "
							+ "If you want to specify more than one method, separate them by a space and surround "
							+ "everything by double quotes. "
							+ "Required, if -em is set, ignored otherwise.");
			options.addOption("emp","enrichManyPcut",true,
							  "the p-value cutoff to use when determining the "
							+ "terms with sufficiently small all-subset minimal p-value. "
							+ "Required, if -em is set, ignored otherwise.");
			options.addOption("emr","enrichManyRule",true,
							  "Specifies the rule to be used for the over-representation. "
							+ "The rule has to be given as a comma-separated list of iteger values between 0 and 100 "
							+ "containing at least two values. "
							+ "The last value represents the noise percentage, i.e. the percentage "
							+ "of the rest of the genes that should end up in the sample. "
							+ "The other values specify the percentage of genes that should be sampled from the terms. "
							+ "The number of terms to over-represent is determined by the number of specified values minus 1 "
							+ "(for the last noise value). "
							+ "For example, if you specify '20,10', then in each sampled set one term is over-represented "
							+ "at a percentage of 20 and 10 percent noise is added (Furthermore -emn is ignored). "
							+ "As another example, if you specify '15,20,10', then in each sample two terms are "
							+ "over-represented at percentages 15 and 20, respectively. Then 10 percent of noise is added. "
							+ "Required, if -em is set, ignored otherwise.");
			options.addOption("emn","enrichManySamples",true,
							  "Specifies the number of sets to produce. "
							+ "Defaults to the 1000 for multiple term enrichment "
							+ "(i.e., when more than 2 values are specified in -emr). "
							+ "Ignored for single term enrichment (2 values in -emr), all terms are considered then by default. "
							+ "Optional, if -em is set, ignored otherwise.");

			Parser parser = new GnuParser();
			CommandLine cmd = parser.parse(options, args);
			if (cmd.hasOption("h"))
			{
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp(
						"SetConstructor [-h] -a <association file> -g <go file> "
								+ "["
								+ "-p <samplesize> "
								+ "-s <sample file> "
								+ "-o <outdir> "
								+ "-n <nsamples> "
								+ "-c "
								+ "-l "
								+ "-f"
								+ "]"
								+ "[-es <rule>]"
								+ "[-em -emr <rule> -emc <calculations> -emp <pvaluecut> [-emn <nsaples>]]",
						"Tool to sample all kinds of sets of genes from a given set of Gene-GO associations. "
								+ "Can sample single or multiple sets with or without enrichment of terms. "
								+ "Has many options, see below!", options, "");
				System.exit(0);
			}

			oboFileName = getRequiredOptionValue(cmd, "g");
			assocFileName = getRequiredOptionValue(cmd, "a");

			baseSampleFileName = cmd.getOptionValue("s", "sampled_genes");
			// remove eventually existing suffixes
			Pattern p = Pattern.compile("\\.[a-zA-Z0-9]+$");
			Matcher m = p.matcher(baseSampleFileName);
			baseSampleFileName = m.replaceAll("");

			outDirName = cmd.getOptionValue("o", ".");
			popSize = Integer.valueOf(cmd.getOptionValue("p", "1000"));
			forceOutDirDelete = cmd.hasOption("f");
			listAllTerms = cmd.hasOption("l");
			completeSet = cmd.hasOption("c");
			enrichMany = cmd.hasOption("em");
			nSamples = Integer.valueOf(cmd.getOptionValue("n", "1"));
			enrichSingle = cmd.getOptionValue("es");
			wantSubTermMatrix = cmd.hasOption("sm");
			listAllAnnotations = cmd.hasOption("la");

			// make sure we don't generate more than one complete set
			if (completeSet)
			{
				nSamples = 1;
			}

			// loading GO graph
			System.out.println("Parse obo file");
			OBOParser oboParser = new OBOParser(oboFileName);
			System.out.println(oboParser.doParse());

			TermContainer goTerms = new TermContainer(oboParser.getTermMap(), oboParser.getFormatVersion(), oboParser.getDate());
			System.out.println("Building graph");
			Ontology graph = new Ontology(goTerms);

			// loading associations
			AssociationParser assocParser = new AssociationParser(assocFileName, goTerms, null);
			AssociationContainer assocs = new AssociationContainer(assocParser.getAssociations(), assocParser.getSynonym2gene(), assocParser.getDbObject2gene());

			// making outDir if necessary
			File outDir = null;
			if (!outDirName.equals("."))
			{
				outDir = new File("./" + outDirName);
				if (forceOutDirDelete)
					deleteDirectory(outDir);
				outDir.mkdirs();
			}

			// we need a population set as a subset of the sets for which we
			// have associations, construct the latter
			Set<ByteString> allAnnotatedGenes = assocs.getAllAnnotatedGenes();
			PopulationSet completePop = new PopulationSet("AllAnnotated");
			for (ByteString gene : allAnnotatedGenes)
				completePop.addGene(gene, "None");
			popSize = java.lang.Math.min(completePop.getGeneCount(), popSize);

			if (wantSubTermMatrix)
			{
				GOTermCounter populationTermCounter = completePop.countGOTerms(graph,assocs);
				PrintWriter matrixOut = new PrintWriter(new File(outDir,"subtermMatrix.txt"));
				
				matrixOut.print("TermID");
				for (TermID i : populationTermCounter)
				{
					matrixOut.print("\t");
					matrixOut.print(i.toString());
				}
				matrixOut.println();
				
				for(TermID i : populationTermCounter)
				{
					matrixOut.print(i.toString());

					for (TermID j : populationTermCounter)
					{
						matrixOut.print("\t");
						if (i.equals(j))
							matrixOut.print("1");
						else if (graph.existsPath(i,j))
							matrixOut.print("1");
						else
							matrixOut.print("0");
					}
					matrixOut.println();
				}
				matrixOut.flush();
				matrixOut.close();
			}

			if (listAllAnnotations)
			{
				PrintWriter annoOut = new PrintWriter(new File(outDir,"annotations.txt"));
				GOTermEnumerator populationTermEnumerator = completePop.enumerateGOTerms(graph,assocs);
				
				for (TermID t : populationTermEnumerator)
				{
					GOTermAnnotatedGenes anno = populationTermEnumerator.getAnnotatedGenes(t);
					for (ByteString gene : anno.directAnnotated)
					{
						annoOut.write(t.toString());
						annoOut.write("\t");
						annoOut.write(gene.toString());
						annoOut.println();
					}
				}
				
				annoOut.flush();
				annoOut.close();
			}
			
			// now we sample a subset of a certain size
			StudySet sampleStudySet;
			if (enrichMany)
			{
				System.out.println("You want to construct a large number of study set samples with artificial enrichment");
				// further parsing of arguments
				// enrichment rule
				String emrValue = getRequiredOptionValue(cmd, "emr");
				String[] enrichValueStrings = emrValue.split(",");
				int k = enrichValueStrings.length;
				if (k == 2) {
					String termPercUnsplit = enrichValueStrings[0];
					String[] subPartsFirst = termPercUnsplit.split("\\."); // who would have thought that one needs double escaping for the dot...
					if (subPartsFirst.length > 1) {
						String oldNoise = enrichValueStrings[1];
						enrichValueStrings = new String[subPartsFirst.length + 1];
						enrichValueStrings[subPartsFirst.length] = oldNoise;
						for (int i=0; i < subPartsFirst.length; i++) {
							enrichValueStrings[i] = subPartsFirst[i];
						}
					}
				}
				int nVals = enrichValueStrings.length;
				int nTupel = nVals - 1;
				int[] termEnrichValues = new int[nTupel];
				for (int i = 0; i < nVals - 1; i++)
				{
					termEnrichValues[i] = Integer.valueOf(enrichValueStrings[i]);
				}
				int noiseEnrichValue = Integer.valueOf(enrichValueStrings[nTupel]);

				System.out.println("The parsed term enrichment values are:");
				for (int val : termEnrichValues)
				{
					System.out.println("\t" + val);
				}
				System.out.println("the parsed noise enrichment value is: "
						+ noiseEnrichValue);

				// calculation methods to consider
				String emcValue = getRequiredOptionValue(cmd, "emc");
				if (emcValue.endsWith("\""))
					emcValue = emcValue.substring(0, emcValue.length() - 1);
				if (emcValue.startsWith("\""))
					emcValue = emcValue.substring(1);
				String[] calcNames = emcValue.split(" ");
				ArrayList<ICalculation> calcs = new ArrayList<ICalculation>();
				for (String name : calcNames)
				{
					ICalculation calc = CalculationRegistry.getCalculationByName(name);
					if (calc == null)
					{
						System.err.println("Calculation \"" + name + "\" doesn't exist");
						System.exit(0);
					}
					calcs.add(calc);
				}
				System.out.println("The calculation methods to consider are:");
				for (ICalculation calc : calcs)
				{
					System.out.println("\t" + calc.getName());
				}

				// p-value cutoff for all-subset minimal p-values
				String empValue = getRequiredOptionValue(cmd, "emp");
				double pCut = Double.valueOf(empValue);
				System.out.println("Cutoff to use for all-subset minimal p-values is "
						+ pCut);

				// constructing all-subset minimal p-value data
				System.out.println("Determining terms with an all-subset minimal p-value below "
						+ pCut + " (good terms) for all calculation methods.");
				AbstractTestCorrection noCorrection = TestCorrectionRegistry.getCorrectionByName("None");
				HashSet<TermID> goodTerms = new HashSet<TermID>();
				boolean firstcalc = true;

				for (ICalculation curCalc : calcs)
				{
					EnrichedGOTermsResult basicResult = curCalc.calculateStudySet(graph, assocs, completePop, completePop, noCorrection);
					HashSet<TermID> thisCalcGoodTerms = basicResult.getGoodTerms(pCut);
					System.out.println("Calculation method "
							+ curCalc.getName() + " has a total of "
							+ thisCalcGoodTerms.size() + " good terms");
					if (firstcalc)
					{
						goodTerms = thisCalcGoodTerms;
						firstcalc = false;
					} else
					{
						goodTerms.retainAll(thisCalcGoodTerms);
					}
				}
				System.out.println("We are left with a total of "
						+ goodTerms.size() + " terms in the intersection.");

				// number of samples
				int nGoodTerms = goodTerms.size();
				int nTermCombis = nGoodTerms;
				if (nTupel > 1)
				{
					nTermCombis = Integer.valueOf(cmd.getOptionValue("emn",
							"1000"));
				}

				// creating studysets
				System.out.println("Creating samples...");

				// We begin with the term combinations
				HashSet<ArrayList<TermID>> sampledTermCombinations = new HashSet<ArrayList<TermID>>();
				if (nTupel == 1) // we don't have to take care about the
									// namespace
				{
					KSubsetSampler<TermID> subsetSampler = new KSubsetSampler<TermID>(goodTerms);
					sampledTermCombinations.addAll(subsetSampler.sampleManyOrderedWithoutReplacement(
							nTupel, nTermCombis));
				} else
				// when sampling combinations of terms we want them to be in the
				// same namespace
				{
					TermID bioproc = new TermID(8150);
					TermID cellcomp = new TermID(5575);
					TermID molfunc = new TermID(3674);
					int nCombisPart = nTermCombis / 3;
					TermID[] mainTerms =
					{ bioproc, cellcomp, molfunc };
					for (TermID tid : mainTerms)
					{
						NamespaceEnum ns = Namespace.getNamespaceEnum(graph.getTerm(tid).getNamespace());
						HashSet<TermID> thisGoodTerms = new HashSet<TermID>();
						for (TermID gtid : goodTerms) {
							NamespaceEnum gns = Namespace.getNamespaceEnum(graph.getTerm(gtid).getNamespace());
							if (ns == gns)
								thisGoodTerms.add(gtid);
						}
						System.out.println("We are left with a total number of "
								+ thisGoodTerms.size()
								+ " good terms for namespace " + tid.toString());
						KSubsetSampler<TermID> subsetSampler = new KSubsetSampler<TermID>(thisGoodTerms);
						sampledTermCombinations.addAll(subsetSampler.sampleManyOrderedWithoutReplacement(
								nTupel, nCombisPart));
					}
				}

				StudySetSampler sampler = new StudySetSampler(completePop);
				int count = 0;
				for (ArrayList<TermID> termCombi : sampledTermCombinations)
				{
					System.out.print("Creating " + ++count + " of "
							+ nTermCombis + " samples...\r");
					PercentageEnrichmentRule rule = new PercentageEnrichmentRule();
					for (int i = 0; i < termCombi.size(); i++)
					{
						rule.addTerm(termCombi.get(i), termEnrichValues[i]);
					}
					rule.setNoisePercentage(noiseEnrichValue);
					String finalSampleFileName = buildFinalSampleFileNameForMultipleTerm(
							termCombi, termEnrichValues, noiseEnrichValue);

					sampleStudySet = sampler.sampleRandomStudySet(graph,
							assocs, rule, true);
					if (sampleStudySet == null)
					{
						System.out.println("At least one term wouldn't be overrepresented due to lack of genes annotated to it. Skipping this study set.");
					} else
					{
						writeAllFiles(finalSampleFileName, listAllTerms, graph,
							assocs, outDir, sampleStudySet);
					}
				}
			} else if (enrichSingle != null)
			{
				PercentageEnrichmentRule enrichDat = parseEs(enrichSingle,
						goTerms);
				String finalSampleFileName = buildFinalSampleFileName(
						baseSampleFileName, 1, 1);
				StudySetSampler sampler = new StudySetSampler(completePop);
				sampleStudySet = sampler.sampleRandomStudySet(graph, assocs,
						enrichDat, false);
				writeAllFiles(finalSampleFileName, listAllTerms, graph, assocs,
						outDir, sampleStudySet);
			} else if (completeSet || popSize == completePop.getGeneCount())
			{
				sampleStudySet = completePop;
				writeAllFiles(baseSampleFileName, listAllTerms, graph, assocs,
						outDir, sampleStudySet);
			} else
			{
				for (int i = 0; i < nSamples; i++)
				{
					String finalSampleFileName = buildFinalSampleFileName(
							baseSampleFileName, i, nSamples);
					sampleStudySet = completePop.generateRandomStudySet(popSize);
					writeAllFiles(finalSampleFileName, listAllTerms, graph,
							assocs, outDir, sampleStudySet);
				}
			}

		} catch (ParseException e)
		{
			System.err.println("Unable to parse the command line: "
					+ e.getLocalizedMessage());
			System.exit(-1);
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	private static String buildFinalSampleFileNameForMultipleTerm(
			ArrayList<TermID> termCombi, int[] termEnrichValues,
			int noiseEnrichValue)
	{
		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < termCombi.size(); i++)
		{
			sb.append(termCombi.get(i).toString().replace(":", "_"));
			sb.append("_");
			sb.append(termEnrichValues[i]);
			sb.append("_");
		}
		sb.append(noiseEnrichValue);

		return sb.toString();
	}

	/**
	 * Parses the string provided in the -es option to construct the appropriate
	 * term enrichment rule.
	 * 
	 * @param esString
	 *            the string provided in the -es option
	 * @param goTerms
	 *            needed to construct the correct term object
	 * @return the term enrichment rule
	 * @throws ParseException
	 *             if string has wrong format
	 */
	private static PercentageEnrichmentRule parseEs(String esString,
			TermContainer goTerms) throws ParseException
	{
		PercentageEnrichmentRule enrichDat = new PercentageEnrichmentRule();
		String[] splitted = esString.split(",");
		int slen = splitted.length;

		if (slen % 2 == 0)
		{
			throw (new ParseException("I need an odd number of entries in enrichment data!"));
		} else
		{
			enrichDat.setNoisePercentage(Integer.valueOf(splitted[slen - 1]));
			for (int i = 0; i < (slen - 1) / 2; i++)
			{
				TermID id = goTerms.get(splitted[2 * i]).getID();
				int perc = Integer.valueOf(splitted[2 * i + 1]);
				enrichDat.addTerm(id, perc);
			}
		}

		return enrichDat;
	}

	/**
	 * A clever routine constructing a final name for the sample file from a
	 * basename. If the total number of samples requires is larger than one,
	 * each name gets extended by a count.
	 * 
	 * @param baseSampleFileName
	 *            the basename for the sample files
	 * @param sampleNumber
	 *            the number of the actual sample
	 * @param totalSamples
	 *            the total number of samples to construct
	 * @return the final sample file name
	 */
	private static String buildFinalSampleFileName(String baseSampleFileName,
			int sampleNumber, int totalSamples)
	{
		if (totalSamples == 1)
		{
			return baseSampleFileName;
		} else
		{
			StringBuilder numberedSampleFileName = new StringBuilder();
			Formatter formatter = new Formatter(numberedSampleFileName);
			formatter.format("%06d", sampleNumber + 1);

			String finalSampleFileName = baseSampleFileName + "_"
					+ numberedSampleFileName.toString();
			return finalSampleFileName;
		}
	}

	/**
	 * Writes out the files for a specified sample. If listAllTerms is set, it
	 * also writes out a directory holding for each term a file with the current
	 * annotation.
	 * 
	 * @param sampleFileName
	 *            the name of the sample file to write
	 * @param listAllTerms
	 *            tells whether term annotation files should be constructed
	 * @param graph
	 *            the underlying graph
	 * @param assocs
	 *            needed to reduce the annotations for the subset
	 * @param outDir
	 *            that's where files get written to
	 * @param sample
	 *            the underlying population set
	 * @throws IOException
	 *             if writing fails
	 */
	private static void writeAllFiles(String sampleFileName,
			boolean listAllTerms, Ontology graph, AssociationContainer assocs,
			File outDir, StudySet sample) throws IOException
	{
		// construct a GOTermEnumerator for the reduced population set
		GOTermEnumerator sampleEnumerator = sample.enumerateGOTerms(graph,
				assocs);

		writeSampledSet(sampleFileName, sample, outDir);

		// looping over all terms and printing lists of annotated genes
		if (listAllTerms)
		{
			writeAllTermLists(sampleFileName, sampleEnumerator, outDir);
		}
	}

	/**
	 * Doing the actual job of writing the all term all lists
	 * 
	 * @param sampleFileName
	 *            the name of the corresponding sample file
	 * @param sampleEnumerator
	 *            needed to actually construct the lists
	 * @param outDir
	 *            that's where files get written to
	 * @throws IOException
	 *             if writing fails
	 */
	private static void writeAllTermLists(String sampleFileName,
			GOTermEnumerator sampleEnumerator, File outDir) throws IOException
	{
		String allTermListDirName = sampleFileName + ".tld";
		File allTermListDir = new File(outDir, allTermListDirName);
		allTermListDir.mkdirs();
		System.out.println("Writing gene lists for all GO terms. This may take a while...");
		for (TermID term : sampleEnumerator)
		{
			/* we have to replace colons in filenames */
			Pattern p = Pattern.compile(":");
			Matcher m = p.matcher(term.toString());
			String termfilename = m.replaceAll("_");

			Collection<ByteString> annGenes = sampleEnumerator.getAnnotatedGenes(term).totalAnnotated;

			// System.out.println(term + "\t" + annGenes.toString());

			// building string to write to file
			StringBuilder outString = new StringBuilder();
			for (ByteString gene : annGenes)
			{
				outString.append(gene + "\n");
			}

			File termFile = new File(allTermListDir, termfilename);
			FileWriter termOut = new FileWriter(termFile);
			termOut.write(outString.toString());
			termOut.close();
		}
	}

	/**
	 * Writes the sampled genes to a file
	 * 
	 * @param sampleFileName
	 *            the name of the sample file
	 * @param sample
	 *            holding the gene list to write
	 * @param outDir
	 *            that's where files get written to
	 * @throws IOException
	 *             if writing fails
	 */
	private static void writeSampledSet(String sampleFileName, StudySet sample,
			File outDir) throws IOException
	{
		// writing the sampled genes to file
		File workPopFile = new File(outDir, sampleFileName + ".txt");
		FileWriter workPopOut = new FileWriter(workPopFile);
		for (ByteString gene : sample)
		{
			workPopOut.write(gene + "\n");
		}
		workPopOut.close();
	}

}
