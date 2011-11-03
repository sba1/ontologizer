/*
 * Created on 06.07.2005
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ontologizer.statistics;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

/**
 * This class is responsible to manage all test correction methods
 * (i.e. objects of classes implememting the ITestCorrection interface)
 * 
 * @author Sebastian Bauer
 */
public class TestCorrectionRegistry
{
	/** The default correction */
	private static AbstractTestCorrection def;

	/** The hashmap which contains all test corrections keyed by their name */
	private static HashMap<String, AbstractTestCorrection> correctionMap = new HashMap<String, AbstractTestCorrection>();

	/**
	 * Registers the given TestCorrection Class.
	 * 
	 * @param testCorrection
	 *            which should be registered.
	 */
	public static void registerCorrection(AbstractTestCorrection testCorrection)
	{
		correctionMap.put(testCorrection.getName(), testCorrection);
	}

	/**
	 * Determines the TestCorrection Class corresponding to the given name.
	 * 
	 * @param name
	 *            defines the name.
	 * @return the corresponding TestCorrecion or null.
	 */
	public static AbstractTestCorrection getCorrectionByName(String name)
	{
		return correctionMap.get(name);
	}

	/**
	 * Returns names of all registered Test Corrections.
	 * 
	 * @return an array containing the names of all registered Test Corrections.
	 */
	public static String[] getRegisteredCorrections()
	{
		String[] names = new String[correctionMap.keySet().size()];
		Iterator<String> iter = correctionMap.keySet().iterator();

		for (int i = 0; iter.hasNext(); i++)
			names[i] = iter.next();

		Arrays.sort(names);

		return names;
	}

	/**
	 * Returns the default test correction.
	 * 
	 * @return The default test correction.
	 */
	public static AbstractTestCorrection getDefault()
	{
		return def;
	}

	static
	{
		def = new None();
		registerCorrection(def);

		registerCorrection(new Bonferroni());
		registerCorrection(new BonferroniHolm());
		registerCorrection(new BenjaminiHochberg());
		registerCorrection(new BenjaminiYekutieli());
		registerCorrection(new WestfallYoungStepDown());
		registerCorrection(new WestfallYoungSingleStep());
//		registerCorrection(new WestfallYoungSingleStepApproximate());
//		registerCorrection(new WestfallYoungStepDownCached());
//		registerCorrection(new WestfallYoungStepDownCachedSecondVersion());
//		registerCorrection(new Sidak());
//		registerCorrection(new FDR());
//		registerCorrection(new FDRBySteffen());

//		registerCorrection(new Storey());
	}
}
