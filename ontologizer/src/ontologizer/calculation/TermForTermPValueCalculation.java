package ontologizer.calculation;

import java.util.Arrays;

import ontologizer.association.AssociationContainer;
import ontologizer.association.Gene2Associations;
import ontologizer.ontology.Ontology;
import ontologizer.ontology.TermID;
import ontologizer.set.PopulationSet;
import ontologizer.set.StudySet;
import ontologizer.statistics.Hypergeometric;
import ontologizer.statistics.IPValueCalculationProgress;
import ontologizer.statistics.PValue;
import ontologizer.types.ByteString;
import ontologizer.util.Util;

/**
 * A specific term-for-term p-value calculation.
 *
 * @author Sebastian Bauer
 */
public class TermForTermPValueCalculation extends AbstractPValueCalculation
{
	public TermForTermPValueCalculation(Ontology graph,
			AssociationContainer associations, PopulationSet populationSet,
			StudySet studySet, Hypergeometric hyperg)
	{
		super(graph, associations, populationSet, studySet, hyperg);
	}

	protected PValue [] calculatePValues(StudySet studySet, IPValueCalculationProgress progress)
	{
		int [] studyIds = new int[studySet.getGeneCount()];
		int mappedStudyItems = 0;
		for (ByteString studyItem : studySet)
		{
			int index = item2Index.getIfAbsent(studyItem, Integer.MAX_VALUE);
			if (index == Integer.MAX_VALUE)
			{
				/* Try synonyms etc. */
				Gene2Associations g2a = associations.get(studyItem);
				if (g2a != null)
					index = item2Index.getIfAbsent(g2a.name(), Integer.MAX_VALUE);
			}
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

		PValue p [] = new PValue[getTotalNumberOfAnnotatedTerms()];

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
};
