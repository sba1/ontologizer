package ontologizer.parser;

import java.io.File;
import java.io.IOException;

import ontologizer.ByteString;

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

		String [] fields = new String[]{"", "", ""};
		String [] sfields = line.split("\\s+", 3);

		if (sfields.length < 2) throw new IllegalArgumentException("Number of colums is smaller than two.");

		for (int i = 0; i < sfields.length; i++)
			if (sfields[i] == null) sfields[i] = "";

		ByteString itemName = new ByteString(fields[0]);
		ValuedItemAttribute itemAttribute = new ValuedItemAttribute();
		itemAttribute.setValue(Double.parseDouble(fields[1]));
		itemAttribute.description = new StringBuilder(fields[2]).toString();
		callback.newEntry(itemName, itemAttribute);
	}
}
