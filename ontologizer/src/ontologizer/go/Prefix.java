package ontologizer.go;

import ontologizer.ByteString;

/**
 * This class implements a ontology prefix which is used before the colon
 * in the specification of the term. I.e., for gene ontology this would be
 * "GO".
 *
 * @author Sebastian Bauer
 *
 */
public class Prefix
{
	private ByteString prefix;

	/**
	 * Constructs a new prefix from a string.
	 *
	 * @param newPrefix
	 */
	public Prefix(String newPrefix)
	{
		prefix = new ByteString(newPrefix);
	}

	/**
	 * Constructs a new prefix from a byte string.
	 *
	 * @param newPrefix
	 */
	public Prefix(ByteString newPrefix)
	{
		prefix = newPrefix;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof Prefix)
		{
			return equals((Prefix)obj);
		}
		if (obj instanceof ByteString)
		{
			return equals((ByteString)obj);
		}

		return super.equals(obj);
	}

	public boolean equals(ByteString obj)
	{
		return prefix.equals(obj);
	}

	public boolean equals(Prefix obj)
	{
		return prefix.equals(obj.prefix);
	}

	@Override
	public int hashCode()
	{
		return prefix.hashCode();
	}
}
