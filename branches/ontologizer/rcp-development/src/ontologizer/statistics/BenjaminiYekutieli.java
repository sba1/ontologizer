package ontologizer.statistics;

import java.util.Arrays;

/**
 * 
 * This class implements the Benjamini-Yekutieli multiple test
 * correction. It controls (conservativly) the FDR for any kind
 * of statistics.
 *
 * @author Sebastian Bauer
 *
 */
public class BenjaminiYekutieli extends AbstractTestCorrection
{
	@Override
	public PValue[] adjustPValues(IPValueCalculation pValueCalculation)
	{
		PValue [] p = pValueCalculation.calculateRawPValues();
		PValue [] relevantP = getRelevantRawPValues(p);
		Arrays.sort(relevantP);
		int n = relevantP.length;

		double h = 0.0;
		for (int l = 1;l<=n;l++)
			h += 1.0/l;

		/* Adjust the p values according to BY. Note that all object
		 * within relevantP also are objects within p!
		 */
		for (int r=0;r<n;r++)
		{
			relevantP[r].p_adjusted = relevantP[r].p * n * h / (r + 1);
		}
		enforcePValueMonotony(relevantP);
		return p;
	}

	public String getDescription()
	{
		return "The Benjamini-Yekutieli multiple test correction";
	}

	public String getName()
	{
		return "Benjamini-Yekutieli";
	}

}
