package ontologizer.dotwriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import ontologizer.BuildInfo;
import ontologizer.go.Ontology;
import ontologizer.go.Term;
import ontologizer.go.TermID;
import ontologizer.go.TermRelation;
import sonumina.math.graph.AbstractGraph.DotAttributesProvider;
import sonumina.math.graph.DOTWriter;

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
	public static void writeDOT(Ontology graph, File file, TermID rootTerm, Set<TermID> terms, IDotAttributesProvider provider)
	{
		writeDOT(graph, file, rootTerm, terms, provider, "nodesep=0.4;", false, false, null);
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
	 * @param ignoreTerms
	 */
	public static void writeDOT(final Ontology graph, File file, TermID rootTerm, Set<TermID> terms, final IDotAttributesProvider provider, final String graphAttrs, final boolean reverseDirection, final boolean edgeLabels, Set<TermID> ignoreTerms)
	{
		/* Collect terms starting from the terms upto the root term and place them into nodeSet */
		HashSet<Term> nodeSet = new HashSet<Term>();
		for (TermID term : terms)
		{
			if (!graph.termExists(term))
				throw new IllegalArgumentException("Requested term " + term.toString() + " couldn't be found in the graph");

			if (!nodeSet.contains(term))
			{
				for (TermID it : graph.getTermsOfInducedGraph(rootTerm,term))
					nodeSet.add(graph.getTerm(it));
			}
		}

		if (ignoreTerms != null)
		{
			for (TermID it : ignoreTerms)
				nodeSet.remove(graph.getTerm(it));
		}

		/* We now have a list of nodes which can be placed into the output */
		try
		{
			DOTWriter.write(graph.getGraph(),new FileOutputStream(file), nodeSet, new DotAttributesProvider<Term>()
					{
						/* Note that the default direction is assumed to be the opposite direction */
						private String direction = reverseDirection?"":"dir=\"back\"";

						@Override
						public String getDotNodeAttributes(Term vt) { return provider.getDotNodeAttributes(vt.getID());	}

						@Override
						public String getDotGraphAttributes() { return graphAttrs; }

						@Override
						public String getDotHeader(){ return "/* Generated with Ontologizer " + BuildInfo.getVersion() + " build " + BuildInfo.getBuildString() + " */"; }

						@Override
						public String getDotEdgeAttributes(Term src, Term dest)
						{
							String color;
							String relationName;
							String label;

							TermRelation rel = graph.getDirectRelation(src.getID(), dest.getID());

							switch (rel)
							{
								case	IS_A: relationName = "is a"; break;
								case	PART_OF_A: relationName = "is part of"; break;
								case	REGULATES: relationName = "regulates"; break;
								case	POSITIVELY_REGULATES: relationName = "positively regulates"; break;
								case	NEGATIVELY_REGULATES: relationName = "negatively regulates"; break;
								default: relationName = "";
							}

							switch (rel)
							{
								case	IS_A: color = "black"; break;
								case	PART_OF_A: color = "blue"; break;
								case	REGULATES:  /* Falls through */
								case	POSITIVELY_REGULATES:  /* Falls through */
								case	NEGATIVELY_REGULATES: color ="green"; break;
								default: color = "black"; break;
							}

							if (edgeLabels)
							{
								label = provider.getDotEdgeAttributes(src.getID(), dest.getID());
								if (label == null)
									label = "label=\"" + relationName + "\"";
							} else label = null;

							String tooltip = "tooltip=\"" + dest.getName() + " " + relationName + " " + src.getName() + "\"";
							return "color=" + color + "," + direction + "," + tooltip + (label!=null?(","+label):"");
						}
					});
		} catch (IOException e)
		{
			logger.severe("Unable to create dot file: " + e.getLocalizedMessage());
			e.printStackTrace();
		}
	}

}
