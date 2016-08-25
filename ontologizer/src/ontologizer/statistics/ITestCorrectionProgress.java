package ontologizer.statistics;

public interface ITestCorrectionProgress
{
	void init(int max);
	void update(int current);
}
