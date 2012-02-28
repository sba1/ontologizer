package ontologizer.association;

import java.io.IOException;

import ontologizer.go.OBOParser;
import ontologizer.go.OBOParserException;
import ontologizer.go.TermContainer;
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
		assertEquals(87599, ap.getAssociations().size());
	}
}
