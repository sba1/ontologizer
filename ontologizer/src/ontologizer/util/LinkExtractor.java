package ontologizer.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LinkExtractor
{
	private static final Pattern linkExtractPattern = Pattern.compile("<a([^>]*)>([^<]*)<\\/a>");
	private static final Pattern hrefExtractPattern = Pattern.compile("href=\\\"([^\\\"]*)\\\"");

	private String str;

	public static class Extracted
	{
		public String text;

		/** The actual hrefs */
		public String [] hrefs;

		/** Starts of links */
		public int [] starts;

		/** Ends of links (exclusive) */
		public int [] ends;
	}

	public LinkExtractor(String str)
	{
		this.str = str;
	}

	public Extracted extract()
	{
		List<Integer> start = new ArrayList<Integer>();
		List<Integer> end = new ArrayList<Integer>();
		List<String> hrefs = new ArrayList<String>();

		StringBuilder extract = new StringBuilder();
		Matcher m = linkExtractPattern.matcher(str);
		int offset = 0;
		while (m.find())
		{
			String attrs = m.group(1);

			Matcher hm = hrefExtractPattern.matcher(attrs);
			if (hm.find())
				hrefs.add(hm.group(1));
			else
				hrefs.add("");

			extract.append(str.substring(offset,m.start()));
			start.add(extract.length());
			extract.append(m.group(2));
			end.add(extract.length());

			offset = m.end();
		}
		extract.append(str.substring(offset));

		Extracted e = new Extracted();
		e.text = extract.toString();
		e.starts = new int[start.size()];
		e.ends = new int[start.size()];
		e.hrefs = new String[start.size()];
		for (int i=0; i < start.size(); i++)
		{
			e.starts[i] = start.get(i);
			e.ends[i] = end.get(i);
			e.hrefs[i] = hrefs.get(i);
		}
		return e;
	}
}
