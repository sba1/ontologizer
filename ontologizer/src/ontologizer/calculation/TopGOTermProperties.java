package ontologizer.calculation;

public class TopGOTermProperties extends AbstractGOTermProperties
{
	private static final String [] propertyNames = new String[]{
		"ID","Pop.total","Pop.term","Study.total","Study.term","Pop.family", "Study.family", "is.trivial", "p", "p.adjusted", "p.min"
		};

	/** Number of genes annotated to family (term and parents) in population set. */
	public int popFamilyGenes;
	
	/** Number of genes annotated to family (term and parents) in study set. */
	public int studyFamilyGenes;

	@Override
	public int getNumberOfProperties()
	{
		return propertyNames.length;
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
			case	7: return Boolean.toString(ignoreAtMTC);
			case	8: return Double.toString(p);
			case	9: return Double.toString(p_adjusted);
			case	10: return Double.toString(p_min);
		}
		return null;
	}

	@Override
	public String getPropertyName(int propNumber)
	{
		return propertyNames[propNumber];
	}

	@Override
	public boolean isPropertyPopulationGeneCount(int propNumber)
	{
		return propNumber == 1;
	}

	@Override
	public boolean isPropertyStudyGeneCount(int propNumber)
	{
		return propNumber == 3;
	}
}
