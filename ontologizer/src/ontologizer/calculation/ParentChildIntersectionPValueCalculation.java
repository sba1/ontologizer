package ontologizer.calculation;

import ontologizer.association.AssociationContainer;
import ontologizer.ontology.Ontology;
import ontologizer.ontology.TermID;
import ontologizer.set.PopulationSet;
import ontologizer.set.StudySet;
import ontologizer.statistics.Hypergeometric;
import ontologizer.util.Util;

public class ParentChildIntersectionPValueCalculation extends ParentChildPValuesCalculation
{
	public ParentChildIntersectionPValueCalculation(Ontology graph,
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

		int [][] allItems = new int[parents.length + 1][];
		for (i=0;i<parents.length;i++)
		{
			allItems[i] =  parentItems[i];
		}
		allItems[i] = studyIds;

		/* number of genes annotated to family (term and parents) */
		Counts counts = new Counts(parents.length, Util.commonInts(allItems), Util.commonInts(parentItems));
		return counts;
	}
}
