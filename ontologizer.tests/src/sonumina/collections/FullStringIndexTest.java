package sonumina.collections;

import static org.junit.Assert.assertEquals;

import java.util.Iterator;

import org.junit.Test;

public class FullStringIndexTest
{
	/**
	 * Simple util to get a number of items of an iterable.
	 *
	 * @param iterable
	 * @return
	 */
	private static <T> int size(Iterable<T> iterable)
	{
		int count = 0;
		for (Iterator<T> iter = iterable.iterator(); iter.hasNext(); iter.next())
			count++;
		return count;
	}

	@Test
	public void test1()
	{
		FullStringIndex<Integer> fsi = new FullStringIndex<Integer>();
		fsi.add("ABCD", 1);
		fsi.add("GDUJ", 2);
		fsi.add("GAQM", 3);
		fsi.add("GDUJK", 4);
		fsi.add("ABCD", 5);
		assertEquals(5,fsi.size());

		assertEquals(3, size(fsi.contains("A")));
		assertEquals(2, size(fsi.contains("AB")));
		assertEquals(1, size(fsi.contains("AQ")));
		assertEquals(0, size(fsi.contains("Z")));

		fsi.clear();

		assertEquals(0, size(fsi.contains("A")));
	}
}
