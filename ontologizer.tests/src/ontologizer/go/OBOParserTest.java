package ontologizer.go;

import junit.framework.Assert;
import junit.framework.TestCase;

public class OBOParserTest extends TestCase
{
	/* reusable fields for dependent tests */
	public OBOParser oboParser;
	
	/* internal fields */
	private String GOtermsOBOFile = "data/gene_ontology.1_2.obo.gz";
	private int nTermCount = 35520;
	private String formatVersion = "1.2";
	private String date = "04:01:2012 11:50";

	@Override
	protected void setUp() throws Exception
	{
		/* Parse OBO file */
		System.out.println("Parse OBO file");
		oboParser = new OBOParser(GOtermsOBOFile);
		System.out.println(oboParser.doParse());
	}


	public void testTermCount()
	{
		Assert.assertEquals(nTermCount, oboParser.getTermMap().size());
	}
	
	public void testFormatVersion()
	{
		Assert.assertEquals(formatVersion,oboParser.getFormatVersion());
	}
	
	public void testDate()
	{
		Assert.assertEquals(date,oboParser.getDate());
	}
}
