package ontologizer.gui.swt.result;

import ontologizer.gui.swt.ISimpleAction;
import ontologizer.gui.swt.support.GraphCanvas;

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

	private GraphCanvas graphCanvas;
	private Browser browser;

	public ResultControls(Composite parent, int style)
	{
		super(parent, style);
		
		setLayout(new FillLayout());

		verticalSashForm = new SashForm(this, SWT.VERTICAL);
		upperSashForm = new SashForm(verticalSashForm, SWT.HORIZONTAL);

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
		browserComposite.addRestoreAction(new ISimpleAction()
		{
			public void act()
			{
				verticalSashForm.setMaximizedControl(null);
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
	
	public GraphCanvas getGraphCanvas()
	{
		return graphCanvas;
	}
	
	public Browser getBrowser()
	{
		return browser;
	}
}
