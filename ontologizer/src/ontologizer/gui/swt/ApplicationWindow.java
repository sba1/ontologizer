package ontologizer.gui.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Widget;

/**
 * The base class of all windows.
 * 
 * @author Sebastian Bauer
 */
public class ApplicationWindow
{
	protected Shell shell;
	protected Display display;

	private Cursor waitCursor;
	private int waitCount;

	public ApplicationWindow(Display display)
	{
		this.display = display;

		waitCursor = display.getSystemCursor(SWT.CURSOR_WAIT);
		shell = new Shell(display);
	}

	public void showWaitPointer()
	{
		if (waitCount == 0)
			shell.setCursor(waitCursor);
		waitCount++;
	}
	
	public void hideWaitPointer()
	{
		if (waitCount == 0) return;
		waitCount--;
		if (waitCount == 0)
			shell.setCursor(null);
	}
	
	/**
	 * Open the window.
	 */
	public void open()
	{
		shell.open();
	}

	/**
	 * Dispose the window.
	 */
	public void dispose()
	{
		if (!shell.isDisposed())
			shell.dispose();
	}

	protected void addSimpleSelectionAction(Widget w, final ISimpleAction act)
	{
		w.addListener(SWT.Selection, new Listener()
		{
			public void handleEvent(Event event)
			{
				act.act();
			}
		});
	}
	

	public void setVisible(boolean b)
	{
		shell.setVisible(b);
	}

}
