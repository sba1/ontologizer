package ontologizer.go;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


/**
 * This class provides a representation of individual GOTerms <BR />

 * Example: <BR />
 * [Term] <BR />
 * id: GO:0000018 <BR />
 * name: regulation of DNA recombination <BR />
 * namespace: biological_process <BR />
 * def: "Any process that modulates the frequency\, rate or extent of DNA
 * recombination\, the processes by which a new genotype is formed by
 * reassortment of genes resulting in gene combinations different from those
 * that were present in the parents." [GO:curators, ISBN:0198506732] <BR />
 * is_a: GO:0051052 (cardinality 0..n) <BR />
 * relationship: part_of GO:0006310 (cardinality 0..n) <BR />
 * <BR />
 * <P>
 * Both isa and part-of refer to child-parent relationships in the GO directed
 * acyclic graph. The Ontologizer does not distinguish between these types of
 * child-parent relationships, but rather places both type of parent in an
 * ArrayList termed parents. This will allow us to traverse the DAG while we are
 * tabulating the counts of functions found in a cluster
 * </P>
 *
 * @author Peter Robinson, Sebastian Bauer
 */

public class Term
{
	/** The id ("accession number") of this GO term */
	private TermID id;

	/** The short human readable name of the id */
	private String name;

	/** The definition of this term. This might be null if this information is not available */
	private String definition;

	/** The parents of the this term */
	private ParentTermID [] parents;

	/** The term's alternatives */
	private TermID [] alternatives;

	/** The term's subsets */
	private Subset [] subsets;

	/** The term's name space */
	private Namespace namespace;

	/** Whether term is declared as obsolete */
	private boolean obsolete;

	/**
	 * @param id
	 *            A term id.
	 * @param name
	 *            A string such as glutathione dehydrogenase.
	 * @param namespace
	 *            A character representing biological_process,
	 *            cellular_component, or molecular_function or null.
	 * @param parentList
	 *            The parent terms of this term including the relation
	 *            type. The supplied list can be reused after the object
	 *            have been constructed.
	 */
	public Term(TermID id, String name, Namespace namespace, ArrayList<ParentTermID> parentList)
	{
		parents = new ParentTermID[parentList.size()];
		parentList.toArray(parents);
		init(id,name,namespace,parents);
	}


	/**
	 * @param strId
	 *            An identifier such as GO:0045174.
	 * @param name
	 *            A string such as glutathione dehydrogenase.
	 * @param namespace
	 *            The name space attribute of the term or null.
	 * @param parentList
	 *            The parent terms of this term including the relation
	 *            type. The supplied list can be reused after the object
	 *            have been constructed.
	 *
	 * @throws IllegalArgumentException if strId is malformatted.
	 */
	public Term(String strId, String name, Namespace namespace, ArrayList<ParentTermID> parentList)
	{
		parents = new ParentTermID[parentList.size()];
		parentList.toArray(parents);
		init(new TermID(strId),name,namespace,parents);
	}

	/**
	 * @param id
	 *            A term id.
	 * @param name
	 *            A string such as glutathione dehydrogenase.
	 * @param namespace
	 *            The name space attribute of the term or null.
	 * @param parents
	 *            The parent terms of this term including the relation
	 *            type.
	 */
	public Term(TermID id, String name, Namespace namespace, ParentTermID...parents)
	{
		init(id,name,namespace,parents);
	}

	/**
	 * Here, the namespace is set to UNKOWN.
	 *
	 * @param id
	 *            A term id.
	 * @param name
	 *            A string such as glutathione dehydrogenase.
	 * @param parents
	 *            The parent terms of this term including the relation
	 *            type.
	 */
	public Term(TermID id, String name, ParentTermID...parents)
	{
		init(id,name,null,parents);
	}


	/**
	 * Here, the namespace is set to UNKOWN.
	 *
	 * @param strId
	 *            An identifier such as GO:0045174.
	 * @param name
	 *            A string such as glutathione dehydrogenase.
	 * @param parents
	 *            The parent terms of this term including the relation
	 *            type.
	 * @throws IllegalArgumentException if strId is malformatted.
	 */
	public Term(String strId, String name, ParentTermID...parents)
	{
		init(new TermID(strId),name,null,parents);
	}

	/**
	 * @param strId
	 *            An identifier such as GO:0045174.
	 * @param name
	 *            A string such as glutathione dehydrogenase.
	 * @param namespace
	 *            The name space attribute of the term or null.
	 * @param parents
	 *            The parent terms of this term including the relation
	 *            type.
	 * @throws IllegalArgumentException if strId is malformatted.
	 */
	public Term(String strId, String name, Namespace namespace, ParentTermID...parents)
	{
		init(new TermID(strId),name,namespace,parents);
	}

	/**
	 * Constructor helper.
	 *
	 * @param strId
	 * @param name
	 * @param namespace
	 * @param parents
	 */
	private void init(TermID id, String name, Namespace namespace, ParentTermID [] parents)
	{
		this.id = id;
		this.name = name;
		this.parents = parents;

		if (namespace == null)
			namespace = Namespace.UNKOWN_NAMESPACE;
		else
			this.namespace = namespace;
	}

	/**
	 * Returns the GO ID as a string.
	 *
	 * @return go:accession
	 */
	public String getIDAsString()
	{
		return id.toString();
	}

	/**
	 * Returns the GO ID as TermID object.
	 *
	 * @return the id
	 */
	public TermID getID()
	{
		return id;
	}

	/**
	 * @return go:name
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * gets the namespace of the term as a Namespace enum
	 *
	 * @return
	 */
	public Namespace getNamespace()
	{
		return namespace;
	}

	/**
	 * gets a single letter String representation of the term's namespace
	 *
	 * @return
	 */
	public String getNamespaceAsString()
	{
		return namespace.getName();
	}

	/**
	 * Returns the parent terms including the relation.
	 *
	 * @return
	 */
	public ParentTermID[] getParents()
	{
		return parents;
	}

	@Override
	public String toString()
	{
		String parents = "";
		for (ParentTermID ptid : getParents())
		{
			parents += ptid.termid.toString() + " ";
		}

		return id.toString() + ": " + parents;
	}

	@Override
	public int hashCode()
	{
		/* We take the hash code of the id */
		return id.hashCode();
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof Term)
		{
			Term goTerm = (Term) obj;
			return goTerm.id.equals(id);
		}
		return super.equals(obj);
	}

	/**
	 * Sets the obsolete state of this term
	 *
	 * @param currentObsolete
	 */
	protected void setObsolete(boolean currentObsolete)
	{
		obsolete = currentObsolete;
	}

	/**
	 * @return whether term is declared as obsolete
	 */
	public boolean isObsolete()
	{
		return obsolete;
	}

	/**
	 * Returns the definition of this term. Might be null if none is available.
	 *
	 * @return the definition or null.
	 */
	public String getDefinition()
	{
		return definition;
	}

	/**
	 * Sets the definition of this term.
	 *
	 * @param definition defines the definition ;)
	 */
	protected void setDefinition(String definition)
	{
		this.definition = definition;
	}

	/**
	 * This sets the alternatives of the term.
	 *
	 * @param altList
	 */
	public void setAlternatives(List<TermID> altList)
	{
		alternatives = new TermID[altList.size()];
		int i = 0;
		for (TermID t : altList)
			alternatives[i++] = t;
	}

	/**
	 * Returns the alternatives of this term.
	 *
	 * @return
	 */
	public TermID [] getAlternatives()
	{
		return alternatives;
	}


	/**
	 * Sets the subsets.
	 *
	 * @param newSubsets
	 */
	public void setSubsets(ArrayList<Subset> newSubsets)
	{
		subsets = new Subset[newSubsets.size()];
		newSubsets.toArray(subsets);
	}

	/**
	 * Returns the subsets.
	 *
	 * @return
	 */
	public Subset[] getSubsets()
	{
		return subsets;
	}

}

