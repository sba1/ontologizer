package ontologizer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import ontologizer.calculation.CalculationRegistry;
import ontologizer.calculation.EnrichedGOTermsResult;
import ontologizer.go.OBOParserException;
import ontologizer.go.TermID;
import ontologizer.statistics.IResampling;
import ontologizer.statistics.TestCorrectionRegistry;

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
 * @author Peter Robinson and Sebastian Bauer
 */
@SuppressWarnings("unused")
public class OntologizerCMD
{
	/**
	 * The required argument stuff of the jakarta cli didn't work
	 * as expected, so we have to do this manually. If the specified
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
	public static void main(String[] args)
	{
		OntologizerCore.Arguments arguments = new OntologizerCore.Arguments();

		try
		{
			/* Build up the calculation string to show it within the help description */
			String calculations[] = CalculationRegistry.getAllRegistered();
			StringBuilder calHelpStrBuilder = new StringBuilder();
			calHelpStrBuilder.append("Specifies the calculation method to use. Possible values are: ");

			for (int i=0;i<calculations.length;i++)
			{
				calHelpStrBuilder.append("\"");
				calHelpStrBuilder.append(calculations[i]);
				calHelpStrBuilder.append("\"");
				
				/* Add default identifier if it is the default correction */
				if (CalculationRegistry.getDefault() == CalculationRegistry.getCalculationByName(calculations[i]))
						calHelpStrBuilder.append(" (default)");

				calHelpStrBuilder.append(", ");
			}
			calHelpStrBuilder.setLength(calHelpStrBuilder.length()-2); /* remove redundant last two characters */
			String calHelpString = calHelpStrBuilder.toString();

			/* Build up the mtc string to show it within the help description */
			boolean resamplingBasedMTCsExists = false;
			String mtcs[] = TestCorrectionRegistry.getRegisteredCorrections();
			StringBuilder mtcHelpStrBuilder = new StringBuilder();
			mtcHelpStrBuilder.append("Specifies the MTC method to use. Possible values are: ");
			
			for (int i=0;i<mtcs.length;i++)
			{
				if (TestCorrectionRegistry.getCorrectionByName(mtcs[i]) instanceof IResampling)
					resamplingBasedMTCsExists = true;

				mtcHelpStrBuilder.append("\"");
				mtcHelpStrBuilder.append(mtcs[i]);
				mtcHelpStrBuilder.append("\"");

				/* Add default identifier if it is the default correction */
				if (TestCorrectionRegistry.getDefault() == TestCorrectionRegistry.getCorrectionByName(mtcs[i]))
						mtcHelpStrBuilder.append(" (default)");

				mtcHelpStrBuilder.append(", ");
			}
			mtcHelpStrBuilder.setLength(mtcHelpStrBuilder.length()-2); /* remove redundant last two characters */
			String mtcHelpString = mtcHelpStrBuilder.toString();
			
			Options options = new Options();
			Option opt;

			options.addOption(new Option("h","help",false,"Shows this help"));
			options.addOption(new Option("g","go",true,"File containig GO terminology and structure (.obo format). Required"));
			options.addOption(new Option("a","association",true,"File containing associations from genes to GO terms. Required"));
			options.addOption(new Option("p","population",true,"File containing genes within the population. Required"));
			options.addOption(new Option("s","studyset",true,"File of the study set or a directory containing study set files. Required"));
			options.addOption(new Option("i","ignore",false,"Ignore genes to which no association exist within the calculation."));
			options.addOption(new Option("c","calculation",true,calHelpString));
			options.addOption(new Option("m","mtc",true,mtcHelpString));
			options.addOption(new Option("d","dot",true, "For every study set analysis write out an additional .dot file (GraphViz) containing "+
														 "the graph that is induced by interesting nodes. The optional argument in range between 0 and 1 "+
														 "specifies the threshold used to identify interesting nodes. "+
														 "By appending a GO Term identifier (separated by a comma) the output is restriced to the " +
														 "subgraph originating at this GO term."));
			options.addOption(new Option("n","annotation",false,"Create an additional file per study set which contains the annotations."));
			options.addOption(new Option("f","filter",true,"Filter the gene names by appling rules in a given file (currently only mapping supported)."));
			options.addOption(new Option("o","outdir",true,"Specfies the directory in which the results will be placed."));

			if (resamplingBasedMTCsExists) {
				options.addOption(new Option("r","resamplingsteps", true, "Specifies the number of steps used in resampling based MTCs"));
				options.addOption(new Option("t","sizetolerance", true, "Specifies the percentage at which the actual study set size and " +
						"the size of the resampled study sets are allowed to differ"));
			}
			options.addOption(new Option("v","version",false,"Shows version information and exits"));

			Parser parser = new GnuParser();
			CommandLine cmd = parser.parse(options,args);
			if (cmd.hasOption("h"))
			{
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("Ontologizer", options);
				System.exit(0);
			}

			if (cmd.hasOption("v"))
			{
				System.out.println("Ontologizer " + BuildInfo.getVersion() + " (Build: " + BuildInfo.getBuildString() + ")");
				System.out.println();
				System.out.println("Copyright (C) " + BuildInfo.getCopyright() + " Ontologizer Development Team.");
				System.exit(0);
			}

			arguments.goTermsOBOFile = getRequiredOptionValue(cmd,'g');
			arguments.associationFile = getRequiredOptionValue(cmd,'a');
			arguments.populationFile = getRequiredOptionValue(cmd,'p');
			arguments.studySet = getRequiredOptionValue(cmd,'s');
			arguments.calculationName = cmd.getOptionValue('c');
			arguments.correctionName = cmd.getOptionValue('m');
			arguments.filterOutUnannotatedGenes = cmd.hasOption('i');
			arguments.filterFile = cmd.getOptionValue('f');

			/* Prepare the output directory name */
			String outputDirectoryName = cmd.getOptionValue('o', ".");
			if (!outputDirectoryName.equals("."))
			{
				File f = new File(outputDirectoryName);
				if (!f.exists())
					f.mkdirs();
				else
				{
					if (!f.isDirectory())
					{
						System.err.println("The specified output is not a directory!");
						System.exit(-1);
					}
				}
			}

			/* Testing availbility of calculation method */
			if (arguments.calculationName != null)
			{
				boolean found = false;
				for (int i=0;i<calculations.length;i++)
				{
					if (arguments.calculationName.equals(calculations[i]))
					{
						found = true;
						break;
					}
				}
				
				if (!found)
				{
					System.err.println("Given calculation method " + arguments.calculationName + " wasn't found");
					System.exit(-1);
				}
			}

			/* Testing availbility of mtc method */
			if (arguments.correctionName != null)
			{
				boolean found = false;
				for (int i=0;i<mtcs.length;i++)
				{
					if (arguments.correctionName.equals(mtcs[i]))
					{
						found = true;
						break;
					}
				}
				
				if (!found)
				{
					System.err.println("Given MTC method " + arguments.correctionName + " wasn't found");
					System.exit(-1);
				}
			}

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

			if (cmd.hasOption('t'))
			{
				try
				{
					String tStr = cmd.getOptionValue('t');
					int t = Integer.parseInt(tStr);
					if (t < 1 || t > 100) throw new Exception();
					arguments.sizeTolerance = t;
				} catch (Exception e)
				{
					System.err.println("The sizetolerance is a percentage between 1 and 100");
					System.exit(-1);
				}
			}

			/* Evaluate the dot option */
			boolean createDOTFile = cmd.hasOption('d');
			double alpha = 0.05;
			TermID dotRootID = null;
			String dotStr = cmd.getOptionValue('d');

			if (dotStr != null && dotStr.length() > 0)
			{
				String [] dotOpts = dotStr.split(",");
				if (dotOpts.length > 0)
				{
					try
					{
						alpha = Double.parseDouble(dotOpts[0]);
					} catch(NumberFormatException nfe)
					{
						System.err.println("The argument specified to the 'dot' option isn't given in a known floatingpoint format!");
						System.exit(-1);
					}
				
					if (!(alpha > 0 && alpha <= 0.99))
					{
						System.err.println("The argument given to the 'dot' option is not in a valid range (between 0 and 0.99)!");
						System.exit(-1);
					}

					if (dotOpts.length > 1)
					{
						try
						{
							dotRootID = new TermID(dotOpts[1]);
						} catch (IllegalArgumentException ex)
						{
							try
							{
								dotRootID = new TermID(Integer.parseInt(dotOpts[1]));
							} catch(NumberFormatException ex2)
							{
								System.err.println("The 2nd argument given to the 'dot' option is no valid GO Term ID format (eighter the \"GO:\" string followed by seven digits or a plain integer)!");
								System.exit(-1);
							}
						}
					}
				}
			}

			/* Annotations */
			boolean createAnnotations = cmd.hasOption('n');
			
			/* Now issue the calculation */
			OntologizerCore controller = new OntologizerCore(arguments);
			
			EnrichedGOTermsResult studySetResult;
			
			while ((studySetResult = controller.calculateNextStudy()) != null)
			{
				/* outfile names are composed of StudySet name, calculation name and correction name */ 
				String outBasename = studySetResult.getStudySet().getName()
						+ "-" + controller.getCalculationName()
						+ "-" + controller.getTestCorrectionName();

				{
					String tableName = "table-" + outBasename + ".txt";
					File outFile = new File(outputDirectoryName,tableName);
					studySetResult.writeTable(outFile);
				}
				
				if (createDOTFile)
				{
					String dotName = "view-" + outBasename + ".dot";
					File outFile = new File(outputDirectoryName,dotName);
					studySetResult.writeDOT(controller.getGoGraph(),outFile,alpha,true,dotRootID);
				}
				
				if (createAnnotations)
				{
					String annoName = "anno-" + outBasename + ".txt";
					File outFile = new File(outputDirectoryName,annoName);
					System.err.println("Writing anno file to " + outFile.getCanonicalPath());
					studySetResult.getStudySet().writeSetWithAnnotations(controller.getGoGraph(),controller.getGoAssociations(),outFile);
				}
			}
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
			System.exit(-1);
		}
	}
}
