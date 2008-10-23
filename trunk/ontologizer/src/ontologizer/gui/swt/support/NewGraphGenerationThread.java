package ontologizer.gui.swt.support;

import java.io.BufferedInputStream;
import java.io.File;

import org.eclipse.swt.widgets.Display;

/**
 * Generates the graph by executing DOT. When finished
 * the layoutFinished method of the specified constructor
 * argument is executed in the context of the GUI thread.
 *
 * @author Sebastian Bauer
 */
public class NewGraphGenerationThread extends Thread
{
//	public GOGraph go;
//	public Term emanatingTerm;
//	public HashSet<TermID> leafTerms = new HashSet<TermID>();
//	public AbstractGOTermsResult result;
	private Display display;
	private String dotCMDPath;
	private String gfxOutFilename;

	private IGraphGenerationSupport support;
//	private IDotNodeAttributesProvider provider;

	public NewGraphGenerationThread(Display display, String dotCMDPath, IGraphGenerationSupport support)
	{
		setPriority(Thread.MIN_PRIORITY);

		this.display = display;
		this.support = support;
		this.dotCMDPath = dotCMDPath;
	}

	public NewGraphGenerationThread(Display display, String dotCMDPath)
	{
		this(display,dotCMDPath,null);
	}

	/**
	 * Sets the support interface.
	 *
	 * @param support
	 */
	public void setSupport(IGraphGenerationSupport support)
	{
		this.support = support;
	}

	/**
	 * Sets the name of the graphical file which should be generated.
	 *
	 * @param gfxOutFilename defines the name of the graphics which should be generated.
	 * Specifying null leads to a layout of the graph.
	 */
	public void setGfxOutFilename(String gfxOutFilename)
	{
		this.gfxOutFilename = gfxOutFilename;
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

			support.writeDOT(dotTmpFile);
/*			if (result != null)
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
*/
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
						dotCMDPath, dotTmpFile.getCanonicalPath(),
						gfxOption, "-o", gfxFile.getCanonicalPath(),
						"-Tdot", "-o", layoutedDotTmpFile.getCanonicalPath()};
			} else
			{
				args = new String[]{
						dotCMDPath, dotTmpFile.getCanonicalPath(),
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
					support.layoutFinished(success,"Dot returned a failure!",gfxFile,layoutedDotTmpFile);
				}
			});
		} catch (final Exception e)
		{
			e.printStackTrace();

			/* Enable the generate graph button */
			display.syncExec(new Runnable(){
				public void run()
				{
					support.layoutFinished(false,e.toString(),null,null);
				}});
		}
	}
};
