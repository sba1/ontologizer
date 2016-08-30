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

/**
 * Class implementing methods to write enrichment results.
 *
 * @author Sebastian Bauer
 */
public class EnrichedGOTermsTableWriter
{
	private static Logger logger = Logger.getLogger(EnrichedGOTermsResult.class.getName());

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

		out.write(first.propHeaderToString());

		/* Place the result into an own list, so we can sort the results */
		ArrayList<AbstractGOTermProperties> propsList = new ArrayList<AbstractGOTermProperties>();
		for (AbstractGOTermProperties props : result)
			propsList.add(props);
		Collections.sort(propsList);

		/* Write out table contents */
		for (AbstractGOTermProperties props : propsList)
		{
			out.println(props.propLineToString(result.getPopulationGeneCount(), result.getStudyGeneCount()));
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
