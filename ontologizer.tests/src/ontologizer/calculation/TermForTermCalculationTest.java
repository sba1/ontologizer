package ontologizer.calculation;

import static ontologizer.calculation.CalculationTestUtils.performTestCalculation;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import ontologizer.ontology.TermID;

public class TermForTermCalculationTest
{
	@Test
	public void whetherTFTWorks()
	{
		class Expected
		{
			public String id;
			public int pop;
			public int study;
			public double p;
			public Expected(String id, int pop, int study, double p)
			{
				this.id = id;
				this.pop = pop;
				this.study = study;
				this.p = p;
			}
		}
		EnrichedGOTermsResult r = performTestCalculation(new TermForTermCalculation());

		Expected [] expected = new Expected[]
		{
			new Expected("GO:0000001", 500, 57, 1.0),
			new Expected("GO:0000002", 444, 57, 7.4010610e-4),
			new Expected("GO:0000003", 383, 31, 0.9999801590716945),
			new Expected("GO:0000004",  57, 57, 1.6147443e-76),
			new Expected("GO:0000005", 338, 25, 0.9999779361656085),
			new Expected("GO:0000006", 333, 26, 0.9998514855899283),
			new Expected("GO:0000007", 291, 22, 0.9995316242235475),
			new Expected("GO:0000008",  63,  5, 0.8758072),
			new Expected("GO:0000009", 179, 10, 0.9996126881087588),
			new Expected("GO:0000010",  66,  5, 0.9011589801093324),
			new Expected("GO:0000011",  65,  2, 0.9977121167095124),
		};

		assertEquals(expected.length, r.getSize());

		for (int i = 0; i < expected.length; i++)
		{
			AbstractGOTermProperties p = r.getGOTermProperties(new TermID(expected[i].id));
			assertEquals(p.getClass(), TermForTermGOTermProperties.class);
			assertEquals("Entry " + i, expected[i].pop, p.annotatedPopulationGenes);
			assertEquals("Entry " + i, expected[i].study, p.annotatedStudyGenes);
			assertEquals("Entry " + i, expected[i].p, p.p, 1e-5);
		}
	}
}
