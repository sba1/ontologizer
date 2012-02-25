package sonumina.collections;

import java.util.ArrayList;

/**
 * A class mapping strings to other objects.
 * 
 * @author Sebastian Bauer
 */
public class FullStringIndex<T>
{
	private ArrayList<String> stringList = new ArrayList<String>();
	private ArrayList<T> objectList = new ArrayList<T>();

	/**
	 * Associates the given string with the given object
	 * @param string
	 * @param t
	 */
	public void add(String string, T o)
	{
		stringList.add(string);
		objectList.add(o);
	}
	
	/**
	 * Returns the size of the index, i.e., the total
	 * number of strings.
	 * 
	 * @return
	 */
	public int size()
	{
		return stringList.size();
	}
}
