package ontologizer.calculation;

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
import ontologizer.types.ByteString;

public class TopologyWeightedCalculation extends AbstractHypergeometricCalculation
{
	static final double SIGNIFICANCE_LEVEL = 0.01;

	private void computeTermSig(PopulationSet populationSet, StudySet studySet, Ontology graph, TermID u, Set<TermID> children, EnrichedGOTermsResult studySetResult, GOTermEnumerator studyTermEnumerator, GOTermEnumerator populationTermEnumerator)
	{
		if (graph.isRootTerm(u)) return;

		/* Execute Fisher */
		TopologyWeightGOTermProperties prop = wFisher(populationSet, studySet, graph, u, studySetResult, studyTermEnumerator, populationTermEnumerator);

		if (children == null || children.size() == 0) return;

		HashMap<TermID,Double> weights = new HashMap<TermID,Double>();
		HashSet<TermID> sigChildren = new HashSet<TermID>();
		for (TermID child : children)
		{
			TopologyWeightGOTermProperties childProp = (TopologyWeightGOTermProperties)studySetResult.getGOTermProperties(child);
			double w = sigRatio(childProp.p, prop.p);
			weights.put(child,w);
			if (w > 1) sigChildren.add(child);
		}

		if (sigChildren.size() == 0)
		{
			/* Case 1: U is the most significant term in the family */
			for (TermID child : children)
			{
				TopologyWeightGOTermProperties childProp = (TopologyWeightGOTermProperties)studySetResult.getGOTermProperties(child);
				double w = weights.get(child);

				/* Readjust the weight for every gene annotated to the child */
				GOTermAnnotatedGenes childAnnotatedGenes = populationTermEnumerator.getAnnotatedGenes(u);
				for (ByteString gene : childAnnotatedGenes.totalAnnotated)
					childProp.setWeight(gene, childProp.getWeight(gene) * w);

				/* Recalculate the child's significance */
				wFisher(populationSet, studySet, graph, child, studySetResult, studyTermEnumerator, populationTermEnumerator);
			}
			return;
		}
		
		/* Case 2: At least one child is more significant than u */
		for (TermID child : sigChildren)
		{
			double w = weights.get(child);

			Set<TermID> upper = graph.getTermsOfInducedGraph(graph.getRootTerm().getID(), u);
			upper.remove(u);
			upper.remove(graph.getRootTerm().getID());

			for (TermID up : upper)
			{
				ensureGOTermPropertiesExistence(graph, up, studySetResult, studyTermEnumerator, populationTermEnumerator);

				TopologyWeightGOTermProperties upProp = (TopologyWeightGOTermProperties)studySetResult.getGOTermProperties(up);

				GOTermAnnotatedGenes upAnnotatedGenes = populationTermEnumerator.getAnnotatedGenes(up);

				for (ByteString gene : upAnnotatedGenes.totalAnnotated)
					upProp.setWeight(gene, upProp.getWeight(gene) / w);
			}
		}

		HashSet<TermID> newChildren = new HashSet<TermID>();
		for (TermID t : children)
		{
			if (!sigChildren.contains(t))
				newChildren.add(t);
		}
	}

	/**
	 * Perform the weighted fisher test.
	 * 
	 * @param populationSet
	 * @param studySet
	 * @param graph
	 * @param u
	 * @param studySetResult
	 * @param studyTermEnumerator
	 * @param populationTermEnumerator
	 * @return
	 */
	private TopologyWeightGOTermProperties wFisher(PopulationSet populationSet,
			StudySet studySet, Ontology graph, TermID u,
			EnrichedGOTermsResult studySetResult,
			GOTermEnumerator studyTermEnumerator,
			GOTermEnumerator populationTermEnumerator)
	{

		TopologyWeightGOTermProperties prop = ensureGOTermPropertiesExistence(
				graph, u, studySetResult, studyTermEnumerator,
				populationTermEnumerator);

		GOTermAnnotatedGenes populationAnnotatedGenes = populationTermEnumerator.getAnnotatedGenes(u);
		GOTermAnnotatedGenes studyAnnotatedGenes = studyTermEnumerator.getAnnotatedGenes(u);

		double goidAnnotatedPopGeneCount = 0;
		double goidAnnotatedStudyGeneCount = 0;
		double popGeneCount = 0;
		double studyGeneCount = 0;

		for (ByteString gene : populationAnnotatedGenes.totalAnnotated)
			goidAnnotatedPopGeneCount += prop.getWeight(gene);
		
		for (ByteString gene : studyAnnotatedGenes.totalAnnotated)
			goidAnnotatedStudyGeneCount += prop.getWeight(gene);

		for (ByteString gene : populationSet)
			popGeneCount += prop.getWeight(gene);

		for (ByteString gene : studySet)
			studyGeneCount += prop.getWeight(gene);

		if (goidAnnotatedStudyGeneCount != 0)
		{
			prop.p = hyperg.phypergeometric((int)Math.ceil(popGeneCount), Math.ceil(goidAnnotatedPopGeneCount) / Math.ceil(popGeneCount),
					(int)studyGeneCount, (int)goidAnnotatedStudyGeneCount);
		} else
		{
			prop.p = 1;
			prop.p_min = 1.0;
		}
		prop.p_adjusted = prop.p;
		return prop;
	}

	private TopologyWeightGOTermProperties ensureGOTermPropertiesExistence(
			Ontology graph, TermID u, EnrichedGOTermsResult studySetResult,
			GOTermEnumerator studyTermEnumerator,
			GOTermEnumerator populationTermEnumerator)
	{
		TopologyWeightGOTermProperties prop = (TopologyWeightGOTermProperties)studySetResult.getGOTermProperties(u);
		if (prop == null)
		{
			GOTermAnnotatedGenes populationAnnotatedGenes = populationTermEnumerator.getAnnotatedGenes(u);
			GOTermAnnotatedGenes studyAnnotatedGenes = studyTermEnumerator.getAnnotatedGenes(u);

			prop = new TopologyWeightGOTermProperties();
			prop.goTerm = graph.getTerm(u);
			prop.annotatedStudyGenes = studyAnnotatedGenes.totalAnnotatedCount();
			prop.annotatedPopulationGenes = populationAnnotatedGenes.totalAnnotatedCount();
			studySetResult.addGOTermProperties(prop);
		}
		return prop;
	}
	
	private double sigRatio(double a, double b)
	{
		return b/a;
	}

	public EnrichedGOTermsResult calculateStudySet(Ontology graph,
			AssociationContainer goAssociations, PopulationSet populationSet,
			StudySet studySet, AbstractTestCorrection testCorrection)
	{
		EnrichedGOTermsResult studySetResult = new EnrichedGOTermsResult(graph, goAssociations, studySet, populationSet.getGeneCount());
		studySetResult.setCalculationName(this.getName());
		studySetResult.setCorrectionName(testCorrection.getName());

		GOTermEnumerator studyTermEnumerator = studySet.enumerateGOTerms(graph,goAssociations);
		GOTermEnumerator populationTermEnumerator = populationSet.enumerateGOTerms(graph,goAssociations);

		Set<TermID> allAnnotatedTerms = studyTermEnumerator.getAllAnnotatedTermsAsSet();
		GOLevels levels = graph.getGOLevels(allAnnotatedTerms);

		for (int i=levels.getMaxLevel();i>=0;i--)
		{
			Set<TermID> terms = levels.getLevelTermSet(i);

			for (TermID t : terms)
			{
				Set<TermID> descs = graph.getTermChildren(t);
				Set<TermID> annotatedDescs = new HashSet<TermID>();
				for (TermID d : descs)
				{
					if (allAnnotatedTerms.contains(d))
						annotatedDescs.add(d);
				}
				
				computeTermSig(populationSet, studySet, graph, t, annotatedDescs, studySetResult, studyTermEnumerator, populationTermEnumerator);
			}
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
		return "Topology-Weighted";
	}

	public boolean supportsTestCorrection()
	{
		return false;
	}

}
