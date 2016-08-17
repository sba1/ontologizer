package org.ontologizer.dataprep;

import com.beust.jcommander.JCommander;

/**
 * A simple tool preparing data for (Web) ontologizer.
 *
 * @author Sebastian Bauer
 */
public class DataPrep
{
	public static void main(String[] args)
	{
		DataPrepCLIConfig cliConfig = new DataPrepCLIConfig();
		JCommander jc = new JCommander(cliConfig);

		jc.parse(args);

		jc.setProgramName(DataPrep.class.getSimpleName());
		if (cliConfig.help)
		{
			jc.usage();
			System.exit(0);
		}

	}
}
