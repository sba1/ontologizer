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
 * @author sebastiankohler
 */
public class SlimDirectedGraphView<VertexType>
{
	/** An array of all terms */
	private Object [] vertices;
	
	/** Map specific terms to the index in the allTerms array */
	public HashMap<VertexType,Integer> vertex2Index;
	
	/** Contains all the ancestors of the terms (and the terms itself). 
	 * Note that the array of ancestors is sorted. */
	public int [][] vertexAncestors;
	
	/** Contains the parents of the terms */
	public int [][] vertexParents;
	
	/** Contains the children of the term */
	public int [][] vertexChildren;
	
	/** Contains the descendants of the (i.e., children, grand-children, etc. and the term itself). 
	 * Note that the array of descendants is sorted.*/
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
			VertexType v 					= (VertexType)vertices[i];
			Iterator<VertexType> parentIter = graph.getParentNodes(v);
			vertexParents[i] 				= createIndicesFromIter(parentIter);
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
			
			/* Sort them, as we require this for binary search in isAncestor() */ 
			Arrays.sort(vertexAncestors[i]);
		}
		
		/* Term children stuff */
		vertexChildren = new int[vertices.length][];
		for (i=0;i<vertices.length;i++)
		{
			VertexType v = (VertexType)vertices[i];

			Iterator<VertexType> childrenIter 	= graph.getChildNodes(v);
			vertexChildren[i] 					= createIndicesFromIter(childrenIter);
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

			/* Sort them, as we require this for binary search in isDescendant() */ 
			Arrays.sort(vertexDescendants[i]);
		}
	}

	/**
	 * Creates an index array from the given vertex iterator. 
	 * 
	 * @param iterator
	 * @return
	 */
	private int[] createIndicesFromIter(Iterator<VertexType> iterator)
	{
		ArrayList<Integer> indicesList = new ArrayList<Integer>(10);

		while (iterator.hasNext())
		{
			VertexType p = iterator.next();
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
	 * Determines whether node with the index i is an ancestor of node with index j. 
	 * @note The ancestors of a given term include the given term itself.
	 * @param i
	 * @param j
	 * @return true if the node with the index i is an ancestor
	 * of the node with the index j, otherwise false.
	 */
	public boolean isAncestor(int i, int j)
	{
		int [] ancs = vertexAncestors[j];
		int r 		=  Arrays.binarySearch(ancs,i);
		return r >= 0;
	}
	
	/**
	 * Determines whether the node with index i is a 
	 * descendant of the node with the index j. 
	 * @note The descendants of a given term include the given term itself.
	 * @param i
	 * @param j
	 * @return true if the node with the index i is a descendant
	 * of the node with the index j, otherwise false.
	 */
	public boolean isDescendant(int i, int j)
	{
		int [] descs 	= vertexDescendants[j];
		int r 			= Arrays.binarySearch(descs,i);
		return r >= 0;
	}
	
	/**
	 * Determines if the given vertex i is an ancestor of
	 * the given vertex j.
	 * @param i
	 * @param j
	 * @return true if the node i is an ancestor
	 * of the node j, otherwise false.
	 */
	public boolean isAncestor(VertexType i, VertexType j){
		
		/* Check that both nodes are present in graph */
		if ( (! isVertexInGraph(i)) || (! isVertexInGraph(j)))
			return false;
		
		int iIdx = vertex2Index.get(i);
		int jIdx = vertex2Index.get(j);
		
		return isAncestor(iIdx, jIdx);
	}
	
	/**
	 * Determines if the given vertex i is a descendant of
	 * the given vertex j.
	 * @param i
	 * @param j
	 * @return true if the node i is a descendant
	 * of the node j, otherwise false.
	 */
	public boolean isDescendant(VertexType i, VertexType j){
		
		/* Check that both nodes are present in graph */
		if ( (! isVertexInGraph(i)) || (! isVertexInGraph(j)))
			return false;
		
		int iIdx = vertex2Index.get(i);
		int jIdx = vertex2Index.get(j);
		
		return isDescendant(iIdx, jIdx);
	}
	
	/**
	 * Get the descendants of a given vertex as ArrayList of vertices.
	 * @param t The vertex for that the descendant vertices should be found.
	 * @return null if the given vertex was not found in the graph, otherwise an ArrayList of vertices
	 * that are descendants of the given vertex.
	 */
	public ArrayList<VertexType> getDescendants(VertexType t){
		
		/* check that this vertex is found in the graph */
		if ( ! isVertexInGraph(t)){
			return null;
		}
		
		/* get the index of the vertex */
		int indexOfTerm 						= getVertexIndex(t);
		/* get all descendent indices of the vertex */
		int[] descendantIndices					= vertexDescendants[indexOfTerm];
		
		/* init the return list of vertex-objects */
		ArrayList<VertexType> descendantObjects = new ArrayList<VertexType>(descendantIndices.length);
		
		/* convert each descendant-index to an vertex object */
		for (int descendantIdx : descendantIndices){
			VertexType descendantVertex = getVertex(descendantIdx);
			descendantObjects.add(descendantVertex);
		}
		return descendantObjects;
	}
	
	
	/**
	 * Get the ancestors of a given vertex as ArrayList of vertices.
	 * @param t The vertex for that the ancestor vertices should be found.
	 * @return null if the given vertex was not found in the graph, otherwise an ArrayList of vertices
	 * that are ancestors of the given vertex.
	 */
	public ArrayList<VertexType> getAncestors(VertexType t){
		
		/* get the index of the vertex */
		int indexOfTerm 							= getVertexIndex(t);
		/* get all descendent indices of the vertex */
		int[] ancestorIndices					= vertexAncestors[indexOfTerm];
		
		/* init the return list of vertex-objects */
		ArrayList<VertexType> ancestorObjects 	= new ArrayList<VertexType>(ancestorIndices.length);
		
		/* convert each ancestor-index to an vertex object */
		for (int ancestorIdx : ancestorIndices){
			VertexType ancestorVertex = getVertex(ancestorIdx);
			ancestorObjects.add(ancestorVertex);
		}
		return ancestorObjects;
	}
	
	/**
	 * Checks if a given vertex can be found in the graph.
	 * @param The vertex to be searched.
	 * @return True if the vertex can be found. False if not.
	 */
	private boolean isVertexInGraph(VertexType vertex){
		return vertex2Index.containsKey(vertex);
	}
	
	
	public ArrayList<VertexType> getParents(VertexType t){
		
		/* get the index of the vertex */
		int indexOfTerm 							= getVertexIndex(t);
		/* get all indices of the vertex parents */
		int[] parentIndices						= vertexParents[indexOfTerm];
		
		/* init the return list of vertex-objects */
		ArrayList<VertexType> parentObjects = new ArrayList<VertexType>(parentIndices.length);
		
		/* convert each parent-index to an vertex object */
		for (int parentIdx : parentIndices){
			VertexType parentVertex = getVertex(parentIdx);
			parentObjects.add(parentVertex);
		}
		return parentObjects;
	}
	
	public ArrayList<VertexType> getChildren(VertexType t){
		
		/* get the index of the vertex */
		int indexOfTerm 							= getVertexIndex(t);
		/* get all indices of the vertex children */
		int[] childrenIndices					= vertexChildren[indexOfTerm];
		
		/* init the return list of vertex-objects */
		ArrayList<VertexType> childrenObjects 	= new ArrayList<VertexType>(childrenIndices.length);
		
		/* convert each child-index to an vertex object */
		for (int childIdx : childrenIndices){
			VertexType childVertex = getVertex(childIdx);
			childrenObjects.add(childVertex);
		}
		return childrenObjects;
	}
	
}
