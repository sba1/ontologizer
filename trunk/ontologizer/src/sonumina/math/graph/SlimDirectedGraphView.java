package sonumina.math.graph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import sonumina.math.graph.AbstractGraph.IVisitor;

/**
 * Instances of this class represent a slim view of a graph. Most attributes can be
 * accessed directly.
 *
 * @author Sebastian Bauer
 *
 */
public class SlimDirectedGraphView<VertexType>
{
	/** An array of all terms */
	private Object [] vertices;

	/** Map specific terms to the index in the allTerms array */
	public HashMap<VertexType,Integer> vertex2Index;

	/** Contains all the ancestors of the terms */
	public int [][] vertexAncestors;

	/** Contains the parents of the terms */
	public int [][] vertexParents;

	/** Contains the children of the term */
	public int [][] vertexChildren;

	/** Contains the descendants of the (i.e., children, grand-children, etc.).*/
	public int [][] vertexDescendants;

	/**
	 * Constructs a slim view from a given directed graph.
	 *
	 * @param graph
	 */
	@SuppressWarnings("unchecked")
	public SlimDirectedGraphView(DirectedGraph<VertexType> graph)
	{
		int i;

		/* Vertices */
		vertices = new Object[graph.getNumberOfVertices()];
		vertex2Index = new HashMap<VertexType, Integer>();
		i = 0;
		for (VertexType t : graph)
		{
			vertices[i] = t;
			vertex2Index.put(t, i);
			i++;
		}

		/* Term parents stuff */
		vertexParents = new int[vertices.length][];
		for (i=0;i<vertices.length;i++)
		{
			VertexType v = (VertexType)vertices[i];

			/* FIXME: The name getAncestorNodes() is misleading */
			Iterator<VertexType> parentIter = graph.getAncestorNodes(v);
			vertexParents[i] = createIndicesFromIter(parentIter);
		}

		/* Term ancestor stuff */
		vertexAncestors = new int[vertices.length][];
		for (i=0;i<vertices.length;i++)
		{
			VertexType v = (VertexType)vertices[i];
			final List<VertexType> ancestors = new ArrayList<VertexType>(20);
			graph.bfs(v, true, new IVisitor<VertexType>() {
				public boolean visited(VertexType vertex)
				{
					ancestors.add(vertex);
					return true;
				};
			});
			vertexAncestors[i] = createIndicesFromIter(ancestors.iterator());
		}

		/* Term children stuff */
		vertexChildren = new int[vertices.length][];
		for (i=0;i<vertices.length;i++)
		{
			VertexType v = (VertexType)vertices[i];

			/* FIXME: The name getDescendantNodes() is misleading */
			Iterator<VertexType> childrenIter = graph.getDescendantNodes(v);
			vertexChildren[i] = createIndicesFromIter(childrenIter);
		}

		/* Term descendants stuff */
		vertexDescendants = new int[vertices.length][];
		for (i=0;i<vertices.length;i++)
		{
			VertexType v = (VertexType)vertices[i];
			final List<VertexType> descendants = new ArrayList<VertexType>(20);
			graph.bfs(v, false, new IVisitor<VertexType>() {
				public boolean visited(VertexType vertex)
				{
					descendants.add(vertex);
					return true;
				};
			});
			vertexDescendants[i] = createIndicesFromIter(descendants.iterator());

			/* Sort them, as we require this for isDescendant() */
			Arrays.sort(vertexDescendants);
		}
	}

	/**
	 * Creates an index array from the given vertex iterator.
	 *
	 * @param parentIter
	 * @return
	 */
	private int[] createIndicesFromIter(Iterator<VertexType> parentIter)
	{
		ArrayList<Integer> indicesList = new ArrayList<Integer>(10);

		while (parentIter.hasNext())
		{
			VertexType p = parentIter.next();
			Integer idx = vertex2Index.get(p);
			if (idx != null)
				indicesList.add(idx);
		}

		int [] indicesArray = new int[indicesList.size()];
		for (int i=0;i<indicesList.size();i++)
			indicesArray[i] = indicesList.get(i);

		return indicesArray;
	}

	/**
	 * Returns the number of vertices.
	 *
	 * @return
	 */
	public int getNumberOfVertices()
	{
		return vertices.length;
	}

	/**
	 * Returns the vertex at the given index.
	 *
	 * @param index
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public VertexType getVertex(int index)
	{
		return (VertexType)vertices[index];
	}

	/**
	 * Returns the index of the given vertex.
	 *
	 * @param v
	 * @return
	 */
	public int getVertexIndex(VertexType v)
	{
		return vertex2Index.get(v);
	}

	/**
	 * Returns the indices of the given vertices in a basic
	 * array.
	 *
	 * @param vertices
	 * @return
	 */
	public int [] getVertexIndices(Collection<VertexType> vertices)
	{
		int i;
		int [] vertexArray = new int[vertices.size()];

		i = 0;
		for (VertexType v : vertices)
			vertexArray[i++] = vertex2Index.get(v);

		return vertexArray;
	}

	/**
	 * Determines whether node i is a descendant of node j.
	 *
	 * @param i
	 * @param j
	 * @return
	 */
	public boolean isDescendant(int i, int j)
	{
		return Arrays.binarySearch(vertexDescendants[i],j) > 0;
	}
}
