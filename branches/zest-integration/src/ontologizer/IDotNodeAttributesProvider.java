/*
 * Created on 16.02.2007
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ontologizer;

import ontologizer.go.TermID;

/**
 * An interface for providing attributes for dot graph. 
 *
 * @author Sebastian Bauer
 */
public interface IDotNodeAttributesProvider
{
	/** Returns the dot attributes for the given term. */
	public String getDotNodeAttributes(TermID id);
}
