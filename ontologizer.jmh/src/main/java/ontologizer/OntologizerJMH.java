package ontologizer;

import java.io.IOException;

import org.openjdk.jmh.annotations.Benchmark;

import ontologizer.ontology.OBOParser;
import ontologizer.ontology.OBOParserException;
import ontologizer.ontology.OBOParserFileInput;

public class OntologizerJMH
{
	private static final String obofile = "../../ontologizer.tests/data/gene_ontology.1_2.obo.gz";

	@Benchmark
	public void benchmarkOBOParser() throws IOException, OBOParserException
	{
		OBOParser oboParser = new OBOParser(new OBOParserFileInput(obofile));
		oboParser.doParse();
	}
}
