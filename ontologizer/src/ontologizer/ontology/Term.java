package ontologizer.ontology;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import ontologizer.types.ByteString;

/**
 * This class provides a representation of individual terms
 *
 * Both isa and part-of refer to child-parent relationships in the directed
 * acyclic graph. The Ontologizer does not distinguish between these types of
 * child-parent relationships, but rather places both type of parent in an
 * ArrayList termed parents. This will allow us to traverse the DAG while we are
 * tabulating the counts of functions found in a cluster
 *
 * @author Peter Robinson, Sebastian Bauer, Sebastian Koehler
 */

public class Term implements ITerm
{
	/** The id ("accession number") of this GO term */
	private TermID id;

	/** The short human readable name of the id */
	private String name;

	/**
	 * The definition of this term. This might be null if this information is
	 * not available
	 */
	private String definition;

	/** The parents of the this term */
	private ParentTermID[] parents;

	/** The term's alternatives */
	private ArrayList<TermID> alternatives;

	/** The term's alternatives */
	private TermID[] equivalents;

	/** The synonyms of this term, as read from the obo file. */
	private ByteString[] synonyms;

	/** The intersections tags of this term, as read from the obo file. */
	private String[] intersections;

	/** Per default, terms are not associated to any subset. This is the empty array */
	private static final Subset [] NO_SUBSETS = new Subset[]{};

	/** The term's subsets */
	private Subset[] subsets = NO_SUBSETS;

	/** The term's xrefs */
	private TermXref[] xrefs;

	/** The term's name space */
	private Namespace namespace;

	/** Whether term is declared as obsolete */
	private boolean obsolete;

	/** The information content associated to the term. TODO: Extract this */
	private double informationContent = -1;

	/**
	 * Default constructor. For builder only.
	 */
	private Term()
	{
	}

	/**
	 * @param id
	 *            A term id.
	 * @param name
	 *            A string such as glutathione dehydrogenase.
	 * @param namespace
	 *            A character representing biological_process,
	 *            cellular_component, or molecular_function or null.
	 * @param parents
	 *            The parent terms of this term including the relation type. The
	 *            supplied list can be reused after the object have been
	 *            constructed.
	 */
	public Term(TermID id, String name, Namespace namespace, Collection<ParentTermID> parents)
	{
		init(id, name, namespace, parents);
	}

	/**
	 * @param strId
	 *            An identifier such as GO:0045174.
	 * @param name
	 *            A string such as glutathione dehydrogenase.
	 * @param namespace
	 *            The name space attribute of the term or null.
	 * @param parents
	 *            The parent terms of this term including the relation type. The
	 *            supplied list can be reused after the object have been
	 *            constructed.
	 *
	 * @throws IllegalArgumentException
	 *             if strId is malformatted.
	 */
	public Term(String strId, String name, Namespace namespace, Collection<ParentTermID> parents)
	{
		init(new TermID(strId), name, namespace, parents);
	}

	/**
	 * @param id
	 *            A term id.
	 * @param name
	 *            A string such as glutathione dehydrogenase.
	 * @param namespace
	 *            The name space attribute of the term or null.
	 * @param parents
	 *            The parent terms of this term including the relation type.
	 */
	public Term(TermID id, String name, Namespace namespace, ParentTermID... parents) {
		init(id, name, namespace, parents);
	}

	/**
	 * Here, the namespace is set to UNKOWN.
	 *
	 * @param id
	 *            A term id.
	 * @param name
	 *            A string such as glutathione dehydrogenase.
	 * @param parents
	 *            The parent terms of this term including the relation type.
	 */
	public Term(TermID id, String name, ParentTermID... parents) {
		init(id, name, null, parents);
	}

	/**
	 * Here, the namespace is set to UNKOWN.
	 *
	 * @param strId
	 *            An identifier such as GO:0045174.
	 * @param name
	 *            A string such as glutathione dehydrogenase.
	 * @param parents
	 *            The parent terms of this term including the relation type.
	 * @throws IllegalArgumentException
	 *             if strId is malformatted.
	 */
	public Term(String strId, String name, ParentTermID... parents) {
		init(new TermID(strId), name, null, parents);
	}

	/**
	 * @param strId
	 *            An identifier such as GO:0045174.
	 * @param name
	 *            A string such as glutathione dehydrogenase.
	 * @param namespace
	 *            The name space attribute of the term or null.
	 * @param parents
	 *            The parent terms of this term including the relation type.
	 * @throws IllegalArgumentException
	 *             if strId is malformatted.
	 */
	public Term(String strId, String name, Namespace namespace, ParentTermID... parents) {
		init(new TermID(strId), name, namespace, parents);
	}

	/**
	 * Constructor helper.
	 *
	 * @param id
	 * @param name
	 * @param namespace
	 * @param parents
	 */
	private void init(TermID id, String name, Namespace namespace, Collection<ParentTermID> parents)
	{
		ParentTermID [] parentArray = new ParentTermID[parents.size()];
		parents.toArray(parentArray);
		init(id, name, namespace, parentArray);
	}

	/**
	 * Constructor helper.
	 *
	 * @param strId
	 * @param name
	 * @param namespace
	 * @param parents
	 */
	private void init(TermID id, String name, Namespace namespace, ParentTermID[] parents) {
		this.id = id;
		this.name = name;
		this.parents = parents;

		if (namespace == null)
			namespace = Namespace.UNKOWN_NAMESPACE;
		else
			this.namespace = namespace;
	}

	/**
	 * Returns the id of the term as a string.
	 *
	 * @return the term id as string.
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
	public TermID getID() {
		return id;
	}

	/**
	 * @return go:name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the namespace of the term as a Namespace enum
	 */
	public Namespace getNamespace()
	{
		if (namespace == null)
			return Namespace.UNKOWN_NAMESPACE;
		return namespace;
	}

	/**
	 * @return the parent terms including the relation.
	 */
	public ParentTermID[] getParents() {
		return parents;
	}

	@Override
	public String toString() {
		return name + " (" + id.toString() + ")";
	}

	@Override
	public int hashCode() {
		/* We take the hash code of the id */
		return id.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Term) {
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
	protected void setObsolete(boolean currentObsolete) {
		obsolete = currentObsolete;
	}

	/**
	 * @return whether term is declared as obsolete
	 */
	public boolean isObsolete() {
		return obsolete;
	}

	/**
	 * Returns the definition of this term. Might be null if none is available.
	 *
	 * @return the definition or null.
	 */
	public String getDefinition() {
		return definition;
	}

	/**
	 * Sets the definition of this term.
	 *
	 * @param definition
	 *            defines the definition ;)
	 */
	public void setDefinition(String definition) {
		this.definition = definition;
	}

	public void setEquivalents(ArrayList<TermID> currentEquivalents) {
		equivalents = new TermID[currentEquivalents.size()];
		int i = 0;
		for (TermID t : currentEquivalents)
			equivalents[i++] = t;
	}

	public TermID[] getEquivalents() {
		return equivalents;
	}

	/**
	 * This sets the alternatives of the term.
	 *
	 * @param altList
	 */
	public void setAlternatives(List<TermID> altList)
	{
		this.alternatives = new ArrayList<TermID>();
		this.alternatives.addAll(altList);
	}

	/**
	 * @return the alternatives of this term.
	 */
	public TermID[] getAlternatives()
	{
		TermID [] alts = new TermID[alternatives.size()];
		return alternatives.toArray(alts);
	}

	/**
	 * Sets the subsets.
	 *
	 * @param newSubsets
	 */
	public void setSubsets(ArrayList<Subset> newSubsets) {
		subsets = new Subset[newSubsets.size()];
		newSubsets.toArray(subsets);
	}

	/**
	 * @return the subsets.
	 */
	public Subset[] getSubsets() {
		return subsets;
	}

	public void setSynonyms(ArrayList<ByteString> currentSynonyms) {

		if (currentSynonyms.size() > 0) {
			synonyms = new ByteString[currentSynonyms.size()];
			currentSynonyms.toArray(synonyms);
		}
	}

	public ByteString[] getSynonyms() {
		return synonyms;
	}

	public void setXrefs(ArrayList<TermXref> currentXrefs) {
		if (currentXrefs.size() > 0) {
			xrefs = new TermXref[currentXrefs.size()];
			currentXrefs.toArray(xrefs);
		}
	}

	public TermXref[] getXrefs() {
		return xrefs;
	}

	public void setIntersections(ArrayList<String> currentIntersections) {
		if (currentIntersections.size() > 0) {
			intersections = new String[currentIntersections.size()];
			currentIntersections.toArray(intersections);
		}

	}

	public String[] getIntersections() {
		return intersections;
	}

	public void addAlternativeId(TermID id2) {
		if (this.alternatives == null)
			this.alternatives = new ArrayList<TermID>();
		this.alternatives.add(id2);
	}

	public void setInformationContent(double informationContent) {
		this.informationContent = informationContent;
	}

	public double getInformationContent() {
		return informationContent;
	}

	/* Simple builder interface */

	public static interface Optional
	{
		Optional definition(String description);

		Optional parents(ParentTermID... parents);

		Optional obsolete(boolean obsolete);

		Term build();
	}

	public static interface RequiresName
	{
		RequiresTermID name(String name);
	}

	public static interface RequiresTermID
	{
		Optional id(String termID);

		Optional id(ByteString termID);

		Optional id(TermID termID);
	}

	public static class TermBuilder implements RequiresName, RequiresTermID, Optional
	{
		private Term term = new Term();
		private PrefixPool prefixPool;

		@Override
		public RequiresTermID name(String name)
		{
			term.name = name;
			return this;
		}

		@Override
		public Optional id(String termID)
		{
			term.id = new TermID(termID, prefixPool);
			return this;
		}

		@Override
		public Optional id(TermID termID)
		{
			term.id = termID;
			return this;
		}

		@Override
		public Optional id(ByteString termID)
		{
			term.id = new TermID(termID, prefixPool);
			return this;
		}

		@Override
		public Optional definition(String definition)
		{
			term.definition = definition;
			return this;
		}

		@Override
		public Optional parents(ParentTermID... parents)
		{
			term.parents = parents;
			return this;
		}

		@Override
		public Optional obsolete(boolean obsolete)
		{
			term.obsolete = obsolete;
			return this;
		}

		@Override
		public Term build()
		{
			return term;
		}
	}

	public static RequiresName prefixPool(PrefixPool prefixPool)
	{
		TermBuilder builder = new TermBuilder();
		builder.prefixPool = prefixPool;
		return builder;
	}

	public static RequiresTermID name(String name)
	{
		TermBuilder builder = new TermBuilder();
		builder.term.name = name;
		return builder;
	}
}
