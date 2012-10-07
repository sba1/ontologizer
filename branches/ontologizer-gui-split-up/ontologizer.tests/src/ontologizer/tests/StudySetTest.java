package ontologizer.tests;

import ontologizer.set.StudySet;
import ontologizer.types.ByteString;

import junit.framework.Assert;
import junit.framework.TestCase;

public class StudySetTest extends TestCase
{

	/*
	 * Test method for 'ontologizer.StudySet.generateRandomStudySet(int)'
	 */
	public void testGenerateRandomStudySetInt()
	{
		StudySet bigStudy = new StudySet("BigStudy");
		
		/* Generate the big study */
		for (int i = 0;i<10000;i++)
			bigStudy.addGene(new ByteString("Gene"+i),"Description"+i);

		for (int i = 0; i<100;i++)
		{
			StudySet randomStudy = bigStudy.generateRandomStudySet(100);
			Assert.assertTrue(randomStudy.getGeneCount() == 100);
		}
	}
}
