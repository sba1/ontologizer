package sonumina.math.combinatorics;

import junit.framework.TestCase;

public class MultisetEnumeratorTest extends TestCase
{
	public void testMultiset()
	{
		/** A common base class (just used to print out the stuff) */
		abstract class MultisetCallback implements IMultisetCallback
		{
			private int stepCount;

			private int [][] asserted;
			
			public MultisetCallback(int [][] asserted)
			{
				this.asserted = asserted;
			}
			
			public void visit(int[] elementList, int[] multiplicitiesPerElement)
			{
				for (int i=0;i<elementList.length;i++)
					assertEquals(asserted[stepCount][i],elementList[i]);
				
				stepCount++;
				
				System.out.print(stepCount + ": ");
				for (int e : elementList)
					System.out.print(e + ",");
//				System.out.println();
				System.out.print(" ");
				for (int i=0;i<multiplicitiesPerElement.length;i++)
				{
					if (multiplicitiesPerElement[i] != 0)
						System.out.print(i + "->" + multiplicitiesPerElement[i] +",");
				}
				System.out.println();
			}
		}

		MultisetEnumerator m = new MultisetEnumerator(3, 3);
		
		/* A basic combination enumerator */
//		System.out.println("Combination");
//		m.enumerate(new MultisetCallback() {
//			public void leave(int pos) {}
//			public int enter(int pos, int[] elementList, int[] multiplicitiesPerElement)
//			{
//				return 0;
//			}
//		});

//		/* A basic subset enumerator */
//		System.out.println("Subset");
//		m.enumerate(new MultisetCallback() {
//			public void leave(int pos) { }
//			@Override
//			public int enter(int pos, int[] elementList, int[] multiplicitiesPerElement)
//			{
//				return elementList[pos] + 1;
//			}
//		});

//		/* A basic multiset enumerator */
//		System.out.println("Multiset");
//		m.enumerate(new MultisetCallback() {
//			@Override
//			public void leave(int pos)
//			{
//			}
//			
//			@Override
//			public int enter(int pos, int[] elementList, int[] multiplicitiesPerElement)
//			{
//				return elementList[pos];
//			}
//		});
//
		/* A basic multiset enumerator with max restrictions */
		System.out.println("Multiset");
		m.enumerate(new MultisetCallback(new int[][]{
			new int[]{0,1,1},
			new int[]{0,1,2},
			new int[]{0,1,3},
			new int[]{0,2,2},
			new int[]{0,2,3},
			new int[]{0,3,3},
			new int[]{1,1,2},
			new int[]{1,1,3},
			new int[]{1,2,2},
			new int[]{1,2,3},
			new int[]{1,3,3},
			new int[]{2,2,2},
			new int[]{2,2,3},
			new int[]{2,3,3},
			new int[]{3,3,3},})
		{
			int [] maxCard = new int[] {1,2,3,3};
			
			public int enter(int pos, int[] elementList, int[] multiplicitiesPerElement)
			{
				int element = elementList[pos];
				
				if (multiplicitiesPerElement[element] > maxCard[element])
					return -1;
				return element;
			}
		});

	}
}
