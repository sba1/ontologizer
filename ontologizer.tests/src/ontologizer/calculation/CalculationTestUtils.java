package ontologizer.calculation;

import static ontologizer.ontology.TermID.tid;
import static org.junit.Assert.assertEquals;

import java.util.Collection;
import java.util.HashMap;
import java.util.Random;

import ontologizer.association.AssociationContainer;
import ontologizer.internal.InternalOntology;
import ontologizer.ontology.Ontology;
import ontologizer.ontology.Term;
import ontologizer.ontology.TermID;
import ontologizer.statistics.None;
import sonumina.collections.IntMapper;

public class CalculationTestUtils
{
	@SuppressWarnings("unchecked")
	public static <T> T prop(EnrichedGOTermsResult result, String id)
	{
		return (T)result.getGOTermProperties(new TermID(id));
	}

	public static int [] asList(IntMapper<TermID> mapper, String...termIDs)
	{
		int [] ids = new int[termIDs.length];
		for (int i = 0; i < termIDs.length; i++)
		{
			ids[i] = mapper.getIndex(tid(termIDs[i]));
		}
		return ids;
	}

	public static int [] asList(IntMapper<TermID> mapper, Collection<?> termIDs)
	{
		int [] ids = new int[termIDs.size()];
		int i = 0;
		for (Object tid : termIDs)
		{
			TermID t;

			if (tid instanceof String)
			{
				String str = (String) tid;
				t = tid(str);
			} else if (tid instanceof TermID)
			{
				t = (TermID)tid;
			} else
			{
				throw new IllegalArgumentException("Type " + tid.getClass().getName() + " is not supported!");
			}

			ids[i] = mapper.getIndex(t);
			i++;
		}
		return ids;
	}

	/**
	 * Perform a standard test calculation using the supplied algorithm
	 * on the initial ontology.
	 *
	 * @param calc the calculation method to use.
	 * @param subOntology set to true if calculation should be done on a subontology only.
	 * @return the result
	 */
	public static EnrichedGOTermsResult performTestCalculation(ICalculation calc, boolean subOntology)
	{
		InternalOntology internalOntology = new InternalOntology();

		final HashMap<TermID,Double> wantedActiveTerms = new HashMap<TermID,Double>(); /* Terms that are active */
		wantedActiveTerms.put(new TermID("GO:0000004"),0.0);

		AssociationContainer assoc = internalOntology.assoc;
		Ontology ontology = internalOntology.graph;

		if (subOntology)
		{
			ontology.setRelevantSubontology("C2");
		}

		EnrichedGOTermsResult [] r = new EnrichedGOTermsResult[2];

		/* Try calculation without (i=0) and with (i=1) synonyms */
		for (int i = 0; i < 2; i++)
		{
			SingleCalculationSetting scs = SingleCalculationSetting.create(new Random(1), wantedActiveTerms, 0.00, ontology, assoc, i==1?internalOntology.synonymMap:null);
			assertEquals(500, scs.pop.getGeneCount());
			assertEquals(57, scs.study.getGeneCount());

			r[i] = calc.calculateStudySet(ontology, assoc, scs.pop, scs.study, new None());
			EnrichedGOTermsTableWriter.writeTable(System.out, r[i]);

			assertEquals(11, r[i].getSize());
			assertEquals(500, r[i].getPopulationGeneCount());
			assertEquals(57, r[i].getStudyGeneCount());
		}

		for (Term t : ontology)
		{
			assertEquals(r[0].getGOTermProperties(t).p_adjusted, r[1].getGOTermProperties(t).p_adjusted, 1e-10);
		}
		return r[0];
	}

	public static EnrichedGOTermsResult performTestCalculation(ICalculation calc)
	{
		return performTestCalculation(calc, false);
	}

	public static void assertResultEquals(Expected[] expected, Class<?> expectedPropClass, EnrichedGOTermsResult r)
	{
		assertEquals(expected.length, r.getSize());

		for (int i = 0; i < expected.length; i++)
		{
			AbstractGOTermProperties p = r.getGOTermProperties(new TermID(expected[i].id));
			String id = expected[i].id;
			assertEquals(expectedPropClass, p.getClass());
			assertEquals("Entry " + id, expected[i].pop, p.annotatedPopulationGenes);
			assertEquals("Entry " + id, expected[i].study, p.annotatedStudyGenes);
			assertEquals("Entry " + id, expected[i].p, p.p, 1e-5);
		}
	}

}
