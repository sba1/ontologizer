package ontologizer.sets.tests;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;

import org.junit.Assert;
import org.junit.Test;

import ontologizer.association.Association;
import ontologizer.association.AssociationContainer;
import ontologizer.association.AssociationParser;
import ontologizer.association.Gene2Associations;
import ontologizer.benchmark.Datafiles;
import ontologizer.calculation.AbstractGOTermProperties;
import ontologizer.calculation.EnrichedGOTermsResult;
import ontologizer.calculation.TermForTermCalculation;
import ontologizer.dotwriter.AbstractDotAttributesProvider;
import ontologizer.dotwriter.GODOTWriter;
import ontologizer.enumeration.GOTermEnumerator;
import ontologizer.enumeration.GOTermEnumerator.GOTermAnnotatedGenes;
import ontologizer.go.OBOParser;
import ontologizer.go.OBOParserException;
import ontologizer.go.OBOParserFileInput;
import ontologizer.go.OBOParserTest;
import ontologizer.go.Ontology;
import ontologizer.go.ParentTermID;
import ontologizer.go.Term;
import ontologizer.go.TermContainer;
import ontologizer.go.TermID;
import ontologizer.go.TermRelation;
import ontologizer.set.PopulationSet;
import ontologizer.set.StudySet;
import ontologizer.statistics.None;
import ontologizer.types.ByteString;
import sonumina.math.graph.AbstractGraph.DotAttributesProvider;
import sonumina.math.graph.DirectedGraph;
import sonumina.math.graph.Edge;

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

		graph = Ontology.create(termContainer);

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

public class StudySetTest
{
	private final static String GOAssociationFile = "data/gene_association.sgd.gz";

	@Test
	public void testEnumerateWithNonExistentTerms() throws IOException, OBOParserException
	{
		// FIXME: Duplicated partly from AssociationParserTest2
		OBOParser oboParser = new OBOParser(new OBOParserFileInput(OBOParserTest.GOtermsOBOFile));
		oboParser.doParse();
		TermContainer container = new TermContainer(oboParser.getTermMap(), oboParser.getFormatVersion(), oboParser.getDate());
		Ontology o = Ontology.create(container);

		AssociationContainer assoc = new AssociationContainer();
		Association a = new Association(new ByteString("Test"), "TEST:0000000");
		assoc.addAssociation(a);

		StudySet study = new StudySet();
		study.addGene(new ByteString("Test"), "");

		try
		{
			study.enumerateGOTerms(o,  assoc);
			Assert.assertTrue(true);
		} catch (IllegalArgumentException iae)
		{
		}
	}

	@Test
	public void testEnumerateOnInternal()
	{
		InternalDatafiles idf = new InternalDatafiles();

		StudySet studySet = new StudySet();
		studySet.addGene(new ByteString("item1"), "");
		studySet.addGene(new ByteString("item2"), "");
		studySet.addGene(new ByteString("item3"), "");
		studySet.addGene(new ByteString("item4"), "");
		studySet.addGene(new ByteString("item5"), "");

		PopulationSet populationSet = new PopulationSet();
		populationSet.addGene(new ByteString("item1"), "");
		populationSet.addGene(new ByteString("item2"), "");
		populationSet.addGene(new ByteString("item3"), "");
		populationSet.addGene(new ByteString("item4"), "");
		populationSet.addGene(new ByteString("item5"), "");


		GOTermEnumerator gote = populationSet.enumerateGOTerms(idf.graph, idf.assoc);
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

		TermForTermCalculation tft = new TermForTermCalculation();
		EnrichedGOTermsResult result = tft.calculateStudySet(idf.graph, idf.assoc, populationSet, studySet, new None());
		int number = 0;
		for (AbstractGOTermProperties prop : result)
			number++;
		assertEquals(15,number);

		/* Remove all terms with less than two annotations */
		gote.removeTerms(new GOTermEnumerator.IRemover() {
			@Override
			public boolean remove(TermID tid, GOTermAnnotatedGenes tag)
			{
				return tag.totalAnnotated.size() < 3;
			}
		});
		populationSet.resetCounterAndEnumerator();

		assertEquals(7,gote.getTotalNumberOfAnnotatedTerms());

		assertEquals(4,gote.getAnnotatedGenes(new TermID("GO:0000007")).totalAnnotated.size());
		assertEquals(1,gote.getAnnotatedGenes(new TermID("GO:0000007")).directAnnotated.size());
		assertEquals(0,gote.getAnnotatedGenes(new TermID("GO:0000014")).totalAnnotated.size());
		assertEquals(0,gote.getAnnotatedGenes(new TermID("GO:0000014")).directAnnotated.size());

//		tft = new TermForTermCalculation();
//		result = tft.calculateStudySet(idf.graph, idf.assoc, populationSet, studySet, new None());
//		number = 0;
//		for (AbstractGOTermProperties prop : result)
//			number++;
//
//		assertEquals(7,number);
	}

	@Test
	public void testEnumerateOnExternal() throws IOException, OBOParserException
	{
		// FIXME: Duplicated partly from AssociationParserTest2
		OBOParser oboParser = new OBOParser(new OBOParserFileInput(OBOParserTest.GOtermsOBOFile));
		oboParser.doParse();
		TermContainer container = new TermContainer(oboParser.getTermMap(), oboParser.getFormatVersion(), oboParser.getDate());
		Ontology o = Ontology.create(container);
		AssociationParser assocParser = new AssociationParser(new OBOParserFileInput(GOAssociationFile), container, null);

		AssociationContainer assocContainer = new AssociationContainer(assocParser.getAssociations(),
				assocParser.getSynonym2gene(),
				assocParser.getDbObject2gene());

		StudySet s = new StudySet();
		s.addGenes(assocContainer.getAllAnnotatedGenes());

		TermID rootTerm = o.getRootTerm().getID();
		Assert.assertEquals(rootTerm,new TermID("GO:0000000"));

		GOTermEnumerator e = s.enumerateGOTerms(o,assocContainer);
		Assert.assertEquals(6721,e.getTotalNumberOfAnnotatedTerms());
		Assert.assertEquals(assocContainer.getAllAnnotatedGenes().size(),e.getGenes().size());
		Assert.assertEquals(assocContainer.getAllAnnotatedGenes().size(),e.getAnnotatedGenes(rootTerm).totalAnnotated.size());

		/* The following has been generated! It is used as a "fingerprint" to track any possible changes */
		Assert.assertEquals(2,e.getAnnotatedGenes(new TermID("GO:0070180")).totalAnnotatedCount());
		Assert.assertEquals(17,e.getAnnotatedGenes(new TermID("GO:0034976")).totalAnnotatedCount());
		Assert.assertEquals(18,e.getAnnotatedGenes(new TermID("GO:0000291")).totalAnnotatedCount());
		Assert.assertEquals(4,e.getAnnotatedGenes(new TermID("GO:0070181")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0034983")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0070178")).totalAnnotatedCount());
		Assert.assertEquals(5,e.getAnnotatedGenes(new TermID("GO:0034982")).totalAnnotatedCount());
		Assert.assertEquals(8,e.getAnnotatedGenes(new TermID("GO:0000293")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0000294")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0000295")).totalAnnotatedCount());
		Assert.assertEquals(4,e.getAnnotatedGenes(new TermID("GO:0000296")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0070191")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0034986")).totalAnnotatedCount());
		Assert.assertEquals(4,e.getAnnotatedGenes(new TermID("GO:0000297")).totalAnnotatedCount());
		Assert.assertEquals(2,e.getAnnotatedGenes(new TermID("GO:0000298")).totalAnnotatedCount());
		Assert.assertEquals(4,e.getAnnotatedGenes(new TermID("GO:0000299")).totalAnnotatedCount());
		Assert.assertEquals(19,e.getAnnotatedGenes(new TermID("GO:0000300")).totalAnnotatedCount());
		Assert.assertEquals(4,e.getAnnotatedGenes(new TermID("GO:0034990")).totalAnnotatedCount());
		Assert.assertEquals(6,e.getAnnotatedGenes(new TermID("GO:0000301")).totalAnnotatedCount());
		Assert.assertEquals(18,e.getAnnotatedGenes(new TermID("GO:0000302")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0070184")).totalAnnotatedCount());
		Assert.assertEquals(6,e.getAnnotatedGenes(new TermID("GO:0000303")).totalAnnotatedCount());
		Assert.assertEquals(2,e.getAnnotatedGenes(new TermID("GO:0000339")).totalAnnotatedCount());
		Assert.assertEquals(2,e.getAnnotatedGenes(new TermID("GO:0035024")).totalAnnotatedCount());
		Assert.assertEquals(6,e.getAnnotatedGenes(new TermID("GO:0000338")).totalAnnotatedCount());
		Assert.assertEquals(9,e.getAnnotatedGenes(new TermID("GO:0000350")).totalAnnotatedCount());
		Assert.assertEquals(8,e.getAnnotatedGenes(new TermID("GO:0000349")).totalAnnotatedCount());
		Assert.assertEquals(3,e.getAnnotatedGenes(new TermID("GO:0000348")).totalAnnotatedCount());
		Assert.assertEquals(4,e.getAnnotatedGenes(new TermID("GO:0000347")).totalAnnotatedCount());
		Assert.assertEquals(7,e.getAnnotatedGenes(new TermID("GO:0000346")).totalAnnotatedCount());
		Assert.assertEquals(13,e.getAnnotatedGenes(new TermID("GO:0070127")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0070126")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0000824")).totalAnnotatedCount());
		Assert.assertEquals(2,e.getAnnotatedGenes(new TermID("GO:0000827")).totalAnnotatedCount());
		Assert.assertEquals(13,e.getAnnotatedGenes(new TermID("GO:0000921")).totalAnnotatedCount());
		Assert.assertEquals(4,e.getAnnotatedGenes(new TermID("GO:0000916")).totalAnnotatedCount());
		Assert.assertEquals(4,e.getAnnotatedGenes(new TermID("GO:0016531")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0071508")).totalAnnotatedCount());
		Assert.assertEquals(10,e.getAnnotatedGenes(new TermID("GO:0016584")).totalAnnotatedCount());
		Assert.assertEquals(4,e.getAnnotatedGenes(new TermID("GO:0016587")).totalAnnotatedCount());
		Assert.assertEquals(18,e.getAnnotatedGenes(new TermID("GO:0016586")).totalAnnotatedCount());
		Assert.assertEquals(74,e.getAnnotatedGenes(new TermID("GO:0016591")).totalAnnotatedCount());
		Assert.assertEquals(4,e.getAnnotatedGenes(new TermID("GO:0016577")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0016576")).totalAnnotatedCount());
		Assert.assertEquals(3,e.getAnnotatedGenes(new TermID("GO:0001025")).totalAnnotatedCount());
		Assert.assertEquals(3,e.getAnnotatedGenes(new TermID("GO:0001026")).totalAnnotatedCount());
		Assert.assertEquals(42,e.getAnnotatedGenes(new TermID("GO:0071428")).totalAnnotatedCount());
		Assert.assertEquals(23,e.getAnnotatedGenes(new TermID("GO:0016579")).totalAnnotatedCount());
		Assert.assertEquals(4,e.getAnnotatedGenes(new TermID("GO:0016578")).totalAnnotatedCount());
		Assert.assertEquals(42,e.getAnnotatedGenes(new TermID("GO:0071426")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0001030")).totalAnnotatedCount());
		Assert.assertEquals(4,e.getAnnotatedGenes(new TermID("GO:0071454")).totalAnnotatedCount());
		Assert.assertEquals(8,e.getAnnotatedGenes(new TermID("GO:0071453")).totalAnnotatedCount());
		Assert.assertEquals(4,e.getAnnotatedGenes(new TermID("GO:0016602")).totalAnnotatedCount());
		Assert.assertEquals(6,e.getAnnotatedGenes(new TermID("GO:0071451")).totalAnnotatedCount());
		Assert.assertEquals(6,e.getAnnotatedGenes(new TermID("GO:0071450")).totalAnnotatedCount());
		Assert.assertEquals(23,e.getAnnotatedGenes(new TermID("GO:0016592")).totalAnnotatedCount());
		Assert.assertEquals(17,e.getAnnotatedGenes(new TermID("GO:0051539")).totalAnnotatedCount());
		Assert.assertEquals(7,e.getAnnotatedGenes(new TermID("GO:0016593")).totalAnnotatedCount());
		Assert.assertEquals(2,e.getAnnotatedGenes(new TermID("GO:0051538")).totalAnnotatedCount());
		Assert.assertEquals(13,e.getAnnotatedGenes(new TermID("GO:0051537")).totalAnnotatedCount());
		Assert.assertEquals(13,e.getAnnotatedGenes(new TermID("GO:0071444")).totalAnnotatedCount());
		Assert.assertEquals(33,e.getAnnotatedGenes(new TermID("GO:0051536")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0001042")).totalAnnotatedCount());
		Assert.assertEquals(8,e.getAnnotatedGenes(new TermID("GO:0016597")).totalAnnotatedCount());
		Assert.assertEquals(2,e.getAnnotatedGenes(new TermID("GO:0071441")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0016598")).totalAnnotatedCount());
		Assert.assertEquals(4,e.getAnnotatedGenes(new TermID("GO:0001047")).totalAnnotatedCount());
		Assert.assertEquals(33,e.getAnnotatedGenes(new TermID("GO:0051540")).totalAnnotatedCount());
		Assert.assertEquals(2,e.getAnnotatedGenes(new TermID("GO:0071440")).totalAnnotatedCount());
		Assert.assertEquals(4,e.getAnnotatedGenes(new TermID("GO:0001046")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0016619")).totalAnnotatedCount());
		Assert.assertEquals(2,e.getAnnotatedGenes(new TermID("GO:0071468")).totalAnnotatedCount());
		Assert.assertEquals(73,e.getAnnotatedGenes(new TermID("GO:0001067")).totalAnnotatedCount());
		Assert.assertEquals(3,e.getAnnotatedGenes(new TermID("GO:0071469")).totalAnnotatedCount());
		Assert.assertEquals(31,e.getAnnotatedGenes(new TermID("GO:0071470")).totalAnnotatedCount());
		Assert.assertEquals(88,e.getAnnotatedGenes(new TermID("GO:0016616")).totalAnnotatedCount());
		Assert.assertEquals(2,e.getAnnotatedGenes(new TermID("GO:0071464")).totalAnnotatedCount());
		Assert.assertEquals(193,e.getAnnotatedGenes(new TermID("GO:0001071")).totalAnnotatedCount());
		Assert.assertEquals(2,e.getAnnotatedGenes(new TermID("GO:0001068")).totalAnnotatedCount());
		Assert.assertEquals(3,e.getAnnotatedGenes(new TermID("GO:0071466")).totalAnnotatedCount());
		Assert.assertEquals(2,e.getAnnotatedGenes(new TermID("GO:0001069")).totalAnnotatedCount());
		Assert.assertEquals(20,e.getAnnotatedGenes(new TermID("GO:0016620")).totalAnnotatedCount());
		Assert.assertEquals(3,e.getAnnotatedGenes(new TermID("GO:0071467")).totalAnnotatedCount());
		Assert.assertEquals(2,e.getAnnotatedGenes(new TermID("GO:0071462")).totalAnnotatedCount());
		Assert.assertEquals(4,e.getAnnotatedGenes(new TermID("GO:0016615")).totalAnnotatedCount());
		Assert.assertEquals(4,e.getAnnotatedGenes(new TermID("GO:0071456")).totalAnnotatedCount());
		Assert.assertEquals(91,e.getAnnotatedGenes(new TermID("GO:0016614")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0071458")).totalAnnotatedCount());
		Assert.assertEquals(5,e.getAnnotatedGenes(new TermID("GO:0071459")).totalAnnotatedCount());
		Assert.assertEquals(10,e.getAnnotatedGenes(new TermID("GO:0001083")).totalAnnotatedCount());
		Assert.assertEquals(4,e.getAnnotatedGenes(new TermID("GO:0016634")).totalAnnotatedCount());
		Assert.assertEquals(7,e.getAnnotatedGenes(new TermID("GO:0016635")).totalAnnotatedCount());
		Assert.assertEquals(5,e.getAnnotatedGenes(new TermID("GO:0001082")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0001081")).totalAnnotatedCount());
		Assert.assertEquals(6,e.getAnnotatedGenes(new TermID("GO:0001080")).totalAnnotatedCount());
		Assert.assertEquals(11,e.getAnnotatedGenes(new TermID("GO:0016638")).totalAnnotatedCount());
		Assert.assertEquals(4,e.getAnnotatedGenes(new TermID("GO:0016639")).totalAnnotatedCount());
		Assert.assertEquals(29,e.getAnnotatedGenes(new TermID("GO:0001085")).totalAnnotatedCount());
		Assert.assertEquals(2,e.getAnnotatedGenes(new TermID("GO:0071482")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0001084")).totalAnnotatedCount());
		Assert.assertEquals(5,e.getAnnotatedGenes(new TermID("GO:0051569")).totalAnnotatedCount());
		Assert.assertEquals(16,e.getAnnotatedGenes(new TermID("GO:0001075")).totalAnnotatedCount());
		Assert.assertEquals(17,e.getAnnotatedGenes(new TermID("GO:0051568")).totalAnnotatedCount());
		Assert.assertEquals(29,e.getAnnotatedGenes(new TermID("GO:0016627")).totalAnnotatedCount());
		Assert.assertEquals(2,e.getAnnotatedGenes(new TermID("GO:0071476")).totalAnnotatedCount());
		Assert.assertEquals(2,e.getAnnotatedGenes(new TermID("GO:0051571")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0001073")).totalAnnotatedCount());
		Assert.assertEquals(4,e.getAnnotatedGenes(new TermID("GO:0016624")).totalAnnotatedCount());
		Assert.assertEquals(2,e.getAnnotatedGenes(new TermID("GO:0071478")).totalAnnotatedCount());
		Assert.assertEquals(8,e.getAnnotatedGenes(new TermID("GO:0001079")).totalAnnotatedCount());
		Assert.assertEquals(5,e.getAnnotatedGenes(new TermID("GO:0071472")).totalAnnotatedCount());
		Assert.assertEquals(14,e.getAnnotatedGenes(new TermID("GO:0001078")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0016631")).totalAnnotatedCount());
		Assert.assertEquals(12,e.getAnnotatedGenes(new TermID("GO:0016628")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0051575")).totalAnnotatedCount());
		Assert.assertEquals(41,e.getAnnotatedGenes(new TermID("GO:0001077")).totalAnnotatedCount());
		Assert.assertEquals(5,e.getAnnotatedGenes(new TermID("GO:0071475")).totalAnnotatedCount());
		Assert.assertEquals(49,e.getAnnotatedGenes(new TermID("GO:0001076")).totalAnnotatedCount());
		Assert.assertEquals(7,e.getAnnotatedGenes(new TermID("GO:0071474")).totalAnnotatedCount());
		Assert.assertEquals(4,e.getAnnotatedGenes(new TermID("GO:0071619")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0071618")).totalAnnotatedCount());
		Assert.assertEquals(5,e.getAnnotatedGenes(new TermID("GO:0071617")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0071627")).totalAnnotatedCount());
		Assert.assertEquals(2,e.getAnnotatedGenes(new TermID("GO:0051599")).totalAnnotatedCount());
		Assert.assertEquals(7,e.getAnnotatedGenes(new TermID("GO:0051598")).totalAnnotatedCount());
		Assert.assertEquals(7,e.getAnnotatedGenes(new TermID("GO:0051597")).totalAnnotatedCount());
		Assert.assertEquals(3,e.getAnnotatedGenes(new TermID("GO:0051596")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0051595")).totalAnnotatedCount());
		Assert.assertEquals(3,e.getAnnotatedGenes(new TermID("GO:0051594")).totalAnnotatedCount());
		Assert.assertEquals(2,e.getAnnotatedGenes(new TermID("GO:0071630")).totalAnnotatedCount());
		Assert.assertEquals(4,e.getAnnotatedGenes(new TermID("GO:0071629")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0051592")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0071628")).totalAnnotatedCount());
		Assert.assertEquals(4,e.getAnnotatedGenes(new TermID("GO:0016405")).totalAnnotatedCount());
		Assert.assertEquals(3,e.getAnnotatedGenes(new TermID("GO:0051606")).totalAnnotatedCount());
		Assert.assertEquals(62,e.getAnnotatedGenes(new TermID("GO:0016407")).totalAnnotatedCount());
		Assert.assertEquals(47,e.getAnnotatedGenes(new TermID("GO:0051604")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0071633")).totalAnnotatedCount());
		Assert.assertEquals(3,e.getAnnotatedGenes(new TermID("GO:0016406")).totalAnnotatedCount());
		Assert.assertEquals(212,e.getAnnotatedGenes(new TermID("GO:0051603")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0051601")).totalAnnotatedCount());
		Assert.assertEquals(13,e.getAnnotatedGenes(new TermID("GO:0016413")).totalAnnotatedCount());
		Assert.assertEquals(2,e.getAnnotatedGenes(new TermID("GO:0016415")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0016414")).totalAnnotatedCount());
		Assert.assertEquals(12,e.getAnnotatedGenes(new TermID("GO:0016409")).totalAnnotatedCount());
		Assert.assertEquals(6,e.getAnnotatedGenes(new TermID("GO:0016408")).totalAnnotatedCount());
		Assert.assertEquals(4,e.getAnnotatedGenes(new TermID("GO:0016411")).totalAnnotatedCount());
		Assert.assertEquals(51,e.getAnnotatedGenes(new TermID("GO:0016410")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0016422")).totalAnnotatedCount());
		Assert.assertEquals(8,e.getAnnotatedGenes(new TermID("GO:0016423")).totalAnnotatedCount());
		Assert.assertEquals(2,e.getAnnotatedGenes(new TermID("GO:0016420")).totalAnnotatedCount());
		Assert.assertEquals(2,e.getAnnotatedGenes(new TermID("GO:0016421")).totalAnnotatedCount());
		Assert.assertEquals(2,e.getAnnotatedGenes(new TermID("GO:0016418")).totalAnnotatedCount());
		Assert.assertEquals(2,e.getAnnotatedGenes(new TermID("GO:0016419")).totalAnnotatedCount());
		Assert.assertEquals(12,e.getAnnotatedGenes(new TermID("GO:0016417")).totalAnnotatedCount());
		Assert.assertEquals(2,e.getAnnotatedGenes(new TermID("GO:0016428")).totalAnnotatedCount());
		Assert.assertEquals(2,e.getAnnotatedGenes(new TermID("GO:0016429")).totalAnnotatedCount());
		Assert.assertEquals(2,e.getAnnotatedGenes(new TermID("GO:0016426")).totalAnnotatedCount());
		Assert.assertEquals(3,e.getAnnotatedGenes(new TermID("GO:0016427")).totalAnnotatedCount());
		Assert.assertEquals(2,e.getAnnotatedGenes(new TermID("GO:0016436")).totalAnnotatedCount());
		Assert.assertEquals(3,e.getAnnotatedGenes(new TermID("GO:0016435")).totalAnnotatedCount());
		Assert.assertEquals(2,e.getAnnotatedGenes(new TermID("GO:0016433")).totalAnnotatedCount());
		Assert.assertEquals(2,e.getAnnotatedGenes(new TermID("GO:0051645")).totalAnnotatedCount());
		Assert.assertEquals(32,e.getAnnotatedGenes(new TermID("GO:0051646")).totalAnnotatedCount());
		Assert.assertEquals(23,e.getAnnotatedGenes(new TermID("GO:0051647")).totalAnnotatedCount());
		Assert.assertEquals(118,e.getAnnotatedGenes(new TermID("GO:0051640")).totalAnnotatedCount());
		Assert.assertEquals(809,e.getAnnotatedGenes(new TermID("GO:0051641")).totalAnnotatedCount());
		Assert.assertEquals(43,e.getAnnotatedGenes(new TermID("GO:0051651")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0001153")).totalAnnotatedCount());
		Assert.assertEquals(3,e.getAnnotatedGenes(new TermID("GO:0016813")).totalAnnotatedCount());
		Assert.assertEquals(58,e.getAnnotatedGenes(new TermID("GO:0051247")).totalAnnotatedCount());
		Assert.assertEquals(3,e.getAnnotatedGenes(new TermID("GO:0016812")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0016815")).totalAnnotatedCount());
		Assert.assertEquals(19,e.getAnnotatedGenes(new TermID("GO:0016814")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0071279")).totalAnnotatedCount());
		Assert.assertEquals(34,e.getAnnotatedGenes(new TermID("GO:0016811")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0071277")).totalAnnotatedCount());
		Assert.assertEquals(64,e.getAnnotatedGenes(new TermID("GO:0016810")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0071266")).totalAnnotatedCount());
		Assert.assertEquals(2,e.getAnnotatedGenes(new TermID("GO:0016805")).totalAnnotatedCount());
		Assert.assertEquals(3,e.getAnnotatedGenes(new TermID("GO:0051238")).totalAnnotatedCount());
		Assert.assertEquals(6,e.getAnnotatedGenes(new TermID("GO:0071267")).totalAnnotatedCount());
		Assert.assertEquals(121,e.getAnnotatedGenes(new TermID("GO:0051236")).totalAnnotatedCount());
		Assert.assertEquals(7,e.getAnnotatedGenes(new TermID("GO:0071265")).totalAnnotatedCount());
		Assert.assertEquals(2,e.getAnnotatedGenes(new TermID("GO:0051237")).totalAnnotatedCount());
		Assert.assertEquals(1284,e.getAnnotatedGenes(new TermID("GO:0051234")).totalAnnotatedCount());
		Assert.assertEquals(3,e.getAnnotatedGenes(new TermID("GO:0016801")).totalAnnotatedCount());
		Assert.assertEquals(48,e.getAnnotatedGenes(new TermID("GO:0051235")).totalAnnotatedCount());
		Assert.assertEquals(2,e.getAnnotatedGenes(new TermID("GO:0016803")).totalAnnotatedCount());
		Assert.assertEquals(8,e.getAnnotatedGenes(new TermID("GO:0051233")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0016802")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0001307")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0051289")).totalAnnotatedCount());
		Assert.assertEquals(8,e.getAnnotatedGenes(new TermID("GO:0001306")).totalAnnotatedCount());
		Assert.assertEquals(17,e.getAnnotatedGenes(new TermID("GO:0016859")).totalAnnotatedCount());
		Assert.assertEquals(3,e.getAnnotatedGenes(new TermID("GO:0016857")).totalAnnotatedCount());
		Assert.assertEquals(8,e.getAnnotatedGenes(new TermID("GO:0001304")).totalAnnotatedCount());
		Assert.assertEquals(12,e.getAnnotatedGenes(new TermID("GO:0051293")).totalAnnotatedCount());
		Assert.assertEquals(5,e.getAnnotatedGenes(new TermID("GO:0016862")).totalAnnotatedCount());
		Assert.assertEquals(5,e.getAnnotatedGenes(new TermID("GO:0051292")).totalAnnotatedCount());
		Assert.assertEquals(4,e.getAnnotatedGenes(new TermID("GO:0016863")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0001310")).totalAnnotatedCount());
		Assert.assertEquals(16,e.getAnnotatedGenes(new TermID("GO:0016860")).totalAnnotatedCount());
		Assert.assertEquals(12,e.getAnnotatedGenes(new TermID("GO:0051294")).totalAnnotatedCount());
		Assert.assertEquals(7,e.getAnnotatedGenes(new TermID("GO:0016861")).totalAnnotatedCount());
		Assert.assertEquals(7,e.getAnnotatedGenes(new TermID("GO:0001308")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0016849")).totalAnnotatedCount());
		Assert.assertEquals(3,e.getAnnotatedGenes(new TermID("GO:0016854")).totalAnnotatedCount());
		Assert.assertEquals(46,e.getAnnotatedGenes(new TermID("GO:0001302")).totalAnnotatedCount());
		Assert.assertEquals(25,e.getAnnotatedGenes(new TermID("GO:0051287")).totalAnnotatedCount());
		Assert.assertEquals(10,e.getAnnotatedGenes(new TermID("GO:0001301")).totalAnnotatedCount());
		Assert.assertEquals(27,e.getAnnotatedGenes(new TermID("GO:0001300")).totalAnnotatedCount());
		Assert.assertEquals(73,e.getAnnotatedGenes(new TermID("GO:0016853")).totalAnnotatedCount());
		Assert.assertEquals(3,e.getAnnotatedGenes(new TermID("GO:0051286")).totalAnnotatedCount());
		Assert.assertEquals(13,e.getAnnotatedGenes(new TermID("GO:0051273")).totalAnnotatedCount());
		Assert.assertEquals(2,e.getAnnotatedGenes(new TermID("GO:0016842")).totalAnnotatedCount());
		Assert.assertEquals(12,e.getAnnotatedGenes(new TermID("GO:0051274")).totalAnnotatedCount());
		Assert.assertEquals(6,e.getAnnotatedGenes(new TermID("GO:0016841")).totalAnnotatedCount());
		Assert.assertEquals(8,e.getAnnotatedGenes(new TermID("GO:0016840")).totalAnnotatedCount());
		Assert.assertEquals(430,e.getAnnotatedGenes(new TermID("GO:0051276")).totalAnnotatedCount());
		Assert.assertEquals(3,e.getAnnotatedGenes(new TermID("GO:0016847")).totalAnnotatedCount());
		Assert.assertEquals(11,e.getAnnotatedGenes(new TermID("GO:0016846")).totalAnnotatedCount());
		Assert.assertEquals(12,e.getAnnotatedGenes(new TermID("GO:0051278")).totalAnnotatedCount());
		Assert.assertEquals(39,e.getAnnotatedGenes(new TermID("GO:0016835")).totalAnnotatedCount());
		Assert.assertEquals(4,e.getAnnotatedGenes(new TermID("GO:0035969")).totalAnnotatedCount());
		Assert.assertEquals(21,e.getAnnotatedGenes(new TermID("GO:0071173")).totalAnnotatedCount());
		Assert.assertEquals(4,e.getAnnotatedGenes(new TermID("GO:0035968")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0051266")).totalAnnotatedCount());
		Assert.assertEquals(28,e.getAnnotatedGenes(new TermID("GO:0071174")).totalAnnotatedCount());
		Assert.assertEquals(7,e.getAnnotatedGenes(new TermID("GO:0016833")).totalAnnotatedCount());
		Assert.assertEquals(4,e.getAnnotatedGenes(new TermID("GO:0016832")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0051268")).totalAnnotatedCount());
		Assert.assertEquals(4,e.getAnnotatedGenes(new TermID("GO:0071168")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0051269")).totalAnnotatedCount());
		Assert.assertEquals(3,e.getAnnotatedGenes(new TermID("GO:0016838")).totalAnnotatedCount());
		Assert.assertEquals(3,e.getAnnotatedGenes(new TermID("GO:0071169")).totalAnnotatedCount());
		Assert.assertEquals(28,e.getAnnotatedGenes(new TermID("GO:0016836")).totalAnnotatedCount());
		Assert.assertEquals(42,e.getAnnotatedGenes(new TermID("GO:0016875")).totalAnnotatedCount());
		Assert.assertEquals(5,e.getAnnotatedGenes(new TermID("GO:0036008")).totalAnnotatedCount());
		Assert.assertEquals(6,e.getAnnotatedGenes(new TermID("GO:0001323")).totalAnnotatedCount());
		Assert.assertEquals(199,e.getAnnotatedGenes(new TermID("GO:0016874")).totalAnnotatedCount());
		Assert.assertEquals(6,e.getAnnotatedGenes(new TermID("GO:0001324")).totalAnnotatedCount());
		Assert.assertEquals(11,e.getAnnotatedGenes(new TermID("GO:0016877")).totalAnnotatedCount());
		Assert.assertEquals(7,e.getAnnotatedGenes(new TermID("GO:0051310")).totalAnnotatedCount());
		Assert.assertEquals(42,e.getAnnotatedGenes(new TermID("GO:0016876")).totalAnnotatedCount());
		Assert.assertEquals(138,e.getAnnotatedGenes(new TermID("GO:0016879")).totalAnnotatedCount());
		Assert.assertEquals(6,e.getAnnotatedGenes(new TermID("GO:0016878")).totalAnnotatedCount());
		Assert.assertEquals(9,e.getAnnotatedGenes(new TermID("GO:0036003")).totalAnnotatedCount());
		Assert.assertEquals(8,e.getAnnotatedGenes(new TermID("GO:0036002")).totalAnnotatedCount());
		Assert.assertEquals(5,e.getAnnotatedGenes(new TermID("GO:0016864")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0016867")).totalAnnotatedCount());
		Assert.assertEquals(24,e.getAnnotatedGenes(new TermID("GO:0016866")).totalAnnotatedCount());
		Assert.assertEquals(4,e.getAnnotatedGenes(new TermID("GO:0001315")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0001316")).totalAnnotatedCount());
		Assert.assertEquals(9,e.getAnnotatedGenes(new TermID("GO:0051302")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0001317")).totalAnnotatedCount());
		Assert.assertEquals(7,e.getAnnotatedGenes(new TermID("GO:0051303")).totalAnnotatedCount());
		Assert.assertEquals(10,e.getAnnotatedGenes(new TermID("GO:0016868")).totalAnnotatedCount());
		Assert.assertEquals(31,e.getAnnotatedGenes(new TermID("GO:0051300")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0001319")).totalAnnotatedCount());
		Assert.assertEquals(304,e.getAnnotatedGenes(new TermID("GO:0051301")).totalAnnotatedCount());
		Assert.assertEquals(14,e.getAnnotatedGenes(new TermID("GO:0051348")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0016662")).totalAnnotatedCount());
		Assert.assertEquals(4,e.getAnnotatedGenes(new TermID("GO:0051349")).totalAnnotatedCount());
		Assert.assertEquals(4,e.getAnnotatedGenes(new TermID("GO:0016661")).totalAnnotatedCount());
		Assert.assertEquals(2,e.getAnnotatedGenes(new TermID("GO:0035927")).totalAnnotatedCount());
		Assert.assertEquals(5,e.getAnnotatedGenes(new TermID("GO:0051351")).totalAnnotatedCount());
		Assert.assertEquals(52,e.getAnnotatedGenes(new TermID("GO:0051345")).totalAnnotatedCount());
		Assert.assertEquals(10,e.getAnnotatedGenes(new TermID("GO:0051346")).totalAnnotatedCount());
		Assert.assertEquals(2,e.getAnnotatedGenes(new TermID("GO:0016657")).totalAnnotatedCount());
		Assert.assertEquals(14,e.getAnnotatedGenes(new TermID("GO:0051347")).totalAnnotatedCount());
		Assert.assertEquals(5,e.getAnnotatedGenes(new TermID("GO:0016671")).totalAnnotatedCount());
		Assert.assertEquals(3,e.getAnnotatedGenes(new TermID("GO:0016670")).totalAnnotatedCount());
		Assert.assertEquals(11,e.getAnnotatedGenes(new TermID("GO:0016668")).totalAnnotatedCount());
		Assert.assertEquals(35,e.getAnnotatedGenes(new TermID("GO:0016667")).totalAnnotatedCount());
		Assert.assertEquals(4,e.getAnnotatedGenes(new TermID("GO:0051352")).totalAnnotatedCount());
		Assert.assertEquals(11,e.getAnnotatedGenes(new TermID("GO:0016646")).totalAnnotatedCount());
		Assert.assertEquals(3,e.getAnnotatedGenes(new TermID("GO:0071361")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0016647")).totalAnnotatedCount());
		Assert.assertEquals(14,e.getAnnotatedGenes(new TermID("GO:0016645")).totalAnnotatedCount());
		Assert.assertEquals(118,e.getAnnotatedGenes(new TermID("GO:0051329")).totalAnnotatedCount());
		Assert.assertEquals(4,e.getAnnotatedGenes(new TermID("GO:0016642")).totalAnnotatedCount());
		Assert.assertEquals(3,e.getAnnotatedGenes(new TermID("GO:0016641")).totalAnnotatedCount());
		Assert.assertEquals(9,e.getAnnotatedGenes(new TermID("GO:0051340")).totalAnnotatedCount());
		Assert.assertEquals(5,e.getAnnotatedGenes(new TermID("GO:0016655")).totalAnnotatedCount());
		Assert.assertEquals(5,e.getAnnotatedGenes(new TermID("GO:0016653")).totalAnnotatedCount());
		Assert.assertEquals(27,e.getAnnotatedGenes(new TermID("GO:0016651")).totalAnnotatedCount());
		Assert.assertEquals(74,e.getAnnotatedGenes(new TermID("GO:0051336")).totalAnnotatedCount());
		Assert.assertEquals(5,e.getAnnotatedGenes(new TermID("GO:0051339")).totalAnnotatedCount());
		Assert.assertEquals(54,e.getAnnotatedGenes(new TermID("GO:0051338")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0016649")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0035959")).totalAnnotatedCount());
		Assert.assertEquals(5,e.getAnnotatedGenes(new TermID("GO:0051382")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0035958")).totalAnnotatedCount());
		Assert.assertEquals(5,e.getAnnotatedGenes(new TermID("GO:0051383")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0016695")).totalAnnotatedCount());
		Assert.assertEquals(3,e.getAnnotatedGenes(new TermID("GO:0035957")).totalAnnotatedCount());
		Assert.assertEquals(3,e.getAnnotatedGenes(new TermID("GO:0035956")).totalAnnotatedCount());
		Assert.assertEquals(3,e.getAnnotatedGenes(new TermID("GO:0035955")).totalAnnotatedCount());
		Assert.assertEquals(3,e.getAnnotatedGenes(new TermID("GO:0035953")).totalAnnotatedCount());
		Assert.assertEquals(42,e.getAnnotatedGenes(new TermID("GO:0001522")).totalAnnotatedCount());
		Assert.assertEquals(4,e.getAnnotatedGenes(new TermID("GO:0051377")).totalAnnotatedCount());
		Assert.assertEquals(3,e.getAnnotatedGenes(new TermID("GO:0035952")).totalAnnotatedCount());
		Assert.assertEquals(25,e.getAnnotatedGenes(new TermID("GO:0035967")).totalAnnotatedCount());
		Assert.assertEquals(9,e.getAnnotatedGenes(new TermID("GO:0016701")).totalAnnotatedCount());
		Assert.assertEquals(32,e.getAnnotatedGenes(new TermID("GO:0035966")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0016703")).totalAnnotatedCount());
		Assert.assertEquals(2,e.getAnnotatedGenes(new TermID("GO:0035965")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0035964")).totalAnnotatedCount());
		Assert.assertEquals(8,e.getAnnotatedGenes(new TermID("GO:0016702")).totalAnnotatedCount());
		Assert.assertEquals(10,e.getAnnotatedGenes(new TermID("GO:0071417")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0016699")).totalAnnotatedCount());
		Assert.assertEquals(2,e.getAnnotatedGenes(new TermID("GO:0035961")).totalAnnotatedCount());
		Assert.assertEquals(2,e.getAnnotatedGenes(new TermID("GO:0035960")).totalAnnotatedCount());
		Assert.assertEquals(19,e.getAnnotatedGenes(new TermID("GO:0016676")).totalAnnotatedCount());
		Assert.assertEquals(9,e.getAnnotatedGenes(new TermID("GO:0016679")).totalAnnotatedCount());
		Assert.assertEquals(71,e.getAnnotatedGenes(new TermID("GO:0001510")).totalAnnotatedCount());
		Assert.assertEquals(6,e.getAnnotatedGenes(new TermID("GO:0071398")).totalAnnotatedCount());
		Assert.assertEquals(6,e.getAnnotatedGenes(new TermID("GO:0071396")).totalAnnotatedCount());
		Assert.assertEquals(19,e.getAnnotatedGenes(new TermID("GO:0016675")).totalAnnotatedCount());
		Assert.assertEquals(3,e.getAnnotatedGenes(new TermID("GO:0035950")).totalAnnotatedCount());
		Assert.assertEquals(17,e.getAnnotatedGenes(new TermID("GO:0016684")).totalAnnotatedCount());
		Assert.assertEquals(3,e.getAnnotatedGenes(new TermID("GO:0035948")).totalAnnotatedCount());
		Assert.assertEquals(6,e.getAnnotatedGenes(new TermID("GO:0071400")).totalAnnotatedCount());
		Assert.assertEquals(9,e.getAnnotatedGenes(new TermID("GO:0016681")).totalAnnotatedCount());
		Assert.assertEquals(7,e.getAnnotatedGenes(new TermID("GO:0071406")).totalAnnotatedCount());
		Assert.assertEquals(3,e.getAnnotatedGenes(new TermID("GO:0035947")).totalAnnotatedCount());
		Assert.assertEquals(8,e.getAnnotatedGenes(new TermID("GO:0016723")).totalAnnotatedCount());
		Assert.assertEquals(12,e.getAnnotatedGenes(new TermID("GO:0016722")).totalAnnotatedCount());
		Assert.assertEquals(2,e.getAnnotatedGenes(new TermID("GO:0051409")).totalAnnotatedCount());
		Assert.assertEquals(8,e.getAnnotatedGenes(new TermID("GO:0035859")).totalAnnotatedCount());
		Assert.assertEquals(7,e.getAnnotatedGenes(new TermID("GO:0016721")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0035861")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0016726")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0035863")).totalAnnotatedCount());
		Assert.assertEquals(5,e.getAnnotatedGenes(new TermID("GO:0016725")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0035862")).totalAnnotatedCount());
		Assert.assertEquals(3,e.getAnnotatedGenes(new TermID("GO:0016724")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0016731")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0071324")).totalAnnotatedCount());
		Assert.assertEquals(2,e.getAnnotatedGenes(new TermID("GO:0016730")).totalAnnotatedCount());
		Assert.assertEquals(5,e.getAnnotatedGenes(new TermID("GO:0071326")).totalAnnotatedCount());
		Assert.assertEquals(4,e.getAnnotatedGenes(new TermID("GO:0016728")).totalAnnotatedCount());
		Assert.assertEquals(5,e.getAnnotatedGenes(new TermID("GO:0071322")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0035870")).totalAnnotatedCount());
		Assert.assertEquals(5,e.getAnnotatedGenes(new TermID("GO:0016706")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0001409")).totalAnnotatedCount());
		Assert.assertEquals(29,e.getAnnotatedGenes(new TermID("GO:0016705")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0001408")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0016708")).totalAnnotatedCount());
		Assert.assertEquals(5,e.getAnnotatedGenes(new TermID("GO:0016709")).totalAnnotatedCount());
		Assert.assertEquals(4,e.getAnnotatedGenes(new TermID("GO:0051403")).totalAnnotatedCount());
		Assert.assertEquals(67,e.getAnnotatedGenes(new TermID("GO:0071310")).totalAnnotatedCount());
		Assert.assertEquals(3,e.getAnnotatedGenes(new TermID("GO:0016717")).totalAnnotatedCount());
		Assert.assertEquals(4,e.getAnnotatedGenes(new TermID("GO:0051443")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0051440")).totalAnnotatedCount());
		Assert.assertEquals(4,e.getAnnotatedGenes(new TermID("GO:0016755")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0051441")).totalAnnotatedCount());
		Assert.assertEquals(104,e.getAnnotatedGenes(new TermID("GO:0016757")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0051446")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0051447")).totalAnnotatedCount());
		Assert.assertEquals(4,e.getAnnotatedGenes(new TermID("GO:0051444")).totalAnnotatedCount());
		Assert.assertEquals(34,e.getAnnotatedGenes(new TermID("GO:0051445")).totalAnnotatedCount());
		Assert.assertEquals(84,e.getAnnotatedGenes(new TermID("GO:0016758")).totalAnnotatedCount());
		Assert.assertEquals(17,e.getAnnotatedGenes(new TermID("GO:0016763")).totalAnnotatedCount());
		Assert.assertEquals(41,e.getAnnotatedGenes(new TermID("GO:0016765")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0051455")).totalAnnotatedCount());
		Assert.assertEquals(26,e.getAnnotatedGenes(new TermID("GO:0051452")).totalAnnotatedCount());
		Assert.assertEquals(27,e.getAnnotatedGenes(new TermID("GO:0051453")).totalAnnotatedCount());
		Assert.assertEquals(5,e.getAnnotatedGenes(new TermID("GO:0071333")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0035873")).totalAnnotatedCount());
		Assert.assertEquals(803,e.getAnnotatedGenes(new TermID("GO:0016740")).totalAnnotatedCount());
		Assert.assertEquals(5,e.getAnnotatedGenes(new TermID("GO:0071331")).totalAnnotatedCount());
		Assert.assertEquals(105,e.getAnnotatedGenes(new TermID("GO:0016741")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0035879")).totalAnnotatedCount());
		Assert.assertEquals(8,e.getAnnotatedGenes(new TermID("GO:0016742")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0071329")).totalAnnotatedCount());
		Assert.assertEquals(2,e.getAnnotatedGenes(new TermID("GO:0016743")).totalAnnotatedCount());
		Assert.assertEquals(6,e.getAnnotatedGenes(new TermID("GO:0016744")).totalAnnotatedCount());
		Assert.assertEquals(130,e.getAnnotatedGenes(new TermID("GO:0016746")).totalAnnotatedCount());
		Assert.assertEquals(107,e.getAnnotatedGenes(new TermID("GO:0016747")).totalAnnotatedCount());
		Assert.assertEquals(6,e.getAnnotatedGenes(new TermID("GO:0051439")).totalAnnotatedCount());
		Assert.assertEquals(2,e.getAnnotatedGenes(new TermID("GO:0016748")).totalAnnotatedCount());
		Assert.assertEquals(8,e.getAnnotatedGenes(new TermID("GO:0051438")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0016749")).totalAnnotatedCount());
		Assert.assertEquals(3,e.getAnnotatedGenes(new TermID("GO:0051437")).totalAnnotatedCount());
		Assert.assertEquals(3,e.getAnnotatedGenes(new TermID("GO:0051436")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0016751")).totalAnnotatedCount());
		Assert.assertEquals(2,e.getAnnotatedGenes(new TermID("GO:0071020")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0071021")).totalAnnotatedCount());
		Assert.assertEquals(3,e.getAnnotatedGenes(new TermID("GO:0017070")).totalAnnotatedCount());
		Assert.assertEquals(12,e.getAnnotatedGenes(new TermID("GO:0017069")).totalAnnotatedCount());
		Assert.assertEquals(13,e.getAnnotatedGenes(new TermID("GO:0071012")).totalAnnotatedCount());
		Assert.assertEquals(6,e.getAnnotatedGenes(new TermID("GO:0017059")).totalAnnotatedCount());
		Assert.assertEquals(8,e.getAnnotatedGenes(new TermID("GO:0071013")).totalAnnotatedCount());
		Assert.assertEquals(4,e.getAnnotatedGenes(new TermID("GO:0017057")).totalAnnotatedCount());
		Assert.assertEquals(8,e.getAnnotatedGenes(new TermID("GO:0071014")).totalAnnotatedCount());
		Assert.assertEquals(7,e.getAnnotatedGenes(new TermID("GO:0017056")).totalAnnotatedCount());
		Assert.assertEquals(5,e.getAnnotatedGenes(new TermID("GO:0071008")).totalAnnotatedCount());
		Assert.assertEquals(6,e.getAnnotatedGenes(new TermID("GO:0017062")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0017061")).totalAnnotatedCount());
		Assert.assertEquals(28,e.getAnnotatedGenes(new TermID("GO:0071010")).totalAnnotatedCount());
		Assert.assertEquals(5,e.getAnnotatedGenes(new TermID("GO:0071037")).totalAnnotatedCount());
		Assert.assertEquals(5,e.getAnnotatedGenes(new TermID("GO:0071036")).totalAnnotatedCount());
		Assert.assertEquals(10,e.getAnnotatedGenes(new TermID("GO:0071039")).totalAnnotatedCount());
		Assert.assertEquals(16,e.getAnnotatedGenes(new TermID("GO:0071038")).totalAnnotatedCount());
		Assert.assertEquals(9,e.getAnnotatedGenes(new TermID("GO:0071033")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0071032")).totalAnnotatedCount());
		Assert.assertEquals(2,e.getAnnotatedGenes(new TermID("GO:0017087")).totalAnnotatedCount());
		Assert.assertEquals(16,e.getAnnotatedGenes(new TermID("GO:0071035")).totalAnnotatedCount());
		Assert.assertEquals(10,e.getAnnotatedGenes(new TermID("GO:0071034")).totalAnnotatedCount());
		Assert.assertEquals(18,e.getAnnotatedGenes(new TermID("GO:0071029")).totalAnnotatedCount());
		Assert.assertEquals(18,e.getAnnotatedGenes(new TermID("GO:0071028")).totalAnnotatedCount());
		Assert.assertEquals(7,e.getAnnotatedGenes(new TermID("GO:0071031")).totalAnnotatedCount());
		Assert.assertEquals(3,e.getAnnotatedGenes(new TermID("GO:0071030")).totalAnnotatedCount());
		Assert.assertEquals(25,e.getAnnotatedGenes(new TermID("GO:0071025")).totalAnnotatedCount());
		Assert.assertEquals(25,e.getAnnotatedGenes(new TermID("GO:0071027")).totalAnnotatedCount());
		Assert.assertEquals(764,e.getAnnotatedGenes(new TermID("GO:0017076")).totalAnnotatedCount());
		Assert.assertEquals(4,e.getAnnotatedGenes(new TermID("GO:0070988")).totalAnnotatedCount());
		Assert.assertEquals(9,e.getAnnotatedGenes(new TermID("GO:0070987")).totalAnnotatedCount());
		Assert.assertEquals(2,e.getAnnotatedGenes(new TermID("GO:0051983")).totalAnnotatedCount());
		Assert.assertEquals(3,e.getAnnotatedGenes(new TermID("GO:0070985")).totalAnnotatedCount());
		Assert.assertEquals(129,e.getAnnotatedGenes(new TermID("GO:0017038")).totalAnnotatedCount());
		Assert.assertEquals(11,e.getAnnotatedGenes(new TermID("GO:0017025")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0017024")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0051974")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0051975")).totalAnnotatedCount());
		Assert.assertEquals(2,e.getAnnotatedGenes(new TermID("GO:0051972")).totalAnnotatedCount());
		Assert.assertEquals(8,e.getAnnotatedGenes(new TermID("GO:0071007")).totalAnnotatedCount());
		Assert.assertEquals(3,e.getAnnotatedGenes(new TermID("GO:0017048")).totalAnnotatedCount());
		Assert.assertEquals(13,e.getAnnotatedGenes(new TermID("GO:0071006")).totalAnnotatedCount());
		Assert.assertEquals(2,e.getAnnotatedGenes(new TermID("GO:0017050")).totalAnnotatedCount());
		Assert.assertEquals(28,e.getAnnotatedGenes(new TermID("GO:0071004")).totalAnnotatedCount());
		Assert.assertEquals(2,e.getAnnotatedGenes(new TermID("GO:0051998")).totalAnnotatedCount());
		Assert.assertEquals(4,e.getAnnotatedGenes(new TermID("GO:0017053")).totalAnnotatedCount());
		Assert.assertEquals(2,e.getAnnotatedGenes(new TermID("GO:0017054")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0051996")).totalAnnotatedCount());
		Assert.assertEquals(2,e.getAnnotatedGenes(new TermID("GO:0017040")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0001578")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0017126")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0017125")).totalAnnotatedCount());
		Assert.assertEquals(4,e.getAnnotatedGenes(new TermID("GO:0017150")).totalAnnotatedCount());
		Assert.assertEquals(19,e.getAnnotatedGenes(new TermID("GO:0017148")).totalAnnotatedCount());
		Assert.assertEquals(42,e.getAnnotatedGenes(new TermID("GO:0070972")).totalAnnotatedCount());
		Assert.assertEquals(8,e.getAnnotatedGenes(new TermID("GO:0017136")).totalAnnotatedCount());
		Assert.assertEquals(2,e.getAnnotatedGenes(new TermID("GO:0070966")).totalAnnotatedCount());
		Assert.assertEquals(5,e.getAnnotatedGenes(new TermID("GO:0017137")).totalAnnotatedCount());
		Assert.assertEquals(3,e.getAnnotatedGenes(new TermID("GO:0017102")).totalAnnotatedCount());
		Assert.assertEquals(83,e.getAnnotatedGenes(new TermID("GO:0070925")).totalAnnotatedCount());
		Assert.assertEquals(2,e.getAnnotatedGenes(new TermID("GO:0070916")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0017091")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0070917")).totalAnnotatedCount());
		Assert.assertEquals(3,e.getAnnotatedGenes(new TermID("GO:0070939")).totalAnnotatedCount());
		Assert.assertEquals(14,e.getAnnotatedGenes(new TermID("GO:0070938")).totalAnnotatedCount());
		Assert.assertEquals(8,e.getAnnotatedGenes(new TermID("GO:0017119")).totalAnnotatedCount());
		Assert.assertEquals(9,e.getAnnotatedGenes(new TermID("GO:0017112")).totalAnnotatedCount());
		Assert.assertEquals(7,e.getAnnotatedGenes(new TermID("GO:0070941")).totalAnnotatedCount());
		Assert.assertEquals(3,e.getAnnotatedGenes(new TermID("GO:0070940")).totalAnnotatedCount());
		Assert.assertEquals(5,e.getAnnotatedGenes(new TermID("GO:0017108")).totalAnnotatedCount());
		Assert.assertEquals(2,e.getAnnotatedGenes(new TermID("GO:0017110")).totalAnnotatedCount());
		Assert.assertEquals(388,e.getAnnotatedGenes(new TermID("GO:0017111")).totalAnnotatedCount());
		Assert.assertEquals(20,e.getAnnotatedGenes(new TermID("GO:0001558")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0070933")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0070932")).totalAnnotatedCount());
		Assert.assertEquals(2,e.getAnnotatedGenes(new TermID("GO:0016929")).totalAnnotatedCount());
		Assert.assertEquals(2,e.getAnnotatedGenes(new TermID("GO:0071139")).totalAnnotatedCount());
		Assert.assertEquals(78,e.getAnnotatedGenes(new TermID("GO:0071156")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0001786")).totalAnnotatedCount());
		Assert.assertEquals(42,e.getAnnotatedGenes(new TermID("GO:0071166")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0071163")).totalAnnotatedCount());
		Assert.assertEquals(13,e.getAnnotatedGenes(new TermID("GO:0016896")).totalAnnotatedCount());
		Assert.assertEquals(5,e.getAnnotatedGenes(new TermID("GO:0001731")).totalAnnotatedCount());
		Assert.assertEquals(4,e.getAnnotatedGenes(new TermID("GO:0016898")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0016899")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0016901")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0001734")).totalAnnotatedCount());
		Assert.assertEquals(26,e.getAnnotatedGenes(new TermID("GO:0016903")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0016906")).totalAnnotatedCount());
		Assert.assertEquals(10,e.getAnnotatedGenes(new TermID("GO:0016925")).totalAnnotatedCount());
		Assert.assertEquals(6,e.getAnnotatedGenes(new TermID("GO:0016926")).totalAnnotatedCount());
		Assert.assertEquals(3,e.getAnnotatedGenes(new TermID("GO:0016998")).totalAnnotatedCount());
		Assert.assertEquals(3,e.getAnnotatedGenes(new TermID("GO:0071073")).totalAnnotatedCount());
		Assert.assertEquals(3,e.getAnnotatedGenes(new TermID("GO:0071072")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0016992")).totalAnnotatedCount());
		Assert.assertEquals(3,e.getAnnotatedGenes(new TermID("GO:0017006")).totalAnnotatedCount());
		Assert.assertEquals(10,e.getAnnotatedGenes(new TermID("GO:0017004")).totalAnnotatedCount());
		Assert.assertEquals(2,e.getAnnotatedGenes(new TermID("GO:0017005")).totalAnnotatedCount());
		Assert.assertEquals(3,e.getAnnotatedGenes(new TermID("GO:0017003")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0001726")).totalAnnotatedCount());
		Assert.assertEquals(12,e.getAnnotatedGenes(new TermID("GO:0001727")).totalAnnotatedCount());
		Assert.assertEquals(4,e.getAnnotatedGenes(new TermID("GO:0017022")).totalAnnotatedCount());
		Assert.assertEquals(3,e.getAnnotatedGenes(new TermID("GO:0017017")).totalAnnotatedCount());
		Assert.assertEquals(78,e.getAnnotatedGenes(new TermID("GO:0071103")).totalAnnotatedCount());
		Assert.assertEquals(13,e.getAnnotatedGenes(new TermID("GO:0017016")).totalAnnotatedCount());
		Assert.assertEquals(10,e.getAnnotatedGenes(new TermID("GO:0071043")).totalAnnotatedCount());
		Assert.assertEquals(14,e.getAnnotatedGenes(new TermID("GO:0071042")).totalAnnotatedCount());
		Assert.assertEquals(2,e.getAnnotatedGenes(new TermID("GO:0071041")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0016966")).totalAnnotatedCount());
		Assert.assertEquals(11,e.getAnnotatedGenes(new TermID("GO:0001671")).totalAnnotatedCount());
		Assert.assertEquals(2,e.getAnnotatedGenes(new TermID("GO:0071040")).totalAnnotatedCount());
		Assert.assertEquals(14,e.getAnnotatedGenes(new TermID("GO:0071047")).totalAnnotatedCount());
		Assert.assertEquals(18,e.getAnnotatedGenes(new TermID("GO:0071046")).totalAnnotatedCount());
		Assert.assertEquals(2,e.getAnnotatedGenes(new TermID("GO:0001664")).totalAnnotatedCount());
		Assert.assertEquals(3,e.getAnnotatedGenes(new TermID("GO:0071044")).totalAnnotatedCount());
		Assert.assertEquals(5,e.getAnnotatedGenes(new TermID("GO:0001666")).totalAnnotatedCount());
		Assert.assertEquals(15,e.getAnnotatedGenes(new TermID("GO:0071051")).totalAnnotatedCount());
		Assert.assertEquals(3,e.getAnnotatedGenes(new TermID("GO:0016972")).totalAnnotatedCount());
		Assert.assertEquals(3,e.getAnnotatedGenes(new TermID("GO:0071050")).totalAnnotatedCount());
		Assert.assertEquals(25,e.getAnnotatedGenes(new TermID("GO:0016973")).totalAnnotatedCount());
		Assert.assertEquals(2,e.getAnnotatedGenes(new TermID("GO:0001676")).totalAnnotatedCount());
		Assert.assertEquals(3,e.getAnnotatedGenes(new TermID("GO:0071049")).totalAnnotatedCount());
		Assert.assertEquals(2,e.getAnnotatedGenes(new TermID("GO:0071048")).totalAnnotatedCount());
		Assert.assertEquals(5,e.getAnnotatedGenes(new TermID("GO:0001678")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0016971")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0001680")).totalAnnotatedCount());
		Assert.assertEquals(3,e.getAnnotatedGenes(new TermID("GO:0001682")).totalAnnotatedCount());
		Assert.assertEquals(2,e.getAnnotatedGenes(new TermID("GO:0016979")).totalAnnotatedCount());
		Assert.assertEquals(8,e.getAnnotatedGenes(new TermID("GO:0071071")).totalAnnotatedCount());
		Assert.assertEquals(82,e.getAnnotatedGenes(new TermID("GO:0070783")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0051775")).totalAnnotatedCount());
		Assert.assertEquals(5,e.getAnnotatedGenes(new TermID("GO:0070775")).totalAnnotatedCount());
		Assert.assertEquals(1,e.getAnnotatedGenes(new TermID("GO:0070774")).totalAnnotatedCount());

		// Code to generate above code
//		for (TermID tid : e)
//			System.out.println(String.format("Assert.assertEquals(%d,e.getAnnotatedGenes(new TermID(\"%s\")).totalAnnotatedCount());",e.getAnnotatedGenes(tid).totalAnnotatedCount(),tid.toString()));
	}
}
