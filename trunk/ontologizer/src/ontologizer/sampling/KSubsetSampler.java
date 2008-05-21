package ontologizer.sampling;

import java.util.ArrayList;
import java.util.HashSet;

import ontologizer.Hypergeometric;

/**
 * Provides functionality to sample one or many k-subsets from a collection of
 * objects. The main focus at the moment is sampling without replacement.
 * Individual samples are ordered lists of k distinct elements of the objects
 * set. Sampling many of such lists without replacements means that the sampled
 * objects are different as ordered lists, but not necessary as sets.
 * 
 * Observe that there is a big difference between sampling individual k-subsets
 * and sampling a large number of them without replacement.
 * 
 * The idea is that it should provide optimized implementations for different
 * settings.
 * 
 * @author grossman
 * 
 * @param <T>
 *            the type of the objects to be sampled from
 */
public class KSubsetSampler<T>
{
	private ArrayList<T> objects;

	private int nObj;

	private Hypergeometric hyperg;

	/**
	 * The logarithm of the p-value 0.5, needed below
	 */
	private static final double logPcut = Math.log(0.5);

	/**
	 * Constructor. The objects from which to sample have to be provided by
	 * something one can iterate through.
	 * 
	 * @param objects
	 */
	public KSubsetSampler(Iterable<T> objects)
	{
		super();
		this.objects = new ArrayList<T>();
		for (T obj : objects)
		{
			this.objects.add(obj);
		}
		// this.objects.addAll(objects);
		this.nObj = this.objects.size();
		this.hyperg = new Hypergeometric();
	}

	/**
	 * Creates one random k-subset of the object set.
	 * 
	 * @param k
	 *            size of sampled subset
	 * @return the sampled objects
	 */
	public ArrayList<T> sampleOneOrdered(int k)
	{
		ArrayList<T> sample = new ArrayList<T>();

		k = Math.min(k, this.nObj);

		for (int i = this.nObj - 1; i >= this.nObj - k; i--)
		{
			int index = (int) (Math.random() * i);
			sample.add(objects.get(index));
			objects.add(index, objects.remove(i));
			objects.add(i, objects.remove(index + 1));
		}

		return sample;
	}

	/**
	 * Samples a larger number of ordered k-subsets without replacement. This
	 * means that the sampled sets are all distinct in the sense of ordered
	 * subsets not as ordinary subsets.
	 * 
	 * @param k
	 *            size of sampled subsets
	 * @param n
	 *            number of samples
	 * @return the sampled samples
	 * @throws Exception
	 */
	public HashSet<ArrayList<T>> sampleManyOrderedWithoutReplacement(int k,
			int n) throws Exception
	{
		HashSet<ArrayList<T>> samples = new HashSet<ArrayList<T>>();

		double logRejectProb = Math.log(n) - hyperg.logfact(this.nObj)
				+ hyperg.logfact(this.nObj - k);

		ArrayList<T> nextSample;

		if (logRejectProb < logPcut)
		{
			for (int i = 0; i < n; i++)
			{
				while (samples.size() == i)
				{
					nextSample = this.sampleOneOrdered(k);
					if (!samples.contains(nextSample))
					{
						samples.add(nextSample);
					}
				}
			}
		} else if (k == 1)
		{
			ArrayList<T> baseSample = this.sampleOneOrdered(n);
			for (T obj : baseSample)
			{
				ArrayList<T> singleton = new ArrayList<T>();
				singleton.add(obj);
				samples.add(singleton);
			}
		} else
		{
			throw (new Exception("Not implemented yet!"));
		}

		return samples;
	}
}
