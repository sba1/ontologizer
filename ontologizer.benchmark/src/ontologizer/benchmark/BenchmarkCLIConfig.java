package ontologizer.benchmark;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;


class ProperPositiveInteger implements IParameterValidator
{
	@Override
	public void validate(String name, String value) throws ParameterException
	{
		int n = Integer.parseInt(value);
		if (n < 1)
			throw new ParameterException("Parameter " + name + " should be a proper positive integer(found " + value +")");
	}
}

/**
 * The command line interface.
 *
 * @author Sebastian Bauer
 */
public class BenchmarkCLIConfig
{
	@Parameter(names={"--term-combinations-per-run"}, description="How many term combinations per should be drawn per run", validateWith=ProperPositiveInteger.class)
	public int termCombinationsPerRun = 300;

	@Parameter(names={"--min-terms-per-combination"}, description="The minimum number of distinct terms per combination", validateWith=ProperPositiveInteger.class)
	public int minTerms = 1;

	@Parameter(names=("--max-terms-per-combination"), description="The maximum number of distinct terms per combination", validateWith=ProperPositiveInteger.class)
	public int maxTerms = 5;

	@Parameter(names={"--help"},description="Shows this help.",help=true)
	public boolean help;
}
