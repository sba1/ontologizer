package sonumina.math.graph;

import java.util.Iterator;

/**
 * Class providing some utils mainly for debugging.
 * 
 * @author Sebastian Bauer
 */
public class DirectedGraphUtil
{
	/**
	 * This is a debug function to print java code to construct the given
	 * graph. This only works with primitive data types.
	 *  
	 * @param graph
	 */
	public static <T> void printGraphCode(DirectedGraph<T> graph)
	{
		Class<? extends Object> clazz = graph.getArbitaryNode().getClass();
		System.out.println("DirectedGraph<" + clazz.getName() + "> g = new DirectedGraph<" + clazz.getName() + ">();");

		for (T v : graph)
			System.out.println("g.addVertex("+v+");");

		for (T v : graph)
		{
			Iterator<T> parentIter = graph.getParentNodes(v);
			while (parentIter.hasNext())
			{
				T p = parentIter.next();
				
				System.out.println("g.addEdge(new Edge(" + p + ", " + v + "));");
				
			}
		}
		
	}
}
