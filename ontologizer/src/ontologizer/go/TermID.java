package ontologizer.go;

import java.util.HashMap;

import ontologizer.types.ByteString;

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
	
	public static final ByteString COLON = new ByteString(":");

	/** Term's prefix */
	private final Prefix prefix;

	/** Its integer part */
	public final int id;
	
	/**
	 * bugfix...
	 */
	private static final HashMap<String, Integer> string2id = new HashMap<String, Integer>();
	private static int index = Integer.MAX_VALUE;
	
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
	 * by the OBO foundry.
	 * 
	 * @param stringID
	 *            specifies the term id string.
	 * 
	 * @throws IllegalArgumentException
	 *             if the string could not be parsed.
	 */
	public TermID(String stringID)
	{
		this(stringID,null);
	}

	/**
	 * Constructs the TermID from a string value assumed in the format defined
	 * by the OBO foundry.
	 * 
	 * @param stringID specifies the term id string.
	 * @param prefixPool specifies the prefix pool which is used to map the prefix
	 *          
	 * @throws IllegalArgumentException
	 *             if the string could not be parsed.
	 */
	public TermID(String stringID, PrefixPool prefixPool)
	{
		int colon = stringID.indexOf(':');

		/* Ensure that there is a proper prefix */
		if (colon < 1) throw new IllegalArgumentException("Failed to find a proper prefix of termid: \"" + stringID + "\"");

		Prefix newPrefix = new Prefix(stringID,colon); 
		if (prefixPool != null) prefix = prefixPool.map(newPrefix);
		else prefix = newPrefix;
		
		int parsedId;
		String parseIdFrom = stringID.substring(colon+1);
		try
		{
			parsedId = Integer.parseInt(parseIdFrom);
		} catch(NumberFormatException ex)
		{
			// this is a fix to keep this running for uberpheno
			parsedId = makeIdFromString(parseIdFrom);
//			throw new IllegalArgumentException("Failed to parse the integer part of termid: \"" + stringID + "\"");
		}
		id = parsedId;
	}


	public TermID(ByteString stringID, PrefixPool prefixPool)
	{
		int colon = stringID.indexOf(COLON);
		
		/* Ensure that there is a proper prefix */
		if (colon < 1) throw new IllegalArgumentException("Failed to find a proper prefix of termid: \"" + stringID.toString() + "\"");

		Prefix newPrefix = new Prefix(stringID.substring(0, colon)); 
		if (prefixPool != null) prefix = prefixPool.map(newPrefix);
		else prefix = newPrefix;

		try
		{
			int parsedId = ByteString.parseFirstInt(stringID);
			id = parsedId;
		} catch(NumberFormatException ex)
		{
			throw new IllegalArgumentException("Failed to parse the integer part of termid: \"" + stringID.toString() + "\"");
		}
	}

	private int makeIdFromString(String parseIdFrom) {
		if ( string2id.containsKey(parseIdFrom))
			return string2id.get(parseIdFrom);
		
		--index;
		string2id.put(parseIdFrom, index);
		return index;
		
	}

	/**
	 * Returns the term's prefix.
	 * 
	 * @return
	 */
	public Prefix getPrefix()
	{
		return prefix;
	}
	
	/**
	 * Return the string representation of this term ID.
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
