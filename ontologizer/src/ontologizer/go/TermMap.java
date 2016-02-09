package ontologizer.go;

import java.util.HashMap;
import java.util.Map;

/**
 * A simple class mapping term ids to actual terms.
 *
 * @author Sebastian Bauer
 */
public class TermMap
{
	/** The set of GO terms */
	private Map<TermID, Term> map = new HashMap<TermID, Term>();

	/**
	 * Return the full term reference to the given term id.
	 *
	 * @param tid the term id for which to get the term.
	 * @return the term.
	 */
	public Term get(TermID tid)
	{
		return map.get(tid);
	}

	/**
	 * Create a term id map.
	 *
	 * @param terms
	 * @return
	 */
	public static TermMap create(Iterable<Term> terms)
	{
		TermMap map = new TermMap();
		for (Term t : terms)
			map.map.put(t.getID(), t);
		return map;
	}
}
