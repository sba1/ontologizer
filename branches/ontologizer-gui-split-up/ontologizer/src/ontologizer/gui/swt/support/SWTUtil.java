package ontologizer.gui.swt.support;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

/**
 * 
 * A utility class for SWT.
 * 
 * @author Sebastian Bauer
 *
 */
public final class SWTUtil
{
	/**
	 * Is a utility class.
	 */
	private SWTUtil()
	{
	}

	/**
	 * Creates a new GridLayout in which the margins are
	 * empty.
	 *
	 * @param columns number of columns of the layout
	 * @return a Gridlayout without any margins.
	 */
	public static GridLayout newEmptyMarginGridLayout(int columns)
	{
		GridLayout gridLayout = new GridLayout(columns,false);
		gridLayout.marginWidth = 0;
		gridLayout.marginHeight = 0;
		return gridLayout;
	}

	/**
	 * Creates a new GridLayout in which the margins are
	 * empty.
	 *
	 * @param columns number of columns of the layout
	 * @param makeColumnsEqualWidth
	 * @return a Gridlayout without any margins.
	 */
	public static GridLayout newEmptyMarginGridLayout(int columns, boolean makeColumnsEqualWidth)
	{
		GridLayout gridLayout = new GridLayout(columns,makeColumnsEqualWidth);
		gridLayout.marginWidth = 0;
		gridLayout.marginHeight = 0;
		return gridLayout;
	}

	/**
	 * Displays an exception conveniently.
	 *
	 * @param shell
	 * @param e
	 */
	public static void displayException(Shell shell, Exception e, String msg)
	{
		MessageBox mb = new MessageBox(shell,SWT.ICON_ERROR|SWT.OK);
		mb.setText("Ontologizer Error");
	
		StringBuilder build = new StringBuilder();
		if (msg != null)
		{
			build.append(msg);
			build.append("\n");
		}
		build.append("Exception \"");
		build.append(e.getClass().getCanonicalName());
		build.append("\" caught\n\n");
		build.append("Error message: ");
		build.append(e.getLocalizedMessage());
		build.append("\n\nStack Trace:\n");
		StackTraceElement [] elems = e.getStackTrace();
		for (StackTraceElement elem : elems)
		{
			build.append(elem.toString());
			build.append("\n");
		}
		mb.setMessage(build.toString());
		mb.open();
	}
	
	public static void displayException(Shell shell, Exception e)
	{
		displayException(shell, e, null);
	}

}
