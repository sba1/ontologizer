package ontologizer.gui.swt;

import ontologizer.gui.swt.support.FileGridCompositeWidgets;
import ontologizer.gui.swt.support.SWTUtil;

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
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;

/**
 * This class implements the preferences window.
 *
 * @author Sebastian Bauer
 *
 */
public class PreferencesWindow extends ApplicationWindow
{
	private static String resamplingToolTipText = "Specifies the number of resampling steps which are performed for a permutation based multiple test procedure.";

	private Button okButton;
	private Text proxyText;
	private Spinner portSpinner;
	private FileGridCompositeWidgets dotFileComposite;
	private Spinner permutationSpinner;
	private Button wrapColumnCheckbox;
	private Spinner wrapColumnSpinner;

	/**
	 * Constructor.
	 *
	 * @param display
	 */
	public PreferencesWindow(Display display)
	{
		super(display);

		shell.setText("Ontologizer - Preferences");

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
		/* Dot composite */
		Composite composite = new Composite(shell,0);
		composite.setLayout(SWTUtil.newEmptyMarginGridLayout(3));
		composite.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL|GridData.HORIZONTAL_ALIGN_FILL));
		dotFileComposite = new FileGridCompositeWidgets(composite);
		dotFileComposite.setToolTipText("Specifies the path of the dot command of the GraphViz package, which is used for layouting the graph.");
		dotFileComposite.setLabel("DOT command");

		Label wrapLabel = new Label(composite,0);
		wrapLabel.setText("Wrap GO Names");
		wrapLabel.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL|GridData.HORIZONTAL_ALIGN_END|GridData.VERTICAL_ALIGN_CENTER));

		Composite wrapComposite = new Composite(composite,0);
		wrapComposite.setLayoutData(new GridData(SWT.FILL,0,true,false,2,1));
		wrapComposite.setLayout(SWTUtil.newEmptyMarginGridLayout(3));
		wrapColumnCheckbox = new Button(wrapComposite,SWT.CHECK);
		wrapColumnCheckbox.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				updateWrapEnableState();
			}
		});

		Label columnLabel = new Label(wrapComposite,0);
		columnLabel.setText("At Column");
		wrapColumnSpinner = new Spinner(wrapComposite,SWT.BORDER);
		wrapColumnSpinner.setLayoutData(new GridData(SWT.FILL,0,true,false,1,1));

		Label permutationLabel = new Label(composite,0);
		permutationLabel.setText("Resampling Steps");
		permutationLabel.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL|GridData.HORIZONTAL_ALIGN_END));
		permutationLabel.setToolTipText(resamplingToolTipText);
		permutationSpinner = new Spinner(composite,SWT.BORDER);
		permutationSpinner.setLayoutData(new GridData(SWT.FILL,0,true,false,2,1));
		permutationSpinner.setMinimum(100);
		permutationSpinner.setMaximum(5000);
		permutationSpinner.setToolTipText(resamplingToolTipText);

		/* Proxy Composite */
		Label proxyLabel = new Label(composite,0);
		proxyLabel.setText("Proxy");
		proxyLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		proxyText = new Text(composite,SWT.BORDER);
		proxyText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL|GridData.GRAB_HORIZONTAL));
		portSpinner = new Spinner(composite,SWT.BORDER);
		portSpinner.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
		portSpinner.setMaximum(65535);

		/* Button composite */
		SelectionAdapter closeWindowAdapter = new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e)
			{
				shell.setVisible(false);
			}
		};
		Composite buttonComposite = new Composite(shell,0);
		buttonComposite.setLayout(SWTUtil.newEmptyMarginGridLayout(2,true));
		buttonComposite.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END|GridData.GRAB_HORIZONTAL));
		okButton = new Button(buttonComposite,0);
		okButton.setText("Ok");
		okButton.setToolTipText("Accept the settings.");
		okButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
		okButton.addSelectionListener(closeWindowAdapter);
		Button cancelButton = new Button(buttonComposite,0);
		cancelButton.setText("Cancel");
		cancelButton.setToolTipText("Decline the settings.");
		cancelButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
		cancelButton.addSelectionListener(closeWindowAdapter);

		shell.pack();
		if (shell.getSize().x < 250) shell.setSize(250,shell.getSize().y);
	}

	/**
	 * Opens the shell window using the current settings.
	 *
	 * @return
	 */
	public void open()
	{
		if (!shell.isVisible())
		{
			/* Initialize the widgets' contents */
			dotFileComposite.setPath(GlobalPreferences.getDOTPath());
			permutationSpinner.setSelection(GlobalPreferences.getNumberOfPermutations());
			portSpinner.setSelection(GlobalPreferences.getProxyPort());

			int wc = GlobalPreferences.getWrapColumn();
			if (wc == -1)
				wrapColumnCheckbox.setSelection(false);
			else
			{
				wrapColumnCheckbox.setSelection(true);
				wrapColumnSpinner.setSelection(wc);
			}
			if (GlobalPreferences.getProxyHost() != null)
				proxyText.setText(GlobalPreferences.getProxyHost());
		}
		updateWrapEnableState();
		super.open();
	}

	/**
	 * Disposes the window.
	 */
	public void dispose()
	{
		shell.dispose();
	}

	/**
	 * Executes the given action on a accept preferences event.
	 *
	 * @param ba
	 */
	public void addAcceptPreferencesAction(final ISimpleAction ba)
	{
		okButton.addSelectionListener(new SelectionAdapter()
		{
			public void widgetSelected(org.eclipse.swt.events.SelectionEvent e)
			{
				ba.act();
			}
		});
	}

	/**
	 * Returns the dot path.
	 *
	 * @return
	 */
	public String getDOTPath()
	{
		return dotFileComposite.getPath();
	}

	/**
	 * Returns the number of permutations.
	 *
	 * @return
	 */
	public int getNumberOfPermutations()
	{
		return permutationSpinner.getSelection();
	}

	/**
	 * Returns the proxy port.
	 *
	 * @return
	 */
	public int getProxyPort()
	{
		return portSpinner.getSelection();
	}

	/**
	 * Returns the proxy host.
	 *
	 * @return
	 */
	public String getProxyHost()
	{
		return proxyText.getText();
	}

	/**
	 * Returns the wrap column or -1 if this feature should
	 * be disabled.
	 *
	 * @return
	 */
	public int getWrapColumn()
	{
		if (!wrapColumnCheckbox.getSelection())
			return -1;

		return wrapColumnSpinner.getSelection();
	}

	/**
	 * Updates the wrap enables state according to the selection
	 * state of the wrap checkbox.
	 */
	private void updateWrapEnableState()
	{
		wrapColumnSpinner.setEnabled(wrapColumnCheckbox.getSelection());
	}
}
