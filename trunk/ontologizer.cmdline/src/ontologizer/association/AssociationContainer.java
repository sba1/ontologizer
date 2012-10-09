package ontologizer.association;

import java.util.*;

import ontologizer.types.ByteString;

/**
 * After AssociationParser was used to parse the gene_association.XXX file, this
 * class is used to store and process the information about Associations.
 */
public class AssociationContainer implements Iterable<Gene2Associations>
{
	/** Mapping from gene (or gene product) names to Association objects */
	private HashMap<ByteString, Gene2Associations> gene2assocs;

	/** Mapping of synonyms to gene names */
	private HashMap<ByteString, ByteString> synonym2gene;

	/** <I>key</I>: dbObject <I>value</I>: main gene name (dbObject_Symbol) */
	private HashMap<ByteString, ByteString> dbObject2gene;

	/**
	 * Total number of annotations available for the genes in our dataset.
	 */
	private int totalAnnotations;

	/**
	 * The constructor receives data from the AssociationParser object, which
	 * does the basic work of Parsing a gene_association file. The constructor
	 * takes an array list of associations, and classifies them according to
	 * gene (one gene can have multiple annotations) in Gene2Association
	 * objects.
	 * 
	 * @param assocFile
	 *            name of the gene_associations.XXX file
	 * @param assocs
	 *            a list of all Associations refererring to genes of the current
	 *            dataset
	 * @param s2g
	 *            HashMap of synonyms for gene names extracted from the
	 *            association file
	 * @param dbo2g
	 *            HashMap of mappings from database objects (e.g., accession
	 *            numbers) to gene names.
	 * @see Gene2Associations
	 * @see AssociationParser
	 */
	public AssociationContainer(
			ArrayList<Association> assocs, HashMap<ByteString, ByteString> s2g,
			HashMap<ByteString, ByteString> dbo2g)
	{
		synonym2gene = s2g;
		dbObject2gene = dbo2g;

		totalAnnotations = 0;
		gene2assocs = new HashMap<ByteString, Gene2Associations>();

		for (Association a : assocs)
			addAssociation(a);
	}

	/**
	 * Constructor for an empty container.
	 * 
	 * @see addAssociation
	 */
	public AssociationContainer()
	{
		synonym2gene = new HashMap<ByteString,ByteString>();
		dbObject2gene = new HashMap<ByteString, ByteString>();
		gene2assocs = new HashMap<ByteString, Gene2Associations>();

		totalAnnotations = 0;
	}


	/**
	 * Adds a new association.
	 * 
	 * @param a
	 */
	public void addAssociation(Association a)
	{
		totalAnnotations++;
		Gene2Associations g2a = null;
		if (gene2assocs.containsKey(a.getObjectSymbol()))
		{
			g2a = gene2assocs.get(a.getObjectSymbol());
			g2a.add(a); // Add the Association to existing g2a
		} else
		{
			// Otherwise create new Gene2Associations object
			// for this gene.
			g2a = new Gene2Associations(a.getObjectSymbol());
			g2a.add(a);
			gene2assocs.put(a.getObjectSymbol(), g2a);
		}
	}

	/** For debugging */
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("*****\n---AssociationContainer---\n*****\n");
		sb.append("Total annotations: " + totalAnnotations + "\n");
		sb.append("Number of genes with associations: " + gene2assocs.size()
				+ "\n");
		sb.append("Number of synonyms: " + synonym2gene.size() + "\n");
		sb.append("Number of dbo -> genename mappings: " + dbObject2gene.size()
				+ "\n");
		return sb.toString();
	}

	/**
	 * get a Gene2Associations object corresponding to a given gene name. If the
	 * name is not initially found as dbObject Symbol, (which is usually a
	 * database name with meaning to a biologist), try dbObject (which may be an
	 * accession number or some other term from the association database), and
	 * finally, look for a synonym (another entry in the gene_association file
	 * that will have been parsed into the present object).
	 * 
	 * @param geneName
	 */
	public Gene2Associations get(ByteString geneName)
	{
		Gene2Associations g2a = gene2assocs.get(geneName);
		if (g2a == null)
		{
			ByteString dbObject = dbObject2gene.get(geneName);
			g2a = gene2assocs.get(dbObject);
		}
		if (g2a == null)
		{
			ByteString synonym = synonym2gene.get(geneName);
			g2a = gene2assocs.get(synonym);
		}
		return g2a;
	}

	/**
	 * Returns whether the given name is a object symbol. 
	 * 
	 * @param name
	 * @return
	 */
	public boolean isObjectSymbol(ByteString name)
	{
		return gene2assocs.containsKey(name);
	}
	
	/**
	 * Returns whether the given name is an object id. 
	 * 
	 * @param name
	 * @return
	 */
	public boolean isObjectID(ByteString name)
	{
		return dbObject2gene.containsKey(name);
	}
	
	/**
	 * Returns whether the given name is a synonym.
	 * 
	 * @param name
	 * @return
	 */
	public boolean isSynonym(ByteString name)
	{
		return synonym2gene.containsKey(name);
	}

	/**
	 * A way to get all annotated genes in the container
	 * 
	 * @author Steffen Grossmann
	 * 
	 * @return The annotated genes as a Set
	 */
	public Set<ByteString> getAllAnnotatedGenes()
	{
		return gene2assocs.keySet();
	}

	public boolean containsGene(ByteString g1)
	{
		return get(g1) != null;
	}

	public Iterator<Gene2Associations> iterator()
	{
		return gene2assocs.values().iterator();
	}
}
