package ontologizer.association;

import static org.junit.Assert.assertEquals;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ontologizer.ontology.OBOParser;
import ontologizer.ontology.OBOParserException;
import ontologizer.ontology.OBOParserFileInput;
import ontologizer.ontology.TermContainer;

public class AssociationParserTest
{
	private static final String OBO_FILE = "data/gene_ontology.1_2.obo.gz";
	private static final String ASSOCIATION_FILE = "data/gene_association.sgd.gz";

	@Rule
	public TemporaryFolder tmpFolder = new TemporaryFolder();

	@Test
	public void testSimple() throws IOException, OBOParserException
	{
		OBOParser oboParser = new OBOParser(new OBOParserFileInput(OBO_FILE));
		oboParser.doParse();
		AssociationParser ap = new AssociationParser(new OBOParserFileInput(ASSOCIATION_FILE), new TermContainer(oboParser.getTermMap(), "", ""));
		assertEquals(ap.getFileType(),AssociationParser.Type.GAF);
		assertEquals(87599, ap.getAssociations().size());

		Association a = ap.getAssociations().get(0);
		assertEquals("S000007287",a.getDB_Object().toString());

		/* Note that this excludes NOT annotations */
		a = ap.getAssociations().get(49088);
		assertEquals("S000004009",a.getDB_Object().toString());
	}

	@Test
	public void testUncompressed() throws IOException, OBOParserException
	{
		/* As testSimple() but bypasses auto decompression by manually decompressing
		 * the association file
		 */
		File assocFile = tmpFolder.newFile();
		GZIPInputStream in = new GZIPInputStream(new FileInputStream(ASSOCIATION_FILE));
		FileOutputStream out = new FileOutputStream(assocFile);
		byte [] buf = new byte[4096];
		int read;
		while ((read = in.read(buf)) > 0)
			out.write(buf, 0,  read);
		in.close();
		out.close();

		OBOParser oboParser = new OBOParser(new OBOParserFileInput(OBO_FILE));
		oboParser.doParse();
		AssociationParser ap = new AssociationParser(new OBOParserFileInput(assocFile.getAbsolutePath()), new TermContainer(oboParser.getTermMap(), "", ""));
		assertEquals(ap.getFileType(),AssociationParser.Type.GAF);
		assertEquals(87599, ap.getAssociations().size());

		Association a = ap.getAssociations().get(0);
		assertEquals("S000007287",a.getDB_Object().toString());

		/* Note that this excludes NOT annotations */
		a = ap.getAssociations().get(49088);
		assertEquals("S000004009",a.getDB_Object().toString());
	}

	@Test
	public void testSkipHeader() throws IOException, OBOParserException
	{
		File tmp = tmpFolder.newFile("testSkipHeaeder.gaf");
		BufferedWriter bw = new BufferedWriter(new FileWriter(tmp));
		bw.write("# Comment1\n");
		bw.write("DB\tDBOBJID2\tSYMBOL\t\tGO:0005760\tPMID:00000\tEVIDENCE\t\tC\t\tgene\ttaxon:4932\t20121212\tSBA\n");
		bw.flush();
		bw.close();

		OBOParser oboParser = new OBOParser(new OBOParserFileInput(OBO_FILE));
		oboParser.doParse();

		AssociationParser ap = new AssociationParser(new OBOParserFileInput(tmp.getAbsolutePath()), new TermContainer(oboParser.getTermMap(), "", ""));
		AssociationContainer assoc = new AssociationContainer(ap.getAssociations(), ap.getSynonym2gene(), ap.getDbObject2gene());

		assertEquals(1, assoc.getAllAnnotatedGenes().size());
	}

	@Test
	public void testReadFromCompressedFile() throws IOException, OBOParserException
	{
		File tmp = tmpFolder.newFile("testReadFromCompressedFile.gaf.gz");
		Writer bw = new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(tmp)));
		bw.write("DB\tDBOBJID2\tSYMBOL\t\tGO:0005760\tPMID:00000\tEVIDENCE\t\tC\t\tgene\ttaxon:4932\t20121212\tSBA\n");
		bw.flush();
		bw.close();

		OBOParser oboParser = new OBOParser(new OBOParserFileInput(OBO_FILE));
		oboParser.doParse();

		AssociationParser ap = new AssociationParser(new OBOParserFileInput(tmp.getAbsolutePath()), new TermContainer(oboParser.getTermMap(), "", ""));
		AssociationContainer assoc = new AssociationContainer(ap.getAssociations(), ap.getSynonym2gene(), ap.getDbObject2gene());

		assertEquals(1, assoc.getAllAnnotatedGenes().size());
	}

	@Test
	public void testAmbiguousGAFCaseA() throws IOException, OBOParserException
	{
		File tmp = tmpFolder.newFile("testAmbiguousGAFCaseA.gaf");
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
		assertEquals(1,assoc.getAllAnnotatedGenes().size());
		assertEquals("SYMBOL",assoc.getAllAnnotatedGenes().iterator().next().toString());
	}

	@Test
	public void testTwoEntries() throws IOException, OBOParserException
	{
		File tmp = tmpFolder.newFile("testTwoEntries.gaf");
		BufferedWriter bw = new BufferedWriter(new FileWriter(tmp));
		bw.write("\n\n");
		bw.write("DB\tDBOBJID\tSYMBOL1\t\tGO:0005763\tPMID:00000\tEVIDENCE\t\tC\tSYNONYM1|SYNONYM2\tgene\ttaxon:4932\t20121212\tSBA\n");
		bw.write("DB\tDBOBJID2\tSYMBOL2\t\tGO:0005760\tPMID:00000\tEVIDENCE\t\tC\t\tgene\ttaxon:4932\t20121212\tSBA\n");
		bw.flush();
		bw.close();

		OBOParser oboParser = new OBOParser(new OBOParserFileInput(OBO_FILE));
		oboParser.doParse();

		AssociationParser ap = new AssociationParser(new OBOParserFileInput(tmp.getAbsolutePath()), new TermContainer(oboParser.getTermMap(), "", ""));
		AssociationContainer assoc = new AssociationContainer(ap.getAssociations(), ap.getSynonym2gene(), ap.getDbObject2gene());

		assertEquals(2,assoc.getAllAnnotatedGenes().size());
	}

	@Test
	public void testIDS() throws IOException, OBOParserException
	{
		File tmp = tmpFolder.newFile("testIDS.ids");
		BufferedWriter bw = new BufferedWriter(new FileWriter(tmp));

		bw.write("S000007287\tGO:0005763,GO:0032543,GO:0042255,GO:0003735,GO:0032543,GO:0005762,GO:0003735,GO:0003735,GO:0042255\n");
		bw.write("S000004660\tGO:0005739,GO:0006810,GO:0005743,GO:0016020,GO:0055085,GO:0005488\n");
		bw.write("S000004660\tGO:0006810,GO:0005471,GO:0016021,GO:0006783,GO:0005743,GO:0005743\n");
		bw.flush();
		bw.close();

		OBOParser oboParser = new OBOParser(new OBOParserFileInput(OBO_FILE));
		oboParser.doParse();

		AssociationParser ap = new AssociationParser(new OBOParserFileInput(tmp.getAbsolutePath()),new TermContainer(oboParser.getTermMap(), "", ""));
		assertEquals(21,ap.getAssociations().size());
	}
}
