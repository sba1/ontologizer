package sonumina.math.combinatorics;

import org.junit.Assert;
import org.junit.Test;

public class SubsetGeneratorTest
{
	@Test
	public void testSubsetGenerator()
	{
		SubsetGenerator subsetGen = new SubsetGenerator(10,3);

		int no = 0;

		while ((subsetGen.next()) != null)
			no++;
		Assert.assertTrue(no == 176);
	}

}
