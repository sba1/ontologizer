package ontologizer.go;

import java.io.IOException;

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
	
	public void testIgnoreSynonyms() throws IOException, OBOParserException
	{
		OBOParser oboParser = new OBOParser(GOtermsOBOFile,OBOParser.IGNORE_SYNONYMS);
		oboParser.doParse();
		for (Term t : oboParser.getTermMap())
			Assert.assertEquals(0,t.getSynonyms().length);
	}
}
