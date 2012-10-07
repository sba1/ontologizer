/*
 * Created on 06.07.2005
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ontologizer.statistics;

import java.util.Arrays;

/**
 * This class implements the Bonferroni-Holm (or step down) multiple test
 * correction.
 * 
 * @author Sebastian Bauer
 * 
 */

public class BonferroniHolm extends AbstractTestCorrection
{
	/** The name of the correction method */
	private static final String NAME = "Bonferroni-Holm";

	public PValue[] adjustPValues(IPValueCalculation pValueCalculation)
	{
		PValue [] p = pValueCalculation.calculateRawPValues();
		PValue [] relevantP = getRelevantRawPValues(p);
		Arrays.sort(relevantP);

		/* Adjust the p values. Note that all object within relevantP
		 * also are objects within p!
		 */
		for (int i=0;i<relevantP.length;i++)
		{
			relevantP[i].p_adjusted = relevantP[i].p * (relevantP.length - i);
		}
		enforcePValueMonotony(relevantP);
		return p;
	}

	public String getDescription()
	{
		return "The Bonferronie-Holm multiple test correction";
	}

	public String getName()
	{
		return NAME;
	}
}
