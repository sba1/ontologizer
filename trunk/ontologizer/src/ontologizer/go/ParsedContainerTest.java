package ontologizer.go;

import junit.framework.Assert;
import junit.framework.TestCase;

public class ParsedContainerTest extends TestCase
{
	// reusable fields for dependent tests
	public TermContainer container;
	
	// internal fields
	private OBOParser oboParser;
	
	private Term bioproc = new Term("GO:0008150", "biological_process", "B");
	private Term cellcomp = new Term("GO:0005575", "cellular_component", "C");
	private Term molfunc = new Term("GO:0003674", "molecular_function", "F");

	@Override
	protected void setUp() throws Exception
	{
		OBOParserTest oPT = new OBOParserTest();
		oPT.run();
		
		oboParser = oPT.oboParser;
		container = new TermContainer(oboParser.getTermMap(), oboParser.getFormatVersion(), oboParser.getDate());
	}


	public void testBasicStructure()
	{
		
		Assert.assertTrue(container.termCount() == oboParser.getTermMap().size());
		Assert.assertTrue(container.getFormatVersion().equals(oboParser.getFormatVersion()));
		Assert.assertTrue(container.getDate().equals(oboParser.getDate()));
		
		Assert.assertTrue(container.getGOName("GO:0008150").equals("biological_process"));
		Assert.assertTrue(container.getGOName(bioproc.getID()).equals("biological_process"));
		Assert.assertTrue(container.getGOName("GO:0005575").equals("cellular_component"));
		Assert.assertTrue(container.getGOName(cellcomp.getID()).equals("cellular_component"));
		Assert.assertTrue(container.getGOName("GO:0003674").equals("molecular_function"));
		Assert.assertTrue(container.getGOName(molfunc.getID()).equals("molecular_function"));

		Assert.assertTrue(container.get("GO:0008150").equals(bioproc));
		Assert.assertTrue(container.get(bioproc.getID()).equals(bioproc));
		Assert.assertTrue(container.get("GO:0005575").equals(cellcomp));
		Assert.assertTrue(container.get(cellcomp.getID()).equals(cellcomp));
		Assert.assertTrue(container.get("GO:0003674").equals(molfunc));
		Assert.assertTrue(container.get(molfunc.getID()).equals(molfunc));
		
		Assert.assertTrue(container.get("GO:0000815") == null);
		Term anotherTerm = new Term("GO:0000815", "dummy", null);
		Assert.assertTrue(container.get(anotherTerm.getID()) == null);
	}
	
	
}
