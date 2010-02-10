package ontologizer.calculation;

public class ChiSquareGOTermProperties extends AbstractGOTermProperties
{
	/** chisquare value */
	public double chisquare;

	/** number of children */
	public int nchildren;

	/** degrees of freedom for chisquare */
	public int df;
	
	private static final String [] propertyNames = new String[]{
		"ID","Pop.total","Pop.term","Study.total","Study.term","nchildren","df","chisquare","p"
		};

	public int getNumberOfProperties()
	{
		return propertyNames.length;
	}

	public String getPropertyName(int propNumber)
	{
		return propertyNames[propNumber];
	}

	public String getProperty(int propNumber)
	{
		switch (propNumber)
		{
			case	0: return goTerm.getIDAsString();
			case	1: return null; /* population gene count */
			case	2: return Integer.toString(annotatedPopulationGenes);
			case	3: return null; /* study gene count */
			case 	4: return Integer.toString(annotatedStudyGenes);
			case	5: return Integer.toString(nchildren);
			case	6: return Integer.toString(df);
			case	7: return Double.toString(chisquare);
			case	8: return Double.toString(p);
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
