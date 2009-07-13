/**
 * 
 */
package ontologizer.sampling;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import ontologizer.go.TermID;

/**
 * Specifies the rule how terms should be enriched in sampling of artificial
 * gene sets. For an arbitary number of terms a rule can be added by specifying
 * the percentage of the term's genes that should appear in the sampled set.
 * 
 * Additionally, one can specify another integer number specifying the
 * percentage of the rest of the terms to appear in the sampled set.
 * 
 * @author grossman
 * 
 */
public class PercentageEnrichmentRule implements Iterable<TermID>
{
	private HashMap<TermID, Integer> termPercentages;

	private int noisePercentage;

	public PercentageEnrichmentRule()
	{
		super();
		termPercentages = new HashMap<TermID, Integer>();
		noisePercentage = 0;
	}

	/**
	 * Gets the percentage of noise genes to add to the sampled set.
	 * 
	 * @return the percentage as an integer value between 0 and 100.
	 */
	public int getNoisePercentage()
	{
		return noisePercentage;
	}

	/**
	 * Sets the percentage of noise genes to add to the sampled set.
	 * 
	 * @param perc
	 *            the percentage as an integer value between 0 and 100.
	 */
	public void setNoisePercentage(int perc) throws IllegalArgumentException
	{
		if (perc < 0 || perc > 100)
			throw(new IllegalArgumentException("noise percentage has to be between 0 and 100"));
		this.noisePercentage = perc;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Iterable#iterator()
	 */
	public Iterator<TermID> iterator()
	{
		return termPercentages.keySet().iterator();
	}

	/**
	 * Adds a term and its sampling percentage to the sampling rule
	 * 
	 * @param term
	 *            the name of the term
	 * @param perc
	 *            the term's sampling percentage (has to be between 0 and 100)
	 */
	public void addTerm(TermID term, int perc)
	{
		if (perc < 0 || perc > 100)
			throw(new IllegalArgumentException("noise percentage has to be between 0 and 100"));
		termPercentages.put(term, perc);
	}

	/**
	 * Gets the percentage for the term if available.
	 * 
	 * @param term
	 *            the name of the term
	 * @return the term's sampling percentage as an integer between 0 and 100
	 */
	public int getPercForTerm(TermID term)
	{
		return termPercentages.get(term);
	}
	
	@Override
	public String toString()
	{
		String str = "";

		for (Entry<TermID, Integer> e : termPercentages.entrySet())
		{
			str += e.getKey() + "/" + e.getValue() + " ";
		}
		str += "noise/" + noisePercentage;
		
		return str;
	}
}
