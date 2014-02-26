package ontologizer.calculation.b2g;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import ontologizer.association.AssociationContainer;
import ontologizer.calculation.EnrichedGOTermsResult;
import ontologizer.calculation.ICalculation;
import ontologizer.calculation.ICalculationProgress;
import ontologizer.enumeration.GOTermEnumerator;
import ontologizer.go.Ontology;
import ontologizer.go.TermID;
import ontologizer.parser.ItemAttribute;
import ontologizer.parser.ValuedItemAttribute;
import ontologizer.set.PopulationSet;
import ontologizer.set.StudySet;
import ontologizer.statistics.AbstractTestCorrection;
import ontologizer.types.ByteString;

/**
 * This class implements an model-based analysis. The description of the entire
 * method can be found in "GOing Bayesian: model-based gene set analysis of genome-scale data"
 *
 * @see http://nar.oxfordjournals.org/content/early/2010/02/19/nar.gkq045.short
 * @author Sebastian Bauer
 */
public class Bayes2GOCalculation implements ICalculation
{
	private static Logger logger = Logger.getLogger(Bayes2GOCalculation.class.getCanonicalName());

	private boolean WRITE_STATS_FILE = false;

	private long seed = 0;

	private boolean usePrior = true;

	private boolean integrateParams = false;

	private DoubleParam alpha = new DoubleParam(B2GParam.Type.MCMC);
	private DoubleParam beta = new DoubleParam(B2GParam.Type.MCMC);
	private IntegerParam expectedNumberOfTerms = new IntegerParam(B2GParam.Type.MCMC);

	private boolean takePopulationAsReference = false;
	private ICalculationProgress calculationProgress;

	private boolean randomStart = false;

	private int mcmcSteps = 1020000;
	private int updateReportTime = 1000; /* Update report time in ms */

	public Bayes2GOCalculation()
	{
	}

	public Bayes2GOCalculation(Bayes2GOCalculation calc)
	{
		this.usePrior = calc.usePrior;
		this.expectedNumberOfTerms = new IntegerParam(calc.expectedNumberOfTerms);
		this.alpha = new DoubleParam(calc.alpha);
		this.beta = new DoubleParam(calc.beta);
		this.seed = calc.seed;
		this.calculationProgress = calc.calculationProgress;
		this.takePopulationAsReference = calc.takePopulationAsReference;
		this.mcmcSteps = calc.mcmcSteps;
	}

	/**
	 * Sets the seed of the random calculation.
	 *
	 * @param seed
	 */
	public void setSeed(long seed)
	{
		this.seed = seed;
	}

	/**
	 * Sets a fixed value for the alpha parameter.
	 *
	 * @param alpha
	 */
	public void setAlpha(double alpha)
	{
		if (alpha < 0.000001) alpha = 0.000001;
		if (alpha > 0.999999) alpha = 0.999999;
		this.alpha.setValue(alpha);
	}

	/**
	 * Sets a fixed value for the beta parameter.
	 *
	 * @param beta
	 */
	public void setBeta(double beta)
	{
		if (beta < 0.000001) beta = 0.000001;
		if (beta > 0.999999) beta = 0.999999;
		this.beta.setValue(beta);
	}

	/**
	 * Sets the type of the alpha parameter.
	 *
	 * @param alpha
	 */
	public void setAlpha(B2GParam.Type alpha)
	{
		this.alpha.setType(alpha);
	}

	/**
	 * Sets the bounds of the alpha parameter.
	 *
	 * @param min
	 * @param max
	 */
	public void setAlphaBounds(double min, double max)
	{
		this.alpha.setMin(min);
		this.alpha.setMax(max);
	}

	/**
	 * Sets the type of the beta parameter.
	 *
	 * @param beta
	 */
	public void setBeta(B2GParam.Type beta)
	{
		this.beta.setType(beta);
	}

	/**
	 * Sets the bounds of the beta parameter.
	 *
	 * @param min
	 * @param max
	 */
	public void setBetaBounds(double min, double max)
	{
		this.beta.setMin(min);
		this.beta.setMax(max);
	}

	/**
	 * Sets the expected number of terms.
	 *
	 * @param expectedNumber
	 */
	public void setExpectedNumber(int expectedNumber)
	{
		this.expectedNumberOfTerms.setValue(expectedNumber);
	}

	/**
	 * Sets the type of expected number variable.
	 *
	 * @param expectedNumber
	 */
	public void setExpectedNumber(B2GParam.Type type)
	{
		this.expectedNumberOfTerms.setType(type);
	}

	/**
	 * Set whether the parameter should be integrated.
	 *
	 * @param integrateParams
	 */
	public void setIntegrateParams(boolean integrateParams)
	{
		this.integrateParams = integrateParams;
	}

	/**
	 * Sets whether all terms that are annotated to the population set should be
	 * considered.
	 *
	 * @param takePopulationAsReference
	 */
	public void setTakePopulationAsReference(boolean takePopulationAsReference)
	{
		this.takePopulationAsReference = takePopulationAsReference;
	}

	/**
	 * Sets the number of mcmc steps that are performed in the following runs.
	 *
	 * @param mcmcSteps
	 */
	public void setMcmcSteps(int mcmcSteps)
	{
		this.mcmcSteps = mcmcSteps;
	}

	/**
	 * Sets whether a random start should be used.
	 *
	 * @param randomStart
	 */
	public void useRandomStart(boolean randomStart)
	{
		this.randomStart = randomStart;
	}

	/**
	 * Sets the update report time.
	 *
	 * @param updateReportTime
	 */
	public void setUpdateReportTime(int updateReportTime)
	{
		this.updateReportTime = updateReportTime;
	}

	public EnrichedGOTermsResult calculateStudySet(Ontology graph,
			AssociationContainer goAssociations, PopulationSet populationSet,
			StudySet studySet)
	{
		if (studySet.getGeneCount() == 0)
			return new EnrichedGOTermsResult(graph,goAssociations,studySet,populationSet.getGeneCount());

		/* For a valued calculation, ony the study set is interesting as it contains all genes */
		boolean valuedCalculation = studySet.hasOnlyValuedItemAttributes();

		if (valuedCalculation)
		{
			System.out.println("We have values!");
		} else
		{
			System.out.println("We don't have values!");
		}


		Bayes2GOEnrichedGOTermsResult result = new Bayes2GOEnrichedGOTermsResult(graph,goAssociations,studySet,populationSet.getGeneCount());
		result.setCalculationName(this.getName());

		GOTermEnumerator populationEnumerator = populationSet.enumerateGOTerms(graph, goAssociations);
		GOTermEnumerator studyEnumerator = studySet.enumerateGOTerms(graph, goAssociations);

		if (valuedCalculation)
		{
			if (!populationEnumerator.getGenes().containsAll(studyEnumerator.getGenes()) ||
				!studyEnumerator.getGenes().containsAll(populationEnumerator.getGenes()))
			{
				throw new IllegalArgumentException("For a valued calculation, study set and population set must be identical");
			}
		}

		System.out.println("Starting calculation: expectedNumberOfTerms=" + expectedNumberOfTerms + " alpha=" + alpha + " beta=" + beta + "  numberOfPop=" + populationEnumerator.getGenes().size() + " numberOfStudy=" + studyEnumerator.getGenes().size());

		long start = System.currentTimeMillis();
		calculateByMCMC(graph, result, populationEnumerator, studyEnumerator, populationSet, studySet);//, llr);
		long end = System.currentTimeMillis();
		System.out.println((end - start) + "ms");
		return result;
	}

	public EnrichedGOTermsResult calculateStudySet(Ontology graph,
			AssociationContainer goAssociations, PopulationSet populationSet,
			StudySet studySet, AbstractTestCorrection testCorrection)
	{
		return calculateStudySet(graph, goAssociations, populationSet, studySet);
	}

	public void setUsePrior(boolean usePrior)
	{
		this.usePrior = usePrior;
	}

	private void calculateByMCMC(Ontology graph,
			Bayes2GOEnrichedGOTermsResult result,
			GOTermEnumerator populationEnumerator,
			GOTermEnumerator studyEnumerator,
			PopulationSet populationSet,
			StudySet studySet)
	{
		List<TermID> allTerms;

		if (takePopulationAsReference) allTerms = populationEnumerator.getAllAnnotatedTermsAsList();
		else allTerms = studyEnumerator.getAllAnnotatedTermsAsList();

		Random rnd;
		if (seed != 0)
		{
			rnd = new Random(seed);
			logger.info("Use a random seed of: " + seed);
		} else
		{
			long newSeed = new Random().nextLong();
			logger.info("Use a random seed of: " + newSeed);
			rnd = new Random(newSeed);
		}

		boolean doAlphaEm = false;
		boolean doBetaEm = false;
		boolean doPEm = false;

		int maxIter;

		double alpha;
		double beta;
		double expectedNumberOfTerms;

		switch (this.alpha.getType())
		{
			case	EM: alpha = 0.4; doAlphaEm = true; break;
			case	MCMC: alpha = Double.NaN; break;
			default: alpha = this.alpha.getValue(); break;
		}

		switch (this.beta.getType())
		{
			case	EM: beta = 0.4; doBetaEm = true; break;
			case	MCMC: beta = Double.NaN; break;
			default: beta = this.beta.getValue(); break;
		}


		switch (this.expectedNumberOfTerms.getType())
		{
			case	EM: expectedNumberOfTerms = 1; doPEm = true; break;
			case	MCMC: expectedNumberOfTerms = Double.NaN; break;
			default: expectedNumberOfTerms = this.expectedNumberOfTerms.getValue(); break;
		}

		boolean doEm = doAlphaEm || doBetaEm || doPEm;

		if (doEm) maxIter = 12;
		else maxIter = 1;

		logger.info(allTerms.size() + " terms and " + populationEnumerator.getGenes().size() + " genes in consideration.");

		BufferedWriter statsFile = null;
		try {
			if (WRITE_STATS_FILE)
			{
				statsFile = new BufferedWriter(new FileWriter(new File("stats.txt")));
				statsFile.append("iter\tstep\tacceptProb\taccepted\tscore\n");
			}
		} catch (IOException e) {
		}

		for (int i=0;i<maxIter;i++)
		{
			FixedAlphaBetaScore bayesScore = new FixedAlphaBetaScore(rnd, allTerms, populationEnumerator,  studyEnumerator.getGenes());
			bayesScore.setIntegrateParams(integrateParams);

			if (doEm)
			{
				System.out.println("EM-Iter("+i+")" + alpha + "  " + beta + "  " + expectedNumberOfTerms);
			} else
			{
				System.out.println("MCMC only: " + alpha + "  " + beta + "  " + expectedNumberOfTerms);

			}

			bayesScore.setAlpha(alpha);
			if (this.alpha.hasMax())
				bayesScore.setMaxAlpha(this.alpha.getMax());
			bayesScore.setBeta(beta);
			if (this.beta.hasMax())
				bayesScore.setMaxBeta(this.beta.getMax());
			bayesScore.setExpectedNumberOfTerms(expectedNumberOfTerms);
			bayesScore.setUsePrior(usePrior);

			result.setScore(bayesScore);

			int maxSteps = mcmcSteps;
			int burnin = 20000;
			int numAccepts = 0;
			int numRejects = 0;

			if (calculationProgress != null)
				calculationProgress.init(maxSteps);

			double score = bayesScore.getScore();

			logger.info("Score of empty set: " + score);

			/* Provide a starting point */
			if (randomStart)
			{
				int numberOfTerms = bayesScore.EXPECTED_NUMBER_OF_TERMS[rnd.nextInt(bayesScore.EXPECTED_NUMBER_OF_TERMS.length)];
				double pForStart = ((double)numberOfTerms) / allTerms.size();

				for (int j=0;j<allTerms.size();j++)
					if (rnd.nextDouble() < pForStart) bayesScore.switchState(j);

				logger.info("Starting with " + bayesScore.getActiveTerms().size() + " terms (p=" + pForStart + ")");

				score = bayesScore.getScore();
			}
			logger.info("Score of initial set: " + score);

			double maxScore = score;
			ArrayList<TermID> maxScoredTerms = bayesScore.getActiveTerms();
			double maxScoredAlpha = Double.NaN;
			double maxScoredBeta = Double.NaN;
			double maxScoredP = Double.NaN;
			int maxWhenSeen = -1;

			long start = System.currentTimeMillis();

			for (int t=0;t<maxSteps;t++)
			{
				/* Remember maximum score and terms */
				if (score > maxScore)
				{
					maxScore = score;
					maxScoredTerms = bayesScore.getActiveTerms();
					maxScoredAlpha = bayesScore.getAlpha();
					maxScoredBeta = bayesScore.getBeta();
					maxScoredP = bayesScore.getP();
					maxWhenSeen = t;
				}

				long now = System.currentTimeMillis();
				if (now - start > updateReportTime)
				{
					logger.info((t*100/maxSteps) + "% (score=" + score +" maxScore=" + maxScore + " #terms="+bayesScore.getActiveTerms().size()+
										" accept/reject=" + String.format("%g",(double)numAccepts / (double)numRejects) +
										" accept/steps=" + String.format("%g",(double)numAccepts / (double)t) +
										" exp=" + expectedNumberOfTerms + " usePrior=" + usePrior + ")");
					start = now;

					if (calculationProgress != null)
						calculationProgress.update(t);
				}

				long oldPossibilities = bayesScore.getNeighborhoodSize();
				long r = rnd.nextLong();
				bayesScore.proposeNewState(r);
				double newScore = bayesScore.getScore();
				long newPossibilities = bayesScore.getNeighborhoodSize();

				double acceptProb = Math.exp(newScore - score)*(double)oldPossibilities/(double)newPossibilities; /* last quotient is the hasting ratio */

				boolean DEBUG = false;

				if (DEBUG) System.out.print(bayesScore.getActiveTerms().size() + "  score=" + score + " newScore="+newScore + " maxScore=" + maxScore + " a=" + acceptProb);

				double u = rnd.nextDouble();
				if (u >= acceptProb)
				{
					bayesScore.undoProposal();
					numRejects++;
				} else
				{
					score = newScore;
					numAccepts++;
				}
				if (DEBUG) System.out.println();

				if (t>burnin)
					bayesScore.record();


				if (statsFile != null)
				{
					try {
						statsFile.append(i + "\t" + t + "\t" + acceptProb + "\t" + numAccepts + "\t" + score + "\n");
					} catch (IOException e) {
					}
				}
			}

			if (doAlphaEm)
			{
				double newAlpha = (double)bayesScore.getAvgN10()/(bayesScore.getAvgN00() + bayesScore.getAvgN10());
				if (newAlpha < 0.0000001) newAlpha = 0.0000001;
				if (newAlpha > 0.9999999) newAlpha = 0.9999999;
				System.out.println("alpha=" + alpha + "  newAlpha=" + newAlpha);
				alpha = newAlpha;
			}

			if (doBetaEm)
			{
				double newBeta = (double)bayesScore.getAvgN01()/(bayesScore.getAvgN01() + bayesScore.getAvgN11());
				if (newBeta < 0.0000001) newBeta = 0.0000001;
				if (newBeta > 0.9999999) newBeta = 0.9999999;
				System.out.println("beta=" + beta + "  newBeta=" + newBeta);
				beta = newBeta;
			}

			if (doPEm)
			{
				double newExpectedNumberOfTerms = (double)bayesScore.getAvgT();
				if (newExpectedNumberOfTerms < 0.0000001) newExpectedNumberOfTerms = 0.0000001;
				System.out.println("expectedNumberOfTerms=" + expectedNumberOfTerms + "  newExpectedNumberOfTerms=" + newExpectedNumberOfTerms);
				expectedNumberOfTerms = newExpectedNumberOfTerms;
			}

			if (i==maxIter - 1)
			{
				for (TermID t : allTerms)
				{
					Bayes2GOGOTermProperties prop = new Bayes2GOGOTermProperties();
					prop.goTerm = graph.getTerm(t);
					prop.annotatedStudyGenes = studyEnumerator.getAnnotatedGenes(t).totalAnnotatedCount();
					prop.annotatedPopulationGenes = populationEnumerator.getAnnotatedGenes(t).totalAnnotatedCount();
					prop.marg = ((double)bayesScore.termActivationCounts[bayesScore.term2TermsIdx.get(t)] / bayesScore.numRecords);

					/* At the moment, we need these fields for technical reasons */
					prop.p = 1 - ((double)bayesScore.termActivationCounts[bayesScore.term2TermsIdx.get(t)] / bayesScore.numRecords);
					prop.p_adjusted = prop.p;
					prop.p_min = 0.001;

					result.addGOTermProperties(prop);
				}
			}

			System.out.println("numAccepts=" + numAccepts + "  numRejects = " + numRejects);

			/* Print out the term combination which scored max */
			System.out.println("Term combination that reaches score of " + maxScore + " when alpha=" + maxScoredAlpha + ", beta=" + maxScoredBeta + ", p=" + maxScoredP + " at step " + maxWhenSeen);
			for (TermID tid : maxScoredTerms)
			{
				System.out.println(tid.toString() + "/" + graph.getTerm(tid).getName());
			}

			if (Double.isNaN(alpha))
			{
				for (int j=0;j<bayesScore.totalAlpha.length;j++)
					System.out.println("alpha(" + bayesScore.ALPHA[j] + ")=" + (double)bayesScore.totalAlpha[j] / bayesScore.numRecords);
			}

			if (Double.isNaN(beta))
			{
				for (int j=0;j<bayesScore.totalBeta.length;j++)
					System.out.println("beta(" + bayesScore.BETA[j] + ")=" + (double)bayesScore.totalBeta[j] / bayesScore.numRecords);
			}

			if (Double.isNaN(expectedNumberOfTerms))
			{
				for (int j=0;j<bayesScore.totalExp.length;j++)
					System.out.println("exp(" + bayesScore.EXPECTED_NUMBER_OF_TERMS[j] + ")=" + (double)bayesScore.totalExp[j] / bayesScore.numRecords);

			}
		}

		if (statsFile != null)
		{
			try {
				statsFile.flush();
			} catch (IOException e) {
			}
		}
	}

	public String getDescription()
	{
		// TODO Auto-generated method stub
		return null;
	}

	public String getName()
	{
		return "MGSA";
	}

	public void setProgress(ICalculationProgress calculationProgress)
	{
		this.calculationProgress = calculationProgress;
	}

	public boolean supportsTestCorrection() {
		return false;
	}

}
