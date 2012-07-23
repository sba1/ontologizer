package ontologizer;

import junit.framework.TestCase;

public class SmithWatermanTest extends TestCase
{
	public void test()
	{
		assertEquals(40, SmithWaterman.getScore("test", "test"));
		assertEquals(30, SmithWaterman.getScore("est", "test"));
		assertEquals(20, SmithWaterman.getScore("tst", "test"));
		assertEquals(10, SmithWaterman.getScore("ts", "test"));
	}
}
