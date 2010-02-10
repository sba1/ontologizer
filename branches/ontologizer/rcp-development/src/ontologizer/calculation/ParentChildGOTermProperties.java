package ontologizer.calculation;

public class ParentChildGOTermProperties extends AbstractGOTermProperties
{
	/** number of parents */
	public int nparents;
	
	/** number of genes annotated to family (term and parents) */
	/** in population set */
	public int popFamilyGenes;
	/** in study set */
	public int studyFamilyGenes;
	
	private static final String [] propertyNames = new String[]{
		"ID","Pop.total","Pop.term","Study.total","Study.term","Pop.family", "Study.family", "nparents", "is.trivial", "p", "p.adjusted", "p.min"
		};
	
	@Override
	public int getNumberOfProperties()
	{
		return propertyNames.length;
	}

	@Override
	public String getPropertyName(int propNumber)
	{
		return propertyNames[propNumber];
	}

	@Override
	public String getProperty(int propNumber)
	{
		switch (propNumber)
		{
			case	0: return goTerm.getIDAsString();
			case	1: return null; /* population gene count */
			case	2: return Integer.toString(annotatedPopulationGenes);
			case	3: return null; /* study gene count */
			case 	4: return Integer.toString(annotatedStudyGenes);
			case	5: return Integer.toString(popFamilyGenes);
			case	6: return Integer.toString(studyFamilyGenes);
			case	7: return Integer.toString(nparents);
			case	8: return Boolean.toString(ignoreAtMTC);
			case	9: return Double.toString(p);
			case	10: return Double.toString(p_adjusted);
			case	11: return Double.toString(p_min);
		}
		return null;
	}

	public boolean isPropertyPopulationGeneCount(int propNumber)
	{
		return propNumber == 1;
	}

	public boolean isPropertyStudyGeneCount(int propNumber)
	{
		return propNumber == 3;
	}


}
