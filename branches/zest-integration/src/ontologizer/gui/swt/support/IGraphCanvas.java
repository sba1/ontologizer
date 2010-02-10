package ontologizer.gui.swt.support;

import java.io.File;

public interface IGraphCanvas
{
	void setLayoutedDotFile(File dotFile) throws Exception;
	void zoomReset();
	void setScaleToFit(boolean fit);
	void zoomIn();
	void zoomOut();
}
