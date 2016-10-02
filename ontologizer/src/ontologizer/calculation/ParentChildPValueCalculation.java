package ontologizer.calculation;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

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
 *
 * This class hides all the details about how the p values are calculated
 * from the multiple test correction.
 *
 * @author Sebastian Bauer
 *
 */
class ParentChildPValuesCalculation extends AbstractPValueCalculation
{
	public ParentChildPValuesCalculation(Ontology graph,
			AssociationContainer goAssociations, PopulationSet populationSet,
			StudySet studySet, Hypergeometric hyperg)
	{
		super(graph, goAssociations, populationSet, studySet, hyperg);
	}

	protected PValue [] calculatePValues(StudySet studySet, IPValueCalculationProgress progress)
	{
		int[] studyIds = getUniqueIDs(studySet);

		PValue p [] = new PValue[getTotalNumberOfAnnotatedTerms()];

		for (int i = 0; i < termIds.length; i++)
		{
			ParentChildGOTermProperties termProp = calculateTerm(studyIds, i);
			p[i] = termProp;
		}

		return p;
	}

	private ParentChildGOTermProperties calculateTerm(int [] studyIds, int termId)
	{
		TermID term = termIds[termId];
		// counts annotated to term
		int studyTermCount = Util.commonInts(studyIds, term2Items[termId]);
		int popTermCount = term2Items[termId].length;

		// this is what we give back
		ParentChildGOTermProperties prop = new ParentChildGOTermProperties();
		prop.goTerm = graph.getTerm(term);
		prop.annotatedPopulationGenes = popTermCount;
		prop.annotatedStudyGenes = studyTermCount;

		if (graph.isRootTerm(term))
		{
			prop.nparents = 0;
			prop.ignoreAtMTC = true;
			prop.p = 1.0;
			prop.p_adjusted = 1.0;
			prop.p_min = 1.0;
		} else
		{
			Set<TermID> parents = graph.getTermParents(term);

			/* These will hold the items annotated to parents (with respect to the population) */
			HashSet<Integer> popParentAllGenes = new HashSet<Integer>();

			// looping over all parents to get the genes and adding all annotated genes to HashSets
			for (TermID parent : parents)
			{
				int p = getIndex(parent);

				for (int i = 0; i < term2Items[p].length; i++)
					popParentAllGenes.add(term2Items[p][i]);
			}

			/* Make an array out of it */
			int [] popItems = new int[popParentAllGenes.size()];
			int i = 0;
			for (int item : popParentAllGenes)
				popItems[i++] = item;
			Arrays.sort(popItems);

			// number of genes annotated to family (term and parents)
			int popFamilyCount = popParentAllGenes.size();
			int studyFamilyCount = Util.commonInts(popItems, studyIds);

			prop.popFamilyGenes = popFamilyCount;
			prop.studyFamilyGenes = studyFamilyCount;
			prop.nparents = parents.size();

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
};
