package ontologizer.go;

import java.util.HashMap;
import java.util.HashSet;

/**
 * A data structure to hold a pool of references.
 *
 * @author Sebastian Bauer
 *
 * @param <T>
 */
class ReferencePool<T>
{
	/**
	 * The container for all refs. Have to use a HashMap here,
	 * as it is not possible to retrieve the reference
	 * via a HashSet.
	 */
	private HashMap<T,T> referenceMap = new HashMap<T,T>();

	public T map(T toBeMapped)
	{
		T ref = referenceMap.get(toBeMapped);
		if (ref != null) return ref;
		referenceMap.put(toBeMapped, toBeMapped);
		return toBeMapped;
	}
}

/**
 * A common pool for Prefix instances.
 *
 * @author Sebastian Bauer
 */
public class PrefixPool
{
	private ReferencePool<Prefix> prefixPool = new ReferencePool<Prefix>();

	public Prefix map(Prefix ref)
	{
		return prefixPool.map(ref);
	}
}

