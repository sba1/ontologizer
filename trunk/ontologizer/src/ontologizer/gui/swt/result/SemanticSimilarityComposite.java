package ontologizer.gui.swt.result;

import java.io.File;
import java.util.HashSet;

import net.sourceforge.nattable.NatTable;
import ontologizer.ByteString;
import ontologizer.association.Gene2Associations;
import ontologizer.calculation.SemanticResult;
import ontologizer.go.TermID;
import ontologizer.gui.swt.GlobalPreferences;
import ontologizer.gui.swt.support.GraphCanvas;
import ontologizer.util.Util;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;

/**
 * The GUI for semantic similarity measures.
 * 
 * @author Sebastian Bauer
 *
 */
public class SemanticSimilarityComposite extends Composite
{
	private SemanticResult result;

	private SemanticSimilarityNatModel semanticSimilarityNatModel;
	private NatTable natTable;
	private Point natTableLastSelected = new Point(-1,-1);

	private Text selectedSimilarityText;

	private ResultControls resultControls;
	private Browser browser;
	private GraphCanvas graphCanvas;

	public SemanticSimilarityComposite(Composite parent, int style)
	{
		super(parent, style);
		
		setLayout(new FillLayout());
	
		resultControls = new ResultControls(this,0);
		browser = resultControls.getBrowser();
		graphCanvas = resultControls.getGraphCanvas();

		Composite tableComposite = resultControls.getTableComposite();
		tableComposite.setLayout(new GridLayout());

		semanticSimilarityNatModel = new SemanticSimilarityNatModel();
		semanticSimilarityNatModel.setSingleCellSelection(true);
		semanticSimilarityNatModel.setEnableMoveColumn(false);
		
		natTable = new NatTable(tableComposite,
								SWT.NO_BACKGROUND | SWT.NO_REDRAW_RESIZE | SWT.DOUBLE_BUFFERED | SWT.V_SCROLL | SWT.H_SCROLL,
				                semanticSimilarityNatModel);
		natTable.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		natTable.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseUp(MouseEvent e)
			{
				updateSelectedText();
			}
		});
		natTable.addMouseMoveListener(new MouseMoveListener()
		{
			public void mouseMove(MouseEvent e)
			{
				updateSelectedText();
			}
		});
		
		selectedSimilarityText = new Text(tableComposite,SWT.BORDER);
		selectedSimilarityText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
	}

	public void updateSelectedText()
	{
		Point newNatTableLastSelected = natTable.getSelectionSupport().getLastSelectedCell();
		if (natTableLastSelected.x != newNatTableLastSelected.x ||
			natTableLastSelected.y != newNatTableLastSelected.y)
		{
			selectedSimilarityText.setText(Double.toString(getLastSelectedValue()));
			
			ByteString gene1 = result.names[newNatTableLastSelected.x];
			ByteString gene2 = result.names[newNatTableLastSelected.y];

			updateBrowser(gene1,gene2);
			updateGraph(gene1,gene2);

			natTableLastSelected.x = newNatTableLastSelected.x;
			natTableLastSelected.y = newNatTableLastSelected.y;
		}
			
//		natTableLastSelected = newNatTableLastSelected;
//		selectedSimilarityText.setText(Double.toString(getLastSelectedValue()));
	}
	
	public double getLastSelectedValue()
	{
		Point p;
		
		p = natTable.getSelectionSupport().getLastSelectedCell();
		if (p == null) return Double.NaN;
		
		int x = p.x;
		int y = p.y;
		
		return semanticSimilarityNatModel.getValue(x,y); 
	}

	private void updateBrowser(ByteString g1, ByteString g2)
	{
		StringBuilder str = new StringBuilder();
		str.append("<html>");
		str.append("<body>");
		str.append("<h1>");
		str.append(g1.toString());
		str.append(" ");
		str.append(g2.toString());
		str.append("</h1>");
		str.append("</body>");
		str.append("<html/>");
		
		browser.setText(str.toString());
	}
	
	private void updateGraph(ByteString g1, ByteString g2)
	{
		HashSet<TermID> leafTerms = new HashSet<TermID>();
		
		AbstractGOGraphGenerationThread gggt = new AbstractGOGraphGenerationThread(getDisplay(),result.g,GlobalPreferences.getDOTPath())
		{
			public void layoutFinished(boolean success, String msg, File pngFile, File dotFile)
			{
				if (success)
				{
					try
					{
						graphCanvas.setLayoutedDotFile(dotFile);
					} catch (Exception e)
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}

			public String getDotNodeAttributes(TermID id)
			{
				StringBuilder attributes = new StringBuilder();
				attributes.append("label=\"");

				if (result.g.isRootGOTermID(id))
				{
					attributes.append("Gene Ontology");
				} else
				{
					attributes.append(id.toString());
					attributes.append("\\n");
					
					String label = result.g.getGOTerm(id).getName();
					if (GlobalPreferences.getWrapColumn() != -1)
						label = Util.wrapLine(label,"\\n",GlobalPreferences.getWrapColumn());
					
					attributes.append(label);
				}
				attributes.append("\"");
				
				return attributes.toString();
			};
		};

		Gene2Associations g2a1 = result.assoc.get(g1);
		Gene2Associations g2a2 = result.assoc.get(g2);
		
		leafTerms.addAll(g2a1.getAssociations());
		leafTerms.addAll(g2a2.getAssociations());
		
		gggt.setLeafTerms(leafTerms);
		gggt.start();
	}
	
	public void setResult(SemanticResult result)
	{
		this.result = result;
		semanticSimilarityNatModel.setValues(result.mat);
		semanticSimilarityNatModel.setNames(result.names);
		natTable.updateResize();
	}
}
