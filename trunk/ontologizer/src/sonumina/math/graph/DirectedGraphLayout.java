package sonumina.math.graph;

import java.util.ArrayList;
import java.util.HashMap;
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
		int distanceToRoot;
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
			if (a.distanceToRoot > maxDistanceToRoot) maxDistanceToRoot = a.distanceToRoot;
		}
		
		/* Determine the heights of each level */
		int [] levelHeight = new int[maxDistanceToRoot+1];
		for (T n : graph)
		{
			Attr a = nodes2Attrs.get(n);
			if (a.height > levelHeight[a.distanceToRoot])
				levelHeight[a.distanceToRoot] = a.height; 
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

		/* Distribute x pos for each level */
		int [] levelCurXPos = new int[maxDistanceToRoot+1];
		for (T n : graph)
		{
			Attr a = nodes2Attrs.get(n);
			a.layoutPosX = levelCurXPos[a.distanceToRoot];
			levelCurXPos[a.distanceToRoot] += a.width + horizSpace;
		}

		/* Emit positions */
		for (T n: graph)
		{
			Attr a = nodes2Attrs.get(n);
			positionCallback.set(n, a.layoutPosX, a.layoutPosY);
		}
		
	}

	public static <T> void layout(DirectedGraph<T> graph, IGetDimension<T> dimensionCallback, IPosition<T> positionCallback)
	{
		new DirectedGraphLayout<T>(graph,dimensionCallback,positionCallback).layout();
	}
}
