/*
 * Created on 16.02.2007
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ontologizer.dotwriter;

import ontologizer.go.TermID;

/**
 * Default implementation of the interface providing attributes for dot graph. 
 *
 * @author Sebastian Bauer
 */
public class AbstractDotAttributesProvider implements IDotAttributesProvider
{
	/** Returns the dot attributes for the given term. */
	public  String getDotNodeAttributes(TermID id)
	{
		return null;
	}

	/**
	 * Returns the dot attributes for the given edge.
	 *  
	 * @param id1
	 * @param id2
	 * @return
	 */
	public String getDotEdgeAttributes(TermID id1, TermID id2)
	{
		return null;
	}
}
