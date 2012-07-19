package ontologizer.sampling;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;

import ontologizer.association.AssociationContainer;
import ontologizer.enumeration.GOTermEnumerator;
import ontologizer.enumeration.GOTermEnumerator.GOTermAnnotatedGenes;
import ontologizer.go.Ontology;
import ontologizer.go.TermID;
import ontologizer.set.StudySet;
import ontologizer.types.ByteString;

/**
 * @author grossman
 * 
 */
public class StudySetSampler
{
	private StudySet baseStudySet;
	private Random rnd;

	/**
	 * Adds a sample of a desired size from a list of genes to an existing sutdy
	 * set
	 * 
	 * @param desiredSize
	 *            the number of genes to sample from the available genes
	 * @param studySet
	 *            the study set to which to add the sampled genes
	 * @param repository
	 *            the repository of available genes from which to draw the
	 *            sample
	 */
	private void addSampleToStudySet(int desiredSize, StudySet studySet,
			Iterable<ByteString> repository)
	{
		if (desiredSize <= 0)
			return;

		// Check first how much we already have to prevent from over-sampling.
		HashSet<ByteString> cleanedRepository = new HashSet<ByteString>();
		for (ByteString entry : repository)
		{
			cleanedRepository.add(entry);
		}
		int nRepTotal = cleanedRepository.size();
		HashSet<ByteString> weHave = studySet.getAllGeneNames();
		int nSampleTotal = weHave.size();
		weHave.retainAll(cleanedRepository);
		int desiredBefore = desiredSize;
		int nWeHave = weHave.size();
		desiredSize -= nWeHave;
		if (desiredSize <= 0)
		{
			System.err.println("We already have more than we want of the term to enrich! "
					+ "nRepTotal = "
					+ nRepTotal
					+ "; nSampleTotal = "
					+ nSampleTotal
					+ "; desiredBefore = "
					+ desiredBefore
					+ "; nWeHave = " + nWeHave);
			return;
		}
		cleanedRepository.removeAll(weHave);
		KSubsetSampler<ByteString> sampler = new KSubsetSampler<ByteString>(cleanedRepository,rnd);
		ArrayList<ByteString> sample = sampler.sampleOneOrdered(desiredSize);

		for (ByteString gene : sample)
		{
			studySet.addGene(gene, baseStudySet.getGeneDescription(gene));
		}
	}

	/**
	 * @param baseStudySet
	 *            The study set from which to sample
	 */
	public StudySetSampler(StudySet baseStudySet)
	{
		this.baseStudySet = baseStudySet;
		this.rnd = new Random();
	}

	/**
	 * 
	 * @param baseStudySet
	 * @param rnd
	 */
	public StudySetSampler(StudySet baseStudySet, Random rnd)
	{
		this.baseStudySet = baseStudySet;
		this.rnd = rnd;
	}

	/**
	 * Samples a random sub study set from the class's base study set
	 * 
	 * @param desiredSize
	 *            the desired size of the sampled subset
	 * @return The sampled study set as a ontologizer.StudySet object
	 */
	public StudySet sampleRandomStudySet(int desiredSize)
	{
		StudySet sampledStudySet = new StudySet();

		addSampleToStudySet(desiredSize, sampledStudySet, baseStudySet);
		return sampledStudySet;
	}

	/**
	 * Samples a random sub study set of random size from the class's base study
	 * set. The size is a uniformly distributed fraction of the base study set's
	 * size.
	 * 
	 * @return the sampled study set as an ontologizer.StudySet object.
	 */
	public StudySet sampleRandomStudySet()
	{
		int desiredSize;

		/* Determine the desired size of the study set randomly */
		desiredSize = (int) Math.floor(rnd.nextDouble()
				* baseStudySet.getGeneCount());

		return sampleRandomStudySet(desiredSize);
	}

	/**
	 * Samples a random sub study set from the class's base study set. In the
	 * sampled set the presence of some terms can be controlled as follows: The
	 * terms to control are listed in the call and for each term the percentage
	 * at which its genes should be present in the sampled study set has to be
	 * specified. Additionally, the percentage of genes that should be sampled
	 * from the rest has to be given.
	 * 
	 * Observe that there is a fundamental difference between the sampling of
	 * the listed genes and the sampling from the rest. For each listed gene,
	 * the method ensures that the specified percentage of its genes will be
	 * present in the sampled set. For the rest of the genes the specified
	 * fraction is sampled from the whole set, giving no control over the
	 * fraction sampled from an individual term.
	 * 
	 * @param graph
	 *            the underlying ontology structure
	 * @param associationContainer
	 *            the gene-term associations
	 * @param enrichRule
	 *            the rule specifying how to enrich terms
	 * @param failIfZero
	 *            if set to true, no study set will be created
	 *            if a desired study set size would be zero.
	 *            In this case, null is returned.
	 * @return the sampled study set
	 */
	public StudySet sampleRandomStudySet(Ontology graph,
			AssociationContainer associationContainer,
			PercentageEnrichmentRule enrichRule,
			boolean failIfZero)
	{
		StudySet sampledStudySet = new StudySet();

		GOTermEnumerator termEnum = baseStudySet.enumerateGOTerms(graph,
				associationContainer);

		HashSet<ByteString> seenGenes = new HashSet<ByteString>();

		for (TermID id : enrichRule)
		{
			// TermID id = termsToOverRepresent[i];
			GOTermAnnotatedGenes annoGenes = termEnum.getAnnotatedGenes(id);
			seenGenes.addAll(annoGenes.totalAnnotated);
			int sampleSize = (int) (0.01 * annoGenes.totalAnnotatedCount() * enrichRule.getPercForTerm(id));
			if (sampleSize == 0)
				return null;
			addSampleToStudySet(sampleSize, sampledStudySet,
					annoGenes.totalAnnotated);
		}

		HashSet<ByteString> genesRest = new HashSet<ByteString>();
		for (TermID id : termEnum)
		{
			GOTermAnnotatedGenes annoGenes = termEnum.getAnnotatedGenes(id);
			genesRest.addAll(annoGenes.totalAnnotated);
		}

		genesRest.removeAll(seenGenes);
		int restSampleSize = (int) (0.01 * genesRest.size() * enrichRule.getNoisePercentage());
		addSampleToStudySet(restSampleSize, sampledStudySet, genesRest);
		return sampledStudySet;
	}

	/**
	 * An alternative way to over-represent terms by increasing the sampling
	 * probability for the genes annotated to the terms.
	 * 
	 * @param desiredSize
	 *            the desired size of the sample
	 * @param termsToOverRepresent
	 *            a list of terms to over-represent
	 * @param OverRepresentationRatio
	 *            the ratio specifying how much higher the sampling probability
	 *            should be for the genes of the specified terms
	 * @return the sapled study set
	 */
	public StudySet sampleRandomStudySet(Ontology graph,
			AssociationContainer associationContainer, int desiredSize,
			TermID[] termsToOverRepresent, double OverRepresentationRatio)
	{
		StudySet studySet = new StudySet();

		GOTermEnumerator termEnum = baseStudySet.enumerateGOTerms(graph,
				associationContainer);

		/*
		 * We will create two HashSets: One containing the genes to
		 * over-represent, the other containing the rest.
		 */
		HashSet<ByteString> genesToOverRepresent = new HashSet<ByteString>();

		for (TermID id : termsToOverRepresent)
		{
			GOTermAnnotatedGenes annoGenes = termEnum.getAnnotatedGenes(id);
			for (ByteString gene : annoGenes.totalAnnotated)
				genesToOverRepresent.add(gene);
		}

		HashSet<ByteString> genesRest = studySet.getAllGeneNames();
		for (ByteString gene : genesToOverRepresent)
		{
			genesRest.remove(gene);
		}
		WeightedUrn<ByteString> termUrn = new WeightedUrn<ByteString>(genesToOverRepresent, genesRest, OverRepresentationRatio);

		HashSet<ByteString> sampledGenes = termUrn.sample(desiredSize);

		for (ByteString gene : sampledGenes)
		{
			studySet.addGene(gene, baseStudySet.getGeneDescription(gene));
		}

		return studySet;
	}

}
