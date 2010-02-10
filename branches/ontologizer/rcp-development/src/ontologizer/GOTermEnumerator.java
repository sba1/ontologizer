package ontologizer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

import ontologizer.association.Association;
import ontologizer.association.Gene2Associations;
import ontologizer.go.GOGraph;
import ontologizer.go.TermID;
import ontologizer.go.GOGraph.IVisitingGOVertex;

/**
 * This class encapsulates the enumeration of explicit and implicit
 * annotations for an set of genes. You can iterate conveniently over
 * all GO terms where genes have been annotated to. Note that if you
 * are only interested in the gene counts per term you should use
 * the class GOTermCounter as this is much faster. 
 *
 * @author Sebastian Bauer
 */
public class GOTermEnumerator implements Iterable<TermID>
{
	public class GOTermAnnotatedGenes
	{
		/** List of directly annotated genes TODO: Make private */
		public List<ByteString> directAnnotated = new ArrayList<ByteString>();

		/** List of genes annotated at whole TODO: Make private */
		public List<ByteString> totalAnnotated = new ArrayList<ByteString>();

		public int directAnnotatedCount()
		{
			return directAnnotated.size();
		}

		public int totalAnnotatedCount()
		{
			return totalAnnotated.size();
		}
	}

	/** The GO graph */
	private GOGraph graph;

	private HashMap<TermID,GOTermAnnotatedGenes> map;

	/** Holds the number of suspicious annotations */
//	private int suspiciousCount;

	/**
	 * Construct the enumerator.
	 * 
	 * @param graph the GO graph
	 */
	public GOTermEnumerator(GOGraph graph)
	{
		this.graph = graph;
		
		map = new HashMap<TermID,GOTermAnnotatedGenes>();
	}

	/**
	 * Pushes the given gene association into the enumerator. I.e.
	 * add the gene in question to all terms annotating that gene.
	 * 
	 * @param geneAssociations the gene associations
	 */
	public void push(Gene2Associations geneAssociations)
	{
		ByteString geneName = geneAssociations.name();
		
		/* Check for suspicious annotations. An annotation i is suspicious
		 * if there exists a more specialized annotation orgininating from
		 * i. If an annotation isn't suspicious it is valid and placed in
		 * validAssocs */
/*		LinkedList<Association> validAssocs = new LinkedList<Association>();
		for (Association i : geneAssociations)
		{
			boolean existsPath = false;

			for (Association j : geneAssociations)
			{
				if (i.getGoID().equals(j.getGoID())) continue;

				if (graph.existsPath(i.getGoID(),j.getGoID()))
				{
					suspiciousCount++;
					existsPath = true;
				}
			}

			if (!existsPath)
			{
*/				/* No direct path from i to another assoc exists hence the association is
				 * valid */
/*				validAssocs.add(i);
			}
		}
*/	
		/* Here we ignore the association qualifier (e.g. colocalized_with)
		 * completely.
		 */
		
		/* At first add the direct counts */
		for (Association association : geneAssociations)
		{
			TermID goTermID = association.getTermID();
			
			if (!graph.isRelevantTermID(goTermID))
				continue;
			
			GOTermAnnotatedGenes termGenes = map.get(goTermID);
			
			/* Create an entry if it doesn't exist */
			if (termGenes == null)
			{
				termGenes = new GOTermAnnotatedGenes();
				map.put(goTermID,termGenes);
			}
			
			termGenes.directAnnotated.add(geneName);
		}

		/* Now build goTermSet */
		HashSet<TermID> goTermSet = new HashSet<TermID>();
		for (Association association : geneAssociations)
			goTermSet.add(association.getTermID());

		/* Then add the total counts */

		/**
		 * The term visitor: To all visited terms (which here
		 * all terms up from the goTerms of the set) add the
		 * given gene.
		 *
		 * @author Sebastian Bauer
		 */
		class VisitingGOVertex implements IVisitingGOVertex
		{
			private ByteString geneName;

			public VisitingGOVertex(ByteString geneName)
			{
				this.geneName = geneName;
			}

			public void visiting(TermID goTermID)
			{
				if (graph.isRelevantTermID(goTermID))
				{
					GOTermAnnotatedGenes termGenes = map.get(goTermID);
	
					if (termGenes == null)
					{
						termGenes = new GOTermAnnotatedGenes();
						map.put(goTermID,termGenes);
					}
					termGenes.totalAnnotated.add(geneName);
				}
			}
		};

		/* Create the visting */
		VisitingGOVertex vistingGOVertex = new VisitingGOVertex(geneName);
		
		/* Walk from goTerm to source by using vistingGOVertex */
		graph.walkToSource(goTermSet,vistingGOVertex);
	}

	/**
	 * Return genes directly or indirectly annotated to the given
	 * goTermID.
	 * 
	 * @param goTermID
	 * @return
	 */
	public GOTermAnnotatedGenes getAnnotatedGenes(TermID goTermID)
	{
		if (map.containsKey(goTermID))
			return map.get(goTermID);
		else
			return new GOTermAnnotatedGenes();
	}
	
	
	/**
	 * 
	 * @author Sebastian Bauer
	 */
	public class GOTermOftenAnnotatedCount implements Comparable<GOTermOftenAnnotatedCount>
	{
		public TermID term;
		public int counts;

		public int compareTo(GOTermOftenAnnotatedCount o)
		{
			/* We sort reversly */
			return o.counts - counts;
		}
	};

	/**
	 * Returns the terms which shares genes with the given term and neither a ascendant nor
	 * descendant.
	 *  
	 * @param goTermID
	 * @return
	 */
	public GOTermOftenAnnotatedCount [] getTermsOftenAnnotatedWithAndNotOnPath(TermID goTermID)
	{
		ArrayList<GOTermOftenAnnotatedCount> list = new ArrayList<GOTermOftenAnnotatedCount>();

		GOTermAnnotatedGenes goTermIDAnnotated = map.get(goTermID);
		if (goTermIDAnnotated == null) return null;
		
		/* For every term genes are annotated to */
		for (TermID curTerm : map.keySet())
		{
			/* Ignore terms on the same path */
			if (graph.isRootGOTermID(curTerm)) continue;
			if (curTerm.equals(goTermID)) continue;
			if (graph.existsPath(curTerm,goTermID) || graph.existsPath(goTermID,curTerm))
				continue;

			/* Find out the number of genes which are annotated to both terms */
			int count = 0;
			GOTermAnnotatedGenes curTermAnnotated = map.get(curTerm);
			for (ByteString gene : curTermAnnotated.totalAnnotated)
			{
				if (goTermIDAnnotated.totalAnnotated.contains(gene))
					count++;
			}

			if (count != 0)
			{
				GOTermOftenAnnotatedCount tc = new GOTermOftenAnnotatedCount();
				tc.term = curTerm;
				tc.counts = count;
				list.add(tc);
			}
		}

		GOTermOftenAnnotatedCount [] termArray = new GOTermOftenAnnotatedCount[list.size()];
		list.toArray(termArray);
		Arrays.sort(termArray);

		return termArray;
	}
	
	public Iterator<TermID> iterator()
	{
		return map.keySet().iterator();
	}
	
	/**
	 * Returns the total number of terms to which at least a single gene has been annotated.
	 * 
	 * @return
	 */
	public int getTotalNumberOfAnnotatedTerms()
	{
		return map.size();
	}
	
	
	/**
	 * Returns the currently annotated terms as a set.
	 * 
	 * @return
	 */
	public Set<TermID> getAllAnnotatedTermsAsSet()
	{
		LinkedHashSet<TermID> at = new LinkedHashSet<TermID>();
		for (TermID t : this)
			at.add(t);
		return at;
	}
	
	/**
	 * Returns the currently annotated terms as a list.
	 * 
	 * @return
	 */
	public List<TermID> getAllAnnotatedTermsAsList()
	{
		ArrayList<TermID> at = new ArrayList<TermID>();
		for (TermID t : this)
			at.add(t);
		return at;
	}

	public Set<ByteString> getGenes()
	{
		LinkedHashSet<ByteString> genes = new LinkedHashSet<ByteString>();
		
		for (Entry<TermID,GOTermAnnotatedGenes> ent : map.entrySet())
			genes.addAll(ent.getValue().totalAnnotated);

		return genes;
	}
}
