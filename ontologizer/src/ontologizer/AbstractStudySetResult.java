package ontologizer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import ontologizer.set.StudySet;

/**
 * 
 * @author Sebastian Bauer
 *
 * @param <Result>
 */
abstract public class AbstractStudySetResult<Result> implements Iterable<Result>
{
	protected ArrayList<Result> list = new ArrayList<Result>();
	private HashMap<String,Integer> goID2Index = new HashMap<String,Integer>();
	private int index = 0;

	private int populationGeneCount;
	private int studyGeneCount;
	private StudySet studySet;

	/**
	 * 
	 * @param studySet the study set where this result should belong to.
	 * @param populationGeneCount number of genes of the populations (FIXME: This infact is redundant)
	 */
	public AbstractStudySetResult(StudySet studySet, int populationGeneCount)
	{
		this.populationGeneCount = populationGeneCount;
		this.studySet = studySet;
		this.studyGeneCount = studySet.getGeneCount();
	}

	/**
	 * Add new Term properties
	 * 
	 * @param goTermID
	 * @param prop
	 */
	public void addGOTermProperties(String goTermID, Result prop)
	{
		list.add(prop);
		Integer integer = new Integer(index);
		goID2Index.put(goTermID,integer);
		index++;
	}

	/**
	 * Return the studyset for these results.
	 * 
	 * @return
	 */
	public StudySet getStudySet()
	{
		return studySet;
	}
	
	/**
	 * Return the property of the given TermID
	 * 
	 * @param goID
	 * @return
	 */
	public Result getGOTermProperties(String goID)
	{
		Integer index = goID2Index.get(goID);
		if (index == null) return null;
		return list.get(index);
	}

	public Iterator<Result> iterator()
	{
		return list.iterator();
	}
}
