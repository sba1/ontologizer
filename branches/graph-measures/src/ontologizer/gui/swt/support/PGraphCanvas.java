package ontologizer.gui.swt.support;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;

import org.eclipse.swt.widgets.Composite;

import att.grappa.Edge;
import att.grappa.Element;
import att.grappa.GraphEnumeration;
import att.grappa.GrappaConstants;
import att.grappa.GrappaLine;
import att.grappa.GrappaPoint;
import att.grappa.Node;
import att.grappa.Parser;
import att.grappa.Subgraph;

import edu.umd.cs.piccolox.swt.PSWTCanvas;
import edu.umd.cs.piccolox.swt.PSWTPath;
import edu.umd.cs.piccolox.swt.PSWTText;

public class PGraphCanvas extends PSWTCanvas implements IGraphCanvas
{
	public PGraphCanvas(Composite parent, int style)
	{
		super(parent, style);
	}

	/**
	 * Prepare the given element.
	 * 
	 * @param e
	 */
	private void prepareElement(Element e)
	{
		switch (e.getType())
		{
			case	GrappaConstants.EDGE:
					{
						GrappaLine line = (GrappaLine)e.getAttributeValue(Edge.POS_ATTR);
						PSWTPath p = new PSWTPath(line);
						getLayer().addChild(p);
					}
					break;

			case	GrappaConstants.NODE:
					{
						Node node = (Node)e;
						GrappaPoint center = node.getCenterPoint();
		
						int x = (int)center.x;
						int y = (int)center.y;
						double w = (Double)node.getAttributeValue(Node.WIDTH_ATTR);
						double h = (Double)node.getAttributeValue(Node.HEIGHT_ATTR);
						int shape = (Integer)node.getAttributeValue(Node.SHAPE_ATTR);
		
						w *= 72;
						h *= 72;

						if (shape != GrappaConstants.PLAINTEXT_SHAPE)
						{
							PSWTPath pNode = PSWTPath.createEllipse((float)(x - w/2), (float)(y-h/2), (float)w, (float)h);
							pNode.setPaint(Color.green);
							getLayer().addChild(pNode);
						}
						
						String label = (String)node.getAttributeValue(Node.LABEL_ATTR);
						if (label != null)
						{
							String [] labelArray = label.split("\\\\n");
							PSWTText [] textArray = new PSWTText[labelArray.length];
							double textHeight = 0;
							
							for (int i=0;i<labelArray.length;i++)
							{
								textArray[i] = new PSWTText(labelArray[i]);
								textArray[i].setTransparent(true);
								textArray[i].setGreekThreshold(1);
								textHeight += textArray[i].getFullBoundsReference().height;
							}
							double textOffsetY = -h / 2 + (h - textHeight)/2;

							for (int i=0;i<labelArray.length;i++)
							{
								textArray[i].translate(x - w / 2 + (w - textArray[i].getFullBoundsReference().width)/2, y + textOffsetY);
								getLayer().addChild(textArray[i]);
								textOffsetY += textArray[i].getFullBoundsReference().height; 
							}
						}

					}
					break;

			case	GrappaConstants.SUBGRAPH:
					{
						GraphEnumeration en = ((Subgraph)e).elements();

						while (en.hasMoreElements())
						{
							Element ne = en.nextGraphElement();
							if (e != ne)
								prepareElement(ne);
						}
					}
					break;
		}
	}

	
	
	/**
	 * Returns the name of the currently selected Node.
	 * 
	 * @return
	 */
	public String getNameOfCurrentSelectedNode()
	{
		return null;
	}

	/**
	 * Selectes the node with the given name. Other nodes are deselected.
	 * 
	 * @param name
	 */
	public void selectNode(String name)
	{
	}

	public void setLayoutedDotFile(File file) throws Exception
	{
		Parser parser = new Parser(new FileInputStream(file), System.err);
		parser.parse();

		getLayer().removeAllChildren();
		
		prepareElement(parser.getGraph());
		
		getCamera().setViewBounds(getLayer().getFullBounds());
	}

	
	public void zoomOut()
	{
	}

	/**
	 * Zoom in.
	 */
	public void zoomIn()
	{
	}

	/**
	 * Reset the zoom.
	 */
	public void zoomReset()
	{
	}

	/**
	 * Set whether the magnification should be chosen such that, the complete
	 * graph is visible (also after resizing).
	 * 
	 * @param selection
	 */
	public void setScaleToFit(boolean selection)
	{
	}
	
	/**
	 * Add a new selection listener. The text field is used
	 * for the node's name.
	 * 
	 * @param sel
	 */
//	public void addSelectionListener(SelectionListener sel)
//	{
//
//	}
//
//	public void addMouseListener(MouseListener mouse)
//	{
//	}

}
