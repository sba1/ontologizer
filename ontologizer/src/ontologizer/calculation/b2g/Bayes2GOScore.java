package ontologizer.calculation.b2g;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.Set;

import ontologizer.enumeration.TermEnumerator;
import ontologizer.ontology.TermID;
import ontologizer.types.ByteString;
import sonumina.collections.ObjectIntHashMap;

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
abstract public class Bayes2GOScore
{
	/** Source of randomness */
	protected Random rnd;

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

	/** Array holding the observed values for each gene */
	protected double [] observedValueOfGene;

	/** Array indicating the genes that have been observed */
	protected boolean [] observedGenes;

	/** Array that indicate the activation counts of the genes */
	protected int [] activeHiddenGenes;

	/** Maps genes to an unique gene index */
	protected ObjectIntHashMap<ByteString> gene2GenesIdx;

	protected ByteString [] genes;

	/** Maps the term to the index in allTermsArray */
	protected ObjectIntHashMap<TermID> term2TermsIdx;

	/** Maps a term id to the ids of the genes to that the term is annotated */
	protected GeneIDs [] termLinks;

	protected int numRecords;
	protected int [] termActivationCounts;

	protected boolean usePrior = true;
	protected double p = Double.NaN;

	/**
	 * This is a simple interface that provides values to the genes.
	 */
	public static interface IGeneValueProvider
	{
		/**
		 * Return the value that is associated with the given gene.
		 *
		 * @param gene the gene whose value should be returned
		 * @return the value
		 */
		double getGeneValue(ByteString gene);

		/**
		 * Returns the threshold that specifies when a gene is considered as observed or not.
		 * For instance, if the associated values are p values, then one would return the desired
		 * significance level.
		 *
		 * @return the threshold
		 */
		double getThreshold();

		/**
		 * Returns whether genes whose associated values that are smaller than others are considered
		 * as better candidates than the larger ones. For instance, if the associated values are p
		 * values, one would return true here.
		 *
		 * @return whether small numeric values are better than large ones.
		 */
		boolean smallerIsBetter();
	}

	/**
	 * Constructs a class for calculating the Bayes2GO/MGSA score suitable for an MCMC algorithm.
	 *
	 * @param rnd
	 * @param termList
	 * @param populationEnumerator
	 * @param geneValueProvider
	 */
	public Bayes2GOScore(Random rnd, List<TermID> termList, TermEnumerator populationEnumerator, IGeneValueProvider geneValueProvider)
	{
		int i;

		this.rnd = rnd;

		double threshold = geneValueProvider.getThreshold();
		boolean smallerIsBetter = geneValueProvider.smallerIsBetter();

		/* Initialize basics of genes */
		population = populationEnumerator.getGenes();
		genes = new ByteString[population.size()];
		observedGenes = new boolean[genes.length];
		observedValueOfGene = new double[genes.length];
		gene2GenesIdx = new ObjectIntHashMap<ByteString>(population.size() * 3 / 2);

		i=0;
		for (ByteString g : population)
		{
			gene2GenesIdx.put(g,i);
			genes[i] = g;
			observedValueOfGene[i] = geneValueProvider.getGeneValue(g);
			if (smallerIsBetter)
				observedGenes[i] = observedValueOfGene[i] <= threshold;
			else
				observedGenes[i] = observedValueOfGene[i] >= threshold;
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
		term2TermsIdx = new ObjectIntHashMap<TermID>(termList.size() * 3 / 2);
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

	}

	/**
	 * Constructs a class for calculating the Bayes2GO/MGSA score suitable for an MCMC algorithm.
	 *
	 * @param termList list of terms that can possibly be selected.
	 * @param populationEnumerator terms to genes.
	 * @param observedActiveGenes defines the set of genes that are observed as active.
	 */
	public Bayes2GOScore(List<TermID> termList, TermEnumerator populationEnumerator, Set<ByteString> observedActiveGenes)
	{
		this(null,termList, populationEnumerator, observedActiveGenes);
	}

	/**
	 * Constructs a class for calculating the Bayes2GO score suitable for an MCMC algorithm.
	 *
	 * @param rnd Random source for proposing states.
	 * @param termList list of terms that can possibly be selected.
	 * @param populationEnumerator terms to genes.
	 * @param observedActiveGenes defines the set of genes that are observed as active.
	 */
	public Bayes2GOScore(Random rnd, List<TermID> termList, TermEnumerator populationEnumerator, final Set<ByteString> observedActiveGenes)
	{
		/* Here a gene value provider is constructed that maps the boolean observed state back
		 * to values some values. A gene, that is observed gets a -1, a gene that is not observed
		 * gets a 1. Applied with a threshold of one, this gives back the same set of observed genes.
		 */
		this(rnd, termList, populationEnumerator, new IGeneValueProvider() {
			@Override
			public boolean smallerIsBetter() {
				return true;
			}
			@Override
			public double getThreshold() {
				return 0;
			}
			@Override
			public double getGeneValue(ByteString gene) {
				if (observedActiveGenes.contains(gene)) return -1;
				return 1;
			}
		});
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

	/**
	 * Returns the score of the setting if the given terms
	 * are active and all others are inactive.
	 *
	 * @param activeTerms defines which terms are considered as active
	 * @return the score
	 */
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
			int idx = term2TermsIdx.getIfAbsent(tid, Integer.MAX_VALUE);
			if (idx != Integer.MAX_VALUE)
				switchState(idx);
		}

		double score = getScore();

		/* Disable new terms */
		for (TermID tid : activeTerms)
		{
			int idx = term2TermsIdx.getIfAbsent(tid, Integer.MAX_VALUE);
			if (idx != Integer.MAX_VALUE)
				switchState(idx);
		}

		/* Enable old terms again */
		for (int i=0;i<oldTerms.length;i++)
			switchState(oldTerms[i]);

		return score;
	}

	/**
	 * @return the score of the current state.
	 */
	public abstract double getScore();

	public abstract void proposeNewState(long rand);
	public void proposeNewState()
	{
		proposeNewState(rnd.nextLong());
	}

	public abstract void hiddenGeneActivated(int gid);
	public abstract void hiddenGeneDeactivated(int gid);


	public void switchState(int toSwitch)
	{
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
