package ontologizer;

/**
 * This is a simple implementation of a smith waterman local alignment score.
 * 
 * @author Sebastian Bauer
 * @see http://en.wikipedia.org/wiki/Smith%E2%80%93Waterman_algorithm
 */
public class SmithWaterman
{
    /**
     * Similarity of a match.
     */
    static private final int MATCH_SCORE = 10;
    
    /**
     * Similarity of a mismatch.
     */
    static private final int MISMATCH_SCORE = -5;
    
    /**
     * Similarity of a indel.
     */
    static private final int INDEL_SCORE = -10;

    /**
     * Calculate the similarity score between position i of str1 and position j of str2.
     * 
     * @param i starts at 1
     * @param j starts at 1
     * @return
     */
    static private int similarity(String s1, int i, String s2, int j)
    {
	    /* First is always a gap */
		if (i == 0 || j == 0)
		    return INDEL_SCORE;

		if (s1.charAt(i - 1) == s2.charAt(j - 1))
			return MATCH_SCORE;

		return MISMATCH_SCORE;
    }

    /**
	 * Returns the score according to smith waterman.
	 * 
	 * @param s1
	 * @param s2
	 * @return
	 */
	static public int getScore(String s1, String s2)
	{
		int l1,i;
	   	int l2,j;
	   	
		int maxScore;
		
		/* Note that we would only need to keep one line, and not the full matrix,
		 * as we want to calculate the score. But I'm to lazy for now.
		 * 
		 * So here, h(i,j) is the maximum similarity score between suffix
		 * a[1..i] and b[1..j]
		 */
	    int [][] h;

	    l1 = s1.length();
		l2 = s2.length();
	    h = new int[l1+1][l2+1];
	    maxScore = 0;

		/* Initialize base case, first row, and first column */
		for (i = 0; i <= l1; i++) h[i][0] = 0;
		for (j = 1; j <= l2; j++) h[0][j] = 0;
	
		/* Compute the remaining elements of the matrix */
		for (i = 1; i <= l1; i++)
		{
		    for (j = 1; j <= l2; j++)
		    {
		    	int diagScore = h[i - 1][j - 1] + similarity(s1, i, s2, j); /* Advance both/Diag */
		    	int upScore = h[i][j - 1] + similarity(s1, 0, s2, j); /* Insertation/Up */
		    	int leftScore = h[i - 1][j] + similarity(s1, i, s2, 0); /* Deletion/Left */
	
		    	h[i][j] = Math.max(diagScore, Math.max(upScore, Math.max(leftScore, 0)));
		    	
		    	if (h[i][j] > maxScore) maxScore = h[i][j]; 
		    }
		}
		return maxScore;
	}
	

    /**
     * Find the most similar str among the given set of string according to
     * the smith waterman method.
     *  
     * @return the index within the array of strs.
     */
    static public String findMostSimilar(String [] strs, String str)
    {
		double maxS = Double.MIN_VALUE;
		int maxI = -1;
		

		for (int i=0;i<strs.length;i++)
		{
			double s = SmithWaterman.getScore(strs[i],str);
			if (s > maxS)
			{
				maxS = s;
				maxI = i;
			}
		}
		return strs[maxI];
    }
}