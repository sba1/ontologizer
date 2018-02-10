package ontologizer.calculation;

import ontologizer.association.AssociationContainer;
import ontologizer.ontology.Ontology;
import ontologizer.ontology.Term;
import ontologizer.ontology.TermID;
import ontologizer.set.PopulationSet;
import ontologizer.set.StudySet;
import ontologizer.statistics.Hypergeometric;
import ontologizer.statistics.IPValueCalculationProgress;
import ontologizer.statistics.PValue;
import ontologizer.util.Util;
import sonumina.math.graph.SlimDirectedGraphView;

/**
 *
 * This class hides all the details about how the p values are calculated
 * from the multiple test correction.
 *
 * @author Sebastian Bauer
 *
 */
abstract class ParentChildPValuesCalculation extends AbstractPValueCalculation
{
	protected SlimDirectedGraphView<TermID> slimGraph;

	/**
	 * Return value type for getCounts().
	 *
	 * @author Sebastian Bauer
	 */
	protected static class Counts
	{
		public final int parents;
		public final int studyFamilyCount;
		public final int popFamilyCount;

		public Counts(int parents, int studyFamilyCount, int popFamilyCount)
		{
			this.parents = parents;
			this.studyFamilyCount = studyFamilyCount;
			this.popFamilyCount = popFamilyCount;
		}
	}

	public ParentChildPValuesCalculation(Ontology graph,
			AssociationContainer goAssociations, PopulationSet populationSet,
			StudySet studySet, Hypergeometric hyperg)
	{
		super(graph, goAssociations, populationSet, studySet, hyperg);

		slimGraph = graph.getTermIDSlimGraphView();
	}

	protected PValue [] calculatePValues(StudySet studySet, IPValueCalculationProgress progress)
	{
		int[] studyIds = getUniqueIDs(studySet);

		PValue p [] = new PValue[getTotalNumberOfAnnotatedTerms()];

		for (int i = 0; i < termIds.length; i++)
		{
			if (progress != null && (i % 256) == 0)
			{
				progress.update(i);
			}

			p[i] = calculateTerm(studyIds, i);
		}

		return p;
	}

	private ParentChildGOTermProperties calculateTerm(int [] studyIds, int termIndex)
	{
		TermID termId = termIds[termIndex];
		// counts annotated to term
		int studyTermCount = Util.commonInts(studyIds, term2Items[termIndex]);
		int popTermCount = term2Items[termIndex].length;

		// this is what we give back
		ParentChildGOTermProperties prop = new ParentChildGOTermProperties();
		prop.term = termId;
		prop.annotatedPopulationGenes = popTermCount;
		prop.annotatedStudyGenes = studyTermCount;

		if (graph.isRootTerm(termId))
		{
			prop.nparents = 0;
			prop.ignoreAtMTC = true;
			prop.p = 1.0;
			prop.p_adjusted = 1.0;
			prop.p_min = 1.0;
		} else
		{
			Counts counts = getCounts(studyIds, termId);

			int studyFamilyCount = counts.studyFamilyCount;
			int popFamilyCount = counts.popFamilyCount;

			prop.popFamilyGenes = popFamilyCount;
			prop.studyFamilyGenes = studyFamilyCount;
			prop.nparents = counts.parents;

			if (studyTermCount != 0)
			{
				if (popFamilyCount == popTermCount)
				{
					prop.ignoreAtMTC = true;
					prop.p = 1.0;
					prop.p_adjusted = 1.0;
					prop.p_min = 1.0;
				} else
				{
					double p = hyperg.phypergeometric(
							popFamilyCount,
							(double)popTermCount / (double)popFamilyCount,
							studyFamilyCount,
							studyTermCount);

					prop.ignoreAtMTC = false;
					prop.p = p;
					prop.p_min = hyperg.dhyper(
							popTermCount,
							popFamilyCount,
							popTermCount,
							popTermCount);
				}
			} else
			{
				prop.ignoreAtMTC = true;
				prop.p = 1.0;
				prop.p_adjusted = 1.0;
				prop.p_min = 1.0;
			}
		}

		return prop;
	}

	/**
	 * Calculate the counts for the given study set ids for the term.
	 *
	 * @param studyIds the study sets
	 * @param term the term for which the counts shall be determined.
	 * @return the count structure.
	 */
	protected abstract Counts getCounts(int[] studyIds, TermID term);
};
