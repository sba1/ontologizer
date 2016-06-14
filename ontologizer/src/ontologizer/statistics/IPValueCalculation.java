package ontologizer.statistics;

/**
 *
 * This interface abstracts the p value calculation for the multiple test
 * correction procedure.
 *
 * @author Sebastian Bauer
 */
public interface IPValueCalculation
{
	/**
	 * Calculate raw (unadjusted) p values. An array of p value objects will be
	 * returned.
	 *
	 * @return the calculated raw p-values
	 */
	PValue[] calculateRawPValues();

	/**
	 * Calculate the p values using a random dataset. Note that a p value's
	 * index calculated from the same (but randomized) data point must
	 * correspond to the p value's index above. This implies that the size of
	 * the returning array has to match the size of the array returned by
	 * calculateRawPValues().
	 *
	 * @return the calculated random p-values
	 */
	PValue[] calculateRandomPValues();

	/**
	 * Gives back the size of the study set currently processed. Needed for
	 * storing of sampled p-values for different sample sizes.
	 *
	 * @return the size of the current study set
	 */
	int currentStudySetSize();
}
