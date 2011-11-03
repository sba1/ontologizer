package ontologizer.enumeration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;

import ontologizer.enumeration.GOTermEnumerator.GOTermAnnotatedGenes;
import ontologizer.go.TermID;
import ontologizer.types.ByteString;

/**
 * Stores which items are annotated to which terms.
 * 
 * @author Sebastian Bauer
 */
public class ItemEnumerator implements Iterable<ByteString>
{
	private ItemEnumerator() { }
	
	private HashMap<ByteString,ArrayList<TermID>> items2Terms;
	private HashMap<ByteString,ArrayList<TermID>> items2DirectTerms;

	public ArrayList<TermID> getTermsAnnotatedToTheItem(ByteString item)
	{
		return items2Terms.get(item);
	}
	
	public ArrayList<TermID> getTermsDirectlyAnnotatedToTheItem(ByteString item)
	{
		return items2DirectTerms.get(item);
	}

	/**
	 * Returns all used term ids.
	 * 
	 * @return
	 */
	public ArrayList<TermID> getAllTermIDs()
	{
		LinkedHashSet<TermID> allTermIDs = new LinkedHashSet<TermID>();
		for (ArrayList<TermID> tids : items2Terms.values())
			allTermIDs.addAll(tids);
		return new ArrayList<TermID>(allTermIDs);
	}

	/**
	 * Create an item enumerator from a term enumerator. 
	 * 
	 * @param termEnumerator
	 * @return
	 */
	public static ItemEnumerator createFromTermEnumerator(GOTermEnumerator termEnumerator)
	{
		HashMap<ByteString,ArrayList<TermID>> items2Terms = new HashMap<ByteString,ArrayList<TermID>>();
		HashMap<ByteString,ArrayList<TermID>> items2DirectTerms = new HashMap<ByteString,ArrayList<TermID>>();

		for (TermID tid : termEnumerator)
		{
			GOTermAnnotatedGenes genes = termEnumerator.getAnnotatedGenes(tid);

			for (ByteString g : genes.totalAnnotated)
			{
				ArrayList<TermID> al = items2Terms.get(g);
				if (al == null)
				{
					al = new ArrayList<TermID>();
					items2Terms.put(g, al);
				}

				al.add(tid);
			}
			
			for (ByteString g : genes.directAnnotated)
			{
				ArrayList<TermID> al = items2DirectTerms.get(g);
				if (al == null)
				{
					al = new ArrayList<TermID>();
					items2DirectTerms.put(g, al);
				}

				al.add(tid);
			}

		}
		
		ItemEnumerator itemEnum = new ItemEnumerator();
		itemEnum.items2Terms = items2Terms;
		itemEnum.items2DirectTerms = items2DirectTerms;
		
		return itemEnum;
	}
	
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		
		for (ByteString gene : items2Terms.keySet())
		{
			builder.append(gene);
			builder.append(": ");

			for (TermID tid : items2Terms.get(gene))
			{
				builder.append(tid.toString());
				builder.append(",");
			}
			builder.append("\n");
		}

		return builder.toString();
	}

	public Iterator<ByteString> iterator()
	{
		return items2Terms.keySet().iterator();
	}
}


