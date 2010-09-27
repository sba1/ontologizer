package ontologizer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import ontologizer.calculation.AbstractGOTermProperties;
import ontologizer.calculation.EnrichedGOTermsResult;
import ontologizer.go.Ontology;
import ontologizer.go.Term;
import ontologizer.go.TermID;
import ontologizer.go.Ontology.IVisitingGOVertex;

/**
 * A class writing dot files for the comparision of different calculation
 * methods. 
 * 
 * @author Steffen Grossmann, Sebastian Bauer
 *
 */
public class DOTDumper
{
	private class MultResultNode
	{
		public Term goTerm;
		public boolean significant[][];
		
		public String dotString()
		{
			StringBuilder attributes = new StringBuilder();
			attributes.append("label=<");
		
			/* open table */
			attributes.append("<TABLE BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\">");

			/* first row */
			attributes.append("<TR><TD>");
			attributes.append(goTerm.getIDAsString());
			attributes.append("<BR/>");
			attributes.append(goTerm.getName());
			attributes.append("</TD></TR>");

			/* second row (matrix) */
			attributes.append("<TR><TD><TABLE BORDER=\"0\" CELLBORDER=\"0\" CELLSPACING=\"2\">");
			for (int r=0; r<significant.length; r++)
			{
				attributes.append("<TR>");
				for (int c = 0; c < significant[r].length; c++)
				{
					if (significant[r][c]) attributes.append("<TD HEIGHT=\"8\" BGCOLOR=\"" + someColors[c + r*significant[r].length] + "\"></TD>");
					else attributes.append("<TD></TD>");
				}
				attributes.append("</TR>");
			}
			attributes.append("</TABLE></TD></TR>");

			/* close table */
			attributes.append("</TABLE>");
			attributes.append(">");
			
			return goTerm.getID().id + "[" + attributes + "];";
		}

		/**
		 * @param goid the GO Identifies
		 * @param name the descriptive name
		 * @param rows the number rows
		 * @param cols the number of columns
		 */
		public MultResultNode(Term term, int rows, int cols)
		{
			goTerm = term;
			significant = new boolean[rows][cols];
		}
	}

	private Ontology graph;
	
	/* TODO: Add more colors */
	private final String someColors[] = new String[] {"#888888", "#222222", "green", "blue", "violet"};
	
	/**
	 * @param terms
	 * @param graph
	 */
	public DOTDumper(Ontology graph)
	{
		this.graph = graph;
	}
	
	/**
	 * Note that the studyResList should result from the same study! (only different
	 * calculation methods and multiple test corrections are allowed)
	 * 
	 * @param studResList
	 * @param file
	 * @param alpha
	 */
	public void Dump2DOT(StudySetResultList studResList, File file, double alpha)
	{
		/* At first, determine different combinations of calculation and correction methods */
		int c = 0, r = 0;
		HashMap<String,Integer> correction2Column = new HashMap<String,Integer>();
		HashMap<String,Integer> method2Row = new HashMap<String,Integer>();
		ArrayList<String> correctionList = new ArrayList<String>();
		ArrayList<String> methodList = new ArrayList<String>();

		for (EnrichedGOTermsResult studySetResult : studResList)
		{
			String correctionName = studySetResult.getCorrectionName();
			String calculationMethod = studySetResult.getCalculationName();

			if (!correction2Column.containsKey(correctionName))
			{
				correctionList.add(correctionName);
				correction2Column.put(correctionName,c++);
			}

			if (!method2Row.containsKey(calculationMethod))
			{
				methodList.add(calculationMethod);
				method2Row.put(calculationMethod,r++);
			}
		}

		int numRows = r;
		int numCols = c;

		/* Now build the multi node map */
		HashMap<Term,MultResultNode> nodeMap = new HashMap<Term,MultResultNode>();
		for (EnrichedGOTermsResult studySetResult : studResList)
		{
			String correctionName = studySetResult.getCorrectionName();
			String calculationMethod = studySetResult.getCalculationName();

			/* Find out the proper position within the matrix */
			r = method2Row.get(calculationMethod);
			c = correction2Column.get(correctionName);

			processStudySetResult(nodeMap, studySetResult, alpha, numRows, numCols, r, c);
		}

		/* Finally write out the dot file */
		try
		{
			FileWriter out = new FileWriter(file);

			System.out.println("Writing dot file to " + file.getCanonicalPath());

			/* Header */
			out.write("digraph G {nodesep=0.4; node [shape=plaintext]\n");
			
			/* Title */
			out.write("title[label=\"" + file.getName() + "\"]\n");

			/* Legend */
			StringBuffer buf = new StringBuffer();
			buf.append("legend[label=<");
			buf.append("<TABLE BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\">");
			buf.append("<TR><TD COLSPAN=\"2\" ROWSPAN=\"2\"></TD><TD COLSPAN=\"2\">Test Correction</TD></TR>");
			buf.append("<TR>");
			
			for (c=0;c<numCols;c++)
			{
				buf.append("<TD>");
				buf.append(correctionList.get(c));
				buf.append("</TD>");
			}
			buf.append("</TR>");
	
			for (r=0;r<numRows;r++)
			{
				buf.append("<TR>");
				if (r==0) buf.append("<TD ROWSPAN=\""+ numRows+ "\">Calculation<BR />method</TD>");
				buf.append("<TD>");
				buf.append(methodList.get(r));
				buf.append("</TD>");
				
				/* For every column in the row place an cell with an unique color */
				for (c=0;c<numCols;c++)
				{
					buf.append("<TD WIDTH=\"150\" BGCOLOR=\""+someColors[c + r*numCols]+"\"></TD>");  
				}
				buf.append("</TR>");
			}

			buf.append("</TABLE>>]\n");
			out.write(buf.toString());

			/* The nodes */
			for (MultResultNode node : nodeMap.values())
			{
				out.write(node.dotString());
				out.write("\n");
			}
			
			/* The edges */
			for (MultResultNode node : nodeMap.values())
			{
				TermID destID = node.goTerm.getID();

				for (TermID sourceID : graph.getTermParents(destID))
				{
					Term source = graph.getTerm(sourceID);
					if (nodeMap.containsKey(source))
						out.write(sourceID.id + " -> " + destID.id + ";\n");
				}
			}

			out.write("}\n");
			out.close();
		} catch (IOException io)
		{
			io.printStackTrace();
		}
	}

	public class MultResultNodeVisitor implements IVisitingGOVertex
	{
		public HashMap<Term, MultResultNode> nodeMap;
		public Ontology graph;
		public int rows, cols;

		/**
		 * @param terms
		 * @param map
		 * @param results
		 */
		public MultResultNodeVisitor(Ontology goGraph, HashMap<Term, MultResultNode> map, int nRows, int nCols)
		{
			graph = goGraph;
			nodeMap = map;
			rows = nRows;
			cols = nCols;
		}
		
		public boolean visited(Term term)
		{
			if (term != null && !nodeMap.containsKey(term))
			{
//				TermID rootTerm = new TermID(8150);
//				if (rootTerm.equals(goTermID) || (!graph.isRootGOTermID(goTermID) && graph.existsPath(rootTerm,goTermID)))
				{
					MultResultNode newNode = new MultResultNode(term,rows, cols);
					nodeMap.put(term,newNode);
				}
			}
			return true;
		}
	}

	private void processStudySetResult(
			HashMap<Term, MultResultNode> nodeMap,
			EnrichedGOTermsResult studRes,
			double alpha, int rows, int cols, int row, int col)
	{
		for (AbstractGOTermProperties props : studRes)
		{
			if (props.p_adjusted < alpha)
			{
				if (nodeMap.containsKey(props.goTerm))
				{
					/* node existing just make sure that significance entry is set properly */
					nodeMap.get(props.goTerm).significant[row][col] = true;
				} else {
					/* add node and potential parents */
					MultResultNodeVisitor visitor =
						new MultResultNodeVisitor(graph, nodeMap, rows, cols);

					graph.walkToSource(props.goTerm.getID(),visitor);
					
					/* set significance properly, but we have to check before
					 * if the term is really contained within the map (because
					 * it can be filtered out, e.g. if it is not part of the
					 * requested subgraph) */
					if (nodeMap.containsKey(props.goTerm))
					{
						nodeMap.get(props.goTerm).significant[row][col] = true;
					}
				}
			}
		}
	}
}
