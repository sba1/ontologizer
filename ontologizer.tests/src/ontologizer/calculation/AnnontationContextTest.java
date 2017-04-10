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

		TermID t1 = new TermID(goPrefix, 1);
		TermID t2 = new TermID(goPrefix, 2);
		TermID t3 = new TermID(goPrefix, 3);
		associations.add(new Association(b("item1"), t1));
		associations.add(new Association(b("item2"), t2));
		associations.add(new Association(b("item3"), t3));
		ArrayList<ByteString> symbols = new ArrayList<ByteString>(associations.size());
		for (Association a : associations)
		{
			symbols.add(a.getObjectSymbol());
		}

		AnnotationContext ac = new AnnotationContext(symbols, new HashMap<ByteString,ByteString>(), new HashMap<ByteString,ByteString>());

		AssociationContainer assoc = new AssociationContainer(associations, ac);
		CalculationContext cc = CalculationContext.create(g, assoc);
		Assert.assertEquals(3, cc.item2Terms.length);

		int i1 = ac.mapSymbol(b("item1"));
		Assert.assertEquals(1, cc.item2Terms[i1].length);
		Assert.assertEquals(g.getVertexIndex(t1), cc.item2Terms[i1][0]);

		Assert.assertEquals(2, cc.item2Terms[ac.mapSymbol(b("item2"))].length);

		Assert.assertEquals(2, cc.item2Terms[ac.mapSymbol(b("item3"))].length);
}
}
