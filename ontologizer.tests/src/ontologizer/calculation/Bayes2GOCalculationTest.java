package ontologizer.calculation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

import junit.framework.TestCase;
import ontologizer.association.AssociationContainer;
import ontologizer.calculation.b2g.B2GParam;
import ontologizer.calculation.b2g.Bayes2GOCalculation;
import ontologizer.enumeration.GOTermEnumerator;
import ontologizer.go.Ontology;
import ontologizer.go.TermID;
import ontologizer.internal.InternalOntology;
import ontologizer.set.PopulationSet;
import ontologizer.set.StudySet;
import ontologizer.statistics.None;
import ontologizer.types.ByteString;

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
