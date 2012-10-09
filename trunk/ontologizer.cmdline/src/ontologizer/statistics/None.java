package ontologizer.statistics;

/**
 * 
 * This class implements the straightforward case, where
 * no multiple test correction is performed.
 *
 * @author Sebastian Bauer
 */
public class None extends AbstractTestCorrection
{
	public PValue[] adjustPValues(IPValueCalculation pValueCalculation)
	{
		PValue [] p = pValueCalculation.calculateRawPValues();
		for (int i=0;i<p.length;i++)
			p[i].p_adjusted = p[i].p;
		return p;
	}

	public String getDescription()
	{
		return "No correction is performed";
	}

	public String getName()
	{
		return "None";
	}
}
