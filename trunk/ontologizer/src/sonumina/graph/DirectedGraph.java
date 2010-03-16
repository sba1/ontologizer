package sonumina.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.Map.Entry;

class VertexAttributes<VertexType>
{
	/** All edges where the vertex is appearing as dest */
	public ArrayList<Edge<VertexType>> inEdges = new ArrayList<Edge<VertexType>>();

	/** Array of ancestors, build on demand and cached for quick access */
	public Object [] ancestorsArray;

	/** All edges where the vertex is appearing as source */
	public ArrayList<Edge<VertexType>> outEdges = new ArrayList<Edge<VertexType>>();
};

/**
 * This class represents holds the structure of a directed graph.
 *
 * @author Sebastian Bauer
 *
 */
public class DirectedGraph<VertexType> implements Iterable<VertexType>
{
	/** Contains the vertices associated to meta information (edges) */
	private HashMap<VertexType,VertexAttributes<VertexType>> vertices;

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
	}

	public static interface IDistanceVisitor<VertexType>
	{
		boolean visit(VertexType vertex, List<VertexType> path, int distance);
	}

	/**
	 * Constructs the directed graph.
	 */
	public DirectedGraph()
	{
		vertices = new HashMap<VertexType,VertexAttributes<VertexType>>();
	}

	/**
	 * Adds the given vertex to the graph. Nothing happens if the graph
	 * already contains the vertex.
	 *
	 * @param vertex
	 */
	public void addVertex(VertexType vertex)
	{
		if (!vertices.containsKey(vertex))
		{
			VertexAttributes<VertexType> va = new VertexAttributes<VertexType>();
			vertices.put(vertex,va);
		}
	}

	/**
	 * Add a new edge into the graph.
	 *
	 * @param edge the edge which links two vertices.
	 *
	 * @throws IllegalArgumentException if the edge links vertices
	 *         which haven't been added to the graph.
	 */
	public void addEdge(Edge<VertexType> edge)
	{
		VertexAttributes<VertexType> vaSource = vertices.get(edge.getSource());
		VertexAttributes<VertexType> vaDest = vertices.get(edge.getDest());

		/* Ensure that the arguments are valid, i.e. both source
		 * and dest must be vertices within the graph */
		if (vaSource == null || vaDest == null)
			throw new IllegalArgumentException();

		vaSource.outEdges.add(edge);

		vaDest.inEdges.add(edge);
		vaDest.ancestorsArray = null;
	}

	/**
	 * Returns the iterator to iterate through all edges going
	 * into the given object (i.e. all edges where the object
	 * is the edge's destination)
	 *
	 * @param t
	 *
	 * @return
	 */
	public Iterator<Edge<VertexType>> getInEdges(VertexType t)
	{
		VertexAttributes<VertexType> va = vertices.get(t);
		assert(va != null);
		return va.inEdges.iterator();
	}


	/**
	 * Returns the direct ancestors of the graph as an array.
	 *
	 * This method is fast when called multiple times.
	 *
	 * @param node
	 * @return
	 */
	public Object [] getAncestors(VertexType node)
	{
		VertexAttributes<VertexType> va = vertices.get(node);
		assert(va != null);
		if (va.ancestorsArray != null) return va.ancestorsArray;

		va.ancestorsArray = new Object[va.inEdges.size()];

		int i = 0;

		for (Edge<VertexType> edge : va.inEdges)
			va.ancestorsArray[i++] = edge.getSource();

		return va.ancestorsArray;
	}

	/**
	 * Returns the iterator to iterate through all edges going
	 * out of given object. (i.e. all edges where the object
	 * is the edge's source)
	 *
	 * @param t
	 *
	 * @return
	 */
	public Iterator<Edge<VertexType>> getOutEdges(VertexType t)
	{
		VertexAttributes<VertexType> va = vertices.get(t);
		assert(va != null);
		return va.outEdges.iterator();
	}

	/**
	 * Performs a breadth-first search onto the graph starting at a given
	 * set of vertices. Vertices occurring in loops are visited only once.
	 *
	 * @param vertex defines the vertex to start with.
	 *
	 * @param againstFlow the bfs in done against the direction of the edges.
	 *
	 * @param visitor a object of a class implementing IVisitor. For every
	 *        vertex visited by the algorithm the visitor.visited() method is
	 *        called. Note that the method is called also for the  vertices
	 *        specified by initialSet (in arbitrary order)
	 *
	 * @see IVisitor
	 */
	public void bfs(VertexType vertex, boolean againstFlow, IVisitor<VertexType> visitor)
	{
		ArrayList<VertexType> set = new ArrayList<VertexType>(1);
		set.add(vertex);
		bfs(set,againstFlow,visitor);
	}

	/**
	 * Performs a breadth-first search onto the graph starting at a given
	 * set of vertices. Vertices occurring in loops are visited only once.
	 *
	 * @param initialSet defines the set of vertices to start with.
	 *
	 * @param againstFlow the bfs in done against the direction of the edges.
	 *
	 * @param visitor a object of a class implementing IVisitor. For every
	 *        vertex visited by the algorithm the visitor.visited() method is
	 *        called. Note that the method is called also for the  vertices
	 *        specified by initialSet (in arbitrary order)
	 *
	 * @see IVisitor
	 */
	public void bfs(Collection<VertexType> initialSet, boolean againstFlow, IVisitor<VertexType> visitor)
	{
		HashSet<VertexType> visited = new HashSet<VertexType>();

		/* Add all terms to the queue */
		LinkedList<VertexType> queue = new LinkedList<VertexType>();
		for (VertexType vertex  : initialSet)
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
			 * and mark them as visited. If bfs is done against flow
			 * neighbours can be found via the ingoing edges otherwise
			 * via the outgoing edges */
			Iterator<Edge<VertexType>> edgeIter;

			if (againstFlow) edgeIter = getInEdges(head);
			else edgeIter = getOutEdges(head);

			while (edgeIter.hasNext())
			{
				Edge<VertexType> edge = edgeIter.next();
				VertexType neighbour;

				if (againstFlow) neighbour = edge.getSource();
				else neighbour = edge.getDest();

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
	 * Calculates the shortest path from the given vertex to all vertices. Note that
	 * negative weights are not supported!
	 *
	 * @param source defines the source
	 * @param againstFlow if specified the path is walked against the direction of the graph
	 * @param visitor object implementing IDistanceVisitor which can be used to process the
	 *        results
	 */
	public void singleSourceShortestPath(VertexType source, boolean againstFlow, IDistanceVisitor<VertexType> visitor)
	{
		/**
		 * This class implements some meta information needed by Dijkstra's
		 * algorithm.
		 *
		 * @author Sebastian Bauer
		 */
		class VertexExtension implements Comparable<VertexExtension>
		{
			public VertexType vertex;
			public int distance;
			public VertexType parent;

			public VertexExtension(VertexType vertex, int distance, VertexType parent)
			{
				this.vertex = vertex;
				this.distance = distance;
				this.parent = parent;
			}

			public int compareTo(VertexExtension arg0)
			{
				return distance - arg0.distance;
			}

			@Override
			public int hashCode()
			{
				return vertex.hashCode();
			}
		}

		/* This is the implementation of the Dijkstra algorithm */

		/* TODO: Get rid of PriorityQueue by using a better suited data structure */
		PriorityQueue<VertexExtension> queue = new PriorityQueue<VertexExtension>();
		HashMap<VertexType,VertexExtension> map = new HashMap<VertexType,VertexExtension>();

		/* place the first node into the priority queue */
		VertexExtension ve = new VertexExtension(source,0,null);
		queue.offer(ve);
		map.put((VertexType)ve.vertex,ve); /* FIXME: (VertexType) is for the java compiler */

		while (!queue.isEmpty())
		{
			VertexExtension next = queue.poll();

			Iterator<Edge<VertexType>> edgeIter;

			if (againstFlow) edgeIter = getInEdges((VertexType)(next.vertex));
			else edgeIter = getOutEdges((VertexType)next.vertex);

			while (edgeIter.hasNext())
			{
				Edge<VertexType> edge = edgeIter.next();
				VertexType neighbour;

				if (againstFlow) neighbour = edge.getSource();
				else neighbour = edge.getDest();

				/* Relax the neighbour (or add it if it is not available) */
				VertexExtension neighbourExt = map.get(neighbour);
				if (neighbourExt == null)
				{
					neighbourExt = new VertexExtension(neighbour, next.distance + edge.getWeight(), next.vertex);
					map.put(neighbour,neighbourExt);
					queue.offer(neighbourExt);
				} else
				{
					/* Would the edge from the current vertex to the neighbour
					 * make the path to the neighbour shorter? */
					if (neighbourExt.distance > next.distance + edge.getWeight())
					{
						queue.remove(neighbourExt);
						neighbourExt.distance = next.distance + edge.getWeight();
						neighbourExt.parent = next.vertex;
						queue.offer(neighbourExt);
					}
				}
			}
		}

		/* Now throw out the results */
		for (Entry<VertexType,VertexExtension> v : map.entrySet())
		{
			/* Build the path by successively traversing the path from
			 * the current destination through the stored ancestors
			 * (parents) */
			LinkedList<VertexType> ll = new LinkedList<VertexType>();
			VertexExtension curVe = v.getValue();
			do
			{
				ll.addFirst((VertexType)curVe.vertex); /* FIXME: (VertexType) is for the java compiler */
				curVe = map.get(curVe.parent);
			} while (curVe != null);

			if (!visitor.visit((VertexType)v.getValue().vertex,ll,v.getValue().distance))  /* FIXME: (VertexType) is for the java compiler */
				return;
		}
	}

	public ArrayList<VertexType> topologicalOrder()
	{
		ArrayList<VertexType> list = new ArrayList<VertexType>(vertices.size());

		return list;
	}


	/**
	 * The bellman-ford algorithm,
	 *
	 * @param source
	 * @param weightMultiplier multiplies the weights by the given factor.
	 * @param visitor
	 */
	public void bf(VertexType source, int weightMultiplier, IDistanceVisitor<VertexType> visitor)
	{
		/**
		 * This class implements some meta information needed by the BF algorithm.
		 *
		 * @author Sebastian Bauer
		 */
		class VertexExtension implements Comparable<VertexExtension>
		{
			public VertexType vertex;
			public int distance;
			public VertexType parent;

			public VertexExtension(VertexType vertex, int distance, VertexType parent)
			{
				this.vertex = vertex;
				this.distance = distance;
				this.parent = parent;
			}

			public int compareTo(VertexExtension arg0)
			{
				return distance - arg0.distance;
			}

			@Override
			public int hashCode()
			{
				return vertex.hashCode();
			}
		}

		HashMap<VertexType,VertexExtension> map = new HashMap<VertexType,VertexExtension>();
		map.put(source, new VertexExtension(source,0,null));

		/* Vertices loop */
		for (int i=0;i<vertices.size();i++)
		{
			boolean changed = false;

			/* Edge loop */
			for (Entry<VertexType, VertexAttributes<VertexType>> ent : vertices.entrySet())
			{
				VertexType u = ent.getKey();

				VertexExtension uExt = map.get(u);
				if (uExt == null) continue;

				for (Edge<VertexType> edge : ent.getValue().outEdges)
				{
					VertexType v = edge.getDest();


					VertexExtension vExt = map.get(v);
					if (vExt == null)
					{
						vExt = new VertexExtension(v, uExt.distance + edge.getWeight()*weightMultiplier, u);
						map.put(v,vExt);
						changed = true;
					} else
					{
						if (vExt.distance > uExt.distance + edge.getWeight() * weightMultiplier)
						{
							vExt.distance = uExt.distance + edge.getWeight() * weightMultiplier;
							vExt.parent = u;
							changed = true;
						}
					}
				}
			}

			/* If this iteration doesn't affect a change, the next own won't change anything either */
			if (!changed)
				break;
		}

		/* Now throw out the results */
		for (Entry<VertexType,VertexExtension> v : map.entrySet())
		{
			/* Build the path by successively traversing the path from
			 * the current destination through the stored ancestors
			 * (parents) */
			LinkedList<VertexType> ll = new LinkedList<VertexType>();
			VertexExtension curVe = v.getValue();
			do
			{
				ll.addFirst((VertexType)curVe.vertex); /* FIXME: (VertexType) is for the java compiler */
				curVe = map.get(curVe.parent);
			} while (curVe != null);

			if (!visitor.visit((VertexType)v.getValue().vertex,ll,v.getValue().distance))  /* FIXME: (VertexType) is for the java compiler */
				return;
		}
	}

	/**
	 * Calculates the shortest path from the given vertex to all vertices. Supports negative weights.
	 *
	 * @param source defines the source
	 * @param visitor object implementing IDistanceVisitor which can be used to process the
	 *        results
	 */
	public void singleSourceShortestPathBF(VertexType source, IDistanceVisitor<VertexType> visitor)
	{
		bf(source,1,visitor);
	}

	/**
	 * Calculates the longest path from the given vertex to all vertices.
	 *
	 * @param source defines the source
	 * @param againstFlow if specified the path is walked against the direction of the graph
	 * @param visitor object implementing IDistanceVisitor which can be used to process the
	 *        results
	 */
	public void singleSourceLongestPath(VertexType source, final IDistanceVisitor<VertexType> visitor)
	{
		bf(source,-1,new IDistanceVisitor<VertexType>()
				{
					public boolean visit(VertexType vertex, java.util.List<VertexType> path, int distance)
					{
						return visitor.visit(vertex, path, distance * -1);
					};
				});
	}

	public Iterator<VertexType> iterator()
	{
		return vertices.keySet().iterator();
	}

	/**
	 * Get the in-degree of the given vertex.
	 *
	 * @param v
	 * @return
	 */
	public int getInDegree(VertexType v)
	{
		VertexAttributes<VertexType> va = vertices.get(v);
		if (va == null) return -1;
		return va.inEdges.size();
	}

	/**
	 * Get the in-degree of the given vertex.
	 *
	 * @param v
	 * @return
	 */
	public int getOutDegree(VertexType v)
	{
		VertexAttributes<VertexType> va = vertices.get(v);
		if (va == null) return -1;
		return va.outEdges.size();
	}
}
