package ontologizer.types.tests;

import ontologizer.types.ByteString;
import junit.framework.Assert;
import junit.framework.TestCase;

public class ByteStringTest extends TestCase
{
	public void testParseInteger()
	{
		assertEquals(1234, ByteString.parseFirstInt(new ByteString("1234")));
		assertEquals(1234, ByteString.parseFirstInt(new ByteString("001234")));
		assertEquals(4500, ByteString.parseFirstInt(new ByteString("4500")));
		assertEquals(4500, ByteString.parseFirstInt(new ByteString("0000000004500")));
		assertEquals(1234, ByteString.parseFirstInt(new ByteString("ssss1234ssss")));
		assertEquals(1234, ByteString.parseFirstInt(new ByteString("ssss001234ssss")));

		try
		{
			ByteString.parseFirstInt(new ByteString("sswwscs"));
			assertTrue(false);
		} catch (NumberFormatException ex) {}
	}

	public void testByteParseInteger()
	{
		byte [] buf = "xx1234xx".getBytes();
		assertEquals(1234, ByteString.parseFirstInt(buf, 0, buf.length));
		assertEquals(123, ByteString.parseFirstInt(buf, 0, 5));
		assertEquals(23, ByteString.parseFirstInt(buf, 3, 2));
	}

	public void testSubstring()
	{
		assertEquals("TEst",new ByteString("TestTEstTest").substring(4,8).toString());
	}

	public void testSplit()
	{
		ByteString [] split = new ByteString("str1|str2|str3").splitBySingleChar('|');
		Assert.assertEquals(3,split.length);
		Assert.assertEquals("str1", split[0].toString());
		Assert.assertEquals("str2", split[1].toString());
		Assert.assertEquals("str3", split[2].toString());

		split = new ByteString("str1|str2|str3|").splitBySingleChar('|');
		Assert.assertEquals(4,split.length);
		Assert.assertEquals("str1", split[0].toString());
		Assert.assertEquals("str2", split[1].toString());
		Assert.assertEquals("str3", split[2].toString());
		Assert.assertEquals("", split[3].toString());

		split = new ByteString("str1").splitBySingleChar('|');
		Assert.assertEquals(1,split.length);
		Assert.assertEquals("str1", split[0].toString());

		split = new ByteString("str1||str3").splitBySingleChar('|');
		Assert.assertEquals(3,split.length);
		Assert.assertEquals("str1", split[0].toString());
		Assert.assertEquals("", split[1].toString());
		Assert.assertEquals("str3", split[2].toString());
}
}
