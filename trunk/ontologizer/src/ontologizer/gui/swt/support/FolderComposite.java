package ontologizer.gui.swt.support;

import java.util.LinkedList;

import ontologizer.gui.swt.ISimpleAction;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabFolder2Adapter;
import org.eclipse.swt.custom.CTabFolderEvent;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;

/**
 * A composite with a folder look.
 * 
 * @author Sebastian Bauer
 */
public abstract class FolderComposite extends Composite
{
	private CTabFolder folder;
	private CTabItem tabItem;
	private Composite contents;
	
	private LinkedList<ISimpleAction> maximizeActionList = new LinkedList<ISimpleAction>();
	private LinkedList<ISimpleAction> minimizeActionList = new LinkedList<ISimpleAction>();
	private LinkedList<ISimpleAction> restoreActionList = new LinkedList<ISimpleAction>();
	
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
		
		folder.addCTabFolder2Listener(new CTabFolder2Adapter()
		{
			@Override
			public void maximize(CTabFolderEvent event)
			{
				folder.setMaximized(true);
				for (ISimpleAction act : maximizeActionList)
					act.act();
			}
			
			@Override
			public void restore(CTabFolderEvent event)
			{
				folder.setMaximized(false);
				for (ISimpleAction act : restoreActionList)
					act.act();
			}
			
			@Override
			public void minimize(CTabFolderEvent event)
			{
				for (ISimpleAction act : minimizeActionList)
					act.act();
			}
		});
	}
	
	/**
	 * Sets the title of the folder.
	 * 
	 * @param text
	 */
	public void setText(String text)
	{
		tabItem.setText(text);
	}
	
	protected abstract Composite createContents(Composite parent);
	
	/**
	 * Returns the composite in which the actual contents should be placed.
	 * 
	 * @return
	 */
	public Composite getContents()
	{
		return contents;
	}
	
	public void setMaximized(boolean max)
	{
		folder.setMaximized(max);
	}
	
	public void addMaximizeAction(ISimpleAction action)
	{
		folder.setMaximizeVisible(true);
		maximizeActionList.add(action);
	}
	
	public void addRestoreAction(ISimpleAction action)
	{
		restoreActionList.add(action);
	}
	
	public void addMinimizeAction(ISimpleAction action)
	{
		folder.setMinimizeVisible(true);
		minimizeActionList.add(action);
	}
	
}

