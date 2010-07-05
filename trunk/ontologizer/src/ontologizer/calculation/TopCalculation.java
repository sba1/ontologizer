package ontologizer.calculation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import ontologizer.ByteString;
import ontologizer.GOTermEnumerator;
import ontologizer.PopulationSet;
import ontologizer.StudySet;
import ontologizer.GOTermEnumerator.GOTermAnnotatedGenes;
import ontologizer.association.AssociationContainer;
import ontologizer.go.GOGraph;
import ontologizer.go.TermID;
import ontologizer.go.GOGraph.GOLevels;
import ontologizer.statistics.AbstractTestCorrection;

public class TopCalculation extends AbstractHypergeometricCalculation
{
	static final double SIGNIFICANCE_LEVEL = 0.01;

	private HashMap<TermID,HashSet<ByteString>> markedGenesMap;

	private void calculateTerm(GOGraph graph,
			AssociationContainer goAssociations, PopulationSet populationSet,
			StudySet studySet, AbstractTestCorrection testCorrection, TermID term,
			EnrichedGOTermsResult studySetResult, GOTermEnumerator studyTermEnumerator, GOTermEnumerator populationTermEnumerator)
	{
		Set<TermID> d = graph.getTermsDescendants(term);

		int popGeneCount = populationSet.getGeneCount();
		int studyGeneCount = studySet.getGeneCount();

		GOTermAnnotatedGenes studyAnnotatedGenes = studyTermEnumerator.getAnnotatedGenes(term);
		
		int goidAnnotatedPopGeneCount = populationTermEnumerator.getAnnotatedGenes(term).totalAnnotatedCount();
		int goidAnnotatedStudyGeneCount = studyAnnotatedGenes.totalAnnotatedCount();

		if (goidAnnotatedStudyGeneCount == 0) return;

		HashSet<ByteString> markedGenes = new HashSet<ByteString>();
		
		/* We are at a leaf */
		if (d == null)
		{
			goidAnnotatedPopGeneCount = populationTermEnumerator.getAnnotatedGenes(term).totalAnnotatedCount();
			goidAnnotatedStudyGeneCount = studyTermEnumerator.getAnnotatedGenes(term).totalAnnotatedCount();
		} else
		{
			if (markedGenesMap.containsKey(term))
				markedGenes.addAll(markedGenesMap.get(term));

			goidAnnotatedStudyGeneCount = 0;
			goidAnnotatedPopGeneCount = 0;

			GOTermAnnotatedGenes annotated = studyTermEnumerator.getAnnotatedGenes(term);
			for (ByteString gene : annotated.totalAnnotated)
			{
				if (!markedGenes.contains(gene))
					goidAnnotatedStudyGeneCount++;
			}

			annotated = populationTermEnumerator.getAnnotatedGenes(term);
			for (ByteString gene : annotated.totalAnnotated)
			{
				if (!markedGenes.contains(gene))
					goidAnnotatedPopGeneCount++;
			}
		}

		TopGOTermProperties myP = new TopGOTermProperties();
		myP.goTerm = graph.getGOTerm(term);
		myP.annotatedStudyGenes = studyAnnotatedGenes.totalAnnotatedCount();
		myP.annotatedPopulationGenes = populationTermEnumerator.getAnnotatedGenes(term).totalAnnotatedCount();

		if (myP.goTerm == null) return;

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

			if (myP.p < SIGNIFICANCE_LEVEL)
			{
				Set<TermID> upperTerms = graph.getTermsOfInducedGraph(graph.getRootTerm().getID(), term);
				for (TermID up : upperTerms)
				{
					HashSet<ByteString> marked = markedGenesMap.get(up);
					if (marked == null)
					{
						marked = new HashSet<ByteString>();
						markedGenesMap.put(up, marked);
					}
					marked.addAll(studyAnnotatedGenes.totalAnnotated);
				}
			}
		} else
		{
			/* Mark this p value as irrelevant so it isn't considered in an mtc */
			myP.p = 1.0;
			myP.ignoreAtMTC = true;
			myP.p_min = 1.0;
		}
		myP.p_adjusted = myP.p;

		studySetResult.addGOTermProperties(myP);
	}

	private HashSet<ByteString> calculateTermNew(GOGraph graph,
			AssociationContainer goAssociations, PopulationSet populationSet,
			StudySet studySet, AbstractTestCorrection testCorrection, TermID term,
			EnrichedGOTermsResult studySetResult, GOTermEnumerator studyTermEnumerator, GOTermEnumerator populationTermEnumerator)
	{
		
		/* Leave early if we already processed this term */
		if (markedGenesMap.containsKey(term))
			return markedGenesMap.get(term);
		
		HashSet<ByteString> markedGenes = new HashSet<ByteString>();
		Set<TermID> d = graph.getTermsDescendants(term);
		if (d != null)
		{
			for (TermID c : d)
				markedGenes.addAll(calculateTermNew(graph, goAssociations, populationSet, studySet, testCorrection, c, studySetResult, studyTermEnumerator, populationTermEnumerator));
		}

		int popGeneCount = populationSet.getGeneCount();
		int studyGeneCount = studySet.getGeneCount();

		GOTermAnnotatedGenes studyAnnotatedGenes = studyTermEnumerator.getAnnotatedGenes(term);
		
		int goidAnnotatedPopGeneCount = 0; 
		int goidAnnotatedStudyGeneCount = 0;

		GOTermAnnotatedGenes annotated = studyTermEnumerator.getAnnotatedGenes(term);
		for (ByteString gene : annotated.totalAnnotated)
		{
			if (!markedGenes.contains(gene))
				goidAnnotatedStudyGeneCount++;
		}

		annotated = populationTermEnumerator.getAnnotatedGenes(term);
		for (ByteString gene : annotated.totalAnnotated)
		{
			if (!markedGenes.contains(gene))
				goidAnnotatedPopGeneCount++;
		}


		TopGOTermProperties myP = new TopGOTermProperties();
		myP.goTerm = graph.getGOTerm(term);
		myP.annotatedStudyGenes = studyAnnotatedGenes.totalAnnotatedCount();
		myP.annotatedPopulationGenes = populationTermEnumerator.getAnnotatedGenes(term).totalAnnotatedCount();

		if (myP.goTerm == null || goidAnnotatedStudyGeneCount == 0)
		{
			markedGenesMap.put(term,markedGenes);
			return markedGenes;
		}

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

			if (myP.p < SIGNIFICANCE_LEVEL)
			{
				Set<TermID> upperTerms = graph.getTermsOfInducedGraph(graph.getRootTerm().getID(), term);
				for (TermID up : upperTerms)
				{
					HashSet<ByteString> marked = markedGenesMap.get(up);
					if (marked == null)
					{
						marked = new HashSet<ByteString>();
						markedGenesMap.put(up, marked);
					}
					marked.addAll(studyAnnotatedGenes.totalAnnotated);
				}
			}
		} else
		{
			/* Mark this p value as irrelevant so it isn't considered in an mtc */
			myP.p = 1.0;
			myP.ignoreAtMTC = true;
			myP.p_min = 1.0;
		}
		myP.p_adjusted = myP.p;

		studySetResult.addGOTermProperties(myP);

		markedGenesMap.put(term,markedGenes);
		return markedGenes;
	}

	public EnrichedGOTermsResult calculateStudySet(GOGraph graph,
			AssociationContainer goAssociations, PopulationSet populationSet,
			StudySet studySet, AbstractTestCorrection testCorrection)
	{
		if (graph.getRelevantSubontology() != null || graph.getRelevantSubset() != null)
			throw new IllegalArgumentException("Subset or sub ontology selection not supported for this calculation method!");
		
		EnrichedGOTermsResult studySetResult = new EnrichedGOTermsResult(graph, goAssociations, studySet, populationSet.getGeneCount());
		studySetResult.setCalculationName(this.getName());
		studySetResult.setCorrectionName(testCorrection.getName());

		markedGenesMap = new HashMap<TermID, HashSet<ByteString>>();

		GOTermEnumerator studyTermEnumerator = studySet.enumerateGOTerms(graph,goAssociations);
		GOTermEnumerator populationTermEnumerator = populationSet.enumerateGOTerms(graph,goAssociations);
		Set<TermID> allAnnotatedTerms = populationTermEnumerator.getAllAnnotatedTermsAsSet();
		
		
		if (true)
		{
			GOLevels levels = graph.getGOLevels(allAnnotatedTerms);
			
			for (int i=levels.getMaxLevel();i>=0;i--)
			{
				Set<TermID> terms = levels.getLevelTermSet(i);
				for (TermID t : terms)
				{
					calculateTerm(graph, goAssociations, populationSet, studySet, testCorrection, t, studySetResult, studyTermEnumerator, populationTermEnumerator);
				}
			}
		} else
		{
			calculateTermNew(graph, goAssociations, populationSet, studySet, testCorrection, graph.getRootTerm().getID(), studySetResult, studyTermEnumerator, populationTermEnumerator);
		}


		return studySetResult;
	}

	public String getDescription()
	{
		// TODO Auto-generated method stub
		return null;
	}

	public String getName()
	{
		return "Topology-Elim";
	}
	
	public boolean supportsTestCorrection() {
		return true;
	}

}
