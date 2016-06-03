package ontologizer.gui.swt;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

public class LogWindow extends ApplicationWindow
{
	private static Logger logger = Logger.getLogger(LogWindow.class.getCanonicalName());

	private StyledText logStyledText;
	private FileDialog logFileDialog;

	public LogWindow(Display display)
	{
		super(display);

		logFileDialog = new FileDialog(shell,SWT.SAVE);
		logFileDialog.setOverwrite(true);
		logFileDialog.setFilterExtensions(new String[]{"*.txt","*.log"});

		shell.setText("Ontologizer - Log");
		shell.setLayout(new FillLayout());

		logStyledText = new StyledText(shell,SWT.BORDER|SWT.READ_ONLY|SWT.V_SCROLL|SWT.H_SCROLL);

		shell.setMenuBar(createMenu());

		shell.addShellListener(new ShellAdapter()
		{
			@Override
			public void shellClosed(ShellEvent e)
			{
				e.doit = false;
				shell.setVisible(false);
			}
		});
	}

	/**
	 * Create the menu of the window.
	 *
	 * @return
	 */
	private Menu createMenu()
	{
		Menu menu = new Menu(shell,SWT.BAR);
		MenuItem logItem = new MenuItem(menu, SWT.CASCADE);
		logItem.setText("Log");
		Menu logMenu = new Menu(menu);
		logItem.setMenu(logMenu);

		MenuItem saveItem = new MenuItem(logMenu, SWT.NONE);
		saveItem.setText("Save...");
		saveItem.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent arg0)
			{
				String fileName = logFileDialog.open();
				if (fileName != null)
				{
					FileWriter fw;
					try {
						fw = new FileWriter(new File(fileName));
						fw.write(logStyledText.getText());
						fw.close();
					} catch (IOException e) {
						logger.log(Level.WARNING, "Failed to write log to \"" + fileName + "\"", e);
					}
				}
			}
		});

		MenuItem editItem = new MenuItem(menu, SWT.CASCADE);
		editItem.setText("Edit");
		Menu editMenu = new Menu(menu);
		editItem.setMenu(editMenu);

		MenuItem copyItem = new MenuItem(editMenu, SWT.NONE);
		copyItem.setText("Copy");
		copyItem.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				logStyledText.copy();
			}
		});
		new MenuItem(editMenu,SWT.SEPARATOR);
		MenuItem selectAllItem = new MenuItem(editMenu, SWT.NONE);
		selectAllItem.setText("Select All");
		selectAllItem.addSelectionListener(new SelectionAdapter()
		{
			public void widgetSelected(SelectionEvent e)
			{
				logStyledText.selectAll();
			};
		});

		return menu;
	}


	public void dispose()
	{
		shell.dispose();
	}

	public void addToLog(String txt)
	{
		logStyledText.append(txt);

		int docLength = logStyledText.getCharCount();
		if (docLength > 0)
		{
			logStyledText.setCaretOffset(docLength);
			logStyledText.showSelection();
		}
	}
}
