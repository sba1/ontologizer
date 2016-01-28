package ontologizer.go;

import java.io.InputStream;

/**
 * An interface wrapping the input for a obo parser.
 *
 * @author Sebastiab Bauer
 */
public interface IOBOParserInput
{
	/**
	 * @return the wrapped input stream
	 */
	public InputStream inputStream();

	/**
	 * Close the associated input streams.
	 */
	public void close();

	/**
	 * @return the size of the contents of the input stream or -1 if this
	 *  information is not available.
	 */
	public int getSize();

	/**
	 * @return the current position of the input.
	 */
	public int getPosition();
}
