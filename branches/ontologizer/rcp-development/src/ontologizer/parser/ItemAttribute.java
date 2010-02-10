package ontologizer.parser;

public class ItemAttribute
{
	public String description;

	public ItemAttribute(ItemAttribute attr)
	{
		this.description = attr.description;
	}

	public ItemAttribute()
	{
		
		this.description = "";
	}

	/**
	 * Merge this attribute with the given attribute. 
	 * 
	 * @param attr
	 * @return the new attribute
	 */
	public ItemAttribute merge(ItemAttribute attr)
	{
		ItemAttribute newAttribute = new ItemAttribute(attr);
		newAttribute.description = description + "; " + attr.description;
		return newAttribute;
	}

	/**
	 * Decide whether this attribute is preferred over the given attribute.
	 * 
	 * @param attr
	 * @return true if the given attribute is preferred.
	 */
	public boolean prefer(ItemAttribute attr)
	{
		if (attr.description == null) return false;
		if (description == null) return true;
		if (attr.description.length() > description.length())
			return true;
		return false;
	}
}

