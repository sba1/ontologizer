package sonumina.collections;

import junit.framework.Assert;
import junit.framework.TestCase;

public class FullStringIndexTest extends TestCase
{
	public void test1()
	{
		FullStringIndex<Integer> fsi = new FullStringIndex<Integer>();
		fsi.add("ABCD", 1);
		fsi.add("GDUJ", 2);
		fsi.add("GAQM", 3);
		fsi.add("GDUJK", 4);
		fsi.add("ABCD", 5);
		Assert.assertEquals(5,fsi.size());
	}
}
