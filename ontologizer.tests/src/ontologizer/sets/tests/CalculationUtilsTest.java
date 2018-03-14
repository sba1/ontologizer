package ontologizer.sets.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import ontologizer.association.AnnotationContext;
import ontologizer.calculation.CalculationUtils;
import ontologizer.set.StudySet;
import ontologizer.types.ByteString;
import sonumina.collections.IntMapper;

import static ontologizer.types.ByteString.b;

public class CalculationUtilsTest
{
	@Test
	public void testGetUniqueIDs()
	{
		InternalDatafiles idf = new InternalDatafiles();
		AnnotationContext annotationContext = idf.assoc.getMapping();

		StudySet studySet = new StudySet();
		studySet.addGene(b("item1"), "");
		studySet.addGene(b("item2"), "");
		studySet.addGene(b("item3"), "");
		studySet.addGene(b("item4"), "");
		studySet.addGene(b("item5"), "");

		IntMapper<ByteString> mapper = IntMapper.create(studySet, studySet.getGeneCount());

		int [] ids = CalculationUtils.getUniqueIDs(studySet, studySet.getGeneCount(), mapper, annotationContext);

		for (int i = 0; i < ids.length; i++)
		{
			assertEquals(i, ids[i]);
		}
	}

	@Test
	public void testGetUniqueIDsWithSynonyms()
	{
		InternalDatafiles idf = new InternalDatafiles();
		AnnotationContext annotationContext = idf.assoc.getMapping();

		StudySet studySet = new StudySet();
		studySet.addGene(b("gene1"), "");
		studySet.addGene(b("gene2"), "");
		studySet.addGene(b("gene3"), "");
		studySet.addGene(b("gene4"), "");
		studySet.addGene(b("gene5"), "");

		IntMapper<ByteString> mapper = IntMapper.create(studySet, studySet.getGeneCount());

		int [] ids = CalculationUtils.getUniqueIDs(studySet, studySet.getGeneCount(), mapper, annotationContext);

		for (int i = 0; i < ids.length; i++)
		{
			assertEquals(i, ids[i]);
		}
	}
}
