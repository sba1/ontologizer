package ontologizer.go;

import java.util.Set;

import ontologizer.go.GOGraph.IVisitingGOVertex;

import junit.framework.Assert;
import junit.framework.TestCase;

public class GOGraphTest extends TestCase
{
	private TermContainer goTerms;

	private GOGraph graph;

	@Override
	protected void setUp() throws Exception
	{
		String GOtermsOBOFile = "data/GO_test.obo";

		/* Parse file and create term container */
		System.out.println("Parse OBO file");
		OBOParser oboParser = new OBOParser(GOtermsOBOFile);
		System.out.println(oboParser.doParse());
		goTerms = new TermContainer(oboParser.getTermMap(), oboParser.getFormatVersion(), oboParser.getDate());

		/* Build graph */
		System.out.println("Build graph");
		graph = new GOGraph(goTerms);
	}

	public void testRoot()
	{
		Set<String> terms;

		terms = graph.getTermsDescendantsAsStrings("GO:0000000");
		Assert.assertTrue("Test we get some Set<String> object back", terms != null);
		Assert.assertTrue("Root has three descendants", terms.size() == 3);

		terms = graph.getTermsAncestorsAsStrings("GO:0000000");
		Assert.assertTrue("Test we gat some Set<String> object back", terms != null);
		Assert.assertTrue("Root has no ancestors", terms.size() == 0);
	}

	public void testExistsPath()
	{
		Assert.assertTrue(graph.existsPath(new TermID("GO:0009987"),
				new TermID("GO:0006281")));
		Assert.assertFalse(graph.existsPath(new TermID("GO:0006281"),
				new TermID("GO:0009987")));

		Assert.assertTrue(graph.existsPath(new TermID("GO:0008150"),
				new TermID("GO:0006281")));
		Assert.assertFalse(graph.existsPath(new TermID("GO:0006281"),
				new TermID("GO:0008150")));

		Assert.assertFalse(graph.existsPath(new TermID("GO:0006139"),
				new TermID("GO:0009719")));
		Assert.assertFalse(graph.existsPath(new TermID("GO:0009719"),
				new TermID("GO:0006139")));
	}

	public void testWalkToRoot()
	{
		/**
		 * A basic visitor: It simply counts up the number of visisted terms.
		 * 
		 * @author Sebastian Bauer
		 */
		class VisitingGOVertex implements IVisitingGOVertex
		{
			public int count = 0;

			public boolean visited(Term term)
			{
				count++;
				return true;
			}

			public void resetCount()
			{
				count = 0;
			};

			public int getCount()
			{
				return count;
			};
		}
		;

		VisitingGOVertex vistingGOVertex = new VisitingGOVertex();

		/*
		 * Note, if GO changes these values are no longer correct. But you can
		 * verfiy them then via www.godatabase.org.
		 */
		graph.walkToSource(new TermID("GO:0008152"), vistingGOVertex);
		Assert.assertTrue(vistingGOVertex.getCount() == 4);
		vistingGOVertex.resetCount();

		graph.walkToSource(new TermID("GO:0044237"), vistingGOVertex);
		Assert.assertTrue(vistingGOVertex.getCount() == 7);
		vistingGOVertex.resetCount();

		graph.walkToSource(new TermID("GO:0006281"), vistingGOVertex);
		Assert.assertTrue(vistingGOVertex.getCount() == 17);
	}
}
