package ontologizer.calculation;

import java.util.Arrays;
import java.util.List;

import ontologizer.association.AssociationContainer;
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
* This class hides all the details about how the p values are calculated
* from the multiple test correction.
*
* @author Sebastian Bauer
*
*/
class SinglePValuesCalculation implements IPValueCalculation
{
	private Ontology graph;
	private PopulationSet populationSet;
	private StudySet observedStudySet;
	private Hypergeometric hyperg;

	private int totalNumberOfAnnotatedTerms;

	private ObjectIntHashMap<ByteString> item2Index;
	private TermID [] termIds;
	private int [][] term2Items;

	public SinglePValuesCalculation(Ontology graph,
			AssociationContainer goAssociations, PopulationSet populationSet,
			StudySet studySet, Hypergeometric hyperg)
	{
		this.graph = graph;
		this.populationSet = populationSet;
		this.observedStudySet = studySet;
		this.hyperg = hyperg;

		initCalculationContext(graph, goAssociations, populationSet);
	}

	private void initCalculationContext(Ontology graph, AssociationContainer goAssociations, StudySet populationSet)
	{
		TermEnumerator populationTermEnumerator = populationSet.enumerateTerms(graph, goAssociations);
		totalNumberOfAnnotatedTerms = populationTermEnumerator.getTotalNumberOfAnnotatedTerms();

		List<ByteString> itemList = populationTermEnumerator.getGenesAsList();
		item2Index = new ObjectIntHashMap<ByteString>(itemList.size());
		int itemId = 0;
		for (ByteString item : itemList)
		{
			item2Index.put(item, itemId++);
		}

		termIds = new TermID[totalNumberOfAnnotatedTerms];
		term2Items = new int[totalNumberOfAnnotatedTerms][];

		int i = 0;

		for (TermID term : populationTermEnumerator)
		{
			TermAnnotatedGenes tag = populationTermEnumerator.getAnnotatedGenes(term);
			int nTermItems = tag.totalAnnotated.size();

			term2Items[i] = new int[nTermItems];

			int j = 0;
			for (ByteString item : tag.totalAnnotated)
			{
				term2Items[i][j++] = item2Index.get(item);
			}

			Arrays.sort(term2Items[i]);

			termIds[i] = term;
			i++;
		}
	}

	private PValue [] calculatePValues(StudySet studySet, IPValueCalculationProgress progress)
	{
		int [] studyIds = new int[studySet.getGeneCount()];
		int mappedStudyItems = 0;
		for (ByteString studyItem : studySet)
		{
			int index = item2Index.getIfAbsent(studyItem, Integer.MAX_VALUE);
			if (index != Integer.MAX_VALUE)
				studyIds[mappedStudyItems++] = index;
		}

		if (mappedStudyItems != studyIds.length)
		{
			/* This could only happen if there are items in the study set that are not in the population */
			int [] newStudyIds = new int[mappedStudyItems];
			for (int j = 0; j < mappedStudyItems; j++)
			{
				newStudyIds[j] = studyIds[j];
			}
			studyIds = newStudyIds;
		}
		/* Sort for simpler intersection finding */
		Arrays.sort(studyIds);

		PValue p [] = new PValue[totalNumberOfAnnotatedTerms];

		for (int i = 0; i < termIds.length; i++)
		{
			if (progress != null && (i % 16) == 0)
			{
				progress.update(i);
			}

			TermID term = termIds[i];
			int goidAnnotatedPopGeneCount = term2Items[i].length;
			int popGeneCount = populationSet.getGeneCount();
			int studyGeneCount = studySet.getGeneCount();
			int goidAnnotatedStudyGeneCount = Util.commonInts(studyIds, term2Items[i]);

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

	@Override
	public PValue[] calculateRawPValues(IPValueCalculationProgress progress)
	{
		return calculatePValues(observedStudySet, progress);
	}

	public int currentStudySetSize()
	{
		return observedStudySet.getGeneCount();
	}

	@Override
	public PValue[] calculateRandomPValues(IPValueCalculationProgress progress)
	{
		return calculatePValues(populationSet.generateRandomStudySet(observedStudySet.getGeneCount()), progress);
	}

	public int getNumberOfPValues()
	{
		return termIds.length;
	}

};


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

		SinglePValuesCalculation pValueCalculation = new SinglePValuesCalculation(graph, goAssociations, populationSet, studySet, hyperg);
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
