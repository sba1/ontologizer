package ontologizer.association;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import ontologizer.go.TermID;
import ontologizer.types.ByteString;

/**
 * Set containing all swiss prot ids linked to affy ids and annotaions.
 *
 * @author sba
 */
class SwissProtAffyAnnotaionSet implements Iterable<SwissProtAffyAnnotation>
{
	private HashMap<ByteString,SwissProtAffyAnnotation> map;

	public SwissProtAffyAnnotaionSet()
	{
		map = new HashMap<ByteString,SwissProtAffyAnnotation>();
	}

	/**
	 * Add a new swissprot id -> affyID -> goID mappping.
	 *
	 * @param swissProtID
	 * @param affyID
	 * @param goIDs
	 */
	public void add(ByteString swissProtID, ByteString affyID, List<TermID> goIDs)
	{
		SwissProtAffyAnnotation an = map.get(swissProtID);
		if (an == null)
		{
			an = new SwissProtAffyAnnotation(swissProtID);
			map.put(swissProtID,an);
		}
		an.addAffyID(affyID);
		for (TermID id : goIDs)
			an.addTermID(id);
	}

	public Iterator<SwissProtAffyAnnotation> iterator()
	{
		return map.values().iterator();
	}
}
