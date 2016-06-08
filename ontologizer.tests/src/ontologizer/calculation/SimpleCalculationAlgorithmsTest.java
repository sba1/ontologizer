package ontologizer.calculation;

import static ontologizer.calculation.CalculationTestUtils.assertResultEquals;
import static ontologizer.calculation.CalculationTestUtils.performTestCalculation;

import org.junit.Test;

public class SimpleCalculationAlgorithmsTest
{
	@Test
	public void whetherTFTWorks()
	{
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

		assertResultEquals(expected, TermForTermGOTermProperties.class, r);
	}

	@Test
	public void whetherPCUWorks()
	{
		EnrichedGOTermsResult r = performTestCalculation(new ParentChildCalculation());

		/* FIXME: Verify manually first */
		Expected [] expected = new Expected[]
		{
			new Expected("GO:0000001", 500, 57, 1.0),
			new Expected("GO:0000002", 444, 57, 7.401061083168661E-4),
			new Expected("GO:0000003", 383, 31, 0.9999801590716945),
			new Expected("GO:0000004",  57, 57, 2.18177417463053E-73),
			new Expected("GO:0000005", 338, 25, 0.9999999973487158),
			new Expected("GO:0000006", 333, 26, 0.9999863944662216),
			new Expected("GO:0000007", 291, 22, 0.6585006334613194),
			new Expected("GO:0000008",  63,  5, 0.5386240751219415),
			new Expected("GO:0000009", 179, 10, 0.9654322584198002),
			new Expected("GO:0000010",  66,  5, 0.2862835934348825),
			new Expected("GO:0000011",  65,  2, 0.9320736961939712),
		};

		assertResultEquals(expected, ParentChildGOTermProperties.class, r);
	}

	@Test
	public void whetherPCIWorks()
	{
		EnrichedGOTermsResult r = performTestCalculation(new ParentChildCutCalculation());

		/* FIXME: Verify manually first */
		Expected [] expected = new Expected[]
		{
			new Expected("GO:0000001", 500, 57, 1.0                 ),
			new Expected("GO:0000002", 444, 57, 7.401061083168661E-4),
			new Expected("GO:0000003", 383, 31, 0.9999801590716945  ),
			new Expected("GO:0000004",  57, 57, 2.18177417463053E-73),
			new Expected("GO:0000005", 338, 25, 0.9999999973487158  ),
			new Expected("GO:0000006", 333, 26, 0.9987172393112891  ),
			new Expected("GO:0000007", 291, 22, 0.8552293234841417  ),
			new Expected("GO:0000008",  63,  5, 0.5386240751219415  ),
			new Expected("GO:0000009", 179, 10, 0.9654322584198002  ),
			new Expected("GO:0000010",  66,  5, 0.2862835934348825  ),
			new Expected("GO:0000011",  65,  2, 0.9320736961939712  ),
		};

		assertResultEquals(expected, ParentChildGOTermProperties.class, r);
	}
}
