package ontologizer.calculation.b2g;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Set;

import ontologizer.enumeration.GOTermEnumerator;
import ontologizer.go.TermID;
import ontologizer.types.ByteString;

/**
 * A basic container representing a set of genes 
 * 
 * @author Sebastian Bauer
 */
class GeneIDs
{
	public int [] gid;
	
	public GeneIDs(int size)
	{
		gid = new int[size];
	}
}

/**
 * The base class of bayes2go Score.
 * 
 * For efficiency reasons terms and genes are represented by own ids.
 * 
 * @author Sebastian Bauer
 */
abstract class Bayes2GOScore
{
	/** Source of randomness */
	protected Random rnd;
	
	protected GOTermEnumerator populationEnumerator;
	protected Set<ByteString> population;
	
	/** Array of terms */
	protected TermID [] termsArray;

	/** Indicates the activation state of a term */
	protected boolean [] isActive;

	/**
	 * Contains indices to terms of termsArray.
	 */
	protected int [] termPartition;

	/**
	 * The current number of inactive terms. Represents the
	 * first part of the partition.
	 */
	protected int numInactiveTerms;

	/**
	 * Contains the position/index of the terms in the partition
	 * (i.e., termPartition[positionOfTermInPartition[i]] = i must hold)
	 */
	protected int [] positionOfTermInPartition;

	/** Array indicating the genes that have been observed */
	protected boolean [] observedGenes;
	
	/** Array that indicate the activation counts of the genes */
	protected int [] activeHiddenGenes;
	
	/** Maps genes to an unique gene index */
	protected HashMap<ByteString,Integer> gene2GenesIdx = new HashMap<ByteString,Integer>();
	
	protected ByteString [] genes;
	
	/** Maps the term to the index in allTermsArray */
	protected HashMap<TermID,Integer> term2TermsIdx = new HashMap<TermID,Integer>();
	
	/** Maps a term id to the ids of the genes to that the term is annotated */
	protected GeneIDs [] termLinks;

	protected int numRecords;
	protected int [] termActivationCounts;

	protected boolean usePrior = true;
	protected double p = Double.NaN;

	public Bayes2GOScore(List<TermID> termList, GOTermEnumerator populationEnumerator, Set<ByteString> observedActiveGenes)
	{
		this(null,termList, populationEnumerator, observedActiveGenes);
	}
	
	public Bayes2GOScore(Random rnd, List<TermID> termList, GOTermEnumerator populationEnumerator, Set<ByteString> observedActiveGenes)
	{
		int i;

		this.rnd = rnd;

		/* Initialize basics of genes */
		population = populationEnumerator.getGenes();
		genes = new ByteString[population.size()];
		observedGenes = new boolean[genes.length];
		i=0;
		for (ByteString g : population)
		{
			gene2GenesIdx.put(g,i);
			genes[i] = g;
			observedGenes[i] = observedActiveGenes.contains(g);
			i++;
		}
		activeHiddenGenes = new int[population.size()];

		/* Initialize basics of terms */
		isActive = new boolean[termList.size()];
		termsArray = new TermID[termList.size()];
		termPartition = new int[termList.size()];
		positionOfTermInPartition = new int[termList.size()];
		numInactiveTerms = termList.size();
		termActivationCounts = new int[termList.size()];
		termLinks = new GeneIDs[termList.size()];

		i=0;
		for (TermID tid : termList)
		{
			term2TermsIdx.put(tid,i);
			termsArray[i]=tid;
			termPartition[i] = i;
			positionOfTermInPartition[i] = i;

			/* Fill in the links */
			termLinks[i] = new GeneIDs(populationEnumerator.getAnnotatedGenes(tid).totalAnnotated.size());
			int j=0;
			for (ByteString gene : populationEnumerator.getAnnotatedGenes(tid).totalAnnotated)
			{
				termLinks[i].gid[j] = gene2GenesIdx.get(gene);
				j++;
			}

			i++;
		}
		
		this.populationEnumerator = populationEnumerator;
	}

	public void setUsePrior(boolean usePrior)
	{
		this.usePrior = usePrior;
	}

	public boolean getUsePrior()
	{
		return usePrior;
	}

	public void setExpectedNumberOfTerms(double terms)
	{
		p = (double)terms / termsArray.length;
	}

	public double score(Collection<TermID> activeTerms)
	{
		int [] oldTerms = new int[termsArray.length - numInactiveTerms];
		for (int i=numInactiveTerms,j=0;i<termsArray.length;i++,j++)
			oldTerms[j] = termPartition[i];

		/* Deactivate old terms */
		for (int i=0;i<oldTerms.length;i++)
			switchState(oldTerms[i]);

		/* Enable new terms */
		for (TermID tid : activeTerms)
		{
			Integer idx = term2TermsIdx.get(tid);
			if (idx != null)
				switchState(idx);
		}
		
		double score = getScore();

		/* Disable new terms */
		for (TermID tid : activeTerms)
		{
			Integer idx = term2TermsIdx.get(tid);
			if (idx != null)
				switchState(idx);
		}

		/* Enable old terms again */
		for (int i=0;i<oldTerms.length;i++)
			switchState(oldTerms[i]);

		return score;
	}

	/**
	 * Returns the score of the current state.
	 * 
	 * @return
	 */
	public abstract double getScore();
	
	public abstract void proposeNewState(long rand);
	public void proposeNewState()
	{
		proposeNewState(rnd.nextLong());
	}

	public abstract void hiddenGeneActivated(int gid);
	public abstract void hiddenGeneDeactivated(int gid);
	
//	public long currentTime;
	
	
		
	public void switchState(int toSwitch)
	{
//		long enterTime = System.nanoTime();

		int [] geneIDs = termLinks[toSwitch].gid;

		isActive[toSwitch] = !isActive[toSwitch];
		if (isActive[toSwitch])
		{
			/* A term was added, activate/deactivate genes */
			for (int gid : geneIDs)
			{
				if (activeHiddenGenes[gid] == 0)
				{
					activeHiddenGenes[gid] = 1;
					hiddenGeneActivated(gid);
				} else
				{
					activeHiddenGenes[gid]++;
				}
			}

			/* Move the added set from the 0 partition to the 1 partition (it essentially becomes the
			 * new first element of the 1 element, while the last 0 element gets the original position
			 * of the added set) */
			numInactiveTerms--;
			if (numInactiveTerms != 0)
			{
				int pos = positionOfTermInPartition[toSwitch];
				int e0 = termPartition[numInactiveTerms];
				
				/* Move last element in the partition to left */
				termPartition[pos] = e0;
				positionOfTermInPartition[e0] = pos;
				/* Let be the newly added term the first in the partition */
				termPartition[numInactiveTerms] = toSwitch;
				positionOfTermInPartition[toSwitch] = numInactiveTerms;
			}
		} else
		{
			/* Update hiddenActiveGenes */
			for (int gid : geneIDs)
			{
				if (activeHiddenGenes[gid] == 1)
				{
					activeHiddenGenes[gid] = 0;
					hiddenGeneDeactivated(gid);
				} else
				{
					activeHiddenGenes[gid]--;
				}
			}

			/* Converse of above. Here the removed set, which belonged to the 1 partition,
			 * is moved at the end of the 0 partition while the element at that place is
			 * pushed to the original position of the removed element. */
			if (numInactiveTerms != (termsArray.length - 1))
			{
				int pos = positionOfTermInPartition[toSwitch];
				int b1 = termPartition[numInactiveTerms];
				termPartition[pos] = b1;
				positionOfTermInPartition[b1] = pos;
				termPartition[numInactiveTerms] = toSwitch;
				positionOfTermInPartition[toSwitch] = numInactiveTerms; 
			}
			numInactiveTerms++;

		}
		
//		{
//			long ds = currentTime / 100000000;
//			currentTime += System.nanoTime() - enterTime;
//			if (currentTime / 100000000 != ds)
//				System.out.println(currentTime / 1000000);
//		}
	}

	public void exchange(TermID t1, TermID t2)
	{
		switchState(term2TermsIdx.get(t1));
		switchState(term2TermsIdx.get(t2));
	}


	public abstract void undoProposal();
	
	public abstract long getNeighborhoodSize();

	/**
	 * Records the current settings.
	 */
	public void record()
	{
		for (int i=numInactiveTerms;i<termsArray.length;i++)
			termActivationCounts[termPartition[i]]++;

		numRecords++;
	}
	
	public ArrayList<TermID> getActiveTerms()
	{
		ArrayList<TermID> list = new ArrayList<TermID>(termsArray.length - numInactiveTerms);
		for (int i=numInactiveTerms;i<termsArray.length;i++)
			list.add(termsArray[termPartition[i]]);
		return list;
	}
}
