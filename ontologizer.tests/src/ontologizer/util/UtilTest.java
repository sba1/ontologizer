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

		assertEquals(0, Util.commonInts());
		assertEquals(a.length, Util.commonInts(a));
		assertEquals(2, Util.commonInts(a, b));
		assertEquals(3, Util.commonInts(a, c));
		assertEquals(0, Util.commonInts(a, d));
		assertEquals(2, Util.commonInts(a, b, c));
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

	@Test
	public void testCommonIntsWithUnion()
	{
		int [] a = new int[]{1,2,3,4,5,6};
		int [] unionCardinality = new int[1];
		int [] b1 = new int[]{1,2};
		int [] b2 = new int[]{3,4};
		int [] b3 = new int[]{5,6};

		assertEquals(6, Util.commonIntsWithUnion(unionCardinality, a));

		assertEquals(6, Util.commonIntsWithUnion(unionCardinality, a, b1, b2, b3));
		assertEquals(6, unionCardinality[0]);

		assertEquals(4, Util.commonIntsWithUnion(unionCardinality, a, b1, b2));
		assertEquals(4, unionCardinality[0]);

		assertEquals(6, Util.commonIntsWithUnion(unionCardinality, a, b1, b2, b3, new int[]{7,8}));
		assertEquals(8, unionCardinality[0]);
	}
}
