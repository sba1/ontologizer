package ontologizer.tests;

import ontologizer.StudySet;
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

	public void testGenerateShuffledStudySets()
	{
		/* Note, this test fails currently!!! */
		StudySet firstStudy = new StudySet("1st study");
		StudySet secondStudy = new StudySet("2st study");

		/* Generate the first study */
		for (int i = 0;i<200;i++)
			firstStudy.addGene(new ByteString("Gene"+i),"Description"+i);
		
		/* Generate the second study (which overlaps with 1st study
		 * in 100 genes and conists of 200 more genes) */
		for (int i = 100;i<400;i++)
			secondStudy.addGene(new ByteString("Gene"+i),"Description"+i);
		
		StudySet array [] = new StudySet[]{firstStudy,secondStudy};

		for (int i=0;i<100;i++)
		{
			StudySet shuffledArray [] = StudySet.generateShuffledStudySets(array);
			Assert.assertTrue(shuffledArray[0].getGeneCount() == 200);
			Assert.assertTrue(shuffledArray[1].getGeneCount() == 300);
		}
	}
	
}
