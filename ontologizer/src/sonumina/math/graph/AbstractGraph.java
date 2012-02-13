package sonumina.math.graph;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Locale;

import sonumina.collections.TinyQueue;

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
	public abstract Iterator<VertexType>getParentNodes(VertexType vt);
	
	/**
	 * Returns the vertices to which the outgoing edges point to.
	 * 
	 * @param vt
	 * @return
	 */
	public abstract Iterator<VertexType>getChildNodes(VertexType vt);

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
	 *        represented by <code>vertex</code> (the one given as parameter to this method).
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
						if (againstFlow) return getParentNodes(t);
						else return getChildNodes(t);
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
		TinyQueue<VertexType> queue = new TinyQueue<VertexType>();
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

			/* Add not yet visited neighbors of old head to the queue
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
	 * Returns whether there is a path from source to dest.
	 * @param source
	 * @param dest
	 * @return
	 */
	public boolean existsPath(final VertexType source, final VertexType dest)
	{
		class ExistsPathVisitor implements IVisitor<VertexType>
		{
			boolean found;
			
			public boolean visited(VertexType vertex)
			{
				if (vertex.equals(dest))
				{
					found = true;
					return false;
				}
				return true;
			}
		}
		ExistsPathVisitor epv = new ExistsPathVisitor();
		
		bfs(source,false,epv);
		return epv.found;
	}
	
	/**
	 * Returns the vertices in a topological order.
	 * 
	 * If the length of the returned differs from the number of vertices we have a cycle. 
	 * 
	 * @return
	 */
	public ArrayList<VertexType> topologicalOrder()
	{
		/* Gather structure */
		HashMap<VertexType,LinkedList<VertexType>> vertex2Children 	= new HashMap<VertexType,LinkedList<VertexType>>(); 
		HashMap<VertexType,Integer> vertex2NumParents 				= new HashMap<VertexType,Integer>();
		LinkedList<VertexType> verticesWithNoParents 				= new LinkedList<VertexType>();

		for (VertexType v : getVertices())
		{
			/* Build list of children */
			LinkedList<VertexType> vChild 			= new LinkedList<VertexType>();
			Iterator<VertexType> childrenIterator 	= getChildNodes(v);
			while (childrenIterator.hasNext())
				vChild.add(childrenIterator.next());
			vertex2Children.put(v, vChild);

			/* Determine the number of parents for each node */
			int numParents 						= 0;
			Iterator<VertexType> parentIterator = getParentNodes(v);
			while (parentIterator.hasNext())
			{
				parentIterator.next();
				numParents++;
			}

			if (numParents == 0){
				verticesWithNoParents.add(v);
			}
			else{
				vertex2NumParents.put(v,numParents);
			}
		}
		
		int numOfVertices 			= vertex2Children.size();
		ArrayList<VertexType> order = new ArrayList<VertexType>(numOfVertices); 

		/* Take the first vertex in the queue verticesWithNoParents and to every
		 * vertex to which vertex is a parent decrease its current number of parents
		 * value by one.
		 */
		while (!verticesWithNoParents.isEmpty())
		{
			VertexType top = verticesWithNoParents.poll();
			order.add(top);

			for (VertexType p : vertex2Children.get(top))
			{
				int newNumParents = vertex2NumParents.get(p)-1;
				vertex2NumParents.put(p,newNumParents);
				
				if (newNumParents == 0) 
					verticesWithNoParents.offer(p);
			}
		}

		return order;
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
	public void writeDOT(OutputStream fos, DotAttributesProvider<VertexType> provider)
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
	public void writeDOT(OutputStream fos, Iterable<VertexType> nodeSet, DotAttributesProvider<VertexType> provider)
	{
		writeDOT(fos,nodeSet,provider,0.4,0.4);
	}

	/**
	 * Writes out the graph as a dot file.
	 * 
	 * @param fos. For the output.
	 * @param nodeSet. Defines the subset of nodes to be written out.
	 * @param provider. Provides the attributes.
	 * @param nodeSep. The space between nodes of the same rank.
	 * @param rankSep. The space between two nodes of subsequent ranks.
	 */
	public void writeDOT(OutputStream fos, Iterable<VertexType> nodeSet, DotAttributesProvider<VertexType> provider, double nodeSep, double rankSep)
	{
		PrintWriter out = new PrintWriter(fos);
		
		out.format(Locale.US,"digraph G {nodesep=%f; ranksep=%f\n", nodeSep,rankSep);
		
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
			Iterator<VertexType> ancest = getChildNodes(s);
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
