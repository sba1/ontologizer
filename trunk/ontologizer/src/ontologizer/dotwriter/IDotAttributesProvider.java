package ontologizer.dotwriter;

import ontologizer.go.TermID;

/**
 * An interface for providing attributes for dot graph.
 * 
 * @author sba
 */
public interface IDotAttributesProvider
{
	/** Returns the dot attributes for the given term. */
	public String getDotNodeAttributes(TermID id);
	
	/**
	 * Returns the dot attributes for the given edge.
	 *  
	 * @param id1
	 * @param id2
	 * @return
	 */
	public String getDotEdgeAttributes(TermID id1, TermID id2);
}
