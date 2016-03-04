package ontologizer.go;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ontologizer.go.Ontology.IVisitingGOVertex;

public class OntologyTest
{
	private TermContainer goTerms;

	private Ontology graph;

	@Before
	public void setUp() throws Exception
	{
		String GOtermsOBOFile = "data/gene_ontology.1_2.obo.gz";

		/* Parse file and create term container */
		System.out.println("Parse OBO file");
		OBOParser oboParser = new OBOParser(new OBOParserFileInput(GOtermsOBOFile));
		System.out.println(oboParser.doParse());
		goTerms = new TermContainer(oboParser.getTermMap(), oboParser.getFormatVersion(), oboParser.getDate());

		/* Build graph */
		System.out.println("Build graph");
		graph = Ontology.create(goTerms);
	}

	@Test
	public void testRoot()
	{
		Set<String> terms;

		terms = graph.getTermChildrenAsStrings("GO:0000000");
		Assert.assertTrue("Test we get some Set<String> object back", terms != null);
		Assert.assertEquals("Root has three descendants",3, terms.size());

		terms = graph.getTermParentsAsStrings("GO:0000000");
		Assert.assertTrue("Test we gat some Set<String> object back", terms != null);
		Assert.assertTrue("Root has no ancestors", terms.size() == 0);
	}

	@Test
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

	@Test
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
				System.out.println(term + " " + count);
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
		 * verify them then via www.godatabase.org.
		 */
		graph.walkToSource(new TermID("GO:0008152"), vistingGOVertex);
		Assert.assertEquals(3,vistingGOVertex.getCount());
		vistingGOVertex.resetCount();

		graph.walkToSource(new TermID("GO:0044237"), vistingGOVertex);
		Assert.assertEquals(5,vistingGOVertex.getCount());
		vistingGOVertex.resetCount();

		graph.walkToSource(new TermID("GO:0006281"), vistingGOVertex);
		Assert.assertEquals(19,vistingGOVertex.getCount());
	}

	/**
	 * Return a termid collection from strings.
	 *
	 * @param ids
	 * @return
	 */
	private static Collection<TermID> newTermIDCollection(String...ids)
	{
		HashSet<TermID> hs = new HashSet<TermID>();
		for (String id : ids)
			hs.add(new TermID(id));
		return hs;
	}

	@Test
	public void testGetSharedParentsSimple()
	{
		Term t = graph.getTerm("GO:0008152");
		Collection<TermID> parents = graph.getSharedParents(t.getID(), t.getID());
		Assert.assertEquals(3,  parents.size());
		Assert.assertTrue(parents.containsAll(newTermIDCollection("GO:0008152", "GO:0008150", "GO:0000000")));
	}

	@Test
	public void testGetSharedParents()
	{
		Term t1 = graph.getTerm("GO:0008152");
		Term t2 = graph.getTerm("GO:0008150");
		Collection<TermID> parents = graph.getSharedParents(t1.getID(), t2.getID());
		Assert.assertEquals(2,  parents.size());
		Assert.assertTrue(parents.containsAll(newTermIDCollection("GO:0008150", "GO:0000000")));
	}

	@Test
	public void testGetSharedParentsTwoLeaves()
	{
		Term t1 = graph.getTerm("GO:0034641");
		Term t2 = graph.getTerm("GO:0019326");
		Collection<TermID> actual = graph.getSharedParents(t1.getID(), t2.getID());

		Collection<TermID> expected = graph.getTermsOfInducedGraph(null, t1.getID());
		expected.retainAll(graph.getTermsOfInducedGraph(null, t2.getID()));
		Assert.assertEquals(expected.size(),  actual.size());
		Assert.assertTrue(actual.containsAll(expected));
	}

	@Test
	public void testRelevantSubontology()
	{
		graph.setRelevantSubontology("biological_process");

		Ontology biologicalProcess = graph.getOntlogyOfRelevantTerms();
		Assert.assertEquals(21763, biologicalProcess.getNumberOfTerms());
		Assert.assertEquals("biological_process", biologicalProcess.getRootTerm().getName());

		Namespace n = biologicalProcess.getRootTerm().getNamespace();
		String nn = n.getName();
		Assert.assertEquals("biological_process", nn);

		for (Term t : biologicalProcess)
			Assert.assertEquals(nn, t.getNamespace().getName());
	}

	private static Set<String> subsetNames(Collection<Subset> subsets)
	{
		HashSet<String> names = new HashSet<String>();
		for (Subset s : subsets)
			names.add(s.getName());
		return names;
	}

	private static Set<String> subsetNames(Subset [] subsets)
	{
		HashSet<String> names = new HashSet<String>();
		for (Subset s : subsets)
			names.add(s.getName());
		return names;
	}

	@Test
	public void testSubsetOntology()
	{
		Set<String> expectedSubsets = new HashSet<String>(
			Arrays.asList(
				"goslim_aspergillus",
				"goslim_candida",
				"goslim_generic",
				"goslim_pir",
				"goslim_plant",
				"goslim_pombe",
				"goslim_yeast",
				"gosubset_prok",
				"high_level_annotation_qc",
				"mf_needs_review"
				));
		Assert.assertEquals(expectedSubsets, subsetNames(graph.getAvailableSubsets()));

		graph.setRelevantSubset("goslim_generic");
		Ontology goslim = graph.getOntlogyOfRelevantTerms();

		int expectedGoSlimTerms = 0;
		for (Term t : graph)
		{
			if (subsetNames(t.getSubsets()).contains("goslim_generic"))
				expectedGoSlimTerms++;
		}
		Assert.assertEquals(expectedGoSlimTerms, goslim.getNumberOfTerms());
	}
}
