package ontologizer.enumeration;

import ontologizer.go.TermID;

/**
 * AssociationCounter.java This class is intended to store information about GO
 * association counts.
 * <P>
 * We want to know 1) How often was an annotation seen in a cluster, 2) what is
 * the aspect
 * </P>
 * <P>
 * Objects of this class will be used for implicit counts. Note that "NOT"
 * counts are not recorded.
 * </P>
 * 
 * @author Peter Robinson
 * @version 0.15 2005-02-21
 */
public class AssociationCounter implements Comparable
{
	/**
	 * Number of times the GO term indicated by this.name and this.accession was
	 * seen in the present group (cluster) of genes/gene products. This is the
	 * **TOTAL** count including both explicit and implicit annotations
	 */
	private int count;

	/** This is the direct annotation count only */
	private int directCount;

	/** Corresponds to GO:id (e.g., GO:0001234). */
	private TermID id;

	/** This constructor is intended to be used for explicit annotations */
	public AssociationCounter(TermID id)
	{
		this.id = id;
		this.count = 0;
		this.directCount = 0;

	}

	public void incrementCount()
	{
		count++;
	}

	public void incrementDirectCount()
	{
		directCount++;
	}

	/** Compare on the basis of total counts (direct and implied). */
	public int compareTo(Object other)
	{
		AssociationCounter ac = (AssociationCounter) other;
		if (this.count > ac.count)
			return -1;
		if (this.count < ac.count)
			return +1;
		return 0;
	}

	public boolean equals(Object other)
	{
		AssociationCounter ac = (AssociationCounter) other;
		return ((this.id == ac.id) && (this.count == ac.count));
	}

	public int getDirectCount()
	{
		return directCount;
	}

	public int getCount()
	{
		return count;
	}

	public TermID getID()
	{
		return id;
	}
}
