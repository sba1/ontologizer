package ontologizer.parser;

import ontologizer.ByteString;

public interface IParserCallback
{
	public void newEntry(ByteString gene, ItemAttribute attribute);
}
