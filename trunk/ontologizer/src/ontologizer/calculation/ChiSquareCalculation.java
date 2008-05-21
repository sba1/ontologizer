/**
 * 
 */
package ontologizer.calculation;

import java.util.Set;

import ontologizer.GOTermEnumerator;
import ontologizer.PopulationSet;
import ontologizer.StudySet;
import ontologizer.GOTermEnumerator.GOTermAnnotatedGenes;
import ontologizer.association.AssociationContainer;
import ontologizer.chisquare.ChiSquare;
import ontologizer.chisquare.ChiSquareIncompatibleCountsException;
import ontologizer.go.GOGraph;
import ontologizer.go.TermID;
import ontologizer.statistics.AbstractTestCorrection;

/**
 * @author grossman
 * 
 */
public class ChiSquareCalculation implements ICalculation
{

	public String getName()
	{
		return "Chi-Square statistics based calculation of p-values";
	}

	public String getDescription()
	{
		return "We calculate a chi-square value for each non.leaf node";
	}

	public EnrichedGOTermsResult calculateStudySet(GOGraph graph,
			AssociationContainer goAssociations,
			PopulationSet populationSet,
			StudySet studySet, AbstractTestCorrection testCorrection)
	{
		GOTermEnumerator studyTermEnumerator = studySet.enumerateGOTerms(graph,
				goAssociations);

		EnrichedGOTermsResult studySetResult = new EnrichedGOTermsResult(graph, goAssociations, studySet, populationSet.getGeneCount());

		for (TermID term : studyTermEnumerator)
		{
			// we don't have to do something for leaves
			if (graph.getTermsDescendants(term).isEmpty())
				continue;

			GOTermEnumerator popuTermEnumerator = populationSet.enumerateGOTerms(
					graph, goAssociations);
			
			
			// calculating properties of term
			AbstractGOTermProperties nextTermProp = calculateTerm(term,
					graph, popuTermEnumerator, studyTermEnumerator);

			// adding properties to studySetResult
			studySetResult.addGOTermProperties(nextTermProp);
		}

		return studySetResult;
	}

	protected AbstractGOTermProperties calculateTerm(TermID term,
			GOGraph graph, GOTermEnumerator populationTermEnumerator,
			GOTermEnumerator studyTermEnumerator)
	{
		ChiSquare chisqCalc = new ChiSquare();

		int backCount;
		int sampleCount;
		/*
		 * What is the expected number of genes annotated in a studyset to given
		 * term?
		 * 
		 * The probability for a randomly drawn gene to be annotated to a given
		 * term is ratio of the number of genes which are annotated to the term
		 * and the total number of annotated genes.
		 * 
		 * This probability has to be multiplied by the size of the StudySet.
		 */

		// counting directly annotated genes
		GOTermAnnotatedGenes termPopGenes = populationTermEnumerator.getAnnotatedGenes(term);
		GOTermAnnotatedGenes termStudyGenes = studyTermEnumerator.getAnnotatedGenes(term);

		backCount = termPopGenes.directAnnotatedCount();
		sampleCount = termStudyGenes.directAnnotatedCount();

		try
		{
			chisqCalc.addCounts(backCount, sampleCount);
		} catch (ChiSquareIncompatibleCountsException e)
		{
			// e.printStackTrace();
			System.err.println("Incompatible background and sample counts for term "
					+ term);
		}

		// counting children
		Set<TermID> children = graph.getTermsDescendants(term);
		for (TermID child : children)
		{
			GOTermAnnotatedGenes childPopGenes = populationTermEnumerator.getAnnotatedGenes(child);
			GOTermAnnotatedGenes childStudyGenes = studyTermEnumerator.getAnnotatedGenes(child);

			if (childPopGenes != null)
			{
				backCount = childPopGenes.totalAnnotatedCount();
				if (childStudyGenes != null)
				{
					sampleCount = childStudyGenes.totalAnnotatedCount();
				} else
				{
					sampleCount = 0;
				}
				try
				{
					chisqCalc.addCounts(backCount, sampleCount);
				} catch (ChiSquareIncompatibleCountsException e)
				{
					// e.printStackTrace();
					System.err.println("Incompatible background and sample counts for term "
							+ child);
				}
			}
		}

		double chisq = chisqCalc.chiSquare();
		ChiSquareGOTermProperties prop = new ChiSquareGOTermProperties();

		prop.goTerm = graph.getGoTermContainer().get(term);
		prop.annotatedPopulationGenes = termPopGenes.totalAnnotatedCount();
		prop.annotatedStudyGenes = termStudyGenes.totalAnnotatedCount();
		prop.nchildren = children.size();
		prop.df = chisqCalc.df();

		// only for testing - random uniform p-values
		// Random rng = new Random();
		// prop.p = rng.nextDouble();
		prop.chisquare = chisq;

		return prop;
	}

}
