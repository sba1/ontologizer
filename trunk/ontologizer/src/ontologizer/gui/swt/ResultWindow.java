/*
 * Created on 12.04.2006
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ontologizer.gui.swt;

import ontologizer.calculation.AbstractGOTermsResult;
import ontologizer.calculation.EnrichedGOTermsResult;
import ontologizer.calculation.SemanticResult;
import ontologizer.calculation.svd.SVDResult;
import ontologizer.gui.swt.images.Images;
import ontologizer.gui.swt.result.AbstractResultComposite;
import ontologizer.gui.swt.result.EnrichedGOTermsComposite;
import ontologizer.gui.swt.result.IGraphAction;
import ontologizer.gui.swt.result.ITableAction;
import ontologizer.gui.swt.result.PValuesSVDGOTermsComposite;
import ontologizer.gui.swt.result.SVDGOTermsComposite;
import ontologizer.gui.swt.result.SemanticSimilarityComposite;
import ontologizer.gui.swt.support.IMinimizedAdapter;
import ontologizer.gui.swt.support.IRestoredAdapter;
import ontologizer.gui.swt.support.SWTUtil;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

/**
 * The window which displays the results of an analysis.
 * 
 * @author Sebastian Bauer
 */
public class ResultWindow extends ApplicationWindow
{
	private CTabFolder cTabFolder = null;
	private ToolBar toolbar = null;
	
	private Composite statusComposite = null;
	private Composite minimizedComposite = null;

	private Composite progressComposite = null;
	private Text progressText = null;
	private ProgressBar progressBar = null;

	private FileDialog graphOutputDialog;
	private FileDialog tableOutputDialog;
	
	/* No need to dispose this! */
	private Cursor appStartingCursor;

	/**
	 * The constructor.
	 * 
	 * @param display
	 */
	public ResultWindow(Display display)
	{
		super(display);

		/* Prevent the disposal of the window on a close event,
		 * but make the window invisible */
		shell.addShellListener(new ShellAdapter(){
			public void shellClosed(ShellEvent e)
			{
				e.doit = false;
				shell.setVisible(false);
			}
		});

		appStartingCursor = display.getSystemCursor(SWT.CURSOR_APPSTARTING);

		createSShell(display);

		graphOutputDialog = new FileDialog(shell, SWT.SAVE);
		tableOutputDialog = new FileDialog(shell, SWT.SAVE);

		shell.open();
	}

	public void addResults(SemanticResult sr)
	{
		boolean added = false;
		
		cTabFolder.setRedraw(false);

		CTabItem cTabItem = new CTabItem(cTabFolder, SWT.NONE);
		cTabItem.setText(sr.name);
		SemanticSimilarityComposite ssc = new SemanticSimilarityComposite(cTabFolder,0);
		ssc.setResult(sr);
		ssc.setMinimizedAdapter(minimizedAdapter);
		cTabItem.setControl(ssc);
		added = true;
		
		if (added && cTabFolder.getSelectionIndex() == -1)
		{
			cTabFolder.setSelection(0);
			updateWindowTitle();
		}
		cTabFolder.setRedraw(true);

	}

	public void addResults(AbstractGOTermsResult result)
	{
		boolean added = false;

		cTabFolder.setRedraw(false);
		if (result instanceof EnrichedGOTermsResult)
		{
			EnrichedGOTermsResult enrichedGOTermsResult = (EnrichedGOTermsResult) result;

			CTabItem cTabItem = new CTabItem(cTabFolder, SWT.NONE);
			EnrichedGOTermsComposite studyResultComposite = new EnrichedGOTermsComposite(cTabFolder,0);
			studyResultComposite.setResult(enrichedGOTermsResult);
			cTabItem.setText(enrichedGOTermsResult.getStudySet().getName());
			cTabItem.setControl(studyResultComposite);
			added = true;
		} else if (result instanceof SVDResult)
		{
			SVDResult svdResult = (SVDResult)result;

			CTabItem cTabItem = new CTabItem(cTabFolder, SWT.NONE);
			SVDGOTermsComposite svdComposite;

			if (svdResult.isPValues())
				svdComposite = new PValuesSVDGOTermsComposite(cTabFolder,0);
			else
				svdComposite = new SVDGOTermsComposite(cTabFolder,0); 

			svdComposite.setResult(svdResult);
			cTabItem.setText(svdComposite.getTitle());;
			cTabItem.setControl(svdComposite);
			added = true;
		}

		if (added && cTabFolder.getSelectionIndex() == -1)
		{
			cTabFolder.setSelection(0);
			updateWindowTitle();
		}
		cTabFolder.setRedraw(true);
	}

	public void appendLog(String text)
	{
		if (!progressText.isDisposed())
			progressText.setText(text);
	}

	/**
	 * Clears progress text after a period of time
	 * (asynchron)
	 */
	public void clearProgressText()
	{
		shell.getDisplay().timerExec(1000, new Runnable(){
			public void run()
			{
				if (!progressText.isDisposed())
					progressText.setText("");
			}
		});
	}

	/**
	 * Returns the IGraphAction instance.
	 * 
	 * @return
	 */
	private IGraphAction getSelectedCompositeAsGraphAction()
	{
		if (cTabFolder.getSelection() == null) return null;
		Control c = cTabFolder.getSelection().getControl();
		
		if (c instanceof IGraphAction) return (IGraphAction)c;
		return null;
	}
	
	private ITableAction getSelectedCompositeAsTableAction()
	{
		if (cTabFolder.getSelection() == null) return null;
		Control c = cTabFolder.getSelection().getControl();
		if (c instanceof ITableAction) return (ITableAction)c;
		return null;
	}
	
	/**
	 * Returns the currently selected result component.
	 * 
	 * @return
	 */
	private AbstractResultComposite getSelectedResultComposite()
	{
		if (cTabFolder.getSelection() == null) return null;
		Control c = cTabFolder.getSelection().getControl();
		
		if (c instanceof AbstractResultComposite) return (AbstractResultComposite)c;
		return null;
	}

	/**
	 * Returns the currently selected result component if it is an enrichment result.
	 * 
	 * @return
	 */
	private EnrichedGOTermsComposite getSelectedResultCompositeIfEnriched()
	{
		AbstractResultComposite comp = getSelectedResultComposite();
		if (comp == null) return null;

		if (comp instanceof EnrichedGOTermsComposite)
			return (EnrichedGOTermsComposite)comp;
		
		return null;
	}

	private void updateWindowTitle()
	{
		AbstractResultComposite comp = getSelectedResultComposite();
		if (comp != null)
			shell.setText("Ontologizer - Results for " + comp.getTitle());
		else 
			shell.setText("Ontologizer - Results");
	}

	/**
	 * This method initializes sShell
	 */
	private void createSShell(Display display)
	{
		shell.setText("Ontologizer - Results");
		shell.setLayout(new GridLayout());
		createToolBar(shell);
		createCTabFolder();
		createStatusComposite();
		shell.setSize(new org.eclipse.swt.graphics.Point(649,486));
	}

	/**
	 * Creates the toolbar.
	 * 
	 * @param parent
	 */
	private void createToolBar(Composite parent)
	{
		/**
		 * A listener suitable for DropDown buttons.
		 *
		 * @author Sebastian Bauer
		 */
		abstract class DropDownListener extends SelectionAdapter
		{
			private Menu menu;
			private Composite parent;

			public DropDownListener(Composite parent)
			{
				this.parent = parent;
			}

			abstract public Menu createMenu(Composite parent);
			abstract void defaultSelected();

			protected void processMenuEvent(final ToolItem toolItem, final MenuItem item)
			{
				final String s = item.getText().replace("...","");
				toolItem.setToolTipText(s);
				menu.setVisible(false);
			}

			@Override
			public void widgetSelected(SelectionEvent e)
			{
				if (menu == null)
					menu = createMenu(parent);

				if (e.detail == SWT.ARROW)
				{
					if (menu.isVisible())
						menu.setVisible(false);
					else
					{
						final ToolItem toolItem = (ToolItem)e.widget;
						final ToolBar toolBar = toolItem.getParent();
						Rectangle toolItemBounds = toolItem.getBounds();
						Point point = toolBar.toDisplay(toolItemBounds.x,toolItemBounds.y + toolItemBounds.height);
						menu.setLocation(point);
						menu.setVisible(true);
					}
				} else defaultSelected();
			}
		};

		
		toolbar = new ToolBar(parent,SWT.FLAT);
		final ToolItem saveTableItem = new ToolItem(toolbar,SWT.DROP_DOWN);
		saveTableItem.setToolTipText("Save Table");
		saveTableItem.setImage(Images.loadImage("savetable.png"));
		final ToolItem saveGraphItem = new ToolItem(toolbar,SWT.DROP_DOWN);
		saveGraphItem.setToolTipText("Save Graph");
		saveGraphItem.setImage(Images.loadImage("savegraph.png"));
		ToolItem previewGraphItem = new ToolItem(toolbar,0);
		previewGraphItem.setToolTipText("Preview Graph");
		previewGraphItem.setImage(Images.loadImage("previewgraph.png"));

		new ToolItem(toolbar,SWT.SEPARATOR);

		ToolItem zoomOutItem = new ToolItem(toolbar,0);
		zoomOutItem.setToolTipText("Zoom Out");
		zoomOutItem.setImage(Images.loadImage("zoomout.png"));
		ToolItem zoomInItem = new ToolItem(toolbar,0);
		zoomInItem.setImage(Images.loadImage("zoomin.png"));
		zoomInItem.setToolTipText("Zoom In");
		ToolItem resetZoomItem = new ToolItem(toolbar,0);
		resetZoomItem.setImage(Images.loadImage("resetzoom.png"));
		resetZoomItem.setToolTipText("Reset Zoom");
		final ToolItem scaleToFitItem = new ToolItem(toolbar,SWT.CHECK);
		scaleToFitItem.setToolTipText("Scale to Fit");
		scaleToFitItem.setImage(Images.loadImage("scaletofit.png"));
		scaleToFitItem.setSelection(true);

		/* Add listener */
		saveTableItem.addSelectionListener(new DropDownListener(parent.getShell())
		{
			private int tableActionNum;

			public Menu createMenu(Composite parent)
			{
				Menu menu = new Menu(parent);
				final MenuItem menuItem1 = new MenuItem(menu,SWT.NULL);
				menuItem1.setText("Save Result as ASCII Table...");
				menuItem1.addSelectionListener(new SelectionAdapter()
				{
					@Override
					public void widgetSelected(SelectionEvent e)
					{
						tableActionNum = 0;
						processMenuEvent(saveTableItem,menuItem1);
						performAction();
					}
				});
				
				final MenuItem menuItem2 = new MenuItem(menu,SWT.NULL);
				menuItem2.setText("Save Result as HTML...");
				menuItem2.addSelectionListener(new SelectionAdapter()
				{
					@Override
					public void widgetSelected(SelectionEvent e)
					{
						tableActionNum = 1;
						processMenuEvent(saveTableItem,menuItem2);
						performAction();
					}
				});

				final MenuItem menuItem4 = new MenuItem(menu,SWT.NULL);
				menuItem4.setText("Save Result as Latex Document...");
				menuItem4.addSelectionListener(new SelectionAdapter()
				{
					@Override
					public void widgetSelected(SelectionEvent e)
					{
						tableActionNum = 3;
						processMenuEvent(saveTableItem,menuItem4);
						performAction();
					}
				});

				final MenuItem menuItem3 = new MenuItem(menu,SWT.NULL);
				menuItem3.setText("Save Study Set with Annotations...");
				menuItem3.addSelectionListener(new SelectionAdapter()
				{
					@Override
					public void widgetSelected(SelectionEvent e)
					{
						tableActionNum = 2;
						processMenuEvent(saveTableItem,menuItem3);
						performAction();
					}
				});

				return menu;
			}

			protected void defaultSelected()
			{
				performAction();
			}
			
			private void performAction()
			{
				ITableAction tblAction = getSelectedCompositeAsTableAction();
				if (tblAction != null)
				{
					String path = tableOutputDialog.open();
					if (path != null)
					{
						switch (tableActionNum)
						{
							case 0: tblAction.tableSave(path); break;
							case 1: tblAction.htmlSave(path); break;
							case 2: tblAction.tableAnnotatedSetSave(path); break;
							case 3:
							{
								if (!path.endsWith(".tex"))
									path = path + ".tex";
								tblAction.latexSave(path);
								break;
							}
						}
					}
				}
			}
		});

		saveGraphItem.addSelectionListener(new DropDownListener(parent.getShell())
		{
			private String extension;

			public Menu createMenu(Composite parent)
			{
				Menu menu = new Menu(parent);

				final MenuItem menuItem0 = new MenuItem(menu,SWT.NULL);
				menuItem0.setText("Save Graph...");
				menuItem0.addSelectionListener(new SelectionAdapter()
				{
					@Override
					public void widgetSelected(SelectionEvent e)
					{
						extension = null;
						processMenuEvent(saveGraphItem,menuItem0);
						performAction();
					}
				});

				final MenuItem menuItem1 = new MenuItem(menu,SWT.NULL);
				menuItem1.setText("Save Graph as PNG...");
				menuItem1.addSelectionListener(new SelectionAdapter()
				{
					@Override
					public void widgetSelected(SelectionEvent e)
					{
						extension = "png";
						processMenuEvent(saveGraphItem,menuItem1);
						performAction();
					}
				});

				final MenuItem menuItem2 = new MenuItem(menu,SWT.NULL);
				menuItem2.setText("Save Graph as SVG...");
				menuItem2.addSelectionListener(new SelectionAdapter()
				{
					@Override
					public void widgetSelected(SelectionEvent e)
					{
						extension = "svg";
						processMenuEvent(saveGraphItem,menuItem2);
						performAction();
					}
				});

				final MenuItem menuItem3 = new MenuItem(menu,SWT.NULL);
				menuItem3.setText("Save Graph as DOT...");
				menuItem3.addSelectionListener(new SelectionAdapter()
				{
					@Override
					public void widgetSelected(SelectionEvent e)
					{
						extension = "dot";
						processMenuEvent(saveGraphItem,menuItem3);
						performAction();
					}
				});

				final MenuItem menuItem4 = new MenuItem(menu,SWT.NULL);
				menuItem4.setText("Save Graph as PS...");
				menuItem4.addSelectionListener(new SelectionAdapter()
				{
					@Override
					public void widgetSelected(SelectionEvent e)
					{
						extension = "ps";
						processMenuEvent(saveGraphItem,menuItem4);
						performAction();
					}
				});

				return menu;
			}

			void defaultSelected()
			{
				performAction();
			}

			private void performAction()
			{
				IGraphAction comp = getSelectedCompositeAsGraphAction();
				if (comp != null)
				{
					if (extension == null)
						graphOutputDialog.setFilterExtensions(new String[]{"*.png","*.dot","*.svg","*.ps"});
					else
						graphOutputDialog.setFilterExtensions(new String[]{"*." + extension});

					String path = graphOutputDialog.open();
					if (path != null)
					{
						/* If explicit extension has been given, ensure that the name
						 * actually has this suffix.
						 */
						if (extension != null)
						{
							if (!path.toLowerCase().endsWith("." + extension))
								path = path + "." + extension;
						}
						comp.saveGraph(path);
					}
				}
			}

		});

		previewGraphItem.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e)
			{
				AbstractResultComposite comp = getSelectedResultComposite();
				if (comp != null)
					comp.updateDisplayedGraph();
			}
		});

		/* Add listener for graph buttons */
		zoomOutItem.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e)
			{
				IGraphAction comp = getSelectedCompositeAsGraphAction();
				if (comp != null)
					comp.zoomOut();
				scaleToFitItem.setSelection(false);
			}
		});
		zoomInItem.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e)
			{
				IGraphAction comp = getSelectedCompositeAsGraphAction();
				if (comp != null)
					comp.zoomIn();
				scaleToFitItem.setSelection(false);
			}
		});
		resetZoomItem.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e)
			{
				IGraphAction comp = getSelectedCompositeAsGraphAction();
				if (comp != null)
					comp.resetZoom();
				scaleToFitItem.setSelection(false);
			}
		});
		scaleToFitItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e)
			{
				IGraphAction comp = getSelectedCompositeAsGraphAction();
				if (comp != null)
					comp.setScaleToFit(scaleToFitItem.getSelection());
			}
		});
	}		
	/**
	 * This method initializes cTabFolder	
	 *
	 */
	private void createCTabFolder()
	{
		GridData gridData = new org.eclipse.swt.layout.GridData();
		gridData.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;  // Generated
		gridData.grabExcessHorizontalSpace = true;  // Generated
		gridData.grabExcessVerticalSpace = true;  // Generated
		gridData.verticalAlignment = org.eclipse.swt.layout.GridData.FILL;  // Generated
		cTabFolder = new CTabFolder(shell, SWT.BORDER);
		cTabFolder.setLayoutData(gridData);  // Generated
		cTabFolder.setSimple(false);

		cTabFolder.addSelectionListener(new SelectionAdapter()
		{
			public void widgetSelected(SelectionEvent e)
			{
				updateWindowTitle();
			}
		});
	}

	/**
	 * This method initializes progressComposite	
	 *
	 */
	private void createStatusComposite()
	{
		statusComposite = new Composite(shell,0);
		statusComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL|GridData.GRAB_HORIZONTAL));
		GridLayout statusLayout = SWTUtil.newEmptyMarginGridLayout(2);
		statusLayout.horizontalSpacing = 0;
		statusComposite.setLayout(statusLayout);

		progressComposite = new Composite(statusComposite, SWT.NONE);
		progressComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL|GridData.GRAB_HORIZONTAL));
		progressComposite.setLayout(SWTUtil.newEmptyMarginGridLayout(2));

		progressText = new Text(progressComposite, SWT.READ_ONLY);
		progressText.setBackground(shell.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
		progressText.setEditable(false);
		progressText.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL|GridData.FILL_HORIZONTAL));
		progressBar = new ProgressBar(progressComposite, SWT.NONE);
		progressBar.setVisible(false);
	}

	/**
	 * Makes the progressbar invisible.
	 */
	public void hideProgressBar()
	{
		progressBar.setVisible(false);
	}
	
	/**
	 * Makes the progressbar visible.
	 */
	public void showProgressBar()
	{
		progressBar.setVisible(true);
	}

	/**
	 * Turns on/off the busy pointer.
	 * 
	 * @param busy
	 */
	public void setBusyPointer(boolean busy)
	{
		if (busy)
			shell.setCursor(appStartingCursor);
		else
			shell.setCursor(null);
	}

	/**
	 * Returns whether the window is disposed.
	 * 
	 * @return
	 */
	public boolean isDisposed()
	{
		return shell.isDisposed();
	}

	/**
	 * Add a new action, performed before the window gets disposed.
	 * 
	 * @param action
	 */
	public void addDisposeAction(final ISimpleAction action)
	{
		shell.addDisposeListener(new DisposeListener(){
			public void widgetDisposed(DisposeEvent e)
			{
				action.act();
			}});
	}

	
	/**
	 * Add a new action, performed before the window is closed.
	 * 
	 * @param action
	 */
	public void addCloseAction(final ISimpleAction action)
	{
		shell.addShellListener(new ShellAdapter()
		{
			public void shellClosed(ShellEvent e)
			{
				action.act();
			}
		});
	}
	
	/**
	 * Initialize the progress bar.
	 * @param max specifies the end of the progressbar's range.
	 */
	public void initProgress(int max)
	{
		progressBar.setMaximum(max);
	}
	
	/**
	 * Updates the progress bar according to the given value.
	 * 
	 * @param p
	 */
	public void updateProgress(int p)
	{
		progressBar.setSelection(p);
	}
	
	/**
	 * This is the minimized adapter whose method is called whenever
	 * something needs to minimized.
	 */
	private IMinimizedAdapter minimizedAdapter = new IMinimizedAdapter()
	{
		public Object addMinimized(String name, final IRestoredAdapter adapter)
		{
			if (minimizedComposite == null)
			{
				minimizedComposite = new Composite(statusComposite, SWT.NONE);
				minimizedComposite.setLayout(new RowLayout(SWT.HORIZONTAL));
			}

			final Button but = new Button(minimizedComposite,0);
			but.setText(name);
			but.addSelectionListener(new SelectionAdapter()
			{
				@Override
				public void widgetSelected(SelectionEvent e)
				{
					adapter.restored();
					but.dispose();
					minimizedComposite.layout();
					statusComposite.layout();
					statusComposite.getParent().layout();
				}
			});
			if (!minimizedComposite.isVisible()) minimizedComposite.setVisible(true);
			minimizedComposite.layout();
			statusComposite.layout();
			statusComposite.getParent().layout();
			return null;
		};
	};
}
