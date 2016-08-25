package ontologizer.statistics;

import java.lang.Math;

/**
 * @author Sebastian Bauer
 */
public class Sidak extends AbstractSimpleTestCorrection
{
	/** The name of the correction method */
	private static final String NAME = "Sidak";

	public PValue[] adjustPValues(IPValueCalculation pValueCalculation, IPValueCalculationProgress progress)
	{
		PValue [] p = pValueCalculation.calculateRawPValues(progress);
		int pvalsCount = countRelevantPValues(p);

		/* Adjust the values */
		for (int i=0;i<p.length;i++)
		{
			if (!p[i].ignoreAtMTC)
				p[i].p_adjusted = 1 - Math.pow(1.0 - p[i].p, pvalsCount);
		}
		return p;
	}

	public String getDescription()
	{
		return "";
	}

	public String getName()
	{
		return NAME;
	}
}
