package ontologizer.parser;

import java.util.HashMap;

import ontologizer.ByteString;

/**
 * Interface for Parsers used to parse files containing names of genes or gene
 * products.
 * 
 * @author Peter Robinson
 */

public interface IGeneNameParser
{
	/**
	 * Returns the hash map of genes where the key are the
	 * gene names and the value are the descriptions. 
	 * 
	 * @return the hash Map
	 */
	public HashMap<ByteString, String> getNames();
}
