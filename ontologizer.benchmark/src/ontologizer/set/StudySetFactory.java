package ontologizer.set;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ontologizer.parser.AbstractItemParser;
import ontologizer.parser.IParserCallback;
import ontologizer.parser.ItemAttribute;
import ontologizer.parser.ParserFactory;
import ontologizer.types.ByteString;

/**
 * Create study sets conveniently
 * 
 * @author Sebastian Bauer
 */
public class StudySetFactory
{
	private static Logger logger = Logger.getLogger(StudySetFactory.class.getCanonicalName());

	private StudySetFactory(){};
	
	/**
	 * Creates a study set from a file.
	 * 
	 * @param file
	 * @param isPopulation
	 * @return
	 * @throws IOException
	 */
	public static StudySet createFromFile(File file, boolean isPopulation) throws IOException
	{
		logger.info("Processing studyset " + file.toString());

		/* Removing suffix from filename */
		String name = file.getName();
		Pattern suffixPat = Pattern.compile("\\.[a-zA-Z0-9]+$");
		Matcher m = suffixPat.matcher(name);
		name = m.replaceAll("");

		AbstractItemParser itemParser = ParserFactory.getNewInstance(file);
		StudySet newStudySet = createFromParser(itemParser,isPopulation);
		newStudySet.setName(name);
		return newStudySet;
	}

	/**
	 * Creates a study set from an array.
	 * 
	 * @param entries
	 * @param isPopulation
	 * @return
	 * @throws IOException
	 */
	public static StudySet createFromArray(String [] entries, boolean isPopulation) throws IOException
	{
		AbstractItemParser itemParser = ParserFactory.getNewInstance(entries);
		return createFromParser(itemParser,isPopulation);
	}

	/**
	 * Creates a new study set.
	 * 
	 * @param itemParser
	 * @param isPopulation
	 * @return
	 * @throws IOException 
	 */
	public static StudySet createFromParser(AbstractItemParser itemParser, boolean isPopulation) throws IOException
	{
		final StudySet studySet;
		if (isPopulation) studySet = new PopulationSet();
		else studySet = new StudySet();

		itemParser.parse(new IParserCallback() {
			public void newEntry(ByteString gene, ItemAttribute attribute)
			{
				studySet.addGene(gene, attribute);
			}
		});
		
		return studySet;
	}
}
