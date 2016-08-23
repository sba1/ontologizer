package ontologizer.ontology;

import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Test;

import ontologizer.ontology.ITerm;
import ontologizer.ontology.Namespace;
import ontologizer.ontology.ParentTermID;
import ontologizer.ontology.Prefix;
import ontologizer.ontology.PrefixPool;
import ontologizer.ontology.Term;
import ontologizer.ontology.TermID;
import ontologizer.ontology.TermRelation;

public class TermTest
{
	@Test
	public void test()
	{
		Namespace bNamespace = new Namespace("B");
		Namespace cNamespace = new Namespace("C");
		Namespace fNamespace = new Namespace("F");

		Term root = new Term("GO:0000000", "root");
		ArrayList<ParentTermID> rootlist = new ArrayList<ParentTermID>();
		rootlist.add(new ParentTermID(root.getID(),TermRelation.PART_OF_A));
		Term bioproc = new Term("GO:0008150", "biological process", bNamespace, rootlist);
		Term cellcomp = new Term("GO:0005575", "cellular component", cNamespace, rootlist);
		Term molfunc = new Term("GO:0003674", "molecular function", fNamespace, rootlist);

		/*
		 * Testing getIDAsString
		 */
		Assert.assertTrue(root.getIDAsString().equals("GO:0000000"));
		Assert.assertTrue(bioproc.getIDAsString().equals("GO:0008150"));
		Assert.assertTrue(cellcomp.getIDAsString().equals("GO:0005575"));
		Assert.assertTrue(molfunc.getIDAsString().equals("GO:0003674"));

		/*
		 * Testing getID
		 */
		TermID rootID = new TermID("GO:0000000");
		Assert.assertTrue(root.getID().equals(rootID));
		TermID bioprocID = new TermID("GO:0008150");
		Assert.assertTrue(bioproc.getID().equals(bioprocID));
		TermID cellcompID = new TermID("GO:0005575");
		Assert.assertTrue(cellcomp.getID().equals(cellcompID));
		TermID molfuncID = new TermID("GO:0003674");
		Assert.assertTrue(molfunc.getID().equals(molfuncID));

		/*
		 * Testing getName
		 */
		Assert.assertTrue(root.getName().equals("root"));
		Assert.assertTrue(bioproc.getName().equals("biological process"));
		Assert.assertTrue(cellcomp.getName().equals("cellular component"));
		Assert.assertTrue(molfunc.getName().equals("molecular function"));

		/*
		 * Testing getNamespace
		 */
		Assert.assertTrue(root.getNamespace().equals(Namespace.UNKOWN_NAMESPACE));
		Assert.assertTrue(bioproc.getNamespace().equals(bNamespace));
		Assert.assertTrue(cellcomp.getNamespace().equals(cNamespace));
		Assert.assertTrue(molfunc.getNamespace().equals(fNamespace));
	}

	@Test
	public void testSimpleBuilder()
	{
		ITerm t = Term.name("root").id("GO:0000000").build();
		Assert.assertEquals("root", t.getName().toString());
		Assert.assertEquals(new TermID("GO:0000000").toString(), t.getID().toString());
	}

	@Test
	public void testSimpleBuilderWithPrefixPool()
	{
		PrefixPool pool = new PrefixPool();
		ITerm t = Term.prefixPool(pool).name("root").id("GO:0000000").build();
		Assert.assertEquals("root", t.getName().toString());
		Assert.assertEquals(new TermID("GO:0000000").toString(), t.getID().toString());
		Assert.assertTrue(pool.map(new Prefix(("GO"))) == t.getID().getPrefix());
	}

	@Test
	public void testSimpleBuilderNotObsoleteDefault()
	{
		ITerm obsoleteTerm = Term.name("root").id("GO:0000000").build();
		Assert.assertEquals(false, obsoleteTerm.isObsolete());
	}

	@Test
	public void testSimpleBuilderNotObsolete()
	{
		ITerm obsoleteTerm = Term.name("root").id("GO:0000000").obsolete(false).build();
		Assert.assertEquals(false, obsoleteTerm.isObsolete());
	}

	@Test
	public void testSimpleBuilderObsolte()
	{
		ITerm obsoleteTerm = Term.name("root").id("GO:0000000").obsolete(true).build();
		Assert.assertEquals(true, obsoleteTerm.isObsolete());
	}
}
