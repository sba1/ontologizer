package ontologizer.gui.swt.support;

import java.io.File;

/**
 * A simple interface which is used by the GraphGenerationThread.
 * 
 * @author Sebastian Bauer
 */
public interface IGraphGenerationFinished
{
	void finished(boolean success, String msg, File pngFile, File dotFile);
}
