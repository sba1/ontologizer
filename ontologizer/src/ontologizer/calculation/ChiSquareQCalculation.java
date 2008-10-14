/*
 * Created on 03.09.2005
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ontologizer.calculation;

import java.util.Arrays;

import ontologizer.GOTermCounter;
import ontologizer.PopulationSet;
import ontologizer.StudySet;
import ontologizer.association.AssociationContainer;
import ontologizer.chisquare.ChiSquare;
import ontologizer.chisquare.ChiSquare2P;
import ontologizer.chisquare.ChiSquareIncompatibleCountsException;
import ontologizer.go.GOGraph;
import ontologizer.go.TermID;
import ontologizer.statistics.AbstractTestCorrection;

/**
 * See "Comparable Analysis of Gene Sets in Gene Ontology Space
 * under the Multiple Hypothesis Testing Framework"
 *
 * @author Sebastian Bauer
 *
 */
public class ChiSquareQCalculation implements ICalculation
{
	/** Specify the number of permutations. TODO: Make this user configurable */
	final int NUMBER_OF_PERMUTATIONS = 100;

	public String getName()
	{
		return "ChiSquareQ";
	}

	public String getDescription()
	{
		// TODO Auto-generated method stub
		return null;
	}

	public EnrichedGOTermsResult calculateStudySet(GOGraph graph,
			AssociationContainer goAssociations, PopulationSet populationSet,
			StudySet studySet, AbstractTestCorrection testCorrection)
	{
		int i = 0,b;

		EnrichedGOTermsResult studySetResult = new EnrichedGOTermsResult(graph, goAssociations, studySet, populationSet.getGeneCount());
		studySetResult.setCalculationName(this.getName());
		studySetResult.setCorrectionName(testCorrection.getName());

		GOTermCounter studyTermCounter = studySet.countGOTerms(graph,goAssociations);
		GOTermCounter populationTermCounter = populationSet.countGOTerms(graph,goAssociations);
		ChiSquareQGOTermProperties props[] = new ChiSquareQGOTermProperties[populationTermCounter.getTotalNumberOfAnnotatedTerms()];

		try
		{
			for (TermID term : populationTermCounter)
			{
				int goidAnnotatedPopGeneCount = populationTermCounter.getCount(term);
				int popGeneCount = populationSet.getGeneCount();
				int studyGeneCount = studySet.getGeneCount();
				int goidAnnotatedStudyGeneCount = studyTermCounter.getCount(term);

				/* Note that the paper from which the calculation is based on
				 * uses a different calculation model. We build the contigency
				 * table based on yes/no outcomes from two random variables
				 * whereby one variable says if the gene is in the studyset
				 * and the other if it is annotated. All the counts sum up
				 * to the genes within the population set.
				 *
				 * However the paper considers two lists of genes which may be
				 * distinct. Here the counts sum up to the number of genes
				 * from list 1 plus from the number of genes of list 2.
				 *
				 * TODO: Research and write up what changes this implies and
				 * the motivation might be. Compare the methods and if the other
				 * method makes sense lastly provide the user an options two choose
				 * between the two different models.
				 *
				 */

				ChiSquare chiSquare = new ChiSquare();
				chiSquare.addCounts(studyGeneCount,goidAnnotatedStudyGeneCount);
				chiSquare.addCounts(popGeneCount - studyGeneCount, goidAnnotatedPopGeneCount - goidAnnotatedStudyGeneCount);

				ChiSquareQGOTermProperties prop = new ChiSquareQGOTermProperties();
				prop.annotatedPopulationGenes = goidAnnotatedPopGeneCount;
				prop.annotatedStudyGenes = goidAnnotatedStudyGeneCount;
				prop.chisquare = chiSquare.chiSquare();
				if (Double.isNaN(prop.chisquare)) prop.chisquare = 0.0;
				prop.goTerm = graph.getGoTermContainer().get(term);
				if (chiSquare.df()>0)
				{
					prop.p = ChiSquare2P.pchi(prop.chisquare,chiSquare.df());
				} else prop.p = 1;
				props[i++] = prop;
			}

			/* Find out how many terms usually have a bigger statistics
			 * than a selected term
			 */
			for (b=0;b<NUMBER_OF_PERMUTATIONS;b++)
			{
				System.out.println((b+1) + " of " + NUMBER_OF_PERMUTATIONS);

				StudySet randomStudySet = populationSet.generateRandomStudySet(studySet.getGeneCount());
				GOTermCounter randomStudyTermCounter = randomStudySet.countGOTerms(graph,goAssociations);

				for (TermID term : populationTermCounter)
				{
					int goidAnnotatedPopGeneCount = populationTermCounter.getCount(term);
					int popGeneCount = populationSet.getGeneCount();
					int studyGeneCount = randomStudySet.getGeneCount();
					int goidAnnotatedStudyGeneCount = randomStudyTermCounter.getCount(term);

					ChiSquare chiSquare = new ChiSquare();
					chiSquare.addCounts(studyGeneCount, goidAnnotatedStudyGeneCount);
					chiSquare.addCounts(popGeneCount - studyGeneCount, goidAnnotatedPopGeneCount - goidAnnotatedStudyGeneCount);
					double chiSq = chiSquare.chiSquare();
					if (chiSq == Double.NaN) chiSq = 0.0;

					for (int k=0;k<props.length;k++)
					{
						if (chiSq >= props[k].chisquare) props[k].expectedHigherChisquaresCount++;
					}
				}
			}

			/* Sort the props */
			Arrays.sort(props);

			/* Calculate the q value for every term */
			for (int k=0;k<props.length;k++)
			{
				/* Find out number of terms with bigger stat then term k within the study set */
				int cnt = 0;
				for (int j=0;j<props.length;j++)
				{
					if (props[j].chisquare >= props[k].chisquare)
						cnt++;
				}
				if (cnt == 0) cnt = 1;

				props[k].expectedHigherChisquares = (double)props[k].expectedHigherChisquaresCount / (double)NUMBER_OF_PERMUTATIONS;
				props[k].observedHigherChisquares = cnt;
				props[k].p_adjusted = (double)props[k].expectedHigherChisquares / (double)cnt;
			}

			/* Enforce p adjusted value monotony */
			AbstractTestCorrection.enforcePValueMonotony(props);

			/* Add the props */
			for (int k=0;k<props.length;k++)
				studySetResult.addGOTermProperties(props[k]);
		} catch (ChiSquareIncompatibleCountsException e)
		{
			e.printStackTrace();
		}
		return studySetResult;
	}
}

/* In R use "dat2<-cbind(dat,p2=pchisq(dat$chisquare,1,lower.tail=FALSE))" to
 * verifiy the raw p values */

