package ontologizer.gui.swt.support;

import java.io.File;

/**
 * A simple interface which is used by the GraphGenerationThread.
 * 
 * @author Sebastian Bauer
 */
public interface IGraphGenerationSupport
{
	/**
	 * Requests to write the graph to be layouted into the given dotFile. May be
	 * called in the context of another thread.
	 * 
	 * @param dotFile
	 */
	public void writeDOT(File dotFile);

	/**
	 * Called when the process of layouting the graph is finished.
	 * 
	 * @param success indicates 
	 * @param msg the message on failure.
	 * @param pngFile the resulting png file.
	 * @param dotFile the resulting dot file.
	 */
	public void layoutFinished(boolean success, String msg, File pngFile, File dotFile);
}
