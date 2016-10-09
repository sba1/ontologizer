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
class ParentChildPValuesCalculation extends AbstractPValueCalculation
{
	private SlimDirectedGraphView<Term> slimGraph;

	public ParentChildPValuesCalculation(Ontology graph,
			AssociationContainer goAssociations, PopulationSet populationSet,
			StudySet studySet, Hypergeometric hyperg)
	{
		super(graph, goAssociations, populationSet, studySet, hyperg);

		slimGraph = graph.getSlimGraphView();
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
			int index = slimGraph.getVertexIndex(prop.goTerm);

			int [] parents = slimGraph.vertexParents[index];
			int [][] parentItems = new int[parents.length][];

			int i = 0;
			for (int parent : parents)
			{
				parentItems[i++] = term2Items[getIndex(slimGraph.getVertex(parent).getID())];
			}

			/* number of genes annotated to family (term and parents) */
			int [] popFamilyCountArray = new int[1];
			int studyFamilyCount = Util.commonIntsWithUnion(popFamilyCountArray, studyIds, parentItems);
			int popFamilyCount = popFamilyCountArray[0];

			prop.popFamilyGenes = popFamilyCount;
			prop.studyFamilyGenes = studyFamilyCount;
			prop.nparents = parents.length;;

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
