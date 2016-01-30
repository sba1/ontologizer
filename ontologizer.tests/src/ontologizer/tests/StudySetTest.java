package ontologizer.tests;

import org.junit.Assert;
import org.junit.Test;

import ontologizer.set.StudySet;
import ontologizer.types.ByteString;

public class StudySetTest
{
	/*
	 * Test method for 'ontologizer.StudySet.generateRandomStudySet(int)'
	 */
	@Test
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
