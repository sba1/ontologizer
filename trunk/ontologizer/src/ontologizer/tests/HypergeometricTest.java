package ontologizer.tests;

import ontologizer.Hypergeometric;
import junit.framework.Assert;
import junit.framework.TestCase;

public class HypergeometricTest extends TestCase
{
	private Hypergeometric hyper = new Hypergeometric();

	/*
	 * Test method for 'ontologizer.Hypergeometric.dhyper(int, int, int, int)'
	 */
	public void testDhyper()
	{
		double result = hyper.dhyper(4,45,20,10);

		Assert.assertTrue(result > 0.268 && result < 0.269);
		Assert.assertTrue(hyper.dhyper(10,10,10,10) == 1);
	}

	public void testPhyper()
	{
		double result = hyper.phyper(2,1526,4,190,false);

		Assert.assertTrue(result > 0.0069 && result < 0.0070);
		Assert.assertTrue((hyper.phyper(22,1526,40,190,false) + hyper.phyper(22,1526,40,190,true)) == 1);
		Assert.assertTrue((hyper.phyper(3,1526,40,190,false) + hyper.phyper(3,1526,40,190,true)) == 1);
		
		/* 
		 * checking unreasonable numbers
		 */
		// drawing more white than available
		Assert.assertTrue(hyper.phyper(4,8,3,6,false) == 0);
		// drawing more white than drawn in total
		Assert.assertTrue(hyper.phyper(4,8,5,3,false) == 0);
		// drawing more white than available in total
		Assert.assertTrue(hyper.phyper(10,8,5,12,false) == 0);
	}
}
