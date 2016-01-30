package ontologizer.statistics.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import ontologizer.statistics.Hypergeometric;

public class HypergeometricTest
{
	private Hypergeometric hyper = new Hypergeometric();

	/*
	 * Test method for 'ontologizer.Hypergeometric.dhyper(int, int, int, int)'
	 */
	@Test
	public void testDhyper()
	{
		assertEquals(0.268,hyper.dhyper(4,45,20,10),0.001);
		assertEquals(1,hyper.dhyper(10,10,10,10),0.00001);
	}

	@Test
	public void testPhyper()
	{
		double result = hyper.phyper(2,1526,4,190,false);

		assertTrue(result > 0.0069 && result < 0.0070);
		assertTrue((hyper.phyper(22,1526,40,190,false) + hyper.phyper(22,1526,40,190,true)) == 1);
		assertTrue((hyper.phyper(3,1526,40,190,false) + hyper.phyper(3,1526,40,190,true)) == 1);

		/*
		 * checking unreasonable numbers
		 */
		// drawing more white than available
		assertTrue(hyper.phyper(4,8,3,6,false) == 0);
		// drawing more white than drawn in total
		assertTrue(hyper.phyper(4,8,5,3,false) == 0);
		// drawing more white than available in total
		assertTrue(hyper.phyper(10,8,5,12,false) == 0);
	}
}
