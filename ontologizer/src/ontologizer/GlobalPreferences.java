package ontologizer;

/**
 * Manages the global preferences.
 * 
 * @author Sebastian Bauer
 *
 */
public final class GlobalPreferences
{
	private static String dotPath = "dot";
	private static int numberOfPermutations = 500;
	private static String proxyHost;
	private static int proxyPort;
	private static int wrapColumn = 30;
	private static int mcmcSteps = 500000;

	private static double b2gAlpha = Double.NaN;
	private static double b2gBeta = Double.NaN;
	private static int b2gDT = -1;
	private static double upperAlpha = 1.;
	private static double upperBeta = 1.;

	static
	{
		/* Initialize default values */
		String proxySet = System.getProperty("proxySet", "false");
		proxyPort = 8888;
		proxyHost = "";
		if (proxySet.equalsIgnoreCase("true"))
		{
			proxyHost = System.getProperty("proxyHost", "");
			String portString = System.getProperty("proxyPort", "8888");
			try
			{
				proxyPort = Integer.parseInt(portString);
			} catch (NumberFormatException e)
			{}
		}
	}

	/**
	 * Private constructor to indicate a uninstanciable
	 * class.
	 */
	private GlobalPreferences()
	{
	}

	/**
	 * Returns the DOT path.
	 * 
	 * @return
	 */
	static public String getDOTPath()
	{
		return dotPath;
	}
	
	/**
	 * Sets the DOT path.
	 * 
	 * @param path specifies the new path.
	 */
	static public void setDOTPath(String path)
	{
		dotPath = path;
	}

	/**
	 * Returns the number of permutations.
	 * 
	 * @return
	 */
	static public int getNumberOfPermutations()
	{
		return numberOfPermutations;
	}

	/**
	 * Sets the number of permutations.
	 * 
	 * @param numberOfPermutations2
	 */
	static public void setNumberOfPermutations(int numberOfPermutations2)
	{
		numberOfPermutations = numberOfPermutations2;
	}
	
	/**
	 * Sets the proxy server.
	 * 
	 * @param proxyServer
	 */
	public static void setProxyHost(String proxyServer)
	{
		GlobalPreferences.proxyHost = proxyServer;
	}
	
	/**
	 * Returns the proxy server.
	 * 
	 * @return
	 */
	public static String getProxyHost()
	{
		return proxyHost;
	}
	
	/**
	 * Sets the proxy port.
	 * 
	 * @param proxyPort
	 */
	public static void setProxyPort(int proxyPort)
	{
		GlobalPreferences.proxyPort = proxyPort;
	}

	/**
	 * Sets the proxy port.
	 * 
	 * @param proxyPort
	 */
	public static void setProxyPort(String proxyPort)
	{
		try
		{
			GlobalPreferences.proxyPort = Integer.parseInt(proxyPort);
		} catch (NumberFormatException e){}
	}

	/**
	 * Returns the proxy port.
	 * 
	 * @return
	 */
	public static int getProxyPort()
	{
		return proxyPort;
	}
	
	public static int getWrapColumn()
	{
		return wrapColumn;
	}
	
	public static void setWrapColumn(int wrapColumn)
	{
		GlobalPreferences.wrapColumn = wrapColumn;
	}

	public static double getUpperAlpha()
	{
		return upperAlpha;
	}

	public static void setUpperAlpha(double upperAlpha)
	{
		GlobalPreferences.upperAlpha = upperAlpha;
	}

	public static double getUpperBeta()
	{
		return upperBeta;
	}

	public static void setUpperBeta(double upperBeta)
	{
		GlobalPreferences.upperBeta = upperBeta;
	}

	public static void setMcmcSteps(int mcmcSteps)
	{
		GlobalPreferences.mcmcSteps = mcmcSteps;
	}
	
	public static int getMcmcSteps() {
		return mcmcSteps;
	}

	public static void setAlpha(double alpha)
	{
		GlobalPreferences.b2gAlpha = alpha;
	}
	
	public static double getAlpha()
	{
		return b2gAlpha;
	}

	public static void setBeta(double beta)
	{
		GlobalPreferences.b2gBeta = beta;
	}
	
	public static double getBeta()
	{
		return b2gBeta;
	}

	public static void setExpectedNumber(int terms)
	{
		b2gDT = terms;
	}

	/**
	 * Returns the expected number of terms setting for MGSA.
	 * 
	 * @return
	 */
	public static int getExpectedNumber()
	{
		return b2gDT;
	}

}
