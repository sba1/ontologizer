package ontologizer.statistics;

public abstract class AbstractSimpleTestCorrection extends AbstractTestCorrection
{
	@Override
	public PValue[] adjustPValues(IPValueCalculation pValueCalculation, final ITestCorrectionProgress progress)
	{
		IPValueCalculationProgress pProgress = null;
		if (progress != null)
		{
			progress.init(pValueCalculation.getNumberOfPValues());
			pProgress = new IPValueCalculationProgress()
			{
				@Override
				public void update(int current)
				{
					progress.update(current);
				}
			};
		}

		PValue [] pVals = adjustPValues(pValueCalculation, pProgress);
		if (progress != null)
		{
			progress.update(pValueCalculation.getNumberOfPValues());
		}
		return pVals;
	}

	protected abstract PValue[] adjustPValues(IPValueCalculation pValueCalculation, IPValueCalculationProgress progress);
}
