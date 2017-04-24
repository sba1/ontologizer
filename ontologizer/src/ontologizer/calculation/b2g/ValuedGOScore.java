package ontologizer.calculation.b2g;

import java.util.Random;

import ontologizer.ontology.TermID;
import ontologizer.parser.ValuedItemAttribute;
import ontologizer.set.StudySet;
import ontologizer.types.ByteString;
import sonumina.collections.IntMapper;

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
	private int proposalT1;
	private int proposalT2;

	public ValuedGOScore(Random rnd, int [][] termLinks, IntMapper<TermID> termMapper, final IntMapper<ByteString> geneMapper,
			final StudySet valuedStudySet)
	{
		super(rnd, termLinks, geneMapper.getSize(), new Bayes2GOScore.IGeneValueProvider() {
			@Override
			public boolean smallerIsBetter() {
				return true;
			}

			@Override
			public double getThreshold() {
				return 0.1;
			}

			@Override
			public double getGeneValue(int gid) {
				return ((ValuedItemAttribute)valuedStudySet.getItemAttribute(geneMapper.get(gid))).getValue();
			}
		});
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
		proposalT1 = -1;
		proposalT2 = -1;

		long choose = Math.abs(rand) % oldPossibilities;

		if (choose < numTerms)
		{
			/* on/off */
			proposalSwitch = (int)choose;
			switchState(proposalSwitch);
		}	else
		{
			long base = choose - numTerms;

			int activeTermPos = (int)(base / numInactiveTerms);
			int inactiveTermPos = (int)(base % numInactiveTerms);

			proposalT1 = termPartition[activeTermPos + numInactiveTerms];
			proposalT2 = termPartition[inactiveTermPos];

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
		return numTerms + (numTerms - numInactiveTerms) * numInactiveTerms;
	}
}
