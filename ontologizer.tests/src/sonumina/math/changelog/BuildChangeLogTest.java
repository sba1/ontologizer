package sonumina.math.changelog;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import sonumina.util.changelog.BuildChangeLog;
import sonumina.util.changelog.Change;

public class BuildChangeLogTest
{
	@Test
	public void testBasicLog()
	{
		String string = "------------------------------------------------------------------------\n" +
						"r716 | sba | 2012-02-08 16:30:24 +0100 (Wed, 08 Feb 2012) | 1 line\n" +
						"\n" +
						"Added support for arbitrary node paths.\n" +
						"$foruser$Test\n"+
						"------------------------------------------------------------------------\n" +
						"r715 | sba | 2012-02-08 14:51:00 +0100 (Wed, 08 Feb 2012) | 1 line\n" +
						"\n" +
						"Removed generateShuffledStudySets().\n" +
						"$foruser$Test2\n"+
						"------------------------------------------------------------------------\n" +
						"r714 | sba | 2012-01-25 14:01:46 +0100 (Wed, 25 Jan 2012) | 1 line\n" +
						"\n" +
						"Added labels to the chart.\n" +
						"$foruser$Test3\n"+
						"------------------------------------------------------------------------\n";
		Change [] result = BuildChangeLog.process(string);
		assertEquals(3,result.length);
		assertEquals("Test", result[0].logString);
		assertEquals("sba", result[0].authorString);
		assertEquals("Test2", result[1].logString);
		assertEquals("sba", result[1].authorString);
		assertEquals("Test3", result[2].logString);
		assertEquals("sba", result[2].authorString);
	}
}
