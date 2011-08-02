package sonumina.math.graph;

import sonumina.math.graph.DirectedGraphLayout.Dimension;
import junit.framework.TestCase;

public class DirectedGraphLayoutTest extends TestCase
{

	public void testLayout()
	{
		final String [] names = new String[]{
				"|Root|","|Node|", "|A third node, which is very long|", "|4th|", "|5th|"
		};
		DirectedGraph<Integer> graph = new DirectedGraph<Integer>();
		graph.addVertex(0);
		graph.addVertex(1);
		graph.addVertex(2);
		graph.addVertex(3);
//		graph.addVertex(4);
		
		graph.addEdge(new Edge<Integer>(0,1));
		graph.addEdge(new Edge<Integer>(1,2));
		graph.addEdge(new Edge<Integer>(0,3));
		graph.addEdge(new Edge<Integer>(3,2));
//		graph.addEdge(new Edge<Integer>(2,4));

		final StringBuilder str = new StringBuilder();
		final String emptyLine = "                                                                                            \n";
		for (int i=0;i<6;i++)
			str.append(emptyLine);

		DirectedGraphLayout.layout(graph, new DirectedGraphLayout.IGetDimension<Integer>()
				{
					@Override
					public void get(Integer vertex, Dimension d)
					{
						String name = names[vertex]; 
						d.height = 1;
						d.width = name.length();
					}
			
				}, new DirectedGraphLayout.IPosition<Integer>() {
					@Override
					public void setSize(int width, int height) { }
					
					@Override
					public void set(Integer vertex, int left, int top)
					{
						String name = names[vertex];
						System.out.println(vertex + " at " + left + "/" + top);
						int start = left + top * emptyLine.length();
						str.replace(start, start + name.length(), name);
					}
				});

		
		System.out.println(str.toString());
		
	}
}
