package ontologizer.association;

import static org.junit.Assert.assertEquals;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import ontologizer.go.OBOParser;
import ontologizer.go.OBOParserException;
import ontologizer.go.OBOParserFileInput;
import ontologizer.go.TermContainer;

public class AssociationParserTest
{
	private static final String OBO_FILE = "data/gene_ontology.1_2.obo.gz";
	private static final String ASSOCIATION_FILE = "data/gene_association.sgd.gz";

	@Test
	public void testSimple() throws IOException, OBOParserException
	{
		OBOParser oboParser = new OBOParser(new OBOParserFileInput(OBO_FILE));
		oboParser.doParse();
		AssociationParser ap = new AssociationParser(new OBOParserFileInput(ASSOCIATION_FILE), new TermContainer(oboParser.getTermMap(), "", ""));
		assertEquals(ap.getFileType(),AssociationParser.Type.GAF);
		assertEquals(87599, ap.getAssociations().size());

		Association a = ap.getAssociations().get(0);
		Assert.assertEquals("S000007287",a.getDB_Object().toString());

		/* Note that this excludes NOT annotations */
		a = ap.getAssociations().get(49088);
		Assert.assertEquals("S000004009",a.getDB_Object().toString());
	}

	@Test
	public void testSkipHeader() throws IOException, OBOParserException
	{
		File tmp = File.createTempFile("test", ".gaf");
		BufferedWriter bw = new BufferedWriter(new FileWriter(tmp));
		bw.write("# Comment1\n");
		bw.write("DB\tDBOBJID2\tSYMBOL\t\tGO:0005760\tPMID:00000\tEVIDENCE\t\tC\t\tgene\ttaxon:4932\t20121212\tSBA\n");
		bw.flush();
		bw.close();

		OBOParser oboParser = new OBOParser(new OBOParserFileInput(OBO_FILE));
		oboParser.doParse();

		AssociationParser ap = new AssociationParser(new OBOParserFileInput(tmp.getAbsolutePath()), new TermContainer(oboParser.getTermMap(), "", ""));
		AssociationContainer assoc = new AssociationContainer(ap.getAssociations(), ap.getSynonym2gene(), ap.getDbObject2gene());

		Assert.assertEquals(1, assoc.getAllAnnotatedGenes().size());
	}

	@Test
	public void testAmbiguousGAFCaseA() throws IOException, OBOParserException
	{

		File tmp = File.createTempFile("test", ".gaf");
		BufferedWriter bw = new BufferedWriter(new FileWriter(tmp));
		bw.write("DB\tDBOBJID1\tSYMBOL\t\tGO:0005763\tPMID:00000\tEVIDENCE\t\tC\tSYNONYM1|SYNONYM2\tgene\ttaxon:4932\t20121212\tSBA\n");
		bw.write("DB\tDBOBJID2\tSYMBOL\t\tGO:0005760\tPMID:00000\tEVIDENCE\t\tC\t\tgene\ttaxon:4932\t20121212\tSBA\n");
		bw.flush();
		bw.close();

		OBOParser oboParser = new OBOParser(new OBOParserFileInput(OBO_FILE));
		oboParser.doParse();

		AssociationParser ap = new AssociationParser(new OBOParserFileInput(tmp.getAbsolutePath()), new TermContainer(oboParser.getTermMap(), "", ""));
		AssociationContainer assoc = new AssociationContainer(ap.getAssociations(), ap.getSynonym2gene(), ap.getDbObject2gene());

		/* We expect only one annotated object as DBOBJID1 is the same as DBOBJID2 due to the same symbol */
		Assert.assertEquals(1,assoc.getAllAnnotatedGenes().size());
		Assert.assertEquals("SYMBOL",assoc.getAllAnnotatedGenes().iterator().next().toString());
	}

	@Test
	public void testIDS() throws IOException, OBOParserException
	{
		File tmp = File.createTempFile("test", ".ids");
		BufferedWriter bw = new BufferedWriter(new FileWriter(tmp));

		bw.write("S000007287\tGO:0005763,GO:0032543,GO:0042255,GO:0003735,GO:0032543,GO:0005762,GO:0003735,GO:0003735,GO:0042255\n");
		bw.write("S000004660\tGO:0005739,GO:0006810,GO:0005743,GO:0016020,GO:0055085,GO:0005488\n");
		bw.write("S000004660\tGO:0006810,GO:0005471,GO:0016021,GO:0006783,GO:0005743,GO:0005743\n");
		bw.flush();
		bw.close();

		OBOParser oboParser = new OBOParser(new OBOParserFileInput(OBO_FILE));
		oboParser.doParse();

		AssociationParser ap = new AssociationParser(new OBOParserFileInput(tmp.getAbsolutePath()),new TermContainer(oboParser.getTermMap(), "", ""));
		Assert.assertEquals(21,ap.getAssociations().size());
	}
}
