package ontologizer.calculation.b2g;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.Set;

import ontologizer.enumeration.TermEnumerator;
import ontologizer.ontology.TermID;
import ontologizer.types.ByteString;
import sonumina.collections.IntMapper;
import sonumina.collections.ObjectIntHashMap;

/**
 * The base class of bayes2go Score.
 *
 * For efficiency reasons terms and genes are represented by own ids.
 *
 * @author Sebastian Bauer
 */
abstract public class Bayes2GOScore extends Bayes2GOScoreBase
{
	/** Source of randomness */
	protected Random rnd;

	/** Array holding the observed values for each gene */
	protected double [] observedValueOfGene;

	protected IntMapper<ByteString> geneMapper;

	/** Array of terms */
	protected TermID [] termsArray;

	/** Maps the term to the index in termsArray */
	protected ObjectIntHashMap<TermID> term2TermsIdx;

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

	public static GeneIDs[] makeTermLinks(List<TermID> termList, TermEnumerator populationEnumerator, IntMapper<ByteString> geneMapper)
	{
		GeneIDs[] termLinks = new GeneIDs[termList.size()];

		int i = 0;
		for (TermID tid : termList)
		{
			/* Fill in the links */
			termLinks[i] = new GeneIDs(populationEnumerator.getAnnotatedGenes(tid).totalAnnotated.size());
			int j=0;
			for (ByteString gene : populationEnumerator.getAnnotatedGenes(tid).totalAnnotated)
			{
				termLinks[i].gid[j] = geneMapper.getIndex(gene);
				j++;
			}

			i++;
		}

		return termLinks;
	}

	private Bayes2GOScore(Random rnd, List<TermID> termList, TermEnumerator populationEnumerator, IGeneValueProvider geneValueProvider, IntMapper<ByteString> geneMapper)
	{
		super(makeTermLinks(termList, populationEnumerator, geneMapper), geneMapper.getSize());

		this.rnd = rnd;
		this.geneMapper = geneMapper;

		double threshold = geneValueProvider.getThreshold();
		boolean smallerIsBetter = geneValueProvider.smallerIsBetter();

		/* Initialize basics of genes */
		observedValueOfGene = new double[geneMapper.getSize()];

		for (int i = 0; i < geneMapper.getSize(); i++)
		{
			observedValueOfGene[i] = geneValueProvider.getGeneValue(geneMapper.get(i));
			if (smallerIsBetter) observedGenes[i] = observedValueOfGene[i] <= threshold;
			else observedGenes[i] = observedValueOfGene[i] >= threshold;
		}

		/* Initialize basics of terms */
		termsArray = new TermID[termList.size()];
		termActivationCounts = new int[termList.size()];

		int i = 0;
		term2TermsIdx = new ObjectIntHashMap<TermID>(termList.size() * 3 / 2);
		for (TermID tid : termList)
		{
			term2TermsIdx.put(tid,i);
			termsArray[i]=tid;
			i++;
		}
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
		this(rnd, termList, populationEnumerator, geneValueProvider, IntMapper.create(populationEnumerator.getGenes()));
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

	public void exchange(int t1, int t2)
	{
		switchState(t1);
		switchState(t2);
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
