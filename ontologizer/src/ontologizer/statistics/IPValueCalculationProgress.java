package ontologizer.statistics;

/**
 *
 * This interface is used by IPValueCalculation to monitor the
 * progress of a calculation.
 *
 * @author Sebastian Bauer
 */
public interface IPValueCalculationProgress
{
	/**
	 * Tell that the given amount of progress has been done.
	 *
	 * @param current a value between and {@link IPValueCalculation#currentStudySetSize()}
	 */
	void update(int current);
}
