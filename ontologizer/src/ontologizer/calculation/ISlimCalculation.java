package ontologizer.calculation;

/**
 * An interface that is implemented by Calculations that offer a slimmed
 * down interface based native int types.
 *
 * @author Sebastian Bauer
 */
public interface ISlimCalculation extends IBaseCalculation
{
	/**
	 * Perform the calculation.
	 *
	 * @param term2Items map terms to the items
	 * @param studyIds contains indicies of items contained in the study.
	 * @param numItems number of total items (max value of both term2Item[] and studyIds)
	 * @return result array, each index corresponds to the term in term2Items.
	 */
	public double [] calculate(int [][] term2Items, int [] studyIds, int numItems);
}
