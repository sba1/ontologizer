package ontologizer.calculation;

import java.io.File;
import java.io.IOException;
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
public class EnrichtedGOTermWriter
{
	private static Logger logger = Logger.getLogger(EnrichedGOTermsResult.class.getName());

	/**
	 * Write the results as tab-separated file.
	 *
	 * @param file
	 */
	public static void writeTable(File file, EnrichedGOTermsResult result)
	{
		AbstractGOTermProperties first = result.iterator().next();

		try
		{
			logger.log(Level.INFO, "Writing to \"" + file.getCanonicalPath() + "\".");

			PrintWriter out = new PrintWriter(file);

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
			out.close();

			logger.log(Level.INFO, "\"" + file.getCanonicalPath() + "\"" + " successfully written.");
		} catch (IOException e)
		{
			logger.log(Level.SEVERE, "Exception occured when writing the table.", e);
		}
	}

}
