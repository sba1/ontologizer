package ontologizer.calculation;

import static ontologizer.calculation.CalculationTestUtils.prop;
import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Random;

import org.junit.Test;

import ontologizer.association.AssociationContainer;
import ontologizer.internal.InternalOntology;
import ontologizer.ontology.Ontology;
import ontologizer.ontology.TermID;
import ontologizer.statistics.None;

public class TermForTermCalculationTest
{
	@Test
	public void whetherTFTWorks()
	{
		class Expected
		{
			public int pop;
			public int study;
			public double p;
			public Expected(int pop, int study, double p)
			{
				this.pop = pop;
				this.study = study;
				this.p = p;
			}
		}
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

		TermForTermGOTermProperties [] p = new TermForTermGOTermProperties[r.getSize()];
		p[0] = prop(r, "GO:0000001");
		p[1] = prop(r, "GO:0000002");
		p[2] = prop(r, "GO:0000003");
		p[3] = prop(r, "GO:0000004");
		p[4] = prop(r, "GO:0000005");
		p[5] = prop(r, "GO:0000006");
		p[6] = prop(r, "GO:0000007");
		p[7] = prop(r, "GO:0000008");
		p[8] = prop(r, "GO:0000009");
		p[9] = prop(r, "GO:00000010");
		p[10] = prop(r, "GO:00000011");

		Expected [] expected = new Expected[]
		{
			new Expected(500, 57, 1.0),
			new Expected(444, 57, 7.4010610e-4),
			new Expected(383, 31, 0.9999801590716945),
			new Expected( 57, 57, 1.6147443e-76),
			new Expected(338, 25, 0.9999779361656085),
			new Expected(333, 26, 0.9998514855899283),
			new Expected(291, 22, 0.9995316242235475),
			new Expected( 63,  5, 0.8758072),
			new Expected(179, 10, 0.9996126881087588),
			new Expected( 66, 5, 0.9011589801093324),
			new Expected( 65,  2, 0.9977121167095124),
		};

		for (int i = 0; i < p.length; i++)
		{
			assertEquals("Entry " + i, expected[i].pop, p[i].annotatedPopulationGenes);
			assertEquals("Entry " + i, expected[i].study, p[i].annotatedStudyGenes);
			assertEquals("Entry " + i, expected[i].p, p[i].p, 1e-5);
		}
	}
}
