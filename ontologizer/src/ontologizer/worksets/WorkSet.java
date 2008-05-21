/*
 * Created on 11.11.2007
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ontologizer.worksets;

/**
 * This class holds all the information of an working set.
 * 
 * @author Sebastian Bauer
 */
public class WorkSet
{
	/* Class attributes */
	private String name;

	private String oboPath;
	private String associationPath;

	public WorkSet(String name)
	{
		this.name = name;
	}

	/**
	 * Return the workset name.
	 * 
	 * @return
	 */
	public String getName()
	{
		return name;
	}
	
	public String getOboPath()
	{
		return oboPath;
	}
	
	public String getAssociationPath()
	{
		return associationPath;
	}
	
	public void setAssociationPath(String associationPath)
	{
		this.associationPath = associationPath;
	}
	
	public void setName(String name)
	{
		this.name = name;
	}
	
	public void setOboPath(String oboPath)
	{
		this.oboPath = oboPath;
	}
	
	public WorkSet clone()
	{
		WorkSet ws = new WorkSet(name);
		ws.associationPath = associationPath;
		ws.oboPath = oboPath;
		return ws;
	}

	public void obtainDatafiles()
	{
	}
	
	public void releaseDatafiles()
	{
	}
}
