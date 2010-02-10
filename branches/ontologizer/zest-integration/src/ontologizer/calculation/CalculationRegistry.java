package ontologizer.calculation;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

import ontologizer.calculation.b2g.Bayes2GOCalculation;

/**
 * 
 * 
 * 
 * @author Sebastian Bauer
 * 
 */
public class CalculationRegistry
{

	/** The default calculation method */
	private static ICalculation def;

	/** The hashmap which contains all calculation methods keyed by their name */
	private static HashMap<String, ICalculation> calculationMap = new HashMap<String, ICalculation>();

	/**
	 * Registers the given Calculation object.
	 * 
	 * @param calculation
	 *            the method which should be registered.
	 */
	public static void registerCalculation(ICalculation calculation)
	{
		calculationMap.put(calculation.getName(), calculation);
	}

	/**
	 * Determines the Calculation class corresponding to the given name.
	 * 
	 * @param name
	 *            defines the name.
	 * @return the corresponding Calculation or null.
	 */
	public static ICalculation getCalculationByName(String name)
	{
		return calculationMap.get(name);
	}

	/**
	 * Returns the default calculation
	 * 
	 * @return the default calculation.
	 */
	public static ICalculation getDefault()
	{
		return def;
	}

	/**
	 * Returns names of all registered Calculations.
	 * 
	 * @return an array containing the names of all registered Calculations.
	 */
	public static String[] getAllRegistered()
	{
		String[] names = new String[calculationMap.keySet().size()];
		Iterator<String> iter = calculationMap.keySet().iterator();

		for (int i = 0; iter.hasNext(); i++)
			names[i] = iter.next();

		Arrays.sort(names);

		return names;
	}

	static public boolean experimentalActivated()
	{
		String enable = System.getenv("ONTOLOGIZER_ENABLE_EXPERIMENTAL"); 
		if (enable != null && enable.equals("yes")) return true;
		return false;
	}

	/**
	 * Register all known calculations.
	 * 
	 * TODO: To make this more flexible we have to go through all
	 * classes (how to get them?) which implements ICalculation
	 * and add them. 
	 */
	static
	{
		def = new ParentChildCalculation();
		registerCalculation(def);
		registerCalculation(new TermForTermCalculation());
		registerCalculation(new ParentChildCutCalculation());
		registerCalculation(new TopCalculation());
		registerCalculation(new TopologyWeightedCalculation());
		registerCalculation(new Bayes2GOCalculation());

		if (experimentalActivated())
		{
			System.err.println("Enabled experimental calculations");
			registerCalculation(new ProbabilisticCalculation());
		}
	}

}
