package ontologizer.sets.tests;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.HashSet;

import ontologizer.association.Association;
import ontologizer.association.AssociationContainer;
import ontologizer.association.Gene2Associations;
import ontologizer.benchmark.Datafiles;
import ontologizer.dotwriter.AbstractDotAttributesProvider;
import ontologizer.dotwriter.GODOTWriter;
import ontologizer.enumeration.GOTermEnumerator;
import ontologizer.enumeration.GOTermEnumerator.GOTermAnnotatedGenes;
import ontologizer.go.Ontology;
import ontologizer.go.ParentTermID;
import ontologizer.go.Term;
import ontologizer.go.TermContainer;
import ontologizer.go.TermID;
import ontologizer.go.TermRelation;
import ontologizer.set.StudySet;
import ontologizer.types.ByteString;
import sonumina.math.graph.DirectedGraph;
import sonumina.math.graph.Edge;
import sonumina.math.graph.AbstractGraph.DotAttributesProvider;
import junit.framework.Assert;
import junit.framework.TestCase;

class InternalDatafiles extends Datafiles
{
	public InternalDatafiles()
	{
		/* Go Graph */
		HashSet<Term> terms = new HashSet<Term>();
		Term c1 = new Term("GO:0000001", "C1");
		Term c2 = new Term("GO:0000002", "C2", new ParentTermID(c1.getID(),TermRelation.IS_A));
		Term c3 = new Term("GO:0000003", "C3", new ParentTermID(c1.getID(),TermRelation.IS_A));
		Term c4 = new Term("GO:0000004", "C4", new ParentTermID(c2.getID(),TermRelation.IS_A));
		Term c5 = new Term("GO:0000005", "C5", new ParentTermID(c2.getID(),TermRelation.IS_A));
		Term c6 = new Term("GO:0000006", "C6", new ParentTermID(c3.getID(),TermRelation.IS_A),new ParentTermID(c2.getID(),TermRelation.IS_A));
		Term c7 = new Term("GO:0000007", "C7", new ParentTermID(c5.getID(),TermRelation.IS_A),new ParentTermID(c6.getID(),TermRelation.IS_A));
		Term c8 = new Term("GO:0000008", "C8", new ParentTermID(c7.getID(),TermRelation.IS_A));
		Term c9 = new Term("GO:0000009", "C9", new ParentTermID(c7.getID(),TermRelation.IS_A));
		Term c10 = new Term("GO:0000010", "C10", new ParentTermID(c9.getID(),TermRelation.IS_A));
		Term c11 = new Term("GO:0000011", "C11", new ParentTermID(c9.getID(),TermRelation.IS_A));
		Term c12 = new Term("GO:0000012", "C12", new ParentTermID(c8.getID(),TermRelation.IS_A));
		Term c13 = new Term("GO:0000013", "C13", new ParentTermID(c8.getID(),TermRelation.IS_A));
		Term c14 = new Term("GO:0000014", "C14", new ParentTermID(c4.getID(),TermRelation.IS_A));
		Term c15 = new Term("GO:0000015", "C15", new ParentTermID(c4.getID(),TermRelation.IS_A));

		terms.add(c1);
		terms.add(c2);
		terms.add(c3);
		terms.add(c4);
		terms.add(c5);
		terms.add(c6);
		terms.add(c7);
		terms.add(c8);
		terms.add(c9);
		terms.add(c10);
		terms.add(c11);
		terms.add(c12);
		terms.add(c13);
		terms.add(c14);
		terms.add(c15);
		TermContainer termContainer = new TermContainer(terms,"","");

		graph = new Ontology(termContainer);

		HashSet<TermID> tids = new HashSet<TermID>();
		for (Term term : terms)
			tids.add(term.getID());

		/* Associations */
		assoc = new AssociationContainer();

		assoc.addAssociation(new Association(new ByteString("item1"),4));
		assoc.addAssociation(new Association(new ByteString("item1"),11));

		assoc.addAssociation(new Association(new ByteString("item2"),10));
		assoc.addAssociation(new Association(new ByteString("item2"),13));

		assoc.addAssociation(new Association(new ByteString("item3"),7));
		assoc.addAssociation(new Association(new ByteString("item3"),15));

		assoc.addAssociation(new Association(new ByteString("item4"),12));
		assoc.addAssociation(new Association(new ByteString("item4"),13));
		assoc.addAssociation(new Association(new ByteString("item4"),14));

		assoc.addAssociation(new Association(new ByteString("item5"),6));
		assoc.addAssociation(new Association(new ByteString("item5"),14));

		GODOTWriter.writeDOT(graph, new File("example.dot"), null, tids, new AbstractDotAttributesProvider() {
			public String getDotNodeAttributes(TermID id) {

				return "label=\""+graph.getTerm(id).getName()+"\"";
			}
		});

		DirectedGraph<String> graphWithItems = new DirectedGraph<String>();
		for (Term term : terms)
			graphWithItems.addVertex(term.getName());

		for (Term term : terms)
		{
			for (ParentTermID pid : term.getParents())
			{
				graphWithItems.addEdge(new Edge<String>(graph.getTerm(pid.termid).getName(),term.getName()));
			}
		}

		graphWithItems.addVertex("item1");
		graphWithItems.addVertex("item2");
		graphWithItems.addVertex("item3");
		graphWithItems.addVertex("item4");
		graphWithItems.addVertex("item5");

		for (Gene2Associations g2a : assoc)
			for (TermID tid : g2a.getAssociations())
				graphWithItems.addEdge(new Edge<String>(graph.getTerm(tid).getName(),g2a.name().toString()));

		try {
			graphWithItems.writeDOT(new FileOutputStream("full.dot"), new DotAttributesProvider<String>()
					{
						@Override
						public String getDotNodeAttributes(String vt)
						{
							if (vt.startsWith("C"))
								return "label=\""+vt+"\"";
							else
								return "shape=\"box\",label=\""+vt+"\"";
						}

						@Override
						public String getDotEdgeAttributes(String src, String dest)
						{
							return "dir=\"back\"";
						}
					});
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}

public class StudySetTest extends TestCase
{
	public void testEnumerate()
	{
		InternalDatafiles idf = new InternalDatafiles();

		StudySet studySet = new StudySet();
		studySet.addGene(new ByteString("item1"), "");
		studySet.addGene(new ByteString("item2"), "");
		studySet.addGene(new ByteString("item3"), "");
		studySet.addGene(new ByteString("item4"), "");
		studySet.addGene(new ByteString("item5"), "");

		GOTermEnumerator gote = studySet.enumerateGOTerms(idf.graph, idf.assoc);
		Assert.assertEquals(idf.graph.getNumberOfTerms(), gote.getTotalNumberOfAnnotatedTerms());

		assertEquals(5,gote.getAnnotatedGenes(new TermID("GO:0000001")).totalAnnotated.size());
		assertEquals(0,gote.getAnnotatedGenes(new TermID("GO:0000001")).directAnnotated.size());
		assertEquals(4,gote.getAnnotatedGenes(new TermID("GO:0000007")).totalAnnotated.size());
		assertEquals(1,gote.getAnnotatedGenes(new TermID("GO:0000007")).directAnnotated.size());
		assertEquals(2,gote.getAnnotatedGenes(new TermID("GO:0000009")).totalAnnotated.size());
		assertEquals(0,gote.getAnnotatedGenes(new TermID("GO:0000009")).directAnnotated.size());
		assertEquals(1,gote.getAnnotatedGenes(new TermID("GO:0000011")).totalAnnotated.size());
		assertEquals(1,gote.getAnnotatedGenes(new TermID("GO:0000011")).directAnnotated.size());
		assertEquals(1,gote.getAnnotatedGenes(new TermID("GO:0000012")).totalAnnotated.size());
		assertEquals(1,gote.getAnnotatedGenes(new TermID("GO:0000012")).directAnnotated.size());
		assertEquals(2,gote.getAnnotatedGenes(new TermID("GO:0000013")).totalAnnotated.size());
		assertEquals(2,gote.getAnnotatedGenes(new TermID("GO:0000013")).directAnnotated.size());
		assertEquals(2,gote.getAnnotatedGenes(new TermID("GO:0000014")).totalAnnotated.size());
		assertEquals(2,gote.getAnnotatedGenes(new TermID("GO:0000014")).directAnnotated.size());

		/* Remove all terms with less than two annotations */
		gote.removeTerms(new GOTermEnumerator.IRemover() {
			@Override
			public boolean remove(TermID tid, GOTermAnnotatedGenes tag)
			{
				return tag.totalAnnotated.size() < 3;
			}
		});

		assertEquals(7,gote.getTotalNumberOfAnnotatedTerms());

		assertEquals(4,gote.getAnnotatedGenes(new TermID("GO:0000007")).totalAnnotated.size());
		assertEquals(1,gote.getAnnotatedGenes(new TermID("GO:0000007")).directAnnotated.size());
		assertEquals(0,gote.getAnnotatedGenes(new TermID("GO:0000014")).totalAnnotated.size());
		assertEquals(0,gote.getAnnotatedGenes(new TermID("GO:0000014")).directAnnotated.size());

	}
}
