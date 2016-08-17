package org.ontologizer.dataprep;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

public class DataPrepCLIConfig
{
	public static class ProperPositiveInteger implements IParameterValidator
	{
		@Override
		public void validate(String name, String value) throws ParameterException
		{
			int n = Integer.parseInt(value);
			if (n < 1)
				throw new ParameterException("Parameter " + name + " should be a proper positive integer(found " + value +")");
		}
	}

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

	@Parameter(names={"--proxy-port"}, description="Port of the proxy that shall be used for http connections.", arity=1, validateWith=ProperPositiveInteger.class)
	public int proxyPort;
}
