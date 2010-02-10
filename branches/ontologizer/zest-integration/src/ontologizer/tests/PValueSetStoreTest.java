package ontologizer.tests;

import ontologizer.statistics.PValue;
import ontologizer.statistics.PvalueSetStore;
import junit.framework.Assert;
import junit.framework.TestCase;

public class PValueSetStoreTest extends TestCase
{

	/*
	 * Test method for 'ontologizer.statistics.PvalueSetStore.PvalueSetStore(int, int)'
	 */
	public void testPvalueSetStore()
	{
		int nSets = 3;
		int setSize = 10;

		// initializing
		PvalueSetStore store = new PvalueSetStore(nSets,setSize);

		// creating random data
		PValue [][] sampledPvals = new PValue[nSets][setSize];
		for (int i=0; i < nSets; i++) {
			for (int j=0; j < setSize; j++) {
				sampledPvals[i][j] = new PValue();
				sampledPvals[i][j].p = Math.random();
				if (sampledPvals[i][j].p > 0.5) {
					sampledPvals[i][j].ignoreAtMTC = true;
				} else {
					sampledPvals[i][j].ignoreAtMTC = false;
				}
			}
		}

		// filling
		for (int i=0; i < nSets; i++) {
			store.add(sampledPvals[i]);
		}

		int count=0;
		for (PValue[] pvals : store) {
			count++;
			Assert.assertTrue("wrong length retrieved Pvalue array!", pvals.length == setSize);
			for (int j=0; j < setSize; j++) {
				if (pvals[j].ignoreAtMTC) {
					Assert.assertTrue("Ignore, we should get 1.0", pvals[j].p == 1.0);
				} else {
					Assert.assertTrue(pvals[j].p <= 0.5);
				}
			}
		}
		Assert.assertTrue(count == 3);
	}

}
