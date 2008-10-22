package ontologizer.gui.swt.result;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;

/**
 * A composite with a folder look.
 * 
 * @author Sebastian Bauer
 */
abstract class FolderComposite extends Composite
{
	private CTabFolder folder;
	private CTabItem tabItem;
	private Composite contents;
	
	public FolderComposite(Composite parent, int style)
	{
		super(parent, style);
		setLayout(new FillLayout());

		folder = new CTabFolder(this,SWT.BORDER);
		folder.setMaximizeVisible(true);
		folder.setSingle(true);
		folder.setSelectionBackground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
		tabItem = new CTabItem(folder,0);

		contents = createContents(folder);
		tabItem.setControl(contents);
		folder.setSelection(0);
	}
	
	public void setText(String text)
	{
		tabItem.setText(text);
	}
	
	protected abstract Composite createContents(Composite parent);
	
	public Composite getContents()
	{
		return contents;
	}
}

