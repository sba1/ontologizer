package ontologizer.util;

import org.junit.Assert;
import org.junit.Test;

import ontologizer.util.LinkExtractor.Extracted;

public class LinkExtractorTest
{
	@Test
	public void testLinkExtractor()
	{
		Extracted extracted = new LinkExtractor("test <a href=\"test\">link</a> test <a href=\"test2\">link2</a>").extract();

		Assert.assertEquals("test link test link2", extracted.text);
		Assert.assertArrayEquals(new int[]{5,15}, extracted.starts);
		Assert.assertArrayEquals(new int[]{9,20}, extracted.ends);
		Assert.assertArrayEquals(new String[]{"test","test2"}, extracted.hrefs);
	}
}
