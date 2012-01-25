package ontologizer.calculation.b2g;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import ontologizer.FileCache;
import ontologizer.OntologizerThreadGroups;
import ontologizer.association.Association;
import ontologizer.association.AssociationContainer;
import ontologizer.calculation.AbstractGOTermProperties;
import ontologizer.calculation.EnrichedGOTermsResult;
import ontologizer.calculation.ICalculation;
import ontologizer.calculation.ICalculationProgress;
import ontologizer.calculation.TermForTermCalculation;
import ontologizer.dotwriter.AbstractDotAttributesProvider;
import ontologizer.dotwriter.GODOTWriter;
import ontologizer.enumeration.GOTermEnumerator;
import ontologizer.go.Ontology;
import ontologizer.go.ParentTermID;
import ontologizer.go.Term;
import ontologizer.go.TermContainer;
import ontologizer.go.TermID;
import ontologizer.go.TermRelation;
import ontologizer.parser.ItemAttribute;
import ontologizer.parser.ValuedItemAttribute;
import ontologizer.set.PopulationSet;
import ontologizer.set.StudySet;
import ontologizer.statistics.AbstractTestCorrection;
import ontologizer.statistics.Bonferroni;
import ontologizer.statistics.None;
import ontologizer.types.ByteString;
import ontologizer.worksets.WorkSet;
import ontologizer.worksets.WorkSetLoadThread;

class B2GTestParameter
{
	static double ALPHA = 0.25;
	static double BETA = 0.60;
	static double BETA2 = 0.10;
	static int MCMC_STEPS = 1020000;
}

/**
 * This class implements an model-based analysis.
 * The description of this method can be found at XXXX.
 * 
 * @author Sebastian Bauer
 */
public class Bayes2GOCalculation implements ICalculation
{
	private static Logger logger = Logger.getLogger(Bayes2GOCalculation.class.getCanonicalName());

	private boolean WRITE_STATS_FILE = false;

	private long seed = 0;
	
	private boolean usePrior = true;

	private DoubleParam alpha = new DoubleParam(B2GParam.Type.MCMC);
	private DoubleParam beta = new DoubleParam(B2GParam.Type.MCMC);
	private IntegerParam expectedNumberOfTerms = new IntegerParam(B2GParam.Type.MCMC);
	
	private boolean takePopulationAsReference = false;
	private ICalculationProgress calculationProgress;
	
	private boolean randomStart = false;

	private int mcmcSteps = B2GTestParameter.MCMC_STEPS;
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
	
	/**
	 * Returns whether the given study set has only valued item attributes.
	 * 
	 * @param studySet
	 * @return
	 */
	private static boolean hasOnlyValuedItemAttributes(StudySet studySet)
	{
		boolean hasOnlyValuedItemAttributes = true;
		
		for (ByteString gene : studySet)
		{
			ItemAttribute item = studySet.getItemAttribute(gene);
			if (!(item instanceof ValuedItemAttribute))
			{
				hasOnlyValuedItemAttributes = false;
				break;
			}
		}
		
		return hasOnlyValuedItemAttributes;
	}
	
	public EnrichedGOTermsResult calculateStudySet(Ontology graph,
			AssociationContainer goAssociations, PopulationSet populationSet,
			StudySet studySet)
	{
		boolean valuedCalculation;
		
		if (studySet.getGeneCount() == 0)
			return new EnrichedGOTermsResult(graph,goAssociations,studySet,populationSet.getGeneCount());
		
		valuedCalculation = hasOnlyValuedItemAttributes(populationSet);
		valuedCalculation = valuedCalculation & hasOnlyValuedItemAttributes(studySet);

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

	public static Ontology graph;
	public static AssociationContainer assoc;
	
	private static void createInternalOntology(long seed)
	{
		/* Go Graph */
		HashSet<Term> terms = new HashSet<Term>();
		Term c1 = new Term("GO:0000001", "C1");
		Term c2 = new Term("GO:0000002", "C2", new ParentTermID(c1.getID(),TermRelation.IS_A));
		Term c3 = new Term("GO:0000003", "C3", new ParentTermID(c1.getID(),TermRelation.IS_A));
		Term c4 = new Term("GO:0000004", "C4", new ParentTermID(c2.getID(),TermRelation.IS_A));
		Term c5 = new Term("GO:0000005", "C5", new ParentTermID(c2.getID(),TermRelation.IS_A));
		Term c6 = new Term("GO:0000006", "C6", new ParentTermID(c3.getID(),TermRelation.IS_A),new ParentTermID(c2.getID(),TermRelation.IS_A));
		Term c7 = new Term("GO:0000007", "C7", new ParentTermID(c5.getID(),TermRelation.IS_A),new ParentTermID(c6.getID(),TermRelation.IS_A));
		Term c8 = new Term("GO:0000008", "C8", new ParentTermID(c7.getID(),TermRelation.IS_A));
		Term c9 = new Term("GO:0000009", "C9", new ParentTermID(c7.getID(),TermRelation.IS_A));
		Term c10 = new Term("GO:0000010", "C10", new ParentTermID(c9.getID(),TermRelation.IS_A));
		Term c11 = new Term("GO:0000011", "C11", new ParentTermID(c9.getID(),TermRelation.IS_A));

		terms.add(c1);
		terms.add(c2);
		terms.add(c3);
		terms.add(c4);
		terms.add(c5);
		terms.add(c6);
		terms.add(c7);
		terms.add(c8);
		terms.add(c9);
		terms.add(c10);
		terms.add(c11);
		TermContainer termContainer = new TermContainer(terms,"","");
		graph = new Ontology(termContainer);

		HashSet<TermID> tids = new HashSet<TermID>();
		for (Term term : terms)
			tids.add(term.getID());

		/* Associations */
		assoc = new AssociationContainer();
		Random r = new Random(seed);

		/* Randomly assign the items (note that redundant associations are filtered out later) */
		for (int i=1;i<=500;i++)
		{
			String itemName = "item" + i;
			int numTerms = r.nextInt(2) + 1;
			
			for (int j=0;j<numTerms;j++)
			{
				int tid = r.nextInt(terms.size())+1;
				assoc.addAssociation(new Association(new ByteString(itemName),tid));
			}
		}
	}

	/**
	 * A test procedure.
	 * 
	 * @param args
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws InterruptedException
	{
		final HashMap<TermID,Double> wantedActiveTerms = new HashMap<TermID,Double>(); /* Terms that are active */

		/* ***************************************************************** */
		loadOntology();
//		wantedActiveTerms.put(new TermID("GO:0007049"), B2GTestParameter.BETA2); /* cell cycle */
//		wantedActiveTerms.put(new TermID("GO:0043473"), B2GTestParameter.BETA2); /* pigmentation */
//		wantedActiveTerms.put(new TermID("GO:0001505"), B2GTestParameter.BETA); /* regulation of neuro transmitter levels */
//////		wantedActiveTerms.put(new TermID("GO:0008078"), B2GTestParameter.BETA); /* mesodermal cell migration */
//////		wantedActiveTerms.put(new TermID("GO:0051208"), B2GTestParameter.BETA); /* sequestering of calcium ion */
//		wantedActiveTerms.put(new TermID("GO:0030011"), B2GTestParameter.BETA); /* maintenace of cell polarity */
//////		wantedActiveTerms.put(new TermID("GO:0035237"), B2GTestParameter.BETA); /* corazonin receptor activity */

		wantedActiveTerms.put(new TermID("GO:0035282"),B2GTestParameter.BETA); /* segmentation */
		wantedActiveTerms.put(new TermID("GO:0007049"), B2GTestParameter.BETA); /* cell cycle */
//		wantedActiveTerms.put(new TermID("GO:0009880"),B2GTestParameter.BETA);
		
		
		
//		createInternalOntology(1);
//		wantedActiveTerms.add(new TermID("GO:0000010"));
//		wantedActiveTerms.add(new TermID("GO:0000004"));

		/* ***************************************************************** */

//		Random rnd = new Random(10);
//		Random rnd = new Random(11); /* Produces a set */
		Random rnd = new Random(11);

		/* Simulation */

		PopulationSet allGenes = new PopulationSet("all");
		for (ByteString gene : assoc.getAllAnnotatedGenes())
			allGenes.addGene(gene, "");

		System.out.println("Total number of genes " + allGenes);
		
		HashMap<TermID,StudySet> wantedActiveTerm2StudySet = new HashMap<TermID,StudySet>();

//		graph.setRelevantSubontology("biological_process");
		final GOTermEnumerator allEnumerator = allGenes.enumerateGOTerms(graph, assoc);
		
		System.out.println("Considering a total of " + allEnumerator.getAllAnnotatedTermsAsList().size() + " terms");

		for (TermID t : wantedActiveTerms.keySet())
		{
			StudySet termStudySet = new StudySet("study");
			for (ByteString g : allEnumerator.getAnnotatedGenes(t).totalAnnotated)
				termStudySet.addGene(g, "");
			termStudySet.filterOutDuplicateGenes(assoc);
			wantedActiveTerm2StudySet.put(t, termStudySet);
		}
		
		StudySet newStudyGenes = new StudySet("study");
		for (TermID t : wantedActiveTerms.keySet())
		{
			System.out.println(t.toString() + " genes=" + wantedActiveTerm2StudySet.get(t).getGeneCount() + " beta=" + wantedActiveTerms.get(t));

			newStudyGenes.addGenes(wantedActiveTerm2StudySet.get(t));
		}

		newStudyGenes.filterOutDuplicateGenes(assoc);

		System.out.println("Number of genes in study set " + newStudyGenes.getGeneCount());
		
		double alphaStudySet = B2GTestParameter.ALPHA;
		double betaStudySet = B2GTestParameter.BETA;

		int tp = newStudyGenes.getGeneCount();
		int tn = allGenes.getGeneCount();

		/* Obfuscate the study set, i.e., create the observed state */
		
		/* false -> true (alpha, false positive) */
		HashSet<ByteString>  fp = new HashSet<ByteString>();
		for (ByteString gene : allGenes)
		{
			if (newStudyGenes.contains(gene)) continue;
			if (rnd.nextDouble() < alphaStudySet) fp.add(gene);
		}

		/* true -> false (beta, false negative) */
		HashSet<ByteString>  fn = new HashSet<ByteString>();
		if (true)
		{
			for (ByteString gene : newStudyGenes)
			{
				if (rnd.nextDouble() < betaStudySet) fn.add(gene);
			}
		} else
		{
			/* If this path is enabled, we support more than one 
			 * term.
			 */
			for (TermID t : wantedActiveTerms.keySet())
			{
				double beta = wantedActiveTerms.get(t);
				StudySet termStudySet = wantedActiveTerm2StudySet.get(t);
				for (ByteString g : termStudySet)
				{
					if (rnd.nextDouble() < beta) fn.add(g);
				}
			}
		}
		newStudyGenes.addGenes(fp);
		newStudyGenes.removeGenes(fn);
		
		
		
		double realAlpha = ((double)fp.size())/tn;
		double realBeta = ((double)fn.size())/tp;
		
		System.out.println("Study set has " + fp.size() + " false positives (alpha=" + realAlpha +")");
		System.out.println("Study set has " + fn.size() + " false negatives (beta=" + realBeta +")");
		System.out.println("Study set has a total of " +  newStudyGenes.getGeneCount() + " genes");

		/**** Write out the graph ****/
		//{
			HashSet<TermID> allTermIDs = new HashSet<TermID>();
			for (Term t : graph)
				allTermIDs.add(t.getID());

			final GOTermEnumerator studySetEnumerator = newStudyGenes.enumerateGOTerms(graph, assoc);

			GODOTWriter.writeDOT(graph, new File("toy-all.dot"), null, allTermIDs, new AbstractDotAttributesProvider()
			{
				public String getDotNodeAttributes(TermID id)
				{
					StringBuilder str = new StringBuilder(200);
					str.append("label=\"");
					str.append(graph.getTerm(id).getName());
					str.append("\\n");
					str.append(studySetEnumerator.getAnnotatedGenes(id).totalAnnotatedCount() + "/" + allEnumerator.getAnnotatedGenes(id).totalAnnotatedCount());
					str.append("\"");
					if (wantedActiveTerms.containsKey(id))
					{
						str.append("style=\"filled\" color=\"gray\"");
					}
					return str.toString();
				}
			});
			
			GODOTWriter.writeDOT(graph, new File("toy-induced.dot"), null, wantedActiveTerms.keySet(), new AbstractDotAttributesProvider()
			{
				public String getDotNodeAttributes(TermID id)
				{
					StringBuilder str = new StringBuilder(200);
					str.append("label=\"");
					str.append(graph.getTerm(id).getName());
					str.append("\\n");
					str.append(studySetEnumerator.getAnnotatedGenes(id).totalAnnotatedCount() + "/" + allEnumerator.getAnnotatedGenes(id).totalAnnotatedCount());
					str.append("\"");
					if (wantedActiveTerms.containsKey(id))
					{
						str.append("style=\"filled\" color=\"gray\"");
					}
					return str.toString();
				}
			});

		//}

//		double p = (double)wantedActiveTerms.size() / allEnumerator.getTotalNumberOfAnnotatedTerms();
//
//		ProbabilisticCalculation calc = new ProbabilisticCalculation();
//		calc.setDefaultP(1- realBeta);
//		calc.setDefaultQ(realAlpha);
		
//		TopologyWeightedCalculation calc = new TopologyWeightedCalculation();
//		TermForTermCalculation calc = new TermForTermCalculation();
//		ParentChildCalculation calc = new ParentChildCalculation();
		Bayes2GOCalculation calc = new Bayes2GOCalculation();
		calc.setSeed(2); /* Finds a optimum */
//		calc.setSeed(3); /* with basement membrane, score 6826.695 */
//		calc.setSeed(4); /* Optimum, score 6826.039 */

////		calc.setAlpha(B2GParam.Type.MCMC);
////		calc.setBeta(B2GParam.Type.MCMC);
////		calc.setExpectedNumber(B2GParam.Type.MCMC);
//		calc.setAlpha(realAlpha);
//		calc.setBeta(realBeta);
//		calc.setExpectedNumber(wantedActiveTerms.size());

		calc.setMcmcSteps(520000);
		calc.setAlpha(B2GParam.Type.MCMC);
		calc.setBeta(B2GParam.Type.MCMC);
		calc.setExpectedNumber(B2GParam.Type.MCMC);
		
//		calc.setAlpha(B2GParam.Type.EM);
//		calc.setBeta(B2GParam.Type.EM);
//		calc.setExpectedNumber(B2GParam.Type.EM);

////	calc.setUsePrior(false);
//		calc.setAlpha(alphaStudySet);
//		calc.setBeta(betaStudySet);
		
		
		evaluate(wantedActiveTerms, allGenes, newStudyGenes, allEnumerator, studySetEnumerator, calc);
		
//		/* Draw the basic example figure */
//		final int MAX_ITER = 20;
//		
//		long rank_sum = 0;
//		double marg_sum = 0;
//
//		double [] marg = new double[MAX_ITER];
//		int [] rank = new int[MAX_ITER];
//		long [] seed = new long[MAX_ITER];
//	
//		final HashMap<TermID,Double> t2marg = new HashMap<TermID,Double>();
////		int [][] allMargs = null;
//		
//		Random seedRandom = new Random();
//		
//		for (int i=0;i<MAX_ITER;i++)
//		{
//			seed[i] = seedRandom.nextLong();
//
//			Bayes2GOCalculation calc2 = new Bayes2GOCalculation();
//			calc2.setSeed(seed[i]);
//			calc2.setAlpha(realAlpha);
//			calc2.setBeta(realBeta);
//			calc2.setExpectedNumber(wantedActiveTerms.size());
//
//			EnrichedGOTermsResult result = calc2.calculateStudySet(graph, assoc, allGenes, newStudyGenes, new None());
//			
//			ArrayList<AbstractGOTermProperties> resultList = new ArrayList<AbstractGOTermProperties>();
//
//			for (AbstractGOTermProperties prop : result)
//			{
//				double tMarg = 1 - prop.p;
//				Double cMarg = t2marg.get(prop.goTerm.getID());
//				if (cMarg != null)
//					tMarg += cMarg;
//				t2marg.put(prop.goTerm.getID(), tMarg);
//				
//				resultList.add(prop);
//			}
//			Collections.sort(resultList);
//
//			/* Determine the rank of a special term */
//			int r = 1;
//			for (AbstractGOTermProperties prop : resultList)
//			{
//				if (prop.goTerm.getID().id == 30011)
//				{
//					marg[i] = (1 - prop.p);
//					rank[i] = r;
//					break;
//				}
//				r++;
//			}
//		}
//		
//		System.out.println("rank\tmarg\tseed");
//		for (int i=0;i<MAX_ITER;i++)
//			System.out.println(rank[i] + "\t" + marg[i] + "\t" + seed[i]);
//
//		GODOTWriter.writeDOT(graph, new File("toy-result-avg.dot"), null, wantedActiveTerms.keySet(), new IDotNodeAttributesProvider()
//		{
//			public String getDotNodeAttributes(TermID id)
//			{
//				StringBuilder str = new StringBuilder(200);
//				str.append("label=\"");
//				
//				str.append(Util.wrapLine(graph.getGOTerm(id).getName(),"\\n",20));
//				
//				str.append("\\n");
//				str.append(studySetEnumerator.getAnnotatedGenes(id).totalAnnotatedCount() + "/" + allEnumerator.getAnnotatedGenes(id).totalAnnotatedCount());
//				str.append("\\n");
//				if (t2marg.get(id) != null)
//					str.append(String.format("P(T=1)=%g", ((Double)t2marg.get(id)/ MAX_ITER)));
//
//				str.append("\"");
//				if (wantedActiveTerms.containsKey(id))
//				{
//					str.append("style=\"filled\" color=\"gray\"");
//				}
//
////				if (result.getGOTermProperties(id) != null && result.getGOTermProperties(id).p_adjusted < 0.999)
////				{
////					str.append(" penwidth=\"2\"");
////				}
//				return str.toString();
//			}
//		});

	}

	private static void evaluate(final HashMap<TermID,Double> wantedActiveTerms,
			PopulationSet allGenes, StudySet newStudyGenes,
			final GOTermEnumerator allEnumerator,
			final GOTermEnumerator studySetEnumerator,
			ICalculation calc)
	{
		final EnrichedGOTermsResult result = calc.calculateStudySet(graph, assoc, allGenes, newStudyGenes, new None());

		TermForTermCalculation tft = new TermForTermCalculation();
		EnrichedGOTermsResult r2 = tft.calculateStudySet(graph, assoc, allGenes, newStudyGenes, new Bonferroni());
		HashSet<TermID> s = new HashSet<TermID>();
		for (AbstractGOTermProperties p2 : r2)
			s.add(p2.goTerm.getID());
		int cnt = 0;
		for (AbstractGOTermProperties prop : result)
		{
			if (!s.contains(prop.goTerm.getID()))
			{
//				System.out.println(prop.annotatedPopulationGenes + "  " + prop.annotatedStudyGenes);
				cnt++;
			}
		}
		System.out.println("There are " + cnt + " terms to which none of the genes of the study set are annotated.");
		boolean pIsReverseMarginal = false;
	
		System.out.println("Method is " + calc.getName());

		System.out.println("We have a statement over a total of " + result.getSize() + " terms.");

		/*** Calculate the score of the given term set ***/
		
		if (calc instanceof Bayes2GOCalculation)
		{
			if (result instanceof Bayes2GOEnrichedGOTermsResult)
			{
				Bayes2GOEnrichedGOTermsResult b2gResult = (Bayes2GOEnrichedGOTermsResult)result;
				double wantedScore = b2gResult.getScore().score(wantedActiveTerms.keySet());
//				if (!(((Bayes2GOCalculation)calc).noPrior)) wantedScore += wantedActiveTerms.size() * Math.log(p/(1.0-p));
				System.out.println("Score of the given set is " + wantedScore);

				HashSet<TermID> terms = new HashSet<TermID>(wantedActiveTerms.keySet());
				terms.remove(new TermID("GO:0030011"));
				wantedScore = b2gResult.getScore().score(terms);
				System.out.println("Score of reduced set is " + wantedScore);
			}
			pIsReverseMarginal = true;
		}
		
		//scoreDistribution(calc,allEnumerator,allGenes,newStudyGenes);
		
		ArrayList<AbstractGOTermProperties> resultList = new ArrayList<AbstractGOTermProperties>();
		for (AbstractGOTermProperties prop : result)
			resultList.add(prop);
		Collections.sort(resultList);

//		ArrayList<AbstractGOTermProperties> interestingList = new ArrayList<AbstractGOTermProperties>();
//		System.out.println("The overrepresented terms:");
//		for (TermID w : wantedActiveTerms)
//		{
//			AbstractGOTermProperties prop = result.getGOTermProperties(w);
//			if (prop!=null)
//				System.out.println(" " + prop.goTerm.getIDAsString() + "/" + prop.goTerm.getName() + "   " + (/*1.0f - */prop.p_adjusted) + ")");
//			else
//				System.out.println(w.toString() + " not found");
//		}

		{
//			System.out.println("The terms found by the algorithm:");
			HashSet<TermID> terms = new HashSet<TermID>();

			System.out.println("The overrepresented terms:");
			
			int rank = 1;
			for (AbstractGOTermProperties prop : resultList)
			{
				if (wantedActiveTerms.containsKey(prop.goTerm.getID()))
					System.out.println(" " + prop.goTerm.getIDAsString() + "/" + prop.goTerm.getName() + "   " + (/*1.0f - */prop.p_adjusted) + " rank=" + rank + " beta=" + wantedActiveTerms.get(prop.goTerm.getID()));
				rank++;
			}

			System.out.println("The terms found by the algorithm:");

			rank = 1;
			for (AbstractGOTermProperties prop : resultList)
			{
				if (prop.p_adjusted < 0.9)
				{
					terms.add(prop.goTerm.getID());
//					System.out.println(" " + prop.goTerm.getIDAsString() + "/" + prop.goTerm.getName() + "   " + (/*1.0f - */prop.p_adjusted)  + " rank=" + rank);
				}
				rank++;
			}

			
			terms.addAll(wantedActiveTerms.keySet());

			GODOTWriter.writeDOT(graph, new File("toy-result.dot"), null, terms, new AbstractDotAttributesProvider()
			{
				public String getDotNodeAttributes(TermID id)
				{
					StringBuilder str = new StringBuilder(200);
					str.append("label=\"");
					str.append(graph.getTerm(id).getName());
					str.append("\\n");
					if (result.getGOTermProperties(id) != null)
						str.append(String.format("p(t)=%g\\n", /*1-*/result.getGOTermProperties(id).p_adjusted));
					str.append(studySetEnumerator.getAnnotatedGenes(id).totalAnnotatedCount() + "/" + allEnumerator.getAnnotatedGenes(id).totalAnnotatedCount());
					str.append("\"");
					if (wantedActiveTerms.containsKey(id))
					{
						str.append("style=\"filled\" color=\"gray\"");
					}
					if (result.getGOTermProperties(id) != null && result.getGOTermProperties(id).p_adjusted < 0.999)
					{
						str.append(" penwidth=\"2\"");
					}
					return str.toString();
				}
			});
		}
	}
	
//	static private void scoreDistribution(Bayes2GOCalculation calc, GOTermEnumerator allEnumerator, PopulationSet popSet, StudySet studySet)
//	{
//		/** Calculates the whole score distribution */
//		class MyResult implements Comparable<MyResult>
//		{
//			public ArrayList<TermID> terms;
//			public double score;
//
//			public int compareTo(MyResult o)
//			{
//				if (o.score > score) return 1;
//				if (o.score < score) return -1;
//				return 0;
//			}
//		}
//		ArrayList<MyResult> rl = new ArrayList<MyResult>();
//		ArrayList<Term> tal = new ArrayList<Term>();
//		for (Term t : graph.getGoTermContainer())
//			tal.add(t);
//		HashMap<ByteString, Double> llr = calcLLR(popSet, studySet);
//		SubsetGenerator sg = new SubsetGenerator(tal.size(),tal.size());
//		Subset s;
//		while ((s = sg.next()) != null)
//		{
//			ArrayList<TermID> activeTerms = new ArrayList<TermID>(s.r);
//			
//			for (int i=0;i<s.r;i++)
//				activeTerms.add(tal.get(s.j[i]).getID());
//			
//			double score = calc.score(llr, activeTerms, allEnumerator, p);
//			MyResult res = new MyResult();
//			res.score = score;
//			res.terms = activeTerms;
//			rl.add(res);
//		}
//		
//		Collections.sort(rl);
//		for (MyResult res : rl)
//		{
//			System.out.print(res.score + " ");
//			for (TermID at : res.terms)
//			{
//				System.out.print(" " + graph.getGOTerm(at).getName());
//			}
//			System.out.println();
//		}
//
//
//	}

	private static void loadOntology() throws InterruptedException
	{
		File workspace = new File(ontologizer.util.Util.getAppDataDirectory("ontologizer"),"workspace");
		if (!workspace.exists())
			workspace.mkdirs();
		FileCache.setCacheDirectory(new File(workspace,".cache").getAbsolutePath());
		final WorkSet ws = new WorkSet("Test");
		ws.setOboPath("http://www.geneontology.org/ontology/gene_ontology_edit.obo");
		ws.setAssociationPath("http://cvsweb.geneontology.org/cgi-bin/cvsweb.cgi/go/gene-associations/gene_association.fb.gz?rev=HEAD");

		final Object notify = new Object();
		
		synchronized (notify)
		{
			WorkSetLoadThread.obtainDatafiles(ws, 
				new Runnable(){
					public void run()
					{
						graph = WorkSetLoadThread.getGraph(ws.getOboPath());
						assoc = WorkSetLoadThread.getAssociations(ws.getAssociationPath());
						synchronized (notify)
						{
							notify.notifyAll();
						}
					}
			});
			notify.wait();
		}
		OntologizerThreadGroups.workerThreadGroup.interrupt();
	}

	public void setProgress(ICalculationProgress calculationProgress)
	{
		this.calculationProgress = calculationProgress;
	}
	
	public boolean supportsTestCorrection() {
		return false;
	}

}
