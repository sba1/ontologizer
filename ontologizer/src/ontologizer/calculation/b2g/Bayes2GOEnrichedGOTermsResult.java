package ontologizer.calculation.b2g;

import ontologizer.association.AssociationContainer;
import ontologizer.calculation.EnrichedGOTermsResult;
import ontologizer.ontology.Ontology;
import ontologizer.ontology.TermID;
import ontologizer.set.StudySet;
import sonumina.collections.IntMapper;


/**
 * Cares about the result of the b2g approach.
 *
 * @author Sebastian Bauer
 */
public class Bayes2GOEnrichedGOTermsResult extends EnrichedGOTermsResult
{
	private Bayes2GOScore score;

	/* FIXME: Remove this */
	private IntMapper<TermID> termMapper;

	public Bayes2GOEnrichedGOTermsResult(Ontology go,
			AssociationContainer associations, StudySet studySet,
			int populationGeneCount)
	{
		super(go, associations, studySet, populationGeneCount);
	}

	public void setScore(Bayes2GOScore score)
	{
		this.score = score;
	}

	public Bayes2GOScore getScore()
	{
		return score;
	}

	public void setTermMapper(IntMapper<TermID> termMapper)
	{
		this.termMapper = termMapper;
	}

	public IntMapper<TermID> getTermMapper()
	{
		return termMapper;
	}
}
