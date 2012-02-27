package ontologizer.go;

import junit.framework.Assert;
import junit.framework.TestCase;

public class PrefixPoolTest extends TestCase
{
	public void testPrefix()
	{
		PrefixPool pp = new PrefixPool();
		Prefix ref = pp.map(new Prefix("GO:"));
		Prefix n = new Prefix("GO:");
		Assert.assertNotSame(ref,n);
		Assert.assertSame(ref,pp.map(n));
	}
}
