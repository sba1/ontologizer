package ontologizer.association;

import java.io.IOException;

import ontologizer.go.OBOParser;
import ontologizer.go.OBOParserException;
import ontologizer.go.TermContainer;
import junit.framework.Assert;
import junit.framework.TestCase;

public class AssociationParserTest extends TestCase
{
	private static final String OBO_FILE = "data/gene_ontology.1_2.obo.gz";
	private static final String ASSOCIATION_FILE = "data/gene_association.sgd.gz";
	
	public void testSimple() throws IOException, OBOParserException
	{
		OBOParser oboParser = new OBOParser(OBO_FILE);
		oboParser.doParse();
		AssociationParser ap = new AssociationParser(ASSOCIATION_FILE, new TermContainer(oboParser.getTermMap(), "", ""));
		assertEquals(ap.getFileType(),AssociationParser.Type.GAF);
		assertEquals(87599, ap.getAssociations().size());
		
		Association a = ap.getAssociations().get(0);
		Assert.assertEquals("S000007287",a.getDB_Object().toString());
		
		/* Note that this excludes NOT annotations */
		a = ap.getAssociations().get(49088);
		Assert.assertEquals("S000004009",a.getDB_Object().toString());
	}
}
