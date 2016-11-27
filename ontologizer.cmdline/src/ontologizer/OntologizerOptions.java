package ontologizer;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import ontologizer.calculation.CalculationRegistry;
import ontologizer.statistics.IResampling;
import ontologizer.statistics.TestCorrectionRegistry;

public class OntologizerOptions
{
	private String [] calculations;
	private String [] mtcs;
	private Options options;

	public static final String MCMC_STEPS = "mcmcSteps";
	public static final String MAX_ALPHA = "maxAlpha";
	public static final String MAX_BETA = "maxBeta";

	public Options options()
	{
		return options;
	}

	public String [] mtcs()
	{
		return mtcs;
	}

	public String [] calculations()
	{
		return calculations;
	}

	public static OntologizerOptions create()
	{
		/* Build up the calculation string to show it within the help description */
		String calculations[] = CalculationRegistry.getAllRegistered();
		StringBuilder calHelpStrBuilder = new StringBuilder();
		calHelpStrBuilder.append("Specifies the calculation method to use. Possible values for name are: ");

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

		options.addOption(new Option("h","help",false,"Shows this help"));
		options.addOption(Option.builder("g").longOpt("go").argName("file").hasArg().desc(
				"File containig GO terminology and structure (.obo format). Required"
				).build());
		options.addOption(Option.builder("a").longOpt("association").argName("file").hasArg().desc(
				"File containing associations from genes to GO terms. Required"
				).build());
		options.addOption(Option.builder("p").longOpt("population").argName("file").hasArg().desc(
				"File containing genes within the population. Required"
				).build());
		options.addOption(Option.builder("s").longOpt("studyset").argName("path").hasArg().desc(
				"Path to a file of a study set or to a directory containing study set files. Required"
				).build());
		options.addOption(new Option("i","ignore",false,"Ignore genes within the calculation to which no association exists."));
		options.addOption(Option.builder("c").longOpt("calculation").argName("name").hasArg().desc(calHelpString).build());
		options.addOption(new Option("m","mtc",true,mtcHelpString));
		options.addOption(Option.builder("d").longOpt("dot").argName("[thrsh[,id]|id]").hasArg().optionalArg(true).desc(
				"For every study set analysis write out an additional .dot file (GraphViz) containing "+
				"the graph that is induced by interesting nodes. The optional argument thrsh must be in range between 0 and 1 "+
				"and it specifies the threshold used to identify interesting nodes (defaults to 0.05). "+
				"The GO term identifier id restricts the output to the subgraph originating at id."
				).build());
		options.addOption(new Option("n","annotation",false,"Create an additional file per study set which contains the annotations."));
		options.addOption(new Option("f","filter",true,"Filter the gene names by appling rules in a given file (currently only mapping supported)."));
		options.addOption(new Option("o","outdir",true,"Specifies the directory in which the results will be placed."));

		options.addOption(Option.builder().longOpt(MCMC_STEPS).argName("steps").hasArg(true).desc("Number of sample steps for MCMC based approaches like MSGA. Defaults to " + GlobalPreferences.getMcmcSteps() + ".").build());
		options.addOption(Option.builder().longOpt(MAX_ALPHA).argName("alpha").hasArg(true).desc("Upper bound for alpha that is inferred in MGSA. Defaults to " + GlobalPreferences.getAlpha() + ".").build());
		options.addOption(Option.builder().longOpt(MAX_BETA).argName("beta").hasArg(true).desc("Upper bound for beta that is inferred in MGSA. Defaults to " + GlobalPreferences.getBeta() + ".").build());
		if (resamplingBasedMTCsExists) {
			options.addOption(new Option("r","resamplingsteps", true, "Specifies the number of steps used in resampling based MTCs"));
			options.addOption(new Option("t","sizetolerance", true, "Specifies the percentage at which the actual study set size and " +
					"the size of the resampled study sets are allowed to differ"));
		}
		options.addOption(new Option("v","version",false,"Shows version information and exits"));

		OntologizerOptions opts = new OntologizerOptions();
		opts.calculations = calculations;
		opts.options = options;
		opts.mtcs = mtcs;
		return opts;
	}
}
