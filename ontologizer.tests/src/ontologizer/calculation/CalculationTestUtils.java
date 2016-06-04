package ontologizer.calculation;

import ontologizer.ontology.TermID;

public class CalculationTestUtils
{
	public static TermForTermGOTermProperties prop(EnrichedGOTermsResult result, String id)
	{
		return (TermForTermGOTermProperties)result.getGOTermProperties(new TermID(id));
	}
}
