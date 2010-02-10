package ontologizer.statistics;

import java.util.Arrays;

public class FDR extends AbstractTestCorrection
						   implements IResampling
{
	/** Specifies the number of resampling steps */
	private int numberOfResamplingSteps = 1000;
	
	public String getDescription()
	{
		// TODO Auto-generated method stub
		return null;
	}

	public String getName()
	{
		return "FDR";
	}

	/**
	 * 
	 * @author Sebastian Bauer
	 *
	 * Class models a double value entry and its index of
	 * a source array.
	 *
	 */
	private class Entry implements Comparable<Entry>
	{
		public String goTermID;
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

		/* Sort the raw P-values and remember their original index */
		int m = rawP.length;
		int r[] = new int[m];
		Entry [] sortedRawPValues = new Entry[m];

		for (i=0;i<m;i++)
		{
			sortedRawPValues[i] = new Entry();
			sortedRawPValues[i].value = rawP[i].p;
			sortedRawPValues[i].index = i;
		}
		Arrays.sort(sortedRawPValues);

		/* Build up r (i.e. the rank), this info is redundant but using
		 * r is more convenient. */
		for (i=0;i<m;i++)
			r[i] = sortedRawPValues[i].index;

		/* TODO: Probably this could be improved by exploiting
		 * the sorted array */
		double [][] pValues = new double[numberOfResamplingSteps][rawP.length];

		/* Now "permute" */
		for (int b=0; b < numberOfResamplingSteps; b++)
		{
			/* Compute raw p values of "permuted" data */
			PValue [] randomRawP = pvalues.calculateRandomPValues();

			assert(randomRawP.length == rawP.length);

			for (i=0;i<m;i++)
				pValues[b][i] = randomRawP[i].p;

			System.out.print("created " + (b+1) + " samples out of " + numberOfResamplingSteps + "\r");	
		}
		System.out.println("");
		
		/* For every P-value determine the adjusted P-value (but TODO: optimze!!!) */
		for (i=0;i<m;i++)
		{
			/* The p-value which is being currently adjusted */
			double p = rawP[i].p;

			/* The number of rejections observed within the dataset using p as rejection level */
			int observedRejections = 0;
			for (int j=0; j < m; j++)
			{
				if (rawP[j].p < p)
					observedRejections++;
			}
			
			/* The number of rejections of the complete permuted data set */
			int totalRejects = 0;
			for (int b=0; b < numberOfResamplingSteps; b++)
			{
				for (int j=0; j < m; j++)
				{
					if (pValues[b][j] < p)
						totalRejects++;
				}
			}
			/* How many (falsely) rejections do we expect? */
			double expectedRejections = ((double)totalRejects) / numberOfResamplingSteps;

//			System.out.println("Expecting " + expectedRejections + " rejections using the P-value of " + p);
			
			/* Calculate the fdr now using the formula:
			 * 
			 * sum_{all samples i} = v_i / (v_i + R - ev)
			 */

			double fdr = 0.0;
			for (int b=0; b < numberOfResamplingSteps; b++)
			{
				/* number of rejections for a single permutation, this is used as an estimate for V */
				int rejections = 0;

				for (int j=0; j < m; j++)
				{
					if (pValues[b][j] < p)
						rejections++;
				}

				fdr += (double)rejections/(rejections + observedRejections -  expectedRejections);
			}

			if (Double.isNaN(fdr)) fdr = 0;
			else fdr /= numberOfResamplingSteps; 

			//System.out.println("adjusted " + fdr);
			//System.out.print("fdr " + fdr + " for p-value " + p + "\r");
			System.out.print("corrected " + i + " out of " + m + " p-values (fdr " + fdr + ", p-value " + p + ")\r");

			rawP[i].p_adjusted = fdr;
		}

		Arrays.sort(rawP);
		//enforcePValueMonotony(rawP);
		
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
		// no cache, nothing to do here
		
	}

	public int getSizeTolerance()
	{
		return 0;
	}

	public void setSizeTolerance(int t)
	{
		
	}
}
