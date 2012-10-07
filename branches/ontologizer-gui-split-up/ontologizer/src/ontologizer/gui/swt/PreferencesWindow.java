package ontologizer.gui.swt;

import ontologizer.GlobalPreferences;
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
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
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
	
	private Spinner alphaSpinner;
	private Button alphaAutoButton;
	private Spinner upperAlphaSpinner;
	private Spinner betaSpinner;
	private Button betaAutoButton;
	private Spinner upperBetaSpinner;
	private Spinner expectedNumberSpinner;
	private Button expectedNumberAutoButton;
	private Spinner mcmcStepsSpinner;
	private final static int ALPHA_BETA_DIGITS = 2;

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
		
		
		TabFolder tabFolder = new TabFolder (shell, SWT.NONE);
		tabFolder.setLayoutData(new GridData(GridData.FILL_BOTH));
		TabItem generalItem = new TabItem(tabFolder,SWT.NONE);
		generalItem.setText("General");
		TabItem b2gItem = new TabItem(tabFolder,SWT.NONE);
		b2gItem.setText("MGSA");
		
		/* Dot composite */
		Composite composite = new Composite(tabFolder,0);
		composite.setLayout(SWTUtil.newEmptyMarginGridLayout(3));

		dotFileComposite = new FileGridCompositeWidgets(composite);
		dotFileComposite.setToolTipText("Specifies the path of the dot command of the GraphViz package, which is used for layouting the graph.");
		dotFileComposite.setLabel("DOT command");

		Label wrapLabel = new Label(composite,0);
		wrapLabel.setText("Wrap GO names");
		wrapLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END|GridData.VERTICAL_ALIGN_CENTER));
		
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
		permutationLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
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

		generalItem.setControl(composite);

		if (true)//CalculationRegistry.experimentalActivated())
		{
			Composite b2gComp = new Composite(tabFolder, 0);
			b2gItem.setControl(b2gComp);
			b2gComp.setLayout(SWTUtil.newEmptyMarginGridLayout(3));
			composite.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL|GridData.HORIZONTAL_ALIGN_FILL));

			Label alphaLabel = new Label(b2gComp,0);
			alphaLabel.setText("Alpha (in percent)");
			alphaLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
			alphaSpinner = new Spinner(b2gComp,SWT.BORDER);
			alphaSpinner.setLayoutData(new GridData(SWT.FILL,0,true,false,1,1));
			alphaSpinner.setMinimum(1);
			alphaSpinner.setMaximum(99*(int)Math.pow(10, ALPHA_BETA_DIGITS));
			alphaSpinner.setSelection(10*(int)Math.pow(10, ALPHA_BETA_DIGITS));
			alphaSpinner.setDigits(ALPHA_BETA_DIGITS);
			alphaSpinner.setEnabled(false);
			alphaAutoButton = new Button(b2gComp,SWT.CHECK);
			alphaAutoButton.setText("Auto");
			alphaAutoButton.setSelection(true);
			alphaAutoButton.addSelectionListener(new SelectionAdapter()
			{
				@Override
				public void widgetSelected(SelectionEvent e)
				{
					alphaSpinner.setEnabled(!alphaAutoButton.getSelection());
				}
			});
			
			Label upperAlphaLabel = new Label(b2gComp,0);
			upperAlphaLabel.setText("Upper bound for alpha");
			upperAlphaLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
			upperAlphaSpinner = new Spinner(b2gComp,SWT.BORDER);
			upperAlphaSpinner.setLayoutData(new GridData(SWT.FILL,0,true,false,1,1));
			upperAlphaSpinner.setMinimum(1);
			upperAlphaSpinner.setMaximum(100*(int)Math.pow(10, ALPHA_BETA_DIGITS));
			upperAlphaSpinner.setSelection(100*(int)Math.pow(10, ALPHA_BETA_DIGITS));
			upperAlphaSpinner.setDigits(ALPHA_BETA_DIGITS);
			upperAlphaSpinner.setEnabled(true);
			new Label(b2gComp,0);

			Label betaLabel = new Label(b2gComp,0);
			betaLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
			betaLabel.setText("Beta (in percent)");
			betaSpinner = new Spinner(b2gComp,SWT.BORDER);
			betaSpinner.setLayoutData(new GridData(SWT.FILL,0,true,false,1,1));
			betaSpinner.setMinimum(1);
			betaSpinner.setMaximum(99*(int)Math.pow(10, ALPHA_BETA_DIGITS));
			betaSpinner.setSelection(25*(int)Math.pow(10, ALPHA_BETA_DIGITS));
			betaSpinner.setDigits(ALPHA_BETA_DIGITS);
			betaSpinner.setEnabled(false);
			betaAutoButton = new Button(b2gComp,SWT.CHECK);
			betaAutoButton.setText("Auto");
			betaAutoButton.setSelection(true);
			betaAutoButton.addSelectionListener(new SelectionAdapter()
			{
				@Override
				public void widgetSelected(SelectionEvent e)
				{
					betaSpinner.setEnabled(!betaAutoButton.getSelection());
				}
			});

			Label upperBetaLabel = new Label(b2gComp,0);
			upperBetaLabel.setText("Upper bound for beta");
			upperBetaLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
			upperBetaSpinner = new Spinner(b2gComp,SWT.BORDER);
			upperBetaSpinner.setLayoutData(new GridData(SWT.FILL,0,true,false,1,1));
			upperBetaSpinner.setMinimum(1);
			upperBetaSpinner.setMaximum(100*(int)Math.pow(10, ALPHA_BETA_DIGITS));
			upperBetaSpinner.setSelection(100*(int)Math.pow(10, ALPHA_BETA_DIGITS));
			upperBetaSpinner.setDigits(ALPHA_BETA_DIGITS);
			upperBetaSpinner.setEnabled(true);
			new Label(b2gComp,0);

			Label priorLabel = new Label(b2gComp,0);
			priorLabel.setText("Expected number of terms");
			priorLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
			expectedNumberSpinner = new Spinner(b2gComp,SWT.BORDER);
			expectedNumberSpinner.setLayoutData(new GridData(SWT.FILL,0,true,false,1,1));
			expectedNumberSpinner.setMinimum(1);
			expectedNumberSpinner.setMaximum(50);
			expectedNumberSpinner.setSelection(5);
			expectedNumberSpinner.setEnabled(false);
			expectedNumberAutoButton = new Button(b2gComp,SWT.CHECK);
			expectedNumberAutoButton.setText("Auto");
			expectedNumberAutoButton.setSelection(true);
			expectedNumberAutoButton.addSelectionListener(new SelectionAdapter()
			{
				@Override
				public void widgetSelected(SelectionEvent e)
				{
					expectedNumberSpinner.setEnabled(!expectedNumberAutoButton.getSelection());
				}
			});
			
			Label mcmcStepsLabel = new Label(b2gComp,0);
			mcmcStepsLabel.setText("Number of steps for MCMC");
			mcmcStepsLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
			mcmcStepsSpinner = new Spinner(b2gComp,SWT.BORDER);
			mcmcStepsSpinner.setLayoutData(new GridData(SWT.FILL,0,true,false,1,1));
			mcmcStepsSpinner.setMaximum(10000000);
			mcmcStepsSpinner.setIncrement(50000);
			mcmcStepsSpinner.setPageIncrement(100000);
			mcmcStepsSpinner.setSelection(500000);
			new Label(b2gComp,0);
		}

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
			upperAlphaSpinner.setSelection((int)(GlobalPreferences.getUpperAlpha() * Math.pow(10, ALPHA_BETA_DIGITS) * 100));
			upperBetaSpinner.setSelection((int)(GlobalPreferences.getUpperBeta() * Math.pow(10, ALPHA_BETA_DIGITS) * 100 ));
			mcmcStepsSpinner.setSelection(GlobalPreferences.getMcmcSteps());

			if (!Double.isNaN(GlobalPreferences.getAlpha()))
			{
				alphaSpinner.setSelection((int)(GlobalPreferences.getAlpha() * Math.pow(10, ALPHA_BETA_DIGITS) * 100));
				alphaSpinner.setEnabled(true);
				alphaAutoButton.setSelection(false);
			}
			if (!Double.isNaN(GlobalPreferences.getBeta()))
			{
				betaSpinner.setSelection((int)(GlobalPreferences.getBeta() * Math.pow(10, ALPHA_BETA_DIGITS) * 100));
				betaSpinner.setEnabled(true);
				betaAutoButton.setSelection(false);
			}

			if (GlobalPreferences.getExpectedNumber() > 0)
			{
				expectedNumberSpinner.setSelection(GlobalPreferences.getExpectedNumber());
				expectedNumberSpinner.setEnabled(true);
				expectedNumberAutoButton.setSelection(false);
			}

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

	/**
	 * Returns the selected alpha.
	 *  
	 * @return the selected alpha or NaN if alpha value is not given.
	 */
	public double getAlpha()
	{
		if (alphaAutoButton != null)
			if (alphaAutoButton.getSelection()) return Double.NaN;

		if (alphaSpinner != null)
			return alphaSpinner.getSelection() / Math.pow(10, ALPHA_BETA_DIGITS) / 100.0;
		return Double.NaN;
	}
	
	public double getUpperAlpha()
	{
		return upperAlphaSpinner.getSelection() / Math.pow(10, ALPHA_BETA_DIGITS) / 100.0;
	}
	
	/**
	 * Returns the selected beta.
	 *  
	 * @return the selected beta or NaN if alpha value is not given.
	 */
	public double getBeta()
	{
		if (betaAutoButton != null)
			if (betaAutoButton.getSelection()) return Double.NaN;

		if (betaSpinner != null)
			return betaSpinner.getSelection() / Math.pow(10, ALPHA_BETA_DIGITS) / 100.0;
		return 0.1;
	}
	
	public double getUpperBeta()
	{
		return upperBetaSpinner.getSelection() / Math.pow(10, ALPHA_BETA_DIGITS) / 100.0;
	}
	
	public int getExpectedNumberOfTerms()
	{
		if (expectedNumberAutoButton != null)
			if (expectedNumberAutoButton.getSelection()) return -1;

		if (expectedNumberSpinner != null)
			return expectedNumberSpinner.getSelection();
		return 1;
	}
	
	/**
	 * Returns the number of MCMC steps to be performed.
	 * 
	 * @return
	 */
	public int getNumberOfMCMCSteps()
	{
		if (mcmcStepsSpinner != null)
			return mcmcStepsSpinner.getSelection();
		return 500000;
	}
}
