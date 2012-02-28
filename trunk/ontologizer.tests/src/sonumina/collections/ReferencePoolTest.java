package sonumina.collections;

import junit.framework.Assert;
import junit.framework.TestCase;

public class ReferencePoolTest extends TestCase
{
	public void testWithInteger()
	{
		ReferencePool<Integer> integerPool = new ReferencePool<Integer>();
		Integer ref = integerPool.map(new Integer(10));
		Integer n = new Integer(10);
		Assert.assertNotSame(ref, n);
		Assert.assertSame(ref, integerPool.map(n));
	}
}
