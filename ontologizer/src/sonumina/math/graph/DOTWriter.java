package sonumina.math.graph;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;

import sonumina.math.graph.AbstractGraph.DotAttributesProvider;

public class DOTWriter
{
	/**
	 * Writes out a graph as a dot file.
	 *
	 * @param g the graph that should be written out
	 * @param os. For the output.
	 * @param nodeSet. Defines the subset of nodes to be written out.
	 * @param provider. Provides the attributes.
	 * @param nodeSep. The space between nodes of the same rank.
	 * @param rankSep. The space between two nodes of subsequent ranks.
	 */
	public static <V> void write(AbstractGraph<V> g, OutputStream os, Iterable<V> nodeSet, final DotAttributesProvider<V> provider, final double nodeSep, final double rankSep)
	{
		DotAttributesProvider<V> newProvider = new DotAttributesProvider<V>()
		{
			@Override
			public String getDotGraphAttributes()
			{
				StringBuilder attrs = new StringBuilder();
				attrs.append(String.format(Locale.US, "nodesep=%f; ranksep=%f;", nodeSep, rankSep));

				if (provider.getDotGraphAttributes() != null)
					attrs.append(provider.getDotGraphAttributes());
				return attrs.toString();
			}

			@Override
			public String getDotNodeAttributes(V vt)
			{
				return provider.getDotNodeAttributes(vt);
			}

			@Override
			public String getDotEdgeAttributes(V src, V dest)
			{
				return provider.getDotEdgeAttributes(src, dest);
			}

			@Override
			public String getDotHeader()
			{
				return provider.getDotHeader();
			}
		};
		DOTWriter.write(g,os,nodeSet,newProvider);
	}

	/**
	 * Writes out a graph as a dot file.
	 *
	 * @param g the graph that should be written out
	 * @param fos. For the output.
	 * @param nodeSet. Defines the subset of nodes to be written out.
	 * @param provider. Provides the attributes.
	 */
	public static <V> void write(AbstractGraph<V> g, OutputStream os, Iterable<V> nodeSet, DotAttributesProvider<V> provider)
	{
		PrintWriter out = new PrintWriter(os);
		String graphHeader = provider.getDotHeader();
		String graphAttributes = provider.getDotGraphAttributes();

		if (graphHeader != null)
		{
			out.append(graphHeader);
			out.append("\n");
		}

		out.append("digraph G {");
		if (graphAttributes != null)
		{
			out.append(graphAttributes);
			out.append('\n');
		}

		/* Write out all nodes, call the given interface. Along the way, remember the indices. */
		HashMap<V,Integer> v2idx = new HashMap<V,Integer>();
		int i = 0;
		for (V v : nodeSet)
		{
			String attributes = provider.getDotNodeAttributes(v);

			out.write(Integer.toString(i));
			if (attributes != null)
				out.write("[" + attributes + "]");
			out.write(";\n");

			v2idx.put(v,i++);
		}

		/* Now write out the edges. Write out only the edges which are linking nodes within the node set. */
		for (V s : nodeSet)
		{
			Iterator<V> ancest = g.getChildNodes(s);
			while (ancest.hasNext())
			{
				V d = ancest.next();

				if (v2idx.containsKey(d))
				{
					out.write(v2idx.get(s) + " -> " + v2idx.get(d));

					String attributes = provider.getDotEdgeAttributes(s,d);
					if (attributes != null)
						out.write("[" + attributes + "]");

					out.println(";\n");
				}
			}
		}

		out.write("}\n");
		out.flush();
		out.close();
	}


}
