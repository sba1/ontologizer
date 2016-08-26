package ontologizer.calculation;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.junit.Test;

import ontologizer.FileCache;
import ontologizer.OntologizerThreadGroups;
import ontologizer.association.AssociationContainer;
import ontologizer.calculation.b2g.B2GParam;
import ontologizer.calculation.b2g.Bayes2GOCalculation;
import ontologizer.calculation.b2g.Bayes2GOEnrichedGOTermsResult;
import ontologizer.dotwriter.AbstractDotAttributesProvider;
import ontologizer.dotwriter.GODOTWriter;
import ontologizer.enumeration.TermEnumerator;
import ontologizer.internal.InternalOntology;
import ontologizer.ontology.Ontology;
import ontologizer.ontology.Term;
import ontologizer.ontology.TermID;
import ontologizer.parser.ValuedItemAttribute;
import ontologizer.set.PopulationSet;
import ontologizer.set.StudySet;
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
 * The testing class
 *
 * @author Sebastian Bauer
 *
 */
public class Bayes2GOCalculationTest
{
	@Test
	public void testBayes2GOSimple()
	{
		InternalOntology internalOntology = new InternalOntology();

		final HashMap<TermID,Double> wantedActiveTerms = new HashMap<TermID,Double>(); /* Terms that are active */
		wantedActiveTerms.put(new TermID("GO:0000010"),0.10);
		wantedActiveTerms.put(new TermID("GO:0000004"),0.10);

		AssociationContainer assoc = internalOntology.assoc;
		Ontology ontology = internalOntology.graph;

		SingleCalculationSetting scs = SingleCalculationSetting.create(new Random(1), wantedActiveTerms, 0.25, ontology, assoc);

		Bayes2GOCalculation calc = new Bayes2GOCalculation();
		calc.setSeed(2);
		calc.setMcmcSteps(520000);
		calc.setAlpha(B2GParam.Type.MCMC);
		calc.setBeta(B2GParam.Type.MCMC);
		calc.setExpectedNumber(B2GParam.Type.MCMC);

		calc.calculateStudySet(ontology, assoc, scs.pop, scs.study, new None());
	}

	@Test
	public void testBayes2GOParameterIntegratedOut()
	{
		InternalOntology internalOntology = new InternalOntology();

		final HashMap<TermID,Double> wantedActiveTerms = new HashMap<TermID,Double>(); /* Terms that are active */
		wantedActiveTerms.put(new TermID("GO:0000010"),0.10);
		wantedActiveTerms.put(new TermID("GO:0000004"),0.10);

		AssociationContainer assoc = internalOntology.assoc;
		Ontology ontology = internalOntology.graph;

		SingleCalculationSetting scs = SingleCalculationSetting.create(new Random(1), wantedActiveTerms, 0.25, ontology, assoc);

		Bayes2GOCalculation calc = new Bayes2GOCalculation();
		calc.setSeed(2);
		calc.setMcmcSteps(520000);
		calc.setIntegrateParams(true);
		calc.setAlpha(B2GParam.Type.FIXED);
		calc.setBeta(B2GParam.Type.FIXED);
		calc.setExpectedNumber(2);

		calc.calculateStudySet(ontology, assoc, scs.pop, scs.study, new None());
	}

	@Test
	public void testValuedGOScore()
	{
		String [] terms = {"GO:0000010", "GO:0000004"};
		InternalOntology internalOntology = new InternalOntology();
		Random rnd = new Random(1);

		AssociationContainer assoc = internalOntology.assoc;
		Ontology ontology = internalOntology.graph;

		PopulationSet populationSet = new PopulationSet();
		populationSet.addGenes(assoc.getAllAnnotatedGenes());
		TermEnumerator populationEnumerator = populationSet.enumerateTerms(ontology, assoc);

		StudySet valuedStudySet = new StudySet();
		for (String t : terms)
		{
			for (ByteString g : populationEnumerator.getAnnotatedGenes(new TermID(t)).totalAnnotated)
			{
				ValuedItemAttribute via = new ValuedItemAttribute();
				via.description = "";
				via.setValue(rnd.nextDouble() * 0.1);
				valuedStudySet.addGene(g, via);
			}
		}
		Set<ByteString> tempGenes = valuedStudySet.getAllGeneNames();
		for (ByteString g : populationSet)
		{
			if (!tempGenes.contains(g))
			{
				ValuedItemAttribute via = new ValuedItemAttribute();
				via.description = "";
				via.setValue(rnd.nextDouble());
				valuedStudySet.addGene(g, via);
			}
		}

		Bayes2GOCalculation calc = new Bayes2GOCalculation();
		calc.setSeed(2);
		calc.setMcmcSteps(520000);

		calc.calculateStudySet(ontology, assoc, populationSet, valuedStudySet, new None());
	}

	public static Ontology graph;
	public static AssociationContainer assoc;

	/**
	 * The original test procedure
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
		final TermEnumerator allEnumerator = allGenes.enumerateTerms(graph, assoc);

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

			final TermEnumerator studySetEnumerator = newStudyGenes.enumerateTerms(graph, assoc);

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
			final TermEnumerator allEnumerator,
			final TermEnumerator studySetEnumerator,
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

//	static private void scoreDistribution(Bayes2GOCalculation calc, TermEnumerator allEnumerator, PopulationSet popSet, StudySet studySet)
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
}
