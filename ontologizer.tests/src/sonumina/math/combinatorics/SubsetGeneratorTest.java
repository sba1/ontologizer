package sonumina.math.combinatorics;

import junit.framework.Assert;
import junit.framework.TestCase;


public class SubsetGeneratorTest extends TestCase
{

	public void testSubsetGenerator()
	{
		SubsetGenerator subsetGen = new SubsetGenerator(10,3);

		int no = 0;

		while ((subsetGen.next()) != null)
			no++;
		Assert.assertTrue(no == 176);
	}

}
