package ontologizer.dotwriter;

import ontologizer.ontology.TermID;

/**
 * An interface for providing attributes for dot graph.
 *
 * @author sba
 */
public interface IDotAttributesProvider
{
	/** @return the dot attributes for the given term */
	public String getDotNodeAttributes(TermID id);

	/**
	 * @param id1 the term id of the source of the edge
	 * @param id2 the term id of the destination of the edge
	 * @return the dot attributes for the edge going from id1 to id2
	 */
	public String getDotEdgeAttributes(TermID id1, TermID id2);
}
