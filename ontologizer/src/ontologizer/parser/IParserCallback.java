package ontologizer.parser;

import ontologizer.types.ByteString;

public interface IParserCallback
{
	public void newEntry(ByteString gene, ItemAttribute attribute);
}
