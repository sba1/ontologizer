package ontologizer.ontology;

/**
 * This class is used to specify a parent accompanied with
 * the kind of relationship. Note that the term "parent" is
 * indeed a misnomer. The usage of this term has historical
 * reasons when there were only a limited set of relations.
 *
 * @author Sebastian Bauer
 */
public class ParentTermID
{
	/** The id of the related term */
	public TermID termid;

	/** the type of the relation */
	public TermRelation relation;

	public ParentTermID(TermID parent, TermRelation relation)
	{
		this.termid = parent;
		this.relation = relation;
	}
}
