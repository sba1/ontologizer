package ontologizer.chisquare;

import java.util.ArrayList;

/**
 * Class ChiSquare represents a chisquare value but together with some
 * background information. This means that the chisquare value carries around a
 * 'signature' which can best be understood in the usual 'urn' picture:
 * 
 * We have an urn with certain counts of balls in different colours. A certain
 * number of balls is sampled from the urn and the counts for the different
 * colours are recorded. Knowing the colour composition in the urn and in the
 * sample, the chisquare value can then be calculated.
 * 
 * Observe that the calculation of the chisquare value is not dependent on
 * whether we draw with or without replacement. This only plays a role, when we
 * want to make some statistical evaluation of the value.
 * 
 * The idea of this class is to provide a basis for the statistical evaluation
 * of chisquare values, since they are presented with enough background
 * information.
 * 
 * @author grossman
 * 
 */
public class ChiSquare
{
	// holds the total number of balls in the urn
	private int urnSize;

	// holds the total size of the sample
	private int sampleSize;

	// holds the numbers of balls of a certain colour in the urn
	private ArrayList<Integer> backgroundCounts;

	// holds the number of balls of a certain colour in the sample
	private ArrayList<Integer> sampleCounts;

	// the degrees of freedom of the chisquare value
	private int df;
	/**
	 * An empty contructor
	 */
	public ChiSquare()
	{
		urnSize = 0;
		sampleSize = 0;
		backgroundCounts = new ArrayList<Integer>();
		sampleCounts = new ArrayList<Integer>();
		df = -1;
	}

	/**
	 * Adds the counts for a 'colour' to the ChiSquare object. This means that
	 * we give a pair of background and sample counts.
	 * 
	 * @param backgroundCount
	 * @param sampleCount
	 * 
	 * @throws ChiSquareIncompatibleCountsException
	 *             if (sampleCount > backgroundCount) or if (sampleCount < 0).
	 * 
	 */
	public void addCounts(int backgroundCount, int sampleCount) throws ChiSquareIncompatibleCountsException
	{
		if (sampleCount > backgroundCount) throw new ChiSquareIncompatibleCountsException("sampleCount too high");
		if (sampleCount < 0) throw new ChiSquareIncompatibleCountsException("sampleCount is negative");
		if (sampleCount == 0) return;
		urnSize += backgroundCount;
		backgroundCounts.add(backgroundCount);
		sampleSize += sampleCount;
		sampleCounts.add(sampleCount);
		df++;
	}

	/**
	 * Calculates the chisquare value of the chisquare object 
	 * 
	 * @return the chisquare value.
	 */
	public double chiSquare()
	{
		double chisq = 0;
		
		/* The table looks as follow (sc = sampleCounts, bc = backgroundCounts):
		 * 
		 *  sc_1       | bc_1 - sc_1            bc_1
		 *  sc_2       | bc_2 - sc_2            bc_2
		 *  
		 *  sc_n       | bc_n - sc_n            bc_n
		 *  
		 *  sampleSize   urnSize - sampleSize   urnSize
		 */
		for(int i=0; i<backgroundCounts.size(); i++)
		{
			double ec = (double) sampleSize * (double) backgroundCounts.get(i)/ (double) urnSize;
			double oc = (double) sampleCounts.get(i);
			chisq += (oc - ec)*(oc - ec)/ec;
			
			ec = (double) (urnSize - sampleSize) * (double) backgroundCounts.get(i)/ (double) urnSize;
			oc = (double) (backgroundCounts.get(i) - sampleCounts.get(i));
			chisq += (oc - ec)*(oc - ec)/ec;
		}
		
		return chisq;
	}
	
	public int df()
	{
		return df;
	}
}
