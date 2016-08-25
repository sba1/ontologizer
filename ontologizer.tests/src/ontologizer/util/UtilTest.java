package ontologizer.util;

import org.junit.Assert;
import org.junit.Test;

public class UtilTest
{
	@Test
	public void testCommonInts()
	{
		int [] a = new int[]{1,2,3,4,5,6};
		int [] b = new int[]{4,5};
		int [] c = new int[]{4,5,6,7};
		int [] d = new int[]{7,8,9};

		Assert.assertEquals(2, Util.commonInts(a, b));
		Assert.assertEquals(3, Util.commonInts(a, c));
		Assert.assertEquals(0, Util.commonInts(a, d));
	}
}
