package sonumina.util.changelog;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This implementation turns an svn log into an html documented.
 * 
 * @author Sebastian Bauer
 */
public class BuildChangeLog
{
	public static Change [] process(String string)
	{
		ArrayList<Change> list = new ArrayList<Change>(100);
		Pattern pat = Pattern.compile("r(\\d+)\\s+\\|\\s+(\\w+)\\s+\\|\\s+(.+?)\\s+\\|.*?\\$userlog\\$(.*?)-----",Pattern.DOTALL);
		Matcher mat = pat.matcher(string);
		while (mat.find())
		{
			String revision = mat.group(1);
			String author = mat.group(2);
			String date = mat.group(3);
			String log = mat.group(4);
		
			Change c = new Change();
			c.author = author.trim();
			c.date = date.trim();
			c.log = log.trim();
			c.revision = revision.trim();
			
			list.add(c);
		}
		Change [] c = new Change[list.size()];
		list.toArray(c);
		return c;
	}

	public static void main(String[] args)
	{
	}
}
