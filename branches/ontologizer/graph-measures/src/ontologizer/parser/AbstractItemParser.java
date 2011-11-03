package ontologizer.parser;

import java.io.IOException;
import java.util.HashMap;

import ontologizer.types.ByteString;

/**
 * Abstract class for parsers used to parse files containing names of items
 * and more.
 * 
 * @author Peter Robinson, Sebastian Bauer
 */

public abstract class AbstractItemParser
{
	private HashMap<ByteString,ItemAttribute> item2Attributes = new HashMap<ByteString, ItemAttribute>();

	protected abstract void parseSource(IParserCallback callback) throws IOException;

	public void parse(final IParserCallback callback) throws IOException
	{
		parseSource(new IParserCallback()
		{
			public void newEntry(ByteString itemName, ItemAttribute itemAttribute)
			{
				item2Attributes.put(itemName, itemAttribute);
				if (callback != null)
					callback.newEntry(itemName, itemAttribute);
			}
		});
	
	}

	public void parse() throws IOException
	{
		parse(null);
	}
	
	public HashMap<ByteString, ItemAttribute> getItem2Attributes()
	{
		return item2Attributes;
	}
}
