/*
 * Created on 28.05.2006
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ontologizer.gui.swt.images;

import java.io.File;
import java.io.InputStream;
import java.util.LinkedList;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;


/**
 * Static class which manages the images of the program.
 * 
 * @author Sebastian Bauer
 */
public class Images
{
	private static LinkedList<Image> imageList = new LinkedList<Image>();
	private static Display display;

	/**
	 * Sets the static display attribute, which is used
	 * by loadImage().
	 * 
	 * @param newDisplay
	 */
	public static void setDisplay(Display newDisplay)
	{
		display = newDisplay;
	}

	/**
	 * Loads the given image.
	 * 
	 * @param fileName
	 * @return
	 */
	public static Image loadImage(String fileName)
	{
		ImageData source;
		try
		{
			source = new ImageData("src/ontologizer/gui/swt/images/" + fileName);
		}
		catch(Exception e)
		{
			InputStream stream = Images.class.getResourceAsStream(fileName);
			if (stream == null)
				return null;
			source = new ImageData(stream);
		}

		Image image = new Image(display, source);

		if (image != null)
			imageList.add(image);
		return image;
	}
	
	/**
	 * Disposes all loaded images. Should be called before the application
	 * leaves.
	 */
	public static void diposeImages()
	{
		for (Image img : imageList)
			img.dispose();
	}
}

