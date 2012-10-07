package sonumina.math.graph;

import java.awt.geom.Rectangle2D;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import att.grappa.Element;
import att.grappa.GraphEnumeration;
import att.grappa.GrappaConstants;
import att.grappa.GrappaPoint;
import att.grappa.Node;
import att.grappa.Parser;
import att.grappa.Subgraph;
import sonumina.math.graph.AbstractGraph.DotAttributesProvider;

/**
 * An extension of DirectedGraphLayout which uses dot.
 * This is a weird construct.
 * 
 * @author Sebastian Bauer
 *
 * @param <T>
 */
public class DirectedGraphDotLayout<T> extends DirectedGraphLayout<T>
{
	private static Logger logger = Logger.getLogger(DirectedGraphDotLayout.class.getCanonicalName());

	/**
	 * Dimensions of dot are specified using inches, this
	 * is the used dots per inch conversion number.
	 */
	final private static double DPI = 72;

	private DirectedGraphDotLayout(DirectedGraph<T> graph, IGetDimension<T> dimensionCallback, IPosition<T> positionCallback)
	{
		super(graph,dimensionCallback,positionCallback);
	}

	/**
	 * Walks through the graph hierarchy and emits the positions
	 * for each node using the supplied positionCallback.
	 * 
	 * @param e
	 * @param miny
	 */
	private void emitPosition(Element e, int miny)
	{
		switch (e.getType())
		{
			case	GrappaConstants.NODE:
					{
						Node node = (Node)e;
						GrappaPoint center = node.getCenterPoint();
		
						double w = (Double)node.getAttributeValue(Node.WIDTH_ATTR) * DPI;
						double h = (Double)node.getAttributeValue(Node.HEIGHT_ATTR) * DPI;
						int x = (int)(center.x - w/2);
						int y = (int)(center.y - miny - h/2);
						
						/* Retrieve back the slim graph index which was stored as a label */
						int index = Integer.parseInt((String) node.getAttributeValue(Node.LABEL_ATTR));

						positionCallback.set(slimGraph.getVertex(index), x, y);
					}
					break;

			case	GrappaConstants.EDGE:
					break;

			case	GrappaConstants.SUBGRAPH:
					{
						GraphEnumeration en = ((Subgraph)e).elements();

						while (en.hasMoreElements())
						{
							Element ne = en.nextGraphElement();
							if (e != ne)
								emitPosition(ne, miny);
						}
					}
					break;
		}
	}
	
	/**
	 * Tries to layout the graph using external dot application.
	 * 
	 * @param horizSpace
	 * @param vertSpace
	 * @return if layout was successful.
	 */
	private boolean layoutViaDot(int horizSpace, int vertSpace)
	{
		final Dimension dim = new Dimension();
		boolean rc = false;
		
		try {
			final File dotTmpFile = File.createTempFile("onto", ".dot");
			final File layoutedDotTmpFile = File.createTempFile("onto", ".dot");

			dotTmpFile.deleteOnExit();
			layoutedDotTmpFile.deleteOnExit();
			
			graph.writeDOT(new FileOutputStream(dotTmpFile), graph.getVertices(), new DotAttributesProvider<T>()
					{
						public String getDotNodeAttributes(T vt)
						{
							dimensionCallback.get(vt, dim);
							
							/* The fixedsize attribute tells dot not to change width and height.
							 * We store the unique slim graph index of this vertex as label. */
							return "width=" + dim.width / DPI + ",height=" + dim.height / DPI + ",fixedsize=true,shape=box,label=\"" + slimGraph.getVertexIndex(vt) + "\"";
						};
					}, horizSpace / DPI, vertSpace / DPI);
			String [] args = new String[]{
					"dot", dotTmpFile.getCanonicalPath(),
					"-Tdot", "-o", layoutedDotTmpFile.getCanonicalPath()};
			Process dotProcess = Runtime.getRuntime().exec(args);

			/* Gather error stream */
			int c;
			BufferedInputStream es = new BufferedInputStream(dotProcess.getErrorStream());
			StringBuilder errStr = new StringBuilder();
			while ((c = es.read()) != -1)
				errStr.append((char) c);

			/* Wait for process to be finished */
			dotProcess.waitFor();

			if (dotProcess.exitValue() == 0)
			{
				logger.info("Layouted graph that was stored at " + layoutedDotTmpFile.getCanonicalPath());

				/* Now parse the dot file using Grappa */
				Parser parser = new Parser(new FileInputStream(layoutedDotTmpFile), System.err);
				parser.parse();
				
				att.grappa.Graph g = parser.getGraph();
				g.setEditable(false);
				
				/* Inform the client about the size
				 * FIXME: Check for rounding errors */
				Rectangle2D bb = g.getBoundingBox();
				positionCallback.setSize((int)(bb.getMaxX() - bb.getMinX()), (int)(bb.getMaxY() - bb.getMinY()));
				
				/* And walk though the graph and emit the positions */
				emitPosition(g,(int)bb.getMinY());
				rc = true;
			} else
			{
				logger.severe(errStr.toString());
			}
		} catch (IOException e) { logger.log(Level.WARNING, "Unable to layout the graph", e);		
		} catch (InterruptedException e) { logger.log(Level.WARNING, "Unable to layout the graph", e);
		} catch (Exception e) { logger.log(Level.WARNING, "Unable to layout the graph", e);
		}

		return rc;
	}

	/**
	 * Lays out the graph using DirectedGraphDotLayout. If this for some reasons fail,
	 * the limited algorithm of DirectedGraphLayout is employed.
	 * 
	 * @param graph
	 * @param dimensionCallback
	 * @param positionCallback
	 * @param horizSpace
	 * @param vertSpace
	 */
	public static <T> void layout(DirectedGraph<T> graph, IGetDimension<T> dimensionCallback, IPosition<T> positionCallback, int horizSpace, int vertSpace)
	{
		if (graph.getNumberOfVertices() == 0)
			return;
		DirectedGraphDotLayout<T> layout = new DirectedGraphDotLayout<T>(graph,dimensionCallback,positionCallback);
		if (!layout.layoutViaDot(horizSpace,vertSpace))
			DirectedGraphLayout.layout(graph,dimensionCallback,positionCallback,horizSpace,vertSpace);

	}
}
