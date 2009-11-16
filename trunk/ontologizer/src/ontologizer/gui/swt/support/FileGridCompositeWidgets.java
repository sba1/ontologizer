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
	private Button labelButton;
	private Text text;
	private Button button;
	private String[] filterExts;
	private String[] filterNames;

	/**
	 * Constructor.
	 *
	 * @param parent
	 */
	public FileGridCompositeWidgets(final Composite parent)
	{
		this(parent,false);
	}

	/**
	 * Constructor.
	 *
	 * @param parent
	 * @param checkable
	 */
	public FileGridCompositeWidgets(final Composite parent, boolean checkable)
	{
		if (checkable)
		{
			labelButton = new Button(parent,checkable?SWT.CHECK:0);
			labelButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

			labelButton.addSelectionListener(new SelectionAdapter()
			{
				@Override
				public void widgetSelected(SelectionEvent e)
				{
					updateEnabledState();
				}
			});

		} else
		{
			label = new Label(parent,0);
			label.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		}


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

		updateEnabledState();
	}

	private void updateEnabledState()
	{
		if (labelButton != null)
		{
			text.setEnabled(labelButton.getSelection());
			button.setEnabled(labelButton.getSelection());
		}
	}

	/**
	 * Sets the path.
	 *
	 * @param path
	 */
	public void setPath(String path)
	{
		if (path == null) path = "";
		text.setText(path);

		if (labelButton != null)
			labelButton.setSelection(path.length() > 0);

		updateEnabledState();
	}

	/**
	 * Returns the current path.
	 *
	 * @return
	 */
	public String getPath()
	{
		if (labelButton != null && !labelButton.getSelection())
			return "";

		return text.getText();
	}

	public void setLabel(String labelString)
	{
		if (label != null) label.setText(labelString);
		else labelButton.setText(labelString);
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
		if (label != null) label.setToolTipText(string);
		else labelButton.setToolTipText(string);
		button.setToolTipText(string);
	}

	public void setEnabled(boolean state)
	{
		text.setEnabled(state);
		if (label != null) label.setEnabled(state);
		else labelButton.setEnabled(state);
		button.setEnabled(state);
	}
}
