package ontologizer.calculation;

import ontologizer.association.AssociationContainer;
import ontologizer.ontology.Ontology;
import ontologizer.set.PopulationSet;
import ontologizer.set.StudySet;
import ontologizer.statistics.AbstractTestCorrection;
import ontologizer.statistics.Hypergeometric;
import ontologizer.statistics.IPValueCalculation;
import ontologizer.statistics.PValue;

/**
 * Abstract class that provides support for calculations
 * using the IPValueCalculation interface.
 *
 * @author Sebastian Bauer
 */
public abstract class AbstractPValueBasedCalculation extends AbstractHypergeometricCalculation implements IProgressFeedback
{
	private ICalculationProgress calculationProgress;

	 * @return
	 */
	protected abstract IPValueCalculation newPValueCalculation(Ontology graph,
			AssociationContainer associations, PopulationSet populationSet,
			StudySet studySet, Hypergeometric hyperg);

	@Override
	public EnrichedGOTermsResult calculateStudySet(
			Ontology graph,
			AssociationContainer associations,
			PopulationSet populationSet,
			StudySet studySet,
			AbstractTestCorrection testCorrection)
	{
		EnrichedGOTermsResult studySetResult = new EnrichedGOTermsResult(graph, associations, studySet, populationSet.getGeneCount());
		studySetResult.setCalculationName(this.getName());
		studySetResult.setCorrectionName(testCorrection.getName());

		IPValueCalculation pValueCalculation = newPValueCalculation(graph, associations, populationSet, studySet, hyperg);
		PValue p[] = testCorrection.adjustPValues(pValueCalculation, CalculationProgress2TestCorrectionProgress.createUnlessNull(calculationProgress));

		/* Add the results to the result list and filter out terms
		 * with no annotated genes.
		 */
		for (int i=0;i<p.length;i++)
		{
			/* Entries are SingleGOTermProperties */
			AbstractGOTermProperties prop = (AbstractGOTermProperties)p[i];

			/* Within the result ignore terms without any annotation */
			if (prop.annotatedStudyGenes == 0)
				continue;

			studySetResult.addGOTermProperties(prop);
		}

		return studySetResult;
	}

	@Override
	public final boolean supportsTestCorrection()
	{
		return true;
	}

	@Override
	public final void setProgress(ICalculationProgress calculationProgress)
	{
		this.calculationProgress = calculationProgress;
	}
}
