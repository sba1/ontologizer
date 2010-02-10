package ontologizer.statistics;

/**
 * Classes implementing this interface are resampling
 * based and have an additional parameter which
 * determines the number of resampling steps. Usually
 * they provide convenient default values.
 * 
 * @author Sebastian Bauer
 *
 */
public interface IResampling
{
	/**
	 * Set the number of steps.
	 * 
	 * @param n
	 */
	public void setNumberOfResamplingSteps(int n);
	
	/**
	 * Get the number of steps.
	 * 
	 * @return
	 */
	public int getNumberOfResamplingSteps();
	
	/**
	 * The size tolerance is the percentage at which the actual studyset and the
	 * resampled studysets are allowed to differ.
	 * 
	 * Set it here
	 * 
	 * @param t
	 */
	public void setSizeTolerance(int t);
	
	/**
	 * get sizeTolerance
	 * 
	 * @return
	 */
	public int getSizeTolerance();

	public void resetCache();
}
