package ontologizer.ontology;

import java.io.PrintStream;
import java.util.Iterator;

import sonumina.math.graph.DirectedGraph;
import sonumina.math.graph.Edge;
import sonumina.math.graph.AbstractGraph.DotAttributesProvider;

public class OBOByteLineScannerGenerator
{
	/* Stanza types */
	private final byte [] TERM_KEYWORD = "term".getBytes();
	private final byte [] TYPEDEF_KEYWORD = "typedef".getBytes();

	/* Supported header types */
	private final byte [] FORMAT_VERSION_KEYWORD = "format-version".getBytes();
	private final byte [] DATE_KEYWORD = "date".getBytes();
	private final byte [] DATA_VERSION_KEYWORD = "data-version".getBytes();
	private final byte [] SUBSETDEF_KEYWORD = "subsetdef".getBytes();

	/* Supported term types */
	private final byte [] ID_KEYWORD = "id".getBytes();
	private final byte [] NAME_KEYWORD = "name".getBytes();
	private final byte [] IS_A_KEYWORD = "is_a".getBytes();
	private final byte [] RELATIONSHIP_KEYWORD = "relationship".getBytes();
	private final byte [] SYNONYM_KEYWORD = "synonym".getBytes();
	private final byte [] DEF_KEYWORD = "def".getBytes();
	private final byte [] NAMESPACE_KEYWORD = "namespace".getBytes();
	private final byte [] ALT_ID_KEYWORD = "alt_id".getBytes();
	private final byte [] EQUIVALENT_TO_KEYWORD = "equivalent_to".getBytes();
	private final byte [] IS_OBSOLETE_KEYWORD = "is_obsolete".getBytes();
	private final byte [] XREF_KEYWORD = "xref".getBytes();
	private final byte [] SUBSET_KEYWORD = "subset".getBytes();
	private final byte [] TRUE_KEYWORD = "true".getBytes();

	private final byte[][] termKeywords =
	{
		ID_KEYWORD,
		NAME_KEYWORD,
		IS_A_KEYWORD,
		RELATIONSHIP_KEYWORD,
		SYNONYM_KEYWORD,
		DEF_KEYWORD,
		NAMESPACE_KEYWORD,
		EQUIVALENT_TO_KEYWORD,
		IS_OBSOLETE_KEYWORD,
		XREF_KEYWORD,
		SUBSET_KEYWORD
	};

	class StringEdge extends Edge<Integer>
	{
		private String l;

		public StringEdge(Integer source, Integer dest, String l)
		{
			super(source, dest);

			this.l = l;
		}

		public String getL() {
			return l;
		}
	}

	/**
	 * Writes selection - action code to stdout.
	 *
	 * @param current
	 * @param tree
	 * @param depth
	 * @param pos
	 * @param name
	 */
	private void writeCode(Integer current, DirectedGraph<Integer> tree, int depth, int pos, String name)
	{
		boolean first = true;
		Iterator<Edge<Integer>> iter = tree.getOutEdges(current);
		while (iter.hasNext())
		{
			StringEdge se = (StringEdge)iter.next();

			for (int i=0;i<depth;i++)
				System.out.print("\t");

			if (!first) System.out.print("else ");

			System.out.print("if (");

			if (depth != 0)
			{
				for (int i=0;i<se.l.length();i++)
					System.out.print(String.format("toLower(buf[keyStart + %d]) == %d && ",pos+i-1,se.l.getBytes()[i]));
				System.out.println(String.format("true) /* %s */",se.l));
			} else
			{
				System.out.println(String.format("keyLen==%d)",se.l.getBytes()[0]));
			}

			for (int i=0;i<depth;i++)
				System.out.print("\t");
			System.out.println("{");
			writeCode(se.getDest(),tree,depth+1,pos + se.l.length(),name + se.l);

			for (int i=0;i<depth;i++)
				System.out.print("\t");
			System.out.println("}");

			first = false;
		}
		if (first)
		{
			/* We are at a leaf */
			for (int i=0;i<depth;i++)
				System.out.print("\t");
			System.out.println(String.format("parse_%s(buf, valueStart, valueLen);",name.substring(1)));
		}
	}


	/**
	 * Try to collapse the given tree at the given node.
	 *
	 * @param current
	 * @param tree
	 */
	private void collapse(Integer current, DirectedGraph<Integer> tree)
	{
		int currentOutDegree = tree.getOutDegree(current);
		if (currentOutDegree > 1)
		{
			Iterator<Edge<Integer>> iter = tree.getOutEdges(current);
			while (iter.hasNext())
				collapse(iter.next().getDest(),tree);
		} else if (currentOutDegree == 1)
		{
			StringEdge e = (StringEdge)tree.getOutEdges(current).next();
			Integer next = e.getDest();
			int nextOutDegree = tree.getOutDegree(next);
			if (nextOutDegree == 1)
			{
				StringEdge ne = (StringEdge)tree.getOutEdges(next).next();
				Integer nextnext = ne.getDest();
				e.l += ne.l;

				/* Move all out edges of next next to next */
				Iterator<Edge<Integer>> nextnextIter = tree.getOutEdges(nextnext);
				while (nextnextIter.hasNext())
				{
					StringEdge se = (StringEdge)nextnextIter.next();
					tree.addEdge(new StringEdge(next,se.getDest(),se.l));
				}

				/* Finally, remove next next */
				tree.removeVertex(nextnext);

				collapse(current,tree);
			} else
			{
				collapse(next,tree);
			}
		}
	}


	/**
	 * Try to identify the neighbor of the current node.
	 *
	 * @param tree
	 * @param current
	 * @param c
	 * @return the neighbor or null if it is non-existent.
	 */
	private Integer followEdge(final DirectedGraph<Integer> tree, Integer current, byte c)
	{
		Integer next = null;
		Iterator<Edge<Integer>> iter = tree.getOutEdges(current);
		while (iter.hasNext())
		{
			StringEdge se = (StringEdge)iter.next();
			if (se.l.getBytes()[0] == c)
			{
				next = se.getDest();
				break;
			}
		}
		return next;
	}

	private int currentVertexIndex = 1;

	/**
	 * Insert the a new edge to the given byte into the tree if it is
	 * not already present.
	 *
	 * @param tree
	 * @param current
	 * @param c
	 * @return the node to which the edge points to.
	 */
	private Integer insertEdge(final DirectedGraph<Integer> tree, Integer current, byte c)
	{
		Integer next = followEdge(tree, current, c);
		if (next == null)
		{
			next = new Integer(currentVertexIndex++);
			tree.addVertex(next);
			StringEdge se = new StringEdge(current, next, ((char)c)+"");
			tree.addEdge(se);
		}
		return next;
	}


	/**
	 * Generate Java code for if clauses.
	 */
	private void generateKeywordIfClauses()
	{
		final DirectedGraph<Integer> tree = new DirectedGraph<Integer>();

		Integer root = new Integer(0);
		tree.addVertex(root);

		for (int i=0;i<termKeywords.length;i++)
		{
			byte [] keyword = termKeywords[i];

			/* First level is the length of the keyword */
			Integer current = insertEdge(tree, root, (byte)keyword.length);

			for (int j=0;j<keyword.length;j++)
			{
				byte c = keyword[j];
				current = insertEdge(tree, current, c);
			}
		}

		/* Collapse */
		collapse(root,tree);

		writeCode(root,tree,0,0,"");

		tree.writeDOT(new PrintStream(System.out), new DotAttributesProvider<Integer>()
				{
					@Override
					public String getDotEdgeAttributes(Integer src, Integer dest) {

						return "label=\"" + ((StringEdge)tree.getEdge(src, dest)).getL() +  "\"";
					}
				});

		System.exit(-1);
	}
}
