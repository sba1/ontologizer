package ontologizer.calculation;

import ontologizer.association.AssociationContainer;
import ontologizer.ontology.Ontology;
import ontologizer.set.PopulationSet;
import ontologizer.set.StudySet;
import ontologizer.statistics.Hypergeometric;
import ontologizer.statistics.IPValueCalculation;

public class ParentChildCutCalculation extends AbstractPValueBasedCalculation implements IProgressFeedback
{
	public String getName()
	{
		return "Parent-Child-Intersection";
	}

	public String getDescription()
	{
		return "We calculate p-values measuring over-representation"
				+ "of GO term annotated genes in a study set by comparing"
				+ "a term's annotation to the annotation of its parent terms."
				+ "This is a second version which uses intersections instead of unions"
				+ "of gene sets in case of multiple parents.";
	}

	@Override
	protected IPValueCalculation newPValueCalculation(Ontology graph,
			AssociationContainer goAssociations, PopulationSet populationSet,
			StudySet studySet, Hypergeometric hyperg)
	{
		return new ParentChildIntersectionPValueCalculation(graph, goAssociations,populationSet, studySet, hyperg);
	}
}
