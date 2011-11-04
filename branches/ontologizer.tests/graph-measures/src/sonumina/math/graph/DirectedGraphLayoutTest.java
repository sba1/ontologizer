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
	
	public void testLayout2()
	{
		DirectedGraph<java.lang.Integer> g = new DirectedGraph<java.lang.Integer>();
		g.addVertex(0);
		g.addVertex(10);
		g.addVertex(232);
		g.addVertex(311);
		g.addVertex(443);
		g.addVertex(681);
		g.addVertex(1540);
		g.addVertex(2667);
		g.addEdge(new Edge(0, 1540));
		g.addEdge(new Edge(10, 232));
		g.addEdge(new Edge(1540, 311));
		g.addEdge(new Edge(232, 2667));
		g.addEdge(new Edge(443, 2667));
		g.addEdge(new Edge(311, 443));
		g.addEdge(new Edge(2667, 681));
		
		DirectedGraphLayout.layout(g,new DirectedGraphLayout.IGetDimension<Integer>()
				{
					@Override
					public void get(Integer vertex, Dimension d) {
						/* Not needed for this test */
					}
				},
				 new DirectedGraphLayout.IPosition<Integer>()
				 {
					@Override
					public void set(Integer vertex, int left, int top) {
					}
					
					@Override
					public void setSize(int width, int height) {
					}
				 });
	}
}
