package ontologizer.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.StringTokenizer;

import ontologizer.types.ByteString;


/**
 * Parse FASTA file for gene name, which is taken to be the first word following
 * a ">" and delimited by white space, slash or parens. The object returns a
 * HashMap with key = gene name (first word on the line), value = description
 * (entire line minus the ">" sign).
 * 
 * @author Peter Robinson
 */

public final class FastaParser extends AbstractItemParser
{
	private File file;

	/**
	 * The first word between ">" and the subsequent DELIM character will be
	 * taken to be the gene name. Anything following the delim character will be
	 * taken to be part of the description of the gene.
	 */
	private static final String DELIM = " \t\n><()[]/\\";

	/**
	 * @param file
	 *            A file containing FASTA-formated sequences for all the genes.
	 * @throws IOException
	 *           On every io error. 
	 */
	public FastaParser(final File file)
	{
		this.file = file;
	}
	
	@Override
	public void parseSource(IParserCallback callback) throws IOException
	{
		String inputLine;
		BufferedReader is = new BufferedReader(new FileReader(file));

		while ((inputLine = is.readLine()) != null)
		{
			if (inputLine.length() > 0 && inputLine.charAt(0) == '>')
				processFASTALine(inputLine,callback);
		}
		is.close();
	}

	/**
	 * @param line
	 *            This methods expects a FASTA line such as: '>ABC1
	 *            (description)' where ABC1 is a gene or protein name. We are
	 *            interested in extracting the gene name and entering this into
	 *            the ArrayList of names for this file. substring(1) is used to
	 *            remove the '>', and the StringTokenizer is used to parse
	 *            everything up to the subsequent delimiter (white space,
	 *            parens/brackets).
	 */

	private void processFASTALine(final String line,IParserCallback callback)
	{
		StringTokenizer st = new StringTokenizer(line.substring(1), DELIM, true);
		StringBuffer sb = new StringBuffer("");
		String name = null;
		if (st.hasMoreTokens())
		{
			name = st.nextToken().trim();
		} else
		{
			/* Nothing on line */
			System.err.println("Malformed FASTA line:\n\t" + line);
//			System.err.println("Location: " + file.getName());
			System.err.println("Please correct and repeat analysis");
			System.exit(1);

		}
		while (st.hasMoreTokens())
		{
			sb.append(st.nextToken());
		}
		ByteString itemName = new ByteString(name);
		ItemAttribute itemAttribute = new ItemAttribute();
		itemAttribute.description = sb.toString();
		callback.newEntry(itemName, itemAttribute);
	}

}
