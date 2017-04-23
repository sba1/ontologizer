package ontologizer.calculation;

import static ontologizer.calculation.CalculationTestUtils.asList;
import static ontologizer.ontology.TermID.tid;

import java.util.HashMap;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import ontologizer.association.AssociationContainer;
import ontologizer.calculation.b2g.Bayes2GOScore;
import ontologizer.calculation.b2g.FixedAlphaBetaScore;
import ontologizer.enumeration.TermEnumerator;
import ontologizer.internal.InternalOntology;
import ontologizer.ontology.Ontology;
import ontologizer.ontology.Term;
import ontologizer.ontology.TermID;
import ontologizer.types.ByteString;
import sonumina.collections.IntMapper;
import sonumina.math.graph.SlimDirectedGraphView;

public class FixedAlphaBetaScoreTest
{
	@Test
	public void testWithFixedParameter()
	{
		InternalOntology internalOntology = new InternalOntology();

		final HashMap<TermID,Double> wantedActiveTerms = new HashMap<TermID,Double>(); /* Terms that are active */
		wantedActiveTerms.put(tid("GO:0000010"),0.00);
		wantedActiveTerms.put(tid("GO:0000004"),0.00);

		AssociationContainer assoc = internalOntology.assoc;
		Ontology ontology = internalOntology.graph;

		SingleCalculationSetting sss = SingleCalculationSetting.create(new Random(1), wantedActiveTerms, 0.0, ontology, assoc);
		TermEnumerator popEnumerator = sss.pop.enumerateTerms(ontology, assoc);
		IntMapper<TermID> termMapper = IntMapper.create(popEnumerator.getAllAnnotatedTermsAsList());
		IntMapper<ByteString> geneMapper = IntMapper.create(popEnumerator.getGenesAsList());
		int [][] termLinks = Bayes2GOScore.makeTermLinks(popEnumerator, termMapper, geneMapper);
		FixedAlphaBetaScore fabs = new FixedAlphaBetaScore(new Random(1), termLinks, geneMapper, sss.study.getAllGeneNames());
		fabs.setAlpha(0.001);
		fabs.setBeta(0.001);
		fabs.setExpectedNumberOfTerms(2);

		SlimDirectedGraphView<Term> slim = ontology.getSlimGraphView();

		double expectedMax = fabs.score(asList(termMapper, "GO:0000010","GO:0000004"));
		double foundMax = Double.NEGATIVE_INFINITY;
		for (int i=0; i < slim.getNumberOfVertices(); i++)
		{
			for (int j=i; j < slim.getNumberOfVertices(); j++)
			{
				String [] tids = new String[i!=j?2:1];
				tids[0] = slim.getVertex(i).getIDAsString();
				if (i!=j)
					tids[1] = slim.getVertex(j).getIDAsString();
				double score = fabs.score(asList(termMapper, tids));
				if (score > foundMax) foundMax = score;
			}
		}
		Assert.assertEquals(expectedMax, foundMax, 1e-10);
	}

	@Test
	public void testWithIntegratedOutParameter()
	{
		InternalOntology internalOntology = new InternalOntology();

		final HashMap<TermID,Double> wantedActiveTerms = new HashMap<TermID,Double>(); /* Terms that are active */
		wantedActiveTerms.put(tid("GO:0000010"),0.00);
		wantedActiveTerms.put(tid("GO:0000004"),0.00);

		AssociationContainer assoc = internalOntology.assoc;
		Ontology ontology = internalOntology.graph;

		SingleCalculationSetting sss = SingleCalculationSetting.create(new Random(1), wantedActiveTerms, 0.0, ontology, assoc);
		TermEnumerator popEnumerator = sss.pop.enumerateTerms(ontology, assoc);
		IntMapper<TermID> termMapper = IntMapper.create(popEnumerator.getAllAnnotatedTermsAsList());
		IntMapper<ByteString> geneMapper = IntMapper.create(popEnumerator.getGenesAsList());
		int [][] termLinks = Bayes2GOScore.makeTermLinks(popEnumerator, termMapper, geneMapper);
		FixedAlphaBetaScore fabs = new FixedAlphaBetaScore(new Random(1), termLinks, geneMapper, sss.study.getAllGeneNames());
		fabs.setIntegrateParams(true);

		SlimDirectedGraphView<Term> slim = ontology.getSlimGraphView();

		double expectedMax = fabs.score(asList(termMapper, "GO:0000010","GO:0000004"));
		double foundMax = Double.NEGATIVE_INFINITY;
		for (int i=0; i < slim.getNumberOfVertices(); i++)
		{
			for (int j=i; j < slim.getNumberOfVertices(); j++)
			{
				String [] tids = new String[i!=j?2:1];
				tids[0] = slim.getVertex(i).getIDAsString();
				if (i!=j)
					tids[1] = slim.getVertex(j).getIDAsString();
				double score = fabs.score(asList(termMapper, tids));
				if (score > foundMax) foundMax = score;
			}
		}
		System.out.println("Max: " + foundMax);
		Assert.assertEquals(expectedMax, foundMax, 1e-10);
	}

}
