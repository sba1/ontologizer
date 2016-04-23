package ontologizer.enumeration;

import static org.junit.Assert.assertEquals;

import java.util.HashSet;

import org.junit.Test;

import ontologizer.association.Gene2Associations;
import ontologizer.enumeration.TermEnumerator.TermAnnotatedGenes;
import ontologizer.internal.InternalOntology;
import ontologizer.ontology.TermID;
import ontologizer.types.ByteString;

public class GOTermEnumeratorTest
{
	private static TermAnnotatedGenes annotatedGenes(TermEnumerator e, String term)
	{
		return e.getAnnotatedGenes(new TermID(term));
	}

	@Test
	public void testEnumeratorOnInternalOntology()
	{
		InternalOntology internal = new InternalOntology();
		TermEnumerator e = new TermEnumerator(internal.graph);
		for (Gene2Associations g2a : internal.assoc)
			e.push(g2a);

		assertEquals(11, e.getTotalNumberOfAnnotatedTerms());
		assertEquals(500, e.getGenes().size());

		TermAnnotatedGenes ag = annotatedGenes(e, "GO:0000001");
		assertEquals(internal.assoc.getAllAnnotatedGenes(), new HashSet<ByteString>(ag.totalAnnotated));
	}
}
