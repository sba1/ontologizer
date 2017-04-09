package ontologizer.calculation;

import static ontologizer.types.ByteString.b;

import java.util.ArrayList;
import java.util.HashMap;

import org.junit.Assert;
import org.junit.Test;

import ontologizer.association.AnnotationContext;
import ontologizer.association.Association;
import ontologizer.association.AssociationContainer;
import ontologizer.internal.InternalOntology;
import ontologizer.ontology.Prefix;
import ontologizer.ontology.TermID;
import ontologizer.types.ByteString;
import sonumina.math.graph.SlimDirectedGraphView;

public class AnnontationContextTest
{
	@Test
	public void testCreate()
	{
		InternalOntology o = new InternalOntology();
		SlimDirectedGraphView<TermID> g = o.graph.getTermIDSlimGraphView();

		ArrayList<Association> associations = new ArrayList<Association>();
		Prefix goPrefix = new Prefix("GO");

		associations.add(new Association(b("item1"), new TermID(goPrefix, 1)));
		ArrayList<ByteString> symbols = new ArrayList<ByteString>(associations.size());
		for (Association a : associations)
		{
			symbols.add(a.getObjectSymbol());
		}

		AnnotationContext ac = new AnnotationContext(symbols, new HashMap<ByteString,ByteString>(), new HashMap<ByteString,ByteString>());

		AssociationContainer assoc = new AssociationContainer(associations, ac);
		CalculationContext cc = CalculationContext.create(g, assoc);
		Assert.assertEquals(1, cc.item2Terms.length);
		Assert.assertEquals(1, cc.item2Terms[0].length);
		Assert.assertEquals(g.getVertexIndex(new TermID(goPrefix, 1)), cc.item2Terms[0][0]);
	}
}
