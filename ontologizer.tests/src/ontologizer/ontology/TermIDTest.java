package ontologizer.ontology;

import org.junit.Assert;
import org.junit.Test;

import ontologizer.ontology.Prefix;
import ontologizer.ontology.PrefixPool;
import ontologizer.ontology.TermID;
import ontologizer.types.ByteString;


public class TermIDTest
{
	@SuppressWarnings("deprecation")
	@Test
	public void test()
	{
		TermID gid = new TermID("GO:0120211");
		TermID gid2 = new TermID("GO:0120211");
		TermID gid3 = new TermID("GO:0120212");
		TermID gid5 = new TermID(0352623);
		TermID gid6 = new TermID(0120211);
		TermID gid7 = new TermID(120211);
		TermID gid8 = new TermID("HPO:0120211");
		TermID gid9 = new TermID(new Prefix("GO"),120211);

		Assert.assertTrue(gid.equals(gid2));
		Assert.assertFalse(gid.equals(gid3));
		Assert.assertTrue(gid.equals(gid5));  // yes, oktal
		Assert.assertFalse(gid.equals(gid6)); // no, oktal
		Assert.assertTrue(gid.equals(gid7));  // yes, decimal
		Assert.assertFalse(gid.equals(gid8)); // no, different prefix
		Assert.assertTrue(gid.equals(gid9));  // yes, uses explicit prefix construction
	}

	@Test
	public void testByte()
	{
		byte [] t1 = "GO:0120211".getBytes();
		TermID gid1 = new TermID(t1,0,t1.length,null);
		Assert.assertEquals("GO:0120211",gid1.toString());

		byte [] t2 = "xxxGO:0120211xxxx".getBytes();
		TermID gid2 = new TermID(t2,3,10,null);
		Assert.assertEquals("GO:0120211",gid2.toString());
	}

	@Test
	public void testWithPrefix()
	{
		PrefixPool pool = new PrefixPool();
		TermID tid = new TermID("HPO:0000100",pool);
		TermID tid2 = new TermID("HPO:0001001",pool);
		Assert.assertSame(tid.getPrefix(), tid2.getPrefix());
	}

	@Test
	public void testToByteString()
	{
		TermID gid = new TermID("GO:0120211");
		Assert.assertEquals(new ByteString("GO:0120211"), gid.toByteString());

		TermID hid = new TermID("HPO:0000001");
		Assert.assertEquals(new ByteString("HPO:0000001"), hid.toByteString());

	}
}
