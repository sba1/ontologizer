package ontologizer.calculation;

import ontologizer.association.AssociationContainer;
import ontologizer.ontology.Ontology;
import ontologizer.set.PopulationSet;
import ontologizer.set.StudySet;
import ontologizer.statistics.AbstractTestCorrection;
import ontologizer.statistics.PValue;

public class ParentChildCutCalculation extends AbstractPValueBasedCalculation implements IProgressFeedback
{
	private ICalculationProgress calculationProgress;

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

	public EnrichedGOTermsResult calculateStudySet(
			Ontology graph,
			AssociationContainer goAssociations,
			PopulationSet popSet,
			StudySet studySet,
			AbstractTestCorrection testCorrection)
	{

		EnrichedGOTermsResult studySetResult = new EnrichedGOTermsResult(graph, goAssociations, studySet, popSet.getGeneCount());
		studySetResult.setCalculationName(this.getName());
		studySetResult.setCorrectionName(testCorrection.getName());

		ParentChildPValuesCalculation pValueCalculation = new ParentChildIntersectionPValueCalculation(graph, goAssociations, popSet, studySet, hyperg);
		PValue p[] = testCorrection.adjustPValues(pValueCalculation, CalculationProgress2TestCorrectionProgress.createUnlessNull(calculationProgress));

		/* Add the results to the result list and filter out terms
		 * with no annotated genes.
		 */
		for (int i=0;i<p.length;i++)
		{
			/* Entries are SingleGOTermProperties */
			ParentChildGOTermProperties prop = (ParentChildGOTermProperties)p[i];

			/* Within the result ignore terms without any annotation */
			if (prop.annotatedStudyGenes == 0)
				continue;

			studySetResult.addGOTermProperties(prop);
		}

		return studySetResult;

	}

	public boolean supportsTestCorrection() {
		return true;
	}

	@Override
	public void setProgress(ICalculationProgress calculationProgress)
	{
		this.calculationProgress = calculationProgress;
	}

}
