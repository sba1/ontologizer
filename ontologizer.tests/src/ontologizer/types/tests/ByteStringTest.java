package ontologizer.types.tests;

import ontologizer.types.ByteString;
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
	
	public void testSubstring()
	{
		assertEquals("TEst",new ByteString("TestTEstTest").substring(4,8).toString());
	}
}
