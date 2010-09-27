/*
 * Created on 19.02.2006
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ontologizer.calculation;

import java.util.HashSet;

import ontologizer.GOTermEnumerator;
import ontologizer.PopulationSet;
import ontologizer.StudySet;
import ontologizer.association.AssociationContainer;
import ontologizer.go.Ontology;
import ontologizer.go.Term;
import ontologizer.go.TermID;
import ontologizer.statistics.AbstractTestCorrection;
import ontologizer.statistics.IPValueCalculation;
import ontologizer.statistics.PValue;
import ontologizer.types.ByteString;

public class NewApproachCalculation extends AbstractHypergeometricCalculation
{
	public String getName()
	{
		return "New approach";
	}

	public String getDescription()
	{
		return "No description yet";
	}

	/**
	 * Start calculation based on fisher exact test of the given study.
	 * @param graph
	 * @param goAssociations
	 * @param populationSet
	 * @param studySet
	 * 
	 * @return
	 */
	public EnrichedGOTermsResult calculateStudySet(
			Ontology graph,
			AssociationContainer goAssociations,
			PopulationSet populationSet,
			StudySet studySet,
			AbstractTestCorrection testCorrection)
	{
		HashSet<Term> fixedGOTerms = new HashSet<Term>();
		EnrichedGOTermsResult studySetResult = new EnrichedGOTermsResult(graph, goAssociations, studySet, populationSet.getGeneCount());
		studySetResult.setCalculationName(this.getName());
		studySetResult.setCorrectionName(testCorrection.getName());

		/**
		 * 
		 * This class hides all the details about how the p values are calculated
		 * from the multiple test correction.
		 * 
		 * @author Sebastian Bauer
		 *
		 */
		class SinglePValuesCalculation implements IPValueCalculation
		{
			public PopulationSet populationSet;
			public StudySet observedStudySet;
			public AssociationContainer goAssociations;
			public Ontology graph;
			public HashSet<Term> fixedGOTerms;

			private PValue [] calculatePValues(StudySet studySet)
			{
				GOTermEnumerator studyTermEnumerator = studySet.enumerateGOTerms(graph,goAssociations);
				GOTermEnumerator populationTermEnumerator = populationSet.enumerateGOTerms(graph,goAssociations);
				int i = 0;

				/* Determine which genes are annotated to the fixed terms within study and population, respectively */
				HashSet<ByteString> populationGenesOfFixedTerms = new HashSet<ByteString>();
				HashSet<ByteString> studyGenesOfFixedTerms = new HashSet<ByteString>();

				for (Term term : fixedGOTerms)
				{
					System.out.println("Number of population genes convered by " + term.getIDAsString() + " " + populationTermEnumerator.getAnnotatedGenes(term.getID()).totalAnnotated.size());
					System.out.println("Number of study genes convered by " + term.getIDAsString() + " " + studyTermEnumerator.getAnnotatedGenes(term.getID()).totalAnnotated.size());

					populationGenesOfFixedTerms.addAll(populationTermEnumerator.getAnnotatedGenes(term.getID()).totalAnnotated);
					studyGenesOfFixedTerms.addAll(studyTermEnumerator.getAnnotatedGenes(term.getID()).totalAnnotated);
				}
				
				System.out.println("Number of population genes covered by " + fixedGOTerms.size() + " fixed terms: " +  populationGenesOfFixedTerms.size());
				System.out.println("Number of study genes covered by " + fixedGOTerms.size() + " fixed terms: " + studyGenesOfFixedTerms.size());
				
				PValue p [] = new PValue[populationTermEnumerator.getTotalNumberOfAnnotatedTerms()];
				TermForTermGOTermProperties myP;

				for (TermID term : populationTermEnumerator)
				{
					int popGeneCount = populationSet.getGeneCount();
					int studyGeneCount = studySet.getGeneCount();
					int populationTermAnnotatedGeneCount = populationTermEnumerator.getAnnotatedGenes(term).totalAnnotatedCount();
					int studyTermAnnotatedGeneCount = studyTermEnumerator.getAnnotatedGenes(term).totalAnnotatedCount();

					// *** For debugging purposes
					if (term.toString().equals("GO:0006807"))
						System.out.println("***");
					if (term.toString().equals("GO:0008652"))
						System.out.println("****");

					HashSet<ByteString> populationGenesOfTermAndFixedTerms = new HashSet<ByteString>();
					HashSet<ByteString> studyGenesOfTermAndFixedTerms = new HashSet<ByteString>();

					for (ByteString gene : populationTermEnumerator.getAnnotatedGenes(term).totalAnnotated)
					{
						if (populationGenesOfFixedTerms.contains(gene))
							populationGenesOfTermAndFixedTerms.add(gene);
					}

					for (ByteString gene : studyTermEnumerator.getAnnotatedGenes(term).totalAnnotated)
					{
						if (studyGenesOfFixedTerms.contains(gene))
							studyGenesOfTermAndFixedTerms.add(gene);
					}
					
					myP = new TermForTermGOTermProperties();
					myP.goTerm = graph.getTerm(term);
					myP.annotatedStudyGenes = studyTermAnnotatedGeneCount;
					myP.annotatedPopulationGenes = populationTermAnnotatedGeneCount;
					
					if (studyTermAnnotatedGeneCount != 0)
					{
						double pRef;
						double p1; /* this should match pRef, if no term is fixed */

						pRef = 0.0;
						p1 = 0.0;

						System.out.println(studyTermAnnotatedGeneCount + " " +
								           popGeneCount + " " +
								           populationTermAnnotatedGeneCount + " " +
								           studyGeneCount + "    " +

								           populationGenesOfFixedTerms.size() + " " +
								           populationGenesOfTermAndFixedTerms.size() + " " +
								           studyGenesOfTermAndFixedTerms.size() + "    " +

								           (popGeneCount - populationGenesOfFixedTerms.size()) + " " + 
								          (populationTermAnnotatedGeneCount - populationGenesOfTermAndFixedTerms.size()) + " " +
										  (studyGeneCount - studyGenesOfTermAndFixedTerms.size()));

						
						for (int j = studyTermAnnotatedGeneCount; j <= Math.min(populationTermAnnotatedGeneCount, studyGeneCount); j++)
						{
//							System.out.println(j + "  " + popGeneCount + "  " + populationTermAnnotatedGeneCount + "  " + studyGeneCount);
							
							for (int k = 0; k <= j; k++)
							{
								double a,b;

/*								if (hyperg.dhyper(  k,
							            populationGenesOfFixedTerms.size(),
							            populationGenesOfTermAndFixedTerms.size(), 
							            studyGenesOfTermAndFixedTerms.size()) != 0.0)
								{
								System.out.println(  k + " " + 
							            populationGenesOfFixedTerms.size() + "  " +
							            populationGenesOfTermAndFixedTerms.size() + " " +
							            studyGenesOfTermAndFixedTerms.size() + "    " +
							            (j-k) + " :" + hyperg.dhyper(  k,
									            populationGenesOfFixedTerms.size(),
									            populationGenesOfTermAndFixedTerms.size(), 
									            studyGenesOfTermAndFixedTerms.size()) + "\t" + (j-k) + " "  + 
					    		        (popGeneCount - populationGenesOfFixedTerms.size()) + " " +
					    		        populationGenesOfTermButNotFixedTermsCount + " " +
					    		        (studyGeneCount - studyGenesOfTermAndFixedTerms.size()) + " :" +
					    		        hyperg.dhyper(j-k,
							    		        popGeneCount - populationGenesOfFixedTerms.size(),
							    		        populationGenesOfTermButNotFixedTermsCount,
							    		        studyGeneCount - studyGenesOfTermAndFixedTerms.size()));
								}*/

								a = hyperg.dhyper(  k,
							            populationGenesOfFixedTerms.size(),
							            populationGenesOfTermAndFixedTerms.size(), 
							            studyGenesOfTermAndFixedTerms.size());

								b = hyperg.dhyper(j-k,
					    		        popGeneCount - populationGenesOfFixedTerms.size(),
					    		        populationTermAnnotatedGeneCount - populationGenesOfTermAndFixedTerms.size(),
					    		        studyGeneCount - studyGenesOfTermAndFixedTerms.size());

								
								if (term.toString().equals("GO:0008652"))
								{
									System.out.printf("k=%d %e j-k=%d %e sum=%e\n",k,a,j-k,b,(a*b));
								}

								p1 +=  a * b;
								    
							}

//							System.out.println(hyperg.dhyper(j,popGeneCount,populationTermAnnotatedGeneCount,studyGeneCount) + " " + o);
							pRef += hyperg.dhyper(j,popGeneCount,populationTermAnnotatedGeneCount,studyGeneCount);
						}

						System.out.printf("%s pOld=%e pRef=%e p1=%e\n",term.toString(),hyperg.phypergeometric(popGeneCount, (double)populationTermAnnotatedGeneCount / (double)popGeneCount,
								studyGeneCount, studyTermAnnotatedGeneCount), pRef, p1);

						myP.p = p1;
						myP.p_min = hyperg.dhyper(
								populationTermAnnotatedGeneCount,
								popGeneCount,
								populationTermAnnotatedGeneCount,
								populationTermAnnotatedGeneCount);
					} else
					{
						/* Mark this p value as irrelevant so it isn't considered in a mtc */
						myP.p = 1.0;
						myP.ignoreAtMTC = true;
						myP.p_min = 1.0;
					}

					p[i++] = myP;
				}
				return p;
			}
			
			public PValue[] calculateRawPValues()
			{
				return calculatePValues(observedStudySet);
			}

			public int currentStudySetSize()
			{
				return observedStudySet.getGeneCount();
			}

			public PValue[] calculateRandomPValues()
			{
				return calculatePValues(populationSet.generateRandomStudySet(observedStudySet.getGeneCount()));
			}
		};

		// *** For debugging purposes
		fixedGOTerms.add(graph.getTerm(new TermID(6807)));
		fixedGOTerms.add(graph.getTerm(new TermID(8652)));

		for (int k = 0; k < 1; k++)
		{
			SinglePValuesCalculation pValueCalculation = new SinglePValuesCalculation();
			pValueCalculation.goAssociations = goAssociations;
			pValueCalculation.graph = graph;
			pValueCalculation.populationSet = populationSet;
			pValueCalculation.observedStudySet = studySet;
			pValueCalculation.fixedGOTerms = fixedGOTerms;
			PValue p[] = testCorrection.adjustPValues(pValueCalculation);

			PValue p_min = p[0];

			/* Find out a minimal P-Value (do not confuse with p_min of a term!) */
			for (int i=1;i<p.length;i++)
			{
				if (p[i].p_adjusted < p_min.p_adjusted)
					p_min = p[i];
			}

			/* Add the term of the minimal P-Value to the fixed set of GO-terms */
			fixedGOTerms.add(((TermForTermGOTermProperties)p_min).goTerm);
			
			System.out.println("Term "
					+ ((TermForTermGOTermProperties) p_min).goTerm.getIDAsString()
					+ " ("
					+ ((TermForTermGOTermProperties) p_min).goTerm.getName()
					+ ") was minimal");
		}

		
		/* Add the results to the result list and filter out terms
		 * with no annotated genes.
		 */
//		for (int i=0;i<p.length;i++)
//		{
//			/* Entries are SingleGOTermProperties */
//			TermForTermGOTermProperties prop = TermForTermGOTermProperties)p[i];
//
//			/* Within the result ignore terms without any annotation */
//			if (prop.annotatedStudyGenes == 0)
//				continue;
//
//			studySetResult.addGOTermProperties(prop);
//		}

		return studySetResult;
	}
	public boolean supportsTestCorrection() {
		return true;
	}
}
