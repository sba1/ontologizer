package ontologizer.go;

import org.junit.Assert;
import org.junit.Test;


public class PrefixPoolTest
{
	@Test
	public void testPrefix()
	{
		PrefixPool pp = new PrefixPool();
		Prefix ref = pp.map(new Prefix("GO:"));
		Prefix n = new Prefix("GO:");
		Assert.assertNotSame(ref,n);
		Assert.assertSame(ref,pp.map(n));
	}
}
