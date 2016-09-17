package ontologizer.association;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import ontologizer.types.ByteString;

/**
 * After AssociationParser was used to parse the gene_association.XXX file, this
 * class is used to store and process the information about Associations.
 */
public class AssociationContainer implements Iterable<Gene2Associations>
{
	/** Associations keys by the unique id */
	private Gene2Associations [] associations;

	/** Mapping */
	private AnnotationContext annotationMapping;

	/**
	 * The constructor receives data from the AssociationParser object, which
	 * does the basic work of Parsing a gene_association file. The constructor
	 * takes an array list of associations, and classifies them according to
	 * gene (one gene can have multiple annotations) in Gene2Association
	 * objects.
	 *
	 * @param assocs
	 *            a list of all Associations referring to genes of the current
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
		Set<ByteString> allSymbols = new HashSet<ByteString>();

		for (Association a : assocs)
			allSymbols.add(a.getObjectSymbol());

		annotationMapping = new AnnotationContext(allSymbols, s2g, dbo2g);

		associations = new Gene2Associations[allSymbols.size()];
		for (Association a : assocs)
			addAssociation(a);
	}

	/**
	 * Adds a new association. Note that this will not read out synonyms or any other field
	 * than the object symbol.
	 *
	 * @param a the associated to be added
	 */
	private void addAssociation(Association a)
	{
		ByteString symbol = a.getObjectSymbol();
		int index = annotationMapping.mapSymbol(a.getObjectSymbol());
		if (index == Integer.MAX_VALUE)
			return;

		Gene2Associations g2a = associations[index];
		if (g2a == null)
		{
			g2a = new Gene2Associations(symbol);
			associations[index] = g2a;
		}
		g2a.add(a);
	}

	/**
	 * get a Gene2Associations object corresponding to a given gene name. If the
	 * name is not initially found as dbObject Symbol, (which is usually a
	 * database name with meaning to a biologist), try dbObject (which may be an
	 * accession number or some other term from the association database), and
	 * finally, look for a synonym (another entry in the gene_association file
	 * that will have been parsed into the present object).
	 *
	 * @param geneName name of the gene whose associations are interesting
	 * @return associations for the given gene
	 */
	public Gene2Associations get(ByteString geneName)
	{
		int index = annotationMapping.mapSymbol(geneName);
		if (index == Integer.MAX_VALUE)
		{
			index = annotationMapping.mapObjectID(geneName);
			if (index == Integer.MAX_VALUE)
			{
				index = annotationMapping.mapSynonym(geneName);
			}
		}

		if (index == Integer.MAX_VALUE)
			return null;

		return associations[index];
	}

	/**
	 * Returns whether the given name is an object symbol.
	 *
	 * @param name defines the name that should be checked
	 * @return whether name is an object symbol or not
	 */
	public boolean isObjectSymbol(ByteString name)
	{
		return annotationMapping.mapSymbol(name) != Integer.MAX_VALUE;
	}

	/**
	 * Returns whether the given name is an object id.
	 *
	 * @param name defines the name that should be checked
	 * @return whether name is an object id or not
	 */
	public boolean isObjectID(ByteString name)
	{
		return annotationMapping.mapObjectID(name) != Integer.MAX_VALUE;
	}

	/**
	 * Returns whether the given name is a synonym.
	 *
	 * @param name defines the name that should be checked
	 * @return whether name is a synonym
	 */
	public boolean isSynonym(ByteString name)
	{
		return annotationMapping.mapSynonym(name) != Integer.MAX_VALUE;
	}

	/**
	 * A way to get all annotated genes in the container
	 *
	 * @return The annotated genes as a Set
	 */
	public Set<ByteString> getAllAnnotatedGenes()
	{
		Set<ByteString> symbols = new HashSet<ByteString>();
		for (ByteString bs : annotationMapping.getSymbols())
			symbols.add(bs);

		return symbols;
	}

	public boolean containsGene(ByteString g1)
	{
		return get(g1) != null;
	}

	public Iterator<Gene2Associations> iterator()
	{
		return new Iterator<Gene2Associations>()
		{
			int current;

			@Override
			public boolean hasNext()
			{
				if (current == associations.length)
					return false;
				return true;
			}

			@Override
			public Gene2Associations next()
			{
				Gene2Associations value = associations[current];
				current++;
				return value;
			}

			@Override
			public void remove()
			{
				throw new UnsupportedOperationException();
			}
		};
	}

	/**
	 * @return all evidence codes and their occurrence count that can be found in the container.
	 */
	public Map<String,Integer> getAllEvidenceCodes()
	{
		Map<String,Integer> evidenceCounts = new HashMap<String, Integer>();
		for (Gene2Associations g2a : associations)
		{
			for (Association a : g2a)
			{
				String ev = a.getEvidence().toString();
				int count;

				if (evidenceCounts.containsKey(ev)) count = evidenceCounts.get(ev) + 1;
				else count = 1;

				evidenceCounts.put(ev, count);
			}
		}
		return evidenceCounts;
	}
}
