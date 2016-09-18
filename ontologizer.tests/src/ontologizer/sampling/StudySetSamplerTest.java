package ontologizer.sampling;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ontologizer.association.AssociationContainer;
import ontologizer.association.AssociationParser;
import ontologizer.ontology.OBOParser;
import ontologizer.ontology.OBOParserFileInput;
import ontologizer.ontology.OBOParserTest;
import ontologizer.ontology.TermContainer;
import ontologizer.set.StudySet;
import ontologizer.set.StudySetFactory;
import ontologizer.types.ByteString;

public class StudySetSamplerTest
{
	private StudySet baseStudySet;
	private StudySetSampler studySetSampler;
	private int baseStudySetsize;

	private final static String GOAssociationFile = "data/gene_association.sgd.gz";

	@Before
	public void setUp() throws Exception
	{
		/* FIXME: Duplicated from AssociationParserTest2 */
		OBOParser oboParser = new OBOParser(new OBOParserFileInput(OBOParserTest.GOtermsOBOFile));
		oboParser.doParse();
		TermContainer container = new TermContainer(oboParser.getTermMap(), oboParser.getFormatVersion(), oboParser.getDate());
		AssociationParser assocParser = new AssociationParser(new OBOParserFileInput(GOAssociationFile), container, null);
		AssociationContainer assocContainer = new AssociationContainer(assocParser.getAssociations(), assocParser.getAnnotationMapping());

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

	@Test
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
