/*
 * myException.java
 *
 */

package ontologizer;

@SuppressWarnings("serial")
public class myException extends Exception
{

	protected int linenum;

	protected String line;

	public myException(String message, String line, int linenum)
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
