package ontologizer.calculation;

/**
 * Helper class for keeping some expected values
 * in a more structured way.
 *
 * @author Sebastian Bauer
 */
class Expected
{
	/** Term of id */
	public String id;

	/** Number of genes annotated to this term in the population */
	public int pop;

	/** Number of genes annotated to this term in the study set */
	public int study;

	/** The probability */
	public double p;

	public Expected(String id, int pop, int study, double p)
	{
		this.id = id;
		this.pop = pop;
		this.study = study;
		this.p = p;
	}
}
