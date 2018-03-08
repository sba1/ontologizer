package ontologizer.sets.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import ontologizer.association.AnnotationContext;
import ontologizer.calculation.CalculationUtils;
import ontologizer.set.StudySet;
import ontologizer.types.ByteString;
import sonumina.collections.IntMapper;

public class CalculationUtilsTest
{
	@Test
	public void testGetUniqueIDs()
	{
		InternalDatafiles idf = new InternalDatafiles();
		AnnotationContext annotationContext = idf.assoc.getMapping();

		StudySet studySet = new StudySet();
		studySet.addGene(new ByteString("item1"), "");
		studySet.addGene(new ByteString("item2"), "");
		studySet.addGene(new ByteString("item3"), "");
		studySet.addGene(new ByteString("item4"), "");
		studySet.addGene(new ByteString("item5"), "");

		IntMapper<ByteString> mapper = IntMapper.create(studySet, studySet.getGeneCount());

		int [] ids = CalculationUtils.getUniqueIDs(studySet, studySet.getGeneCount(), mapper, annotationContext);

		for (int i = 0; i < ids.length; i++)
		{
			assertEquals(i, ids[i]);
		}
	}
}
