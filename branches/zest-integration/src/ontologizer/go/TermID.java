package ontologizer.go;

/**
 * This is a simple wrapper class for representing a term identifier such
 * as GO:0001004.
 * 
 * The class is immutable.
 * 
 * @author Sebastian Bauer
 * 
 */
public class TermID
{
	/** The default prefix. Only used with no prefix is specified */
	public static final Prefix DEFAULT_PREFIX = new Prefix("GO"); 

	/** Term's prefix */
	private final Prefix prefix;

	/** Its integer part */
	public final int id;
	
	/**
	 * Constructs the TermID from a plain integer value. The prefix defaults 
	 * to DEFAULT_PREFIX. For example, when DEFAULT_PREFIX is GO, provide the
	 * integer 8150 to get the term id representing the term "biological_process"
	 * that has id "GO:0008150".
	 * 
	 * @param id
	 * 
	 * @deprecated as it lacks the specification of the prefix (assumes DEFAULT_PREFIX)
	 */
	public TermID(int id)
	{
		this.id = id;
		this.prefix = DEFAULT_PREFIX;
	}

	/**
	 * Constructs the TermID.
	 * 
	 * @param prefix defines the prefix part of the identifier
	 * @param id defines the integer part of the identifier.
	 */
	public TermID(Prefix prefix, int id)
	{
		this.id = id;
		this.prefix = prefix;
	}

	/**
	 * Constructs the TermID from a string value assumed in the format defined
	 * by the OBO foundry (i.e. %s:%07d).
	 * 
	 * @param stringID
	 *            specifies the go term id string.
	 * 
	 * @throws IllegalArgumentException
	 *             if the string could not be parsed.
	 */
	public TermID(String stringID)
	{
		int colon = stringID.indexOf(':');

		/* Ensure that there is a proper prefix */
		if (colon < 1) throw new IllegalArgumentException("Failed to find a proper prefix of termid: \"" + stringID + "\"");

		/* condition, sine qua non for the integer part */
		if (stringID.length() - colon != 8) throw new IllegalArgumentException("Failed to parse the integer part of termid: \"" + stringID + "\"");
		
		prefix = new Prefix(stringID,colon);
		try
		{
			id = Integer.parseInt(stringID.substring(colon+1));
		} catch(NumberFormatException ex)
		{
			throw new IllegalArgumentException("Failed to parse the integer part of termid: \"" + stringID + "\"");
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
		return String.format("%s:%07d", prefix.toString(), id);
	}

	@Override
	public int hashCode()
	{
		/* We simply use the Term ID as a hash value neglecting the prefix */
		return id;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof TermID)
		{
			TermID goTermID = (TermID) obj;
			if (goTermID.id != id) return false;
			return goTermID.prefix.equals(prefix); 
		}
		return super.equals(obj);
	}
}
