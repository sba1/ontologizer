package sonumina.collections;

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

import org.junit.Test;

public class ReferencePoolTest
{
	@Test
	public void testWithInteger()
	{
		ReferencePool<Integer> integerPool = new ReferencePool<Integer>();
		Integer ref = integerPool.map(new Integer(10));
		Integer n = new Integer(10);
		assertNotSame(ref, n);
		assertSame(ref, integerPool.map(n));
	}
}
