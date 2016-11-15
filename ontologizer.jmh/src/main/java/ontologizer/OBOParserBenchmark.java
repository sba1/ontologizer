package ontologizer;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Warmup;

import ontologizer.ontology.OBOParser;
import ontologizer.ontology.OBOParserException;
import ontologizer.ontology.OBOParserFileInput;

public class OBOParserBenchmark
{
	private static final String obofile = "../../ontologizer.tests/data/gene_ontology.1_2.obo.gz";

	@Benchmark
	@Warmup(iterations=5)
	@Fork(value=1)
	@Measurement(time=2,timeUnit=TimeUnit.SECONDS)
	public void benchmarkOBOParser() throws IOException, OBOParserException
	{
		OBOParser oboParser = new OBOParser(new OBOParserFileInput(obofile));
		oboParser.doParse();
	}
}
