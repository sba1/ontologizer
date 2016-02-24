package ontologizer.go;

/**
 * The read-only interface for terms.
 *
 * @author Sebastian Bauer
 */
public interface ITerm
{
	/**
	 * Returns the id as vanilla TermID object.
	 *
	 * @return the id
	 */
	public TermID getID();

	/**
	 * Returns the name of the term.
	 *
	 * @return the name
	 */
	public String getName();

	/**
	 * Returns the associated namespace of the term.
	 *
	 * @return the namespace
	 */
	public Namespace getNamespace();

	/**
	 * Returns the parent term ids and their relation.
	 *
	 * @return the parental terms and there relation.
	 */
	public ParentTermID[] getParents();

	/**
	 * @return whether term is declared as obsolete
	 */
	public boolean isObsolete();

	/**
	 * Returns the definition of this term. Might be null if none is available.
	 *
	 * @return the definition or null.
	 */
	public String getDefinition();

	/**
	 * Return the term id of terms that are declared as equivalent.
	 * @return equivalent terms
	 */
	public TermID[] getEquivalents();

	/**
	 * Returns the terms that have been declared as alternatives of this term.
	 *
	 * @return the alternatives.
	 */
	public TermID[] getAlternatives();

	/**
	 * Returns the subsets.
	 *
	 * @return
	 */
	public Subset[] getSubsets();

	/**
	 * Returns the synonyms.
	 *
	 * @return the synonyms.
	 */
	public String[] getSynonyms();

	/**
	 * Returns the associated xrefs
	 *
	 * @return the associated xrefs
	 */
	public TermXref[] getXrefs();

	/**
	 * Return the terms that are declared in an intersection relation.
	 *
	 * @return
	 */
	public String[] getIntersections();
}
