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
	public static TermForTermGOTermProperties prop(EnrichedGOTermsResult result, String id)
	{
		return (TermForTermGOTermProperties)result.getGOTermProperties(new TermID(id));
	}

	/**
	 * Perform a standard test calculation on the initial ontology
	 *
	 * @return
	 */
	public static EnrichedGOTermsResult performTestCalculation()
	{
		InternalOntology internalOntology = new InternalOntology();

		final HashMap<TermID,Double> wantedActiveTerms = new HashMap<TermID,Double>(); /* Terms that are active */
		wantedActiveTerms.put(new TermID("GO:0000004"),0.0);

		AssociationContainer assoc = internalOntology.assoc;
		Ontology ontology = internalOntology.graph;

		SingleCalculationSetting scs = SingleCalculationSetting.create(new Random(1), wantedActiveTerms, 0.00, ontology, assoc);
		assertEquals(500, scs.pop.getGeneCount());
		assertEquals(57, scs.study.getGeneCount());

		TermForTermCalculation tft = new TermForTermCalculation();
		EnrichedGOTermsResult r = tft.calculateStudySet(ontology, assoc, scs.pop, scs.study, new None());
		EnrichedGOTermsTableWriter.writeTable(System.out, r);

		assertEquals(11, r.getSize());
		assertEquals(500, r.getPopulationGeneCount());
		assertEquals(57, r.getStudyGeneCount());
		return r;
	}

}
