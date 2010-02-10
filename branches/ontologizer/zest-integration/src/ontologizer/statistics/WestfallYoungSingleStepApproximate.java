package ontologizer.statistics;

import java.util.Arrays;
import java.util.HashMap;

public class WestfallYoungSingleStepApproximate extends AbstractResamplingTestCorrection
{
	private HashMap<Integer,double[]> sampledMinPPerSize = new HashMap<Integer,double[]>();
	private int sizeTolerance = 10;
	
	public String getDescription()
	{
		return null;
	}

	public String getName()
	{
		return "Westfall-Young-Approximate";
	}

	/**
	 * 
	 * @author Steffen Grossmann
	 *
	 * Class models a double value entry and its index of
	 * a source array.
	 *
	 */
	private class Entry implements Comparable<Entry>
	{
		public double value;
		public int index;

		public int compareTo(Entry o)
		{
			if (value < o.value) return -1;
			if (value == o.value) return 0;
			return 1;
		}
	};

	public PValue[] adjustPValues(IPValueCalculation pvalues)
	{
		int i;

		/* Calculate raw P-values */
		PValue [] rawP = pvalues.calculateRawPValues();
		int m = rawP.length;
		
		/* Sort the raw P-values and remember their original index */
		Entry [] sortedRawPValues = new Entry[m];

		for (i=0;i<m;i++)
		{
			sortedRawPValues[i] = new Entry();
			sortedRawPValues[i].value = rawP[i].p;
			sortedRawPValues[i].index = i;
		}
		Arrays.sort(sortedRawPValues);
		
		/* this will hold the minima of the sampled p-values */
		double [] sampledMinP = new double[numberOfResamplingSteps];
		
		int studySetSize = pvalues.currentStudySetSize();
		
		// determine optimal optimal studyset size for which samples are available
		int bestStudysetSize = 0;
		double bestStudysetRatio = 0.0;

		
		for (int s : sampledMinPPerSize.keySet()) {
			double curRatio;
			if (s >= studySetSize) {
				curRatio = ((double) s)/studySetSize;
			} else {
				curRatio = ((double) studySetSize)/s;
			}
			if (bestStudysetRatio == 0.0 || curRatio < bestStudysetRatio) {
				bestStudysetRatio = curRatio;
				bestStudysetSize = s;
			}
		}
		double ratioCutoff = 1 + 0.01 * sizeTolerance;
		
		
		if (bestStudysetSize != 0 && bestStudysetRatio <= ratioCutoff) {  // use approximate samples
			System.out.println("Needing samples for study set size " + studySetSize);
			System.out.println("Using available samples made for study set size " + bestStudysetSize);
			sampledMinP = sampledMinPPerSize.get(bestStudysetSize);
		} else {        // we have to sample
			System.out.println("Sampling for study set size " + studySetSize + "\nThis may take a while...");
			
			initProgress(numberOfResamplingSteps);

			for (int b=0; b < numberOfResamplingSteps; b++) {
				/* create random sample */
				PValue [] randomRawP = pvalues.calculateRandomPValues();
				
				if (randomRawP.length > 0)
				{
					/* determine minimal p-value in sample */
					double minP = randomRawP[0].p;
					for (i=1; i < randomRawP.length; i++) {
						minP = Math.min(minP,randomRawP[i].p);
					}
					sampledMinP[b] = minP;
				}
				
				updateProgress(b);
				System.out.print("created " + b + " samples out of " + numberOfResamplingSteps + "\r");
			}
			/* sort sampled minimal p-values according to size */
			Arrays.sort(sampledMinP);
			
			sampledMinPPerSize.put(studySetSize,sampledMinP);
		}
		
		/* 
		 * this will hold the counts used for adjusting p-values
		 * Attention! Counts are for the sorted raw p-values! 
		 */
		int [] count = new int[m];

		int samplesConsidered = 0;
		int lastcount = 0;
		
		for (i=0; i < m; i++) {
			count[i] = lastcount;
			while (samplesConsidered < numberOfResamplingSteps && sampledMinP[samplesConsidered] <= sortedRawPValues[i].value) {
				count[i]++;
				samplesConsidered++;
			}
			lastcount = count[i];
		}


		/* Calculate the adjusted p values */
		for (i=0;i<m;i++)
		{
			rawP[sortedRawPValues[i].index].p_adjusted = ((double)count[i])/numberOfResamplingSteps;
		}
		return rawP;
	}

	public void resetCache()
	{
		sampledMinPPerSize = new HashMap<Integer,double[]>();
	}

	public int getSizeTolerance()
	{
		return sizeTolerance;
	}

	public void setSizeTolerance(int t)
	{
		sizeTolerance = t;
	}
}
