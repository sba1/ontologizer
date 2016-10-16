package ontologizer.calculation;

import ontologizer.association.AssociationContainer;
import ontologizer.ontology.Ontology;
import ontologizer.set.PopulationSet;
import ontologizer.set.StudySet;
import ontologizer.statistics.Hypergeometric;
import ontologizer.statistics.IPValueCalculation;

public class ParentChildCalculation extends AbstractPValueBasedCalculation implements IProgressFeedback
{
	public String getName()
	{
		return "Parent-Child-Union";
	}

	public String getDescription()
	{
		return "We calculate p-values measuring over-representation" +
				"of GO term annotated genes in a study set by comparing" +
				"a term's annotation to the annotation of its parent terms.";
	}

	@Override
	protected IPValueCalculation newPValueCalculation(Ontology graph,
			AssociationContainer goAssociations, PopulationSet populationSet,
			StudySet studySet, Hypergeometric hyperg)
	{
		return new ParentChildUnionPValueCalculation(graph, goAssociations, populationSet, studySet, hyperg);
	}
}
