package ontologizer.ontology;
import java.util.ArrayList;
import java.util.HashSet;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ontologizer.ontology.Namespace;
import ontologizer.ontology.ParentTermID;
import ontologizer.ontology.Term;
import ontologizer.ontology.TermContainer;
import ontologizer.ontology.TermRelation;


public class TermContainerTest
{
	private TermContainer container;
	private Term root;
	private Term bioproc;
	private Term cellcomp;
	private Term molfunc;


	@Before
	public void setUp() throws Exception
	{
		root = new Term("GO:0000000", "root", null);
		ArrayList<ParentTermID> rootlist = new ArrayList<ParentTermID>();
		rootlist.add(new ParentTermID(root.getID(),TermRelation.PART_OF_A));
		bioproc = new Term("GO:0008150", "biological process", new Namespace("B"), rootlist);
		cellcomp = new Term("GO:0005575", "cellular component", new Namespace("C"), rootlist);
		molfunc = new Term("GO:0003674", "molecular function", new Namespace("F"), rootlist);

		HashSet<Term> termsConstructed = new HashSet<Term>();
		termsConstructed.add(root);
		termsConstructed.add(bioproc);
		termsConstructed.add(cellcomp);
		termsConstructed.add(molfunc);

		container = new TermContainer(termsConstructed, "noformat", "nodate");
	}

	@Test
	public void testBasicStructure()
	{

		Assert.assertTrue(container.termCount() == 4);
		Assert.assertTrue(container.getFormatVersion().equals("noformat"));
		Assert.assertTrue(container.getDate().equals("nodate"));

		Assert.assertTrue(container.getGOName("GO:0000000").equals("root"));
		Assert.assertTrue(container.getGOName(root.getID()).equals("root"));
		Assert.assertTrue(container.getGOName("GO:0008150").equals("biological process"));
		Assert.assertTrue(container.getGOName(bioproc.getID()).equals("biological process"));
		Assert.assertTrue(container.getGOName("GO:0005575").equals("cellular component"));
		Assert.assertTrue(container.getGOName(cellcomp.getID()).equals("cellular component"));
		Assert.assertTrue(container.getGOName("GO:0003674").equals("molecular function"));
		Assert.assertTrue(container.getGOName(molfunc.getID()).equals("molecular function"));

		Assert.assertTrue(container.get("GO:0000000").equals(root));
		Assert.assertTrue(container.get(root.getID()).equals(root));
		Assert.assertTrue(container.get("GO:0008150").equals(bioproc));
		Assert.assertTrue(container.get(bioproc.getID()).equals(bioproc));
		Assert.assertTrue(container.get("GO:0005575").equals(cellcomp));
		Assert.assertTrue(container.get(cellcomp.getID()).equals(cellcomp));
		Assert.assertTrue(container.get("GO:0003674").equals(molfunc));
		Assert.assertTrue(container.get(molfunc.getID()).equals(molfunc));

		Assert.assertTrue(container.get("GO:0000815") == null);
		Term anotherTerm = new Term("GO:0000815", "dummy", null);
		Assert.assertTrue(container.get(anotherTerm.getID()) == null);
	}
}
