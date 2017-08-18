package ontologizer.calculation;

import java.util.Arrays;
import java.util.List;

import ontologizer.association.AssociationContainer;
import ontologizer.association.ItemAssociations;
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
import sonumina.collections.ObjectIntHashMap;

public abstract class AbstractPValueCalculation implements IPValueCalculation
{
	protected final Ontology graph;
	protected final AssociationContainer associations;
	protected final PopulationSet populationSet;
	protected final StudySet observedStudySet;
	protected final Hypergeometric hyperg;

	private int totalNumberOfAnnotatedTerms;

	protected ObjectIntHashMap<ByteString> item2Index;
	protected TermID [] termIds;
	private ObjectIntHashMap<TermID> termId2Index;
	protected int [][] term2Items;

	public AbstractPValueCalculation(Ontology graph,
			AssociationContainer goAssociations, PopulationSet populationSet,
			StudySet studySet, Hypergeometric hyperg)
	{
		this.graph = graph;
		this.associations = goAssociations;
		this.populationSet = populationSet;
		this.observedStudySet = studySet;
		this.hyperg = hyperg;

		initCalculationContext(graph, goAssociations, populationSet);
	}

	private void initCalculationContext(Ontology graph, AssociationContainer goAssociations, StudySet populationSet)
	{
		TermEnumerator populationTermEnumerator = populationSet.enumerateTerms(graph, goAssociations);
		totalNumberOfAnnotatedTerms = populationTermEnumerator.getTotalNumberOfAnnotatedTerms();

		List<ByteString> itemList = populationTermEnumerator.getGenesAsList();
		item2Index = new ObjectIntHashMap<ByteString>(itemList.size()*3/2);
		int itemId = 0;
		for (ByteString item : itemList)
		{
			item2Index.put(item, itemId++);
		}

		termIds = new TermID[totalNumberOfAnnotatedTerms];
		term2Items = new int[totalNumberOfAnnotatedTerms][];

		int i = 0;

		for (TermID term : populationTermEnumerator)
		{
			TermAnnotations tag = populationTermEnumerator.getAnnotatedGenes(term);
			int nTermItems = tag.totalAnnotated.size();

			term2Items[i] = new int[nTermItems];

			int j = 0;
			for (ByteString item : tag.totalAnnotated)
			{
				term2Items[i][j++] = item2Index.get(item);
			}

			Arrays.sort(term2Items[i]);

			termIds[i] = term;
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
		return termIds.length;
	}

	/**
	 * Calculate the p-values for the given study set. The study set must not be the same
	 * as the observed study set.
	 *
	 * @param studySet the studyset.
	 * @param progress the progress,
	 * @return the array of p-values.
	 */
	protected abstract PValue [] calculatePValues(StudySet studySet, IPValueCalculationProgress progress);

	public final PValue[] calculateRawPValues(IPValueCalculationProgress progress)
	{
		return calculatePValues(observedStudySet, progress);
	}

	public final PValue[] calculateRandomPValues(IPValueCalculationProgress progress)
	{
		return calculatePValues(populationSet.generateRandomStudySet(observedStudySet.getGeneCount()), progress);
	}


	/**
	 * Get a unique id representation of the given study set.
	 *
	 * @param studySet the study set
	 * @return the unique id representation of the study set.
	 */
	protected int[] getUniqueIDs(StudySet studySet)
	{
		int [] studyIds = new int[studySet.getGeneCount()];
		int mappedStudyItems = 0;
		for (ByteString studyItem : studySet)
		{
			int index = item2Index.getIfAbsent(studyItem, Integer.MAX_VALUE);
			if (index == Integer.MAX_VALUE)
			{
				/* Try synonyms etc. */
				ItemAssociations g2a = associations.get(studyItem);
				if (g2a != null)
					index = item2Index.getIfAbsent(g2a.name(), Integer.MAX_VALUE);
			}
			if (index != Integer.MAX_VALUE)
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

	/**
	 * Return the index of the given term.
	 *
	 * @param tid the term whose index shall be determined
	 * @return the index or Integer.MAX if the term id is not known.
	 */
	protected final int getIndex(TermID tid)
	{
		if (termId2Index == null)
		{
			termId2Index = new ObjectIntHashMap<TermID>(termIds.length);
			for (int i = 0; i < termIds.length; i++)
				termId2Index.put(termIds[i], i);
		}
		return termId2Index.getIfAbsentPut(tid, Integer.MAX_VALUE);
	}
}
