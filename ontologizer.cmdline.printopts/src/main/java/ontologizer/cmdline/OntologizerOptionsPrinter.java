package ontologizer.cmdline;

import org.apache.commons.cli.HelpFormatter;

import ontologizer.OntologizerOptions;

public class OntologizerOptionsPrinter
{
	public static void main(String[] args)
	{
		OntologizerOptions opts = OntologizerOptions.create();
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp(80, "java -jar Ontologizer.jar", "Analyze High-Throughput Biological Data Using Gene Ontology", opts.options(), "", true);
	}
}
