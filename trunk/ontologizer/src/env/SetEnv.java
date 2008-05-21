package env;

/**
 * This is class is used for setting environment variables within
 * Ontologizer.
 *  
 * @author Sebastian Bauer
 */
public class SetEnv
{
	static boolean library_loaded = false;

	static
	{
		try
		{
			/* Load the external setenv library, which is linked into
			 * the setenv_native call. */
			System.loadLibrary("setenv");

			library_loaded = true;
		} catch(UnsatisfiedLinkError e)
		{
			/* If library is not found, we don't do anything */
		}
	}

	public static native void setenv_native(String key, String value);

	public static void setenv(String key, String value)
	{
		if (library_loaded)
			setenv_native(key, value);
	}
}
