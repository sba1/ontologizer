package sonumina.util.changelog;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This implementation turns a git log into an text document for user
 * consumption. It calls "git log" on the current directory.
 *
 * @author Sebastian Bauer
 */
public class BuildChangeLog
{
	public static Change [] process(String string)
	{
		int revision = 0;
		ArrayList<Change> list = new ArrayList<Change>(100);
		String [] commits = string.split("\f");

		Pattern pat = Pattern.compile(".*\\$foruser\\$(.*)",Pattern.DOTALL|Pattern.MULTILINE);

		DateFormat df = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z");

		for (String c : commits)
		{
			String [] cols = c.trim().split("\t");
			if (cols.length < 4) continue;
			String author = cols[1];
			String date = cols[2];
			String logs = cols[3];

			revision++;

			logs = logs.split("git-svn-id:")[0];

			try
			{
				Date parsedDate = df.parse(date);

				Matcher mat = pat.matcher(logs);
				if (!mat.find()) continue;

				Change ch = new Change();
				ch.revision = revision;
				ch.authorString = author.trim();
				ch.logString = mat.group(1).trim();
				ch.dateString = date;
				ch.date = parsedDate;

				list.add(ch);
			} catch (ParseException e)
			{
				e.printStackTrace();
			}
		}

		int totalRevisions = revision;

		Change [] c = new Change[list.size()];
		list.toArray(c);
		for (int i=0; i < c.length; i++)
		{
			c[i].revision = totalRevisions - c[i].revision;
			c[i].revisionString = Integer.toString(c[i].revision);
		}
		return c;
	}

	public static void main(String[] args) throws IOException, InterruptedException
	{
		PrintStream out = System.out;

		String path = ".";

		if (args.length > 0)
		{
			path = args[0];
			if (args.length > 1)
			{
				System.err.println("Writing to \"" + args[1] + "\"");
				out = new PrintStream(new FileOutputStream(args[1]));
			}
		}
		path = new File(path).getCanonicalPath();
		System.err.println("Getting log for \"" + path + "\"");

		/* Start git log and read the output */
		Process gitProcess = Runtime.getRuntime().exec(new String[]{"git", "log", "--pretty=format:%h%x09%an%x09%ad%x09%B%x0c"}, null, new File(path));
		BufferedReader br = new BufferedReader(new InputStreamReader(gitProcess.getInputStream()));
		StringBuilder str = new StringBuilder();
		String line;
		while ((line = br.readLine())!=null)
			str.append(line + "\n");

		int rc = gitProcess.waitFor();
		BufferedReader err = new BufferedReader(new InputStreamReader(gitProcess.getErrorStream()));
		System.err.println("The git command returned " + rc);
		while ((line = err.readLine())!=null)
			System.err.println(line);

		/* Process the output */
		Change [] changes = process(str.toString());
		for (Change change : changes)
		{
			out.println(DateFormat.getDateInstance(DateFormat.MEDIUM).format(change.date) + " - r" + change.revisionString);
			out.println(" - " + change.logString + " (" + change.authorString + ")");
			out.println();
		}

		System.err.println("Wrote " + changes.length + " entries");
	}
}
