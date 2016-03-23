package ontologizer.enumeration;

import static org.junit.Assert.assertEquals;

import java.util.HashSet;

import org.junit.Test;

import ontologizer.association.Gene2Associations;
import ontologizer.enumeration.GOTermEnumerator.GOTermAnnotatedGenes;
import ontologizer.go.TermID;
import ontologizer.internal.InternalOntology;
import ontologizer.types.ByteString;

public class GOTermEnumeratorTest
{
	private static GOTermAnnotatedGenes annotatedGenes(GOTermEnumerator e, String term)
	{
		return e.getAnnotatedGenes(new TermID(term));
	}

	@Test
	public void testEnumeratorOnInternalOntology()
	{
		InternalOntology internal = new InternalOntology();
		GOTermEnumerator e = new GOTermEnumerator(internal.graph);
		for (Gene2Associations g2a : internal.assoc)
			e.push(g2a);

		assertEquals(11, e.getTotalNumberOfAnnotatedTerms());
		assertEquals(500, e.getGenes().size());

		GOTermAnnotatedGenes ag = annotatedGenes(e, "GO:0000001");
		assertEquals(internal.assoc.getAllAnnotatedGenes(), new HashSet<ByteString>(ag.totalAnnotated));
	}
}
