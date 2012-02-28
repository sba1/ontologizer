package ontologizer.go;

import sonumina.collections.ReferencePool;

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

