package ontologizer.calculation;

import java.util.ArrayList;
import java.util.HashSet;

import ontologizer.ByteString;
import ontologizer.PopulationSet;
import ontologizer.StudySet;
import ontologizer.association.Association;
import ontologizer.association.AssociationContainer;
import ontologizer.association.AssociationParser;
import ontologizer.go.GOGraph;
import ontologizer.go.ParentTermID;
import ontologizer.go.Term;
import ontologizer.go.TermContainer;
import ontologizer.go.TermRelation;
import junit.framework.TestCase;

public class ProbabilisticCalculationTest extends TestCase {

	public void testCalculateStudySet()
	{
		/* Go Graph */
		HashSet<Term> terms = new HashSet<Term>();
		Term c1 = new Term("GO:0000001", "C1", null);
		Term c2 = new Term("GO:0000002", "C2", null,new ParentTermID(c1.getID(),TermRelation.IS_A));
		Term c3 = new Term("GO:0000003", "C3", null,new ParentTermID(c1.getID(),TermRelation.IS_A));
		Term c4 = new Term("GO:0000004", "C4", null,new ParentTermID(c1.getID(),TermRelation.IS_A));
		terms.add(c1);
		terms.add(c2);
		terms.add(c3);
		terms.add(c4);
		TermContainer termContainer = new TermContainer(terms,"","");
		GOGraph graph = new GOGraph(termContainer);

		/* Population */
		PopulationSet pop = new PopulationSet("population");
		for (int i=0;i<10000;i++)
			pop.addGene(new ByteString("gene"+i), "");

		/* Associations */
		AssociationContainer assocContainer = new AssociationContainer();

		/* for C2 */
		for (int i=0;i<10;i++)
			assocContainer.addAssociation(new Association(new ByteString("gene"+i),2));

		/* for C3 */
		for (int i=10;i<20;i++)
			assocContainer.addAssociation(new Association(new ByteString("gene"+i),3));

		/* for C4 */
		for (int i=20;i<100;i++)
			assocContainer.addAssociation(new Association(new ByteString("gene"+i),4));

		/* Study */
		StudySet study = new StudySet("study");

		/* 9 in C2 */
		for (int i=0;i<9;i++)
			study.addGene(new ByteString("gene"+i), "");

		/* 9 in C3 */
		for (int i=10;i<19;i++)
			study.addGene(new ByteString("gene"+i), "");

		/* 2 in C4 */
		for (int i=20;i<22;i++)
			study.addGene(new ByteString("gene"+i), "");

		/* 30 somewhere else */
		for (int i=100;i<130;i++)
			study.addGene(new ByteString("gene"+i), "");

		ProbabilisticCalculation calc = new ProbabilisticCalculation();
		calc.calculateStudySet(graph, assocContainer, pop, study, null);


//		calc.calculateStudySet(graph, goAssociations, populationSet, studySet, null);
	}

}
