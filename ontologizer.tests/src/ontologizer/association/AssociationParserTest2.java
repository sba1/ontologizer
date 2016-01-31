package ontologizer.association;
import org.junit.Before;

import org.junit.Test;

import org.junit.Assert;

import ontologizer.association.AssociationContainer;
import ontologizer.association.AssociationParser;
import ontologizer.go.OBOParser;
import ontologizer.go.OBOParserFileInput;
import ontologizer.go.OBOParserTest;
import ontologizer.go.TermContainer;
import ontologizer.types.ByteString;

public class AssociationParserTest2
{
	private TermContainer container;
	private AssociationParser assocParser;
	private AssociationContainer assocContainer;

	// data for testing
	private String GOAssociationFile = "data/gene_association.sgd.gz";
	private int nAnnotatedGenes = 6359;
	private int nAssociations = 87599;
	private int nSynonyms = 9317;
	private int nDBObjects = 6359;

	private String[] someGenes = {"SRL1", "DDR2", "UFO1"};
	private int[] someGeneTermCounts = {11, 4, 8};

	@Before
	public void setUp() throws Exception
	{
		/* FIXME: Duplicated from ParsedContainerTest */
		OBOParser oboParser = new OBOParser(new OBOParserFileInput(OBOParserTest.GOtermsOBOFile));
		oboParser.doParse();
		container = new TermContainer(oboParser.getTermMap(), oboParser.getFormatVersion(), oboParser.getDate());

		assocParser = new AssociationParser(new OBOParserFileInput(GOAssociationFile), container, null);

		assocContainer = new AssociationContainer(assocParser.getAssociations(),
				assocParser.getSynonym2gene(),
				assocParser.getDbObject2gene());
	}


	@Test
	public void testBasicStructure()
	{
		Assert.assertEquals("number of parsed associations", nAssociations, assocParser.getAssociations().size());
		Assert.assertEquals("number of parsed synonyms", nSynonyms,assocParser.getSynonym2gene().size());
		Assert.assertEquals("number of parsed DB objects", nDBObjects,assocParser.getDbObject2gene().size());
		Assert.assertEquals("number of annotated genes", nAnnotatedGenes,assocContainer.getAllAnnotatedGenes().size());

		for (int i=0; i<someGenes.length; i++) {
			Assert.assertEquals(assocContainer.get(new ByteString(someGenes[i])).getAssociations().size(), someGeneTermCounts[i]);
		}
	}


}
