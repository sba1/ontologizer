package ontologizer.calculation;

import java.util.Arrays;

import ontologizer.association.AnnotationContext;
import ontologizer.enumeration.TermEnumerator;
import ontologizer.ontology.TermID;
import ontologizer.set.StudySet;
import ontologizer.types.ByteString;
import sonumina.collections.IntMapper;

/**
 * Some utils for preparing calculations.
 *
 * @author Sebastian Bauer
 */
public class CalculationUtils
{
	/**
	 * Creates an array of term to item associations. Each item vector is sorted by
	 * the id.
	 *
	 * @param populationEnumerator
	 * @param termMapper
	 * @param geneMapper
	 * @return the array.
	 */
	public static int [][] makeTermLinks(TermEnumerator populationEnumerator, IntMapper<TermID> termMapper, IntMapper<ByteString> geneMapper)
	{
		int [][] termLinks = new int[termMapper.getSize()][];

		for (int i = 0; i < termMapper.getSize(); i++)
		{
			TermID tid = termMapper.get(i);

			/* Fill in the links */
			termLinks[i] = new int[populationEnumerator.getAnnotatedGenes(tid).totalAnnotated.size()];
			int j=0;
			for (ByteString gene : populationEnumerator.getAnnotatedGenes(tid).totalAnnotated)
			{
				int gid = geneMapper.getIndex(gene);
				if (gid == -1)
				{
					throw new IllegalArgumentException("Gene " + gene.toString() + " is not contained within the gene mapper");
				}
				termLinks[i][j] = gid;
				j++;
			}
			Arrays.sort(termLinks[i]);
		}

		return termLinks;
	}

	/**
	 * Get a unique id representation of the given study set.
	 *
	 * @param studySet the study set
	 * @param itemMapper the mapper for getting unique integer ids.
	 * @param associations the container for getting synonyms.
	 * @return the unique id representation of the study set.
	 */
	public static int[] getUniqueIDs(StudySet studySet, IntMapper<ByteString> itemMapper, AnnotationContext annotationContext)
	{
		int [] studyIds = new int[studySet.getGeneCount()];
		int mappedStudyItems = 0;
		for (ByteString studyItem : studySet)
		{
			int index = itemMapper.getIndex(studyItem);
			if (index == -1)
			{
				/* Try synonyms etc. */
				int id = annotationContext.mapSynonym(studyItem);
				if (id != Integer.MAX_VALUE)
				{
					index = itemMapper.getIndex(annotationContext.getSymbols()[id]);
				}
			}
			if (index != -1)
				studyIds[mappedStudyItems++] = index;
		}

		if (mappedStudyItems != studyIds.length)
		{
			/* This could only happen if there are items in the study set that are not in the population */
			int [] newStudyIds = new int[mappedStudyItems];
			for (int j = 0; j < mappedStudyItems; j++)
			{
				newStudyIds[j] = studyIds[j];
			}
			studyIds = newStudyIds;
		}
		/* Sort for simpler intersection finding */
		Arrays.sort(studyIds);
		return studyIds;
	}

}
