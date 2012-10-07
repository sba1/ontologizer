
package ontologizer;

import java.lang.reflect.Field;

/**
 * Simple class to get some info about the current
 * Ontologizer instance. 
 * 
 * @author Sebastian Bauer
 *
 */
public class BuildInfo
{
	private static String revisionNumber="NA";
	private static String date ="NA";
	private static String version = "2.1";
	private static String copyright = "2005-2012";
	private static boolean infoExtracted = false;


	/**
	 * Extract the info stored in BuildInfoData.class.
	 */
	private static void extractInfo()
	{
		if (infoExtracted)
			return;
		
		try {
			Class<?> c = Class.forName("ontologizer.BuildInfoData");
			Field r = c.getField("revisionNumber");
			Field d = c.getField("date");
			revisionNumber = (String)r.get(null);
			date = (String)d.get(null);
		} catch (ClassNotFoundException e) {
		} catch (SecurityException e) {
		} catch (NoSuchFieldException e) {
		} catch (IllegalArgumentException e) {
		} catch (IllegalAccessException e) {
		}
		infoExtracted = true;
	}
	
	/**
	 * Returns the revision number.
	 * 
	 * @return
	 */
	public static String getRevisionNumber()
	{
		extractInfo();
		
		return revisionNumber;
	}
	
	/**
	 * Returns the compilation date.
	 * 
	 * @return
	 */
	public static String getDate()
	{
		extractInfo();

		return date;
	}
	
	/**
	 * Returns the version string.
	 * 
	 * @return
	 */
	public static String getVersion()
	{
		return version;
	}

	/**
	 * Returns the copyright years.
	 * 
	 * @return
	 */
	public static String getCopyright()
	{
		return copyright;
	}
	
	/**
	 * Returns the build string.
	 * 
	 * @return
	 */
	public static String getBuildString()
	{
		return BuildInfo.getDate() + "-" + BuildInfo.getRevisionNumber();
	}
}
