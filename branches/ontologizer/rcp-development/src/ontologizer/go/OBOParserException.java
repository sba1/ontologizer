/*
 * myException.java
 *
 */

package ontologizer.go;


/**
 * An exception which may be thrown by the OBOParser class.
 * 
 * @see OBOParser
 * @author Sebastian Bauer
 *
 */
public class OBOParserException extends Exception
{
	/** Serial UID */
	private static final long serialVersionUID = 1L;

	protected int linenum;
	protected String line;

	public OBOParserException(String message, String line, int linenum)
	{
		super(message);
		this.line = line;
		this.linenum = linenum;
	}

	public String getLine()
	{
		return line;
	}

	public int getLineNum()
	{
		return linenum;
	}

	public String toString()
	{
		return "Error: " + getMessage() + "\n" + "on line: " + linenum
				+ "\nline: " + line;
	}
}
