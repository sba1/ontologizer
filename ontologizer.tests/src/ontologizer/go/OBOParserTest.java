package ontologizer.go;

import junit.framework.Assert;
import junit.framework.TestCase;

public class OBOParserTest extends TestCase
{
	// reusable fields for dependent tests
	public OBOParser oboParser;

	// internal fields
	private String GOtermsOBOFile = "data/GO_test.obo";
	private int nTermCount = 18;
	private String formatVersion = "1.0";
	private String date = "12:04:2006 20:50";

	@Override
	protected void setUp() throws Exception
	{
		/* Parse OBO file */
		System.out.println("Parse OBO file");
		oboParser = new OBOParser(GOtermsOBOFile);
		System.out.println(oboParser.doParse());
	}


	public void testBasicStructure()
	{
		Assert.assertTrue("TermMap size test", oboParser.getTermMap().size() == nTermCount);
		Assert.assertTrue("format version test", oboParser.getFormatVersion().equals(formatVersion));
		Assert.assertTrue("date test", oboParser.getDate().equals(date));
	}


}
