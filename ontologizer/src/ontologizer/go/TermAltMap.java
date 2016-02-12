package ontologizer.go;

import java.util.HashMap;
import java.util.Map;

/**
 * A simple class mapping termids to primary ids.
 *
 * @author Sebastian Bauer
 */
public class TermAltMap
{
	private Map<TermID, TermID> altMap = new HashMap<TermID, TermID>();

	public TermAltMap()
	{
	}

	/**
	 * Initialize the map with the given terms.
	 *
	 * @param terms
	 */
	private void init(Iterable<Term> terms)
	{
		for (Term t : terms)
		{
			for (TermID alt : t.getAlternatives())
			{
				altMap.put(alt, t.getID());
			}
		}
	}

	/**
	 * Get the primary term id from the given one.
	 *
	 * @param tid the term whose primary id should be determined
	 * @return the primary term id (which may be also the given term id).
	 */
	public TermID get(TermID tid)
	{
		if (altMap.containsKey(tid))
			return altMap.get(tid);
		return tid;
	}

	/**
	 * Create alternative term map mapping from alternative ids to primary ids.
	 *
	 * @param terms
	 * @return the alternative map.
	 */
	public static TermAltMap create(Iterable<Term> terms)
	{
		TermAltMap map = new TermAltMap();
		map.init(terms);
		return map;
	}
}
