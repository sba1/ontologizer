package ontologizer.go;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.junit.Test;


public class OBOParserTest
{
	/* internal fields */
	public static final String GOtermsOBOFile = "data/gene_ontology.1_2.obo.gz";
	private static final int nTermCount = 35520;
	private static final int nRelations = 63105;
	private static final String formatVersion = "1.2";
	private static final String date = "04:01:2012 11:50";

	@Test
	public void testTermBasics() throws IOException, OBOParserException
	{
		/* Parse OBO file */
		System.out.println("Parse OBO file");
		OBOParser oboParser = new OBOParser(new OBOParserFileInput(GOtermsOBOFile));
		System.out.println(oboParser.doParse());
		HashMap<String,Term> id2Term = new HashMap<String,Term>();

		int relations = 0;
		for (Term t : oboParser.getTermMap())
		{
			relations += t.getParents().length;
			id2Term.put(t.getIDAsString(),t);
		}

		assertEquals(nTermCount, oboParser.getTermMap().size());
		assertEquals(formatVersion,oboParser.getFormatVersion());
		assertEquals(date,oboParser.getDate());
		assertEquals(nRelations,relations);
		assertTrue(id2Term.containsKey("GO:0008150"));
		assertEquals(0,id2Term.get("GO:0008150").getParents().length);
	}

	@Test
	public void testIgnoreSynonyms() throws IOException, OBOParserException
	{
		OBOParser oboParser = new OBOParser(new OBOParserFileInput(GOtermsOBOFile),OBOParser.IGNORE_SYNONYMS);
		oboParser.doParse();
		for (Term t : oboParser.getTermMap())
			assertTrue(t.getSynonyms() == null || t.getSynonyms().length == 0);
	}

	@Test
	public void testMultiline() throws IOException, OBOParserException
	{
		File tmp = File.createTempFile("onto", ".obo");
		PrintWriter pw = new PrintWriter(tmp);
		pw.append("[term]\nname: test\\\ntest\\\ntest\n");
		pw.close();

		OBOParser oboParser = new OBOParser(new OBOParserFileInput(tmp.getCanonicalPath()));
		oboParser.doParse();
	}

	@Test
	public void testPartOf() throws IOException, OBOParserException
	{
		File tmp = File.createTempFile("onto", ".obo");
		PrintWriter pw = new PrintWriter(tmp);
		pw.append("[term]\n" +
		          "name: test\n" +
				  "id: GO:0000001\n\n" +
		          "[term]\n" +
				  "name: test2\n" +
		          "id: GO:0000002\n\n" +
		          "relationship: part_of GO:0000001 ! test\n");
		pw.close();

		OBOParser oboParser = new OBOParser(new OBOParserFileInput(tmp.getCanonicalPath()));
		oboParser.doParse();
		ArrayList<Term> terms = new ArrayList<Term>(oboParser.getTermMap());
		HashMap<String,Term> name2Term = new HashMap<String,Term>();
		for (Term t : terms)
			name2Term.put(t.getIDAsString(), t);
		assertEquals(TermRelation.PART_OF_A, name2Term.get("GO:0000002").getParents()[0].relation);
		assertEquals("GO:0000001", name2Term.get("GO:0000002").getParents()[0].termid.toString());
	}

	@Test
	public void testRegulates() throws IOException, OBOParserException
	{
		File tmp = File.createTempFile("onto", ".obo");
		PrintWriter pw = new PrintWriter(tmp);
		pw.append("[term]\n" +
		          "name: test\n" +
				  "id: GO:0000001\n\n" +
		          "[term]\n" +
				  "name: test2\n" +
		          "id: GO:0000002\n\n" +
		          "relationship: regulates GO:0000001 ! test\n");
		pw.close();

		OBOParser oboParser = new OBOParser(new OBOParserFileInput(tmp.getCanonicalPath()));
		oboParser.doParse();
		ArrayList<Term> terms = new ArrayList<Term>(oboParser.getTermMap());
		HashMap<String,Term> name2Term = new HashMap<String,Term>();
		for (Term t : terms)
			name2Term.put(t.getIDAsString(), t);
		assertEquals(TermRelation.REGULATES, name2Term.get("GO:0000002").getParents()[0].relation);
		assertEquals("GO:0000001", name2Term.get("GO:0000002").getParents()[0].termid.toString());
	}

	@Test
	public void testUnknownRelationship() throws IOException, OBOParserException
	{
		File tmp = File.createTempFile("onto", ".obo");
		PrintWriter pw = new PrintWriter(tmp);
		pw.append("[term]\n" +
		          "name: test\n" +
				  "id: GO:0000001\n\n" +
		          "[term]\n" +
				  "name: test2\n" +
		          "id: GO:0000002\n\n" +
		          "relationship: zzz GO:0000001 ! test\n");
		pw.close();

		OBOParser oboParser = new OBOParser(new OBOParserFileInput(tmp.getCanonicalPath()));
		oboParser.doParse();
		ArrayList<Term> terms = new ArrayList<Term>(oboParser.getTermMap());
		HashMap<String,Term> name2Term = new HashMap<String,Term>();
		for (Term t : terms)
			name2Term.put(t.getIDAsString(), t);
		assertEquals(TermRelation.UNKOWN, name2Term.get("GO:0000002").getParents()[0].relation);
		assertEquals("GO:0000001", name2Term.get("GO:0000002").getParents()[0].termid.toString());
	}

	@Test
	public void testSynonyms() throws IOException, OBOParserException
	{
		File tmp = File.createTempFile("onto", ".obo");
		PrintWriter pw = new PrintWriter(tmp);
		pw.append("[term]\n" +
		          "name: test\n" +
				  "id: GO:0000001\n" +
		          "synonym: \"test2\"\n" +
				  "synonym: \"test3\" EXACT []\n");
		pw.close();

		OBOParser oboParser = new OBOParser(new OBOParserFileInput(tmp.getCanonicalPath()));
		oboParser.doParse();
		ArrayList<Term> terms = new ArrayList<Term>(oboParser.getTermMap());
		assertEquals(1, terms.size());
		String [] expected = new String[]{"test2","test3"};
		assertEquals(expected.length, terms.get(0).getSynonyms().length);
		for (int i=0;i<expected.length;i++)
			assertEquals(expected[i],terms.get(0).getSynonyms()[i]);
	}

	@Test
	public void testDef() throws IOException, OBOParserException
	{
		File tmp = File.createTempFile("onto", ".obo");
		PrintWriter pw = new PrintWriter(tmp);
		pw.append("[term]\n" +
		          "name: test\n" +
				  "id: GO:0000001\n" +
		          "def: \"This is a so-called \\\"test\\\"\"\n");
		pw.close();

		OBOParser oboParser = new OBOParser(new OBOParserFileInput(tmp.getCanonicalPath()),OBOParser.PARSE_DEFINITIONS);
		oboParser.doParse();
		ArrayList<Term> terms = new ArrayList<Term>(oboParser.getTermMap());
		assertEquals(1, terms.size());
		assertEquals("This is a so-called \"test\"", terms.get(0).getDefinition());
	}

	@Test
	public void testEquivalent() throws IOException, OBOParserException
	{
		File tmp = File.createTempFile("onto", ".obo");
		PrintWriter pw = new PrintWriter(tmp);
		pw.append("[term]\n" +
		          "name: test\n" +
				  "id: GO:0000001\n" +
		          "def: \"This is a so-called \\\"test\\\"\"\n\n"+
				  "[term]\n" +
				  "name: test2\n" +
				  "id: GO:0000002\n" +
				  "equivalent_to: GO:0000001\n" +
				  "equivalent_to: GO:0000003 ! comment\n");
		pw.close();

		OBOParser oboParser = new OBOParser(new OBOParserFileInput(tmp.getCanonicalPath()));
		oboParser.doParse();

		ArrayList<Term> terms = new ArrayList<Term>(oboParser.getTermMap());
		HashMap<String,Term> name2Term = new HashMap<String,Term>();
		for (Term t : terms)
			name2Term.put(t.getIDAsString(), t);

		assertEquals(2,name2Term.get("GO:0000002").getEquivalents().length);
		HashSet<String> ids = new HashSet<String>();
		ids.add("GO:0000001");
		ids.add("GO:0000003");
		for (TermID id : name2Term.get("GO:0000002").getEquivalents())
			assertTrue(ids.contains(id.toString()));
	}

	@Test
	public void testObsolete() throws IOException, OBOParserException
	{
		File tmp = File.createTempFile("onto", ".obo");
		PrintWriter pw = new PrintWriter(tmp);
		pw.append("[term]\n" +
		          "name: test\n" +
				  "id: GO:0000001\n" +
		          "def: \"This is a so-called \\\"test\\\"\"\n" +
				  "is_obsolete: true");
		pw.close();

		OBOParser oboParser = new OBOParser(new OBOParserFileInput(tmp.getCanonicalPath()),OBOParser.PARSE_XREFS);
		oboParser.doParse();
		ArrayList<Term> terms = new ArrayList<Term>(oboParser.getTermMap());
		assertTrue(terms.get(0).isObsolete());
	}

	@Test
	public void testXRef() throws IOException, OBOParserException
	{
		File tmp = File.createTempFile("onto", ".obo");
		PrintWriter pw = new PrintWriter(tmp);
		pw.append("[term]\n" +
		          "name: test\n" +
				  "id: GO:0000001\n" +
		          "def: \"This is a so-called \\\"test\\\"\"\n" +
				  "xref: db:ID \"WW\"");
		pw.close();

		OBOParser oboParser = new OBOParser(new OBOParserFileInput(tmp.getCanonicalPath()),OBOParser.PARSE_XREFS);
		oboParser.doParse();
		ArrayList<Term> terms = new ArrayList<Term>(oboParser.getTermMap());
		assertEquals(1, terms.size());
		assertEquals("db",terms.get(0).getXrefs()[0].getDatabase());
		assertEquals("ID",terms.get(0).getXrefs()[0].getXrefId());
		assertEquals("WW",terms.get(0).getXrefs()[0].getXrefName());
	}

	@Test
	public void testXRef2Spaces() throws IOException, OBOParserException
	{
		File tmp = File.createTempFile("onto", ".obo");
		PrintWriter pw = new PrintWriter(tmp);
		pw.append("[term]\n" +
		          "name: test\n" +
				  "id: GO:0000001\n" +
		          "def: \"This is a so-called \\\"test\\\"\"\n" +
				  "xref: db:ID  \"WW\"");
		pw.close();

		OBOParser oboParser = new OBOParser(new OBOParserFileInput(tmp.getCanonicalPath()),OBOParser.PARSE_XREFS);
		oboParser.doParse();
		ArrayList<Term> terms = new ArrayList<Term>(oboParser.getTermMap());
		assertEquals(1, terms.size());
		assertEquals("db",terms.get(0).getXrefs()[0].getDatabase());
		assertEquals("ID",terms.get(0).getXrefs()[0].getXrefId());
		assertEquals("WW",terms.get(0).getXrefs()[0].getXrefName());
	}

	@Test
	public void testSimpleXRef() throws IOException, OBOParserException
	{
		File tmp = File.createTempFile("onto", ".obo");
		PrintWriter pw = new PrintWriter(tmp);
		pw.append("[term]\n" +
		          "name: test\n" +
				  "id: GO:0000001\n" +
		          "def: \"This is a so-called \\\"test\\\"\"\n" +
				  "xref: db:ID");
		pw.close();

		OBOParser oboParser = new OBOParser(new OBOParserFileInput(tmp.getCanonicalPath()),OBOParser.PARSE_XREFS);
		oboParser.doParse();
		ArrayList<Term> terms = new ArrayList<Term>(oboParser.getTermMap());
		assertEquals(1, terms.size());
		assertEquals("db",terms.get(0).getXrefs()[0].getDatabase());
		assertEquals("ID",terms.get(0).getXrefs()[0].getXrefId());
		assertNull(terms.get(0).getXrefs()[0].getXrefName());
	}

	@Test
	public void testSubset() throws IOException, OBOParserException
	{
		File tmp = File.createTempFile("onto", ".obo");
		PrintWriter pw = new PrintWriter(tmp);
		pw.append("subsetdef: subset \"Subset\"\n" +
		          "[term]\n" +
				  "name: test\n" +
		          "id: GO:0000001\n" +
				  "subset: subset\n" +
		          "[term]\n" +
				  "name: test2\n" +
		          "id: GO:0000002\n");
		pw.close();

		OBOParser oboParser = new OBOParser(new OBOParserFileInput(tmp.getCanonicalPath()),OBOParser.PARSE_DEFINITIONS);
		oboParser.doParse();
		ArrayList<Term> terms = new ArrayList<Term>(oboParser.getTermMap());
		assertEquals(2, terms.size());
		assertEquals(1, terms.get(0).getSubsets().length);
		assertEquals("subset", terms.get(0).getSubsets()[0].getName());
		assertEquals(0, terms.get(1).getSubsets().length);
	}

	@Test
	public void testAltId() throws IOException, OBOParserException
	{
		File tmp = File.createTempFile("onto", ".obo");
		PrintWriter pw = new PrintWriter(tmp);
		pw.append("[term]\n" +
		          "name: test\n" +
				  "id: GO:0000001\n" +
		          "alt_id: GO:0000003\n");
		pw.close();

		OBOParser oboParser = new OBOParser(new OBOParserFileInput(tmp.getCanonicalPath()),OBOParser.PARSE_DEFINITIONS);
		oboParser.doParse();
		ArrayList<Term> terms = new ArrayList<Term>(oboParser.getTermMap());
		assertEquals(1, terms.size());
		assertEquals("GO:0000003", terms.get(0).getAlternatives()[0].toString());
	}

	@Test
	public void testExceptions() throws IOException
	{
		File tmp = File.createTempFile("onto", ".obo");
		PrintWriter pw = new PrintWriter(tmp);
		pw.append("[term\nimport: sss\n");
		pw.close();


		OBOParser oboParser = new OBOParser(new OBOParserFileInput(tmp.getCanonicalPath()));
		try
		{
			oboParser.doParse();
			assertTrue("Exception asserted", false);
		} catch (OBOParserException ex)
		{
			ex.printStackTrace();
			assertEquals(1,ex.linenum);
		}
	}

	@Test
	public void testExceptions2() throws IOException
	{
		File tmp = File.createTempFile("onto", ".obo");
		PrintWriter pw = new PrintWriter(tmp);
		pw.append("[term \nimport: sss\n");
		pw.close();


		OBOParser oboParser = new OBOParser(new OBOParserFileInput(tmp.getCanonicalPath()));
		try
		{
			oboParser.doParse();
			assertTrue("Exception asserted", false);
		} catch (OBOParserException ex)
		{
			ex.printStackTrace();
			assertEquals(1,ex.linenum);
		}
	}

	@Test
	public void testArbitraryID() throws IOException, OBOParserException
	{
		File tmp = File.createTempFile("onto", ".obo");
		PrintWriter pw = new PrintWriter(tmp);
		pw.append("[term]\n" +
		          "name: test\n" +
				  "id: prefix:test\n");
		pw.close();

		OBOParser oboParser = new OBOParser(new OBOParserFileInput(tmp.getCanonicalPath()),0);
		oboParser.doParse();
		ArrayList<Term> terms = new ArrayList<Term>(oboParser.getTermMap());
		assertEquals(1, terms.size());
	}

}
