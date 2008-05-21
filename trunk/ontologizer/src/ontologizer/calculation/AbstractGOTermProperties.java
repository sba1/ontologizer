package ontologizer.calculation;

import ontologizer.go.Term;
import ontologizer.statistics.PValue;

/**
 * 
 * This class defines the generalized interface a
 * GOTermProperty should have.
 * 
 * @author Sebastian Bauer
 */
public abstract class AbstractGOTermProperties extends PValue
{
	public Term goTerm; 
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
	 * @see isPropertyStudyGeneCount()
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
	 * @param propNumber
	 * @return boolean whether the property number represents a
	 *         study gene count or not.
	 * @see isPropertyPopulationGeneCount()
	 */
	public boolean isPropertyStudyGeneCount(int i)
	{
		return false;
	}

	/**
	 * Creates a line giving the data in the object.
	 * 
	 * Two values which are not Term dependent (but rather StudySet specific)
	 * and which are needed to make a reasonable line have to be given as
	 * parameters.
	 * 
	 * @param populationGeneCount -
	 *            The number of annotated genes in the PopulationSet
	 * @param studyGeneCount -
	 *            The number of annotated genes in the StudySet
	 * 
	 * @return The line as a String
	 * 
	 * @author grossman
	 */
	
	//TODO: Solve the passing of StudySet related data differently 
	
	public String propLineToString(int populationGeneCount, int studyGeneCount)
	{
		int i;
		int columns;
		StringBuilder locstr = new StringBuilder();
		columns = this.getNumberOfProperties();

		for (i=0;i<columns;i++)
		{
			String prop = this.getProperty(i);
			if (prop == null)
			{
				if (this.isPropertyPopulationGeneCount(i)) prop = Integer.toString(populationGeneCount);
				else if (this.isPropertyStudyGeneCount(i)) prop = Integer.toString(studyGeneCount);
			}

			locstr.append(prop);
			locstr.append("\t");
		}
		/* crop last tabulator */
		locstr.setLength(locstr.length()-1);
		
		return locstr.toString();
	}

	/**
	 * Creates a header to use in connection with propLineToString method
	 * 
	 * @return The header as a String
	 * 
	 * @author grossman
	 */
	public String propHeaderToString()
	{
		StringBuilder locstr = new StringBuilder();
		int i;
		int headercolumns;
		headercolumns = this.getNumberOfProperties();
		
		for (i=0;i<headercolumns;i++)
		{
			locstr.append(this.getPropertyName(i));
			locstr.append("\t");
		}
		/* erase last tabulator */
		locstr.setLength(locstr.length()-1);
		locstr.append("\n");
		
		return locstr.toString();
		
	}
}
