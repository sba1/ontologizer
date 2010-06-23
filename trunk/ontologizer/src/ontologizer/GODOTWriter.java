package ontologizer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import ontologizer.go.GOGraph;
import ontologizer.go.ParentTermID;
import ontologizer.go.TermID;
import ontologizer.go.TermRelation;

public class GODOTWriter
{
	private static Logger logger = Logger.getLogger(GODOTWriter.class.getCanonicalName());

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
	 * @param rootTerm
	 *          defines the first term of the sub graph which should
	 *          be considered.
	 *
	 * @param terms
	 * 			defines which terms should be included within the
	 *          graphs.
	 * @param provider
	 *          should provide for every property an appropiate id.
	 */
	public static void writeDOT(GOGraph graph, File file, TermID rootTerm, Set<TermID> terms, IDotAttributesProvider provider)
	{
		writeDOT(graph, file, rootTerm, terms, provider, "nodesep=0.4;", true, false);
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
	 * @param rootTerm
	 *          defines the first term of the sub graph which should
	 *          be considered.
	 *
	 * @param terms
	 * 			defines which terms should be included within the
	 *          graphs.
	 * @param provider
	 *          should provide for every property an appropiate id.
	 * @param reverseDirection spec
	 * @param edgeLabels
	 */
	public static void writeDOT(GOGraph graph, File file, TermID rootTerm, Set<TermID> terms, IDotAttributesProvider provider, String graphAttrs, boolean reverseDirection, boolean edgeLabels)
	{
		/* Collect terms starting from the terms upto the root term and place them into nodeSet */
		HashSet<TermID> nodeSet = new HashSet<TermID>();
		for (TermID term : terms)
		{
			if (!nodeSet.contains(term))
				nodeSet.addAll(graph.getTermsOfInducedGraph(rootTerm,term));
		}

		/* We now have a list of nodes which can be placed into the output */
		try
		{
			FileWriter out = new FileWriter(file);

			out.write("digraph G { " + (graphAttrs!=null?graphAttrs:""));
			out.write("\n");
			/* Title */
//			out.write("title[label=\"" + file.getName() + "\",shape=plaintext]\n");

			/* Write out all nodes, call the given interface */
			for (TermID id : nodeSet)
			{
				String attributes = provider.getDotNodeAttributes(id);
				out.write(id.id + "[" + attributes + "];\n");
			}

			String direction;

			if (!reverseDirection)
				direction = " dir=\"back\"";
			else
				direction = "";

			/* Write out the edges */
			for (TermID destID : nodeSet)
			{
				for (ParentTermID source : graph.getTermsAncestorsWithRelation(destID))
				{
					if (nodeSet.contains(source.termid))
					{
						String sourceName = graph.getGOTerm(source.termid).getName();
						String destName = graph.getGOTerm(destID).getName();

						out.write(source.termid.id + " -> " + destID.id);

						if (source.relation == TermRelation.PART_OF_A)
							out.write("[color=blue, tooltip=\"" + destName + " is part of " + sourceName + "\"" + direction + (edgeLabels?"label=\"part of\"":"") + " ]");
						else if (source.relation == TermRelation.REGULATES || source.relation == TermRelation.POSITIVELY_REGULATES || source.relation == TermRelation.NEGATIVELY_REGULATES)
							out.write("[color=green, tooltip=\"" + destName + " regulates " + sourceName + "\"" + direction + (edgeLabels?"label=\"regulates\"":"") + " ]");
						else if (source.relation == TermRelation.IS_A)
							out.write("[color=black, tooltip=\"" + destName + " is a " + sourceName + "\"" + direction + (edgeLabels?"label=\"is a\"":"") + "]");
						else
							out.write("[" + direction + "]");

						out.write(";\n");
					}
				}
			}

			out.write("}\n");

			out.flush();
			out.close();
		} catch (IOException e)
		{
			logger.severe("Unable to create dot file: " + e.getLocalizedMessage());
			e.printStackTrace();
		}
	}

}
