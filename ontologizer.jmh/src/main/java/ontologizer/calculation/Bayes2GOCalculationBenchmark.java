package ontologizer.calculation;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Warmup;

import ontologizer.io.obo.OBOParserException;

public class Bayes2GOCalculationBenchmark
{
	@Benchmark
	@Warmup(iterations=5)
	@Fork(value=1)
	@Measurement(time=2,timeUnit=TimeUnit.SECONDS)
	public void benchmark() throws IOException, OBOParserException
	{
	}
}
