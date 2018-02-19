package ontologizer.calculation;

import static ontologizer.ontology.TermID.tid;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import ontologizer.association.AnnotationContext;
import ontologizer.association.AssociationContainer;
import ontologizer.calculation.b2g.B2GParam;
import ontologizer.calculation.b2g.Bayes2GOCalculation;
import ontologizer.calculation.b2g.Bayes2GOGOTermProperties;
import ontologizer.enumeration.TermEnumerator;
import ontologizer.internal.InternalOntology;
import ontologizer.ontology.Ontology;
import ontologizer.ontology.TermID;
import ontologizer.parser.ValuedItemAttribute;
import ontologizer.set.PopulationSet;
import ontologizer.set.StudySet;
import ontologizer.statistics.None;
import ontologizer.types.ByteString;
import sonumina.collections.IntMapper;

class B2GTestParameter
{
	static double ALPHA = 0.25;
	static double BETA = 0.60;
	static double BETA2 = 0.10;
	static int MCMC_STEPS = 1020000;
}

/**
 * The testing class
 *
 * @author Sebastian Bauer
 *
 */
public class Bayes2GOCalculationTest
{
	public static double marg(EnrichedGOTermsResult result, String tid)
	{
		return ((Bayes2GOGOTermProperties)result.getGOTermProperties(tid(tid))).marg;
	}

	private Ontology ontology;
	private AssociationContainer assoc;

	/** Terms that are assumed to be active */
	private HashMap<TermID,Double> wantedActiveTerms;

	@Before
	public void setup()
	{
		InternalOntology internalOntology = new InternalOntology();
		ontology = internalOntology.graph;
		assoc = internalOntology.assoc;
		wantedActiveTerms = new HashMap<TermID,Double>(); /* Terms that are active */
		wantedActiveTerms.put(tid("GO:0000010"),0.10);
		wantedActiveTerms.put(tid("GO:0000004"),0.10);
	}

	@Test
	public void testBayes2GOSimple()
	{
		SingleCalculationSetting scs = SingleCalculationSetting.create(new Random(1), wantedActiveTerms, 0.25, ontology, assoc);

		Bayes2GOCalculation calc = new Bayes2GOCalculation();
		calc.setSeed(2);
		calc.setMcmcSteps(520000);
		calc.setAlpha(B2GParam.Type.MCMC);
		calc.setBeta(B2GParam.Type.MCMC);
		calc.setExpectedNumber(2);

		EnrichedGOTermsResult result = calc.calculateStudySet(ontology, assoc, scs.pop, scs.study, new None());
		EnrichedGOTermsTableWriter.writeTable(System.out, result);
		assertEquals(11, result.getSize());
		assertEquals(500, result.getPopulationGeneCount());
		assertEquals(1, marg(result, "GO:0000004"), 1e-5);
		assertEquals(1, marg(result, "GO:0000010"), 1e-5);
		assertEquals(0, marg(result, "GO:0000011"), 1e-5);
		assertEquals(0, marg(result, "GO:0000001"), 1e-5);
		assertEquals(0, marg(result, "GO:0000008"), 1e-5);
		assertEquals(0, marg(result, "GO:0000009"), 1e-5);
		assertEquals(0, marg(result, "GO:0000007"), 1e-5);
		assertEquals(0, marg(result, "GO:0000006"), 1e-5);
		assertEquals(0, marg(result, "GO:0000005"), 1e-5);
		assertEquals(0, marg(result, "GO:0000003"), 1e-5);
		assertEquals(0, marg(result, "GO:0000002"), 1e-5);
	}

	@Test
	public void testBayes2GOSlimSimple()
	{
		SingleCalculationSetting scs = SingleCalculationSetting.create(new Random(1), wantedActiveTerms, 0.25, ontology, assoc);

		Bayes2GOCalculation calc = new Bayes2GOCalculation();
		calc.setSeed(2);
		calc.setMcmcSteps(520000);
		calc.setAlpha(B2GParam.Type.MCMC);
		calc.setBeta(B2GParam.Type.MCMC);
		calc.setExpectedNumber(2);

		AnnotationContext am = assoc.getMapping();

		/* Create int study set */
		int [] studyIds = new int[scs.study.getGeneCount()];
		int studyIdsLen = 0;
		for (ByteString item : scs.study)
		{
			int id = am.mapSymbol(item);
			assertNotEquals(Integer.MAX_VALUE, id);
			studyIds[studyIdsLen++] = id;
		}
		assertEquals(studyIds.length, studyIdsLen);
		Arrays.sort(studyIds);

		TermEnumerator populationEnumerator = scs.study.enumerateTerms(ontology, assoc);
		IntMapper<TermID> termMapper = IntMapper.create(populationEnumerator.getAllAnnotatedTermsAsList());
		IntMapper<ByteString> geneMapper = IntMapper.create(populationEnumerator.getGenesAsList());
		int [][] termLinks = CalculationUtils.makeTermLinks(populationEnumerator, termMapper, geneMapper);
		assertEquals(11, termLinks.length);

		double [] result = calc.calculate(termLinks, studyIds, am.getSymbols().length);
		assertEquals(termLinks.length, result.length);
	}

	@Test
	public void testBayes2GOParameterIntegratedOut()
	{
		SingleCalculationSetting scs = SingleCalculationSetting.create(new Random(1), wantedActiveTerms, 0.25, ontology, assoc);

		Bayes2GOCalculation calc = new Bayes2GOCalculation();
		calc.setSeed(2);
		calc.setMcmcSteps(520000);
		calc.setIntegrateParams(true);
		/* Hyperparameter are integrated out here, so they don't need to be selected */
		calc.setAlpha(B2GParam.Type.FIXED);
		calc.setBeta(B2GParam.Type.FIXED);
		calc.setExpectedNumber(2);

		EnrichedGOTermsResult result = calc.calculateStudySet(ontology, assoc, scs.pop, scs.study, new None());
		EnrichedGOTermsTableWriter.writeTable(System.out, result);
		assertEquals(1, marg(result, "GO:0000004"), 1e-5);
		assertEquals(1, marg(result, "GO:0000010"), 1e-5);
		assertEquals(0, marg(result, "GO:0000011"), 1e-5);
		assertEquals(0, marg(result, "GO:0000001"), 1e-5);
		assertEquals(0, marg(result, "GO:0000008"), 1e-5);
		assertEquals(0, marg(result, "GO:0000009"), 1e-5);
		assertEquals(0, marg(result, "GO:0000007"), 1e-5);
		assertEquals(0, marg(result, "GO:0000006"), 1e-5);
		assertEquals(0, marg(result, "GO:0000005"), 1e-5);
		assertEquals(0, marg(result, "GO:0000003"), 1e-5);
		assertEquals(0, marg(result, "GO:0000002"), 1e-5);
	}

	/* Disabled test @Test */
	public void testValuedGOScore()
	{
		String [] terms = {"GO:0000010", "GO:0000004"};
		InternalOntology internalOntology = new InternalOntology();
		Random rnd = new Random(1);

		AssociationContainer assoc = internalOntology.assoc;
		Ontology ontology = internalOntology.graph;

		PopulationSet populationSet = new PopulationSet();
		populationSet.addGenes(assoc.getAllAnnotatedGenes());
		TermEnumerator populationEnumerator = populationSet.enumerateTerms(ontology, assoc);

		StudySet valuedStudySet = new StudySet();
		for (String t : terms)
		{
			for (ByteString g : populationEnumerator.getAnnotatedGenes(tid(t)).totalAnnotated)
			{
				ValuedItemAttribute via = new ValuedItemAttribute();
				via.description = "";
				via.setValue(rnd.nextDouble() * 0.1);
				valuedStudySet.addGene(g, via);
			}
		}
		Set<ByteString> tempGenes = valuedStudySet.getAllGeneNames();
		for (ByteString g : populationSet)
		{
			if (!tempGenes.contains(g))
			{
				ValuedItemAttribute via = new ValuedItemAttribute();
				via.description = "";
				via.setValue(rnd.nextDouble());
				valuedStudySet.addGene(g, via);
			}
		}

		Bayes2GOCalculation calc = new Bayes2GOCalculation();
		calc.setSeed(2);
		calc.setMcmcSteps(520000);

		calc.calculateStudySet(ontology, assoc, populationSet, valuedStudySet, new None());
	}
}
