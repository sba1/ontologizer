package ontologizer.statistics;

/**
 *
 * A class holding an p value and an object needed
 * for a class implementing IPValueCalculation.
 *
 * @author Sebastian Bauer
 *
 */
public class PValue implements Cloneable, Comparable<PValue>
{
	public double p;
	public double p_adjusted;
	public double p_min;

	/**
	 * Indicates whether the p value should be ignored my a mtc
	 * (and hence no adjusted p value will be applied)
	 */
	public boolean ignoreAtMTC;

	public int compareTo(PValue o)
	{
		if (p < o.p) return -1;
		if (p > o.p) return 1;
		return 0;
	}
}
