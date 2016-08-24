package ontologizer.calculation;

import ontologizer.association.AssociationContainer;
import ontologizer.enumeration.TermEnumerator;
import ontologizer.ontology.Ontology;
import ontologizer.ontology.TermID;
import ontologizer.set.PopulationSet;
import ontologizer.set.StudySet;
import ontologizer.statistics.AbstractTestCorrection;
import ontologizer.statistics.Hypergeometric;
import ontologizer.statistics.IPValueCalculation;
import ontologizer.statistics.PValue;

/**
*
* This class hides all the details about how the p values are calculated
* from the multiple test correction.
*
* @author Sebastian Bauer
*
*/
class SinglePValuesCalculation implements IPValueCalculation
{
	private Ontology graph;
	private AssociationContainer goAssociations;
	private PopulationSet populationSet;
	private StudySet observedStudySet;
	private Hypergeometric hyperg;

	private TermEnumerator populationTermEnumerator;
	private int totalNumberOfAnnotatedTerms;
	private TermID [] termIds;

	public SinglePValuesCalculation(Ontology graph,
			AssociationContainer goAssociations, PopulationSet populationSet,
			StudySet studySet, Hypergeometric hyperg)
	{
		this.graph = graph;
		this.goAssociations = goAssociations;
		this.populationSet = populationSet;
		this.observedStudySet = studySet;
		this.hyperg = hyperg;

		populationTermEnumerator = populationSet.enumerateGOTerms(graph, goAssociations);
		totalNumberOfAnnotatedTerms = populationTermEnumerator.getTotalNumberOfAnnotatedTerms();

		int i = 0;

		termIds = new TermID[totalNumberOfAnnotatedTerms];
		for (TermID term : populationTermEnumerator)
		{
			termIds[i++] = term;
		}
	}

	private PValue [] calculatePValues(StudySet studySet)
	{
		TermEnumerator studyTermEnumerator = studySet.enumerateGOTerms(graph, goAssociations);

		PValue p [] = new PValue[totalNumberOfAnnotatedTerms];

		for (int i = 0; i < termIds.length; i++)
		{
			TermID term = termIds[i];
			int goidAnnotatedPopGeneCount = populationTermEnumerator.getAnnotatedGenes(term).totalAnnotatedCount();
			int popGeneCount = populationSet.getGeneCount();
			int studyGeneCount = studySet.getGeneCount();
			int goidAnnotatedStudyGeneCount = studyTermEnumerator.getAnnotatedGenes(term).totalAnnotatedCount();

			TermForTermGOTermProperties myP = new TermForTermGOTermProperties();
			myP.goTerm = graph.getTerm(term);
			myP.annotatedStudyGenes = goidAnnotatedStudyGeneCount;
			myP.annotatedPopulationGenes = goidAnnotatedPopGeneCount;

			if (goidAnnotatedStudyGeneCount != 0)
			{
				/* Imagine the following...
				 *
				 * In an urn you put popGeneCount number of balls where a color of a
				 * ball can be white or black. The number of balls having white color
				 * is goidAnnontatedPopGeneCount (all genes of the population which
				 * are annotated by the current GOID).
				 *
				 * You choose to draw studyGeneCount number of balls without replacement.
				 * How big is the probability, that you got goidAnnotatedStudyGeneCount
				 * white balls after the whole drawing process?
				 */

				myP.p = hyperg.phypergeometric(popGeneCount, (double)goidAnnotatedPopGeneCount / (double)popGeneCount,
						studyGeneCount, goidAnnotatedStudyGeneCount);
				myP.p_min = hyperg.dhyper(
						goidAnnotatedPopGeneCount,
						popGeneCount,
						goidAnnotatedPopGeneCount,
						goidAnnotatedPopGeneCount);
			} else
			{
				/* Mark this p value as irrelevant so it isn't considered in a mtc */
				myP.p = 1.0;
				myP.ignoreAtMTC = true;
				myP.p_min = 1.0;
			}

			p[i] = myP;
		}
		return p;
	}

	public PValue[] calculateRawPValues()
	{
		return calculatePValues(observedStudySet);
	}

	public int currentStudySetSize()
	{
		return observedStudySet.getGeneCount();
	}

	public PValue[] calculateRandomPValues()
	{
		return calculatePValues(populationSet.generateRandomStudySet(observedStudySet.getGeneCount()));
	}
};


/**
 *
 * Significant test using a simple independent Fisher Exact
 * calculation for every single term.
 *
 * @author Sebastian Bauer
 */
public class TermForTermCalculation extends AbstractHypergeometricCalculation
{
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

		SinglePValuesCalculation pValueCalculation = new SinglePValuesCalculation(graph, goAssociations, populationSet, studySet, hyperg);
		PValue p[] = testCorrection.adjustPValues(pValueCalculation);

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
}
