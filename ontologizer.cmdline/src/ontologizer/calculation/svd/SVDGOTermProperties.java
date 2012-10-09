package ontologizer.calculation.svd;

import ontologizer.calculation.AbstractGOTermProperties;

/**
 * Properties for a single term for SVD.
 * 
 * @author Sebastian Bauer
 *
 */
public class SVDGOTermProperties extends AbstractGOTermProperties
{
	/** The corresponding for this term in the matrix */
	public int rowInMatrix;
	
	/** Counts within the study */
	public int [] counts;
	
	/** PValues within the study */
	public double [] pVals;
	
	/** Weigths */
	public double [] weights;
	
	public SVDGOTermProperties(int numberOfDatasets)
	{
		counts = new int[numberOfDatasets];
		weights = new double[numberOfDatasets];
		pVals = new double[numberOfDatasets];
	}

	@Override
	public int getNumberOfProperties()
	{
		return 3 + counts.length + weights.length;
	}

	@Override
	public String getProperty(int propNumber)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getPropertyName(int propNumber)
	{
		// TODO Auto-generated method stub
		return null;
	}

}
