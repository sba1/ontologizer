package ontologizer.calculation.b2g;

import java.util.List;
import java.util.Random;
import java.util.Set;

import ontologizer.enumeration.GOTermEnumerator;
import ontologizer.go.TermID;
import ontologizer.types.ByteString;

/**
 * Score of a setting in which alpha and beta are not known.
 * 
 * @author Sebastian Bauer
 */
public class FixedAlphaBetaScore extends Bayes2GOScore
{
	private int proposalSwitch;
	private TermID proposalT1;
	private TermID proposalT2;

	protected  double [] ALPHA = new double[] {0.0000001,0.05, 0.1,0.15,0.2,0.25,0.3,0.35,0.4,0.45,0.5, 0.55,0.6,0.65,0.7,0.75,0.8,0.85,0.9,0.95};
	private int alphaIdx = 0;
	private int oldAlphaIdx;
	protected int [] totalAlpha = new int[ALPHA.length];
	private boolean doAlphaMCMC = true;
	
	protected double [] BETA = ALPHA;
	private int betaIdx = 0;
	private int oldBetaIdx;
	protected int totalBeta[] = new int[BETA.length];
	private boolean doBetaMCMC = true;
	
	protected final int [] EXPECTED_NUMBER_OF_TERMS = new int[]{1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20};
	private int expIdx = 0;
	private int oldExpIdx;
	protected int totalExp[] = new int[EXPECTED_NUMBER_OF_TERMS.length];
	private boolean doExpMCMC = true;

	protected double alpha = Double.NaN;
	protected double beta = Double.NaN;

	private int n00;
	private int n01;
	private int n10;
	private int n11;

	private long totalN00;
	private long totalN01;
	private long totalN10;
	private long totalN11;
	private long totalT;

	public void setAlpha(double alpha)
	{
		this.alpha = alpha;
		doAlphaMCMC = Double.isNaN(alpha);
	}
	
	public void setBeta(double beta)
	{
		this.beta = beta;
		doBetaMCMC = Double.isNaN(beta);
	}

	@Override
	public void setExpectedNumberOfTerms(double terms)
	{
		super.setExpectedNumberOfTerms(terms);
		doExpMCMC = Double.isNaN(terms);
	}

	public void setMaxAlpha(double maxAlpha)
	{
		int span;

		if (Double.isNaN(maxAlpha)) maxAlpha = 1;

		if (maxAlpha < 0.01) maxAlpha = 0.01;
		if (maxAlpha > 0.99999999) span = 20;
		else span = 19;

		ALPHA = new double[20];
		totalAlpha = new int[20];

		ALPHA[0] = 0.0000001;
		for (int i=1;i<20;i++)
			ALPHA[i] = i * maxAlpha / span;
	}
	
	public void setMaxBeta(double maxBeta)
	{
		int span;
		
		if (Double.isNaN(maxBeta)) maxBeta = 1;

		if (maxBeta < 0.01) maxBeta = 0.01;
		if (maxBeta > 0.99999999) span = 20;
		else span = 19;

		BETA = new double[20];
		totalBeta = new int[20];

		BETA[0] = 0.0000001;
		for (int i=1;i<20;i++)
			BETA[i] = i * maxBeta / span;
		
	}

	public FixedAlphaBetaScore(Random rnd, List<TermID> termList, GOTermEnumerator populationEnumerator, Set<ByteString> observedActiveGenes)
	{
		super(rnd, termList, populationEnumerator, observedActiveGenes);

		setMaxAlpha(1.);
		setMaxBeta(1.);

		n10 = observedActiveGenes.size();
		n00 = population.size() - n10;
	}

	@Override
	public void hiddenGeneActivated(int gid)
	{
		if (observedGenes[gid])
		{
			n11++;
			n10--;
		} else
		{
			n01++;
			n00--;
		}
	}

	@Override
	public void hiddenGeneDeactivated(int gid)
	{
		if (observedGenes[gid])
		{
			n11--;
			n10++;
		} else
		{
			n01--;
			n00++;
		}
	}

	@Override
	public void proposeNewState(long rand)
	{
		long oldPossibilities = getNeighborhoodSize();

		proposalSwitch = -1;
		proposalT1 = null;
		proposalT2 = null;
		oldAlphaIdx = -1;
		oldBetaIdx = -1;
		oldExpIdx = -1;

		if ((!doAlphaMCMC && !doBetaMCMC && !doExpMCMC)|| rnd.nextBoolean())
		{
			long choose = Math.abs(rand) % oldPossibilities;

			if (choose < termsArray.length)
			{
				/* on/off */
				proposalSwitch = (int)choose;
				switchState(proposalSwitch);
			}	else
			{
				long base = choose - termsArray.length;
				
				int activeTermPos = (int)(base / numInactiveTerms);
				int inactiveTermPos = (int)(base % numInactiveTerms);
				
				proposalT1 = termsArray[termPartition[activeTermPos + numInactiveTerms]];
				proposalT2 = termsArray[termPartition[inactiveTermPos]];
	
				exchange(proposalT1, proposalT2);
			}
		} else
		{
			int max = 0;
			
			if (doAlphaMCMC) max += ALPHA.length;
			if (doBetaMCMC) max += BETA.length;
			if (doExpMCMC) max += EXPECTED_NUMBER_OF_TERMS.length;

			int choose = Math.abs((int)rand) % max;

			if (doAlphaMCMC)
			{
				if (choose < ALPHA.length)
				{
					oldAlphaIdx = alphaIdx;
					alphaIdx = choose;
					return;
				}
				choose -= ALPHA.length;
			}
			
			if (doBetaMCMC)
			{
				if (choose < BETA.length)
				{
					oldBetaIdx = betaIdx;
					betaIdx = choose;
					return;
				}
				choose -= BETA.length;
			}

			if (!doExpMCMC)
				throw new RuntimeException("MCMC requested proposal but no proposal is possible");

			oldExpIdx = expIdx;
			expIdx = choose;
		}
	}
	
	public final double getAlpha()
	{
		double alpha;

		if (Double.isNaN(this.alpha))
			alpha = ALPHA[alphaIdx];
		else alpha = this.alpha;

		return alpha;
	}
	
	public final double getBeta()
	{
		double beta;

		if (Double.isNaN(this.beta))
			beta = BETA[betaIdx];
		else beta = this.beta;

		return beta;
	}
	
	public final double getP()
	{
		double p;
		if (Double.isNaN(this.p))
			p = (double)EXPECTED_NUMBER_OF_TERMS[expIdx] / termsArray.length;
		else p = this.p;

		return p;
	}
	
	@Override
	public double getScore()
	{
		double alpha;
		double beta;
		double p;
	
		alpha = getAlpha();
		beta = getBeta();
		p = getP();

		double newScore2 = Math.log(alpha) * n10 + Math.log(1-alpha)*n00 + Math.log(1-beta)*n11 + Math.log(beta)*n01;

		if (usePrior)
			newScore2 += Math.log(p)*(termsArray.length - numInactiveTerms) + Math.log(1-p)*numInactiveTerms;

//		newScore2 -= Math.log(alpha) * observedActiveGenes.size() + Math.log(1-beta)* (population.size() - observedActiveGenes.size());
		return newScore2;
	}
	
	public void undoProposal()
	{
		if (proposalSwitch != -1)	switchState(proposalSwitch);
		else if (proposalT1 != null) exchange(proposalT2, proposalT1);
		else if (oldAlphaIdx != -1) alphaIdx = oldAlphaIdx;
		else if (oldBetaIdx != -1) betaIdx = oldBetaIdx;
		else if (oldExpIdx != -1) expIdx = oldExpIdx;
		else throw new RuntimeException("Wanted to undo a proposal that wasn't proposed");
	}

	public long getNeighborhoodSize()
	{
		long size = termsArray.length + (termsArray.length - numInactiveTerms) * numInactiveTerms;
		return size;
	}
	
	@Override
	public void record()
	{
		super.record();

		totalN00 += n00;
		totalN01 += n01;
		totalN10 += n10;
		totalN11 += n11;

		totalAlpha[alphaIdx]++;
		totalBeta[betaIdx]++;
		totalExp[expIdx]++;
		totalT += (termsArray.length - numInactiveTerms);
	}

	public double getAvgN00()
	{
		return (double)totalN00 / numRecords;
	}

	public double getAvgN01()
	{
		return (double)totalN01 / numRecords;
	}

	public double getAvgN10()
	{
		return (double)totalN10 / numRecords;
	}

	public double getAvgN11()
	{
		return (double)totalN11 / numRecords;
	}
	
	public double getAvgT()
	{
		return (double)totalT / numRecords;
	}
	
	/**
	 * Returns possible alpha values.
	 * 
	 * @return
	 */
	public double [] getAlphaValues()
	{
		return ALPHA;
	}
	
	/**
	 * Returns the distribution of the given counts.
	 * 
	 * @param counts
	 * @return
	 */
	private double [] getDistribution(int [] counts)
	{
		double [] dist = new double[counts.length];
		int total = 0;
		for (int a : counts)
			total += a;
		for (int i=0;i<counts.length;i++)
		{
			dist[i] = counts[i] / (double)total;
		}
		return dist;
	}
	
	/**
	 * Returns the inferred alpha distribution.
	 *  
	 * @return
	 */
	public double [] getAlphaDistribution()
	{
		return getDistribution(totalAlpha);
	}
	
	/**
	 * Returns possible alpha values.
	 * 
	 * @return
	 */
	public double [] getBetaValues()
	{
		return BETA;
	}
	
	/**
	 * Returns the inferred alpha distribution.
	 *  
	 * @return
	 */
	public double [] getBetaDistribution()
	{
		return getDistribution(totalBeta);
	}

}

