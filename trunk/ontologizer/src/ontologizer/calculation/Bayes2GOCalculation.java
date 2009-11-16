package ontologizer.calculation;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;

import ontologizer.ByteString;
import ontologizer.FileCache;
import ontologizer.GODOTWriter;
import ontologizer.GOTermEnumerator;
import ontologizer.IDotNodeAttributesProvider;
import ontologizer.OntologizerThreadGroups;
import ontologizer.PopulationSet;
import ontologizer.StudySet;
import ontologizer.association.Association;
import ontologizer.association.AssociationContainer;
import ontologizer.go.GOGraph;
import ontologizer.go.ParentTermID;
import ontologizer.go.Term;
import ontologizer.go.TermContainer;
import ontologizer.go.TermID;
import ontologizer.go.TermRelation;
import ontologizer.statistics.AbstractTestCorrection;
import ontologizer.statistics.Bonferroni;
import ontologizer.worksets.WorkSet;
import ontologizer.worksets.WorkSetLoadThread;

/**
 * The base class of bayes2go Score.
 * 
 * For efficiency reasons terms and genes are represented by own ids.
 * 
 * @author Sebastian Bauer
 */
abstract class Bayes2GOScore
{
	/** Current score */
	protected double score;
	
	/** Source of randomness */
	protected Random rnd;
	
	protected GOTermEnumerator populationEnumerator;
	protected Set<ByteString> population;
	protected Set<ByteString> observedActiveGenes;
	
	/** Array of terms */
	protected TermID [] termsArray;

	/** Indicates the activation state of a term */
	protected boolean [] isActive;

	/** Contains active terms */
	protected LinkedHashSet<TermID> activeTerms = new LinkedHashSet<TermID>();

	/** Contains genes that are active according to the active terms */
	protected LinkedHashMap<ByteString,Integer> activeHiddenGenes = new LinkedHashMap<ByteString,Integer>();
	
	/** Maps the term to the index in allTermsArray */
	protected HashMap<TermID,Integer> term2TermsIdx = new HashMap<TermID,Integer>();
	
	/** The current number of inactive terms */
	protected int numInactiveTerms;
	
	/**
	 * An array representing the inactive terms.
	 * 
	 * Note that only the first elements as given
	 * by the attribute numInactiveTerms are the
	 * inactive terms. 
	 */
	protected TermID[] inactiveTermsArray;

	/**
	 * From a term to an index of the inactiveTermsArray.
	 */
	protected HashMap<TermID,Integer> term2InactiveTermsIdx = new HashMap<TermID,Integer>();


	public Bayes2GOScore(List<TermID> termList, GOTermEnumerator populationEnumerator, Set<ByteString> observedActiveGenes)
	{
		this(null,termList, populationEnumerator, observedActiveGenes);
	}
	
	public Bayes2GOScore(Random rnd, List<TermID> termList, GOTermEnumerator populationEnumerator, Set<ByteString> observedActiveGenes)
	{
		int i;

		this.rnd = rnd;
		
		isActive = new boolean[termList.size()];
		termsArray = new TermID[termList.size()];
		inactiveTermsArray = new TermID[termList.size()];
		numInactiveTerms = termList.size();
		i=0;
		for (TermID tid : termList)
		{
			term2TermsIdx.put(tid,i);
			termsArray[i]=tid;

			inactiveTermsArray[i] = tid;
			term2InactiveTermsIdx.put(tid, i);

			i++;
		}
		
		this.populationEnumerator = populationEnumerator;

		activeTerms = new LinkedHashSet<TermID>();
		activeHiddenGenes = new LinkedHashMap<ByteString,Integer>();

		population = populationEnumerator.getGenes(); 
		this.observedActiveGenes = observedActiveGenes;
	}

	/**
	 * Returns the score of the current state.
	 * 
	 * @return
	 */
	public double getScore()
	{
		return score;
	}
	
	public abstract double proposeNewState(long rand);
	public double proposeNewState()
	{
		return proposeNewState(rnd.nextLong());
	}

	public abstract void hiddenGeneActivated(ByteString gene);
	public abstract void hiddenGeneDeactivated(ByteString gene);
	
	public void switchState(int toSwitch)
	{
		TermID t = termsArray[toSwitch];
		isActive[toSwitch] = !isActive[toSwitch];
		if (isActive[toSwitch])
		{
			/* A term is added */
			activeTerms.add(t);

			/* Update hiddenActiveGenes */
			for (ByteString gene : populationEnumerator.getAnnotatedGenes(t).totalAnnotated)
			{
				Integer cnt = activeHiddenGenes.get(gene);
				if (cnt == null)
				{
					hiddenGeneActivated(gene);
					activeHiddenGenes.put(gene, 1);
				} else
				{
					activeHiddenGenes.put(gene, cnt + 1);
				}
			}

			int inactiveIndex = term2InactiveTermsIdx.get(t);

			if (inactiveIndex != (numInactiveTerms - 1))
			{
				inactiveTermsArray[inactiveIndex] = inactiveTermsArray[numInactiveTerms - 1];
				term2InactiveTermsIdx.put(inactiveTermsArray[inactiveIndex], inactiveIndex);
			}
			
			term2InactiveTermsIdx.remove(t);
			numInactiveTerms--;
		} else
		{
			/* Remove a term */
			activeTerms.remove(t);

			/* Update hiddenActiveGenes */
			for (ByteString gene : populationEnumerator.getAnnotatedGenes(t).totalAnnotated)
			{
				int cnt = activeHiddenGenes.get(gene);
				cnt--;
				if (cnt == 0)
				{
					activeHiddenGenes.remove(gene);
					hiddenGeneDeactivated(gene);
				} else activeHiddenGenes.put(gene, cnt);
			}

			/* Append the new term at the end of the index list */
			inactiveTermsArray[numInactiveTerms] = t;
			term2InactiveTermsIdx.put(t, numInactiveTerms);
			numInactiveTerms++;
		}
	}

	public void exchange(TermID t1, TermID t2)
	{
		switchState(term2TermsIdx.get(t1));
		switchState(term2TermsIdx.get(t2));
	}


	public void undoProposal()
	{
	}
	
	public int neighbourhoodSize()
	{
		return 0;
	}
}

/**
 * Score of a setting in which alpha and beta are known.
 *  
 * @author Sebastian Bauer
 */
class VariableAlphaBetaScore extends Bayes2GOScore
{
	private HashMap<ByteString, Double> llr = new HashMap<ByteString,Double>();
	private double alpha;
	private double beta;

	public VariableAlphaBetaScore(Random rnd, List<TermID> termList, GOTermEnumerator populationEnumerator, Set<ByteString> observedActiveGenes, double alpha, double beta)
	{
		super(rnd, termList, populationEnumerator, observedActiveGenes);
		
		this.alpha = alpha;
		this.beta = beta;
		
		calcLLR();
	}

	public void calcLLR()
	{
		for (ByteString g : population)
		{
			if (observedActiveGenes.contains(g))
				llr.put(g, Math.log(1-beta) - Math.log(alpha)); // P(oi=1|h=1) / P(oi=1|h=0)
			else
				llr.put(g, Math.log(beta) - Math.log(1-alpha)); // P(oi=0|h=1) / P(oi=0|h=0)
		}

	}

	private int proposalSwitch;
	private TermID proposalT1;
	private TermID proposalT2;

	public void hiddenGeneActivated(ByteString gene)
	{
		score += llr.get(gene);
	}
	
	public void hiddenGeneDeactivated(ByteString gene)
	{
		score -= llr.get(gene);
	}
	
	@Override
	public double proposeNewState(long rand)
	{
		long oldPossibilities = getNeighborhoodSize();

		proposalSwitch = -1;
		proposalT1 = null;
		proposalT2 = null;

		long choose = Math.abs(rand) % oldPossibilities;

		if (choose < termsArray.length)
		{
			/* on/off */
			proposalSwitch = (int)choose;
			switchState(proposalSwitch);
		}	else
		{
			long base = choose - termsArray.length;
			
			int activeTermPos = (int)(base / numInactiveTerms);
			int inactiveTermPos = (int)(base % numInactiveTerms);
			
			for (TermID tid : activeTerms)
				if (activeTermPos-- == 0) proposalT1 = tid;
			proposalT2 = inactiveTermsArray[inactiveTermPos];

			exchange(proposalT1, proposalT2);
		}
		return score;
	}
	
	@Override
	public void undoProposal()
	{
		if (proposalSwitch != -1)	switchState(proposalSwitch);
		else exchange(proposalT2, proposalT1);
	}

	public long getNeighborhoodSize()
	{
		return termsArray.length + activeTerms.size() * numInactiveTerms;
	}
}

/**
 * Score of a setting in which alpha and beta are not known.
 * 
 * @author Sebastian Bauer
 */
class FixedAlphaBetaScore extends Bayes2GOScore
{
	private int proposalSwitch;
	private TermID proposalT1;
	private TermID proposalT2;

	private int n00;
	private int n01;
	private int n10;
	private int n11;

	public FixedAlphaBetaScore(Random rnd, List<TermID> termList, GOTermEnumerator populationEnumerator, Set<ByteString> observedActiveGenes)
	{
		super(rnd, termList, populationEnumerator, observedActiveGenes);

		n10 = observedActiveGenes.size();
		n00 = population.size() - n10;
	}

	@Override
	public void hiddenGeneActivated(ByteString gene)
	{
		if (observedActiveGenes.contains(gene))
		{
			n11++;
			n10--;
		} else
		{
			n01++;
			n00--;
		}
	}

	@Override
	public void hiddenGeneDeactivated(ByteString gene)
	{
		if (observedActiveGenes.contains(gene))
		{
			n11--;
			n10++;
		} else
		{
			n01--;
			n00++;
		}
	}

	@Override
	public double proposeNewState(long rand)
	{
		long oldPossibilities = getNeighborhoodSize();

		proposalSwitch = -1;
		proposalT1 = null;
		proposalT2 = null;

		long choose = Math.abs(rand) % oldPossibilities;

		if (choose < termsArray.length)
		{
			/* on/off */
			proposalSwitch = (int)choose;
			switchState(proposalSwitch);
		}	else
		{
			long base = choose - termsArray.length;
			
			int activeTermPos = (int)(base / numInactiveTerms);
			int inactiveTermPos = (int)(base % numInactiveTerms);
			
			for (TermID tid : activeTerms)
				if (activeTermPos-- == 0) proposalT1 = tid;
			proposalT2 = inactiveTermsArray[inactiveTermPos];

			exchange(proposalT1, proposalT2);
		}
		return score;
	}
	
	@Override
	public double getScore()
	{
		double alpha = 0.1;
		double beta = 0.1;

//		HashSet<ByteString> activeAgreement = new HashSet<ByteString>(activeHiddenGenes.keySet());
//		activeAgreement.retainAll(observedActiveGenes);
//
//		int n11 = activeAgreement.size();
//		int n10 = observedActiveGenes.size() - n11;
//
//		HashSet<ByteString> inactiveAgreement = new HashSet<ByteString>(population);
//		inactiveAgreement.removeAll(observedActiveGenes);
//		inactiveAgreement.removeAll(activeHiddenGenes.keySet());
//		
//		int n00 = inactiveAgreement.size();
//		int n01 = population.size() - observedActiveGenes.size() - n00;
//
//		System.out.println("n00="+ n00 + " n01="+n01 + " n10=" + n10 + " n11="+n11);
//		System.out.println("n00="+ this.n00 + " n01="+this.n01 + " n10=" + this.n10 + " n11="+this.n11);
		
		double newScore2 = Math.log(alpha) * n10 + Math.log(1-alpha)*n11 + Math.log(1-beta)*n00 + Math.log(beta)*n01;
		newScore2 -= Math.log(alpha) * observedActiveGenes.size() + Math.log(1-beta)* (population.size() - observedActiveGenes.size());
		return newScore2;
	}
	
	public void undoProposal()
	{
		if (proposalSwitch != -1)	switchState(proposalSwitch);
		else exchange(proposalT2, proposalT1);
	}

	public long getNeighborhoodSize()
	{
		return termsArray.length + activeTerms.size() * numInactiveTerms;
	}
}

/**
 * 
 * @author Sebastian Bauer
 */
public class Bayes2GOCalculation implements ICalculation
{
	public double defaultP = 0.01;
	
	private boolean noPrior = false;
	
	private int expectedNumber = -1;
	public double alpha = 0.1;
	public double beta = 0.1;
	public long seed = 0;
	public boolean parameterEstimation = true;

	public ICalculationProgress calculationProgress;

	/**
	 * Sets the seed of the random calculation.
	 * 
	 * @param seed
	 */
	public void setSeed(long seed)
	{
		this.seed = seed;
	}

	public void setP(double defaultP)
	{
		this.defaultP = defaultP;
	}
	
	public void setAlpha(double alpha)
	{
		this.alpha = alpha;
	}
	
	public void setBeta(double beta)
	{
		this.beta = beta;
	}

	/**
	 * Calculates the score.
	 * 
	 * @param llr
	 * @param activeTerms
	 * @param allEnumerator
	 * @param p
	 * @return
	 */
	double score(HashMap<ByteString,Double> llr, Collection<TermID> activeTerms, GOTermEnumerator allEnumerator, double p)
	{
		double score = 0.0;

		LinkedHashSet<ByteString> hiddenActiveGenes = new LinkedHashSet<ByteString>();
		for (TermID active : activeTerms)
			hiddenActiveGenes.addAll(allEnumerator.getAnnotatedGenes(active).totalAnnotated);
		
		for (ByteString gene : hiddenActiveGenes)
			score += llr.get(gene);

		if (!noPrior)
			score += activeTerms.size() * Math.log(p/(1.0-p));
		return score;
	}

	double scoreFast(HashMap<ByteString,Double> llr, State state, double p)
	{
		double score = 0.0;
		
		for (Entry<ByteString, Integer>  entry : state.hiddenActiveGenes.entrySet())
			score += llr.get(entry.getKey());

		if (!noPrior)
			score += state.activeTerms.size() * Math.log(p/(1.0-p));
		return score;
	}

	public EnrichedGOTermsResult calculateStudySet(GOGraph graph,
			AssociationContainer goAssociations, PopulationSet populationSet,
			StudySet studySet, double p)
	{
		EnrichedGOTermsResult result = new EnrichedGOTermsResult(graph,goAssociations,studySet,populationSet.getGeneCount());

		GOTermEnumerator populationEnumerator = populationSet.enumerateGOTerms(graph, goAssociations);
		GOTermEnumerator studyEnumerator = studySet.enumerateGOTerms(graph, goAssociations);

		HashMap<ByteString, Double> llr = calcLLR(populationSet, studySet);

		if (expectedNumber != -1)
		{
			p = expectedNumber / (double)studyEnumerator.getTotalNumberOfAnnotatedTerms();
			System.out.println("Parameter p has been overwritten by expected number.");
		}

		System.out.println("Starting calculation: p=" + p + " alpha=" + alpha + " beta=" + beta);

		long start = System.nanoTime();
		calculateByMCMC(graph, result, populationEnumerator, studyEnumerator, populationSet, studySet, llr, p);
		long end = System.nanoTime();
		System.out.println(((end - start)/1000) + "ms");

//		calculateByOptimization(graph, result, populationEnumerator, studyEnumerator, llr);

		/** Print out the results **/
//		{
//			ArrayList<AbstractGOTermProperties> al = new ArrayList<AbstractGOTermProperties>(result.list.size());
//			al.addAll(result.list);
//			Collections.sort(al);
//			for (AbstractGOTermProperties prop : al)
//				System.out.println(prop.goTerm.getName() + " " + prop.p);
//			System.out.println("A total of " + al.size() + " entries");
//		}

		return result;
	}

	public HashMap<ByteString, Double> calcLLR(PopulationSet populationSet, StudySet studySet)
	{
		HashMap<ByteString,Double> llr = new HashMap<ByteString,Double>();
		for (ByteString g : populationSet)
		{
			if (studySet.contains(g))
				llr.put(g, Math.log(1-beta) - Math.log(alpha)); // P(oi=1|h=1) - P(oi=1|h=0)
			else
				llr.put(g, Math.log(beta) - Math.log(1-alpha)); // P(oi=0|h=1) - P(oi=0|h=0)
		}
		return llr;
	}

	public EnrichedGOTermsResult calculateStudySet(GOGraph graph,
			AssociationContainer goAssociations, PopulationSet populationSet,
			StudySet studySet, AbstractTestCorrection testCorrection)
	{
		return calculateStudySet(graph, goAssociations, populationSet, studySet, defaultP);
	}

	/**
	 * Represents a single space configuration.
	 * 
	 * @author Sebastian Bauer
	 */
	class State
	{
		/** Contains the active terms */
		public LinkedHashSet<TermID> activeTerms;

		/**
		 * Represents the genes that are covered by activeTerms. The value
		 * part contains the actual number of terms that annotate the gene.
		 * Note that genes that are not covered are not included in this 
		 * map. 
		 */ 
		private LinkedHashMap<ByteString,Integer> hiddenActiveGenes;

		private HashMap<ByteString, Double> llr;
		private double llScore;


		/** Contains all terms */ 
		private TermID[] allTermsArray;
		
		/** Indicates the activation state of a term */
		private boolean [] isActive;

		/** Maps the term to the index in allTermsArray */
		private HashMap<TermID,Integer> term2allTermsIdx;

		/** The current number of inactive terms */
		private int numInactiveTerms;
		
		/** Links terms to genes */
		private GOTermEnumerator populationEnumerator;

		/**
		 * An array representing the inactive terms.
		 * 
		 * Note that only the first elements as given
		 * by the attribute numInactiveTerms are the
		 * inactive terms. 
		 */
		private TermID[] inactiveTermsArray;

		/**
		 * From a term to an index of the inactievTermsArray.
		 */
		private HashMap<TermID,Integer> term2InactiveTermsIdx;

		/** Genes that are correctly active */
		public HashSet<ByteString> correctActiveGenes = new HashSet<ByteString>();
		
		/** Genes that are correctly inactive */
		public HashSet<ByteString> correctInactiveGenes = new HashSet<ByteString>();

//		public HashSet<ByteString> observedGenes;
		
		public State(List<TermID> allTerms, GOTermEnumerator populationEnumerator)
		{
			int i;
			
			this.populationEnumerator = populationEnumerator;

			llScore = 0;

			activeTerms = new LinkedHashSet<TermID>();
			hiddenActiveGenes = new LinkedHashMap<ByteString,Integer>();

			allTermsArray = new TermID[allTerms.size()];
			term2allTermsIdx = new HashMap<TermID,Integer>();

			numInactiveTerms = allTermsArray.length;
			inactiveTermsArray = new TermID[allTermsArray.length];
			term2InactiveTermsIdx = new HashMap<TermID,Integer>();

			i = 0;
			for (TermID tid : allTerms)
			{
				allTermsArray[i] = tid;
				term2allTermsIdx.put(tid, i);

				inactiveTermsArray[i] = tid;
				term2InactiveTermsIdx.put(tid, i);
				i++;
			}
			isActive = new boolean[allTermsArray.length];
		}
		
		public void switchState(int toSwitch)
		{
			TermID t = allTermsArray[toSwitch];
			isActive[toSwitch] = !isActive[toSwitch];
			if (isActive[toSwitch])
			{
				/* A term is added */
				activeTerms.add(t);

				/* Update hiddenActiveGenes */
				for (ByteString gene : populationEnumerator.getAnnotatedGenes(t).totalAnnotated)
				{
					Integer cnt = hiddenActiveGenes.get(gene);
					if (cnt == null)
					{
						llScore += llr.get(gene);
						hiddenActiveGenes.put(gene, 1);

//						/* Reformulation */
//						if (observedGenes.contains(gene))
//							correctActiveGenes.add(gene);
					} else
					{
						hiddenActiveGenes.put(gene, cnt + 1);
					}
				}

				int inactiveIndex = term2InactiveTermsIdx.get(t);

				/* Put the last element onto the position of the to be removed term */
//				System.err.println("Add Active " + t.toString() + "  " + inactiveIndex + " " + (numInactiveTerms - 1));

				if (inactiveIndex != (numInactiveTerms - 1))
				{
					inactiveTermsArray[inactiveIndex] = inactiveTermsArray[numInactiveTerms - 1];
					term2InactiveTermsIdx.put(inactiveTermsArray[inactiveIndex], inactiveIndex);
				}
				
				term2InactiveTermsIdx.remove(t);
				numInactiveTerms--;
			} else
			{
				/* Remove a term */
				activeTerms.remove(t);

				/* Update hiddenActiveGenes */
				for (ByteString gene : populationEnumerator.getAnnotatedGenes(t).totalAnnotated)
				{
					int cnt = hiddenActiveGenes.get(gene);
					cnt--;
					if (cnt == 0)
					{
						hiddenActiveGenes.remove(gene);
						llScore -= llr.get(gene);
					} else hiddenActiveGenes.put(gene, cnt);
				}

				/* Append the new term at the end of the index list */
				inactiveTermsArray[numInactiveTerms] = t;
				term2InactiveTermsIdx.put(t, numInactiveTerms);
				numInactiveTerms++;
			}
			
//			System.out.println("switch: " + hiddenActiveGenes.size());

//			testForConsitency();
		}
		
		public void testForConsitency()
		{
			for (int i=0;i<numInactiveTerms;i++)
			{
				TermID tid = inactiveTermsArray[i];
				if (activeTerms.contains(tid))
				{
					System.err.println("Inconsitent!! pos=" + i + "  " + tid.toString());
				}
			}
			
			if (numInactiveTerms + activeTerms.size() != allTermsArray.length)
			{
				System.err.println("Inconsitent!");
			}
		}
		
		public void exchange(TermID t1, TermID t2)
		{
			switchState(term2allTermsIdx.get(t1));
			switchState(term2allTermsIdx.get(t2));
		}
	}

	public void setExpectedNumber(int expectedNumber)
	{
		this.expectedNumber = expectedNumber;
	}

	public void setNoPrior(boolean noPrior)
	{
		this.noPrior = noPrior;
	}

	private void calculateByMCMC(GOGraph graph,
			EnrichedGOTermsResult result,
			GOTermEnumerator populationEnumerator,
			GOTermEnumerator studyEnumerator,
			PopulationSet populationSet,
			StudySet studySet,
			HashMap<ByteString, Double> llr,
			double p)
	{
		List<TermID> allTerms = studyEnumerator.getAllAnnotatedTermsAsList();

		Random rnd;
		if (seed != 0)
		{
			rnd = new Random(seed);
			System.err.println("Created random number generator with seed of " + seed);
		}
		else rnd = new Random();

//		VariableAlphaBetaScore bayesScore = new VariableAlphaBetaScore(rnd, allTerms, populationEnumerator, studySet.getAllGeneNames(), alpha, beta);
		FixedAlphaBetaScore bayesScore = new FixedAlphaBetaScore(rnd, allTerms, populationEnumerator,  studySet.getAllGeneNames());

//		score.
//		State state = new State(allTerms, populationEnumerator);
//		state.llr = llr;
//		state.observedGenes = observedGenes;

		/* Stores the terms activation counts */
		int [] activeCount = new int[allTerms.size()];

//		/* Initialize initially active terms randomly */
//		for (int i=0;i<allTermsArrayList.size();i++)
//		{
//			if ((isActive[i] = rnd.nextBoolean()))
//				activeTerms.add(term[i]);
//		}

		/* Initialize with some default values */
//		activeTerms.add(new TermID("GO:0007049")); /* cell cycle */
//		activeTerms.add(new TermID("GO:0043473")); /* pigmentation */
//		activeTerms.add(new TermID("GO:0001505")); /* regulation of neuro transmitter levels */

		
//		for (TermID t : state.activeTerms)
//			state.switchState(state.term2allTermsIdx.get(t));

		int maxSteps = 320000;
		int burnin = 20000;
		int samplesTaken = 0;
		int numAccepts = 0;
		int numRejects = 0;

		if (noPrior) maxSteps = maxSteps * 3 / 2;

		if (calculationProgress != null)
			calculationProgress.init(maxSteps);

//		double score = score(llr, state.activeTerms, populationEnumerator, p);
		double score = bayesScore.getScore();
		
		double maxScore = Double.MIN_VALUE;
		ArrayList<TermID> maxScoredTerms = new ArrayList<TermID>();
		
		System.out.println("Initial score: " + score);
		
		long start = System.currentTimeMillis();
		
		for (int t=0;t<maxSteps;t++)
		{
			/* Remember maximum score and terms */
			if (score > maxScore)
			{
				maxScore = score;
				maxScoredTerms = new ArrayList<TermID>(bayesScore.activeTerms);
			}

			long now = System.currentTimeMillis();
			if (now - start > 5000)
			{
				System.out.println((t*100/maxSteps) + "% (score=" + score +" maxScore=" + maxScore + " #terms="+bayesScore.activeTerms.size()+
									" accept/reject=" + String.format("%g",(double)numAccepts / (double)numRejects) +
									" accept/steps=" + String.format("%g",(double)numAccepts / (double)t) +
									" p=" + p + " noPrior=" + noPrior + ")");
				start = now;
				
				if (calculationProgress != null)
					calculationProgress.update(t);
			}

			long oldPossibilities = bayesScore.getNeighborhoodSize();
			long r = rnd.nextLong();
			bayesScore.proposeNewState(r);
//			fBayesScore.proposeNewState(r);
			double newScore = bayesScore.getScore();
//			double newScore2 = fBayesScore.getScore();
			long newPossibilities = bayesScore.getNeighborhoodSize();

//			System.out.println(newScore + "  " + newScore2);
//
//			long oldPossibilities;
//			
//			/* The number of possibilities is, on/off plus the number of replacements */
//			oldPossibilities = state.allTermsArray.length + state.activeTerms.size() * state.numInactiveTerms;
//
//			int toSwitch = -1;
//			TermID t1 = null;
//			TermID t2 = null;
//			long choose = Math.abs(rnd.nextLong()) % oldPossibilities;
//
//			if (choose < state.allTermsArray.length)
//			{
//				/* on/off */
//				toSwitch = (int)choose;
//				state.switchState(toSwitch);
//			}	else
//			{
//				long base = choose - state.allTermsArray.length;
//				
//				int activeTermPos = (int)(base / state.numInactiveTerms);
//				int inactiveTermPos = (int)(base % state.numInactiveTerms);
//				
//				for (TermID tid : state.activeTerms)
//					if (activeTermPos-- == 0) t1 = tid;
//				t2 = state.inactiveTermsArray[inactiveTermPos];
//
//				state.exchange(t1, t2);
//			}
//			
//
//			long newPossibilities = state.allTermsArray.length + state.activeTerms.size() * state.numInactiveTerms;
//
////			double newScore = score(llr, state.activeTerms, populationEnumerator, p);
////			double newScore = scoreFast(llr, state, p);
//			double newScore = state.llScore;
//			
//			{
////				double newScore2 =  Math.log(alpha);
//				
//				HashSet<ByteString> activeAgreement = new HashSet<ByteString>(state.hiddenActiveGenes.keySet());
//				activeAgreement.retainAll(studySet.getAllGeneNames());
//
//				int n11 = activeAgreement.size();
//				int n10 = studySet.getGeneCount() - n11;
//
//				HashSet<ByteString> inactiveAgreement = new HashSet<ByteString>(populationSet.getAllGeneNames());
//				inactiveAgreement.removeAll(studySet.getAllGeneNames());
//				inactiveAgreement.removeAll(state.hiddenActiveGenes.keySet());
//				
//				int n00 = inactiveAgreement.size();
//				int n01 = populationSet.getGeneCount() - studySet.getGeneCount() - n00;
//
//				double newScore2 = Math.log(alpha) * n10 + Math.log(1-alpha)*n11 + Math.log(1-beta)*n00 + Math.log(beta)*n01;
//				
//
//				newScore2 -= Math.log(alpha) * studySet.getGeneCount() + Math.log(1-beta)* (populationSet.getGeneCount() - studySet.getGeneCount());
//
//				System.out.println(newScore + "  " + newScore2 + " n10=" + n10 + " n11="+n11 + " n01=" + n01 + " n00=" + n00);
//			}
//
			if (!noPrior) newScore += bayesScore.activeTerms.size() * Math.log(p/(1.0-p));
//
			double acceptProb = Math.exp(newScore - score)*(double)oldPossibilities/(double)newPossibilities; /* last quotient is the hasting ratio */
//
			boolean DEBUG = false;
//
			if (DEBUG) System.out.print(bayesScore.activeTerms.size() + "  score=" + score + " newScore="+newScore + " maxScore=" + maxScore + " a=" + acceptProb);
//			
			double u = rnd.nextDouble();
			if (u >= acceptProb)
			{
//				if (toSwitch != -1)
//				{
//					state.switchState(toSwitch);
//				} else
//				{
//					state.exchange(t2, t1);
//				}
				bayesScore.undoProposal();
//				fBayesScore.undoProposal();
				numRejects++;
			} else
			{
				score = newScore;
				numAccepts++;
			}
			if (DEBUG) System.out.println();

			if (t>burnin)
			{
				for (TermID tid : bayesScore.activeTerms)
					activeCount[bayesScore.term2TermsIdx.get(tid)]++;
				samplesTaken++;
			}
		}

		for (TermID t : allTerms)
		{
			TermForTermGOTermProperties prop = new TermForTermGOTermProperties();
			prop.ignoreAtMTC = true;
			prop.goTerm = graph.getGOTerm(t);
			prop.annotatedStudyGenes = studyEnumerator.getAnnotatedGenes(t).totalAnnotatedCount();
			prop.annotatedPopulationGenes = populationEnumerator.getAnnotatedGenes(t).totalAnnotatedCount();
			
			/* We reverse the probability as the framework assumes that low p values are important */
			prop.p = 1 - ((double)activeCount[bayesScore.term2TermsIdx.get(t)] / samplesTaken);
			prop.p_adjusted = prop.p;
			prop.p_min = 0.001;
			result.addGOTermProperties(prop);
		}

		System.out.println("numAccepts=" + numAccepts + "  numRejects = " + numRejects);

		/* Print out the term combination which scored max */
		System.out.println("Term combination that reaches score of " + maxScore);
		for (TermID tid : maxScoredTerms)
		{
			System.out.println(tid.toString() + "/" + graph.getGOTerm(tid).getName());
		}
	}

	private void calculateByOptimization(GOGraph graph,
			EnrichedGOTermsResult result,
			GOTermEnumerator populationEnumerator,
			GOTermEnumerator studyEnumerator, HashMap<ByteString, Double> llr, double p)
	{
		List<TermID> allTerms = populationEnumerator.getAllAnnotatedTermsAsList();
		LinkedHashSet<TermID> activeTerms = new LinkedHashSet<TermID>();

		double totalBestScore = score(llr, activeTerms, populationEnumerator, p);
		
		System.out.println("Initial cost: " + totalBestScore);

		TermID bestTerm;
		do
		{
			double currentBestCost = totalBestScore;
			bestTerm = null;

			/* Find the best term */
			for (TermID t : allTerms)
			{
				if (activeTerms.contains(t))
					continue;

				activeTerms.add(t);
				double newCost = score(llr,activeTerms,populationEnumerator,p);
				if (newCost > currentBestCost)
				{
					bestTerm = t;
					currentBestCost = newCost;
				}
				activeTerms.remove(t);
			}

			if (bestTerm == null)
				break;

			activeTerms.add(bestTerm);
			totalBestScore = score(llr,activeTerms,populationEnumerator,p);

			System.out.println("Adding term " + bestTerm + "  " + graph.getGOTerm(bestTerm).getName() + "  " + graph.getGOTerm(bestTerm).getNamespaceAsString() + "  " + currentBestCost);
		} while(bestTerm != null);

		for (TermID t : allTerms)
		{
			TermForTermGOTermProperties prop = new TermForTermGOTermProperties();
			prop.ignoreAtMTC = true;
			prop.goTerm = graph.getGOTerm(t);
			prop.annotatedStudyGenes = studyEnumerator.getAnnotatedGenes(t).totalAnnotatedCount();
			prop.annotatedPopulationGenes = populationEnumerator.getAnnotatedGenes(t).totalAnnotatedCount();

			if (activeTerms.contains(t))
			{
				prop.p = 0.005;
				prop.p_adjusted = 0.005;
				prop.p_min = 0.001;
			} else
			{
				prop.p = 0.99;
				prop.p_adjusted = 0.99;
				prop.p_min = 0.001;
			}
			result.addGOTermProperties(prop);
		}
	}

	public String getDescription()
	{
		// TODO Auto-generated method stub
		return null;
	}

	public String getName() {
		return "Bayes2GO";
	}

	public static GOGraph graph;
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
		graph = new GOGraph(termContainer);

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

	public static void main(String[] args) throws InterruptedException
	{
		final HashSet<TermID> wantedActiveTerms = new HashSet<TermID>(); /* Terms that are active */

		/* ***************************************************************** */
		loadOntology();
		wantedActiveTerms.add(new TermID("GO:0007049")); /* cell cycle */
		wantedActiveTerms.add(new TermID("GO:0043473")); /* pigmentation */
		wantedActiveTerms.add(new TermID("GO:0001505")); /* regulation of neuro transmitter levels */
		wantedActiveTerms.add(new TermID("GO:0035237")); /* corazonin receptor activity */

//		createInternalOntology(1);
//		wantedActiveTerms.add(new TermID("GO:0000010"));
//		wantedActiveTerms.add(new TermID("GO:0000004"));

		/* ***************************************************************** */

		Random rnd = new Random(1);
		
		/* Simulation */

		PopulationSet allGenes = new PopulationSet("all");
		for (ByteString gene : assoc.getAllAnnotatedGenes())
			allGenes.addGene(gene, "");

		System.out.println("Total number of genes " + allGenes);
		
		StudySet newStudyGenes = new StudySet("study");
		
		final GOTermEnumerator allEnumerator = allGenes.enumerateGOTerms(graph, assoc);
		for (TermID t : wantedActiveTerms)
		{
			for (ByteString g : allEnumerator.getAnnotatedGenes(t).totalAnnotated)
				newStudyGenes.addGene(g, "");
		}
		newStudyGenes.filterOutDuplicateGenes(assoc);
		System.out.println("Number of genes in study set " + newStudyGenes.getGeneCount());

		double alphaStudySet = 0.1;
		double betaStudySet = 0.1;

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
		for (ByteString gene : newStudyGenes)
		{
			if (rnd.nextDouble() < betaStudySet) fn.add(gene);
		}
		newStudyGenes.addGenes(fp);
		newStudyGenes.removeGenes(fn);
		
		System.out.println("Study set misses " + fn.size() + " genes");
		System.out.println("Study set has " + fp.size() + " false positives");
		System.out.println("Study set has a total of " +  newStudyGenes.getGeneCount() + " genes.");

		/**** Write out the graph ****/
		//{
			HashSet<TermID> allTermIDs = new HashSet<TermID>();
			for (Term t : graph)
				allTermIDs.add(t.getID());

			final GOTermEnumerator studySetEnumerator = newStudyGenes.enumerateGOTerms(graph, assoc);

			GODOTWriter.writeDOT(graph, new File("toy-all.dot"), null, allTermIDs, new IDotNodeAttributesProvider()
			{
				public String getDotNodeAttributes(TermID id)
				{
					StringBuilder str = new StringBuilder(200);
					str.append("label=\"");
					str.append(graph.getGOTerm(id).getName());
					str.append("\\n");
					str.append(studySetEnumerator.getAnnotatedGenes(id).totalAnnotatedCount() + "/" + allEnumerator.getAnnotatedGenes(id).totalAnnotatedCount());
					str.append("\"");
					if (wantedActiveTerms.contains(id))
					{
						str.append("style=\"filled\" color=\"gray\"");
					}
					return str.toString();
				}
			});
			
			GODOTWriter.writeDOT(graph, new File("toy-induced.dot"), null, wantedActiveTerms, new IDotNodeAttributesProvider()
			{
				public String getDotNodeAttributes(TermID id)
				{
					StringBuilder str = new StringBuilder(200);
					str.append("label=\"");
					str.append(graph.getGOTerm(id).getName());
					str.append("\\n");
					str.append(studySetEnumerator.getAnnotatedGenes(id).totalAnnotatedCount() + "/" + allEnumerator.getAnnotatedGenes(id).totalAnnotatedCount());
					str.append("\"");
					if (wantedActiveTerms.contains(id))
					{
						str.append("style=\"filled\" color=\"gray\"");
					}
					return str.toString();
				}
			});

		//}

		double p = (double)wantedActiveTerms.size() / allEnumerator.getTotalNumberOfAnnotatedTerms();

//		ProbabilisticCalculation calc = new ProbabilisticCalculation();
//		TopologyWeightedCalculation calc = new TopologyWeightedCalculation();
//		TermForTermCalculation calc = new TermForTermCalculation();
		Bayes2GOCalculation calc = new Bayes2GOCalculation();
//		calc.setNoPrior(true);
		calc.setP(p);
		calc.setSeed(1);
		
		evaluate(wantedActiveTerms, allGenes, newStudyGenes, allEnumerator, studySetEnumerator, calc, p);
	}

	private static void evaluate(final HashSet<TermID> wantedActiveTerms,
			PopulationSet allGenes, StudySet newStudyGenes,
			final GOTermEnumerator allEnumerator,
			final GOTermEnumerator studySetEnumerator,
			ICalculation calc, double p)
	{
		final EnrichedGOTermsResult result = calc.calculateStudySet(graph, assoc, allGenes, newStudyGenes, new Bonferroni());
		
		boolean pIsReverseMarginal = false;
	
		System.out.println("Method is " + calc.getName());

		/*** Calculate the score of the optimal term set ***/
		
		if (calc instanceof Bayes2GOCalculation)
		{
			Bayes2GOCalculation b2g = (Bayes2GOCalculation)calc;
			
			HashMap<ByteString, Double> llr = b2g.calcLLR(allGenes, newStudyGenes);
			double wantedScore = b2g.score(llr, wantedActiveTerms, allEnumerator, p);
			
			System.out.println("Score of the optimal set is " + wantedScore);
			
			pIsReverseMarginal = true;
		}
		
		//scoreDistribution(calc,allEnumerator,allGenes,newStudyGenes);
		
		System.out.println("The overrepresented terms:");
		for (TermID w : wantedActiveTerms)
		{
			AbstractGOTermProperties prop = result.getGOTermProperties(w);
			System.out.println(" " + prop.goTerm.getIDAsString() + "/" + prop.goTerm.getName() + "   " + (/*1.0f - */prop.p_adjusted) + ")");
		}

		{
			System.out.println("The terms found by the algorithm:");
			HashSet<TermID> terms = new HashSet<TermID>();
			for (AbstractGOTermProperties prop : result)
			{
				if (prop.p_adjusted < 0.9)
				{
					terms.add(prop.goTerm.getID());
					System.out.println(" " + prop.goTerm.getIDAsString() + "/" + prop.goTerm.getName() + "   " + (/*1.0f - */prop.p_adjusted) + ")");
				}
			}
			terms.addAll(wantedActiveTerms);

			GODOTWriter.writeDOT(graph, new File("toy-result.dot"), null, terms, new IDotNodeAttributesProvider()
			{
				public String getDotNodeAttributes(TermID id)
				{
					StringBuilder str = new StringBuilder(200);
					str.append("label=\"");
					str.append(graph.getGOTerm(id).getName());
					str.append("\\n");
					if (result.getGOTermProperties(id) != null)
						str.append(String.format("p(t)=%g\\n", /*1-*/result.getGOTermProperties(id).p_adjusted));
					str.append(studySetEnumerator.getAnnotatedGenes(id).totalAnnotatedCount() + "/" + allEnumerator.getAnnotatedGenes(id).totalAnnotatedCount());
					str.append("\"");
					if (wantedActiveTerms.contains(id))
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
}

