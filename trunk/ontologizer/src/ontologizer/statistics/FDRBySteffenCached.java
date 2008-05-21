package ontologizer.statistics;

import java.util.Arrays;
import java.util.HashMap;

/**
 * 
 * @author grossman
 *
 */
public class FDRBySteffenCached extends AbstractTestCorrection
						   implements IResampling
{
	/** Specifies the number of resampling steps */
	private int numberOfResamplingSteps = 1000;
	private HashMap<Integer,PvalueSetStore> sampledPValuesPerSize = new HashMap<Integer,PvalueSetStore>();
	
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

		int studySetSize = pvalues.currentStudySetSize();
		
		/* holds the sampled random p values for the current study set size */
		PvalueSetStore randomSampledPValues;
		
		if (sampledPValuesPerSize.containsKey(studySetSize)) {
			System.out.println("Using available samples for study set size " + studySetSize);
			randomSampledPValues = sampledPValuesPerSize.get(studySetSize);
		} else {
			System.out.println("Sampling for study set size " + studySetSize + "\nThis may take a while...");
			randomSampledPValues = new PvalueSetStore(numberOfResamplingSteps,m);
			for (int b=0; b < numberOfResamplingSteps; b++) {
				/* Compute raw p values of "permuted" data */
				PValue [] randomRawP = pvalues.calculateRandomPValues();
				Arrays.sort(randomRawP);
				
				assert(randomRawP.length == m);
				randomSampledPValues.add(randomRawP);
				
				System.out.print("created " + b + " samples out of " + numberOfResamplingSteps + "\r");				
			}
			System.out.println();
			sampledPValuesPerSize.put(studySetSize,randomSampledPValues);
		}

		/* For every P-value determine the adjusted P-value */

		// we go along the sorted p-values
		int lastObservedRejections = 0;
		double lastPValue = rawP[0].p;

		/* This will hold the number of rejected tests in the samples at the current level.
		 * We assume that sampled p-values are sorted! */
		int [] lastSampleRejects = new int[numberOfResamplingSteps];
		int lastTotalSampleRejects = 0;
		// initializing
		int b=0;
		for (PValue [] randomRawP : randomSampledPValues) {
			lastSampleRejects[b] = 0;
			while (randomRawP[lastSampleRejects[b]].p < lastPValue) {
				lastSampleRejects[b]++;
			}
			lastTotalSampleRejects += lastSampleRejects[b];
			b++;
		}
		
		double lastFDR = 0.0;
		for (b=0; b < numberOfResamplingSteps; b++) {
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
			b=0;
			for (PValue [] randomRawP : randomSampledPValues) {
				while (randomRawP[lastSampleRejects[b]].p < lastPValue) {
					lastSampleRejects[b]++;
				}
				lastTotalSampleRejects += lastSampleRejects[b];
				b++;
			}
			
			// update FDR
			lastFDR = 0.0;
			for (b=0; b < numberOfResamplingSteps; b++) {
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
		sampledPValuesPerSize = new HashMap<Integer,PvalueSetStore>();
	}

	public int getSizeTolerance()
	{
		return 0;
	}

	public void setSizeTolerance(int t)
	{
		
	}
}
