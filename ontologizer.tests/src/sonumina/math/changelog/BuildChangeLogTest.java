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
		String string =
			"a0cad40\tSebastian Bauer\tThu Feb 4 16:30:58 2016 +0100\tIntroduced ArrayBufferHttpRequest class.\n" +
			"\n" +
			"$foruser$Test\n\f" +
			"86f4645\tSebastian Bauer\tWed Feb 3 21:35:51 2016 +0100\tAdded \"Ontologize\" button and use bootstrap table to display results.\n" +
			"\n" +
			"It is all very slow.\n"+
			"\n\f" +
			"ae87f12\tSebastian Bauer\tWed Feb 3 21:36:21 2016 +0100\tTeaVM compatibility.\n" +
			"$foruser$Test2\n" +
			"\n\f" +
			"c918776\tSebastian Bauer\tWed Feb 3 21:31:38 2016 +0100\tExtracted HTMLTextAreaElement class.\n" +
			"$foruser$Test3\n"+
			"\n\f";

		Change [] result = BuildChangeLog.process(string);
		assertEquals(3,result.length);
		assertEquals("Test", result[0].logString);
		assertEquals("Sebastian Bauer", result[0].authorString);
		assertEquals("Test2", result[1].logString);
		assertEquals("Sebastian Bauer", result[1].authorString);
		assertEquals("Test3", result[2].logString);
		assertEquals("Sebastian Bauer", result[2].authorString);
	}
}
