package ontologizer.go;

import java.util.ArrayList;


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
	static public enum Namespace
	{
		BIOLOGICAL_PROCESS,
		MOLECULAR_FUNCTION,
		CELLULAR_COMPONENT,
		UNSPECIFIED;
	};

	/** The id ("accession number") of this GO term */
	private TermID id;

	/** The short human readable name of the id */
	private String name;

	/** The definition of this term. This might be null if this information is not available */
	private String definition;
	
	/** The parents of the this term */
	private ArrayList<ParentTermID> parents;

	/** Used for the iterator stuff */
	private int index, size;

	/** The terms name space */
	private Namespace namespace;

	/** Whether term is declared as obsolete */
	private boolean obsolete;

	/**
	 * @param id
	 *            An identifier such as GO:0045174.
	 * @param name
	 *            A string such as glutathione dehydrogenase.
	 * @param parents
	 *            A Java.util.ArrayList containing a list of Strings with the
	 *            accession numbers of the parents of this GO term.
	 * @param namespace
	 *            A character representing biological_process,
	 *            cellular_component, or molecular_function or null.
	 */
	Term(String strId, String name, String namespace, ArrayList<ParentTermID> parents)
	{
		if (namespace == null) this.namespace = Namespace.UNSPECIFIED;
		else if (namespace.startsWith("B")) this.namespace = Namespace.BIOLOGICAL_PROCESS;
		else if (namespace.startsWith("F")) this.namespace = Namespace.MOLECULAR_FUNCTION;
		else if (namespace.startsWith("C")) this.namespace = Namespace.CELLULAR_COMPONENT;
		else throw new IllegalArgumentException("The namespace '" + namespace + "' is unknown");

		this.id = new TermID(strId);
		this.name = name;
		this.parents = parents;
	}

	/**
	 * Clients can obtain a list of parents of this term by means of an
	 * iterator-like interface by first calling this function and then calling
	 * hasNext() and next()
	 */
	public void setParentIterator()
	{
		this.index = 0;
		this.size = parents.size();
	}

	public boolean hasNext()
	{
		return index < size;
	}

	/**
	 * Note that this does not correspond to the Iterator interface because we
	 * are returning a String rather than an Object
	 */
	public ParentTermID next()
	{
		if (index >= size)
			throw new IndexOutOfBoundsException("Only " + size + " elements");
		return parents.get(index++);
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
		switch (namespace)
		{
			case BIOLOGICAL_PROCESS: return "B";
			case MOLECULAR_FUNCTION: return "M";
			case CELLULAR_COMPONENT:	return "C";
			default: return "-";
		}
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
	 * @param definition defines the defintion ;)
	 */
	protected void setDefinition(String definition)
	{
		this.definition = definition;
	}
}
