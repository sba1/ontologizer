package sonumina.math.graph;

import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * An abstract class for graphs.
 *
 * @author Sebastian Bauer
 */
abstract public class AbstractGraph<VertexType>
{
	/**
	 * This interface is used as a callback mechanism by different search
	 * methods.
	 *
	 * @author Sebastian Bauer
	 */
	public static interface IVisitor<VertexType>
	{
		/**
		 * Called for every vertex visited by the algorithm.
		 *
		 * @param goTermID
		 *
		 * @return false if algorithm should be stopped (i.e. no further
		 *         calls to this method will be issued) otherwise true
		 */
		boolean visited(VertexType vertex);
	};

	/**
	 * This interface is used as a callback for the bfs and used to determine valid neighbors.
	 *
	 * @author Sebastian Bauer
	 */
	public static interface INeighbourGrabber<VertexType>
	{
		Iterator<VertexType> grabNeighbours(VertexType t);
	}

	/**
	 * Returns the vertices to which the in-going edges point to.
	 *
	 * @param vt
	 * @return
	 */
	public abstract Iterator<VertexType>getAncestorNodes(VertexType vt);

	/**
	 * Returns the vertices to which the outgoing edges point to.
	 *
	 * @param vt
	 * @return
	 */
	public abstract Iterator<VertexType>getDescendantNodes(VertexType vt);

	/**
	 * Performs a breadth-first search onto the graph starting at a given
	 * vertex. Vertices occurring in loops are visited only once.
	 *
	 * @param vertex defines the vertex to start with.
	 *
	 * @param againstFlow the bfs in done against the direction of the edges.
	 *
	 * @param visitor a object of a class implementing IVisitor. For every
	 *        vertex visited by the algorithm the visitor.visited() method is
	 *        called. Note that the method is also called for the vertex
	 *        represented by vertex.
	 *
	 * @see IVisitor
	 */
	public void bfs(VertexType vertex, boolean againstFlow, IVisitor<VertexType> visitor)
	{
		ArrayList<VertexType> initial = new ArrayList<VertexType>(1);
		initial.add(vertex);
		bfs(initial,againstFlow,visitor);
	}

	/**
	 * Performs a breadth-first search onto the graph starting at a given
	 * vertex. Vertices occurring in loops are visited only once.
	 *
	 * @param vertex defines the vertex to start with.
	 *
	 * @param grabber a object of a class implementing INeighbourGrabber which
	 *        returns the nodes which should be visited next.
	 *
	 * @param visitor a object of a class implementing IVisitor. For every
	 *        vertex visited by the algorithm the visitor.visited() method is
	 *        called. Note that the method is also called for the vertex
	 *        represented by vertex.
	 *
	 * @see IVisitor
	 */
	public void bfs(VertexType vertex,  INeighbourGrabber<VertexType> grabber, IVisitor<VertexType> visitor)
	{
		ArrayList<VertexType> initial = new ArrayList<VertexType>(1);
		initial.add(vertex);
		bfs(initial,grabber,visitor);
	}

	/**
	 * Performs a breadth-first search onto the graph starting at a given
	 * set of vertices. Vertices occurring in loops are visited only once.
	 *
	 * @param initial defines the set of vertices to start with.
	 *
	 * @param againstFlow the bfs in done against the direction of the edges.
	 *
	 * @param visitor a object of a class implementing IVisitor. For every
	 *        vertex visited by the algorithm the visitor.visited() method is
	 *        called. Note that the method is also called for the vertices
	 *        specified by initialSet (in arbitrary order)
	 *
	 * @see IVisitor
	 */
	public void bfs(Collection<VertexType> initial, final boolean againstFlow, IVisitor<VertexType> visitor)
	{
		bfs(initial,
				new INeighbourGrabber<VertexType>()
				{
					public Iterator<VertexType> grabNeighbours(VertexType t)
					{
						/* If bfs is done against flow neighbours can be found via the
						 * in-going edges otherwise via the outgoing edges */
						if (againstFlow) return getAncestorNodes(t);
						else return getDescendantNodes(t);
					}
				}, visitor);
	}

	/**
	 * Performs a breadth-first search onto the graph starting at a given
	 * set of vertices. Vertices occurring in loops are visited only once.
     *
	 * @param initial defines the set of vertices to start with.
	 *
	 * @param grabber a object of a class implementing INeighbourGrabber which
	 *        returns the nodes which should be visited next.
     *
	 * @param visitor a object of a class implementing IVisitor. For every
	 *        vertex visited by the algorithm the visitor.visited() method is
	 *        called. Note that the method is also called for the vertices
	 *        specified by initialSet (in arbitrary order)
	 */
	public void bfs(Collection<VertexType> initial, INeighbourGrabber<VertexType> grabber, IVisitor<VertexType> visitor)
	{
		HashSet<VertexType> visited = new HashSet<VertexType>();

		/* Add all nodes into the queue */
		LinkedList<VertexType> queue = new LinkedList<VertexType>();
		for (VertexType vertex  : initial)
		{
			queue.offer(vertex);
			visited.add(vertex);
			if (!visitor.visited(vertex))
				return;
		}

		while (!queue.isEmpty())
		{
			/* Remove head of the queue */
			VertexType head = queue.poll();

			/* Add not yet visited neighbours of old head to the queue
			 * and mark them as visited. */
			Iterator<VertexType> neighbours = grabber.grabNeighbours(head);

			while (neighbours.hasNext())
			{
				VertexType neighbour = neighbours.next();

				if (!visited.contains(neighbour))
				{
					queue.offer(neighbour);
					visited.add(neighbour);
					if (!visitor.visited(neighbour))
						return;
				}
			}
		}
	}

	/**
	 * The provider class for node and edge attributes that are used in the
	 * dot file.
	 *
	 * @author Sebastian Bauer
	 *
	 * @param <VertexType>
	 */
	public static class DotAttributesProvider<VertexType>
	{
		public String getDotNodeAttributes(VertexType vt)
		{
			return null;
		}

		public String getDotEdgeAttributes(VertexType src, VertexType dest)
		{
			return null;
		}
	}

	/**
	 * Returns the vertices is a iterable object.
	 *
	 * @return
	 */
	abstract public Iterable<VertexType> getVertices();

	/**
	 * Writes out the graph as a dot file.
	 *
	 * @param fos. For the output.
	 * @param provider. Provides the attributes.
	 */
	public void writeDOT(FileOutputStream fos, DotAttributesProvider<VertexType> provider)
	{
		writeDOT(fos,getVertices(),provider);
	}

	/**
	 * Writes out the graph as a dot file.
	 *
	 * @param fos. For the output.
	 * @param nodeSet. Defines the subset of nodes to be written out.
	 * @param provider. Provides the attributes.
	 */
	public void writeDOT(FileOutputStream fos, Iterable<VertexType> nodeSet, DotAttributesProvider<VertexType> provider)
	{
		PrintWriter out = new PrintWriter(fos);

		out.write("digraph G {nodesep=0.4;\n");

		/* Write out all nodes, call the given interface. Along the way, remember the indices. */
		HashMap<VertexType,Integer> v2idx = new HashMap<VertexType,Integer>();
		int i = 0;
		for (VertexType v : nodeSet)
		{
			String attributes = provider.getDotNodeAttributes(v);

			out.write(Integer.toString(i));
			if (attributes != null)
				out.write("[" + attributes + "]");
			out.write(";\n");

			v2idx.put(v,i++);
		}

		/* Now write out the edges. Write out only the edges which are linking nodes within the node set. */
		for (VertexType s : nodeSet)
		{
			Iterator<VertexType> ancest = getDescendantNodes(s);
			while (ancest.hasNext())
			{
				VertexType d = ancest.next();

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
