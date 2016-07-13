package ontologizer.workspace;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.InvalidPropertiesFormatException;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Represents a single Ontologizer project.
 *
 * @author Sebastian Bauer
 */
public class Project
{
	/** The logger */
	private static Logger logger = Logger.getLogger(Project.class.getCanonicalName());

	/** Filename of the settings file */
	private static final String PROJECT_SETTINGS_NAME = ".project";

	/** Filename of the population file */
	private static final String POPULATION_NAME = "Population";

	public File projectDirectory;
	public ProjectSettings settings = new ProjectSettings();
	private List<ItemSet> itemSets = new ArrayList<ItemSet>();

	public Project(File projectDirectory)
	{
		this.projectDirectory = projectDirectory;

		refresh();
	}

	/**
	 * Read the given filename as item set.
	 *
	 * @return the item set.
	 */
	private ItemSet readAsItemSet(String name)
	{
		File f = new File(projectDirectory, name);
		ItemSet set = new ItemSet(this);
		set.name = name;

		try
		{
			BufferedReader is;
			String line;
			StringBuilder str = new StringBuilder();

			is = new BufferedReader(new FileReader(f));
			while ((line = is.readLine()) != null)
			{
				str.append(line);
				str.append("\n");
				set.numEntries++;
			}
			is.close();

			set.entries = str.toString();
		} catch (IOException e)
		{
		}
		return set;
	}

	private void addPopulation(String name)
	{
		ItemSet set = readAsItemSet(name);
		set.population = true;
		itemSets.add(set);
	}

	private void addStudy(String name)
	{
		ItemSet set = readAsItemSet(name);
		set.population = false;
		itemSets.add(set);
	}

	public void refresh()
	{
		itemSets.clear();

		String [] names = projectDirectory.list();

		if (names == null)
		{
			logger.warning("Listing the contents of " + projectDirectory.getPath() + " failed");
			return;
		}

		for (String name : names)
		{
			if (name.equalsIgnoreCase(POPULATION_NAME))
			{
				addPopulation(name);
				continue;
			}

			if (name.equals(PROJECT_SETTINGS_NAME))
			{
				Properties prop = new Properties();
				try
				{
					FileInputStream fis = new FileInputStream(new File(projectDirectory,PROJECT_SETTINGS_NAME));
					prop.loadFromXML(fis);
					fis.close();
					settings.fromProperties(prop);
				} catch (InvalidPropertiesFormatException e)
				{
					e.printStackTrace();
				} catch (FileNotFoundException e)
				{
					e.printStackTrace();
				} catch (IOException e)
				{
					e.printStackTrace();
				}
				continue;
			}
			addStudy(name);
		}
	}

	public Iterable<ItemSet> itemSets()
	{
		return itemSets;
	}

	/**
	 * Find a item set by the given name.
	 *
	 * @param name the name of the item set to be found.
	 *
	 * @return the found item set or null.
	 */
	public ItemSet findItemSetByName(String name)
	{
		for (ItemSet set : itemSets())
		{
			if (set.name.equals(name))
				return set;
		}
		return null;
	}

	/**
	 * Remove the given item set permanently.
	 *
	 * @param toBeRemoved
	 * @return whether it was removed or not.
	 */
	public boolean remove(ItemSet toBeRemoved)
	{
		boolean removed = false;
		for (ItemSet item : itemSets())
		{
			if (item == toBeRemoved)
			{
				File f = new File(projectDirectory,item.name);
				if (!f.delete())
					break;
				itemSets.remove(item);
				removed = true;
				break;
			}
		}
		return removed;
	}

	/**
	 * Remove the entire project from the workspace.
	 *
	 * @return whether successful or not
	 */
	public boolean remove()
	{
		for (ItemSet sets : itemSets)
			remove(sets);

		File f = new File(projectDirectory,PROJECT_SETTINGS_NAME);
		if (f.exists()) f.delete();

		if (!(projectDirectory.delete()))
			return false;

		return true;
	}

	/**
	 * Rename the project.
	 *
	 * @param newName the new name of the project
	 * @return whether the renaming was successful
	 */
	public boolean rename(String newName)
	{
		File dest = new File(projectDirectory.getParentFile(),newName);
		if (dest.exists())
			return false;

		if (!projectDirectory.renameTo(dest))
			return false;

		projectDirectory = dest;

		return true;

	}

	/**
	 * Create a population set if no such population exists.
	 *
	 * @return the newly created population or null
	 */
	public ItemSet newPopulation()
	{
		ItemSet pop = null;
		File newPopFile = new File(projectDirectory,"Population");
		if (!newPopFile.exists())
		{
			try
			{
				newPopFile.createNewFile();
				pop = readAsItemSet("Population");
				pop.population = true;
				itemSets.add(pop);
			} catch (IOException e1)
			{
			}
		}
		return pop;
	}

	/**
	 * Create a study set.
	 *
	 * @return the newly created study set
	 */
	public ItemSet newStudySet()
	{
		ItemSet study = null;
		File f;
		int i = 1;
		do
		{
			String name = "New Study Set";
			if (i!=1) name += " ("+i+")";
			f = new File(projectDirectory,name);
			i++;
		} while (f.exists());

		File newStudyFile = new File(projectDirectory,f.getName());
		try
		{
			newStudyFile.createNewFile();
			study = readAsItemSet(newStudyFile.getName());
			itemSets.add(study);
		} catch (IOException e1)
		{
		}
		return study;
	}
}
