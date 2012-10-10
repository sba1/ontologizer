/*
 * Created on 06.07.2005
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ontologizer.statistics;

/**
 * A multiple test correction...
 *
 * TODO: Write more (theory) and adapt the API to fit
 * more procedures.
 * 
 * @author Sebastian Bauer
 *
 */
public abstract class AbstractTestCorrection
{
	/**
	 * Perform multiple test correction on p values originating
	 * from the given p value calculation.
	 * 
	 * @param pValueCalculation an object implementing the p value
	 *        calculation.
	 *
	 * @return the adjusted p values matching the order of the
	 *         the p values of the 
	 */
	abstract public PValue[] adjustPValues(IPValueCalculation pValueCalculation);

	/**
	 * Return a description of the test.
	 * 
	 * @return the descripton
	 */
	abstract public String getDescription();

	/**
	 * Return the name of the test correction. 
	 * 
	 * @return the name of the test.
	 */
	abstract public String getName();

	/**
	 * Returns the number of pvalues that don't have
	 * the ignoreAtMTC attribute set.
	 * 
	 * @param p
	 * @return
	 */
	protected int countRelevantPValues(PValue [] p)
	{
		int pvalsCount = 0;

		/* Count number of p values which shouldn't be ignored */
		for (int i=0;i<p.length;i++)
			if (!p[i].ignoreAtMTC) pvalsCount++;

		return pvalsCount;
	}

	/**
	 * Returns an array of all raw p values that don't have
	 * the ignoreAtMTC attribute set.
	 * 
	 * @param pValueCalculation
	 * @return
	 */
	protected PValue [] getRelevantRawPValues(PValue [] p)
	{
		int i,j;
		int pvalsCount = 0;

		/* Count number of p values which shouldn't be ignored */
		for (i=0;i<p.length;i++)
			if (!p[i].ignoreAtMTC) pvalsCount++;

		/* Now put all relevant p values into a new array */
		PValue [] filteredP = new PValue[pvalsCount];
		for (i=0,j=0;i<p.length;i++)
			if (!p[i].ignoreAtMTC) filteredP[j++] = p[i]; 

		return filteredP;
	}
	
	/**
	 * Enforce monotony contrains of the p values (i.e. that
	 * adjusted p values of increasing p values is increasing
	 * as well)
	 *  
	 * @param p specfies the p values array which has to be already
	 *        in sorted order!
	 */
	public static void enforcePValueMonotony(PValue [] p)
	{
		int m = p.length;

		/* Do nothing if there are not enough pvalues */
		if (m<2) return;

		p[m-1].p_adjusted = Math.min(p[m-1].p_adjusted,1);

		for (int i=m-2;i>=0;i--)
			p[i].p_adjusted = Math.min(p[i].p_adjusted,p[i+1].p_adjusted);		
	}
}
