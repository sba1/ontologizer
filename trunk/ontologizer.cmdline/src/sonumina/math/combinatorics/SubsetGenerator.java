package sonumina.math.combinatorics;

/**
 * A class to generate stepwise subsets with cardinality not greater than
 * m of the set {0,1,...,n-1}. Note that an empty subset is generated as well.
 * 
 * @author sba
 *
 */
public class SubsetGenerator
{
	static public class Subset
	{
		/** Subset */
		public int [] j;
		
		/** Size of the subset */
		public int r;
	}
	private Subset subset;

	private int n;
	private int m;
	
	/** Indicates whether first subset has already been generated */
	private boolean firstSubset;

	/**
	 * Constructor.
	 * 
	 * @param n defines size of the set 
	 * @param m defines the maximum cardinality of the generated subsets.
	 */
	public SubsetGenerator(int n, int m)
	{
		this.n = n;
		this.m = m;
		firstSubset = true;
		subset = new Subset();
	}
	
	/**
	 * Returns the next subset or null if all subsets have already been created.
	 * Note that the returned array is read only!
	 * 
	 * @return
	 */
	public Subset next()
	{
		if (subset.r==0)
		{
			if (firstSubset)
			{
				firstSubset = false;
				return subset;
			}

			/* Special case when subset of an empty set should be generated */
			if (n == 0)
			{
				firstSubset = true;
				return null;
			}

			/* First call of next inside a subset generating phase */
			subset.j = new int[m];
			subset.r = 1;
			return subset;
		}

		int [] j = subset.j;
		int r = subset.r;

		if (j[r-1] < n-1 && r < m)
		{
			/* extend */
			j[r] = j[r-1] + 1;
			r++;
		} else
		{
			/* modified reduce */
			if (j[r-1] >= n-1)
				r--;
			
			if (r==0)
			{
				subset.r = 0;
				firstSubset = true;
				return null;
			}
			j[r-1] = j[r-1] + 1;
		}
		
		subset.r = r;
		return subset;
	}
}

