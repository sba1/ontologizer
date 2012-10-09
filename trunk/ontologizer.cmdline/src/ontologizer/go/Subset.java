package ontologizer.go;

/**
 * This class represents a single subset. Terms can be accompanied by
 * subsets to restrict the ontology for certain purposes.
 * 
 * @author Sebastian Bauer
 */
public class Subset
{
	private String name;
	private String desc;
	
	public Subset(String name, String desc)
	{
		this.name = name;
	}
	
	public String getName()
	{
		return name;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		return name.equals(((Subset)obj).name);
	}
	
	@Override
	public int hashCode()
	{
		return name.hashCode();
	}

	/**
	 * Creates a subset from the given string. The first part is made 
	 * of the reference id while the second part defines a user presentable
	 * description.
	 * 
	 * @param str
	 * @return
	 */
	public static Subset createFromString(String str)
	{
		int del = str.indexOf(' ');
//		if (del < 1) throw new IllegalArgumentException("\"" + str + "\" is no valid subset definition");

		String name;
		if (del < 1)
			name = str;
		else
			name = new String(str.substring(0, del).toCharArray());
		
		String desc;
		
		try
		{
			/* TODO: skip quotation marks */
			desc = new String(str.substring(del+1, str.length()).toCharArray());	
		} catch (Exception ex)
		{
			/* Nothing bad if anything fails */
			desc = "";
		}

		return new Subset(name,desc);
	}
}
