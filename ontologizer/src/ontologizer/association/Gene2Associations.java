package ontologizer.association;

import java.util.*;

import ontologizer.ByteString;
import ontologizer.go.TermID;

/**
 * <P>
 * Gene2Associations objects store all the gene ontology associations for one
 * gene.
 * </P>
 * <P>
 * Note that duplicate entries are possible in the association files. For this
 * reason, we make sure there is only one entry for each GO:id number. We do
 * this by storing a list of all goIDs seen in the arrayList goIDs.
 * </P>
 * <P>
 * This class implements the Iterable interface, so you easly can iterate
 * over the associations to this gene.
 * 
 * @author Peter Robinson, Sebastian Bauer
 */

public class Gene2Associations implements Iterable<Association>
{
	/** Name of the gene for which this object stores 0 - n associations */
	private ByteString gene;

	/** List of GO functional annotations */
	private ArrayList<Association> associations;

	/* Use to keep a running list of GO ids seen to avoid duplicate entries */
	private ArrayList<TermID> goIDs;

	public Gene2Associations(ByteString name)
	{
		associations = new ArrayList<Association>();
		goIDs = new ArrayList<TermID>();
		gene = name;
	}

	/**
	 * Add a new association to the gene.
	 * 
	 * @param a defines the association to be added.
	 */
	public void add(Association a)
	{
		/* Only add, if association is really assoicated with the gene */
		if (gene.equals(a.getObjectSymbol()))
		{
			/* avoid duplocates */
			if (goIDs.contains(a.getGoID())) return;

			goIDs.add(a.getGoID());
			associations.add(a);
		}
	}

	public ByteString name()
	{
		return gene;
	}

	/**
	 * Get an arraylist of all GO Ids to which this gene is directly
	 * annotated by extracting the information from the Association object(s)
	 * belonging to the gene.
	 */
	public ArrayList<TermID> getAssociations()
	{
		ArrayList<TermID> a = new ArrayList<TermID>();
		Iterator<Association> it = associations.iterator();
		while (it.hasNext())
		{
			Association assoc = it.next();
			a.add(assoc.getGoID());
		}
		return a;
	}

	public Iterator<Association> iterator()
	{
		return associations.iterator();
	}
}
