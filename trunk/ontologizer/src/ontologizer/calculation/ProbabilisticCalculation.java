package ontologizer.calculation;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import ontologizer.ByteString;
import ontologizer.GOTermEnumerator;
import ontologizer.PopulationSet;
import ontologizer.StudySet;
import ontologizer.association.AssociationContainer;
import ontologizer.go.GOGraph;
import ontologizer.go.TermID;
import ontologizer.statistics.AbstractTestCorrection;

/**
 * This calculation implements the approach described in
 * 
 * "A probabilistic generative model for GO enrichment analysis" by Lu et al.
 * 
 * 
 * TODO: The current implementation is neither elegant nor clean.
 * 
 * @author Sebastian Bauer
 */
public class ProbabilisticCalculation implements ICalculation
{
	/** Run data */
	private static class Data
	{
		public double p = 0.5;
		public double q = 0.5;
		public double alpha = 3;

		public int sg;
		public int sn;
		public int ag;
		public int an;
		public int inactive;

		public Set<TermID> activeTerms = new LinkedHashSet<TermID>();

		/* Fixed (initialized from outside) */
		private GOTermEnumerator popEnumerator;
		private HashSet<ByteString> activeGenes;
		private HashSet<ByteString> allGenes;
		private List<TermID> allTerms;

		/**
		 * Switch the given term (i.e., make it active if not active, 
		 * make it inactive if active)
		 * @param t
		 */
		public void switchTerm(TermID t)
		{
			if (activeTerms.contains(t)) activeTerms.remove(t);
			else activeTerms.add(t);
		}

		/**
		 * The function which should be optimized.
		 *  
		 * @param data
		 * @return
		 */
		public double objective()
		{
			calculateParamters(); 

			double obj;

			obj = ag*Math.log(p) + an * Math.log(q) + sg * Math.log(1-p) + sn * Math.log(1-q) - alpha*activeTerms.size();

			return obj;
		}

		/**
		 * Recalculate all parameters based upon the active terms from
		 * scatch. 
		 */
		public void calculateParamters()
		{
			/* Active gene nodes connected to at least one active term */
			Set<ByteString> Ag = new HashSet<ByteString>();

			/* I inactive gene nodes */
			/* Number of edges connecting nodes in I with active term nodes */
			sg = 0;
			
			/* Number of edges connecting nodes in I with inactive term nodes */
			sn = 0;

			for (TermID t : activeTerms)
			{
				for (ByteString g : popEnumerator.getAnnotatedGenes(t).totalAnnotated)
				{
					if (activeGenes.contains(g))
						Ag.add(g);
					else
					{
						/* Gene is inactive but term active */
						sg++;
					}
				}
			}

			for (TermID t : popEnumerator.getAllAnnotatedTermsAsSet())
			{
				if (activeTerms.contains(t))
					continue;

				/* Inactive terms */
				for (ByteString g : popEnumerator.getAnnotatedGenes(t).totalAnnotated)
				{
					if (!activeGenes.contains(g))
					{
						/* Gene is inactive as terms are inactive */
						sn++;
					}
				}
			}

			/* Active gene nodes connected to at least one active term */
			ag = Ag.size();
			
			/* Active gene nodes not connected to any active term */
			an = activeGenes.size() - ag;
			inactive = allGenes.size() - activeGenes.size();
		}
		
		/**
		 * Optimize the objective the for active terms.
		 * 
		 * @param graph
		 * @return
		 */
		private double optimizeForTerms(GOGraph graph)
		{
			double obj = objective();

			do
			{
				double best = Double.NEGATIVE_INFINITY;
				TermID bestTerm = null;
				
				for (TermID t : allTerms)
				{
					switchTerm(t);
					
					double o = objective();
					if (o>best)
					{
						best = o;
						bestTerm = t;
					}
					
					switchTerm(t);
				}

				if (bestTerm != null && best > obj)
				{
					switchTerm(bestTerm);
					obj = objective();
				} else
				{
					break;
				}
			} while(true);
			
			return obj;
		}

}


	@Override
	public EnrichedGOTermsResult calculateStudySet(GOGraph graph,
			AssociationContainer goAssociations, PopulationSet populationSet,
			StudySet studySet, AbstractTestCorrection testCorrection)
	{
		Data data = new Data();
		data.popEnumerator = populationSet.enumerateGOTerms(graph, goAssociations);
		data.allTerms = data.popEnumerator.getAllAnnotatedTermsAsList();
		data.allGenes = populationSet.getAllGeneNames();
		data.activeGenes = studySet.getAllGeneNames();
		
		data.p = 0.5;
		data.q = ((double)data.activeGenes.size())/data.allGenes.size();

		double eps = 0.0001;

		data.calculateParamters();

		while (true)
		{
			double pNext = (double)(data.ag) / (data.ag + data.sg);
			double qNext = (double)(data.an) / (data.an + data.sn);

			/* No active terms in the init phase */
			data.activeTerms.clear();
			data.optimizeForTerms(graph);

			data.calculateParamters();
			pNext = (double)(data.ag) / (data.ag + data.sg);
			qNext = (double)(data.an) / (data.an + data.sn);

			if (Double.isNaN(pNext) || Double.isNaN(qNext))
			{
				System.err.println("Breaked: #terms=" + data.activeTerms.size() + " ag=" + data.ag + " sg="+data.sg);
				break;
			}

			if (Math.abs(qNext - data.q) < eps) break;
			if (Math.abs(pNext - data.p) < eps) break;
			
			data.p = pNext;
			data.q = qNext;
		} 
		
		/* Now do the term for term calculation */
		/* TODO: Fix  MTC issue */
		TermForTermCalculation tftc = new TermForTermCalculation();
		EnrichedGOTermsResult results = tftc.calculateStudySet(graph, goAssociations, populationSet, studySet, testCorrection);

		/* Merge results and flag all inactive terms as insignificant */
		for (AbstractGOTermProperties prop : results)
		{
			if (!data.activeTerms.contains(prop.goTerm.getID()))
			{
				prop.p = prop.p_adjusted = 1;
			}
		}
		
		return results;
	}


	@Override
	public String getDescription()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getName()
	{
		return "Probabilistic";
	}

}
