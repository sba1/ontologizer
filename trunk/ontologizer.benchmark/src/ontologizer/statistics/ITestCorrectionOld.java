/*
 * Created on 06.07.2005
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ontologizer.statistics;

/**
 * A multiple test correction...
 *
 * TODO: Write more (theory) and adapt the API to fit
 * more procedures.
 * 
 * @author Sebastian Bauer
 *
 */
public interface ITestCorrectionOld
{
	/**
	 * Correct the given pvalue array.
	 * 
	 * @param pValues defines the array of pValues
	 * @param alpha defines the threshold to rejected null hypotheses.
	 * @return array containing the corrected pValues. The prder matches
	 *         the order of the input array.
	 * 
	 */
	public double[] correctPValues(double[] pValues, double alpha);

	/**
	 * Return a description of the test.
	 * 
	 * @return the descripton
	 */
	public String getDescription();

	/**
	 * Return the name of the test correction. 
	 * 
	 * @return the name of the test.
	 */
	public String getName();
}
