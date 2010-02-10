/*
 * Created on 04.09.2005
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ontologizer.util;

import java.io.File;

/**
 * 
 * This class implements some utility functions.
 * 
 * @author Sebastian Bauer
 *
 */
public final class Util
{
	/**
	 * Hides the default constructor.
	 */
	private Util()
	{
	}

	/** 
	 * Returns the File object for the directory where the application
	 * can store persistent data. If directory doesn't exist it will
	 * be created.
	 *
	 * @param appName specifies the name of the application, from which
	 *        the name of the directory is derived.
	 *
	 * @return the path where application dependend files can be stored.
	 */
	static public File getAppDataDirectory(final String appName)
	{
		/* On windows we can try the APPDATA path */
		String dirName = System.getenv("APPDATA");
		if (dirName == null)
		{
			/* If not defined we simply take the user directory */
			dirName = System.getProperty("user.home");
		}

		File app = new File(dirName);
		if (app.exists() && app.isDirectory())
		{
			File onto = new File(app, "." + appName);
			if (!onto.exists())	onto.mkdir();
			if (onto.exists()) return onto;
			return onto;
		}
		return null;
	}

	 /**
     * Wraps a single line of text. 
     * Called by wrapText() to do the real work of wrapping.
     *
     * @param line  a line which is in need of word-wrapping
     * @param newline  the characters that define a newline
     * @param wrapColumn  the column to wrap the words at
     * @return a line with newlines inserted
     */
	/* Taken from WordWrapUtils.java of the jakarta project */
    public static String wrapLine(String line, String newline, int wrapColumn) {
        StringBuilder wrappedLine = new StringBuilder();

        while (line.length() > wrapColumn) {
            int spaceToWrapAt = line.lastIndexOf(' ', wrapColumn);

            if (spaceToWrapAt >= 0) {
                wrappedLine.append(line.substring(0, spaceToWrapAt));
                wrappedLine.append(newline);
                line = line.substring(spaceToWrapAt + 1);
            }

            // This must be a really long word or URL. Pass it
            // through unchanged even though it's longer than the
            // wrapColumn would allow. This behavior could be
            // dependent on a parameter for those situations when
            // someone wants long words broken at line length.
            else {
                spaceToWrapAt = line.indexOf(' ', wrapColumn);

                if (spaceToWrapAt >= 0) {
                    wrappedLine.append(line.substring(0, spaceToWrapAt));
                    wrappedLine.append(newline);
                    line = line.substring(spaceToWrapAt + 1);
                } else {
                    wrappedLine.append(line);
                    line = "";
                }
            }
        }

        // Whatever is left in line is short enough to just pass through
        wrappedLine.append(line);

        return (wrappedLine.toString());
    }	
	/**
	 * For debugging only.
	 * 
	 * @param args defines the args.
	 */
	public static void main(final String[] args)
	{
		System.out.println(getAppDataDirectory("testapp").getAbsolutePath());
	}
}
