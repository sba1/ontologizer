package ontologizer.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

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

		assertEquals(2, Util.commonInts(a, b));
		assertEquals(3, Util.commonInts(a, c));
		assertEquals(0, Util.commonInts(a, d));
	}

	@Test
	public void testUnion()
	{
		int [] a = new int[]{1,2,3,4,5,6};
		int [] b = new int[]{4,5};
		int [] c = new int[]{4,5,6,7};
		int [] d = new int[]{7,8,9};

		assertArrayEquals(a, Util.union(a, a));
		assertArrayEquals(b, Util.union(b, b));
		assertArrayEquals(c, Util.union(c, c));
		assertArrayEquals(d, Util.union(d, d));

		assertArrayEquals(a, Util.union(new int[]{}, a));
		assertArrayEquals(a, Util.union(a, new int[]{}));
		assertArrayEquals(a, Util.union(a, b));

		assertArrayEquals(new int[]{1,2,3,4,5,6,7}, Util.union(a, c));
		assertArrayEquals(new int[]{1,2,3,4,5,6,7,8,9}, Util.union(a, d));

		assertArrayEquals(new int[]{4,5,6,7}, Util.union(b, c));

		assertArrayEquals(new int[]{4,5,6,7,8,9}, Util.union(c, d));
	}
}
