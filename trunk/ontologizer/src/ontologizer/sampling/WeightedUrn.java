package ontologizer.sampling;

import java.util.HashSet;
import java.util.LinkedList;

public class WeightedUrn<E>
{
	private HashSet<E> numeratorObjects;
	private HashSet<E> denominatorObjects;
	private double ratio;
	
	/**
	 * @param numeratorObjects
	 * @param denominatorObjects
	 * @param ratio
	 */
	public WeightedUrn(HashSet<E> numeratorObjects, HashSet<E> denominatorObjects, double ratio)
	{
		this.numeratorObjects = numeratorObjects;
		this.denominatorObjects = denominatorObjects;
		this.ratio = ratio;
		
		/*
		 * Make sure object sets are disjoint
		 */
		int countRemoved = 0;
		for (Object numeratorObject : this.numeratorObjects) {
			if (this.denominatorObjects.contains(numeratorObject)) {
				this.denominatorObjects.remove(numeratorObject);
				countRemoved++;
			}
		}
		if (countRemoved > 0) {
			System.err.println("Warning: Removed " + countRemoved + " denominatorObjects to assure disjointness of Sets!");
		}
	}
	
	public HashSet<E> sample(int desiredSize) {
		HashSet<E> sampledObjects = new HashSet<E>();
		
		int n = desiredSize;
		int restInNumerator = this.numeratorObjects.size();
		int restInDenominator = this.denominatorObjects.size();

		LinkedList<E> numeratorList = new LinkedList<E>(this.numeratorObjects);
		LinkedList<E> denominatorList = new LinkedList<E>(this.denominatorObjects);
		
		while ((restInNumerator + restInDenominator) > 0 && n > 0) {
			double sampleCutoff = (restInNumerator * ratio) / (restInNumerator * ratio + (double) restInDenominator);
			
			if (Math.random() < sampleCutoff) {
				int which = (int)(Math.random() * restInNumerator);
				sampledObjects.add(numeratorList.remove(which));
				restInNumerator = numeratorList.size();
			} else {
				int which = (int)(Math.random() * restInDenominator);
				sampledObjects.add(denominatorList.remove(which));
				restInDenominator = denominatorList.size();
			}
			n--;
		}
		
		if (n>0) {
			System.err.println("Warning: You tried to sample more objects than available!");
		}
		
		return sampledObjects;
	}
}
