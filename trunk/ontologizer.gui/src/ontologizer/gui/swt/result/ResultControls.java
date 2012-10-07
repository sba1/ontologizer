package ontologizer.gui.swt.result;

import ontologizer.gui.swt.ISimpleAction;
import ontologizer.gui.swt.support.FolderComposite;
import ontologizer.gui.swt.support.GraphCanvas;
import ontologizer.gui.swt.support.IGraphCanvas;
import ontologizer.gui.swt.support.IMinimizedAdapter;
import ontologizer.gui.swt.support.IRestoredAdapter;
import ontologizer.gui.swt.support.PGraphCanvas;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;

/**
 * Implements the logic of the result controls.
 * 
 * @author Sebastian Bauer
 */
public class ResultControls extends Composite
{
	private SashForm verticalSashForm;
	private SashForm upperSashForm;

	private FolderComposite tableComposite;
	private FolderComposite graphComposite;
	private FolderComposite browserComposite;

	private IGraphCanvas graphCanvas;
	private Browser browser;

	private IMinimizedAdapter minimizedAdapter;

	private ISimpleAction restoreAction = new ISimpleAction()
	{
		public void act()
		{
			upperSashForm.setMaximizedControl(null);
			verticalSashForm.setMaximizedControl(null);
		}
	};

	public ResultControls(Composite parent, int style)
	{
		super(parent, style);
		
		setLayout(new FillLayout());

		verticalSashForm = new SashForm(this, SWT.VERTICAL);
		upperSashForm = new SashForm(verticalSashForm, SWT.HORIZONTAL);

		/* Table */
		tableComposite = new FolderComposite(upperSashForm,0)
		{
			@Override
			protected Composite createContents(Composite parent)
			{
				Composite comp = new Composite(parent,0);
				comp.setLayout(new FillLayout());
				return comp;
			}
		};
		tableComposite.setText("Table");
		tableComposite.addMaximizeAction(new ISimpleAction()
		{
			public void act()
			{
				verticalSashForm.setMaximizedControl(upperSashForm);
				upperSashForm.setMaximizedControl(tableComposite);
			}
		});
		tableComposite.addRestoreAction(restoreAction);

		/* Graph */
		graphComposite = new FolderComposite(upperSashForm,0)
		{
			@Override
			protected Composite createContents(Composite parent)
			{
				Composite comp = new Composite(parent,0);
				comp.setLayout(new FillLayout());
				graphCanvas = new GraphCanvas(comp,0);
				return comp;
			}
		};
		graphComposite.setText("Graph");
		graphComposite.addMaximizeAction(new ISimpleAction()
		{
			public void act()
			{
				verticalSashForm.setMaximizedControl(upperSashForm);
				upperSashForm.setMaximizedControl(graphComposite);
			}
		});
		graphComposite.addRestoreAction(restoreAction);
		graphComposite.addMinimizeAction(new ISimpleAction()
		{
			public void act()
			{
				if (minimizedAdapter != null)
				{
					graphComposite.setVisible(false);
					upperSashForm.layout();
					
					minimizedAdapter.addMinimized("Graph", new IRestoredAdapter()
					{
						public void restored()
						{
							graphComposite.setVisible(true);
							upperSashForm.layout();
						}
					});
				}
			}
		});

		/* Browser */
		browserComposite = new FolderComposite(verticalSashForm,0)
		{
			@Override
			protected Composite createContents(Composite parent)
			{
				Composite comp = new Composite(parent,0);
				comp.setLayout(new FillLayout());
				browser = new Browser(comp, SWT.BORDER);
				return comp;
			}
		};
		browserComposite.setText("Browser");
		browserComposite.addMaximizeAction(new ISimpleAction()
		{
			public void act()
			{
				verticalSashForm.setMaximizedControl(browserComposite);
			}
		});
		browserComposite.addRestoreAction(restoreAction);
		browserComposite.addMinimizeAction(new ISimpleAction()
		{
			public void act()
			{
				if (minimizedAdapter != null)
				{
					browserComposite.setVisible(false);
					verticalSashForm.layout();
					
					minimizedAdapter.addMinimized("Browser", new IRestoredAdapter()
					{
						public void restored()
						{
							browserComposite.setVisible(true);
							verticalSashForm.layout();
						}
					});
				}
			}
		});

		
	}

	public Composite getTableComposite()
	{
		return tableComposite.getContents();
	}
	
	public void addBrowserLocationListener(LocationListener ll)
	{
		browser.addLocationListener(ll);
	}
	
	public IGraphCanvas getGraphCanvas()
	{
		return graphCanvas;
	}
	
	public Browser getBrowser()
	{
		return browser;
	}
	
	public void setMinimizedAdapter(IMinimizedAdapter minimizedAdapter)
	{
		this.minimizedAdapter = minimizedAdapter;
	}
}
