package ontologizer.calculation;

import ontologizer.association.AnnotationContext;
import ontologizer.association.Association;
import ontologizer.association.AssociationContainer;
import ontologizer.ontology.TermID;
import ontologizer.types.ByteString;
import ontologizer.util.Util;
import sonumina.math.graph.SlimDirectedGraphView;

public class CalculationContext
{
	public int [][] item2Terms;

	private CalculationContext()
	{
	}

	public static CalculationContext create(SlimDirectedGraphView<TermID> g, AssociationContainer assocs)
	{
		AnnotationContext mapping = assocs.getMapping();
		ByteString [] symbols = mapping.getSymbols();
		CalculationContext cc = new CalculationContext();
		cc.item2Terms = new int[symbols.length][];

		for (int i = 0; i < symbols.length; i++)
		{
			int [] terms = new int[0];
			for (Association a : assocs.getItemAssociations(i))
			{
				int [] ancestors = g.vertexAncestors[g.getVertexIndex(a.getTermID())];
				terms = Util.union(ancestors, terms);
			}
			cc.item2Terms[i] = terms;
		}
		return cc;
	}
}
