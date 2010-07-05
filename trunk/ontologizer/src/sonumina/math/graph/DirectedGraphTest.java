/*
 * Created on 21.08.2005
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package sonumina.math.graph;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import sonumina.math.graph.AbstractGraph.DotAttributesProvider;
import sonumina.math.graph.DirectedGraph.IDistanceVisitor;

import junit.framework.Assert;
import junit.framework.TestCase;

class TestData
{
	public String id;

	public TestData(String id)
	{
		this.id = id;
	}

	@Override
	public String toString() {
		return id;
	}
};

public class DirectedGraphTest extends TestCase
{
	public void testGraph()
	{
		DirectedGraph<TestData> graph = new DirectedGraph<TestData>();
		TestData root = new TestData("root");
		TestData a = new TestData("a");
		TestData b = new TestData("b");
		TestData c = new TestData("c");
		TestData d = new TestData("d");
		TestData e = new TestData("e");
		TestData f = new TestData("f");
		TestData g = new TestData("g");

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

		/* Validate results */
		Iterator<Edge<TestData>> iter = graph.getOutEdges(root);
		HashSet<TestData> vertices = new HashSet<TestData>();
		while (iter.hasNext())
		{
			Edge<TestData> edge = iter.next();
			Assert.assertTrue(edge.getSource() == root);
			vertices.add(edge.getDest());
		}

		/* root should have three outgoing edges linking root with a,b
		 * and c */
		Assert.assertTrue("Number of outgoing edges differs",vertices.size() == 3);
		Assert.assertTrue(vertices.contains(a));
		Assert.assertTrue(vertices.contains(b));
		Assert.assertTrue(vertices.contains(c));

		/* b shouldn't have any outgoing edges */
		iter = graph.getOutEdges(b);
		Assert.assertFalse(iter.hasNext());

		/* g has two ancestors, namely d and f */
		vertices.clear();
		iter = graph.getInEdges(g);
		while (iter.hasNext())
		{
			Edge<TestData> edge = iter.next();
			Assert.assertTrue(edge.getDest() == g);
			vertices.add(edge.getSource());
		}
		Assert.assertTrue("Number of ingoing edges differs",vertices.size() == 2);
		Assert.assertTrue(vertices.contains(d));
		Assert.assertTrue(vertices.contains(f));

		/* c has two descendants called d and e */
		vertices.clear();
		iter = graph.getOutEdges(c);
		while (iter.hasNext())
		{
			Edge<TestData> edge = iter.next();
			Assert.assertTrue(edge.getSource() == c);
			vertices.add(edge.getDest());
		}
		Assert.assertTrue("Number of outgoing edges differs",vertices.size() == 2);
		Assert.assertTrue(vertices.contains(d));
		Assert.assertTrue(vertices.contains(e));

		/* and c has one ancestor, the root */
		iter = graph.getInEdges(c);
		Assert.assertTrue(iter.hasNext());
		Assert.assertTrue(iter.next().getSource() == root);

		/* b has two ancestors, the root but also a */
		vertices.clear();
		iter = graph.getInEdges(b);
		while (iter.hasNext())
		{
			Edge<TestData> edge = iter.next();
			Assert.assertTrue(edge.getDest() == b);
			vertices.add(edge.getSource());
		}
		Assert.assertTrue("Number of ingoing edges differs",vertices.size() == 2);
		Assert.assertTrue(vertices.contains(root));
		Assert.assertTrue(vertices.contains(a));

		/* Shortest path stuff */
		final HashMap<TestData,Integer> distanceMap = new HashMap<TestData,Integer>();
		distanceMap.put(root,0);
		distanceMap.put(a,1);
		distanceMap.put(b,1);
		distanceMap.put(c,1);
		distanceMap.put(d,2);
		distanceMap.put(e,2);
		distanceMap.put(f,3);
		distanceMap.put(g,3);

		graph.singleSourceShortestPath(root,false,new IDistanceVisitor<TestData>(){

			public boolean visit(TestData vertex, List<TestData> path, int distance)
			{
				Assert.assertTrue(distanceMap.get(vertex) == distance);
				return true;
			}
		});


		HashSet<TestData> sub = new HashSet<TestData>();
		sub.add(root);
		sub.add(b);
		sub.add(e);
		sub.add(f);

		DirectedGraph<TestData> subGraph = graph.transitivitySubGraph(sub);
		try {
			subGraph.writeDOT(new FileOutputStream(new File("sub.dot")), new DotAttributesProvider<TestData>(){
				@Override
				public String getDotNodeAttributes(TestData vt) {
					return "label=\""+vt.id + "\"";
				}
			});
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}


	}
}
