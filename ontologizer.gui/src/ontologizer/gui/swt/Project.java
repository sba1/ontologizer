package ontologizer.gui.swt;

import java.io.File;

/**
 * Represents a single Ontologizer project.
 *
 * @author Sebastian Bauer
 */
public class Project
{
	public File projectDirectory;
	public ProjectSettings settings = new ProjectSettings();

	public Project(File projectDirectory)
	{
		this.projectDirectory = projectDirectory;
	}
}
