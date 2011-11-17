package sonumina.math.graph;

import java.util.ArrayList;
import java.util.HashMap;

import junit.framework.TestCase;

class VertexData
{
	String name;

	VertexData(){ };
	VertexData(String s)
	{
		this.name = s;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		VertexData other = (VertexData) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
}


public class GraphMeasureTest extends TestCase {

	public void testGetNeighbourhoodConnectivity()
	{
		DirectedGraph<VertexData> G = new DirectedGraph<VertexData>();

		VertexData a = new VertexData("a");
		VertexData b = new VertexData("b");
		VertexData c = new VertexData("c");
		VertexData d = new VertexData("d");
		VertexData e = new VertexData("e");

		G.addVertex(a);
		G.addVertex(b);
		G.addVertex(c);
		G.addVertex(d);
		G.addVertex(e);

		G.addEdge( new Edge<VertexData>(a, b) );

		G.addEdge( new Edge<VertexData>(b, a) );
		G.addEdge( new Edge<VertexData>(b, c) );
		G.addEdge( new Edge<VertexData>(b, d) );

		G.addEdge( new Edge<VertexData>(c, b) );
		G.addEdge( new Edge<VertexData>(c, e) );
		G.addEdge( new Edge<VertexData>(c, d) );

		G.addEdge( new Edge<VertexData>(d, e) );
		G.addEdge( new Edge<VertexData>(d, b) );
		G.addEdge( new Edge<VertexData>(d, c) );

		G.addEdge( new Edge<VertexData>(e, c) );
		G.addEdge( new Edge<VertexData>(e, d) );

		/*		   c
		 * 		 / |  \
		 * a - b   |   e
		 * 		 \ |  /
		 *         d
		 */

		double ac = G.getNeighbourhoodConnectivity(a);
		double bc = G.getNeighbourhoodConnectivity(b);
		double cc = G.getNeighbourhoodConnectivity(c);
		double dc = G.getNeighbourhoodConnectivity(d);
		double ec = G.getNeighbourhoodConnectivity(e);

		System.out.println("A " + ac);
		System.out.println("B " + bc);
		System.out.println("C " + cc);
		System.out.println("D " + dc);
		System.out.println("E " + ec);

		assertEquals(3.0, ac, 0.0);
		assertEquals(7.0/3.0, bc, 0.0001);
		assertEquals(8.0/3.0, cc, 0.0001);
		assertEquals(8.0/3.0, dc, 0.0001);
		assertEquals(6.0/2.0, ec, 0.0);

		DirectedGraph<VertexData> g2 = new DirectedGraph<VertexData>();

		g2.addVertex(a);
		g2.addVertex(b);
		g2.addVertex(c);
		g2.addVertex(d);
		g2.addVertex(e);

		/*
		 *  a \	  / d
		 *  |	c 	|
		 *  b /	  \ e
		 */
		g2.addEdge( new Edge<VertexData>(a, b) );
		g2.addEdge( new Edge<VertexData>(b, a) );

		g2.addEdge( new Edge<VertexData>(a, c) );
		g2.addEdge( new Edge<VertexData>(c, a) );

		g2.addEdge( new Edge<VertexData>(b, c) );
		g2.addEdge( new Edge<VertexData>(c, b) );

		g2.addEdge( new Edge<VertexData>(c, d) );
		g2.addEdge( new Edge<VertexData>(d, c) );

		g2.addEdge( new Edge<VertexData>(c, e) );
		g2.addEdge( new Edge<VertexData>(e, c) );

		g2.addEdge( new Edge<VertexData>(d, e) );
		g2.addEdge( new Edge<VertexData>(e, d) ) ;

		ac = g2.getNeighbourhoodConnectivity(a);
		bc = g2.getNeighbourhoodConnectivity(b);
		cc = g2.getNeighbourhoodConnectivity(c);
		dc = g2.getNeighbourhoodConnectivity(d);
		ec = g2.getNeighbourhoodConnectivity(e);

		System.out.println();
		System.out.println("A2 " + ac);
		System.out.println("B2 " + bc);
		System.out.println("C2 " + cc);
		System.out.println("D2 " + dc);
		System.out.println("E2 " + ec);

		assertEquals(6.0/2.0, ac, 0.0);
		assertEquals(6.0/2.0, bc, 0.0);
		assertEquals(8.0/4.0, cc, 0.00001);
		assertEquals(6.0/2.0, dc, 0.0);
		assertEquals(6.0/2.0, ec, 0.0);

		DirectedGraph<VertexData> g3 = new DirectedGraph<VertexData>();

		g3.addVertex(a);
		g3.addVertex(b);
		g3.addVertex(c);
		g3.addVertex(d);
		g3.addVertex(e);

		// a - b - c - d - e
		g3.addEdge( new Edge<VertexData>(a, b) );
		g3.addEdge( new Edge<VertexData>(b, a) );
		g3.addEdge( new Edge<VertexData>(b, c) );
		g3.addEdge( new Edge<VertexData>(c, b) );
		g3.addEdge( new Edge<VertexData>(c, d) );
		g3.addEdge( new Edge<VertexData>(d, c) );
		g3.addEdge( new Edge<VertexData>(d, e) );
		g3.addEdge( new Edge<VertexData>(e, d) );

		assertEquals(2, g3.getNeighbourhoodConnectivity(a), 0.0);
		assertEquals(2.0, g3.getNeighbourhoodConnectivity(c), 0.0);

		// a \
		// |  c - d
		// | /
		// b - -  e

		DirectedGraph<VertexData> g4 = new DirectedGraph<VertexData>();

		g4.addVertex(a);
		g4.addVertex(b);
		g4.addVertex(c);
		g4.addVertex(d);
		g4.addVertex(e);

		g4.addEdge( new Edge<VertexData>(a, b) );
		g4.addEdge( new Edge<VertexData>(b, a) );

		g4.addEdge( new Edge<VertexData>(a, c) );
		g4.addEdge( new Edge<VertexData>(c, a) );

		g4.addEdge( new Edge<VertexData>(b, c) );
		g4.addEdge( new Edge<VertexData>(c, b) );

		g4.addEdge( new Edge<VertexData>(c, d) );
		g4.addEdge( new Edge<VertexData>(d, c) );

		g4.addEdge( new Edge<VertexData>(b, e) );
		g4.addEdge( new Edge<VertexData>(e, b) );

		assertEquals(6.0/2.0, g4.getNeighbourhoodConnectivity(a), 0.0);
		assertEquals(6.0/3.0, g4.getNeighbourhoodConnectivity(b), 0.0);
		assertEquals(6.0/3.0, g4.getNeighbourhoodConnectivity(c), 0.0);
		assertEquals(3.0, g4.getNeighbourhoodConnectivity(d), 0.0);
		assertEquals(3.0, g4.getNeighbourhoodConnectivity(e), 0.0);

/*		G.singleSourceShortestPath(a, true, new DirectedGraph.IDistanceVisitor<VertexData>() {

			@Override
			public boolean visit(VertexData vertex, List<VertexData> path,
					int distance) {
				System.out.println(vertex.name + " dist: "+ distance);
				return true;
			}
		});*/
	}

	public void testGetSharedNeighbours()
	{
		DirectedGraph<VertexData> G = new DirectedGraph<VertexData>();

		VertexData a = new VertexData("a");
		VertexData b = new VertexData("b");
		VertexData c = new VertexData("c");
		VertexData d = new VertexData("d");
		VertexData e = new VertexData("e");

		G.addVertex(a);
		G.addVertex(b);
		G.addVertex(c);
		G.addVertex(d);
		G.addVertex(e);

		G.addEdge( new Edge<VertexData>(a, b) );

		G.addEdge( new Edge<VertexData>(b, a) );
		G.addEdge( new Edge<VertexData>(b, c) );
		G.addEdge( new Edge<VertexData>(b, d) );

		G.addEdge( new Edge<VertexData>(c, b) );
		G.addEdge( new Edge<VertexData>(c, e) );
		G.addEdge( new Edge<VertexData>(c, d) );

		G.addEdge( new Edge<VertexData>(d, e) );
		G.addEdge( new Edge<VertexData>(d, b) );
		G.addEdge( new Edge<VertexData>(d, c) );

		G.addEdge( new Edge<VertexData>(e, c) );
		G.addEdge( new Edge<VertexData>(e, d) );

		/*		   c
		 * 		 / |  \
		 * a - b   |   e
		 * 		 \ |  /
		 *         d
		 */

		assertEquals(0, G.getSharedNeighbours(a, b).size());
		assertEquals(2, G.getSharedNeighbours(b, e).size());
		assertEquals(2, G.getSharedNeighbours(d, c).size());

		DirectedGraph<VertexData> g2 = new DirectedGraph<VertexData>();

		g2.addVertex(a);
		g2.addVertex(b);
		g2.addVertex(c);
		g2.addVertex(d);
		g2.addVertex(e);

		/*
		 *  a \	  / d
		 *  |	c 	|
		 *  b /	  \ e
		 */
		g2.addEdge( new Edge<VertexData>(a, b) );
		g2.addEdge( new Edge<VertexData>(b, a) );

		g2.addEdge( new Edge<VertexData>(a, c) );
		g2.addEdge( new Edge<VertexData>(c, a) );

		g2.addEdge( new Edge<VertexData>(b, c) );
		g2.addEdge( new Edge<VertexData>(c, b) );

		g2.addEdge( new Edge<VertexData>(c, d) );
		g2.addEdge( new Edge<VertexData>(d, c) );

		g2.addEdge( new Edge<VertexData>(c, e) );
		g2.addEdge( new Edge<VertexData>(e, c) );

		g2.addEdge( new Edge<VertexData>(d, e) );
		g2.addEdge( new Edge<VertexData>(e, d) ) ;

		assertEquals(1, g2.getSharedNeighbours(a, b).size());
		assertEquals(1, g2.getSharedNeighbours(c, e).size());
		assertEquals(1, g2.getSharedNeighbours(b, e).size());

		DirectedGraph<VertexData> g3 = new DirectedGraph<VertexData>();

		g3.addVertex(a);
		g3.addVertex(b);
		g3.addVertex(c);
		g3.addVertex(d);
		g3.addVertex(e);

		// a - b - c - d - e
		g3.addEdge( new Edge<VertexData>(a, b) );
		g3.addEdge( new Edge<VertexData>(b, a) );
		g3.addEdge( new Edge<VertexData>(b, c) );
		g3.addEdge( new Edge<VertexData>(c, b) );
		g3.addEdge( new Edge<VertexData>(c, d) );
		g3.addEdge( new Edge<VertexData>(d, c) );
		g3.addEdge( new Edge<VertexData>(d, e) );
		g3.addEdge( new Edge<VertexData>(e, d) );

		assertEquals(1, g3.getSharedNeighbours(c, e).size());
		assertEquals(0,  g3.getSharedNeighbours(c, d).size());

		// a \
		// |  c - d
		// | /
		// b - -  e

		DirectedGraph<VertexData> g4 = new DirectedGraph<VertexData>();

		g4.addVertex(a);
		g4.addVertex(b);
		g4.addVertex(c);
		g4.addVertex(d);
		g4.addVertex(e);

		g4.addEdge( new Edge<VertexData>(a, b) );
		g4.addEdge( new Edge<VertexData>(b, a) );

		g4.addEdge( new Edge<VertexData>(a, c) );
		g4.addEdge( new Edge<VertexData>(c, a) );

		g4.addEdge( new Edge<VertexData>(b, c) );
		g4.addEdge( new Edge<VertexData>(c, b) );

		g4.addEdge( new Edge<VertexData>(c, d) );
		g4.addEdge( new Edge<VertexData>(d, c) );

		g4.addEdge( new Edge<VertexData>(b, e) );
		g4.addEdge( new Edge<VertexData>(e, b) );

		assertEquals(1, g4.getSharedNeighbours(b, c).size());
		assertEquals(0, g4.getSharedNeighbours(d, c).size());

		DirectedGraph<VertexData> g5 = new DirectedGraph<VertexData>();

		g5.addVertex(a);
		g5.addVertex(b);
		g5.addVertex(c);
		g5.addVertex(d);
		g5.addVertex(e);

		//     e
		//   /   \
		// a - c - b
		//   \   /
		//     d

		g5.addEdge( new Edge<VertexData>(a, c) );
		g5.addEdge( new Edge<VertexData>(c, a) );
		g5.addEdge( new Edge<VertexData>(a, d) );
		g5.addEdge( new Edge<VertexData>(d, a) );
		g5.addEdge( new Edge<VertexData>(a, e) );
		g5.addEdge( new Edge<VertexData>(e, a) );

		g5.addEdge( new Edge<VertexData>(b, c) );
		g5.addEdge( new Edge<VertexData>(c, b) );
		g5.addEdge( new Edge<VertexData>(b, d) );
		g5.addEdge( new Edge<VertexData>(d, b) );
		g5.addEdge( new Edge<VertexData>(b, e) );
		g5.addEdge( new Edge<VertexData>(e, b) );

		assertEquals(3, g5.getSharedNeighbours(a, b).size());

	}

	public void testGetTopologicalCoefficient()
	{
		DirectedGraph<VertexData> G = new DirectedGraph<VertexData>();

		VertexData a = new VertexData("a");
		VertexData b = new VertexData("b");
		VertexData c = new VertexData("c");
		VertexData d = new VertexData("d");
		VertexData e = new VertexData("e");

		G.addVertex(a);
		G.addVertex(b);
		G.addVertex(c);
		G.addVertex(d);
		G.addVertex(e);

		G.addEdge( new Edge<VertexData>(a, b) );

		G.addEdge( new Edge<VertexData>(b, a) );
		G.addEdge( new Edge<VertexData>(b, c) );
		G.addEdge( new Edge<VertexData>(b, d) );

		G.addEdge( new Edge<VertexData>(c, b) );
		G.addEdge( new Edge<VertexData>(c, e) );
		G.addEdge( new Edge<VertexData>(c, d) );

		G.addEdge( new Edge<VertexData>(d, e) );
		G.addEdge( new Edge<VertexData>(d, b) );
		G.addEdge( new Edge<VertexData>(d, c) );

		G.addEdge( new Edge<VertexData>(e, c) );
		G.addEdge( new Edge<VertexData>(e, d) );

		/*		   c
		 * 		 / |  \
		 * a - b   |   e
		 * 		 \ |  /
		 *         d
		 */

		assertEquals(1.0, G.getTopologicalCoefficient(a), 0.0);
		assertEquals((double) 2/3, G.getTopologicalCoefficient(b), 0.0);
		assertEquals((double) 2/3, G.getTopologicalCoefficient(c), 0.0);
		assertEquals((double) 2/3, G.getTopologicalCoefficient(d), 0.0);
		assertEquals(1.0, G.getTopologicalCoefficient(e), 0.0);

		DirectedGraph<VertexData> g2 = new DirectedGraph<VertexData>();

		g2.addVertex(a);
		g2.addVertex(b);
		g2.addVertex(c);
		g2.addVertex(d);
		g2.addVertex(e);

		/*
		 *  a \	  / d
		 *  |	c 	|
		 *  b /	  \ e
		 */
		g2.addEdge( new Edge<VertexData>(a, b) );
		g2.addEdge( new Edge<VertexData>(b, a) );

		g2.addEdge( new Edge<VertexData>(a, c) );
		g2.addEdge( new Edge<VertexData>(c, a) );

		g2.addEdge( new Edge<VertexData>(b, c) );
		g2.addEdge( new Edge<VertexData>(c, b) );

		g2.addEdge( new Edge<VertexData>(c, d) );
		g2.addEdge( new Edge<VertexData>(d, c) );

		g2.addEdge( new Edge<VertexData>(c, e) );
		g2.addEdge( new Edge<VertexData>(e, c) );

		g2.addEdge( new Edge<VertexData>(d, e) );
		g2.addEdge( new Edge<VertexData>(e, d) ) ;

		assertEquals(0.75, g2.getTopologicalCoefficient(a), 0.0);
		assertEquals(0.75, g2.getTopologicalCoefficient(b), 0.0);
		assertEquals(0.5, g2.getTopologicalCoefficient(c), 0.0);
		assertEquals(0.75, g2.getTopologicalCoefficient(d), 0.0);
		assertEquals(0.75, g2.getTopologicalCoefficient(e), 0.0);

		DirectedGraph<VertexData> g3 = new DirectedGraph<VertexData>();

		g3.addVertex(a);
		g3.addVertex(b);
		g3.addVertex(c);
		g3.addVertex(d);
		g3.addVertex(e);

		// a - b - c - d - e
		g3.addEdge( new Edge<VertexData>(a, b) );
		g3.addEdge( new Edge<VertexData>(b, a) );
		g3.addEdge( new Edge<VertexData>(b, c) );
		g3.addEdge( new Edge<VertexData>(c, b) );
		g3.addEdge( new Edge<VertexData>(c, d) );
		g3.addEdge( new Edge<VertexData>(d, c) );
		g3.addEdge( new Edge<VertexData>(d, e) );
		g3.addEdge( new Edge<VertexData>(e, d) );

		assertEquals(1.0, g3.getTopologicalCoefficient(a), 0.0);
		assertEquals(0.5, g3.getTopologicalCoefficient(c), 0.0);

		// a \
		// |  c - d
		// | /
		// b - -  e

		DirectedGraph<VertexData> g4 = new DirectedGraph<VertexData>();

		g4.addVertex(a);
		g4.addVertex(b);
		g4.addVertex(c);
		g4.addVertex(d);
		g4.addVertex(e);

		g4.addEdge( new Edge<VertexData>(a, b) );
		g4.addEdge( new Edge<VertexData>(b, a) );

		g4.addEdge( new Edge<VertexData>(a, c) );
		g4.addEdge( new Edge<VertexData>(c, a) );

		g4.addEdge( new Edge<VertexData>(b, c) );
		g4.addEdge( new Edge<VertexData>(c, b) );

		g4.addEdge( new Edge<VertexData>(c, d) );
		g4.addEdge( new Edge<VertexData>(d, c) );

		g4.addEdge( new Edge<VertexData>(b, e) );
		g4.addEdge( new Edge<VertexData>(e, b) );

		assertEquals(0.75, g4.getTopologicalCoefficient(a), 0.0);
		assertEquals((double) 5/9, g4.getTopologicalCoefficient(b), 0.0); //(5/3) / 3
		assertEquals((double) 5/9, g4.getTopologicalCoefficient(c), 0.0);
		assertEquals(1.0, g4.getTopologicalCoefficient(d), 0.0);
		assertEquals(1.0, g4.getTopologicalCoefficient(e), 0.0);

		DirectedGraph<VertexData> g5 = new DirectedGraph<VertexData>();

		g5.addVertex(a);
		g5.addVertex(b);
		g5.addVertex(c);
		g5.addVertex(d);
		g5.addVertex(e);

		//     e
		//   /   \
		// a - c - b
		//   \   /
		//     d

		g5.addEdge( new Edge<VertexData>(a, c) );
		g5.addEdge( new Edge<VertexData>(c, a) );
		g5.addEdge( new Edge<VertexData>(a, d) );
		g5.addEdge( new Edge<VertexData>(d, a) );
		g5.addEdge( new Edge<VertexData>(a, e) );
		g5.addEdge( new Edge<VertexData>(e, a) );

		g5.addEdge( new Edge<VertexData>(b, c) );
		g5.addEdge( new Edge<VertexData>(c, b) );
		g5.addEdge( new Edge<VertexData>(b, d) );
		g5.addEdge( new Edge<VertexData>(d, b) );
		g5.addEdge( new Edge<VertexData>(b, e) );
		g5.addEdge( new Edge<VertexData>(e, b) );

		assertEquals(1.0, g5.getTopologicalCoefficient(a), 0.0);
		assertEquals(1.0, g5.getTopologicalCoefficient(c), 0.0);

		DirectedGraph<VertexData> g6 = new DirectedGraph<VertexData>();

		g6.addVertex(a);
		g6.addVertex(b);
		g6.addVertex(c);
		g6.addVertex(d);
		g6.addVertex(e);

		// a - b - c - e
		//      \  |  /
		//         d

		g6.addEdge( new Edge<VertexData>(a, b) );

		g6.addEdge( new Edge<VertexData>(b, a) );
		g6.addEdge( new Edge<VertexData>(b, c) );
		g6.addEdge( new Edge<VertexData>(b, d) );

		g6.addEdge( new Edge<VertexData>(c, b) );
		g6.addEdge( new Edge<VertexData>(c, e) );
		g6.addEdge( new Edge<VertexData>(c, d) );

		g6.addEdge( new Edge<VertexData>(d, e) );
		g6.addEdge( new Edge<VertexData>(d, b) );
		g6.addEdge( new Edge<VertexData>(d, c) );

		g6.addEdge( new Edge<VertexData>(e, c) );
		g6.addEdge( new Edge<VertexData>(e, d) );

		assertEquals(1.0, g6.getTopologicalCoefficient(a), 0.0);
		assertEquals((double) 2/3, g6.getTopologicalCoefficient(b), 0.0);
		assertEquals((double) 2/3, g6.getTopologicalCoefficient(c), 0.0);
		assertEquals((double) 2/3, g6.getTopologicalCoefficient(d), 0.0);
		assertEquals(1.0, g6.getTopologicalCoefficient(e), 0.0);
	}

	public void testDegreeDistributions()
	{
		DirectedGraph<VertexData> G = new DirectedGraph<VertexData>();

		VertexData a = new VertexData("a");
		VertexData b = new VertexData("b");
		VertexData c = new VertexData("c");
		VertexData d = new VertexData("d");
		VertexData e = new VertexData("e");
		VertexData f = new VertexData("f");
		VertexData g = new VertexData("g");

		G.addVertex(a);
		G.addVertex(b);
		G.addVertex(c);
		G.addVertex(d);
		G.addVertex(e);
		G.addVertex(f);
		G.addVertex(g);

		G.addEdge( new Edge<VertexData>(a, b) );
		G.addEdge( new Edge<VertexData>(a, c) );

		G.addEdge( new Edge<VertexData>(b, a) );
		G.addEdge( new Edge<VertexData>(b, c) );
		G.addEdge( new Edge<VertexData>(b, d) );
		G.addEdge( new Edge<VertexData>(b, e) );
		G.addEdge( new Edge<VertexData>(b, f) );
		G.addEdge( new Edge<VertexData>(b, g) );

		G.addEdge( new Edge<VertexData>(c, a) );
		G.addEdge( new Edge<VertexData>(c, b) );

		G.addEdge( new Edge<VertexData>(d, b) );
		G.addEdge( new Edge<VertexData>(d, e) );

		G.addEdge( new Edge<VertexData>(e, b) );
		G.addEdge( new Edge<VertexData>(e, d) );

		G.addEdge( new Edge<VertexData>(f, b) );
		G.addEdge( new Edge<VertexData>(g, b) );

		//deg: a = c = d = e = 2; f = g = 1; b = 6;
		int chk = 0; //target value 3 if all degrees are correct
		for(int key : G.getDegreeDistribution().keySet())
		{
			int val = G.getDegreeDistribution().get(key);
			if(key == 2 && val == 4)
				++chk;
			else if(key == 1 && val == 2)
				++chk;
			else if(key == 6 && val == 1)
				++chk;
		}
		assertEquals(3, chk);

		// a \
		// |  c - d
		// | /
		// b - -  e

		DirectedGraph<VertexData> g4 = new DirectedGraph<VertexData>();

		g4.addVertex(a);
		g4.addVertex(b);
		g4.addVertex(c);
		g4.addVertex(d);
		g4.addVertex(e);

		g4.addEdge( new Edge<VertexData>(a, b) );
		g4.addEdge( new Edge<VertexData>(b, a) );

		g4.addEdge( new Edge<VertexData>(a, c) );
		g4.addEdge( new Edge<VertexData>(c, a) );

		g4.addEdge( new Edge<VertexData>(b, c) );
		g4.addEdge( new Edge<VertexData>(c, b) );

		g4.addEdge( new Edge<VertexData>(c, d) );
		g4.addEdge( new Edge<VertexData>(d, c) );

		g4.addEdge( new Edge<VertexData>(b, e) );
		g4.addEdge( new Edge<VertexData>(e, b) );

		HashMap<Integer, Integer> map = g4.getDegreeDistribution();
		assertEquals(2, (int) map.get(3)); //2 nodes w/ degree 3
		assertEquals(1, (int) map.get(2));
		assertEquals(2, (int) map.get(1));

	}
}
