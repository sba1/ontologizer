package sonumina.math.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

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

	/*
	public void testGetNeighbourhoodConnectivity()
	{
		DirectedGraph<VertexData> G = new DirectedGraph<VertexData>();
		Random r = new Random();
		int numOfNodes = r.nextInt(2000) + 1; //[1,200]
		//VertexData[] nodes = new VertexData[numOfNodes];
		int[] numOfConns = new int[numOfNodes];
		ArrayList<VertexData> usedNodes = new ArrayList<VertexData>();
		
		for(int i = 0; i < numOfNodes; i++)
		{
			VertexData v = new VertexData(Integer.toString(i));
			G.addVertex( v );
		}
		
		Iterable<VertexData> It = G.getVertices();
		//set up connections
		for(VertexData v: It)
		{
			usedNodes.clear();
			
			int numOfChild = r.nextInt(numOfNodes - 1); //exclude loops
			int gain = Math.abs(numOfChild - numOfConns[Integer.parseInt(v.name)]);
			numOfConns[Integer.parseInt(v.name)] += gain;
			
			while(usedNodes.size() < gain)
			{
				int nextChild = r.nextInt(numOfNodes);
				VertexData u = new VertexData(Integer.toString(nextChild));
				if( (!G.hasEdge(v, u) && !G.hasEdge(u, v) ) && nextChild != Integer.parseInt(v.name))
				{
					G.addEdge( new Edge<VertexData>(v, u) );
					G.addEdge( new Edge<VertexData>(u, v) );
					--gain;
				}
			}
		}
		
		double nbConnectivity;
		for(VertexData vd : It)
		{
			Iterator<VertexData> neighbIter = G.getChildNodes(vd);
			int neighbConns = 0;
			while(neighbIter.hasNext())
			{
				neighbConns += G.getNumberOfOutEdges(neighbIter.next());
			}
			
			nbConnectivity = neighbConns / (double) numOfConns[Integer.parseInt(vd.name)];
			
			assertEquals(nbConnectivity, G.getNeighbourhoodConnectivity(vd),0.0);
		}
	}

	public void testGetSharedNeighbours() {
		DirectedGraph<VertexData> G = new DirectedGraph<VertexData>();
		
		//maybe better: fixed network size and only random number of connections
		Random r = new Random(7438);
		int numOfNodes = r.nextInt(2000) + 1; //[1,200]
		VertexData[] nodes = new VertexData[numOfNodes];
		
		int numOfCommonNodes = r.nextInt(numOfNodes); //subset of nodes excluding two 
		ArrayList<Integer> commonNodesIdx = new ArrayList<Integer>();
		
		for(int i = 0; i < numOfNodes; i++)
			nodes[i] = new VertexData(Integer.toString(i));
		
		int srcId = r.nextInt(numOfNodes); //a
		int destId = r.nextInt(numOfNodes); //b
		int cnidx;
		while(commonNodesIdx.size() < numOfCommonNodes)
		{
			cnidx = r.nextInt(numOfCommonNodes);
			if(!commonNodesIdx.contains(cnidx) && (cnidx != srcId || cnidx != destId))
				commonNodesIdx.add(cnidx);
		}
		
		for(VertexData v : nodes)
			G.addVertex(v);
		for(int idx : commonNodesIdx)
		{
			G.addEdge( new Edge<VertexData>(nodes[srcId], nodes[idx]));
			G.addEdge( new Edge<VertexData>(nodes[idx], nodes[srcId]));
			
			G.addEdge( new Edge<VertexData>(nodes[destId], nodes[idx]));
			G.addEdge( new Edge<VertexData>(nodes[idx], nodes[destId]));
		}
		
		/*
		VertexData a = new VertexData("a");
		VertexData b = new VertexData("b");
		VertexData c = new VertexData("c");
		VertexData d = new VertexData("d");
		VertexData e = new VertexData("e");
		VertexData f = new VertexData("f");
		
		G.addVertex(a);
		G.addVertex(b);
		G.addVertex(c);
		G.addVertex(d);
		G.addVertex(e);
		G.addVertex(f);
		
		G.addEdge( new Edge<VertexData>(a,c));
		G.addEdge( new Edge<VertexData>(a,d));
		G.addEdge( new Edge<VertexData>(a,e));
		G.addEdge( new Edge<VertexData>(a,f));
		
		G.addEdge( new Edge<VertexData>(b,c));
		G.addEdge( new Edge<VertexData>(b,d));
		G.addEdge( new Edge<VertexData>(b,e));
		G.addEdge( new Edge<VertexData>(b,f));

		ArrayList<VertexData> sb = G.getSharedNeighbours(nodes[srcId], nodes[destId]);
		System.out.println("Shared Neighbours Test");
		System.out.println(String.format("#Nodes: %d, Pair: (%d,%d), #Shared: %d", numOfNodes, srcId, destId, numOfCommonNodes));
		assertTrue("No shared Nodes found!", sb.size() > 0);
		assertEquals(commonNodesIdx.size(), sb.size());
	}
*/
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
		
		G.addEdge( new Edge<VertexData>(a, c) );
		G.addEdge( new Edge<VertexData>(a, d) );
		G.addEdge( new Edge<VertexData>(a, e) );
		G.addEdge( new Edge<VertexData>(b, c) );
		G.addEdge( new Edge<VertexData>(b, d) );
		G.addEdge( new Edge<VertexData>(b, e) );
		
		assert(G.getSharedNeighbours(a, b).size() == 3);
	}
	
	public void testGetBetweennessCentrality()
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
		G.addEdge( new Edge<VertexData>(b, c) );
		G.addEdge( new Edge<VertexData>(b, d) );
		G.addEdge( new Edge<VertexData>(c, e) );
		G.addEdge( new Edge<VertexData>(d, e) );
		
		/*		   c
		 * 		 /	  \
		 * a - b	   e
		 * 		 \    /
		 *         d
		 */		   
		
		HashMap<VertexData, Double> res = G.getBetweennessCentrality();
		assertEquals(0.583, res.get(b), 0.0001);
		
	}

}
