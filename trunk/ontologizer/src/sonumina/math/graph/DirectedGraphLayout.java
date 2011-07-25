package sonumina.math.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * A very basic layout algorithm for directed graphs.
 *
 * @author Sebastian Bauer
 */
public class DirectedGraphLayout<T>
{
	class Attr
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
		void get(T vertex, Dimension d);
	}

	static public interface IPosition<T>
	{
		void setSize(int width, int height);
		void set(T vertex, int left, int top);
	}

	private DirectedGraph<T> graph;
	private IGetDimension<T> dimensionCallback;
	private IPosition<T> positionCallback;

	protected DirectedGraphLayout(DirectedGraph<T> graph, IGetDimension<T> dimensionCallback, IPosition<T> positionCallback)
	{
		this.graph = graph;
		this.dimensionCallback = dimensionCallback;
		this.positionCallback = positionCallback;
	}

	protected void layout()
	{
		final int horizSpace = 0;
		final int vertSpace = 0;

		final HashMap<T,Attr> nodes2Attrs = new HashMap<T,Attr>();

		if (graph.getNumberOfVertices() == 0)
			return;

		List<T> rootList = new ArrayList<T>(4);

		/* Find the roots */
		for (T n : graph)
			if (graph.getInDegree(n)==0) rootList.add(n);
		if (rootList.size() == 0)
			rootList.add(graph.getArbitaryNode());

		/* Find out the distance to the root of each vertex */
		for (T root : rootList)
		{
			graph.singleSourceLongestPath(root,new DirectedGraph.IDistanceVisitor<T>() {
				public boolean visit(T n, List<T> path, int distance) {

					Attr a = nodes2Attrs.get(n);
					if (a == null)
					{
						a = new Attr();
						a.distanceToRoot = distance;
						nodes2Attrs.put(n, a);
					}
					return true;
				}
			});
		}

		/* Determine the dimension of each node */
		final Dimension dim = new Dimension();
		int maxDistanceToRoot = -1;
		for (T n : graph)
		{
			Attr a = nodes2Attrs.get(n);
			if (a == null) /* May happen if graph as more than one root */
			{
				a = new Attr();
				nodes2Attrs.put(n,a);
			}
			dimensionCallback.get(n, dim);
			a.width = dim.width;
			a.height = dim.height;
			if (a.distanceToRoot > maxDistanceToRoot)
				maxDistanceToRoot = a.distanceToRoot;
		}

		/* Determine the heights of each level and the width of each level as well as the number of objects per level */
		int [] levelHeight = new int[maxDistanceToRoot+1];
		int [] levelWidth = new int[maxDistanceToRoot+1];
		int [] levelCounts = new int[maxDistanceToRoot+1];
		ArrayList [] levelNodes = new ArrayList[maxDistanceToRoot+1];
		int maxLevelWidth = -1;
		for (T n : graph)
		{
			Attr a = nodes2Attrs.get(n);
			if (a.height > levelHeight[a.distanceToRoot])
				levelHeight[a.distanceToRoot] = a.height;
			levelCounts[a.distanceToRoot]++;
			levelWidth[a.distanceToRoot] += a.width;
			if (levelNodes[a.distanceToRoot] == null)
				levelNodes[a.distanceToRoot] = new ArrayList();
			levelNodes[a.distanceToRoot].add(n);
		}
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
		for (T n : graph)
		{
			Attr a = nodes2Attrs.get(n);
			a.layoutPosY = levelYPos[a.distanceToRoot];
		}

		/* Distribute x rank of nodes for each level */
		int [] levelCurXRank = new int[maxDistanceToRoot+1];
		for (T n : graph)
		{
			Attr a = nodes2Attrs.get(n);
			a.horizontalRank = levelCurXRank[a.distanceToRoot]++;
		}

		/* Assign initial xpos */
		int [] levelCurXPos = new int[maxDistanceToRoot+1];
		for (T n : graph)
		{
			Attr a = nodes2Attrs.get(n);
			a.layoutPosX = levelCurXPos[a.distanceToRoot];
			levelCurXPos[a.distanceToRoot] += a.width + horizSpace;
		}

		int currentScore = scoreLayout(nodes2Attrs, maxDistanceToRoot, levelNodes);

		/* In each run, we select a node which decreases the score best */
		for (int run = 0; run < 100; run++)
		{
			int bestScore = currentScore;
			int bestLayoutPosX = -1;
			T bestNode = null;

			for (int l = 0; l <= maxDistanceToRoot; l++)
			{
				for (int j=0;j<levelNodes[l].size();j++)
				{
					T n = (T) levelNodes[l].get(j);
					Attr na = nodes2Attrs.get(n);

					int minX;
					if (j==0) minX = 0;
					else minX = nodes2Attrs.get(levelNodes[l].get(j-1)).layoutPosX + nodes2Attrs.get(levelNodes[l].get(j-1)).width + horizSpace;

					int maxX;
					if (j==levelNodes[l].size()-1) maxX = maxLevelWidth - na.width;
					else maxX = nodes2Attrs.get(levelNodes[l].get(j+1)).layoutPosX - horizSpace - na.width;


					/* Determine all neighbors */
					ArrayList<T> neighbors = new ArrayList<T>();
					Iterator<T> iter = graph.getParentNodes(n);
					while (iter.hasNext())
						neighbors.add(iter.next());
					iter = graph.getChildNodes(n);
					while (iter.hasNext())
						neighbors.add(iter.next());

					int savedLayoutPosX = na.layoutPosX; /* Remember the current pos */

					for (T neighbor : neighbors)
					{
						Attr neighbora = nodes2Attrs.get(neighbor);
						int newenx = getEdgeX(neighbora);

						na.layoutPosX = Math.min(maxX,Math.max(minX,newenx - na.width / 2));

						int newScore = scoreLayout(nodes2Attrs, maxDistanceToRoot, levelNodes);
						if (newScore < bestScore)
						{
							bestScore = newScore;
							bestLayoutPosX = na.layoutPosX;
							bestNode = n;
						}
					}
					na.layoutPosX = savedLayoutPosX; /* Restore */
				}
			}

			if (bestNode == null)
				break;

//			System.out.println(bestNode + " changed from " + nodes2Attrs.get(bestNode).layoutPosX + " to " + bestLayoutPosX);
			nodes2Attrs.get(bestNode).layoutPosX = bestLayoutPosX;
			currentScore = bestScore;

//			for (T n : graph)
//			{
//				Attr a = nodes2Attrs.get(n);
//				graph.get
//
//				a.layoutPosX = levelCurXPos[a.distanceToRoot];
//				levelCurXPos[a.distanceToRoot] += a.width + horizSpace;
//			}
//

		}

		/* Calculate area */
		int width = 0;
		int height = 0;
		for (T n: graph)
		{
			Attr a = nodes2Attrs.get(n);
			if (a.layoutPosX + a.width > width) width = a.layoutPosX + a.width - 1;
			if (a.layoutPosY + a.height > height) height = a.layoutPosY + a.height - 1;
		}
		positionCallback.setSize(width, height);

		/* Emit positions */
		for (T n: graph)
		{
			Attr a = nodes2Attrs.get(n);
			positionCallback.set(n, a.layoutPosX, a.layoutPosY);
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
	private int scoreLayout(final HashMap<T, Attr> nodes2Attrs,	int maxDistanceToRoot, ArrayList[] levelNodes)
	{
		int length = 0;
		for (int i=1;i<=maxDistanceToRoot;i++)
		{
			for (int j=0;j<levelNodes[i].size();j++)
			{
				T n = (T) levelNodes[i].get(j);
				Attr na = nodes2Attrs.get(n);
				int e1x = getEdgeX(na);

				Iterator<T> parents = graph.getParentNodes(n);
				while (parents.hasNext())
				{
					T p = parents.next();
					Attr ap = nodes2Attrs.get(p);
					int e2x = getEdgeX(ap);
					length += Math.abs(e1x - e2x);
				}
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
		new DirectedGraphLayout<T>(graph,dimensionCallback,positionCallback).layout();
	}
}
