package ontologizer.calculation;

import ontologizer.ontology.TermID;
import ontologizer.statistics.PValue;

/**
 *
 * This class defines the generalized interface a
 * GOTermProperty should have.
 *
 * @author Sebastian Bauer
 * @author Steffen Grossmann
 */
public abstract class AbstractGOTermProperties extends PValue
{
	public TermID term;
	public int annotatedStudyGenes;
	public int annotatedPopulationGenes;

	/**
	 * Return the number of properties.
	 *
	 * @return the number of properties.
	 */
	abstract public int getNumberOfProperties();

	/**
	 * Return the name of the requested property (starting
	 * at 0) which can be used as a table header. Arguments
	 * may not exceed getNumberOfProperties() minus 1.
	 *
	 * @param propNumber
	 * @return the name.
	 */
	abstract public String getPropertyName(int propNumber);

	/**
	 * Return the specified property of the term represented
	 * by the object. If property is not avaiable (e.g.
	 * eighter it is not stored or argument is invalid) null
	 * will be returned.
	 *
	 * @param propNumber
	 * @return the property as a String or null, if property
	 *         is not available.
	 */
	abstract public String getProperty(int propNumber);

	/**
	 * Sometimes it is convenient to store the population gene count
	 * (e.g. within a table). However, since this information does
	 * not depend directly on a special GO term the class model
	 * doesn't provide functionality to store this information.
	 * To be able to control the column where this information is
	 * stored within a possible table this function is provided
	 * (and can be overwritten by subclass implementors).
	 *
	 * @param propNumber
	 * @return boolean whether the property number represents a
	 *         population gene count or not.
	 * @see #isPropertyStudyGeneCount(int)
	 */
	public boolean isPropertyPopulationGeneCount(int propNumber)
	{
		return false;
	}

	/**
	 * Sometimes it is convenient to store the study gene count
	 * (e.g. within a table). However, since this information does
	 * not depend directly on a special GO term the class model
	 * doesn't provide functionality to store this information.
	 * To be able to control the column where this information is
	 * stored within a possible table this function is provided
	 * (and can be overwritten by subclass implementors).
	 *
	 * @param propNumber the number of the property that should be
	 *  checked.
	 * @return boolean whether the property number represents a
	 *         study gene count or not.
	 * @see #isPropertyPopulationGeneCount(int)
	 */
	public boolean isPropertyStudyGeneCount(int propNumber)
	{
		return false;
	}
}
