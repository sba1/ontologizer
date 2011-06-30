/*
 * Created on 14.07.2005
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ontologizer.set;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * 
 * @author Sebastian Bauer
 *
 * This class represents the whole population. It inherits from
 * StudySet and extents it by allowing the generation of random
 * study sets which are subsets of the population.
 */

/**
 * @author grossman
 *
 */
public class PopulationSet extends StudySet
{
	/**
	 * Constructs the population set from the given file
	 * 
	 * @param file
	 *  specifies the file (a simple list of strings or FASTA format)
	 *  where the names are extracted from.
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
//	public PopulationSet(File file) throws FileNotFoundException, IOException
//	{
//		super(file);
//	}

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
	
//	public PopulationSet(String name, String [] names)
//	{
//		super(name, names);
//	}
}
