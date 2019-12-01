package ontologizer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import ontologizer.calculation.EnrichedGOTermsResult;
import ontologizer.calculation.EnrichedGOTermsTableWriter;
import ontologizer.io.obo.OBOParserException;
import ontologizer.ontology.TermID;

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
	 * @see OntologizerCore
	 */
	public static void main(String[] args)
	{
		String commandName = "java -jar Ontologizer.jar";

		// TODO: Also ask "sun.java.command"
		commandName = System.getProperty("ontologizer.commandName", commandName);

		OntologizerCore.Arguments arguments = new OntologizerCore.Arguments();

		try
		{
			OntologizerOptions ontologizerOptions = OntologizerOptions.create();
			Options options = ontologizerOptions.options();
			CommandLineParser parser = new DefaultParser();
			CommandLine cmd = parser.parse(options,args);
			if (cmd.hasOption("h"))
			{
				HelpFormatter formatter = new HelpFormatter();
				int width;
				try
				{
					Process p = Runtime.getRuntime().exec(new String[] {"sh", "-c", "tput cols 2> /dev/tty" });
					Scanner scanner = new Scanner(p.getInputStream());
					width = scanner.nextInt();
					scanner.close();
					if (width < 50)
					{
						width = 50;
					}
				} catch (Exception ex)
				{
					width = 100;
				}
				formatter.printHelp(width, commandName, "Analyze High-Throughput Biological Data Using Gene Ontology", options, "", true);
				System.exit(0);
			}

			if (cmd.hasOption("v"))
			{
				System.out.println("Ontologizer " + BuildInfo.getVersion() + " (Build: " + BuildInfo.getBuildString() + ")");
				System.out.println();
				System.out.println("Copyright (C) " + BuildInfo.getCopyright() + " Ontologizer Development Team.");
				System.exit(0);
			}

			String [] obo = getRequiredOptionValue(cmd,'g').split(",");
			arguments.goTermsOBOFile = obo[0];
			if (obo.length > 1)
			{
				arguments.subontology = obo[1];
			}

			arguments.associationFile = getRequiredOptionValue(cmd,'a');
			arguments.populationFile = getRequiredOptionValue(cmd,'p');
			arguments.studySet = getRequiredOptionValue(cmd,'s');
			arguments.calculationName = cmd.getOptionValue('c');
			arguments.correctionName = cmd.getOptionValue('m');
			arguments.filterOutUnannotatedGenes = cmd.hasOption('i');
			arguments.filterFile = cmd.getOptionValue('f');
			if (cmd.hasOption(OntologizerOptions.MCMC_STEPS))
			{
				int mcmcSteps;

				try
				{
					mcmcSteps = Integer.parseInt(cmd.getOptionValue(OntologizerOptions.MCMC_STEPS));
					if (mcmcSteps < 10000)
					{
						throw new Exception();
					}
					GlobalPreferences.setMcmcSteps(mcmcSteps);
				} catch (Exception e)
				{
					System.err.println("The --" + OntologizerOptions.MCMC_STEPS + " argument needs to be an integer larger than 10000.");
					System.exit(-1);
				}
			}
			arguments.mcmcSteps = GlobalPreferences.getMcmcSteps();

			if (cmd.hasOption(OntologizerOptions.MAX_ALPHA))
			{
				double alphaMax;

				try
				{
					alphaMax = Double.parseDouble(cmd.getOptionValue(OntologizerOptions.MAX_ALPHA));
					if (alphaMax < 0 || alphaMax > 1)
					{
						throw new Exception();
					}
					GlobalPreferences.setUpperAlpha(alphaMax);
				} catch (Exception e)
				{
					System.err.println("The --" + OntologizerOptions.MAX_ALPHA + " argument needs to be a value between 0 and 1.");
					System.exit(-1);
				}
			}

			if (cmd.hasOption(OntologizerOptions.MAX_BETA))
			{
				double betaMax;

				try
				{
					betaMax = Double.parseDouble(cmd.getOptionValue(OntologizerOptions.MAX_BETA));
					if (betaMax < 0 || betaMax > 1)
					{
						throw new Exception();
					}
					GlobalPreferences.setUpperBeta(betaMax);
				} catch (Exception e)
				{
					System.err.println("The --" + OntologizerOptions.MAX_BETA + " argument needs to be a value between 0 and 1.");
					System.exit(-1);
				}
			}

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

			/* Testing availability of calculation method */
			if (arguments.calculationName != null)
			{
				boolean found = false;
				for (int i=0;i<ontologizerOptions.calculations().length;i++)
				{
					if (arguments.calculationName.equals(ontologizerOptions.calculations()[i]))
					{
						found = true;
						break;
					}
				}

				if (!found)
				{
					String addInfo = " Did you mean perhaps \"" + SmithWaterman.findMostSimilar(ontologizerOptions.calculations(), arguments.calculationName) + "\"?";
					System.err.println("Given calculation method " + arguments.calculationName + " wasn't found!" + addInfo);
					System.exit(-1);
				}
			}

			/* Testing availability of mtc method */
			if (arguments.correctionName != null)
			{
				boolean found = false;
				for (int i=0;i<ontologizerOptions.mtcs().length;i++)
				{
					if (arguments.correctionName.equals(ontologizerOptions.mtcs()[i]))
					{
						found = true;
						break;
					}
				}

				if (!found)
				{
					String addInfo = " Did you mean perhaps \"" + SmithWaterman.findMostSimilar(ontologizerOptions.mtcs(), arguments.correctionName) + "\"?";
					System.err.println("Given MTC method " + arguments.correctionName + " wasn't found!" + addInfo);
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
								dotRootID = new TermID(TermID.DEFAULT_PREFIX, Integer.parseInt(dotOpts[1]));
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
					EnrichedGOTermsTableWriter.writeTable(outFile, studySetResult);
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
