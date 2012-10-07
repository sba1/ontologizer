package ontologizer.gui.swt;

import ontologizer.FileCache;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

public class FileCacheWindow extends ApplicationWindow
{
	private Table table;
	private Text directoryText;
	private Button removeButton;

	private TableColumn fileNameColumn;
	private TableColumn urlColumn;
	private TableColumn downloadedColumn;

	public FileCacheWindow(Display display)
	{
		super(display);

		shell.setText("Ontologizer - File Cache");
		shell.setLayout(new GridLayout(1,false));

		/* Prevent the disposal of the window on a close event,
		 * but make the window invisible */
		shell.addShellListener(new ShellAdapter(){
			public void shellClosed(ShellEvent e)
			{
				e.doit = false;
				shell.setVisible(false);
			}
		});

		directoryText = new Text(shell,SWT.READ_ONLY);
		directoryText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		table = new Table(shell, SWT.BORDER|SWT.FULL_SELECTION);
		table.setLayoutData(new GridData(SWT.FILL,SWT.FILL,true,true,2,1));
		table.setHeaderVisible(true);

		fileNameColumn = new TableColumn(table,0);
		fileNameColumn.setText("Filename");
		fileNameColumn.pack();
		
		urlColumn = new TableColumn(table,0);
		urlColumn.setText("URL");
		urlColumn.pack();
		
		downloadedColumn = new TableColumn(table,0);
		downloadedColumn.setText("Downloaded at");
		downloadedColumn.pack();

		removeButton = new Button(shell,0);
		removeButton.setText("Remove");
		
		updateView();
		
		shell.pack();
	}
	
	@Override
	public void open() {
		
		updateView();

		shell.pack();
		super.open();
	}
	
	public void setDirectoryText(String directory)
	{
		directoryText.setText("Contents of '" + directory + "'");
	}
	
	public void updateView()
	{
		table.removeAll();
		FileCache.visitFiles(new FileCache.IFileVisitor() {
			public boolean visit(String filename, String url, String info, String downloadedAt)
			{
				TableItem item = new TableItem(table,0);
				item.setText(0,filename);
				item.setText(1,url);
				item.setText(2,downloadedAt);
				return true;
			}
		});
		fileNameColumn.pack();
		urlColumn.pack();
	}

	public void addRemoveAction(final ISimpleAction a)
	{
		removeButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				a.act();
			}
		});
	}

	/**
	 * Returns the URL of the selected entry.
	 */
	public String getSelectedURL()
	{
		TableItem [] sel = table.getSelection();
		if (sel != null && sel.length > 0)
			return sel[0].getText(1);
		return null;
	}
}
