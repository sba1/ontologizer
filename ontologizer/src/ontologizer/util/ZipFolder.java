package ontologizer.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipFolder
{
	/**
	 * Zip one level of the given folder.
	 *
	 * @param folder the folder to be zipped
	 * @param archiveName the name of the resulting archive
	 * @throws FileNotFoundException if file could not be found
	 * @throws IOException on general io error
	 */
	public static void zip(File folder, String archiveName) throws FileNotFoundException, IOException
	{
		/* Write out a zip archive containing all the data of the project */
		String projectName = folder.getName();
		ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(archiveName));
		ZipEntry entry = new ZipEntry(projectName + "/");
		zip.putNextEntry(entry);
		zip.closeEntry();

		byte [] buffer = new byte[4096];

		String [] names = folder.list();
		for (String name : names)
		{
			File f = new File(folder,name);
			FileInputStream in = new FileInputStream(f);

			try
			{
				/* Add zip entry to the output stream */
				zip.putNextEntry(new ZipEntry(projectName + "/" + name));
				int len;
				while ((len = in.read(buffer)) > 0)
					zip.write(buffer, 0, len);
				zip.closeEntry();
			} finally
			{
				in.close();
			}
		}

		zip.close();
	}
}
