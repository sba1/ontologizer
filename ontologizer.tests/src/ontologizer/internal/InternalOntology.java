package ontologizer.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;

import ontologizer.association.Association;
import ontologizer.association.AssociationContainer;
import ontologizer.ontology.Ontology;
import ontologizer.ontology.ParentTermID;
import ontologizer.ontology.Prefix;
import ontologizer.ontology.Subset;
import ontologizer.ontology.Term;
import ontologizer.ontology.TermContainer;
import ontologizer.ontology.TermID;
import ontologizer.ontology.TermRelation;
import ontologizer.types.ByteString;

/**
 * Represents an example data set. It contains an ontology with 11 terms (including
 * the single root) and a set of 500 genes.
 *
 * @author Sebastian Bauer
 */
public class InternalOntology
{
	public Ontology graph;
	public AssociationContainer assoc;

	/** Maps each item to a synonym */
	public Map<ByteString,ByteString> synonymMap = new HashMap<ByteString,ByteString>();

	public InternalOntology()
	{
		long seed = 1;

		Subset subset = new Subset("slim","Slim internal ontology");
		ArrayList<Subset> subsets = new ArrayList<Subset>();
		subsets.add(subset);

		/* Go Graph */
		HashSet<Term> terms = new HashSet<Term>();
		Term c1 = new Term("GO:0000001", "C1");
		Term c2 = new Term("GO:0000002", "C2", new ParentTermID(c1.getID(),TermRelation.IS_A));
		Term c3 = new Term("GO:0000003", "C3", new ParentTermID(c1.getID(),TermRelation.IS_A));
		Term c4 = new Term("GO:0000004", "C4", new ParentTermID(c2.getID(),TermRelation.IS_A));
		Term c5 = new Term("GO:0000005", "C5", new ParentTermID(c2.getID(),TermRelation.IS_A));
		Term c6 = new Term("GO:0000006", "C6", new ParentTermID(c3.getID(),TermRelation.IS_A),new ParentTermID(c2.getID(),TermRelation.IS_A));
		Term c7 = new Term("GO:0000007", "C7", new ParentTermID(c5.getID(),TermRelation.IS_A),new ParentTermID(c6.getID(),TermRelation.IS_A));
		Term c8 = new Term("GO:0000008", "C8", new ParentTermID(c7.getID(),TermRelation.IS_A));
		Term c9 = new Term("GO:0000009", "C9", new ParentTermID(c7.getID(),TermRelation.IS_A));
		Term c10 = new Term("GO:0000010", "C10", new ParentTermID(c9.getID(),TermRelation.IS_A));
		Term c11 = new Term("GO:0000011", "C11", new ParentTermID(c9.getID(),TermRelation.IS_A));

		c1.setSubsets(subsets);
		c2.setSubsets(subsets);
		c3.setSubsets(subsets);
		c7.setSubsets(subsets);
		c10.setSubsets(subsets);

		terms.add(c1);
		terms.add(c2);
		terms.add(c3);
		terms.add(c4);
		terms.add(c5);
		terms.add(c6);
		terms.add(c7);
		terms.add(c8);
		terms.add(c9);
		terms.add(c10);
		terms.add(c11);
		TermContainer termContainer = new TermContainer(terms,"","");
		graph = Ontology.create(termContainer);

		HashSet<TermID> tids = new HashSet<TermID>();
		for (Term term : terms)
			tids.add(term.getID());

		/* Associations */
		ArrayList<Association> associations = new ArrayList<Association>();
		HashMap<ByteString,ByteString> synonym2Item = new HashMap<ByteString,ByteString>();

		Random r = new Random(seed);

		Prefix goPrefix = new Prefix("GO");

		/* Randomly assign the items (note that redundant associations are filtered out later) */
		for (int i=1;i<=500;i++)
		{
			int numTerms = r.nextInt(2) + 1;
			ByteString itemName = new ByteString("item" + i);
			ByteString synonymName = new ByteString("synonym" + i);

			for (int j=0;j<numTerms;j++)
			{
				int tid = r.nextInt(terms.size())+1;
				associations.add(new Association(itemName, new TermID(goPrefix, tid)));
			}
			synonym2Item.put(synonymName, itemName);
			synonymMap.put(itemName, synonymName);
		}

		assoc = new AssociationContainer(associations, synonym2Item, new HashMap<ByteString,ByteString>());
	}
}

