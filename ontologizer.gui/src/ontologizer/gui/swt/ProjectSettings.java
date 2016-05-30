package ontologizer.gui.swt;

import java.util.Properties;

/**
 * Settings that belongs to the entire project.
 *
 * @author Sebastian Bauer
 */
class ProjectSettings
{
	public String ontologyFileName;
	public String annotationsFileName;
	public String mappingFileName;
	public String subset;
	public String subontology;
	public boolean isClosed;

	public Properties toProperties()
	{
		Properties prop = new Properties();
		prop.setProperty("annotationsFileName",annotationsFileName);
		prop.setProperty("ontologyFileName",ontologyFileName);
		prop.setProperty("mappingFileName",mappingFileName);
		prop.setProperty("isClosed", Boolean.toString(isClosed));
		prop.setProperty("subontology",subontology);
		prop.setProperty("subset",subset);
		return prop;
	}
};


