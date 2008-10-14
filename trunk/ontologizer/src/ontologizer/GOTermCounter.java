package ontologizer;

import java.util.*;
import java.util.Map.Entry;

import ontologizer.go.GOGraph;
import ontologizer.go.Term;
import ontologizer.go.TermContainer;
import ontologizer.go.TermID;
import ontologizer.go.ParentTermID;

/**
 * This class encapsulates the counting of explicit and implicit annotations for
 * an entire study set. You can iterate conveniently over all GO terms where genes
 * have been annotated to.
 * 
 * @author Peter N. Robinson, Sebastian Bauer
 */

public class GOTermCounter implements Iterable<TermID>
{
	/**
	 * Explicit and total annotations to a given term in the biological process
	 * namespace; key: a GO:id, value: an Association Counter object.
	 */
	private HashMap<TermID, AssociationCounter> processHashMap;

	/** Molecular function */
	private HashMap<TermID, AssociationCounter> functionHashMap;

	/** Cellular component */
	private HashMap<TermID, AssociationCounter> componentHashMap;

	/**
	 * This object holds information about all terms and their ancestor
	 * relationships.
	 */
	private TermContainer GOterms;
	
	/**
	 * The graph of the ontology
	 */
	private GOGraph graph;

	public GOTermCounter(GOGraph g)
	{
		processHashMap = new HashMap<TermID, AssociationCounter>();
		functionHashMap = new HashMap<TermID, AssociationCounter>();
		componentHashMap = new HashMap<TermID, AssociationCounter>();
		
		graph = g;
		GOterms = g.getGoTermContainer();
	}

	/**
	 * @param ids
	 *            A list of all the GO terms to which <B>an individual gene</B>
	 *            is annotated directly.
	 */
	public void add(ArrayList<TermID> ids)
	{
		if (ids == null)
		{
			System.err.println("Error: Attempt to use empty "
					+ " set of annotations at GOTermCounter.add");
			System.exit(1);
		}

		Iterator<TermID> it;
		/** First add direct counts for ids */

		it = ids.iterator();
		while (it.hasNext())
		{
			TermID id = it.next();
			addDirect(id);
		}

		Stack<TermID> stack = new Stack<TermID>();
		HashSet<TermID> allTerms = new HashSet<TermID>();

		it = ids.iterator(); // reset
		while (it.hasNext())
		{
			TermID goid = it.next();
			allTerms.add(goid);
			Term gt = GOterms.get(goid);
			if (gt == null)
			{
				System.err.println("Error: Could not retrieve " + " info for "
						+ goid + " at GOTermCounter.add");
				System.exit(1);
			}
			gt.setParentIterator();
			while (gt.hasNext())
			{
				ParentTermID par = gt.next();
				stack.push(par.termid);
			}

			/*
			 * The stack now has all parents of gt Now go up the DAG to find
			 * parents of parents and so on.
			 */
			while (!stack.empty())
			{
				TermID acc = stack.pop();
				allTerms.add(acc);
				Term gt2 = GOterms.get(acc);
				if (gt2 == null)
				{
					System.err.println("[Ontologizer -- Noncritical error]: "
							+ "Could not find " + acc);
				} else
				{
					gt2.setParentIterator();
					while (gt2.hasNext())
					{
						ParentTermID par = gt2.next();
						stack.push(par.termid);
					}
				}
			} // while (! stack.empty() )
		} // while (it.hashNext() )

		/*
		 * When we get here, allTerms has a list of all terms that are directly
		 * or indirectly annotated to the present gene. Each of these terms now
		 * needs to be incremented by "1" and "1" only (i.e., avoid duplicate
		 * because of diamond shaped paths or two children of one term being
		 * annotated).
		 */
		Iterator<TermID> it2 = allTerms.iterator();
		while (it2.hasNext())
		{
			TermID id = it2.next();
			addTotal(id);
		}

	}

	/** Add direct annotation */
	private void addDirect(TermID id)
	{
		Term gt = GOterms.get(id);
		AssociationCounter ac = null;
		if (gt == null)
		{
			System.err.println("Error: GOTermCounter.addDirect"
					+ " Could not find " + id);
			System.exit(1);
		}
		switch (gt.getNamespace())
		{
			case	BIOLOGICAL_PROCESS: 
					if (processHashMap.containsKey(id))
					{
						ac = processHashMap.get(id);
						ac.incrementDirectCount();
					} else
					{
						ac = new AssociationCounter(id);
						ac.incrementDirectCount();
						processHashMap.put(id, ac);
					}
					break;

			case	MOLECULAR_FUNCTION:
					if (functionHashMap.containsKey(id))
					{
						ac = functionHashMap.get(id);
						ac.incrementDirectCount();
					} else
					{
						ac = new AssociationCounter(id);
						ac.incrementDirectCount();
						functionHashMap.put(id, ac);
					}
					break;

			case	CELLULAR_COMPONENT:
					if (componentHashMap.containsKey(id))
					{
						ac = componentHashMap.get(id);
						ac.incrementDirectCount();
					} else
					{
						ac = new AssociationCounter(id);
						ac.incrementDirectCount();
						componentHashMap.put(id, ac);
					}
					break;
		}
	}

	/** Add to total (direct + indirect) annotation */
	private void addTotal(TermID id)
	{
		Term gt = GOterms.get(id);
		AssociationCounter ac = null;
		if (gt == null)
		{
			System.err.println("Error: GOTermCounter.addDirect"
					+ " Could not find " + id);
			System.exit(1);
		}
		
		switch (gt.getNamespace())
		{
			case	BIOLOGICAL_PROCESS:
					if (processHashMap.containsKey(id))
					{
						ac = processHashMap.get(id);
						ac.incrementCount();
					} else
					{
						ac = new AssociationCounter(id);
						ac.incrementCount();
						processHashMap.put(id, ac);
					}
					break;

			case	MOLECULAR_FUNCTION:
					if (functionHashMap.containsKey(id))
					{
						ac = functionHashMap.get(id);
						ac.incrementCount();
					} else
					{
						ac = new AssociationCounter(id);
						ac.incrementCount();
						functionHashMap.put(id, ac);
					}
					break;

			case	CELLULAR_COMPONENT:
					if (componentHashMap.containsKey(id))
					{
						ac = componentHashMap.get(id);
						ac.incrementCount();
					} else
					{
						ac = new AssociationCounter(id);
						ac.incrementCount();
						componentHashMap.put(id, ac);
					}
					break;
		}
	}

	private AssociationCounter find(TermID id)
	{
		AssociationCounter ac = null;
		ac = processHashMap.get(id);
		if (ac == null)
		{
			ac = functionHashMap.get(id);
		}
		if (ac == null)
		{
			ac = componentHashMap.get(id);
		}

		return ac;

	}

	/**
	 * @return the count of direct annotations for GO term id.
	 * @param id
	 *            a GO:id, e.g., GO:0001234
	 */
	public int getDirectCount(TermID id)
	{
		AssociationCounter ac = find(id);
		if (ac == null)
		{
			System.out.println("Error: Could not find Association counts for "
					+ id);
			return 0;

		}
		return ac.getDirectCount();
	}

	/**
	 * @param id
	 *            a GO:id, e.g., GO:0001234
	 *
	 * @return the count of total annotations for the given
	 *         GO term id.
	 */
	public int getCount(TermID id)
	{
		AssociationCounter ac = find(id);
		if (ac == null)
			return 0;
		return ac.getCount();
	}

	public int getTotalNumberOfAnnotatedTerms()
	{
		int sum = processHashMap.size() + componentHashMap.size() + functionHashMap.size();
		return sum;

	}
	
	private void appendHashMap(StringBuilder str, HashMap<String, AssociationCounter> map)
	{
		Iterator<Entry<String,AssociationCounter>> iter = map.entrySet().iterator();
		while (iter.hasNext())
		{
			Entry<String,AssociationCounter> entry = iter.next();
			str.append(entry.getKey());
			str.append(" ");
			str.append(entry.getValue().getCount());
			str.append("\n");
		}
	}

/*
	public HashMap<String, AssociationCounter> getComponentHashMap()
	{
		return componentHashMap;
	}


	public HashMap<String, AssociationCounter> getFunctionHashMap()
	{
		return functionHashMap;
	}


	public HashMap<String, AssociationCounter> getProcessHashMap()
	{
		return processHashMap;
	}*/

	public Iterator<TermID> iterator()
	{
		/* TODO: This list built up is only temporarily because
		 * we have currently three separeted ontologies */
		LinkedList<TermID> nameList = new LinkedList<TermID>();
		
		for (TermID term : processHashMap.keySet())
			nameList.add(term);

		for (TermID term : functionHashMap.keySet())
			nameList.add(term);

		for (TermID term : componentHashMap.keySet())
			nameList.add(term);
		
		return nameList.iterator();
	}
}