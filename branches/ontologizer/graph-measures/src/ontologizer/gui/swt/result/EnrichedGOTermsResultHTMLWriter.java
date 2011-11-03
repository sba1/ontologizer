package ontologizer.gui.swt.result;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;

import ontologizer.calculation.AbstractGOTermProperties;
import ontologizer.calculation.EnrichedGOTermsResult;
import ontologizer.calculation.b2g.Bayes2GOEnrichedGOTermsResult;
import ontologizer.calculation.b2g.Bayes2GOGOTermProperties;
import ontologizer.go.TermID;
import ontologizer.gui.swt.support.GraphPaint;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.widgets.Display;

import att.grappa.Element;
import att.grappa.Graph;
import att.grappa.GraphEnumeration;
import att.grappa.GrappaConstants;
import att.grappa.GrappaPoint;
import att.grappa.Node;
import att.grappa.Parser;
import att.grappa.Subgraph;

public class EnrichedGOTermsResultHTMLWriter
{
	static class AllMapData
	{
		public double minX;
		public double minY;
		
		public HashMap<TermID,Node> term2node = new HashMap<TermID, Node>();
	}

	static private void buildNodeRectangles(Element e, AllMapData data)
	{
		switch (e.getType())
		{
			case	GrappaConstants.NODE:
					{
						try
						{
							Node n = (Node)e;
							GrappaPoint center = n.getCenterPoint();

							double x = center.x;
							double y = center.y;
//							double w = (Double)n.getAttributeValue(Node.WIDTH_ATTR);
//							double h = (Double)n.getAttributeValue(Node.HEIGHT_ATTR);

							int id = Integer.parseInt(n.getName());
							TermID tid = new TermID(id);
							data.term2node.put(tid,n);

							if (x < data.minX) data.minX = x;
							if (y < data.minY) data.minY = y;
						} catch (NumberFormatException nfe) {}
					}
					break;

			case	GrappaConstants.SUBGRAPH:
					{
						GraphEnumeration en = ((Subgraph)e).elements();

						while (en.hasMoreElements())
						{
							Element ne = en.nextGraphElement();
							if (e != ne)
								buildNodeRectangles(ne, data);
						}
					}
					break;
		}

	}

	/**
	 * @param htmlFile
	 * @param dotFile 
	 * @param pngFile 
	 */
	public static void write(EnrichedGOTermsResult result, File htmlFile, File dotFile)
	{
		try
		{
			boolean showMarginals = result instanceof Bayes2GOEnrichedGOTermsResult;

			File pngFile = new File(htmlFile + ".png");

			/* Parse the dot file */
			Parser parser = new Parser(new FileInputStream(dotFile), System.err);
			parser.parse();
			Graph g = parser.getGraph();
			g.setEditable(false);

			/* Build and store the graphics, remember the transformation */
			Display disp = Display.getDefault();
			GraphPaint gp = new GraphPaint(disp,g);
			float gw = (float)(g.getBoundingBox().getMaxX() - g.getBoundingBox().getMinX() + 1);
			float gh = (float)(g.getBoundingBox().getMaxY() - g.getBoundingBox().getMinY() + 1);

			float scale = 1.0f;

			if (gw > 4000)
				scale = 4000f / gw;

			float pw = gw * scale;
			float ph = gh * scale;

			Image img = new Image(disp,(int)pw,(int)ph);
			GC gc = new GC(img);
			gc.setAntialias(SWT.ON);
			Transform transform = new Transform(disp);
			float alignedXOffset = 0;
			float alignedYOffset = gh;
			transform.scale(scale, scale);
			transform.translate(alignedXOffset, alignedYOffset);

			gc.setTransform(transform);
			gp.drawGraph(gc);

			ImageLoader il = new ImageLoader();
			il.data = new ImageData[]{img.getImageData()};
			il.save(pngFile.getAbsolutePath(), SWT.IMAGE_PNG);

			gc.setTransform(null);
			gc.dispose();
			img.dispose();
			gp.dispose();

			/* Build ID->Node mapping */
			AllMapData data = new AllMapData();
			buildNodeRectangles(g,data);
			
			PrintWriter out = new PrintWriter(htmlFile);
			
			out.println("<html>");
			out.println("<body>");

			out.println("<map name=\"graph\">");

			for (Entry<TermID,Node> entry : data.term2node.entrySet())
			{
				Node n = entry.getValue();
				GrappaPoint center = n.getCenterPoint();

				double cx = center.x;
				double cy = center.y;

				double w = (Double)n.getAttributeValue(Node.WIDTH_ATTR) * 72;
				double h = (Double)n.getAttributeValue(Node.HEIGHT_ATTR) * 72;
				double x1 = cx - w/2;
				double y1 = cy - h/2;
				
				float [] points = new float[4];
				points[0] = (float)x1;
				points[1] = (float)y1;
				points[2] = (float)(x1 + w);
				points[3] = (float)(y1 + h);
				
				transform.transform(points);
				String coords = String.format("%d,%d,%d,%d", (int)points[0], (int)points[1], (int)points[2], (int)points[3]);
				
				out.println("<area shape=\"rect\" coords=\""+coords+"\" href=\"#" + entry.getKey().id + "\"/>");
			}
			out.println("</map>");

			transform.dispose();

			out.println("<table>");
			if (showMarginals) out.println("<tr><th>ID</th><th>Name</th><th>Marginal</th><th>Study Count</th><th>Population Count</th>");
			else out.println("<tr><th>ID</th><th>Name</th><th>p-Value</th><th>p-Value (Adj)</th><th>Study Count</th><th>Population Count</th>");
			out.println("</tr>");

			AbstractGOTermProperties [] sortedProps = new AbstractGOTermProperties[result.getSize()];
			int i=0;
			for (AbstractGOTermProperties prop : result)
				sortedProps[i++] = prop;
//			for (int i=0;i<list.size();i++)
//				sortedProps[i] = list.get(i);
			Arrays.sort(sortedProps);

			for (AbstractGOTermProperties props : sortedProps)
			{
				String title;
				if (props.goTerm.getDefinition() != null)
					title = " title=\"" +props.goTerm.getDefinition() + "\"";
				else title = "";

				out.println("<tr" + title + ">");
				out.println("<td>");
				out.printf("<a name=\"%s\" href=\"http://www.ebi.ac.uk/ego/DisplayGoTerm?id=%s\">", props.goTerm.getID().id, props.goTerm.getIDAsString());
				out.println(props.goTerm.getIDAsString());
				out.println("</a>");
				out.println("</td>");
				
				out.println("<td>");
				out.println(props.goTerm.getName());
				out.println("</td>");
				
				if (!showMarginals)
				{
					out.println("<td>");
					out.printf("%g", props.p);
					out.println("</td>");
	
					out.println("<td>");
					out.printf("%g", props.p_adjusted);
					out.println("</td>");
				} else
				{
					out.println("<td>");
					out.printf("%g", ((Bayes2GOGOTermProperties)props).marg);
					out.println("</td>");
				}

				out.println("<td>");
				out.println(props.annotatedStudyGenes);
				out.println("</td>");

				out.println("<td>");
				out.println(props.annotatedPopulationGenes);
				out.println("</td>");
				
				out.println("</tr>");
			}
			out.println("</table>");
			
			out.println("<hr />");
			out.println("<center>");
			out.println("<img src=\""+pngFile.getName()+"\" usemap=\"#graph\"/>");
			out.println("</center>");
			
			out.println("</body>");
			out.println("</html>");
			
			out.flush();
			out.close();
		} catch (IOException e)
		{
			e.printStackTrace();
		} catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
