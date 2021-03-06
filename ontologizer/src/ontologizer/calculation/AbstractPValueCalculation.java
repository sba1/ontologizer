package ontologizer.calculation;

import java.util.Arrays;

import ontologizer.association.AssociationContainer;
import ontologizer.enumeration.TermAnnotations;
import ontologizer.enumeration.TermEnumerator;
import ontologizer.ontology.Ontology;
import ontologizer.ontology.TermID;
import ontologizer.set.PopulationSet;
import ontologizer.set.StudySet;
import ontologizer.statistics.Hypergeometric;
import ontologizer.statistics.IPValueCalculation;
import ontologizer.statistics.IPValueCalculationProgress;
import ontologizer.statistics.PValue;
import ontologizer.types.ByteString;
import sonumina.collections.IntMapper;

public abstract class AbstractPValueCalculation implements IPValueCalculation
{
	private final AssociationContainer associations;
	private final PopulationSet populationSet;
	private final StudySet observedStudySet;
	protected final Hypergeometric hyperg;

	private int totalNumberOfAnnotatedTerms;

	protected IntMapper<ByteString> itemMapper;
	protected IntMapper<TermID> termMapper;
	protected int [][] term2Items;

	public AbstractPValueCalculation(Ontology graph,
			AssociationContainer goAssociations, PopulationSet populationSet,
			StudySet studySet, Hypergeometric hyperg)
	{
		this.associations = goAssociations;
		this.populationSet = populationSet;
		this.observedStudySet = studySet;
		this.hyperg = hyperg;

		initCalculationContext(graph, goAssociations, populationSet);
	}

	private void initCalculationContext(Ontology graph, AssociationContainer goAssociations, StudySet populationSet)
	{
		TermEnumerator populationTermEnumerator = populationSet.enumerateTerms(graph, goAssociations);
		totalNumberOfAnnotatedTerms = 0;
		for (TermID term : populationTermEnumerator)
		{
			if (!graph.isRelevantTermID(term))
				continue;
			totalNumberOfAnnotatedTerms++;
		}

		itemMapper = IntMapper.create(populationTermEnumerator.getGenesAsList());
		termMapper = IntMapper.create(graph.filterRelevant(populationTermEnumerator.getAllAnnotatedTermsAsList()));

		term2Items = new int[totalNumberOfAnnotatedTerms][];

		int i = 0;

		for (TermID term : populationTermEnumerator)
		{
			if (!graph.isRelevantTermID(term))
				continue;

			TermAnnotations tag = populationTermEnumerator.getAnnotatedGenes(term);
			int nTermItems = tag.totalAnnotated.size();

			term2Items[i] = new int[nTermItems];

			int j = 0;
			for (ByteString item : tag.totalAnnotated)
			{
				term2Items[i][j++] = itemMapper.getIndex(item);
			}

			Arrays.sort(term2Items[i]);

			i++;
		}
	}

	protected final int getTotalNumberOfAnnotatedTerms()
	{
		return totalNumberOfAnnotatedTerms;
	}

	public final int currentStudySetSize()
	{
		return observedStudySet.getGeneCount();
	}

	public final int getNumberOfPValues()
	{
		return termMapper.getSize();
	}

	/**
	 * Calculate the p-values for the given study set. The study set must not be the same
	 * as the observed study set.
	 *
	 * @param studyIds the ids of the study set.
	 * @param progress the progress,
	 * @return the array of p-values.
	 */
	protected abstract PValue [] calculatePValues(int [] studyIds, IPValueCalculationProgress progress);

	public final PValue[] calculateRawPValues(IPValueCalculationProgress progress)
	{
		return calculatePValues(CalculationUtils.getUniqueIDs(observedStudySet, itemMapper, associations.getMapping()), progress);
	}

	public final PValue[] calculateRandomPValues(IPValueCalculationProgress progress)
	{
		// TODO: Simply randomly draw integers
		return calculatePValues(CalculationUtils.getUniqueIDs(populationSet.generateRandomStudySet(observedStudySet.getGeneCount()), itemMapper, associations.getMapping()), progress);
	}


	/**
	 * Return the index of the given term.
	 *
	 * @param tid the term whose index shall be determined
	 * @return the index or Integer.MAX if the term id is not known.
	 */
	protected final int getIndex(TermID tid)
	{
		int idx = termMapper.getIndex(tid);
		if (idx == -1)
		{
			return Integer.MAX_VALUE;
		}
		return idx;
	}
}
