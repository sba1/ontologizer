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
public final class IntMapper<T>
{
	/** An array of all items */
	private Object [] item;

	/** Map specific terms to the index in the allTerms array */
	private ObjectIntHashMap<T> item2Index;

	private IntMapper(Iterable<T> iterable, int size)
	{
		item = new Object[size];
		item2Index = new ObjectIntHashMap<T>(size);

		int i = 0;
		for (T t : iterable)
		{
			item[i] = t;
			item2Index.put(t, i);
			i++;
		}
	}

	/**
	 * Get the object with index i.
	 *
	 * @param i
	 * @return the object with index i.
	 */
	@SuppressWarnings("unchecked")
	public T get(int i)
	{
		return (T)item[i];
	}

	/**
	 * Return the index of the given object.
	 *
	 * @param t
	 * @return the index or -1 if the object is not indexed
	 */
	public int getIndex(T t)
	{
		return item2Index.getIfAbsent(t, -1);
	}

	/**
	 * Return the size of the mapper, i.e., the number of element it contains.
	 *
	 * @return the size.
	 */
	public int getSize()
	{
		return item.length;
	}

	/**
	 * Return the dense boolean vector of the given collection.
	 *
	 * @param collection
	 * @return the dense boolean vector with true for each element that is in the collection.
	 * @note all elements in the collection must exist in the mapper
	 */
	public boolean [] getDense(Collection<T> collection)
	{
		boolean [] d = new boolean[item.length];
		for (T c : collection)
		{
			d[getIndex(c)] = true;
		}
		return d;
	}

	/**
	 * Create a new intmap from the given collection.
	 *
	 * @param collection
	 * @return the intmap
	 */
	public static <T> IntMapper<T> create(Collection<T> collection)
	{
		return new IntMapper<T>(collection, collection.size());
	}

	/**
	 * Create a new intmap from the given iterable with the given amount of elements.
	 *
	 * @param iterable
	 * @param size
	 * @return the intmap
	 */
	public static <T> IntMapper<T> create(Iterable<T> iterable, int size)
	{
		return new IntMapper<T>(iterable, size);
	}
}
