package ontologizer.go;

/**
 * This is a simple wrapper class for representing GOTermIDs.
 *
 * @author Sebastian Bauer
 *
 */
public class TermID
{
	public int id;

	/**
	 * Constructs the TermID from a plain integer value. For example, provide
	 * the integer 8150 to get the term "biological_process" which has ID
	 * GO:0008150
	 *
	 * @param id
	 */
	public TermID(int id)
	{
		this.id = id;
	}

	/**
	 * Constructs the TermID from a string value assumed in the format defined
	 * by Gene Ontology (i.e. GO:%07d)
	 *
	 * @param stringID
	 *            specifies the go term id string.
	 *
	 * @throws IllegalArgumentException
	 *             if the string could not be parsed.
	 */
	public TermID(String stringID)
	{
		if (stringID.length() != 10 || !stringID.startsWith("GO:"))
			throw new IllegalArgumentException("Failed to parse termid: \"" + stringID + "\"");
		try
		{
			id = Integer.parseInt(stringID.substring(3));
		} catch(NumberFormatException ex)
		{
			throw new IllegalArgumentException("Failed to parse termid: \"" + stringID + "\"");
		}
	}

	/**
	 * Return the string representation of this GO ID
	 */
	public String toString()
	{
		/*
		 * Luckily java has support for sprintf() functions as known from ANSI-C
		 * since 1.5
		 */
		return String.format("GO:%07d", id);
	}

	@Override
	public int hashCode()
	{
		/* We simply use the Term ID as a hash value */
		return id;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof TermID)
		{
			TermID goTermID = (TermID) obj;
			return goTermID.id == id;
		}
		return super.equals(obj);
	}
}
