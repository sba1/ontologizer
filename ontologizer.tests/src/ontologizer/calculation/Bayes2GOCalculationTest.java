package ontologizer.calculation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

import ontologizer.association.Association;
import ontologizer.association.AssociationContainer;
import ontologizer.calculation.b2g.B2GParam;
import ontologizer.calculation.b2g.Bayes2GOCalculation;
import ontologizer.enumeration.GOTermEnumerator;
import ontologizer.go.Ontology;
import ontologizer.go.ParentTermID;
import ontologizer.go.Term;
import ontologizer.go.TermContainer;
import ontologizer.go.TermID;
import ontologizer.go.TermRelation;
import ontologizer.set.PopulationSet;
import ontologizer.set.StudySet;
import ontologizer.statistics.None;
import ontologizer.types.ByteString;
import junit.framework.TestCase;

class InternalOntology
{
	public Ontology graph;
	public AssociationContainer assoc;

	public InternalOntology()
	{
		long seed = 1;

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
		graph = new Ontology(termContainer);

		HashSet<TermID> tids = new HashSet<TermID>();
		for (Term term : terms)
			tids.add(term.getID());

		/* Associations */
		assoc = new AssociationContainer();
		Random r = new Random(seed);

		/* Randomly assign the items (note that redundant associations are filtered out later) */
		for (int i=1;i<=500;i++)
		{
			String itemName = "item" + i;
			int numTerms = r.nextInt(2) + 1;

			for (int j=0;j<numTerms;j++)
			{
				int tid = r.nextInt(terms.size())+1;
				assoc.addAssociation(new Association(new ByteString(itemName),tid));
			}
		}
	}
}

/**
 * The testing class
 *
 * @author Sebastian Bauer
 *
 */
public class Bayes2GOCalculationTest extends TestCase
{
	public void testBayes2GOSimple()
	{
		InternalOntology internalOntology = new InternalOntology();

		final HashMap<TermID,Double> wantedActiveTerms = new HashMap<TermID,Double>(); /* Terms that are active */
		wantedActiveTerms.put(new TermID("GO:0000010"),0.10);
		wantedActiveTerms.put(new TermID("GO:0000004"),0.10);

		/* TODO: We definitively want to refactor the following code */
		Random rnd = new Random(1);
		AssociationContainer assoc = internalOntology.assoc;
		Ontology ontology = internalOntology.graph;

		PopulationSet allGenes = new PopulationSet("all");
		for (ByteString gene : assoc.getAllAnnotatedGenes())
			allGenes.addGene(gene, "");

		final GOTermEnumerator allEnumerator = allGenes.enumerateGOTerms(ontology,assoc);

		/* Create for each wanted term an study set for its own */
		HashMap<TermID,StudySet> wantedActiveTerm2StudySet = new HashMap<TermID,StudySet>();
		for (TermID t : wantedActiveTerms.keySet())
		{
			StudySet termStudySet = new StudySet("study");
			for (ByteString g : allEnumerator.getAnnotatedGenes(t).totalAnnotated)
				termStudySet.addGene(g, "");
			termStudySet.filterOutDuplicateGenes(assoc);
			wantedActiveTerm2StudySet.put(t, termStudySet);
		}

		/* Combine the study sets into one */
		StudySet newStudyGenes = new StudySet("study");
		for (TermID t : wantedActiveTerms.keySet())
			newStudyGenes.addGenes(wantedActiveTerm2StudySet.get(t));
		newStudyGenes.filterOutDuplicateGenes(assoc);


		double alphaStudySet = 0.25;
		int tp = newStudyGenes.getGeneCount();
		int tn = allGenes.getGeneCount();

		/* Obfuscate the study set, i.e., create the observed state */

		/* false -> true (alpha, false positive) */
		HashSet<ByteString>  fp = new HashSet<ByteString>();
		for (ByteString gene : allGenes)
		{
			if (newStudyGenes.contains(gene)) continue;
			if (rnd.nextDouble() < alphaStudySet) fp.add(gene);
		}

		/* true -> false (beta, false negative) */
		HashSet<ByteString>  fn = new HashSet<ByteString>();
		for (TermID t : wantedActiveTerms.keySet())
		{
			double beta = wantedActiveTerms.get(t);
			StudySet termStudySet = wantedActiveTerm2StudySet.get(t);
			for (ByteString g : termStudySet)
			{
				if (rnd.nextDouble() < beta) fn.add(g);
			}
		}

		newStudyGenes.addGenes(fp);
		newStudyGenes.removeGenes(fn);

		Bayes2GOCalculation calc = new Bayes2GOCalculation();
		calc.setSeed(2);
		calc.setMcmcSteps(520000);
		calc.setAlpha(B2GParam.Type.MCMC);
		calc.setBeta(B2GParam.Type.MCMC);
		calc.setExpectedNumber(B2GParam.Type.MCMC);

		calc.calculateStudySet(ontology, assoc, allGenes, newStudyGenes, new None());
	}
}
