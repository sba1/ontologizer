package ontologizer.parser;

import java.io.File;
import java.io.IOException;

import ontologizer.types.ByteString;

public class OneOnALineValueParser extends OneOnALineParser
{

	public OneOnALineValueParser(File file) throws IOException
	{
		super(file);
	}

	public OneOnALineValueParser(String[] names)
	{
		super(names);
	}

	@Override
	protected void processLine(String line, IParserCallback callback)
	{
		if (ignoreLine(line)) return;

		String [] sfields = line.split("\\s+", 3);
		
		if (sfields.length < 2) throw new IllegalArgumentException("Number of colums is smaller than two.");
		
		for (int i = 0; i < sfields.length; i++)
			if (sfields[i] == null) sfields[i] = "";

		ByteString itemName = new ByteString(sfields[0]);
		ValuedItemAttribute itemAttribute = new ValuedItemAttribute();
		itemAttribute.setValue(Double.parseDouble(sfields[1]));
		if (sfields.length > 2) itemAttribute.description = new StringBuilder(sfields[2]).toString();
		else itemAttribute.description = "";
		callback.newEntry(itemName, itemAttribute);
	}
}
