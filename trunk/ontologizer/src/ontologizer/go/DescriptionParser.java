package ontologizer.go;

/**
 * This is a simple parser for descriptions used in OBO.
 *
 * @author Sebastian Bauer
 */
public class DescriptionParser
{
	public interface IDescriptionPartCallback
	{
		public boolean part(String txt, TermID tid);
	}

	/**
	 * Parse the given description. For each component, the part() method
	 * of IDescriptionPartCallback is called with appropriate parameters.
	 *
	 * @param txt text to be parsed.
	 * @param cb callback to be called
	 */
	public static void parse(String txt, IDescriptionPartCallback cb)
	{

	}
}
