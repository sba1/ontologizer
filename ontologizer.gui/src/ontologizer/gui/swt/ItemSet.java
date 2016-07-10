package ontologizer.gui.swt;

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

}
