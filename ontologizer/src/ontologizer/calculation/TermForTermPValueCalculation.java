package ontologizer.calculation;

import ontologizer.association.AssociationContainer;
import ontologizer.ontology.Ontology;
import ontologizer.ontology.TermID;
import ontologizer.set.PopulationSet;
import ontologizer.set.StudySet;
import ontologizer.statistics.Hypergeometric;
import ontologizer.statistics.IPValueCalculationProgress;
import ontologizer.statistics.PValue;
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

	protected PValue [] calculatePValues(int [] studyIds, IPValueCalculationProgress progress)
	{
		PValue p [] = new PValue[getTotalNumberOfAnnotatedTerms()];

		for (int i = 0; i < termMapper.getSize(); i++)
		{
			if (progress != null && (i % 256) == 0)
			{
				progress.update(i);
			}

			TermID term = termMapper.get(i);
			int goidAnnotatedPopGeneCount = term2Items[i].length;
			int popGeneCount = populationSet.getGeneCount();
			int studyGeneCount = studyIds.length;
			int goidAnnotatedStudyGeneCount = Util.commonInts(studyIds, term2Items[i]);

			TermForTermGOTermProperties myP = new TermForTermGOTermProperties();
			myP.term = term;
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
