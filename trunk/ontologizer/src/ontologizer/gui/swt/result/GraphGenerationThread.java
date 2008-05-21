package ontologizer.gui.swt.result;

import java.io.BufferedInputStream;
import java.io.File;
import java.util.HashSet;

import ontologizer.GODOTWriter;
import ontologizer.IDotNodeAttributesProvider;
import ontologizer.calculation.AbstractGOTermsResult;
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
public class GraphGenerationThread extends Thread
{
	public GOGraph go;
	public Term emanatingTerm;
	public HashSet<TermID> leafTerms = new HashSet<TermID>();
	public AbstractGOTermsResult result;
	public Display display;
	public String gfxOutFilename;
	public String dotPath;

	private IGraphGenerationFinished finished;
	private IDotNodeAttributesProvider provider;

	public GraphGenerationThread(IGraphGenerationFinished finished, IDotNodeAttributesProvider provider)
	{
		setPriority(Thread.MIN_PRIORITY);

		this.finished = finished;
		this.provider = provider;
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

			if (result != null)
			{
				result.writeDOT(go, dotTmpFile,
					emanatingTerm != null ? emanatingTerm.getID() : null,
					leafTerms, provider);
			} else
			{
				GODOTWriter.writeDOT(go, dotTmpFile,
					emanatingTerm != null ? emanatingTerm.getID() : null,
					leafTerms, provider);
			}

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

			/* Create the result window. Runs within the application's context */
			display.syncExec(new Runnable()
			{
				public void run()
				{
					finished.finished(success,"Dot returned a failure!",gfxFile,layoutedDotTmpFile);
				}
			});
		} catch (final Exception e)
		{
			e.printStackTrace();
			/* Enable the generate graph button */
			display.syncExec(new Runnable(){
				public void run()
				{
					finished.finished(false,e.toString(),null,null);
				}});
		}
	}
};
