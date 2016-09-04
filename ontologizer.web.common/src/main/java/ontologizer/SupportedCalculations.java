package ontologizer;

public class SupportedCalculations
{
	/**
	 * List of supported calculations.
	 *
	 * Note that this list much match the list in OntologierWorkerClient. Also,
	 * the order must be identical.
	 *
	 * FIXME: There should be only one source for this.
	 */
	public final static String [] NAMES = new String[]
	{
			"Term-For-Term",
			"MGSA"
	};
}
