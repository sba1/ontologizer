package ontologizer.gui.swt.support;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;

import org.eclipse.zest.core.widgets.Graph;
import org.eclipse.zest.core.widgets.GraphConnection;
import org.eclipse.zest.core.widgets.GraphNode;
import org.eclipse.zest.core.widgets.ZestStyles;
import org.eclipse.zest.layouts.LayoutStyles;
import org.eclipse.zest.layouts.algorithms.TreeLayoutAlgorithm;

import att.grappa.Edge;
import att.grappa.Element;
import att.grappa.GraphEnumeration;
import att.grappa.GrappaConstants;
import att.grappa.Node;
import att.grappa.Parser;
import att.grappa.Subgraph;

public class ZestGraphCanvas extends Composite implements IGraphCanvas {

	private org.eclipse.zest.core.widgets.Graph zestGraph;
	private att.grappa.Graph grappaGraph;

	private HashMap<Node,GraphNode> grappaNode2ZestNode = new HashMap<Node,GraphNode>();

	public ZestGraphCanvas(Composite parent, int style)
	{
		super(parent, style);

		setLayout(new FillLayout());
	}

	public void setLayoutedDotFile(File dotFile) throws Exception
	{
		Parser parser = new Parser(new FileInputStream(dotFile), System.err);
		parser.parse();

		grappaGraph = parser.getGraph();
		grappaGraph.setEditable(false);

		if (zestGraph != null) zestGraph.dispose();
		zestGraph = new Graph(this, SWT.NONE);
		zestGraph.setConnectionStyle(ZestStyles.CONNECTIONS_DIRECTED);
		grappaNode2ZestNode.clear();

		updateZestGraph(grappaGraph);

		zestGraph.setLayoutAlgorithm(new TreeLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING), true);
		layout();
		zestGraph.redraw();
	}

	private void updateZestGraph(att.grappa.Element e)
	{
		switch (e.getType())
		{
			case	GrappaConstants.NODE:
					{
						Node node = (Node)e;
						String label = (String)node.getAttributeValue(Node.LABEL_ATTR);
						if (label == null) label = "";
						label = label.replace("\\n", "\n");

						GraphNode zestNode = new GraphNode(zestGraph,ZestStyles.CONNECTIONS_DIRECTED, label);
						grappaNode2ZestNode.put(node,zestNode);
					}
					break;

			case	GrappaConstants.EDGE:
					{
						Edge edge = (Edge)e;

						GraphNode source = grappaNode2ZestNode.get(edge.getTail());
						GraphNode dest = grappaNode2ZestNode.get(edge.getHead());

						if (source != null && dest != null)
							new GraphConnection(zestGraph, SWT.NONE, source, dest);
					}
					break;

			case	GrappaConstants.SUBGRAPH:
					{
						GraphEnumeration en = ((Subgraph)e).elements();

						while (en.hasMoreElements())
						{
							Element ne = en.nextGraphElement();
							if (e != ne)
								updateZestGraph(ne);
						}
					}
					break;
		}

	}

	public void setScaleToFit(boolean fit)
	{
	}

	public void zoomIn() {
	}

	public void zoomOut() {
	}

	public void zoomReset() {
	}

}
