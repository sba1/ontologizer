package ontologizer.calculation.b2g;

import java.util.List;
import java.util.Random;

import ontologizer.enumeration.GOTermEnumerator;
import ontologizer.go.TermID;
import ontologizer.set.StudySet;

public class ValuedGOScore extends Bayes2GOScore {

	public ValuedGOScore(Random rnd, List<TermID> termList,
			GOTermEnumerator populationEnumerator,
			StudySet valuedStudySet)
	{
		super(rnd, termList, populationEnumerator, valuedStudySet.getAllGeneNames());
	}

	@Override
	public double getScore()
	{
		return 0;
	}

	@Override
	public void proposeNewState(long rand)
	{
	}

	@Override
	public void hiddenGeneActivated(int gid)
	{
	}

	@Override
	public void hiddenGeneDeactivated(int gid)
	{
	}

	@Override
	public void undoProposal()
	{
	}

	@Override
	public long getNeighborhoodSize()
	{
		return 0;
	}
}
