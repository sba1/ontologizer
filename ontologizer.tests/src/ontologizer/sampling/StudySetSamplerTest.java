package ontologizer.sampling;

import java.util.Set;

import junit.framework.Assert;
import junit.framework.TestCase;
import ontologizer.StudySet;
import ontologizer.StudySetFactory;
import ontologizer.association.AssociationContainer;
import ontologizer.tests.AssociationParserTest;
import ontologizer.types.ByteString;

public class StudySetSamplerTest extends TestCase
{
	private StudySet baseStudySet;
	private StudySetSampler studySetSampler;
	private int baseStudySetsize;

	@Override
	protected void setUp() throws Exception
	{
		AssociationParserTest assocPT = new AssociationParserTest();
		assocPT.run();
		// container = assocPT.container;
		AssociationContainer assocContainer = assocPT.assocContainer;

		Set<ByteString> allAnnotatedGenes = assocContainer.getAllAnnotatedGenes();

		String[] allAnnotatedGenesArray = new String[allAnnotatedGenes.size()];
		int i = 0;
		for (ByteString gene : allAnnotatedGenes)
			allAnnotatedGenesArray[i++] = gene.toString();
		
		baseStudySet = StudySetFactory.createFromArray(allAnnotatedGenesArray, false);
		baseStudySet.setName("baseStudy");
		baseStudySetsize = baseStudySet.getGeneCount();
		studySetSampler = new StudySetSampler(baseStudySet);
	}

	public void testBasicSampling()
	{
		StudySet sample;
		int ss;

		ss = 10;
		sample = studySetSampler.sampleRandomStudySet(ss);
		Assert.assertTrue(sample.getGeneCount() == ss);

		ss = 0;
		sample = studySetSampler.sampleRandomStudySet(ss);
		Assert.assertTrue(sample.getGeneCount() == ss);

		sample = studySetSampler.sampleRandomStudySet(baseStudySetsize);
		Assert.assertTrue(sample.getGeneCount() == baseStudySetsize);

		sample = studySetSampler.sampleRandomStudySet(baseStudySetsize + 1);
		Assert.assertTrue(sample.getGeneCount() == baseStudySetsize);
		Assert.assertEquals(sample.getAllGeneNames(), baseStudySet.getAllGeneNames());
	}

}
