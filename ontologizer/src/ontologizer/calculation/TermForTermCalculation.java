package ontologizer.calculation;

import java.util.Arrays;
import java.util.List;

import ontologizer.association.AssociationContainer;
import ontologizer.association.Gene2Associations;
import ontologizer.enumeration.TermEnumerator;
import ontologizer.enumeration.TermEnumerator.TermAnnotatedGenes;
import ontologizer.ontology.Ontology;
import ontologizer.ontology.TermID;
import ontologizer.set.PopulationSet;
import ontologizer.set.StudySet;
import ontologizer.statistics.AbstractTestCorrection;
import ontologizer.statistics.Hypergeometric;
import ontologizer.statistics.IPValueCalculation;
import ontologizer.statistics.IPValueCalculationProgress;
import ontologizer.statistics.PValue;
import ontologizer.types.ByteString;
import ontologizer.util.Util;
import sonumina.collections.ObjectIntHashMap;

/**
 *
 * Significant test using a simple independent Fisher Exact
 * calculation for every single term.
 *
 * @author Sebastian Bauer
 */
public class TermForTermCalculation extends AbstractHypergeometricCalculation implements IProgressFeedback
{
	private ICalculationProgress calculateProgress;

	public String getName()
	{
		return "Term-For-Term";
	}

	public String getDescription()
	{
		return "No description yet";
	}

	@Override
	public EnrichedGOTermsResult calculateStudySet(
			Ontology graph,
			AssociationContainer goAssociations,
			PopulationSet populationSet,
			StudySet studySet,
			AbstractTestCorrection testCorrection)
	{
		EnrichedGOTermsResult studySetResult = new EnrichedGOTermsResult(graph, goAssociations, studySet, populationSet.getGeneCount());
		studySetResult.setCalculationName(this.getName());
		studySetResult.setCorrectionName(testCorrection.getName());

		TermForTermPValueCalculation pValueCalculation = new TermForTermPValueCalculation(graph, goAssociations, populationSet, studySet, hyperg);
		PValue p[] = testCorrection.adjustPValues(pValueCalculation, CalculationProgress2TestCorrectionProgress.createUnlessNull(calculateProgress));

		/* Add the results to the result list and filter out terms
		 * with no annotated genes.
		 */
		for (int i=0;i<p.length;i++)
		{
			/* Entries are SingleGOTermProperties */
			TermForTermGOTermProperties prop = (TermForTermGOTermProperties)p[i];

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
		this.calculateProgress = calculationProgress;
	}
}
