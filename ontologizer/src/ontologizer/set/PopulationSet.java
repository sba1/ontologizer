/*
 * Created on 14.07.2005
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ontologizer.set;

/**
 * This class represents the whole population. It inherits from
 * StudySet
 *
 * @author Sebastian Bauer, Steffen Grossmann
 */
public class PopulationSet extends StudySet
{
	/**
	 * Constructs the population set.
	 */
	public PopulationSet()
	{
		super();
	}

	/**
	 * constructs an empty PopulationSet with the given name
	 *
	 * @param name the name of the PopulationSet to construct
	 */

	public PopulationSet(String name)
	{
		super();

		setName(name);
	}
}
