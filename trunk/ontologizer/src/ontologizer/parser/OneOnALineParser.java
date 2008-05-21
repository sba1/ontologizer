package ontologizer.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

import ontologizer.ByteString;

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
public class OneOnALineParser implements IGeneNameParser
{
	/** The hash map storing genes and their descriptions. */
	private HashMap<ByteString,String> genes = new HashMap<ByteString,String>();

	/**
	 * Constructs the gene names by parsing a file.
	 *
	 * @param file the file from which to read.
	 * @throws IOException emitted on an error.
	 */
	public OneOnALineParser(final File file) throws IOException
	{
		BufferedReader is;

		is = new BufferedReader(new FileReader(file));
		String inputLine;
		while ((inputLine = is.readLine()) != null)
		{
			processLine(inputLine);
		}
		is.close();
	}

	/**
	 * Constructs the gene names from an array of strings.
	 *
	 * @param names the array of strings. Spaces within any string
	 *        separate the names and their descriptions.
	 */
	public OneOnALineParser(final String [] names)
	{
		int i;

		for (i = 0; i < names.length; i++)
			processLine(names[i]);
	}

	/**
	 * @return a HashMap with key = gene name value = description
	 */
	public HashMap<ByteString,String> getNames()
	{
		return genes;
	}

	/**
	 * Processes the given line. The line should start with a genname followed by
	 * an optional descriptions (separated by a space sign). If the line starts
	 * with ';' or '#' it is ignored. Empty lines are ignored as well.
	 *
	 * @param line
	 *            a single line of the input gene list file.
	 */

	private void processLine(final String line)
	{
		/* Ignore comments */
		if (line.length() == 0 || line.startsWith(";") || line.startsWith("#"))
			return;

		String [] fields = new String[]{ "", ""};

		String [] sfields = line.split("\\s+", 2);
		for (int i = 0; i < sfields.length; ++i)
			fields[i] = sfields[i];

		genes.put(new ByteString(fields[0]), fields[1]);
	}

}
