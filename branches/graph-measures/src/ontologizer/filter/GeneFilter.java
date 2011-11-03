package ontologizer.filter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;

import ontologizer.types.ByteString;

/**
 * 
 * This class is resonsible for managing the GeneFilter stuff
 * we can be used to filter genes. Currently a filter is a
 * plain ascii file containing two columns which represent the
 * mapping.
 *  
 * @author Sebastian Bauer
 *
 */
public class GeneFilter 
{
	private HashMap<ByteString,ByteString> mapping = new HashMap<ByteString,ByteString>();
	
	public GeneFilter(Reader reader) throws IOException
	{
		BufferedReader is = new BufferedReader(reader);
		String inputLine;
		while ((inputLine = is.readLine()) != null)
		{
			/* Ignore comments */
			if (inputLine.startsWith(";")) continue;
			if (inputLine.startsWith("#")) continue;

			int firstEndPos = -1;
			int secondStartPos = -1;
			int i;
			
			/* First */
			for (i=0;i<inputLine.length();i++)
			{
				char c = inputLine.charAt(i);
				if (c == ' ' || c == '\t')
				{
					firstEndPos = i;
					break;
				}
			}
			
			/* Skip spaces or tabs */
			for (;i<inputLine.length();i++)
			{
				char c = inputLine.charAt(i);
				if (c != ' ' && c != '\t')
				{
					secondStartPos = i;
					break;
				}
			}

			if (secondStartPos != -1)
			{
				mapping.put(new ByteString(inputLine.substring(0,firstEndPos)),
							new ByteString(inputLine.substring(secondStartPos)));
			}
		}
		is.close();


	}
	
	/**
	 * Constructs the Genefilter.
	 * 
	 * @param in
	 * @throws IOException
	 */
	public GeneFilter(InputStream in) throws IOException
	{
		this(new InputStreamReader(in));
	}
	
	/**
	 * Construct the GeneFilter. 
	 * 
	 * @param filterFile
	 */
	public GeneFilter(File filterFile) throws FileNotFoundException, IOException
	{
		this(new FileReader(filterFile));
	}
	
	/**
	 * Returns the mapped name of the given gene.
	 * 
	 * @return the mapped name or null if no gene exists
	 */
	public ByteString mapGene(ByteString gene)
	{
		return mapping.get(gene);
	}
}
