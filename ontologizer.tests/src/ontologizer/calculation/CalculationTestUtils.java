package ontologizer.calculation;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Random;

import ontologizer.association.AssociationContainer;
import ontologizer.internal.InternalOntology;
import ontologizer.ontology.Ontology;
import ontologizer.ontology.TermID;
import ontologizer.statistics.None;

public class CalculationTestUtils
{
	@SuppressWarnings("unchecked")
	public static <T> T prop(EnrichedGOTermsResult result, String id)
	{
		return (T)result.getGOTermProperties(new TermID(id));
	}

	/**
	 * Perform a standard test calculation using the supplied algorithm
	 * on the initial ontology.
	 *
	 * @return
	 */
	public static EnrichedGOTermsResult performTestCalculation(ICalculation calc)
	{
		InternalOntology internalOntology = new InternalOntology();

		final HashMap<TermID,Double> wantedActiveTerms = new HashMap<TermID,Double>(); /* Terms that are active */
		wantedActiveTerms.put(new TermID("GO:0000004"),0.0);

		AssociationContainer assoc = internalOntology.assoc;
		Ontology ontology = internalOntology.graph;

		SingleCalculationSetting scs = SingleCalculationSetting.create(new Random(1), wantedActiveTerms, 0.00, ontology, assoc);
		assertEquals(500, scs.pop.getGeneCount());
		assertEquals(57, scs.study.getGeneCount());

		EnrichedGOTermsResult r = calc.calculateStudySet(ontology, assoc, scs.pop, scs.study, new None());
		EnrichedGOTermsTableWriter.writeTable(System.out, r);

		assertEquals(11, r.getSize());
		assertEquals(500, r.getPopulationGeneCount());
		assertEquals(57, r.getStudyGeneCount());
		return r;
	}

}
