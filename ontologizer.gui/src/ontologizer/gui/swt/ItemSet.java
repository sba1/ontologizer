package ontologizer.gui.swt;

import java.io.File;

/**
 * Describes a set of item.
 */
public class ItemSet
{
	private Project project;

	public boolean population;
	public String name;
	public String entries;
	public int numEntries;
	public int numKnownEntries = -1;

	public ItemSet(Project project)
	{
		this.project = project;
	}

	public boolean rename(String newName)
	{
		if (project == null)
			return false;

		File src = new File(project.projectDirectory,name);
		File dest = new File(project.projectDirectory,name);

		if (dest.exists())
			return false;

		if (!src.renameTo(dest))
			return false;

		if (name.equalsIgnoreCase("Population") && !newName.equalsIgnoreCase("Population"))
			population = false;
		else
		{
			if (!name.equalsIgnoreCase("Population") && newName.equalsIgnoreCase("Population"))
				population = true;
		}

		name = newName;

		return true;
	}
}
