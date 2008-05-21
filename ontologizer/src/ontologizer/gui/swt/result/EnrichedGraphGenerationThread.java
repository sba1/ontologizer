package ontologizer.gui.swt.result;

import java.io.BufferedInputStream;
import java.io.File;
import java.util.HashSet;

import ontologizer.calculation.EnrichedGOTermsResult;
import ontologizer.go.GOGraph;
import ontologizer.go.Term;
import ontologizer.go.TermID;
import ontologizer.gui.swt.support.IGraphGenerationFinished;

import org.eclipse.swt.widgets.Display;

/**
 * Generates the graph by executing DOT. When finished
 * the finished method of the specifed constructor argument
 * is executed in the context of the GUI thread.
 *
 * @author Sebastian Bauer
 */
public class EnrichedGraphGenerationThread extends Thread
{
	public GOGraph go;
	public Term emanatingTerm;
	public double significanceLevel;
	public HashSet<TermID> leafTerms = new HashSet<TermID>();
	public EnrichedGOTermsResult result;
	public Display display;
	public String gfxOutFilename;
	public String dotPath;

	private IGraphGenerationFinished finished;

	public EnrichedGraphGenerationThread(IGraphGenerationFinished finished)
	{
		setPriority(Thread.MIN_PRIORITY);

		this.finished = finished;
	}

	public void run()
	{
		try
		{
			final File dotTmpFile = File.createTempFile("onto", ".dot");
			final File layoutedDotTmpFile = File.createTempFile("onto", ".dot");
			final File gfxFile;

			/* Remove temporary files on exit */
			dotTmpFile.deleteOnExit();
			layoutedDotTmpFile.deleteOnExit();

			if (gfxOutFilename != null) gfxFile = new File(gfxOutFilename);
			else gfxFile = null;

			result.writeDOT( go.getGoTermContainer(), go, dotTmpFile,
					significanceLevel, true,
					emanatingTerm != null ? emanatingTerm.getID() : null,
					leafTerms);

			String [] args;
			if (gfxFile != null)
			{
				String gfxOption = "-Tpng";

				if (gfxFile.getName().endsWith(".svg"))
					gfxOption = "-Tsvg";
				if (gfxFile.getName().endsWith(".dot"))
					gfxOption = "-Tdot";
				if (gfxFile.getName().endsWith(".ps"))
					gfxOption = "-Tps2";

				args = new String[]{
						dotPath, dotTmpFile.getCanonicalPath(),
						gfxOption, "-o", gfxFile.getCanonicalPath(),
						"-Tdot", "-o", layoutedDotTmpFile.getCanonicalPath()};
			} else
			{
				args = new String[]{
						dotPath, dotTmpFile.getCanonicalPath(),
						"-Tdot", "-o", layoutedDotTmpFile.getCanonicalPath()};
			}

			Process dotProcess = Runtime.getRuntime().exec(args);

			int c;
			BufferedInputStream es = new BufferedInputStream(dotProcess.getErrorStream());
			StringBuilder errStr = new StringBuilder();
			while ((c = es.read()) != -1)
				errStr.append((char) c);

			dotProcess.waitFor();

			final boolean success = dotProcess.exitValue() == 0;

			System.err.println(errStr.toString());

			/* Issue finished message on GUI thread context */
			display.syncExec(new Runnable()
			{
				public void run()
				{
					finished.finished(success,"Dot returned a failure!",gfxFile,layoutedDotTmpFile);
				}
			});
		} catch (final Exception e)
		{
			/* Issue finished message on GUI thread context */
			display.syncExec(new Runnable(){
				public void run()
				{
					finished.finished(false,e.getLocalizedMessage(),null,null);
				}});
		}
	}
};
