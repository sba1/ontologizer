package ontologizer.statistics;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Based upon algorithm decribed in "Statistical significance for
 * genomewide studies"
 *
 * @author Sebastian Bauer
 */
public class Storey implements ITestCorrectionOld
{
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
		public double value;
		public int index;

		public int compareTo(Entry o)
		{
			if (value < o.value) return -1;
			if (value == o.value) return 0;
			return 1;
		}
	};

	public double[] correctPValues(double[] pValues, double alpha)
	{
		double [] qValues;		/* Resulting array of qvalues */
		double [] rank;			/* Rank of the pvalues (ranks will start from 1) */
		Entry [] sortedPValues; /* sorted PValues associated with their index */
 
		int m; /* number of tests */
		int i;

		m = pValues.length;
		qValues = new double[pValues.length];
		rank = new double[pValues.length];

		/* Create sortedPValues Array */
		sortedPValues = new Entry[m];
		for (i=0;i<m;i++)
			sortedPValues[i] = new Entry();

		for (i=0;i<m;i++)
		{
			sortedPValues[i].index = i;
			sortedPValues[i].value = pValues[i];
		}
		Arrays.sort(sortedPValues);

		/* Now fill the rank array by using the additional stored
		 * index within an sortedPValue entry */
		i = 0;
		while (i<m)
		{
			int end = i + 1;

			double curValue = sortedPValues[i].value;
			double curRank =  i + 1;

			while ((end < m) && (curValue == sortedPValues[end].value))
			{
				curRank += end + 1;
				end++;
			}

			/* now average the rank */
			curRank /= end - i;
			
			for (;i<end;i++)
				rank[sortedPValues[i].index] = curRank;

			/* Note that i=end and end is at least i+1 */
		}

		/* Calculate the pi0 value */
		double pi0 = calculatePI0(sortedPValues);

		for (i=0;i<m;i++)
		{
			/* Calculate q value and overwrite p value */
			qValues[i] = pi0 * m * pValues[i] / rank[i]; 
		}

		/* TODO: qvalue <- pi0*m*p/(v*(1-(1-p)^m)) for the r
		        remark <- c(remark, "The robust version of the q-value was calculated. See Storey JD (2002) JRSS-B 64: 479-498.")
*/

		/* "Fix" the values */
		qValues[sortedPValues[m-1].index] = Math.min(qValues[sortedPValues[m-1].index],1);
		for (i=m-2;i>=0;i--)
			qValues[sortedPValues[i].index] = Math.min(qValues[sortedPValues[i].index],qValues[sortedPValues[i+1].index]);

		return qValues;
	}

	/**
	 * Calculates the pi0 value (= m0/m i.e. the proportion of true
	 * null hypothesis and all null hypothesis) 
	 * 
	 * @param sortedPValues
	 *        specifies the p values which must be sorted increasingly.
	 *
	 * @return
	 */
	private double calculatePI0(Entry[] sortedPValues)
	{
		double [] pi = new double[96];
		double lamda = 0.00;
		int i;
		int m = sortedPValues.length;

		System.out.println("------------ " + m);
		for (i=0;i<pi.length;i++)
		{
			int count = 0;
			
			/* TODO: Since the array is sorted and lamda is increasing
			 * this can be optimized */
			for (int j=0;j<m;j++)
			{
				if (sortedPValues[j].value > lamda)
					count++;
			}
			
			pi[i] = count / (m*(1 - lamda));

			System.out.println("lamda = " + lamda + " " + count + " " + pi[i]);
			lamda += 0.01;
		}

		/* We estimate 1 for now */
		return 1;
	}

	public String getDescription()
	{
		// TODO Auto-generated method stub
		return null;
	}

	public String getName()
	{
		return "Storey (QValues)";
	}

	public static void main(String[] args) throws IOException
	{
		ITestCorrectionOld corr = new Storey();
		FileReader file = new FileReader("/home/sba/R/multtest/pvalues.txt");
		BufferedReader reader = new BufferedReader(file);
		ArrayList<Double> list = new ArrayList<Double>();
		String line;
		double [] pValues;
		double [] correctedPValues;

		for (int linenum = 1; (line = reader.readLine()) != null; linenum++)
			list.add(Double.parseDouble(line));

		pValues = new double[list.size()];
		for (int i = 0;i<list.size();i++)
			pValues[i] = list.get(i);

		correctedPValues = corr.correctPValues(pValues,0.05);
		Arrays.sort(correctedPValues);
		
		for (int i = 0;i<list.size();i++)
		{
			System.out.println(correctedPValues[i]);
		}
	}
}
