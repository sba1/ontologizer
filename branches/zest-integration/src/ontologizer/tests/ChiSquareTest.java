package ontologizer.tests;

import ontologizer.chisquare.ChiSquare;
import ontologizer.chisquare.ChiSquareIncompatibleCountsException;
import junit.framework.Assert;
import junit.framework.TestCase;

public class ChiSquareTest extends TestCase
{


	/*
	 * Test method for 'ontologizer.chisquare.ChiSquare.ChiSquare()'
	 */
	public void testChiSquare()
	{
		ChiSquare chisq = new ChiSquare();
		Assert.assertTrue(chisq.df() == -1);
		Assert.assertTrue(chisq.chiSquare() == 0.0);
	}

	/*
	 * Test method for 'ontologizer.chisquare.ChiSquare.addColour(int, int)'
	 */
	public void testAddColour()
	{
		ChiSquare chisq = new ChiSquare();

		boolean exceptionThrown = false;

		try
		{
			chisq.addCounts(4,2);
		} catch (ChiSquareIncompatibleCountsException e)
		{
			exceptionThrown = true;
		}
		Assert.assertFalse("chisq.addColour shouldn't throw here!", exceptionThrown);
		exceptionThrown = false;

		try
		{
			chisq.addCounts(6,3);
		} catch (ChiSquareIncompatibleCountsException e)
		{
			exceptionThrown = true;
		}
		Assert.assertFalse("chisq.addColour shouldn't throw here!", exceptionThrown);
		exceptionThrown = false;
		Assert.assertTrue(chisq.df() == 1);
		Assert.assertTrue(chisq.chiSquare() == 0.0);

		try
		{
			chisq.addCounts(5,1);
		} catch (ChiSquareIncompatibleCountsException e)
		{
			exceptionThrown = true;
		}
		Assert.assertFalse("chisq.addColour shouldn't throw here!", exceptionThrown);
		exceptionThrown = false;
		Assert.assertTrue(chisq.chiSquare() == 0.75);



		try
		{
			chisq.addCounts(6,7);
		} catch (ChiSquareIncompatibleCountsException e)
		{
			exceptionThrown = true;
		}
		Assert.assertTrue("chisq.addColour should throw here!", exceptionThrown);
		exceptionThrown = false;

		// chiSquare should still be the same
		Assert.assertTrue(chisq.chiSquare() == 0.75);
	}

}
