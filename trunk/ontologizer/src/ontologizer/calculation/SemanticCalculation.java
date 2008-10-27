package ontologizer.calculation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ontologizer.ByteString;
import ontologizer.GOTermEnumerator;
import ontologizer.StudySet;
import ontologizer.association.AssociationContainer;
import ontologizer.go.GOGraph;
import ontologizer.go.TermID;

public class SemanticCalculation
{
	private GOGraph graph;
	private AssociationContainer goAssociations;

	private StudySet allGenesStudy;
	private GOTermEnumerator enumerator;
	private int totalAnnotated;
	
	/** Similarity cache (indexed by term) */
	private Object [] cache;

	/**
	 * Non-redundant associations (indexed by genes).
	 * Objects is an array of terms
	 */
	private Object [] associations;

	private HashMap<ByteString,Integer> gene2index = new HashMap<ByteString,Integer>();

	public SemanticCalculation(GOGraph g, AssociationContainer assoc)
	{
		this.graph = g;
		this.goAssociations = assoc;

		allGenesStudy = new StudySet("population");
		for (ByteString gene : goAssociations.getAllAnnotatedGenes())
			allGenesStudy.addGene(gene,"");

		enumerator = allGenesStudy.enumerateGOTerms(graph, goAssociations);
		totalAnnotated = enumerator.getAnnotatedGenes(graph.getRootGOTerm().getID()).totalAnnotated.size();

		cache = new Object[g.maximumTermID()];

		/* Making associations non-redundant */
		associations = new Object[allGenesStudy.getGeneCount()];
		int i = 0;
		for (ByteString gene : allGenesStudy)
		{
			gene2index.put(gene,i);

			ArrayList<TermID> assocList = assoc.get(gene).getAssociations();
			HashSet<TermID> inducedNodes = new HashSet<TermID>();
			for (TermID tid : assocList)
				inducedNodes.addAll(g.getTermsOfInducedGraph(null, tid));
			HashSet<TermID> nonRedundantTerms = new HashSet<TermID>();

			termloop:
			for (TermID tid : assocList)
			{
				for (TermID desc : g.getTermsDescendants(tid))
				{
					if (inducedNodes.contains(desc))
						continue termloop;
				}
				nonRedundantTerms.add(tid);
			}

			TermID terms[] = new TermID[nonRedundantTerms.size()];
			int j=0;
			for (TermID t : nonRedundantTerms)
				terms[j++]=t;

			/* TODO: Sort terms according to their information content */
			associations[i] = terms;
			i++;
		}
	}

	/**
	 * Returns the information content of the given term.
	 * 
	 * @param id
	 * @return
	 */
	public double p(TermID id)
	{
		return (double)enumerator.getAnnotatedGenes(id).totalAnnotatedCount() / totalAnnotated;
	}
	
	/**
	 * Returns the shared information content of two given terms
	 *  
	 * @param t1
	 * @param t2
	 * @return
	 */
	private double p(TermID t1, TermID t2)
	{
		Set<TermID> sharedParents = graph.getSharedParents(t1, t2);
		double p = 1.0;
	
		/* The information content of two terms is defined as the minimum of
		 * the information content of the shared parents.
		 */
		for (TermID t : sharedParents)
		{
			double newP = p(t);
			if (newP < p) p = newP; 
		}
		return p;
	}

	/**
	 * Returns the similarity of the two given terms.
	 * 
	 * @param t1
	 * @param t2
	 * @return
	 */
	private double sim(TermID t1, TermID t2)
	{
		HashMap<TermID,Double> map = (HashMap<TermID, Double>) cache[t1.id];
		if (map == null)
		{
			map = new HashMap<TermID,Double>();
			cache[t1.id] = map;
		}

		Double val = map.get(t2);
		if (val != null) return val;
		
		double p = -Math.log(p(t1,t2));
		map.put(t2,p);
		return p;
	}

	
	/**
	 * Returns the similarity of two given genes.
	 * 
	 * @param index of gene1
	 * @param index of gene2
	 * @return
	 */
	private double sim(int g1, int g2)
	{
		double sim = 0.0;

		TermID [] tl1 = (TermID[])associations[g1]; 
		TermID [] tl2 = (TermID[])associations[g2];
		
		for (TermID t1 : tl1)
		{
			for (TermID t2 : tl2)
			{
				double newSim = sim(t1,t2);
				if (newSim > sim) sim = newSim;
			}
		}
		return sim;
	}
	
	/**
	 * Returns the similarity of two given genes.
	 * 
	 * @param g1
	 * @param g2
	 * @return
	 */
	public double sim(ByteString g1, ByteString g2)
	{
		double sim = 0.0;

		if (!(goAssociations.containsGene(g1))) return 0;
		if (!(goAssociations.containsGene(g2))) return 0;
		
		List<TermID> tl1 = goAssociations.get(g1).getAssociations();
		List<TermID> tl2 = goAssociations.get(g2).getAssociations();
		
//		System.out.println(tl1.size() + "  " + tl2.size());

		for (TermID t1 : tl1)
		{
			for (TermID t2 : tl2)
			{
				double newSim = sim(t1,t2);
				if (newSim > sim) sim = newSim;
			}
		}
		return sim;
	}

	/**
	 * Calculates the similarity of genes of the study set.
	 * 
	 * @param study
	 * @return
	 */
	public SemanticResult calculate(StudySet study)
	{
		SemanticResult sr = new SemanticResult();
		
		double [][] mat =  new double[study.getGeneCount()][study.getGeneCount()];
		int i=0;
		
		for (ByteString g1 : study)
		{
			int j=0;
			for (ByteString g2 : study)
			{
				mat[i][j]=sim(g1,g2);
				j++;
			}
			i++;
		}
		sr.mat = mat;
		sr.names = study.getGenes();
		sr.name = study.getName();
		sr.assoc = goAssociations;
		sr.g = graph;
		sr.calculation = this;
//		sr.enumerator = enumerator;
//		sr.totalAnnotated = totalAnnotated;
		return sr;
	}

	public void calculate()
	{
		long millis = System.currentTimeMillis();
		int gene = 0;
		
		for (int i=0;i<associations.length;i++)
		{
			for (int j=0;j<associations.length;j++)
			{
				sim(i,j);
			}
			long newMillis = System.currentTimeMillis();
			if (newMillis > millis + 250)
			{
				System.out.println(gene * 100.0 / allGenesStudy.getGeneCount() + "%");
				millis = newMillis;
			}
			gene++;
		}
		
/*		for (ByteString g1 : allGenesStudy)
		{
			for (ByteString g2 : allGenesStudy)
			{
				sim(g1,g2);
			}
			long newMillis = System.currentTimeMillis();
			if (newMillis > millis + 250)
			{
				System.out.println(gene * 100.0 / allGenesStudy.getGeneCount() + "%");
				millis = newMillis;
			}
			gene++;
		}*/
	}
}
