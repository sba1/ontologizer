package sonumina.math.graph;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.junit.Test;

public class SlimDirectedGraphViewTest
{
	/**
	 * Check whether the nodes match via map.
	 *
	 * @param expected
	 * @param actual
	 * @param map
	 */
	private void checkNodes(Iterator<TestData> expected, int[] actual,  Map<TestData, Integer> map)
	{
		while (expected.hasNext())
		{
			TestData ch = expected.next();
			boolean found = false;
			for (int j=0; j < actual.length; j++)
			{
				if (actual[j] == map.get(ch))
				{
					found = true;
					break;
				}
			}
			assertTrue(found);
		}
	}

	@Test
	public void testCreateSlimGraph()
	{
		final DirectedGraph<TestData> graph = new DirectedGraph<TestData>();
		final TestData root = new TestData("root");
		final TestData a = new TestData("a");
		final TestData b = new TestData("b");
		final TestData c = new TestData("c");
		final TestData d = new TestData("d");
		final TestData e = new TestData("e");
		final TestData f = new TestData("f");
		final TestData g = new TestData("g");

		/* Add some vertices */
		graph.addVertex(root);
		graph.addVertex(a);
		graph.addVertex(b);
		graph.addVertex(c);
		graph.addVertex(d);
		graph.addVertex(e);
		graph.addVertex(f);
		graph.addVertex(g);

		/* Link the vertices as follow (always top down direction):
		       root
		       / |\
		      /  | \
		      a->b  c
		           / \
		           d e
		           | |
		           | f
		           \ /
		            g
		*/

		graph.addEdge(new Edge<TestData>(root,a));
		graph.addEdge(new Edge<TestData>(root,b));
		graph.addEdge(new Edge<TestData>(root,c));
		graph.addEdge(new Edge<TestData>(a,b));
		graph.addEdge(new Edge<TestData>(c,d));
		graph.addEdge(new Edge<TestData>(c,e));
		graph.addEdge(new Edge<TestData>(d,g));
		graph.addEdge(new Edge<TestData>(e,f));
		graph.addEdge(new Edge<TestData>(f,g));

		SlimDirectedGraphView<TestData> sg = SlimDirectedGraphView.create(graph);
		assertEquals(8, sg.getNumberOfVertices());

		/* Populate own map that we use to verify the structure */
		Map<TestData, Integer> map = new HashMap<TestData, Integer>();
		int vs = sg.getNumberOfVertices();
		for (int i=0; i<vs; i++)
		{
			map.put(sg.getVertex(i), i);
		}

		for (int i=0; i<vs; i++)
		{
			checkNodes(graph.getChildNodes(sg.getVertex(i)), sg.vertexChildren[i], map);
			checkNodes(graph.getParentNodes(sg.getVertex(i)), sg.vertexParents[i], map);
		}
	}
}
