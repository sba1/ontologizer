package ontologizer.association;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
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
	
	public void testIDS() throws IOException, OBOParserException
	{
		File tmp = File.createTempFile("test", ".ids");
		BufferedWriter bw = new BufferedWriter(new FileWriter(tmp));
		
		bw.write("S000007287\tGO:0005763,GO:0032543,GO:0042255,GO:0003735,GO:0032543,GO:0005762,GO:0003735,GO:0003735,GO:0042255\n");
		bw.write("S000004660\tGO:0005739,GO:0006810,GO:0005743,GO:0016020,GO:0055085,GO:0005488\n");
		bw.write("S000004660\tGO:0006810,GO:0005471,GO:0016021,GO:0006783,GO:0005743,GO:0005743\n");
		bw.flush();
		
		OBOParser oboParser = new OBOParser(OBO_FILE);
		oboParser.doParse();

		AssociationParser ap = new AssociationParser(tmp.getAbsolutePath(),new TermContainer(oboParser.getTermMap(), "", ""));
		Assert.assertEquals(21,ap.getAssociations().size());
	}
}
