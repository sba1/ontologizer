package ontologizer.statistics;

public abstract class AbstractResamplingTestCorrection extends AbstractTestCorrection
	implements IResampling
{
	/** Specifies the number of resampling steps */
	protected int numberOfResamplingSteps = 500;

	/** Used for progress update */
	private IResamplingProgress progress;

	/**
	 * Set the number of resampling steps.
	 */
	public void setNumberOfResamplingSteps(int n)
	{
		numberOfResamplingSteps = n;
	}

	/**
	 * Returns the current number of resampling steps.
	 */
	public int getNumberOfResamplingSteps()
	{
		return numberOfResamplingSteps;
	}

	/**
	 * Sets the progress update instance used for
	 * progress notifications.
	 * 
	 * @param newProgress
	 */
	public void setProgressUpdate(IResamplingProgress newProgress)
	{
		progress = newProgress;
	}

	/**
	 * Used for sub classes.
	 * 
	 * @param max
	 */
	protected void initProgress(int max)
	{
		if (progress != null) progress.init(max);
	}
	
	protected void updateProgress(int c)
	{
		if (progress != null) progress.update(c);
	}
}
