package ontologizer.calculation;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.logging.Logger;

import ontologizer.IDotNodeAttributesProvider;
import ontologizer.StudySet;
import ontologizer.association.AssociationContainer;
import ontologizer.go.GOGraph;
import ontologizer.go.OBOParser;
import ontologizer.go.Term;
import ontologizer.go.TermContainer;
import ontologizer.go.TermID;
import ontologizer.go.GOGraph.IVisitingGOVertex;
import ontologizer.gui.swt.GlobalPreferences;
import ontologizer.gui.swt.Ontologizer;
import ontologizer.gui.swt.support.GraphPaint;
import ontologizer.util.Util;

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

/**
 * This class is a container for all the results dervived
 * from a term enrichment calculation.
 *
 * @author Sebastian Bauer
 */
public class EnrichedGOTermsResult extends AbstractGOTermsResult
{
	private static Logger logger = Logger.getLogger(EnrichedGOTermsResult.class.getCanonicalName());

	private int populationGeneCount;
	private int studyGeneCount;
	private StudySet studySet;
	
	private String calculationName;
	private String correctionName;

	/**
	 * 
	 * @param studySet
	 *            the study set where this result should belong to.
	 * @param populationGeneCount
	 *            number of genes of the populations (FIXME: This infact is
	 *            redundant)
	 */
	public EnrichedGOTermsResult(GOGraph go, AssociationContainer associations, StudySet studySet, int populationGeneCount)
	{
		super(go, associations);

		this.populationGeneCount = populationGeneCount;
		this.studySet = studySet;
		this.studyGeneCount = studySet.getGeneCount();
	}

	/**
	 * Returns the name of the calculation method used for this
	 * result.
	 * 
	 * @return
	 */
	public String getCalculationName()
	{
		return calculationName;
	}

	/**
	 * Sets the name of the calculation method used for this
	 * result.
	 * 
	 * @param calculationName
	 */
	public void setCalculationName(String calculationName)
	{
		this.calculationName = calculationName;
	}

	/**
	 * Returns the name of the multiple test correction used
	 * for this result.
	 * 
	 * @return
	 */
	public String getCorrectionName()
	{
		return correctionName;
	}

	public void setCorrectionName(String correctionName)
	{
		this.correctionName = correctionName;
	}

	/**
	 * 
	 * @param file
	 */
	public void writeTable(File file)
	{
		if (list.isEmpty())
			return;

		try
		{
			logger.info("Writing to \"" + file.getCanonicalPath() + "\".");

			PrintWriter out = new PrintWriter(file);

			/* Write out the table header */
			AbstractGOTermProperties first = list.get(0);

			out.write(first.propHeaderToString());

			/* Write out table contents */
			for (AbstractGOTermProperties props : this)
			{
				out.println(props.propLineToString(populationGeneCount,
						studyGeneCount));
			}

			out.flush();
			out.close();
			
			logger.info("\"" + file.getCanonicalPath() + "\"" + " successfully written.");
		} catch (IOException e)
		{
			Ontologizer.logException(e);
		}
	}

	class AllMapData
	{
		public double minX;
		public double minY;
		
		public HashMap<TermID,Node> term2node = new HashMap<TermID, Node>();
	}

	private void buildNodeRectangles(Element e, AllMapData data)
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
	public void writeHTML(File htmlFile, File dotFile)
	{
		try
		{
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
			
			out.println("<tr><th>ID</th><th>Name</th><th>p-Value</th><th>p-Value (Adj)</th><th>Study Count</th><th>Population Count</th>");
			out.println("</tr>");

			AbstractGOTermProperties [] sortedProps = new AbstractGOTermProperties[list.size()];
			for (int i=0;i<list.size();i++)
				sortedProps[i] = list.get(i);
			Arrays.sort(sortedProps);

			for (AbstractGOTermProperties props : sortedProps)
			{
				String title;
				if (props.goTerm.getDefinition() != null)
					title = " title=\"" +props.goTerm.getDefinition() + "\"";
				else title = "";

				out.printf("<tr" + title + ">");
				out.println("<td>");
				out.printf("<a name=\"%s\" href=\"http://www.ebi.ac.uk/ego/DisplayGoTerm?id=%s\">", props.goTerm.getID().id, props.goTerm.getIDAsString());
				out.println(props.goTerm.getIDAsString());
				out.println("</a>");
				out.println("</td>");
				
				out.println("<td>");
				out.println(props.goTerm.getName());
				out.println("</td>");
				
				out.println("<td>");
				out.printf("%.4f", props.p);
				out.println("</td>");

				out.println("<td>");
				out.printf("%.4f", props.p_adjusted);
				out.println("</td>");

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

	/**
	 * Returns the set of terms for which the all-subset minimal p-value is
	 * below the given cutoff. Those are the "good" terms.
	 * 
	 * @param pvalCutoff
	 *            the cutoff to use
	 * @return the list of good terms
	 */
	public HashSet<TermID> getGoodTerms(double pvalCutoff)
	{
		HashSet<TermID> goodTerms = new HashSet<TermID>();

		for (AbstractGOTermProperties goProp : this)
		{
			TermID curTerm = goProp.goTerm.getID();
			if (goProp.p_min < pvalCutoff)
				goodTerms.add(curTerm);
		}

		return goodTerms;
	}

	/**
	 * Writes out a basic dot file which can be used within graphviz. All terms
	 * of the terms parameter are included in the graph if they are within the
	 * sub graph originating at the rootTerm. In other words, all nodes
	 * representing the specified terms up to the given rootTerm node are
	 * included. 
	 * 
	 * @param goTerms
	 * @param graph
	 * @param file
	 * 			defines the file in which the output is written to.
	 * @param alpha
	 * 			defines the significance level. Used to colorize
	 *          nodes of significant terms.
	 * @param counts
	 *          if true, the nodes will be labeled with the gene
	 *          counts.
	 * @param rootTerm
	 *          defines the first term of the sub graph which should
	 *          be considered.
	 *
	 * @param terms
	 * 			defines which terms should be included within the
	 *          graphs.
	 */
	public void writeDOT(TermContainer goTerms, final GOGraph graph, File file,
			final double alpha, final boolean counts, TermID rootTerm, HashSet<TermID> terms)
	{
		/* Build the props Array and count the number significant p values */
		int i = 0;
		int scount = 0;
		AbstractGOTermProperties propArray[] = new AbstractGOTermProperties[list.size()];
		for (AbstractGOTermProperties props : this)
		{
			propArray[i++] = props;
			if (props.p_adjusted < alpha)
				scount++;
		}
		Arrays.sort(propArray);

		final int significants_count = scount;

		/*
		 * Traverse through the sorted props remembering the GO Terms index
		 * in a hash
		 */
		final HashMap<Term, Integer> goTermRank = new HashMap<Term, Integer>();
		for (i = 0; i < propArray.length; i++)
			goTermRank.put(propArray[i].goTerm, i);

		writeDOT(graph, file, rootTerm, terms, new IDotNodeAttributesProvider()
		{
			public String getDotNodeAttributes(TermID id)
			{
				StringBuilder attributes = new StringBuilder();
				attributes.append("label=\"");

				if (graph.isRootGOTermID(id))
				{
					attributes.append("Gene Ontology");
				} else
				{
					attributes.append(id.toString());
					attributes.append("\\n");
					
					String label = graph.getGOTerm(id).getName();
					if (GlobalPreferences.getWrapColumn() != -1)
						label = Util.wrapLine(label,"\\n",GlobalPreferences.getWrapColumn());
					
					attributes.append(label);
				}

				AbstractGOTermProperties prop = getGOTermProperties(id);
				if (prop != null && counts)
				{
					attributes.append(String.format("\\n%d/%d, %d/%d",
							prop.annotatedPopulationGenes, populationGeneCount,
							prop.annotatedStudyGenes, studyGeneCount));
				}

				/* TODO: tip attribute */
				attributes.append("\"");
				
				if (prop != null && prop.p_adjusted < alpha)
				{
					/* A term is "extremal" if it is significant and no one of its children is significant */
					boolean isExtremal;

					class ExtremalVisitor implements IVisitingGOVertex
					{
						public boolean isExtremal = true;

						public void visiting(TermID goTermID)
						{
							AbstractGOTermProperties subtermProp = getGOTermProperties(goTermID);
							if (subtermProp != null && subtermProp.p_adjusted < alpha)
								isExtremal = false;
						}
					}

					ExtremalVisitor visitor = new ExtremalVisitor();
					graph.walkToSinks(graph.getTermsDescendants(id), visitor);
					isExtremal = visitor.isExtremal;

					float hue, saturation, brightness;

					/*
					 * Use the rank to determine the saturation We want that
					 * more significant nodes have more saturation, but we avoid
					 * having significant nodes with too less saturation (at
					 * least 0.2)
					 */
					int rank = goTermRank.get(prop.goTerm);
					assert (rank < significants_count);
					saturation = 1.0f - (((float) rank + 1) / significants_count) * 0.8f;

					/* Always full brightness */
					brightness = 1.0f;

					/* Hue depends on namespace */
					switch (prop.goTerm.getNamespace())
					{
						case BIOLOGICAL_PROCESS: hue = 120.f / 360; break;
						case MOLECULAR_FUNCTION: hue = 60.f / 360; break;
						case CELLULAR_COMPONENT: hue = 300.f / 360; 	break;
						default:
							hue = 0.f;
							saturation = 0.f;
							break;
					}

					String style = "filled,gradientfill";
					if (isExtremal) style += ",setlinewidth(3)";
					String fillcolor = String.format(Locale.US, "%f,%f,%f", hue, saturation, brightness);
					attributes.append(",style=\""+ style + "\",color=\"white\",fillcolor=\"" + fillcolor + "\"");
				}
				return attributes.toString();
			}
		});
	}


	/**
	 * Writes out a basic dot file which can be used within graphviz.
	 * 
	 * @param goTerms
	 * @param graph
	 * @param file
	 * @param alpha
	 * @param counts
	 * @param rootTerm
	 */
	public void writeDOT(TermContainer goTerms, GOGraph graph, File file, double alpha, boolean counts, TermID rootTerm)
	{
		HashSet<TermID> nodes = new HashSet<TermID>();
		
		for(AbstractGOTermProperties props : this)
		{
			if (props.p_adjusted < alpha)
				nodes.add(props.goTerm.getID());
		}
		
		writeDOT(goTerms,graph,file,alpha,counts,rootTerm,nodes);
	}
	
	public Iterator<AbstractGOTermProperties> iterator()
	{
		return list.iterator();
	}

	/**
	 * Returns the studyset where these results are belonging to.
	 * 
	 * @return the study set.
	 */
	public StudySet getStudySet()
	{
		return studySet;
	}

	public int getPopulationGeneCount()
	{
		return populationGeneCount;
	}

	public int getStudyGeneCount()
	{
		return studyGeneCount;
	}

}
