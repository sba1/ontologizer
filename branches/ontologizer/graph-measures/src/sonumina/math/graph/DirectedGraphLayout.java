package sonumina.math.graph;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

/**
 * A very basic layout algorithm for directed graphs.
 * 
 * @author Sebastian Bauer
 */
public class DirectedGraphLayout<T>
{
	static class Attr
	{
		int posX;
		int posY;
		int layoutPosX;
		int layoutPosY;
		int distanceToRoot;	/* This defines the vertical rank */
		int horizontalRank;
		int width;
		int height;
	}
	
	static public class Dimension
	{
		public int width;
		public int height;
	}

	static public interface IGetDimension<T>
	{
		/**
		 * Determine the dimension of the given vertex.
		 * 
		 * @param vertex
		 * @param d
		 * @note that this method may be called more than once.
		 */
		void get(T vertex, Dimension d);
	}
	
	static public interface IPosition<T>
	{
		void setSize(int width, int height);
		void set(T vertex, int left, int top);
	}
	
	protected DirectedGraph<T> graph;
	protected IGetDimension<T> dimensionCallback;
	protected IPosition<T> positionCallback;
	protected SlimDirectedGraphView<T> slimGraph;

	private int maxDistanceToRoot = -1;
	private Attr [] attrs;

	DirectedGraphLayout(DirectedGraph<T> graph, IGetDimension<T> dimensionCallback, IPosition<T> positionCallback)
	{
		this.graph = graph;
		this.dimensionCallback = dimensionCallback;
		this.positionCallback = positionCallback;
		this.slimGraph = new SlimDirectedGraphView<T>(graph);
		
		attrs = new Attr[graph.getNumberOfVertices()];
		for (int i=0;i<graph.getNumberOfVertices();i++)
			attrs[i] = new Attr();
		
		/* Find the roots */
		List<T> rootList = new ArrayList<T>(4);
		for (T n : graph)
			if (graph.getInDegree(n)==0) rootList.add(n);
		if (rootList.size() == 0)
			rootList.add(graph.getArbitaryNode());
				
		/* Find out the distance to the root of each vertex. Remember the deepest one */
		for (T root : rootList)
		{
			graph.singleSourceLongestPath(root,new DirectedGraph.IDistanceVisitor<T>() {
				public boolean visit(T n, List<T> path, int distance)
				{
					/* Note that we could have more than one root, for which the distance
					 * could be different. We remember the largest distance.
					 */
					if (distance > attrs[slimGraph.getVertexIndex(n)].distanceToRoot)
						attrs[slimGraph.getVertexIndex(n)].distanceToRoot = distance;
					if (distance > maxDistanceToRoot) maxDistanceToRoot = distance;
					return true;
				}
			});
		}
	}

	private void layout(final int horizSpace, final int vertSpace)
	{
		if (graph.getNumberOfVertices() == 0)
			return;

		/* Determine the dimension of each node */
		final Dimension dim = new Dimension();
		for (int i=0;i<slimGraph.getNumberOfVertices();i++)
		{
			Attr a = attrs[i];
			dimensionCallback.get(slimGraph.getVertex(i), dim);
			a.width = dim.width;
			a.height = dim.height;
		}
		
		/* Determine the heights of each level and the width of each level as well as the number of objects per level */
		int [] levelHeight = new int[maxDistanceToRoot+1];
		int [] levelWidth = new int[maxDistanceToRoot+1];
		int [] levelCounts = new int[maxDistanceToRoot+1];

		int maxLevelWidth = -1;
		for (int i=0;i<slimGraph.getNumberOfVertices();i++)
		{
			Attr a = attrs[i];
			if (a.height > levelHeight[a.distanceToRoot])
				levelHeight[a.distanceToRoot] = a.height;
			levelCounts[a.distanceToRoot]++;
			levelWidth[a.distanceToRoot] += a.width;
		}
		
		/* Now assign level nodes */
		int [][] levelNodes = new int[maxDistanceToRoot+1][];
		int [] levelCounter = new int[maxDistanceToRoot+1]; /* An index counter per level */
		for (int i=0;i<slimGraph.getNumberOfVertices();i++)
		{
			Attr a = attrs[i];
			if (levelNodes[a.distanceToRoot] == null)
				levelNodes[a.distanceToRoot] = new int[levelCounts[a.distanceToRoot]];
			levelNodes[a.distanceToRoot][levelCounter[a.distanceToRoot]++] = i;
		}

		/* Determine max width of any level */
		for (int i=0;i<=maxDistanceToRoot;i++)
		{
			levelWidth[i] += horizSpace * levelCounts[i];
			if (levelWidth[i] > maxLevelWidth)
				maxLevelWidth = levelWidth[i];
		}
		
		/* Determine the vertical position of each level */
		int [] levelYPos = new int[maxDistanceToRoot+1];
		for (int i=1;i<levelYPos.length;i++)
			levelYPos[i] = levelYPos[i-1] + levelHeight[i-1] + vertSpace;

		/* Assign ypos */
		for (int i=0;i<slimGraph.getNumberOfVertices();i++)
		{
			Attr a = attrs[i];
			a.layoutPosY = levelYPos[a.distanceToRoot];
		}

		/* Distribute x rank of nodes for each level */
		int [] levelCurXRank = new int[maxDistanceToRoot+1];
		for (int i=0;i<slimGraph.getNumberOfVertices();i++)
		{
			Attr a = attrs[i];
			a.horizontalRank = levelCurXRank[a.distanceToRoot]++;
		}

		/* Assign initial xpos */
		int [] levelCurXPos = new int[maxDistanceToRoot+1];
		for (int i=0;i<slimGraph.getNumberOfVertices();i++)
		{
			Attr a = attrs[i];
			a.layoutPosX = levelCurXPos[a.distanceToRoot];
			levelCurXPos[a.distanceToRoot] += a.width + horizSpace;
		}

		int currentScore = scoreLayout();

		/* Build node queue */
		LinkedList<T> nodeQueue = new LinkedList<T>();
		for (int l = 0; l <= maxDistanceToRoot; l++)
		{
			for (int j=0;j<levelNodes[l].length;j++)
			{
				nodeQueue.add(slimGraph.getVertex(levelNodes[l][j]));
			}
		}

		boolean onlyAcceptImprovements = true;

		/* In each run, we select a node which decreases the score best */
		for (int run = 0; run < 100; run++)
		{
			int bestScore = currentScore;
			int bestLayoutPosX = -1;
			T bestNode = null;

			ListIterator<T> queueIter = nodeQueue.listIterator();
			
			LinkedList<T> savedNodes = new LinkedList<T>(); 

			boolean improved = false;
			
			/* First pass, we try to improve the configuration */

			while (queueIter.hasNext())
			{
				T n = queueIter.next();
				int vi = slimGraph.getVertexIndex(n);
				Attr na = attrs[vi];
				
				int horizRank = na.horizontalRank;
				int vertRank = na.distanceToRoot;
				
				/* Determine the minimal x position of this node. This is aligned to the left border of the node */
				int minX;
				if (horizRank==0) minX = 0;
				else minX = attrs[levelNodes[vertRank][horizRank-1]].layoutPosX + attrs[levelNodes[vertRank][horizRank-1]].width + horizSpace; 

				/* Determine the maximal x position of this node. This is aligned to the left border of the node */
				int maxX;
				if (horizRank==levelNodes[vertRank].length-1) maxX = maxLevelWidth - na.width;
				else maxX = attrs[levelNodes[vertRank][horizRank+1]].layoutPosX - horizSpace - na.width;

				/* Remember the current pos */
				int savedLayoutPosX = na.layoutPosX; 

				/* Calculate the sum of all horizontal positions (for determination of the mean) */
				int sumX = 0;
				for (int p : slimGraph.vertexParents[vi])
					sumX += getEdgeX(attrs[p]);
				for (int c : slimGraph.vertexChildren[vi])
					sumX += getEdgeX(attrs[c]);

				int cnt = slimGraph.vertexParents[vi].length + slimGraph.vertexChildren[vi].length; 

				na.layoutPosX = Math.min(maxX,Math.max(minX,sumX / cnt - na.width / 2));

				int newScore = scoreLayout();
				if (newScore <= bestScore && savedLayoutPosX != na.layoutPosX)
				{
					if (newScore < bestScore || !onlyAcceptImprovements)
					{
						bestScore = newScore;
						bestLayoutPosX = na.layoutPosX;
						bestNode = n;
						
						if (newScore == bestScore)
						{
							queueIter.remove();
							savedNodes.addLast(n);
						} else
						{
							improved = true;
						}
					}
				}

				na.layoutPosX = savedLayoutPosX; /* Restore */
			}
			
			if (bestNode != null)
			{
				attrs[slimGraph.getVertexIndex(bestNode)].layoutPosX =  bestLayoutPosX;
				currentScore = bestScore;
				onlyAcceptImprovements = true;
			} else
			{
				if (!onlyAcceptImprovements)
					break;

				onlyAcceptImprovements = false;
			}
			
			for (T n : savedNodes)
				nodeQueue.addLast(n);
		}
		
		/* Calculate area */
		int width = 0;
		int height = 0;
		for (int i=0;i<slimGraph.getNumberOfVertices();i++)
		{
			Attr a = attrs[i];
			if (a.layoutPosX + a.width > width) width = a.layoutPosX + a.width - 1;
			if (a.layoutPosY + a.height > height) height = a.layoutPosY + a.height - 1;
		}		
		positionCallback.setSize(width, height);

		/* Emit positions */
		for (int i=0;i<slimGraph.getNumberOfVertices();i++)
		{
			Attr a = attrs[i];
			positionCallback.set(slimGraph.getVertex(i), a.layoutPosX, a.layoutPosY);
		}
	}

	/**
	 * Scores the current layout.
	 * 
	 * @param nodes2Attrs
	 * @param maxDistanceToRoot
	 * @param levelNodes
	 * @return
	 */
	private int scoreLayout()
	{
		int length = 0;
		for (int j=0;j<slimGraph.getNumberOfVertices();j++)
		{
			Attr na = attrs[j];
			int e1x = getEdgeX(na);
			
			for (int p : slimGraph.vertexParents[j])
			{
				Attr ap = attrs[p];
				int e2x = getEdgeX(ap);
				length += Math.abs(e1x - e2x);
			}
		}
		return length;
	}

	/**
	 * Returns the x coordiate of the given attr.
	 * 
	 * @param na
	 * @return
	 */
	private final int getEdgeX(Attr na)
	{
		return na.layoutPosX + na.width / 2;
	}

	public static <T> void layout(DirectedGraph<T> graph, IGetDimension<T> dimensionCallback, IPosition<T> positionCallback)
	{
		layout(graph,dimensionCallback,positionCallback,2,2);
	}

	public static <T> void layout(DirectedGraph<T> graph, IGetDimension<T> dimensionCallback, IPosition<T> positionCallback, int horizSpace, int vertSpace)
	{
		if (graph.getNumberOfVertices() == 0)
			return;
		new DirectedGraphLayout<T>(graph,dimensionCallback,positionCallback).layout(horizSpace,vertSpace);
	}
}
