package ontologizer.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * This class tries to determine the format and returns a parser object
 * corresponding to one of the above options. Users who desire to add
 * support for new formats should create a class that implements the
 * interface IGeneNameParser. Currently Fasta and a basic list of
 * gene names is supported.
 * 
 * @author Peter Robinson, Sebastian Bauer
 */

public final class ParserFactory
{
	/**
	 * Hide the constructor.
	 */
	private ParserFactory()
	{
	}

	/**
	 * Returns an instance of a gene name parser. The file type is
	 * determined automatically.
	 * 
	 * @param file the input file.
	 * @return an object which can be queried for gene names.
	 * @throws IOException on an error
	 */
	public static AbstractItemParser getNewInstance(final File file) throws IOException
	{
		String type = getFileType(file);
		if (type.equals("fasta"))
		{
			return new FastaParser(file);
		} else
		{
			if (type.equals("plain-valued"))
			{
				return new OneOnALineValueParser(file);
			}
			return new OneOnALineParser(file);
		}
	}

	/**
	 * Returns an instance of a gene name parser.
	 * 
	 * @param entries
	 * @return
	 */
	public static AbstractItemParser getNewInstance(String[] entries)
	{
		if (entries.length > 0)
		{
			String [] splitted = entries[0].split("\\s+");
			try
			{
				if (splitted.length > 1)
				{
					Double.parseDouble(splitted[1]);
					return new OneOnALineValueParser(entries);
				}
			} catch (NumberFormatException ex) {}
		}
		return new OneOnALineParser(entries);
	}

	/**
	 * Tries to determine the file type of the given file.
	 * 
	 * @param file specifies the file whose type should be identified.
	 * 
	 * @return currently eighter "plain" or "fasta"
	 * 
	 * @throws IOException when something fails.
	 */
	private static String getFileType(final File file) throws IOException
	{
		/* default: one gene name on a line */
		String type = "plain"; 
		String inputLine;
		BufferedReader is = new BufferedReader(new FileReader(file));

		int num = 0;
		while ((inputLine = is.readLine()) != null && num < 3)
		{
			if (inputLine.startsWith(">"))
			{
				type = "fasta";
				break;
			}	else
			{
				String [] splitted = inputLine.split("\\s+");
				if (splitted.length > 2)
				{
					try
					{
						Double.parseDouble(splitted[1]);
						type = "plain-valued";
					} catch (NumberFormatException ex) {}
				}
			}
			num++;
		}
		is.close();
		return type;
	}

}
