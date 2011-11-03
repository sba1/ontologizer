package ontologizer.statistics;

/**
 * @author Sebastian Bauer
 */
public interface IResamplingProgress
{
	void init(int max);
	void update(int current);
}
