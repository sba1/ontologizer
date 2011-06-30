package ontologizer.calculation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import ontologizer.association.AssociationContainer;
import ontologizer.enumeration.GOTermEnumerator;
import ontologizer.enumeration.GOTermEnumerator.GOTermAnnotatedGenes;
import ontologizer.go.Ontology;
import ontologizer.go.TermID;
import ontologizer.go.Ontology.GOLevels;
import ontologizer.set.PopulationSet;
import ontologizer.set.StudySet;
import ontologizer.statistics.AbstractTestCorrection;
import ontologizer.statistics.IPValueCalculation;
import ontologizer.statistics.PValue;
import ontologizer.types.ByteString;

public class TopCalculation extends AbstractHypergeometricCalculation
{
	static final double SIGNIFICANCE_LEVEL = 0.01;

	public EnrichedGOTermsResult calculateStudySet(Ontology graph,
			AssociationContainer goAssociations, PopulationSet populationSet,
			StudySet studySet, AbstractTestCorrection testCorrection)
	{
		EnrichedGOTermsResult studySetResult = new EnrichedGOTermsResult(graph, goAssociations, studySet, populationSet.getGeneCount());
		studySetResult.setCalculationName(this.getName());
		studySetResult.setCorrectionName(testCorrection.getName());
		
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
			public PopulationSet populationSet;
			public StudySet observedStudySet;
			public AssociationContainer goAssociations;
			public Ontology graph;

			private HashMap<TermID,HashSet<ByteString>> markedGenesMap;

			/**
			 * 
			 * Recursive function performing elim.
			 * 
			 * @param populationTermEnumerator
			 * @param studyTermEnumerator
			 * @param studySet
			 * @param term
			 * @param p
			 * @return
			 */
			private HashSet<ByteString> calculateTerm(GOTermEnumerator populationTermEnumerator, GOTermEnumerator studyTermEnumerator, StudySet studySet, TermID term, ArrayList<PValue> pList)
			{
				if (term.id == 5982)
					System.out.println("HUHUHUH" + markedGenesMap.containsKey(term));

				/* Leave early if we already processed this term */
				if (markedGenesMap.containsKey(term))
					return markedGenesMap.get(term);
				
				/* Determine genes that are marked */
				HashSet<ByteString> markedGenes = new HashSet<ByteString>();
				Set<TermID> d = graph.getTermChildren(term);
				if (d != null)
				{
					for (TermID c : d)
						markedGenes.addAll(calculateTerm(populationTermEnumerator, studyTermEnumerator, studySet, c, pList));
				}

				/* Now calculate the p value */
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
				/* We have to use the real count here */
				if (annotated.totalAnnotated.size() == 0)
				{
					markedGenesMap.put(term,markedGenes);
					return markedGenes;
				}

				TopGOTermProperties myP = new TopGOTermProperties();
				myP.goTerm = graph.getTerm(term);
				myP.annotatedStudyGenes = studyAnnotatedGenes.totalAnnotatedCount();
				myP.annotatedPopulationGenes = populationTermEnumerator.getAnnotatedGenes(term).totalAnnotatedCount();

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
					
					myP.p = hyperg.phypergeometric(popGeneCount, (double)goidAnnotatedPopGeneCount / (double)popGeneCount, studyGeneCount, goidAnnotatedStudyGeneCount);
					myP.p_min = hyperg.dhyper(goidAnnotatedPopGeneCount,popGeneCount,goidAnnotatedPopGeneCount,goidAnnotatedPopGeneCount);

					if (myP.p < SIGNIFICANCE_LEVEL)
						markedGenes.addAll(studyAnnotatedGenes.totalAnnotated);
				} else
				{
					/* Mark this p value as irrelevant so it isn't considered in an mtc */
					myP.p = 1.0;
					myP.ignoreAtMTC = true;
					myP.p_min = 1.0;
				}
				myP.p_adjusted = myP.p;
				pList.add(myP);
				markedGenesMap.put(term,markedGenes);
				return markedGenes;
			}

			private PValue [] calculatePValues(StudySet studySet)
			{
				markedGenesMap = new HashMap<TermID, HashSet<ByteString>>();
				GOTermEnumerator studyTermEnumerator = studySet.enumerateGOTerms(graph,goAssociations);
				GOTermEnumerator populationTermEnumerator = populationSet.enumerateGOTerms(graph,goAssociations);
				ArrayList<PValue> list = new ArrayList<PValue>(100);
				calculateTerm(populationTermEnumerator, studyTermEnumerator, studySet, graph.getRootTerm().getID(), list);
				PValue p [] = new PValue[list.size()];
				return list.toArray(p);
			}
			
			public PValue[] calculateRawPValues()
			{
				return calculatePValues(observedStudySet);
			}

			public PValue[] calculateRandomPValues()
			{
				return calculatePValues(populationSet.generateRandomStudySet(observedStudySet.getGeneCount()));
			}

			public int currentStudySetSize()
			{
				return observedStudySet.getGeneCount();
			}
		}

		SinglePValuesCalculation pValueCalculation = new SinglePValuesCalculation();
		pValueCalculation.goAssociations = goAssociations;
		pValueCalculation.graph = graph;
		pValueCalculation.populationSet = populationSet;
		pValueCalculation.observedStudySet = studySet;
		PValue p[] = testCorrection.adjustPValues(pValueCalculation);
		
		/* Add the results to the result list and filter out terms
		 * with no annotated genes.
		 */
		for (int i=0;i<p.length;i++)
		{
			/* Entries are SingleGOTermProperties */
			TopGOTermProperties prop = (TopGOTermProperties)p[i];

			/* Within the result ignore terms without any annotation */
			if (prop.annotatedStudyGenes == 0)
				continue;

			studySetResult.addGOTermProperties(prop);
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
	
	public boolean supportsTestCorrection()
	{
		return true;
	}

}
