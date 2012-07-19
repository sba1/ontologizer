package ontologizer.go;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This is a simple parser for descriptions used in OBO.
 * 
 * @author Sebastian Bauer
 */
public class DescriptionParser
{
	private static final Pattern termPattern = Pattern.compile("`([^`]*)` \\(([^)]*)\\)");
	private static final Pattern textPattern = Pattern.compile("([^`]+)`");
	
	public interface IDescriptionPartCallback
	{
		public boolean part(String txt, String ref);
	}
	
	/**
	 * Parse the given description. For each component, the part() method
	 * of IDescriptionPartCallback is called with appropriate parameters.
	 * 
	 * @param txt text to be parsed.
	 * @param cb callback to be called
	 */
	public static void parse(String txt, IDescriptionPartCallback cb)
	{
		int pos = 0;

		while (pos < txt.length())
		{
			String ptxt = txt.substring(pos);

			Matcher m = termPattern.matcher(ptxt);
			if (m.find() && m.start() == 0)
			{
				pos += m.end();
				cb.part(m.group(1),m.group(2));
			} else
			{
				m = textPattern.matcher(ptxt);
				if (m.find())
				{
					cb.part(m.group(1), null);
					pos += m.end() - 1; /* The ` is important for the next round, so enure that we start with that */
				}
				else
				{
					cb.part(ptxt,null);
					return;
				}
					
			}
		}
	}
	
	/**
	 * Parse the given description using the default filter. It will simply skip over the reference.
	 * 
	 * @param txt
	 * @return
	 */
	public static String parse(String txt)
	{
		final StringBuilder str = new StringBuilder();
		
		DescriptionParser.parse(txt, new DescriptionParser.IDescriptionPartCallback() {
			public boolean part(String txt, String ref)
			{
				str.append(txt);
				return true;
			}
		});

		return str.toString();
	}
	
}
