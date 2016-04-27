package ontologizer.calculation;

import java.util.HashSet;

import org.junit.Test;

import ontologizer.association.Association;
import ontologizer.association.AssociationContainer;
import ontologizer.ontology.Ontology;
import ontologizer.ontology.ParentTermID;
import ontologizer.ontology.Term;
import ontologizer.ontology.TermContainer;
import ontologizer.ontology.TermRelation;
import ontologizer.set.PopulationSet;
import ontologizer.set.StudySet;
import ontologizer.statistics.None;
import ontologizer.types.ByteString;

public class ProbabilisticCalculationTest
{
	@Test
	public void testCalculateStudySet()
	{
		/* Go Graph */
		HashSet<Term> terms = new HashSet<Term>();
		Term c1 = new Term("GO:0000001", "C1");
		Term c2 = new Term("GO:0000002", "C2",new ParentTermID(c1.getID(),TermRelation.IS_A));
		Term c3 = new Term("GO:0000003", "C3",new ParentTermID(c1.getID(),TermRelation.IS_A));
		Term c4 = new Term("GO:0000004", "C4",new ParentTermID(c1.getID(),TermRelation.IS_A));
		terms.add(c1);
		terms.add(c2);
		terms.add(c3);
		terms.add(c4);
		TermContainer termContainer = new TermContainer(terms,"","");
		Ontology graph = Ontology.create(termContainer);

		/* Population */
		PopulationSet pop = new PopulationSet("population");
		for (int i=0;i<10000;i++)
			pop.addGene(new ByteString("gene"+i), "");

		/* Associations */
		AssociationContainer assocContainer = new AssociationContainer();

		/* for C2 */
		for (int i=0;i<10;i++)
			assocContainer.addAssociation(new Association(new ByteString("gene"+i),"GO:0000002"));

		/* for C3 */
		for (int i=10;i<20;i++)
			assocContainer.addAssociation(new Association(new ByteString("gene"+i),"GO:0000003"));

		/* for C4 */
		for (int i=20;i<100;i++)
			assocContainer.addAssociation(new Association(new ByteString("gene"+i),"GO:0000004"));

		/* Study */
		StudySet study = new StudySet("study");

		/* 9 of 10 in C2 */
		for (int i=0;i<9;i++)
			study.addGene(new ByteString("gene"+i), "");

		/* 9 of 10 in C3 */
		for (int i=10;i<19;i++)
			study.addGene(new ByteString("gene"+i), "");

		/* 2 of 100 in C4 */
		for (int i=20;i<22;i++)
			study.addGene(new ByteString("gene"+i), "");

		/* 30 somewhere else */
		for (int i=100;i<130;i++)
			study.addGene(new ByteString("gene"+i), "");

		ProbabilisticCalculation calc = new ProbabilisticCalculation();
		calc.setDefaultP(0.9);
		calc.setDefaultQ(0.17);
		calc.calculateStudySet(graph, assocContainer, pop, study, new None());
	}

}
