/*
 * Created on 11.11.2007
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ontologizer.gui.swt;

import ontologizer.FileCache;
import ontologizer.gui.swt.support.FileGridCompositeWidgets;
import ontologizer.gui.swt.support.SWTUtil;
import ontologizer.worksets.WorkSet;
import ontologizer.worksets.WorkSetList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;

/**
 * This is the window displaying the worksets.
 * 
 * @author Sebastian Bauer
 */
public class WorkSetWindow extends ApplicationWindow
{
	private Tree workSetTree;
	private TreeColumn nameColumn;
	private TreeColumn pathColumn;
	private TreeColumn downloadedColumn;
	
	private Button invalidateButton;
	private Button downloadButton;
	private Button newButton;
	private Button deleteButton;
	private FileGridCompositeWidgets locationGridComposite;

	/**
	 * Constructs the work set window.
	 * 
	 * @param display
	 */
	public WorkSetWindow(Display display)
	{
		super(display);
		
		shell.setText("Ontologizer - File Sets");
		shell.setLayout(new GridLayout());

		/* Prevent the disposal of the window on a close event,
		 * but make the window invisible */
		shell.addShellListener(new ShellAdapter(){
			public void shellClosed(ShellEvent e)
			{
				e.doit = false;
				shell.setVisible(false);
			}
		});
	
		workSetTree = new Tree(shell,SWT.BORDER|SWT.FULL_SELECTION);
		GridData gd = new GridData(GridData.FILL_BOTH|GridData.GRAB_HORIZONTAL|GridData.GRAB_VERTICAL);
		gd.widthHint = 500;
		gd.heightHint = 400;
		workSetTree.setLayoutData(gd);
		
		nameColumn = new TreeColumn(workSetTree,0);
		nameColumn.setText("Name");
		
		downloadedColumn = new TreeColumn(workSetTree,0);
		downloadedColumn.setText("Last download at");

		pathColumn = new TreeColumn(workSetTree,0);
		pathColumn.setText("Path");

		workSetTree.setHeaderVisible(true);
		workSetTree.addSelectionListener(new SelectionAdapter()
		{
			public void widgetSelected(SelectionEvent e)
			{
				String location = getSelectedAddress();
				if (location != null)
				{
					locationGridComposite.setEnabled(true);
					downloadButton.setEnabled(true);
					invalidateButton.setEnabled(true);
					locationGridComposite.setPath(location);
				} else
				{
					locationGridComposite.setPath("");
					invalidateButton.setEnabled(false);
					locationGridComposite.setEnabled(false);
					downloadButton.setEnabled(false);
				}
			}
		});

		nameColumn.pack();
		pathColumn.pack();
		downloadedColumn.pack();

		Composite textComposite = new Composite(shell,0);
		textComposite.setLayout(SWTUtil.newEmptyMarginGridLayout(5));
		textComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		locationGridComposite = new FileGridCompositeWidgets(textComposite);
		locationGridComposite.setLabel("Location");
		downloadButton = new Button(textComposite,0);
		downloadButton.setText("Download");
		invalidateButton = new Button(textComposite,0);
		invalidateButton.setText("Invalidate");
		
		
//		Composite buttonComposite = new Composite(shell,0);
//		buttonComposite.setLayout(SWTUtil.newEmptyMarginGridLayout(2));

//		newButton = new Button(buttonComposite,0);
//		newButton.setText("New");
		
//		deleteButton = new Button(buttonComposite,0);
//		deleteButton.setText("Delete");
	
		shell.pack();
	}

	/**
	 * Updates the displayed work set list.
	 * 
	 * @param wsl
	 */
	public void updateWorkSetList(WorkSetList wsl)
	{
		WorkSet selectedWorkSet = getSelectedWorkSet();
		String selectedAddress = getSelectedAddress();

		workSetTree.setRedraw(false);
		workSetTree.removeAll();

		for (WorkSet ws : wsl)
		{
			String obo = ws.getOboPath();
			String association = ws.getAssociationPath();

			TreeItem ti = new TreeItem(workSetTree,0);
			ti.setData(ws);
			ti.setText(0, ws.getName());

			TreeItem oboTi = new TreeItem(ti, 0);
			oboTi.setText(0, "Definitions");
			oboTi.setText(1, FileCache.getDownloadTime(obo));
			oboTi.setText(2, obo);
			oboTi.setData(0);

			TreeItem associationTi = new TreeItem(ti,0);
			associationTi.setText("Association");
			associationTi.setText(1, FileCache.getDownloadTime(association));
			associationTi.setText(2,association);
			associationTi.setData(1);

			ti.setExpanded(true);
			
			if (selectedWorkSet != null)
			{
				if (selectedWorkSet.getName().equals(ws.getName()))
				{
					if (selectedAddress != null)
					{
						if (obo.equals(selectedAddress)) workSetTree.setSelection(oboTi);
						else if (association.equals(selectedAddress)) workSetTree.setSelection(associationTi);
					} else
						workSetTree.setSelection(ti);
				}
			}
		}

		nameColumn.pack();
		pathColumn.pack();
		downloadedColumn.pack();

		workSetTree.setRedraw(true);
	}

	/**
	 * Updates the given WorkSet (which is at the given index).
	 * 
	 * @param idx
	 * @param ws
	 */
	public void updateWorkSet(int idx, WorkSet ws)
	{
		TreeItem ti = workSetTree.getItem(idx);
		TreeItem [] children = ti.getItems();
		TreeItem oboTi = children[0];
		TreeItem associationTi = children[1];
	}

	/**
	 * Add an action executed on a click of the download button.
	 * 
	 * @param act
	 */
	public void addDownloadAction(final ISimpleAction act)
	{
		addSimpleSelectionAction(downloadButton, act);
	}

	/**
	 * Add an action that is executed on a click of the new button.
	 * 
	 * @param act
	 */
	public void addNewAction(ISimpleAction act)
	{
		addSimpleSelectionAction(newButton, act);
	}
	
	/**
	 * Add an action that is executed on a click of the invalidate button.
	 * 
	 * @param act
	 */
	public void addInvalidateAction(ISimpleAction act)
	{
		addSimpleSelectionAction(invalidateButton, act);
	}
	
	/**
	 * Add an action that is executed on a click of the delete button.
	 * 
	 * @param act
	 */
	public void addDeleteAction(ISimpleAction act)
	{
		addSimpleSelectionAction(deleteButton, act);
	}
	
	/**
	 * Return the selected work set.
	 * 
	 * @return
	 */
	public WorkSet getSelectedWorkSet()
	{
		TreeItem [] ti = workSetTree.getSelection();
		if (ti != null && ti.length > 0)
		{
			TreeItem p = ti[0];
			if (p.getParentItem() != null)
				p = p.getParentItem();

			return (WorkSet)p.getData();
		}
		return null;
	}
	
	/**
	 * Returns the selected address.
	 * 
	 * @return
	 */
	public String getSelectedAddress()
	{
		TreeItem [] ti = workSetTree.getSelection();
		if (ti != null && ti.length > 0)
		{
			WorkSet ws;

			TreeItem p = ti[0];
			if (p.getParentItem() != null)
				p = p.getParentItem();

			ws = (WorkSet)p.getData();

			Object data = ti[0].getData();
			if (data instanceof Integer)
			{
				Integer i = (Integer) data;
				if (i==0) return ws.getOboPath();
				else return ws.getAssociationPath();
			}
		}
		return null;
	}
}
