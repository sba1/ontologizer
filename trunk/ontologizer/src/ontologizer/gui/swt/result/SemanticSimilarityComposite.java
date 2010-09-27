package ontologizer.gui.swt.result;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Locale;

import javax.imageio.ImageIO;

import net.sourceforge.nattable.NatTable;
import ontologizer.GlobalPreferences;
import ontologizer.association.Gene2Associations;
import ontologizer.calculation.SemanticResult;
import ontologizer.go.Ontology;
import ontologizer.go.TermID;
import ontologizer.gui.swt.support.GraphCanvas;
import ontologizer.gui.swt.support.IGraphCanvas;
import ontologizer.gui.swt.support.IMinimizedAdapter;
import ontologizer.types.ByteString;
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
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Text;

/**
 * The GUI for semantic similarity measures.
 * 
 * @author Sebastian Bauer
 *
 */
public class SemanticSimilarityComposite extends Composite implements IGraphAction, ITableAction
{
	private SemanticResult result;

	private SemanticSimilarityNatModel semanticSimilarityNatModel;
	private NatTable natTable;
	private Point natTableLastSelected = new Point(-1,-1);

	private Text selectedSimilarityText;

	private ResultControls resultControls;
	private Browser browser;
	private IGraphCanvas graphCanvas;

	private static File geneSet1BackgroundFile;
	private static File geneSet2BackgroundFile;
	private static File geneSetBothBackgroundFile;
	
	static
	{
		try
		{
			geneSet1BackgroundFile = createBackgroundFile(Color.getHSBColor(180.f / 360.f, 1, 1),new Color(255,255,255));
			geneSet2BackgroundFile = createBackgroundFile(Color.getHSBColor(60.f / 360.f, 1, 1), new Color(255,255,255));
			geneSetBothBackgroundFile = createBackgroundFile(Color.getHSBColor(120.f / 360.f, 1, 1),new Color(255,255,255));
		} catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	};
	
	private static File createBackgroundFile(Color color, Color color2) throws IOException
	{
		File tmp = File.createTempFile("onto", ".png");
		BufferedImage bi = new BufferedImage(20,100,BufferedImage.TYPE_INT_ARGB);
		Graphics2D ig2 = bi.createGraphics();
		GradientPaint gp = new GradientPaint(0,0,color,0,100,color2);
		ig2.setPaint(gp);
		ig2.fillRect(0,0,20,100);
		ImageIO.write(bi,"PNG",tmp);
		return tmp;
	}



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
		HashSet<TermID> onlyG1 = new HashSet<TermID>();
		HashSet<TermID> onlyG2 = new HashSet<TermID>();
		HashSet<TermID> both = new HashSet<TermID>();
		
		Gene2Associations g2a1 = result.assoc.get(g1);
		Gene2Associations g2a2 = result.assoc.get(g2);

		if (g2a1 != null)
		{
			for (TermID t : g2a1.getAssociations())
				onlyG1.addAll(result.g.getTermsOfInducedGraph(null, t));
		}
			
		if (g2a2 != null)
		{	
			for (TermID t : g2a2.getAssociations())
				onlyG2.addAll(result.g.getTermsOfInducedGraph(null, t));
		}

		both.addAll(onlyG1);
		both.retainAll(onlyG2);
		
		onlyG1.removeAll(both);
		onlyG2.removeAll(both);

		StringBuilder str = new StringBuilder();
		str.append("<html>");
		
		str.append("<body>");

		str.append("<h1>");
		str.append(g1.toString());
		str.append(" vs. ");
		str.append(g2.toString());
		str.append("</h1>");
		
		str.append("<table border=\"1\">");
		str.append("<tr>");
		str.append("<th>" + g1.toString() + "</th>");
		str.append("<th>" + g2.toString() + "</th>");
		str.append("</tr>");

		str.append("<tr>");
		str.append("<td " + buildBackgroundAttribute(geneSet1BackgroundFile) + ">");
		for (TermID t:onlyG1)
		{
			str.append(t.toString());
			str.append(" ");
		}
		str.append("</td>");
		str.append("<td " + buildBackgroundAttribute(geneSet2BackgroundFile) + ">");
//		str.append("<td background=\""+ geneSet2BackgroundFile.getAbsolutePath()+ "\">");
		for (TermID t:onlyG2)
		{
			str.append(t.toString());
			str.append(" ");
		}
		str.append("</td>");
		str.append("</tr>");
		
		
		str.append("<tr>");
		
		str.append("<td colspan=\"2\" " + buildBackgroundAttribute(geneSetBothBackgroundFile) + ">");
		for (TermID t:both)
		{
			str.append(t.toString());
			str.append(" ");
		}
		str.append("</td>");
		str.append("</tr>");
		
		str.append("</table>");
		
		str.append("</body>");
		str.append("<html/>");
		
		browser.setText(str.toString());
	}
	
	private String buildBackgroundAttribute(File file)
	{
		if (file == null) return "";
		return "background=\""+ file.getAbsolutePath()+"\" style=\"background-repeat: repeat-x;\"";
	}

	/**
	 * 
	 *
	 * @author Sebastian Bauer
	 */
	class SemanticGOGraphGenerationThread extends AbstractGOGraphGenerationThread
	{
		private HashSet<TermID> gene1Set = new HashSet<TermID>();
		private HashSet<TermID> gene2Set = new HashSet<TermID>();

		public SemanticGOGraphGenerationThread(ByteString g1, ByteString g2, Display display, Ontology graph, String dotCMDPath)
		{
			super(display, graph, dotCMDPath);

			HashSet<TermID> leafTerms = new HashSet<TermID>();

			Gene2Associations g2a1 = result.assoc.get(g1);
			Gene2Associations g2a2 = result.assoc.get(g2);

			if (g2a1 != null && g2a2 != null)
			{	
				for (TermID t : g2a1.getAssociations())
					gene1Set.addAll(result.g.getTermsOfInducedGraph(null, t));
			
				for (TermID t : g2a2.getAssociations())
					gene2Set.addAll(result.g.getTermsOfInducedGraph(null, t));

				leafTerms.addAll(g2a1.getAssociations());
				leafTerms.addAll(g2a2.getAssociations());
			}
			
			setLeafTerms(leafTerms);
		}

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

			if (result.g.isRootTerm(id))
			{
				attributes.append("Gene Ontology");
			} else
			{
				attributes.append(id.toString());
				attributes.append("\\n");
				
				String label = result.g.getTerm(id).getName();
				if (GlobalPreferences.getWrapColumn() != -1)
					label = Util.wrapLine(label,"\\n",GlobalPreferences.getWrapColumn());
				
				attributes.append(label);
			}
			attributes.append("\\nIC: ");
			attributes.append(String.format("%g",result.calculation.p(id)));
			attributes.append("\"");
			
			double saturation = 1.0f - result.calculation.p(id)*0.9f;// 1.0f;// - (((float) rank + 1) / significants_count) * 0.8f;

			/* Always full brightness */
			double brightness = 1.0f;

			double hue = 0.0;
			/* Hue depends on set */
			if (gene1Set.contains(id))
			{
				if (gene2Set.contains(id))
					hue = 120.f / 360;
				else hue = 180.f / 360;
			} else
			{
				if (gene2Set.contains(id)) hue = 60.f / 360;
			}

			String style = "filled,gradientfill";
			String fillcolor = String.format(Locale.US, "%f,%f,%f", hue, saturation, brightness);
			attributes.append(",style=\""+ style + "\",color=\"white\",fillcolor=\"" + fillcolor + "\"");

			return attributes.toString();
		}

		public String getDotEdgeAttributes(TermID id1, TermID id2)
		{
			return null;
		};
		
	};

	private void updateGraph(ByteString g1, ByteString g2)
	{
		SemanticGOGraphGenerationThread sgggt = 
			new SemanticGOGraphGenerationThread(g1,g2,getDisplay(),result.g,GlobalPreferences.getDOTPath()); 
		sgggt.start();
	}

	public void setResult(SemanticResult result)
	{
		this.result = result;
		semanticSimilarityNatModel.setValues(result.mat);
		semanticSimilarityNatModel.setNames(result.names);
		natTable.updateResize();
	}

	public void setMinimizedAdapter(IMinimizedAdapter minimizedAdapter)
	{
		resultControls.setMinimizedAdapter(minimizedAdapter);
	}

	public void resetZoom()
	{
		graphCanvas.zoomReset();
	}

	public void setScaleToFit(boolean fit)
	{
		graphCanvas.setScaleToFit(fit);
	}

	public void zoomIn()
	{
		graphCanvas.zoomIn();
	}

	public void zoomOut()
	{
		graphCanvas.zoomOut();
	}

	public void htmlSave(String path)
	{
		// TODO Auto-generated method stub
		
	}

	public void tableAnnotatedSetSave(String path)
	{
		// TODO Auto-generated method stub
		
	}

	public void tableSave(String path)
	{
		File tableFile = new File(path);
		result.writeTable(tableFile);
	}
	
	public void latexSave(String path)
	{
		MessageBox mbox = new MessageBox(getShell(), SWT.ICON_ERROR | SWT.OK);
		mbox.setMessage("Storing results of a semantic similarity\nanalsis is not supported");
		mbox.setText("Ontologizer - Error");
		mbox.open();

	}

	public void saveGraph(String file)
	{
		if (natTableLastSelected.x != -1)
		{
			ByteString gene1 = result.names[natTableLastSelected.x];
			ByteString gene2 = result.names[natTableLastSelected.y];

			SemanticGOGraphGenerationThread sgggt = new SemanticGOGraphGenerationThread(gene1,gene2,getDisplay(),result.g,GlobalPreferences.getDOTPath())
			{
				@Override
				public void layoutFinished(boolean success, String message, File pngFile, File dotFile)
				{
					if (!success && !isDisposed())
					{
						MessageBox mbox = new MessageBox(getShell(), SWT.ICON_ERROR | SWT.OK);
						mbox.setMessage("Unable to execute the 'dot' tool!\n\n" + message);
						mbox.setText("Ontologizer - Error");
						mbox.open();
					}
				}
			};
			sgggt.setGfxOutFilename(file);
			sgggt.start();
		}
	}
}
