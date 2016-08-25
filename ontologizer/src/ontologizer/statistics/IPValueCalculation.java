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
	 * @param progress the interface for updating the progress
	 * @return the calculated raw p-values
	 */
	PValue[] calculateRawPValues(IPValueCalculationProgress progress);

	/**
	 * Calculate the p values using a random dataset. Note that a p value's
	 * index calculated from the same (but randomized) data point must
	 * correspond to the p value's index above. This implies that the size of
	 * the returning array has to match the size of the array returned by
	 * calculateRawPValues().
	 *
	 * @param progress the interface for updating the progress
	 * @return the calculated random p-values
	 */
	PValue[] calculateRandomPValues(IPValueCalculationProgress progress);

	/**
	 * Gives back the size of the study set currently processed. Needed for
	 * storing of sampled p-values for different sample sizes.
	 *
	 * @return the size of the current study set
	 */
	int currentStudySetSize();

	/**
	 * @return the number of terms for which a calculation will be done.
	 */
	public int getNumberOfPValues();
}
