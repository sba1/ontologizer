package ontologizer.gui.swt.result;

/**
 * Interface for the graphical display.
 * 
 * @author Sebastian Bauer
 */
public interface IGraphAction
{
	/**
	 * Should the displayed graphics be fitted to the actual dimensions.
	 *  
	 * @param fit
	 */
	public void setScaleToFit(boolean fit);
	
	/**
	 * Zoom in.
	 */
	public void zoomIn();
	
	/**
	 * Zoom out.
	 */
	public void zoomOut();
	
	/**
	 * Reset zoom.
	 */
	public void resetZoom();
	
	/**
	 * Save the current graph to the given file.
	 * 
	 * @param file
	 */
	public void saveGraph(String file);
}
