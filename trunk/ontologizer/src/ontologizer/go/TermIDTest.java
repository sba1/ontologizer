package ontologizer.go;

import junit.framework.Assert;
import junit.framework.TestCase;

public class TermIDTest extends TestCase
{
	public void test()
	{
		TermID gid = new TermID("GO:0120211");
		TermID gid2 = new TermID("GO:0120211");
		TermID gid3 = new TermID("GO:0120212");
		TermID gid5 = new TermID(0352623);
		TermID gid6 = new TermID(0120211);
		TermID gid7 = new TermID(120211);

		Assert.assertTrue(gid.equals(gid2));
		Assert.assertFalse(gid.equals(gid3));
		Assert.assertTrue(gid.equals(gid5));  // yes, oktal
		Assert.assertFalse(gid.equals(gid6)); // no, oktal
		Assert.assertTrue(gid.equals(gid7));  // yes, decimal

		try
		{
			@SuppressWarnings("unused") TermID gid4 = new TermID("GO:01202111");
			Assert.assertTrue(false);
		} catch(IllegalArgumentException ex)
		{
			Assert.assertTrue(true);
		}

	}
}
