package ontologizer.calculation;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import ontologizer.association.AssociationContainer;
import ontologizer.calculation.Bayes2GOCalculationTest.SingleCalculationSetting;
import ontologizer.calculation.b2g.FixedAlphaBetaScore;
import ontologizer.enumeration.GOTermEnumerator;
import ontologizer.go.Ontology;
import ontologizer.go.Term;
import ontologizer.go.TermID;
import ontologizer.internal.InternalOntology;
import sonumina.math.graph.SlimDirectedGraphView;
import junit.framework.Assert;
import junit.framework.TestCase;

public class FixedAlphaBetaScoreTest extends TestCase
{
	static Collection<TermID> asList(String...termIDs)
	{
		List<TermID> list = new LinkedList<TermID>();
		for (String t : termIDs)
			list.add(new TermID(t));
		return list;
	}
	
	public void testWithFixedParameter()
	{
		InternalOntology internalOntology = new InternalOntology();
		
		final HashMap<TermID,Double> wantedActiveTerms = new HashMap<TermID,Double>(); /* Terms that are active */
		wantedActiveTerms.put(new TermID("GO:0000010"),0.00);
		wantedActiveTerms.put(new TermID("GO:0000004"),0.00);

		AssociationContainer assoc = internalOntology.assoc;
		Ontology ontology = internalOntology.graph;
		
		SingleCalculationSetting sss = SingleCalculationSetting.create(new Random(1), wantedActiveTerms, 0.0, ontology, assoc);
		GOTermEnumerator popEnumerator = sss.pop.enumerateGOTerms(ontology, assoc);
		FixedAlphaBetaScore fabs = new FixedAlphaBetaScore(new Random(1), popEnumerator.getAllAnnotatedTermsAsList(), popEnumerator, sss.study.getAllGeneNames());
		fabs.setAlpha(0.001);
		fabs.setBeta(0.001);
		fabs.setExpectedNumberOfTerms(2);

		SlimDirectedGraphView<Term> slim = ontology.getSlimGraphView();
		
		double expectedMax = fabs.score(asList("GO:0000010","GO:0000004"));
		double foundMax = Double.NEGATIVE_INFINITY;
		for (int i=0; i < slim.getNumberOfVertices(); i++)
		{
			for (int j=i; j < slim.getNumberOfVertices(); j++)
			{
				String [] tids = new String[i!=j?2:1];
				tids[0] = slim.getVertex(i).getIDAsString();
				if (i!=j)
					tids[1] = slim.getVertex(j).getIDAsString();
				double score = fabs.score(asList(tids));
				if (score > foundMax) foundMax = score;
			}
		}
		Assert.assertEquals(expectedMax, foundMax);
	}
	
	public void testWithIntegratedOutParameter()
	{
		InternalOntology internalOntology = new InternalOntology();
		
		final HashMap<TermID,Double> wantedActiveTerms = new HashMap<TermID,Double>(); /* Terms that are active */
		wantedActiveTerms.put(new TermID("GO:0000010"),0.00);
		wantedActiveTerms.put(new TermID("GO:0000004"),0.00);

		AssociationContainer assoc = internalOntology.assoc;
		Ontology ontology = internalOntology.graph;
		
		SingleCalculationSetting sss = SingleCalculationSetting.create(new Random(1), wantedActiveTerms, 0.0, ontology, assoc);
		GOTermEnumerator popEnumerator = sss.pop.enumerateGOTerms(ontology, assoc);
		FixedAlphaBetaScore fabs = new FixedAlphaBetaScore(new Random(1), popEnumerator.getAllAnnotatedTermsAsList(), popEnumerator, sss.study.getAllGeneNames());
		fabs.setIntegrateParams(true);

		SlimDirectedGraphView<Term> slim = ontology.getSlimGraphView();
		
		double expectedMax = fabs.score(asList("GO:0000010","GO:0000004"));
		double foundMax = Double.NEGATIVE_INFINITY;
		for (int i=0; i < slim.getNumberOfVertices(); i++)
		{
			for (int j=i; j < slim.getNumberOfVertices(); j++)
			{
				String [] tids = new String[i!=j?2:1];
				tids[0] = slim.getVertex(i).getIDAsString();
				if (i!=j)
					tids[1] = slim.getVertex(j).getIDAsString();
				double score = fabs.score(asList(tids));
				if (score > foundMax) foundMax = score;
			}
		}
		System.out.println("Max: " + foundMax);
		Assert.assertEquals(expectedMax, foundMax);
	}

}
