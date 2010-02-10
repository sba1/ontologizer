package ontologizer.calculation;

public interface ICalculationProgress
{
	public void init(int max);
	public void update(int current);
}
