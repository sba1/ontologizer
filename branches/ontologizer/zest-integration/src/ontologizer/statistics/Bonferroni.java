/*
 * Created on 06.07.2005
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ontologizer.statistics;

import java.lang.Math;

/**
 * This class implements the Bonferroni multiple test correction which is the
 * most conservative approach.
 * 
 * @author Sebastian Bauer
 */
public class Bonferroni extends AbstractTestCorrection
{
	/** The name of the correction method */
	private static final String NAME = "Bonferroni";

	public PValue[] adjustPValues(IPValueCalculation pValueCalculation)
	{
		PValue [] p = pValueCalculation.calculateRawPValues();
		int pvalsCount = countRelevantPValues(p);
		
		/* Adjust the values */
		for (int i=0;i<p.length;i++)
		{
			if (!p[i].ignoreAtMTC)
				p[i].p_adjusted = Math.min(1.0, p[i].p * pvalsCount);
		}
		return p;
	}

	public String getDescription()
	{
		return "The Bonferroni correction is the most conservative approach.";
	}

	public String getName()
	{
		return NAME;
	}
}
