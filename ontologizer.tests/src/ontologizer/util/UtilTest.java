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

	@Test
	public void testUnion()
	{
		int [] a = new int[]{1,2,3,4,5,6};
		int [] b = new int[]{4,5};
		int [] c = new int[]{4,5,6,7};
		int [] d = new int[]{7,8,9};

		Assert.assertArrayEquals(a, Util.union(a, a));
		Assert.assertArrayEquals(b, Util.union(b, b));
		Assert.assertArrayEquals(c, Util.union(c, c));
		Assert.assertArrayEquals(d, Util.union(d, d));

		Assert.assertArrayEquals(a, Util.union(new int[]{}, a));
		Assert.assertArrayEquals(a, Util.union(a, new int[]{}));
		Assert.assertArrayEquals(a, Util.union(a, b));

		Assert.assertArrayEquals(new int[]{1,2,3,4,5,6,7}, Util.union(a, c));
		Assert.assertArrayEquals(new int[]{1,2,3,4,5,6,7,8,9}, Util.union(a, d));

		Assert.assertArrayEquals(new int[]{4,5,6,7}, Util.union(b, c));

		Assert.assertArrayEquals(new int[]{4,5,6,7,8,9}, Util.union(c, d));
	}
}
