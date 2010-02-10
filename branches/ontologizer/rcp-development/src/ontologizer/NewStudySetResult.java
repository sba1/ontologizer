package ontologizer;

import ontologizer.calculation.TermForTermGOTermProperties;

/**
 *
 * @author Sebastian Bauer
 *
 */
public class NewStudySetResult extends AbstractStudySetResult<TermForTermGOTermProperties>
{
	public NewStudySetResult(StudySet studySet, int populationGeneCount)
	{
		super(studySet,populationGeneCount);
	}
}
