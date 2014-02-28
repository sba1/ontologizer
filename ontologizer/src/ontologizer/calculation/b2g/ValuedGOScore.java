package ontologizer.calculation.b2g;

import java.util.List;
import java.util.Random;

import ontologizer.enumeration.GOTermEnumerator;
import ontologizer.go.TermID;
import ontologizer.parser.ValuedItemAttribute;
import ontologizer.set.StudySet;

/**
 * This implements a score that takes values that are associated the genes into account.
 * In contrast to FixedAlphaBetaScore we don't rely on a threshold that accounts for
 * the boolean labeling of genes. In order words, here we assume that the study set
 * contains the full population.
 * 
 * @author Sebastian Bauer
 */
public class ValuedGOScore extends Bayes2GOScore
{
	private int proposalSwitch;
	private TermID proposalT1;
	private TermID proposalT2;

	private double [] observedValueOfGene;

	public ValuedGOScore(Random rnd, List<TermID> termList,
			GOTermEnumerator populationEnumerator,
			StudySet valuedStudySet)
	{
		super(rnd, termList, populationEnumerator, valuedStudySet.getAllGeneNames());

		observedValueOfGene = new double[genes.length];
		for (int i=0; i < genes.length; i++)
			observedValueOfGene[i] = ((ValuedItemAttribute)valuedStudySet.getItemAttribute(genes[i])).getValue();
	}

	double score;

	@Override
	public double getScore()
	{
		return score;
	}

	@Override
	public void proposeNewState(long rand)
	{
		long oldPossibilities = getNeighborhoodSize();

		proposalSwitch = -1;
		proposalT1 = null;
		proposalT2 = null;

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
	}

	@Override
	public void hiddenGeneActivated(int gid)
	{
		score += 1 - observedValueOfGene[gid];
	}

	@Override
	public void hiddenGeneDeactivated(int gid)
	{
		score -= 1 - observedValueOfGene[gid];
	}

	@Override
	public void undoProposal()
	{
		if (proposalSwitch != -1)	switchState(proposalSwitch);
		else exchange(proposalT2, proposalT1);
	}

	@Override
	public long getNeighborhoodSize()
	{
		return termsArray.length + (termsArray.length - numInactiveTerms) * numInactiveTerms;
	}
}
