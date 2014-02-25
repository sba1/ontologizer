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

	@Parameter(names={"-o", "--obo"}, description="The obo file that shall be used for running the benchmark. For instance, " +
				"\"http://www.geneontology.org/ontology/gene_ontology_edit.obo\"", arity=1, required=true)
	public String obo;

	@Parameter(names={"-a", "--association"}, description="Name of the file containing associations from items to terms. For instance, "+
				"\"http://cvsweb.geneontology.org/cgi-bin/cvsweb.cgi/go/gene-associations/gene_association.fb.gz?rev=HEAD\"", arity=1, required=true)
	public String assoc;

	@Parameter(names={"--proxy"}, description="Name of the proxy that shall be used for http connections.", arity=1)
	public String proxy;

	@Parameter(names={"--proxyPort"}, description="Port of the proxy that shall be used for http connections.", arity=1, validateWith=ProperPositiveInteger.class)
	public int proxyPort;

	@Parameter(names={"--output-dir"}, description="Folder where all the output is stored. Defaults to the current directory.", arity=1)
	public String outputDirectory;
}
