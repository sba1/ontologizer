package ontologizer.calculation;

import ontologizer.association.AssociationContainer;
import ontologizer.ontology.Ontology;
import ontologizer.ontology.TermID;
import ontologizer.set.PopulationSet;
import ontologizer.set.StudySet;
import ontologizer.statistics.Hypergeometric;
import ontologizer.util.Util;

public class ParentChildUnionPValueCalculation extends ParentChildPValuesCalculation
{
	public ParentChildUnionPValueCalculation(Ontology graph,
			AssociationContainer goAssociations, PopulationSet populationSet,
			StudySet studySet, Hypergeometric hyperg)
	{
		super(graph, goAssociations, populationSet, studySet, hyperg);
	}

	@Override
	protected Counts getCounts(int[] studyIds, TermID term)
	{
		int slimIndex = slimGraph.getVertexIndex(term);
		int [] parents = slimGraph.vertexParents[slimIndex];
		int [][] parentItems = new int[parents.length][];

		int i = 0;
		for (int parent : parents)
		{
			parentItems[i++] = term2Items[getIndex(slimGraph.getVertex(parent))];
		}

		/* number of genes annotated to family (term and parents) */
		int [] popFamilyCountArray = new int[1];
		Counts counts = new Counts(parents.length, Util.commonIntsWithUnion(popFamilyCountArray, studyIds, parentItems), popFamilyCountArray[0]);
		return counts;
	}
}
