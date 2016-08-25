package ontologizer.calculation;

import ontologizer.statistics.ITestCorrectionProgress;

public class CalculationProgress2TestCorrectionProgress implements ITestCorrectionProgress
{
	private ICalculationProgress calculationProgress;

	public CalculationProgress2TestCorrectionProgress(ICalculationProgress calculationProgress)
	{
		this.calculationProgress = calculationProgress;
	}

	@Override
	public void init(int max)
	{
		calculationProgress.init(max);
	}

	@Override
	public void update(int current)
	{
		calculationProgress.update(current);
	}

	public static CalculationProgress2TestCorrectionProgress createUnlessNull(ICalculationProgress calculationProgress)
	{
		if (calculationProgress == null)
			return null;

		return new CalculationProgress2TestCorrectionProgress(calculationProgress);
	}
}

