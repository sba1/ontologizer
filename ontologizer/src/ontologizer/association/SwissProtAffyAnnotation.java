package ontologizer.association;

import java.util.Collection;
import java.util.HashSet;

import ontologizer.ontology.TermID;
import ontologizer.types.ByteString;

/**
 * This class stores a single swiss prot id it's affymetrix probes
 * and their GO annotations.
 *
 * @author sba
 *
 */
class SwissProtAffyAnnotation
{
	private ByteString swissProtID;
	private HashSet<ByteString> affyIDs;
	private HashSet<TermID> goIDs;

	public SwissProtAffyAnnotation(ByteString newSwissProtID)
	{
		swissProtID = newSwissProtID;
		affyIDs = new HashSet<ByteString>();
		goIDs = new HashSet<TermID>();
	}

	public void addAffyID(ByteString affyID)
	{
		affyIDs.add(affyID);
	}

	public void addTermID(TermID id)
	{
		goIDs.add(id);
	}

	public ByteString getSwissProtID()
	{
		return swissProtID;
	}

	public Collection<TermID> getGOIDs()
	{
		return goIDs;
	}

	public Collection<ByteString> getAffyIDs()
	{
		return affyIDs;
	}
}
