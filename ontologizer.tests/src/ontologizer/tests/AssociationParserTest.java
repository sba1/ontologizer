package ontologizer.tests;

import junit.framework.Assert;
import junit.framework.TestCase;
import ontologizer.association.AssociationContainer;
import ontologizer.association.AssociationParser;
import ontologizer.go.ParsedContainerTest;
import ontologizer.go.TermContainer;
import ontologizer.types.ByteString;

public class AssociationParserTest extends TestCase
{
	public TermContainer container;
	private AssociationParser assocParser;
	public AssociationContainer assocContainer;
	
	// data for testing
	private String GOAssociationFile = "data/gene_association.sgd_select";
	private int nAnnotatedGenes = 189;
	private int nAssociations = 235;
	private int nSynonyms = 309;
	private int nDBObjects = 189;
	
	private String[] someGenes = {"SRL1", "DDR2", "UFO1"};
	private int[] someGeneTermCounts = {1, 1, 1};
	
	@Override
	protected void setUp() throws Exception
	{
		ParsedContainerTest oboPT = new ParsedContainerTest();
		oboPT.run();
		container = oboPT.container;
		
		assocParser = new AssociationParser(GOAssociationFile, container, null);
			
		assocContainer = new AssociationContainer(assocParser.getAssociations(),
				assocParser.getSynonym2gene(),
				assocParser.getDbObject2gene());
	}


	public void testBasicStructure()
	{
		Assert.assertTrue("number of parsed associations", assocParser.getAssociations().size() == nAssociations);
		Assert.assertTrue("number of parsed synonyms", assocParser.getSynonym2gene().size() == nSynonyms);
		Assert.assertTrue("number of parsed DB objects", assocParser.getDbObject2gene().size() == nDBObjects);
		Assert.assertTrue("number of annotated genes", assocContainer.getAllAnnotatedGenes().size() == nAnnotatedGenes);
		
		for (int i=0; i<someGenes.length; i++) {
//			System.err.println(assocContainer.get(someGenes[i]).getAssociations().size());
			Assert.assertEquals(assocContainer.get(new ByteString(someGenes[i])).getAssociations().size(), someGeneTermCounts[i]);
		}
	}
	
	
}
