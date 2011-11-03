package ontologizer.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import ontologizer.types.ByteString;

/**
 * <P>
 * Parses files that have one gene or gene product name per line Assumes that
 * the first word (\w+) is the gene name. Creates a HashMap with key = gene
 * name; value = description (entire line). Therefore, users can attach some
 * description to the gene name.
 * </P>
 * <P>
 * Note that at the moment, duplicate genes are not allowed (only one entry is
 * produced for each unique gene). This behavior should be changed to optional
 * in future Ontologizer versions.
 * </P>
 * 
 * @author Peter Robinson, Sebastian Bauer
 */
public class OneOnALineParser extends AbstractItemParser
{
	private File file;
	private String [] names;

	/**
	 * Constructs the gene names by parsing a file.
	 * 
	 * @param file the file from which to read.
	 * @throws IOException emitted on an error.
	 */
	public OneOnALineParser(final File file) throws IOException
	{
		this.file = file;
	}

	/**
	 * Constructs the gene names from an array of strings.
	 * 
	 * @param names the array of strings. Spaces within any string
	 *        separate the names and their descriptions.
	 */
	public OneOnALineParser(final String [] names)
	{
		this.names = names;
	}
	
	@Override
	public void parseSource(IParserCallback callback) throws IOException
	{
		if (file != null)
		{
			BufferedReader is;
			is = new BufferedReader(new FileReader(file));
			String inputLine;
			while ((inputLine = is.readLine()) != null)
				processLine(inputLine,callback);
			is.close();

			return;
		}
		
		if (names != null)
		{
			for (int i = 0; i < names.length; i++)
				processLine(names[i],callback);
		}
	}

	protected boolean ignoreLine(String line)
	{
		return line.length() == 0 || line.startsWith(";") || line.startsWith("#");
	}

	/**
	 * Processes the given line. The line should start with a genename followed by
	 * an optional descriptions (separated by a space sign). If the line starts
	 * with ';' or '#' it is ignored. Empty lines are ignored as well.
	 * 
	 * @param line
	 *            a single line of the input gene list file.
	 */

	protected void processLine(final String line, IParserCallback callback)
	{
		if (ignoreLine(line)) return;

//		String [] fields = new String[]{ "", ""};
		String [] sfields = line.split("\\s+", 2);
		for (int i = 0; i < sfields.length; i++)
			if (sfields[i] == null) sfields[i] = "";
		
		ByteString itemName = new ByteString(sfields[0]);
		ItemAttribute itemAttribute = new ItemAttribute();
		if (sfields.length > 1)
			itemAttribute.description = new StringBuilder(sfields[1]).toString();
		else itemAttribute.description = "";
		callback.newEntry(itemName, itemAttribute);
	}
}
