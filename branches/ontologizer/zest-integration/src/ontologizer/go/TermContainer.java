package ontologizer.go;

import java.util.*;

/**
 * A Container class for the terms parsed by OBOParser. The class stores the
 * parsed terms as a HashMap. While OBOParser basically has to do with input
 * from the gene_ontology.obo file, this class has to do more with storing and
 * processing the information about GO terms.
 *
 * This class implements the Iterable interface so you can iterate over all
 * GOTerms conveniently.
 *
 * @author Peter N. Robinson, Sebastian Bauer, Steffen Grossmann
 */

public class TermContainer implements Iterable<Term>
{
	/** The set of GO terms */
	private HashMap<TermID, Term> goterms;

	/** To allow easy iteration over all GO terms */
	private LinkedList<Term> goTermList;

	/** Format version of the gene_ontology.obo file */
	private String format_version;

	/** Date of the gene_ontology.obo file */
	private String date;

	public TermContainer(Set<Term> terms, String format, String datum)
	{
		format_version = format;
		date = datum;

		/* Build our data structures linked list */
		goterms = new HashMap<TermID, Term>();
		goTermList = new LinkedList<Term>();
		for (Term entry : terms)
		{
			goterms.put(entry.getID(), entry);
			goTermList.add(entry);
		}
	}

	/**
	 * Returns the number of terms stored in this container.
	 */
	public int termCount()
	{
		return goterms.size();
	}

	public String getFormatVersion()
	{
		return format_version;
	}

	public String getDate()
	{
		return date;
	}

	/**
	 * Given a GO:id such as GO:0001234 get the corresponding English name
	 */
	public String getGOName(String GOid)
	{
		Term got = get(GOid);
		if (got == null)
			return null;
		else
			return got.getName();
	}

	/**
	 * Given a GO id get the corresponding English name
	 */
	public String getGOName(TermID id)
	{
		Term got = get(id);
		if (got == null)
			return null;
		else
			return got.getName();
	}

	public Term get(TermID id)
	{
		return goterms.get(id);
	}

	public Term get(String id)
	{
		TermID tempID = new TermID(id);
		return goterms.get(tempID);
	}

	/** The following is intended for debugging purposes. */
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("*****\n---Term Container---\n*****\n");
		sb.append("gene_ontology.obo format-version " + getFormatVersion()
				+ " from " + getDate() + " was parsed.\n");
		sb.append("A total of " + termCount() + " terms were identified.\n");
		return sb.toString();
	}

	public Iterator<Term> iterator()
	{
		return goTermList.iterator();
	}

}