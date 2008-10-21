package ontologizer.gui.swt.result;

import ontologizer.gui.swt.support.GraphCanvas;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;

abstract class FolderComposite extends Composite
{
	private CTabFolder graphFolder;
	private CTabItem tabItem;
	private Composite contents;
	
	public FolderComposite(Composite parent, int style)
	{
		super(parent, style);
		setLayout(new FillLayout());

		graphFolder = new CTabFolder(this,SWT.BORDER);
		graphFolder.setMaximizeVisible(true);
		graphFolder.setSingle(true);
		graphFolder.setSelectionBackground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
		tabItem = new CTabItem(graphFolder,0);

		contents = createContents(graphFolder);
		tabItem.setControl(contents);
		graphFolder.setSelection(0);
	}
	
	public void setText(String text)
	{
		tabItem.setText(text);
	}
	
	protected abstract Composite createContents(Composite parent);
	
	public Composite getContents()
	{
		return contents;
	}
}

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
				return comp;
			}
		};
		browserComposite.setText("Browser");
		
	}

	public Composite getTableComposite()
	{
		return tableComposite.getContents();
	}
}
