package ontologizer;

import java.util.ArrayList;
import java.util.Iterator;

import ontologizer.calculation.EnrichedGOTermsResult;

public class StudySetResultList implements Iterable<EnrichedGOTermsResult>
{
	private String name = new String();
	private ArrayList<EnrichedGOTermsResult> list = new ArrayList<EnrichedGOTermsResult>();
	
	public void addStudySetResult(EnrichedGOTermsResult studySetRes)
	{
		list.add(studySetRes);
	}
	
	public ArrayList<EnrichedGOTermsResult> getStudySetResults()
	{
		return list;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public Iterator<EnrichedGOTermsResult> iterator()
	{
		return list.iterator();
	}
	
	public int size()
	{
		return list.size();
	}
}
