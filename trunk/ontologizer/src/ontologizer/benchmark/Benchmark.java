package ontologizer.benchmark;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ontologizer.ByteString;
import ontologizer.GOTermEnumerator;
import ontologizer.GlobalPreferences;
import ontologizer.OntologizerThreadGroups;
import ontologizer.PopulationSet;
import ontologizer.StudySet;
import ontologizer.association.AssociationContainer;
import ontologizer.calculation.AbstractGOTermProperties;
import ontologizer.calculation.Bayes2GOCalculation;
import ontologizer.calculation.CalculationRegistry;
import ontologizer.calculation.EnrichedGOTermsResult;
import ontologizer.calculation.ICalculation;
import ontologizer.go.GOGraph;
import ontologizer.go.TermID;
import ontologizer.sampling.KSubsetSampler;
import ontologizer.sampling.PercentageEnrichmentRule;
import ontologizer.sampling.StudySetSampler;
import ontologizer.statistics.AbstractTestCorrection;
import ontologizer.statistics.None;

/**
 * Main class responsible for performing benchmarks.
 * 
 * @author Sebastian Bauer
 */
public class Benchmark
{
	private static int NOISE_PERCENTAGE = 10;
	private static int TERM_PERCENTAGE = 75;
	private static double ALPHA = 0.1;
	private static double BETA = 0.25;
	private static boolean ORIGINAL_SAMPLING = false;
	private static int MAX_TERMS = 5;
	private static int TERMS_PER_RUN = 50;
	
	/**
	 * Senseful terms are terms that have an annotation proportion between 0.1
	 * and 0.9
	 */
	private static int SENSEFUL_TERMS_PER_RUN = 50;

	private static AbstractTestCorrection testCorrection = new None();

	static class Method 
	{
		public String method;
		public String abbrev;
		public double alpha;
		public double beta;
		public boolean noPrior;
		
		/** Number of desired terms */
		public int dt;

		public boolean em;

		public Method(String m, String a)
		{
			method = m;
			abbrev = a;
		}

		public Method(String m, String a, double alpha, double beta, int dt)
		{
			method = m;
			abbrev = a;

			this.alpha = alpha;
			this.beta = beta;
			this.dt = dt;
		}

	}

	static ArrayList<Method> calcMethods;
	static double [] calcAlpha = new double[]{/*0.05,0.1,0.25,*/0.5};
	static double [] calcBeta = new double[]{/*0.05,0.1,0.25,*/0.5};
	static int [] calcDesiredTerms = new int []{1/*,2,4,6,8*/};

	static
	{
		calcMethods = new ArrayList<Method>();
//		calcMethods.add(new Method("Bayes2GO","b2g.ideal"));
//		for (double a : calcAlpha)
//		{
//			for (double b : calcBeta)
//			{
//				for (int cdt : calcDesiredTerms)
//				{
//					String colName = String.format("b2g.a%.2g.b%.2g.d%d", a,b,cdt);
//					calcMethods.add(new Method("Bayes2GO",colName,a,b,cdt));
//				}
//			}
//		}
		calcMethods.add(new Method("Term-For-Term","tft"));
		calcMethods.add(new Method("Parent-Child-Union","pcu"));
//		calcMethods.add(new Method("Probabilistic","pb"));
//		calcMethods.add(new Method("Topology-Weighted","tweight"));
//
//		Method m = new Method("Bayes2GO","b2g.em");
//		m.em = true;
//		calcMethods.add(m);
//
//		m = new Method("Bayes2GO","b2g.ideal.nop");
//		m.noPrior = true;
//		calcMethods.add(m);
	}

	public static void main(String[] args) throws Exception
	{
		int numProcessors = Runtime.getRuntime().availableProcessors();

//		Bayes2GOCalculation.alpha = ALPHA;
//		Bayes2GOCalculation.beta = BETA;

GlobalPreferences.setProxyPort(888);
GlobalPreferences.setProxyHost("realproxy.charite.de");

		String oboPath = "http://www.geneontology.org/ontology/gene_ontology_edit.obo";
		String assocPath = "http://cvsweb.geneontology.org/cgi-bin/cvsweb.cgi/go/gene-associations/gene_association.fb.gz?rev=HEAD";

		long seed = 1;//System.nanoTime());
		final Random rnd = new Random(seed);

		Datafiles df = new Datafiles(oboPath,assocPath);
		final AssociationContainer assoc = df.assoc;
		final GOGraph graph = df.graph;

		Set<ByteString> allAnnotatedGenes = assoc.getAllAnnotatedGenes();

		final PopulationSet completePop = new PopulationSet("AllAnnotated");
		for (ByteString gene : allAnnotatedGenes)
			completePop.addGene(gene,"None");
		completePop.filterOutAssociationlessGenes(assoc);
		completePop.countGOTerms(graph, assoc);

		final GOTermEnumerator completePopEnumerator = completePop.enumerateGOTerms(graph, assoc);
		final ByteString [] allGenesArray = completePop.getGenes();
		final TermID root = graph.getRootGOTerm().getID();
		final int rootGenes = completePopEnumerator.getAnnotatedGenes(root).totalAnnotatedCount();

		System.out.println("Population set conists of " + allGenesArray.length + " genes. Root term has " + rootGenes + " associated genes");
		if (allGenesArray.length != rootGenes)
		{
			System.out.println("Gene count doesn't match! Aborting.");
			System.exit(-1);
		}

		/* Two scenarios: 1) alpha/beta pair
		 *                2) study set enrichment 
		 */

		ArrayList<TermID> goodTerms = new ArrayList<TermID>();
		for (TermID t : completePopEnumerator)
			goodTerms.add(t);
		
		System.out.println("We have a total of " + goodTerms.size() + " terms with annotations");
		
		ArrayList<TermID> sensefulTerms = new ArrayList<TermID>();
		for (TermID t : completePopEnumerator)
		{
			int terms = completePopEnumerator.getAnnotatedGenes(t).totalAnnotated.size();
			double p = (double)terms / rootGenes;
			
			if (terms > 4 && p < 0.90)
				sensefulTerms.add(t);
		}

		System.out.println("We have a total of " + sensefulTerms.size() + " senseful terms");
		
		final PrintWriter out = new PrintWriter(new File("result-" + (ORIGINAL_SAMPLING?"tn":"fp") + ".txt"));

		/* Prepare header */
		out.print("term\t");
		out.print("label\t");
		for (Method cm : calcMethods)
		{
			if (CalculationRegistry.getCalculationByName(cm.method) == null)
			{
				System.err.println("Couldn't find calculation with name \"" + cm.method + "\"");
				System.exit(-1);
			}
			out.print("p." + cm.abbrev + "\t");
		}
		out.print("more.general\t");
		out.print("more.specific\t");
		out.print("pop.genes\t");
		out.print("study.genes\t");
		out.print("run\t");
		out.println("senseful");
		
		/* We start with the term combinations */
		class Combination
		{
			ArrayList<TermID> termCombi;
			boolean isSenseful;
		}

		KSubsetSampler<TermID> kSubsetSampler = new KSubsetSampler<TermID>(goodTerms,rnd);
		ArrayList<Combination> combinationList = new ArrayList<Combination>();
		for (int i=1;i<=MAX_TERMS;i++)
		{
			for (ArrayList<TermID> termCombi : kSubsetSampler.sampleManyOrderedWithoutReplacement(i,TERMS_PER_RUN))
			{
				Combination comb = new Combination();
				comb.termCombi = termCombi;
				comb.isSenseful = false;
				combinationList.add(comb);
			}
		}

		KSubsetSampler<TermID> kSensefulSubsetSampler = new KSubsetSampler<TermID>(sensefulTerms,rnd);
		for (int i=1;i<=MAX_TERMS;i++)
		{
			for (ArrayList<TermID> termCombi : kSensefulSubsetSampler.sampleManyOrderedWithoutReplacement(i,SENSEFUL_TERMS_PER_RUN))
			{
				Combination comb = new Combination();
				comb.termCombi = termCombi;
				comb.isSenseful = true;
				combinationList.add(comb);
			}
		}

		ExecutorService es = Executors.newFixedThreadPool(numProcessors);
		
		/* Generate study set and calculate */
		int current = 0;
		final int max = combinationList.size();
		final StudySetSampler sampler = new StudySetSampler(completePop);
		for (final Combination combi : combinationList)
		{
			current++;

			final int currentRun = current;
			final ArrayList<TermID> termCombi = combi.termCombi;
			
			es.execute(new Runnable()
			{
				public void run()
				{
					try
					{
						System.out.println("***** " + currentRun + "/" + max + ": " + termCombi.size() + " terms" + " *****");

						StudySet newStudySet = generateStudySet(rnd, assoc, graph,
								completePopEnumerator, allGenesArray, sampler,
								termCombi);

						if (newStudySet != null)
						{
							GOTermEnumerator studyEnumerator = newStudySet.enumerateGOTerms(graph, assoc);

							/* Some buffer for the result */
							StringBuilder builder = new StringBuilder(100000);
							LinkedHashMap<TermID,Double []> terms2PVal = new LinkedHashMap<TermID,Double[]>();

							/* Gather results */
							for (int mPos = 0; mPos < calcMethods.size(); mPos++)
							{
								Method m = calcMethods.get(mPos);
								ICalculation calc = CalculationRegistry.getCalculationByName(m.method);
								
								EnrichedGOTermsResult result;
							
								if (calc instanceof Bayes2GOCalculation)
								{
									/* Set some parameter */
									Bayes2GOCalculation b2g = (Bayes2GOCalculation) calc;
									calc = b2g = new Bayes2GOCalculation(b2g);

									b2g.setSeed(rnd.nextLong());
									b2g.setNoPrior(m.noPrior);

									double p;
									
									if (!m.em)
									{
										if (m.dt == 0)
										{
											p = (double)termCombi.size() / completePopEnumerator.getTotalNumberOfAnnotatedTerms();
											b2g.setAlpha(ALPHA);
											b2g.setBeta(BETA);
											b2g.setP(p);
	
										} else
										{
											p = (double)m.dt / completePopEnumerator.getTotalNumberOfAnnotatedTerms();
											b2g.setAlpha(m.alpha);
											b2g.setBeta(m.beta);
											b2g.setP(p);
										}
									}
									
//									System.out.println(p);
//									result = b2g.calculateStudySet(graph, assoc, completePop, newStudySet, (double)termCombi.size() / completePopEnumerator.getTotalNumberOfAnnotatedTerms());
								}
								result = calc.calculateStudySet(graph, assoc, completePop, newStudySet, testCorrection);

								for (AbstractGOTermProperties p : result)
								{
									Double [] pVals = terms2PVal.get(p.goTerm.getID());
									if (pVals == null)
									{
										pVals = new Double[calcMethods.size()];
										for (int i=0;i<pVals.length;i++)
											pVals[i] = 2.0;
										terms2PVal.put(p.goTerm.getID(), pVals);
									}
									pVals[mPos] = p.p_adjusted;
								}
							}

							/* Write out the results */
							for (Entry<TermID,Double[]> entry : terms2PVal.entrySet())
							{
								TermID tid = entry.getKey();

								boolean termIsMoreGeneral = false;
								boolean termIsMoreSpecific = false;
								boolean label = termCombi.contains(tid);
	
								for (TermID toLookForTerm : termCombi)
								{
									if (graph.existsPath(tid,toLookForTerm))
									{
										termIsMoreGeneral = true;
										break;
									}
								}
	
								for (TermID t : termCombi)
								{
									if (graph.existsPath(t,tid))
									{
										termIsMoreSpecific = true;
										break;
									}
								}

								Double [] pVals = terms2PVal.get(tid);
	
								builder.append(tid.id + "\t");
								builder.append((label?"1":"0") + "\t");
								for (double p : pVals)
									builder.append(p + "\t");
								builder.append((termIsMoreGeneral?"1":"0") + "\t");
								builder.append((termIsMoreSpecific?"1":"0") + "\t");
								builder.append(completePopEnumerator.getAnnotatedGenes(tid).totalAnnotatedCount() + "\t");
								builder.append(studyEnumerator.getAnnotatedGenes(tid).totalAnnotatedCount() + "\t");
								builder.append(currentRun + "\t");
								builder.append((combi.isSenseful?"1":"0"));
								builder.append('\n');
							}
							
							synchronized (out) {
								out.print(builder);
								out.flush();
							}
						}
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
				}
			});
		}

		
		es.shutdown();
		System.out.println("Finish");

		synchronized (out) {
			out.flush();
		}

		OntologizerThreadGroups.workerThreadGroup.interrupt();
	}

	/**
	 * Generate the random study sets.
	 * 
	 * @param rnd
	 * @param assoc
	 * @param graph
	 * @param completePopEnumerator
	 * @param allGenesArray an array of all genes.
	 * @param sampler
	 * @param termCombi term combinations to sample from
	 * @return
	 */
	private static StudySet generateStudySet(Random rnd,
			AssociationContainer assoc, GOGraph graph,
			GOTermEnumerator completePopEnumerator,
			ByteString[] allGenesArray, StudySetSampler sampler,
			ArrayList<TermID> termCombi)
	{
		StudySet newStudySet;
		/* Original variant */
		if (ORIGINAL_SAMPLING)
		{
			PercentageEnrichmentRule rule = new PercentageEnrichmentRule();
			rule.setNoisePercentage(NOISE_PERCENTAGE);
			for (TermID t : termCombi)
				rule.addTerm(t, TERM_PERCENTAGE);

			newStudySet = sampler.sampleRandomStudySet(graph, assoc, rule, true);
//								System.out.println("Studyset with ");
//								for (TermID tid : termCombi)
//									System.out.println("   " + df.graph.getGOTerm(tid).getName() + "  " + tid.toString());
		} else
		{
			newStudySet = new StudySet("study");
			
			for (TermID t : termCombi)
			{
				for (ByteString g : completePopEnumerator.getAnnotatedGenes(t).totalAnnotated)
					newStudySet.addGene(g, "");
			}
			newStudySet.filterOutDuplicateGenes(assoc);

			int tp = newStudySet.getGeneCount();
			int tn = allGenesArray.length - tp;

			/* Obfuscate the study set, i.e., create the observed state */
			
			/* false -> true (alpha, false positive) */
			HashSet<ByteString>  fp = new HashSet<ByteString>();
			for (ByteString gene : allGenesArray)
			{
				if (newStudySet.contains(gene)) continue;
				if (rnd.nextDouble() < ALPHA) fp.add(gene);
			}

			/* true -> false (beta, false negative) */
			HashSet<ByteString>  fn = new HashSet<ByteString>();
			for (ByteString gene : newStudySet)
			{
				if (rnd.nextDouble() < BETA) fn.add(gene);	
			}

			newStudySet.addGenes(fp);
			newStudySet.removeGenes(fn);

			System.out.println("Number of genes in study set " + newStudySet.getGeneCount() + " " + termCombi.size() + " terms enriched");
			System.out.println("Study set has " + fp.size() + " false positives (alpha=" + ((double)fp.size())/tn +")");
			System.out.println("Study set misses " + fn.size() + " genes (beta=" + ((double)fn.size())/tp +")");
		}
		return newStudySet;
	}
}
