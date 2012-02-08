package sonumina.math.changelog;

import sonumina.util.changelog.BuildChangeLog;
import sonumina.util.changelog.Change;
import junit.framework.Assert;
import junit.framework.TestCase;

public class BuildChangeLogTest extends TestCase
{
	public void testBasicLog()
	{
		String string = "------------------------------------------------------------------------\n" +
						"r716 | sba | 2012-02-08 16:30:24 +0100 (Wed, 08 Feb 2012) | 1 line\n" +
						"\n" +
						"Added support for arbitrary node paths.\n" +
						"$userlog$Test\n"+
						"------------------------------------------------------------------------\n" +
						"r715 | sba | 2012-02-08 14:51:00 +0100 (Wed, 08 Feb 2012) | 1 line\n" +
						"\n" +
						"Removed generateShuffledStudySets().\n" +
						"$userlog$Test2\n"+
						"------------------------------------------------------------------------\n" +
						"r714 | sba | 2012-01-25 14:01:46 +0100 (Wed, 25 Jan 2012) | 1 line\n" +
						"\n" +
						"Added labels to the chart.\n" +
						"$userlog$Test3\n"+
						"------------------------------------------------------------------------\n";
		Change [] result = BuildChangeLog.process(string);
		Assert.assertEquals(3,result.length);
		Assert.assertEquals("Test", result[0].log);
		Assert.assertEquals("sba", result[0].author);
		Assert.assertEquals("Test2", result[1].log);
		Assert.assertEquals("sba", result[1].author);
		Assert.assertEquals("Test3", result[2].log);
		Assert.assertEquals("sba", result[2].author);
	}
}
