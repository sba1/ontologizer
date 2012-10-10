package sonumina.math.combinatorics;

public interface IMultisetCallback {
	
	/** 
	 * Method is called when a new element is to be added
	 * into the multi set. Use elementList[pos] to query
	 * which element it is. You can abort to consider any
	 * sub problems by returning -1.
	 *  
	 * @param pos position at which we are
	 * @param elementList the list of elements
	 * @param multiplicitiesPerElement the quantities of each element
	 * 
	 * @return the next starting value. Return -1 to refuse. Return -2 to stop recursion immedeately.
	 */
	int enter ( int pos, int [] elementList, int [] multiplicitiesPerElement );
	
	
	/**
	 * Method is called for every constructed multiset.
	 * 
	 * @param elementList the list of elements
	 * @param multiplicitiesPerElement the quantities of each element
	 */
	void visit ( int [] elementList, int [] multiplicitiesPerElement );
	
	
}
