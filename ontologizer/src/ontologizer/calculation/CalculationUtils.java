package ontologizer.calculation;

import java.util.Arrays;

import ontologizer.enumeration.TermEnumerator;
import ontologizer.ontology.TermID;
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


}
