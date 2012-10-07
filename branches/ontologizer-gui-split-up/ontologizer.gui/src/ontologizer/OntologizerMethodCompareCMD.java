package ontologizer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

import ontologizer.calculation.EnrichedGOTermsResult;
import ontologizer.go.Ontology;
import ontologizer.go.OBOParser;
import ontologizer.go.OBOParserException;
import ontologizer.go.TermContainer;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.Parser;

/**
 * OntologizerCMD.java
 * <P>
 * This class is used by the command-line version but not by the GUI version of
 * the Ontologizer. Its purpose is to get the command line arguments and create
 * a new instance of the class Controller, which in turn takes care of the XML
 * parsing and HTML or XML output.
 * </P>
 * <P>
 * Note that the static main method is the only thing that is used in this
 * class.
 * </P>
 * 
 * @author Peter Robinson
 * @version 0.22 2005-07-11
 */
@SuppressWarnings("unused")
public class OntologizerMethodCompareCMD
{
	/**
	 * The required argument stuff of the jakarta cli didn't work
	 * as expeced, so we have to do this manually. If specifed
	 * argument is not found an appropriate error message is written
	 * the program exited.
     *
	 * @param cmd
	 * @param name
	 * @return
	 */
	private static String getRequiredOptionValue(CommandLine cmd, char name)
	{
		String val = cmd.getOptionValue(name);
		if (val == null)
		{
			System.err.println("Aborting because the required argument \"-" + name + "\" wasn't specified! Use the -h for more help.");
			System.exit(-1);
		}
		return val;
	}
	
	/**
	 * Extract command line arguments and use them to direct program flow using
	 * a Controller instance.
	 * 
	 * @param args
	 *            an array of Strings that will passed to a myCommandLine object
	 * @see myCommandLine
	 * @see OntologizerCore
	 */
	@SuppressWarnings("static-access")
	public static void main(String[] args)
	{
		OntologizerCore.Arguments arguments = new OntologizerCore.Arguments();

		try
		{			
			Options options = new Options();
			Option opt;

			options.addOption(new Option("h","help",false,"Shows this help"));
			options.addOption(new Option("g","go",true,"File containig GO terminology and structure (.obo format). Required"));
			options.addOption(new Option("a","association",true,"File containing associations from genes to GO terms. Required"));
			options.addOption(new Option("p","population",true,"File containing genes within the population. Required"));
			options.addOption(new Option("s","studyset",true,"File of the study set or a directory containing study set files. Required"));
			options.addOption(new Option("r","resamplingsteps", true, "Specifies the number of steps used in resampling based MTCs"));
			//options.addOption(new Option("c","calculation",true,calHelpString));
			//options.addOption(new Option("m","mtc",true,mtcHelpString));
			//options.addOption(new Option("d","dot",false,"For every studyset analysis write out an addiotional .dot file containing significant nodes usable for GraphViz."));
			

			Parser parser = new GnuParser();
			CommandLine cmd = parser.parse(options,args);
			if (cmd.hasOption("h"))
			{
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("Ontologizer", options);
				System.exit(0);
			}

			arguments.goTermsOBOFile = getRequiredOptionValue(cmd,'g');
			arguments.associationFile = getRequiredOptionValue(cmd,'a');
			arguments.populationFile = getRequiredOptionValue(cmd,'p');
			arguments.studySet = getRequiredOptionValue(cmd,'s');
			if (cmd.hasOption('r'))
			{
				try
				{
					String rStr = cmd.getOptionValue('r');
					int r = Integer.parseInt(rStr);
					if (r < 100 || r > 100000) throw new Exception();
					arguments.resamplingSteps = r;
				} catch (Exception e)
				{
					System.err.println("The resampling parameter needs to be an integer between 100 and 100000");
					System.exit(-1);
				}
			}


			/* Now issue the calculation */
			
			String calculations[] = new String[] {"Term-For-Term", "Parent-Child"};
			//String mtcs[] = new String[] {"Bonferroni", "Westfall-Young-Single-Step"};
			String mtcs[] = new String[] {"Westfall-Young-Single-Step"};
			//String mtcs[] = new String[] {"Bonferroni"};
			
			HashMap<String,StudySetResultList> studResListsHash = new HashMap<String,StudySetResultList>();
			
			
			for (String calc : calculations) {
				arguments.calculationName = calc;
				for (String mtc : mtcs) {
					arguments.correctionName = mtc;
					
					OntologizerCore controller = new OntologizerCore(arguments);
					controller.calculate();
					
					Iterator<EnrichedGOTermsResult> iter = controller.studySetResultIterator();
					while (iter.hasNext())
					{
						EnrichedGOTermsResult studySetResult = iter.next();
						String studySetName = studySetResult.getStudySet().getName();
						
						if (studResListsHash.containsKey(studySetName)) {
							studResListsHash.get(studySetName).addStudySetResult(studySetResult);
						} else {
							StudySetResultList newStudResList = new StudySetResultList();
							newStudResList.setName(studySetName);
							newStudResList.addStudySetResult(studySetResult);
							studResListsHash.put(studySetName, newStudResList);
						}
						
						// outfile names are composed of StudySet name, calculation name and correction name 
						String outBasename = studySetName
						+ "-" + arguments.calculationName
						+ "-" + arguments.correctionName;
						String tableName = "table-" + outBasename + ".txt";
						File outFile = new File(tableName);
						studySetResult.writeTable(outFile);
						
					}
				}
			}
			
			
			/* Parse the gene_ontology.obo file to get information about all terms.
			 * Transfer the information to a TermContainer object.
			 */
			System.out.println("Parse obo file");
			OBOParser oboParser = new OBOParser(arguments.goTermsOBOFile);
			System.out.println(oboParser.doParse());
			TermContainer goTerms = new TermContainer(oboParser.getTermMap(), oboParser.getFormatVersion(), oboParser.getDate());
			System.out.println("Building graph");
			Ontology graph = new Ontology(goTerms); 
			
			DOTDumper dotDumper = new DOTDumper(graph);
			double alpha = 0.05;
			
			for (StudySetResultList studResList : studResListsHash.values()) {
				File file = new File(studResList.getName() + ".dot");
				dotDumper.Dump2DOT(studResList, file, alpha);
			}

			/* Write out the results to the current directory */

		} catch (ParseException e)
		{
			System.err.println("Unable to parse the command line: " + e.getLocalizedMessage());
			System.exit(-1);
		} catch (FileNotFoundException e)
		{
			System.err.println(e.getMessage());
			System.exit(-1);
		} catch (IOException e)
		{
			System.err.println(e.getMessage());
			System.exit(-1);
		} catch (OBOParserException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
