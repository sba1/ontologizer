package ontologizer.statistics;

import java.util.Iterator;

/**
 * 
 * A class providing memory efficient storage of PValue arrays. The idea is to
 * deflate the arrays by storing only the pvalues which are not marked by
 * "ignoreAtMTC". The PValue sets to be stored are assumed to all have the same
 * size which has to be set at creation. Furthermore, apart from the
 * "ignoreAtMTC" attributes and the "p" value itself, nothing else gets stored.
 * Especially, the "p_min" and "p_adjusted" entries get lost.
 * 
 * @author grossman
 * 
 */
public class PvalueSetStoreSecondVersion implements Iterable<PValue[]>
{

	/**
	 * 
	 * The iterator to conveniently hide the inflation process.
	 * 
	 * @author grossman
	 * 
	 */
	public class PValueSetStoreIterator implements Iterator<PValue[]>
	{
		private int returnedCount = 0;
		
		public boolean hasNext()
		{
			return (returnedCount < addCount);
		}

		public PValue[] next()
		{
			//return inflate_pvals(reducedIterator.next());
			int endLast = pvalSetSizes[returnedCount];
			int endThis = pvalSetSizes[returnedCount+1];
			int sizeThis = endThis - endLast;
			double [] pvalRet = new double[sizeThis];
			int [] pvalIndicesRet = new int[sizeThis];
			
			for (int i = endLast; i < endThis; i++) {
				pvalRet[i - endLast] = pvals[i];
				pvalIndicesRet[i - endLast] = pvalIndices[i];
			}
			
			deflatedPVals defPvals = new deflatedPVals(pvalRet,pvalIndicesRet);
			returnedCount++;
			
			return inflate_pvals(defPvals);
		}

		public void remove()
		{
			// TODO: don't know what to implement here!
			//reducedIterator.remove();
		}

	}

	//private ArrayList<HashMap<Integer, Double>> reducedStoredSets;

	/**
	 * A class to hold the deflated p values.
	 * Deflated p values consist of two arrays.
	 * One holds the p values, the other holds the
	 * indices of the p values in the original array. 
	 */
	
	private class deflatedPVals {
		private double [] defPVals;
		private int [] defPValsIndices;
		
		/**
		 * @param defPVals
		 * @param defPValsIndices
		 */
		public deflatedPVals(double[] defPVals, int[] defPValsIndices)
		{
			super();
			// TODO Auto-generated constructor stub
			this.defPVals = defPVals;
			this.defPValsIndices = defPValsIndices;
		}
		public double[] getDefPVals()
		{
			return defPVals;
		}
		public void setDefPVals(double[] defPVals)
		{
			this.defPVals = defPVals;
		}
		public int[] getDefPValsIndices()
		{
			return defPValsIndices;
		}
		public void setDefPValsIndices(int[] defPValsIndices)
		{
			this.defPValsIndices = defPValsIndices;
		}
		
		
	}
	
	private int setSize;
	private double [] pvals;
	private int [] pvalIndices;
	private int [] pvalSetSizes;
	private int addCount;

	/**
	 * 
	 * @param numberOfResamplingSteps
	 * @param setSize
	 */
	public PvalueSetStoreSecondVersion(int numberOfResamplingSteps, int setSize)
	{
		
		//reducedStoredSets = new ArrayList<HashMap<Integer, Double>>(numberOfResamplingSteps);
		this.setSize = setSize;
		this.pvalSetSizes = new int[numberOfResamplingSteps + 1];
		this.pvalSetSizes[0] = 0;
		this.addCount = 0;
		
		// empty initialization
		this.pvals = new double[0];
		this.pvalIndices = new int[0];
	}

	public void add(PValue[] values)
	{
		deflatedPVals addDefP = deflate_pvals(values);

		double [] addDefPValues = addDefP.getDefPVals();
		int [] addDefPIndices = addDefP.getDefPValsIndices();
		
		int oldLength = pvals.length;
		int newLength = oldLength + addDefPValues.length;
		
		// the arrays to hold the extended data
		double [] newPvals = new double[newLength];
		int [] newPvalIndices = new int[newLength];
		
		// copying the old values
		for (int i=0; i < oldLength; i++) {
			newPvals[i] = pvals[i];
			newPvalIndices[i] = pvalIndices[i];
		}
		
		// adding new values
		for (int i=oldLength; i < newLength; i++) {
			newPvals[i] = addDefPValues[i - oldLength];
			newPvalIndices[i] = addDefPIndices[i - oldLength];
		}
		
		pvals = newPvals;
		pvalIndices = newPvalIndices;
		pvalSetSizes[addCount + 1] = newLength;
		addCount++;
	}

	private deflatedPVals deflate_pvals(PValue[] values)
	{
		int nPValsInput = values.length;
		
		double [] defPValsLarge = new double[nPValsInput];
		int [] defPValsIndicesLarge = new int[nPValsInput];
		int nPValsOutput = 0;
		
		for (int i = 0; i < nPValsInput; i++)
		{
			if (!values[i].ignoreAtMTC)
			{
				defPValsLarge[nPValsOutput] = values[i].p;
				defPValsIndicesLarge[nPValsOutput] = i;
				nPValsOutput++;
			}
		}
		// counted one too much
		nPValsOutput--;
		
		double [] defPValsSmall = new double[nPValsOutput];
		int [] defPValsIndicesSmall = new int[nPValsOutput];
		for (int i=0; i < nPValsOutput; i++) {
			defPValsSmall[i] = defPValsLarge[i];
			defPValsIndicesSmall[i] = defPValsIndicesLarge[i];
		}
		
		return new deflatedPVals(defPValsSmall,defPValsIndicesSmall);
	}

	private PValue[] inflate_pvals(deflatedPVals reducedSet)
	{
		PValue[] pvals = new PValue[setSize];

		// initialize array
		for (int i = 0; i < setSize; i++)
		{
			pvals[i] = new PValue();
			pvals[i].ignoreAtMTC = true;
			pvals[i].p = 1.0;
		}

		// set old values
		double [] defPVals = reducedSet.getDefPVals();
		int [] defPValsIndices = reducedSet.getDefPValsIndices();
		for (int k=0; k < defPVals.length; k++) 
		{
			pvals[defPValsIndices[k]].ignoreAtMTC = false;
			pvals[defPValsIndices[k]].p = defPVals[k];
		}

		return pvals;
	}

	public Iterator<PValue[]> iterator()
	{
		return new PValueSetStoreIterator();
	}

}
