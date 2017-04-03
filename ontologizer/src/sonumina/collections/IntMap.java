package sonumina.collections;

import java.util.Collection;

/**
 * Simple class that holds a int mapping for objects of the same type.
 * This is useful if you want to store ints rather than references to
 * the objects.
 *
 * @author Sebastian Bauer
 *
 * @param <T>
 */
public final class IntMap<T>
{
	/** An array of all items */
	private Object [] item;

	/** Map specific terms to the index in the allTerms array */
	private ObjectIntHashMap<T> item2Index;

	private IntMap(Collection<T> collection)
	{
		item = new Object[collection.size()];
		item2Index = new ObjectIntHashMap<T>(collection.size());

		int i = 0;
		for (T t : collection)
		{
			item[i] = t;
			item2Index.put(t, i);
			i++;
		}
	}

	@SuppressWarnings("unchecked")
	public T get(int i)
	{
		return (T)item[i];
	}

	public int getIndex(T t)
	{
		return item2Index.getIfAbsent(t, -1);
	}

	/**
	 * Create a new intmap from the given collection.
	 *
	 * @param collection
	 * @return the intmap
	 */
	public static <T> IntMap<T> create(Collection<T> collection)
	{
		return new IntMap<T>(collection);
	}
}
