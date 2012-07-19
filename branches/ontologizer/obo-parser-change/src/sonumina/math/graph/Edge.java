/*
 * Created on 21.08.2005
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package sonumina.math.graph;

public class Edge<Type>
{
	private Type source;
	private Type dest;
	
	public Edge(Type source, Type dest)
	{
		this.source = source;
		this.dest = dest;
	}

	/**
	 * Returns the edge's destination.
	 * 
	 * @return
	 */
	public final Type getDest()
	{
		return dest;
	}

	/**
	 * Returns the edge's source.
	 * 
	 * @return
	 */
	public final Type getSource()
	{
		return source;
	}
	
	/**
	 * Returns the weight of an edge. The default implementation
	 * returns always 1 and hence must be overwritten by subclasses
	 * in order to return different weights.
	 * 
	 * @return
	 */
	public int getWeight()
	{
		return 1;
	}

	void setSource(Type source) {
		this.source = source;
	}

	void setDest(Type dest) {
		this.dest = dest;
	}
}
