package ontologizer.statistics;

import java.util.Arrays;

/**
 * 
 * @author grossman
 *
 */
public class FDRBySteffen extends AbstractTestCorrection
						   implements IResampling
{
	/** Specifies the number of resampling steps */
	private int numberOfResamplingSteps = 1000;
	
	public String getDescription()
	{
		return "The FDR controlling MTC method as proposed by Sharan/Yekutieli." +
				"Optimized implementation by Steffen";
	}

	public String getName()
	{
		return "FDR-By-Steffen";
	}

	public PValue[] adjustPValues(IPValueCalculation pvalues)
	{
		int i;

		/* Calculate raw P-values  and sort them*/
		PValue [] rawP = pvalues.calculateRawPValues();
		Arrays.sort(rawP);

		int m = rawP.length;

		/* this will hold the sorted resampled p-values*/
		double [][] pValues = new double[numberOfResamplingSteps][m];

		/* create them */
		for (int b=0; b < numberOfResamplingSteps; b++)
		{
			/* Compute raw p values of "permuted" data */
			PValue [] randomRawP = pvalues.calculateRandomPValues();
			Arrays.sort(randomRawP);
			
			assert(randomRawP.length == m);

			for (i=0;i<m;i++)
				pValues[b][i] = randomRawP[i].p;

			System.out.print("created " + (b+1) + " samples out of " + numberOfResamplingSteps + "\r");	
		}
		System.out.println();

		/* For every P-value determine the adjusted P-value */

		// we go along the sorted p-values
		int lastObservedRejections = 0;
		double lastPValue = rawP[0].p;

		/* This will hold the number of rejected tests in the samples at the current level.
		 * We assume that sampled p-values are sorted! */
		int [] lastSampleRejects = new int[numberOfResamplingSteps];
		int lastTotalSampleRejects = 0;
		// initializing
		for (int b=0; b < numberOfResamplingSteps; b++) {
			lastSampleRejects[b] = 0;
			while (pValues[b][lastSampleRejects[b]] < lastPValue) {
				lastSampleRejects[b]++;
			}
			lastTotalSampleRejects += lastSampleRejects[b];
		}
		
		double lastFDR = 0.0;
		for (int b=0; b < numberOfResamplingSteps; b++) {
			lastFDR +=
				((double)lastSampleRejects[b])/(lastSampleRejects[b] + lastObservedRejections - ((double)lastTotalSampleRejects)/numberOfResamplingSteps);
		}
		if (Double.isNaN(lastFDR)) lastFDR = 0;
		else lastFDR /= numberOfResamplingSteps; 
		
		
		i=0;
		
		while (i < m) // we increment i++ in the loop
		{
			//System.out.println("Before: m: " + m + "\ti: " + i + "\tlast P: " + lastPValue + "\tlastObservedRejections : " + lastObservedRejections);
			// we take old values until something happens
			int lc = 0;
			while (i < m && rawP[i].p <= lastPValue)
			{
				rawP[i].p_adjusted = lastFDR;
				lc++;
				i++;
			}
			
			// we need an emergency exit...
			if (i == m) break;
			
			// update p-value
			lastPValue = rawP[i].p;
			
			// update counts
			lastObservedRejections += lc;
			lastTotalSampleRejects = 0;
			for (int b=0; b < numberOfResamplingSteps; b++) {
				while (pValues[b][lastSampleRejects[b]] < lastPValue) {
					lastSampleRejects[b]++;
				}
				lastTotalSampleRejects += lastSampleRejects[b];
			}
			
			// update FDR
			lastFDR = 0.0;
			for (int b=0; b < numberOfResamplingSteps; b++) {
				lastFDR +=
					(double)lastSampleRejects[b]/(lastSampleRejects[b] + lastObservedRejections - (double)lastTotalSampleRejects/numberOfResamplingSteps);
			}
			if (Double.isNaN(lastFDR)) lastFDR = 0;
			else lastFDR /= numberOfResamplingSteps; 
/*			System.out.println("After: m: " + m
					+ "\ti: " + i
					+ "\tlast P: " + lastPValue
					+ "\tlastObservedRejections: " + lastObservedRejections
					+ "\tlastFDR: " + lastFDR);
*/		}
			
		return rawP;
	}

	public void setNumberOfResamplingSteps(int n)
	{
		numberOfResamplingSteps = n;
	}

	public int getNumberOfResamplingSteps()
	{
		return numberOfResamplingSteps;
	}

	public void resetCache()
	{
		// no cache, nothing to do here!
		
	}

	public int getSizeTolerance()
	{
		return 0;
	}

	public void setSizeTolerance(int t)
	{
		
	}
}
