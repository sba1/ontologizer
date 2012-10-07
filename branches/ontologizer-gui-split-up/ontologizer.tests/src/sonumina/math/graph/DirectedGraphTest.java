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
import sonumina.math.graph.AbstractGraph.INeighbourGrabber;
import sonumina.math.graph.AbstractGraph.IVisitor;
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
	public void testVertexRemove()
	{
		DirectedGraph<TestData> graph = new DirectedGraph<TestData>();
		TestData a = new TestData("a");
		TestData b = new TestData("b");
		TestData c = new TestData("c");
		TestData d = new TestData("d");

		/* Add some vertices */
		graph.addVertex(a);
		graph.addVertex(b);
		graph.addVertex(c);
		graph.addVertex(d);

		graph.addEdge(new Edge<TestData>(a,b));
		graph.addEdge(new Edge<TestData>(b,c));
		graph.addEdge(new Edge<TestData>(c,d));
		graph.addEdge(new Edge<TestData>(d,a));

		graph.removeVertex(a);
	}

	public void testGraph()
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

		DirectedGraph<TestData> subGraph = graph.pathMaintainingSubGraph(sub);
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

		final HashSet<TestData> visited = new HashSet<TestData>();

		final INeighbourGrabber<TestData> childGrabber = getChildNodeNeighbourGrabber(graph);
		class MyVisitor implements IVisitor<TestData>
		{
			private TestData prev;

			@Override
			public boolean visited(TestData vertex)
			{
				Assert.assertFalse(visited.contains(vertex));
				if (prev == e) Assert.assertEquals(f,vertex);
				if (prev == g) Assert.assertTrue(vertex == d || vertex == e);
				if (prev == a) Assert.assertTrue(vertex == b || vertex == c);

				visited.add(vertex);
				prev = vertex;
				return true;
			}
		};

		graph.dfs(root,childGrabber, new MyVisitor());
		Assert.assertEquals(graph.getNumberOfVertices(), visited.size());

		visited.clear();
		HashMap<TestData,TestData> shortCutLinks = graph.getDFSShotcutLinks(root, childGrabber, new MyVisitor());
		Assert.assertEquals(graph.getNumberOfVertices(), visited.size());

		Assert.assertEquals(null,shortCutLinks.get(root));
		Assert.assertEquals(c,shortCutLinks.get(a));
		Assert.assertEquals(c,shortCutLinks.get(b));
		Assert.assertEquals(null,shortCutLinks.get(c));
		Assert.assertEquals(e,shortCutLinks.get(d));
		Assert.assertEquals(null,shortCutLinks.get(e));
		Assert.assertEquals(null,shortCutLinks.get(f));
		Assert.assertEquals(e,shortCutLinks.get(g));
	}

	/**
	 * @param graph
	 * @return
	 */
	private INeighbourGrabber<TestData> getChildNodeNeighbourGrabber(
			final DirectedGraph<TestData> graph) {
		final INeighbourGrabber<TestData> childGrabber = new INeighbourGrabber<TestData>() {
			@Override
			public Iterator<TestData> grabNeighbours(TestData t)
			{
				return graph.getChildNodes(t);
			}};
		return childGrabber;
	}

	public void testShortLinksOnTree()
	{
		/*
		 * Build a graph like this
		 *
		 *       0
		 *      / \
		 *     /   \
		 *    1     4
		 *   / \   / \
		 *  2   3 5   6
		 *
		 */

		final DirectedGraph<TestData> graph = new DirectedGraph<TestData>();
		final TestData n0 = new TestData("n0");
		final TestData n1 = new TestData("n1");
		final TestData n2 = new TestData("n2");
		final TestData n3 = new TestData("n3");
		final TestData n4 = new TestData("n4");
		final TestData n5 = new TestData("n5");
		final TestData n6 = new TestData("n6");

		graph.addVertex(n0);
		graph.addVertex(n1);
		graph.addVertex(n2);
		graph.addVertex(n3);
		graph.addVertex(n4);
		graph.addVertex(n5);
		graph.addVertex(n6);

		graph.addEdge(new Edge<TestData>(n0,n1));
		graph.addEdge(new Edge<TestData>(n1,n2));
		graph.addEdge(new Edge<TestData>(n1,n3));
		graph.addEdge(new Edge<TestData>(n0,n4));
		graph.addEdge(new Edge<TestData>(n4,n5));
		graph.addEdge(new Edge<TestData>(n4,n6));

		final INeighbourGrabber<TestData> childGrabber = getChildNodeNeighbourGrabber(graph);
		final HashSet<TestData> visited = new HashSet<TestData>();
		final IVisitor<TestData> visitor = new IVisitor<TestData>() {
			private TestData prev;

			@Override
			public boolean visited(TestData vertex)
			{
				Assert.assertFalse(visited.contains(vertex));

				visited.add(vertex);
				prev = vertex;
				return true;
			}
		};
		HashMap<TestData,TestData> shortCutLinks = graph.getDFSShotcutLinks(n0, childGrabber, visitor);
		Assert.assertEquals(graph.getNumberOfVertices(),shortCutLinks.keySet().size());

		Assert.assertNull(shortCutLinks.get(n0));
		Assert.assertTrue((shortCutLinks.get(n1) == n4 && shortCutLinks.get(n4) == null ) ||
				          (shortCutLinks.get(n4) == n1 && shortCutLinks.get(n1) == null));

		if (shortCutLinks.get(n2) == n3)
			Assert.assertTrue(shortCutLinks.get(n3) == n4 || shortCutLinks.get(n3) == null);
		else
		{
			/* Can happen but did not occur yet */
			Assert.assertFalse(true);
		}
	}
}
