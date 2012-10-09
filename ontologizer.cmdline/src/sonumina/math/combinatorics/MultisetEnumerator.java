package sonumina.math.combinatorics;

/**
 * This class implements a basic multiset enumerator
 * that enumerates all possible multisets with cardinality
 * q.
 *
 * @author Sebastian Bauer, Sebastian Koehler
 *
 */
public class MultisetEnumerator
{


	private int maxElement;
	private int [] elementList;
	private int [] multiplicitiesPerElement;


	/**
	 *
	 * @param maxElement defines the maximum value of an element of the underlying set
	 *        (size of that set would be maxElement + 1)
	 * @param q defines the cardinallity of the multiset.
	 */
	public MultisetEnumerator(int maxElement, int q)
	{
		this.multiplicitiesPerElement 	= new int[maxElement+1];
		this.maxElement 				= maxElement;
		this.elementList 				= new int[q];
	}

	/**
	 * Enumerates...
	 *
	 * @param callback
	 */
	public void enumerate(IMultisetCallback callback)
	{
		enumerate(0,0,callback);
	}

	/**
	 * Real work is done here.
	 *
	 * @param pos defines the position of the element which is going to be determined.
	 * @param nextValue defines the first value that should be used.
	 * @param callback
	 */
	private void enumerate(int pos, int nextValue, IMultisetCallback callback)
	{
		int i;

		/* Cancelation criterion, we're ready, when we are ready :) */
		if (pos >= elementList.length)
		{
			callback.visit(elementList, multiplicitiesPerElement);
			return;
		}

		for ( i = nextValue ; i <= maxElement ; i++){

			elementList[pos] = i;
			multiplicitiesPerElement[i]++;

			nextValue = callback.enter(pos, elementList, multiplicitiesPerElement);
			if (nextValue > -1)
			{
				/* Sub problem */
				enumerate(pos+1, nextValue, callback);
//				callback.leave(pos);
			}
			else if (nextValue == -2){
				multiplicitiesPerElement[i]--;
				return;
			}

			multiplicitiesPerElement[i]--;
		}
	}

	/**
	 * Just for debugging purposes.
	 */
	public void print()
	{
		for (int k=0;k<elementList.length;k++)
			System.out.print(elementList[k] + " ");
		System.out.println();
	}

	public int getMaxElement() {
		return maxElement;
	}

}
