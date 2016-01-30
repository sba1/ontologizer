package sonumina.collections;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class FullStringIndexTest
{
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

		int cnt = 0;
		for (Integer i : fsi.contains("A"))
			cnt++;
		assertEquals(3,cnt);

		cnt = 0;
		for (Integer i : fsi.contains("AB"))
			cnt++;
		assertEquals(2,cnt);

		cnt = 0;
		for (Integer i : fsi.contains("AQ"))
			cnt++;
		assertEquals(1,cnt);

		cnt = 0;
		for (Integer i : fsi.contains("Z"))
			cnt++;
		assertEquals(0,cnt);

		fsi.clear();
		cnt = 0;
		for (Integer i : fsi.contains("A"))
			cnt++;
		assertEquals(0,cnt);
	}
}
