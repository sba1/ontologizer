package ontologizer.calculation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

import ontologizer.ontology.Ontology;
import ontologizer.ontology.Term;

/**
 * Class implementing methods to write enrichment results.
 *
 * @author Sebastian Bauer
 */
public class EnrichedGOTermsTableWriter
{
	private static Logger logger = Logger.getLogger(EnrichedGOTermsResult.class.getName());


	/**
	 * Creates a line giving the data in the object.
	 *
	 * Two values which are not Term dependent (but rather StudySet specific)
	 * and which are needed to make a reasonable line have to be given as
	 * parameters.
	 *
	 * @param populationGeneCount -
	 *            The number of annotated genes in the PopulationSet
	 * @param studyGeneCount -
	 *            The number of annotated genes in the StudySet
	 *
	 * @return The line as a String
	 */

	//TODO: Solve the passing of StudySet related data differently

	public static String propLineToString(Ontology o, AbstractGOTermProperties p, int populationGeneCount, int studyGeneCount)
	{
		int i;
		int columns;
		StringBuilder locstr = new StringBuilder();
		columns = p.getNumberOfProperties();

		for (i=0;i<columns;i++)
		{
			String prop = p.getProperty(i);
			if (prop == null)
			{
				if (p.isPropertyPopulationGeneCount(i)) prop = Integer.toString(populationGeneCount);
				else if (p.isPropertyStudyGeneCount(i)) prop = Integer.toString(studyGeneCount);
			}

			locstr.append(prop);
			locstr.append("\t");
		}
		Term t = o.getTerm(p.term);
		if (t != null)
			locstr.append(t.getName());
		else
			locstr.append("Unknown");

		return locstr.toString();
	}

	/**
	 * Creates a header to use in connection with propLineToString method
	 *
	 * @return The header as a String
	 */
	public static String propHeaderToString(AbstractGOTermProperties p)
	{
		StringBuilder locstr = new StringBuilder();
		int i;
		int headercolumns;
		headercolumns = p.getNumberOfProperties();

		for (i=0;i<headercolumns;i++)
		{
			locstr.append(p.getPropertyName(i));
			locstr.append("\t");
		}
		/* erase last tabulator */
		locstr.append("name");
		locstr.append("\n");

		return locstr.toString();

	}

	/**
	 * Write the results in a tab-separated format to the given output stream.
	 *
	 * @param os the stream in which the data is written to.
	 * @param result the results to save
	 */
	public static void writeTable(OutputStream os, EnrichedGOTermsResult result)
	{
		PrintWriter out = new PrintWriter(os);

		if (!result.iterator().hasNext())
			return;

		AbstractGOTermProperties first = result.iterator().next();

		/* Write out the table header */

		out.write(propHeaderToString(first));

		/* Place the result into an own list, so we can sort the results */
		ArrayList<AbstractGOTermProperties> propsList = new ArrayList<AbstractGOTermProperties>();
		for (AbstractGOTermProperties props : result)
			propsList.add(props);
		Collections.sort(propsList);

		/* Write out table contents */
		for (AbstractGOTermProperties props : propsList)
		{
			out.println(propLineToString(result.go, props, result.getPopulationGeneCount(), result.getStudyGeneCount()));
		}
		out.flush();
	}

	/**
	 * Write the results as tab-separated file.
	 *
	 * @param file the filename that is used for the newly created file.
	 * @param result the results to save
	 */
	public static void writeTable(File file, EnrichedGOTermsResult result)
	{
		try
		{
			logger.log(Level.INFO, "Writing to \"" + file.getCanonicalPath() + "\".");

			FileOutputStream out = new FileOutputStream(file);
			writeTable(out, result);

			out.close();

			logger.log(Level.INFO, "\"" + file.getCanonicalPath() + "\"" + " successfully written.");
		} catch (IOException e)
		{
			logger.log(Level.SEVERE, "Exception occured when writing the table.", e);
		}
	}

}
