package ontologizer.calculation.b2g;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Random;
import java.util.Set;

import ontologizer.ByteString;
import ontologizer.GOTermEnumerator;
import ontologizer.StudySet;
import ontologizer.association.AssociationContainer;
import ontologizer.calculation.EnrichedGOTermsResult;
import ontologizer.go.GOGraph;
import ontologizer.go.TermID;

/**
 * Score of a setting in which alpha and beta are not known.
 *
 * @author Sebastian Bauer
 */
class FixedAlphaBetaScore extends Bayes2GOScore
{
	private int proposalSwitch;
	private TermID proposalT1;
	private TermID proposalT2;

	protected final double [] ALPHA = new double[] {0.0000001,0.05, 0.1,0.15,0.2,0.25,0.3,0.35,0.4,0.45,0.5, 0.55,0.6,0.65,0.7,0.75,0.8,0.85,0.9,0.95};
	private int alphaIdx = 0;
	private int oldAlphaIdx;
	protected int totalAlpha[] = new int[ALPHA.length];
	private boolean doAlphaMCMC = true;

	protected final double [] BETA = ALPHA;
	private int betaIdx = 0;
	private int oldBetaIdx;
	protected int totalBeta[] = new int[BETA.length];
	private boolean doBetaMCMC = true;

	protected final int [] EXPECTED_NUMBER_OF_TERMS = new int[]{1,2,3,4,5,6,7,8,9,10,12,15,17,20,25};
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

	private BigInteger totalN00 = new BigInteger("0");
	private BigInteger totalN01 = new BigInteger("0");
	private BigInteger totalN10 = new BigInteger("0");
	private BigInteger totalN11 = new BigInteger("0");
	private BigInteger totalT = new BigInteger("0");

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

	public FixedAlphaBetaScore(Random rnd, List<TermID> termList, GOTermEnumerator populationEnumerator, Set<ByteString> observedActiveGenes)
	{
		super(rnd, termList, populationEnumerator, observedActiveGenes);

		n10 = observedActiveGenes.size();
		n00 = population.size() - n10;
	}

	@Override
	public void hiddenGeneActivated(ByteString gene)
	{
		if (observedActiveGenes.contains(gene))
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
	public void hiddenGeneDeactivated(ByteString gene)
	{
		if (observedActiveGenes.contains(gene))
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

				for (TermID tid : activeTerms)
					if (activeTermPos-- == 0) proposalT1 = tid;
				proposalT2 = inactiveTermsArray[inactiveTermPos];

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
				throw new RuntimeException("MCMC requested but no proposal possible");

			oldExpIdx = expIdx;
			expIdx = choose;
		}
	}

	@Override
	public double getScore()
	{
		double alpha;
		double beta;
		double p;

		if (Double.isNaN(this.alpha))
			alpha = ALPHA[alphaIdx];
		else alpha = this.alpha;

		if (Double.isNaN(this.beta))
			beta = BETA[betaIdx];
		else beta = this.beta;

		if (Double.isNaN(this.p))
			p = (double)EXPECTED_NUMBER_OF_TERMS[expIdx] / termsArray.length;
		else p = this.p;

		double newScore2 = Math.log(alpha) * n10 + Math.log(1-alpha)*n00 + Math.log(1-beta)*n11 + Math.log(beta)*n01;

		if (usePrior)
			newScore2 += Math.log(p)*activeTerms.size() + Math.log(1-p)*(termsArray.length - activeTerms.size());

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
		long size = termsArray.length + activeTerms.size() * numInactiveTerms;
		return size;
	}

	@Override
	public void record()
	{
		super.record();

		totalN00 = totalN00.add(new BigInteger(new String(n00 +"")));
		totalN01 = totalN01.add(new BigInteger(new String(n01 +"")));
		totalN10 = totalN10.add(new BigInteger(new String(n10 +"")));
		totalN11 = totalN11.add(new BigInteger(new String(n11 +"")));

		totalAlpha[alphaIdx]++;
		totalBeta[betaIdx]++;
		totalExp[expIdx]++;

		totalT = totalT.add(new BigInteger(new String(activeTerms.size() + "")));
	}

	public double getAvgN00()
	{
		BigDecimal avgN00 = new BigDecimal(totalN00.toString());
		return avgN00.divide(new BigDecimal(Integer.toString(numRecords)),15,BigDecimal.ROUND_HALF_EVEN).doubleValue();
	}

	public double getAvgN01()
	{
		BigDecimal avgN01 = new BigDecimal(totalN01.toString());
		return avgN01.divide(new BigDecimal(Integer.toString(numRecords)),15,BigDecimal.ROUND_HALF_EVEN).doubleValue();
	}

	public double getAvgN10()
	{
		BigDecimal avgN10 = new BigDecimal(totalN10.toString());
		return avgN10.divide(new BigDecimal(Integer.toString(numRecords)),15,BigDecimal.ROUND_HALF_EVEN).doubleValue();
	}

	public double getAvgN11()
	{
		BigDecimal avgN11 = new BigDecimal(totalN11.toString());
		return avgN11.divide(new BigDecimal(Integer.toString(numRecords)),15,BigDecimal.ROUND_HALF_EVEN).doubleValue();
	}

	public double getAvgT()
	{
		BigDecimal avgT = new BigDecimal(totalT.toString());
		return avgT.divide(new BigDecimal(Integer.toString(numRecords)),15,BigDecimal.ROUND_HALF_EVEN).doubleValue();
	}
}

