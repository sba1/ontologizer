package ontologizer.sampling;

import java.util.HashSet;

import junit.framework.Assert;
import junit.framework.TestCase;

public class WeightedUrnTest extends TestCase
{

	public void testWeightedUrn()
	{
		HashSet<String> enumeratorSet = new HashSet<String>();
		String[] enumeratorObjs = { "a","b","c" };
		for (String entry : enumeratorObjs) {
			enumeratorSet.add(entry);
		}

		HashSet<String> denominatorSet = new HashSet<String>();
		String[] denominatorObjs = { "1","2","3","4","5" };

		for (String entry : denominatorObjs) {
			denominatorSet.add(entry);
		}

		double [] ratiosToTest = { 1.0, 2.0, 4.0, 10.0};
		for (double ratio : ratiosToTest) {
			WeightedUrn<String> testUrn = new WeightedUrn<String>(enumeratorSet,denominatorSet,ratio);
			HashSet<String> sampledSet;

			sampledSet = testUrn.sample(3);
			Assert.assertTrue(sampledSet.size() == 3);

			sampledSet = testUrn.sample(8);
			//Assert.assertTrue(sampledSet.size() == 8);
			//Assert.assertTrue(sampledSet.containsAll(enumeratorSet));
			//Assert.assertTrue(sampledSet.containsAll(denominatorSet));
		}
	}

	public void testWeightedUrnMini()
	{
		HashSet<String> enumeratorSet = new HashSet<String>();
		String[] enumeratorObjs = { "a" };
		for (String entry : enumeratorObjs) {
			enumeratorSet.add(entry);
		}

		HashSet<String> denominatorSet = new HashSet<String>();

		double [] ratiosToTest = { 1.0, 2.0, 4.0, 10.0};

		for (double ratio : ratiosToTest) {
			WeightedUrn<String> testUrn = new WeightedUrn<String>(enumeratorSet,denominatorSet,ratio);
			HashSet<String> sampledSet = new HashSet<String>();

			sampledSet = testUrn.sample(1);
			Assert.assertTrue(sampledSet.size() == 1);

			sampledSet = testUrn.sample(1);
			Assert.assertTrue(sampledSet.size() == 1);
			Assert.assertTrue(sampledSet.containsAll(enumeratorSet));
			Assert.assertTrue(sampledSet.containsAll(denominatorSet));
		}
	}

}
