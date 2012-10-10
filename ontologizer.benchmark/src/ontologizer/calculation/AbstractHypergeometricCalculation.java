package ontologizer.calculation;

import ontologizer.statistics.Hypergeometric;

/**
 * 
 * This is the abstract base class for all calculation involving
 * the hypergeometric distribution.
 * 
 * @author Sebastian Bauer
 *
 */
public abstract class AbstractHypergeometricCalculation implements ICalculation
{
	/** 
	 * An object responsible for calculation of hypergeometric 
	 * data
	 */
	protected Hypergeometric hyperg = new Hypergeometric();
}
