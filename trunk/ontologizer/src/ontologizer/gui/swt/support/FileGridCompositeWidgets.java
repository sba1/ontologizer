package ontologizer.gui.swt.support;

import java.io.File;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * A custom widget which can be used to allow the user to specify a file.
 * 
 * @author Sebastian Bauer
 *
 */
public class FileGridCompositeWidgets
{
	private Label label;
	private Text text;
	private Button button;
	private String[] filterExts;
	private String[] filterNames;
	
	/**
	 * Constructor.
	 * 
	 * @param parent
	 * @param style
	 */
	public FileGridCompositeWidgets(final Composite parent)
	{
		label = new Label(parent,0);
		label.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		text = new Text(parent,SWT.BORDER);
		text.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL|GridData.GRAB_HORIZONTAL));
		button = new Button(parent,0);
		button.setText("Browse...");
		button.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e)
			{
				FileDialog fileDialog = new FileDialog(parent.getShell(),SWT.OPEN);
				if (filterExts != null) fileDialog.setFilterExtensions(filterExts);
				if (filterNames != null) fileDialog.setFilterNames(filterNames);

				if (text.getText() != null)
				{
					File f = new File(text.getText());
					fileDialog.setFileName(f.getName());
					fileDialog.setFilterPath(f.getParent());
				}

				String fileName = fileDialog.open();
				if (fileName != null)
					text.setText(fileName);
			}
		});

	}

	/**
	 * Sets the path.
	 * 
	 * @param path
	 */
	public void setPath(String path)
	{
		text.setText(path);
	}
	
	/**
	 * Returns the current path.
	 * 
	 * @return
	 */
	public String getPath()
	{
		return text.getText();
	}

	public void setLabel(String labelString)
	{
		label.setText(labelString);
	}

	public void setFilterExtensions(String [] filterExts)
	{
		this.filterExts = filterExts;
	}
	
	public void setFilterNames(String [] filterNames)
	{
		this.filterNames = filterNames;
	}

	public void setToolTipText(String string)
	{
		text.setToolTipText(string);
		label.setToolTipText(string);
		button.setToolTipText(string);
	}

	public void setEnabled(boolean state)
	{
		text.setEnabled(state);
		label.setEnabled(state);
		button.setEnabled(state);
	}
}
