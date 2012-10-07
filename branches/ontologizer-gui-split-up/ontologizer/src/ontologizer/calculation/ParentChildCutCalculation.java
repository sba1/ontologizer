package ontologizer.calculation;

import java.util.HashSet;
import java.util.Set;

import ontologizer.association.AssociationContainer;
import ontologizer.enumeration.GOTermEnumerator;
import ontologizer.go.Ontology;
import ontologizer.go.TermID;
import ontologizer.set.PopulationSet;
import ontologizer.set.StudySet;
import ontologizer.statistics.AbstractTestCorrection;
import ontologizer.statistics.IPValueCalculation;
import ontologizer.statistics.PValue;
import ontologizer.types.ByteString;

public class ParentChildCutCalculation extends
		AbstractHypergeometricCalculation
{

	public String getName()
	{
		return "Parent-Child-Intersection";
	}

	public String getDescription()
	{
		return "We calculate p-values measuring over-representation"
				+ "of GO term annotated genes in a study set by comparing"
				+ "a term's annotation to the annotation of its parent terms."
				+ "This is a second version which uses cuts instead of unions"
				+ "of gene sets in case that there exist multiple parents";
	}

	public EnrichedGOTermsResult calculateStudySet(Ontology graph,
			AssociationContainer goAssociations, PopulationSet popSet,
			StudySet studySet, AbstractTestCorrection testCorrection)
	{

		EnrichedGOTermsResult studySetResult = new EnrichedGOTermsResult(graph, goAssociations, studySet, popSet.getGeneCount());
		studySetResult.setCalculationName(this.getName());
		studySetResult.setCorrectionName(testCorrection.getName());

		/**
		 * 
		 * This class hides all the details about how the p values are
		 * calculated from the multiple test correction.
		 * 
		 * @author Sebastian Bauer
		 * 
		 */
		class ParentChildPValuesCalculation implements IPValueCalculation
		{
			/*
			 * We basically have the arguments of calculateStudy as fields
			 */
			public Ontology graph;

			public AssociationContainer goAssociations;

			public PopulationSet populationSet;

			public GOTermEnumerator popTermEnumerator;

			public StudySet observedStudySet;

			private PValue[] calculatePValues(StudySet studySet)
			{
				/* We need this to get genes annotated in the study set */
				GOTermEnumerator studyTermEnumerator = studySet.enumerateGOTerms(
						graph, goAssociations);

				// PValue p [] = new
				// PValue[populationTermCounter.getTotalNumberOfAnnotatedTerms()];
				PValue p[] = new PValue[popTermEnumerator.getTotalNumberOfAnnotatedTerms()];
				int i = 0;

				/* For every term within the goTermCounter */
				for (TermID term : popTermEnumerator)
				{
					// calculating properties of term
					ParentChildGOTermProperties termProp = calculateTerm(term,
							graph, popTermEnumerator, studyTermEnumerator);

					// adding properties to p Vector
					p[i++] = termProp;
				}

				return p;
			}

			public int currentStudySetSize()
			{
				return observedStudySet.getGeneCount();
			}

			public PValue[] calculateRawPValues()
			{
				return calculatePValues(observedStudySet);
			}

			public PValue[] calculateRandomPValues()
			{
				return calculatePValues(populationSet.generateRandomStudySet(observedStudySet.getGeneCount()));
			}

			private ParentChildGOTermProperties calculateTerm(TermID term,
					Ontology graph, GOTermEnumerator popTermEnumerator,
					GOTermEnumerator studyTermEnumerator)
			{
				// counts annotated to term
				int studyTermCount = studyTermEnumerator.getAnnotatedGenes(term).totalAnnotatedCount();
				int popTermCount = popTermEnumerator.getAnnotatedGenes(term).totalAnnotatedCount();

				// this is what we give back
				ParentChildGOTermProperties prop = new ParentChildGOTermProperties();
				prop.goTerm = graph.getTerm(term);
				prop.annotatedPopulationGenes = popTermCount;
				prop.annotatedStudyGenes = studyTermCount;

				if (graph.isRootTerm(term))
				{
					prop.nparents = 0;
					prop.ignoreAtMTC = true;
					prop.p = 1.0;
					prop.p_adjusted = 1.0;
					prop.p_min = 1.0;
				} else
				{
					// getting parents
					Set<TermID> parents = graph.getTermParents(term);

					// These will hold the names of all genes annotated to all
					// parents (intersection)
					HashSet<ByteString> popParentCutGenes = new HashSet<ByteString>();
					HashSet<ByteString> studyParentCutGenes = new HashSet<ByteString>();

					boolean initialized = false;
					// looping over all parents to 
					for (TermID parent : parents)
					{
						if (!initialized)  // we record all genes annotated to the first parent
						{
							popParentCutGenes.addAll(popTermEnumerator.getAnnotatedGenes(parent).totalAnnotated);
							studyParentCutGenes.addAll(studyTermEnumerator.getAnnotatedGenes(parent).totalAnnotated);
							initialized = true;
						} else // we delete those which are not amongst the genes of the actual parent
						{
							// Important: The conversion into a HashSet speeds up the performance of 'retainAll' tremendously!
							// The reason is that the 'contains' method has linear time for LinkedList as given back by 'totalAnnotated'.
							HashSet<ByteString> popParentNext = new HashSet<ByteString>();
							popParentNext.addAll(popTermEnumerator.getAnnotatedGenes(parent).totalAnnotated);
							popParentCutGenes.retainAll(popParentNext);
							
							HashSet<ByteString> studyParentNext = new HashSet<ByteString>();
							studyParentNext.addAll(studyTermEnumerator.getAnnotatedGenes(parent).totalAnnotated);
							studyParentCutGenes.retainAll(studyParentNext);
						}
					}

					// number of genes annotated to family (term and parents)
					int popFamilyCount = popParentCutGenes.size();
					int studyFamilyCount = studyParentCutGenes.size();

					prop.popFamilyGenes = popFamilyCount;
					prop.studyFamilyGenes = studyFamilyCount;
					prop.nparents = parents.size();

					if (studyTermCount != 0)
					{
						if (popFamilyCount == popTermCount)
						{
							prop.ignoreAtMTC = true;
							prop.p = 1.0;
							prop.p_adjusted = 1.0;
							prop.p_min = 1.0;
						} else
						{
							double p = hyperg.phypergeometric(popFamilyCount,
									(double) popTermCount
											/ (double) popFamilyCount,
									studyFamilyCount, studyTermCount);

							prop.ignoreAtMTC = false;
							prop.p = p;
							prop.p_min = hyperg.dhyper(popTermCount,
									popFamilyCount, popTermCount, popTermCount);
						}
					} else
					{
						prop.ignoreAtMTC = true;
						prop.p = 1.0;
						prop.p_adjusted = 1.0;
						prop.p_min = 1.0;
					}
				}

				return prop;
			}
		}
		;

		ParentChildPValuesCalculation pValueCalculation = new ParentChildPValuesCalculation();
		pValueCalculation.goAssociations = goAssociations;
		pValueCalculation.graph = graph;
		pValueCalculation.populationSet = popSet;
		pValueCalculation.popTermEnumerator = popSet.enumerateGOTerms(graph,
				goAssociations);
		pValueCalculation.observedStudySet = studySet;
		PValue p[] = testCorrection.adjustPValues(pValueCalculation);

		/*
		 * Add the results to the result list and filter out terms with no
		 * annotated genes.
		 */
		for (int i = 0; i < p.length; i++)
		{
			/* Entries are SingleGOTermProperties */
			ParentChildGOTermProperties prop = (ParentChildGOTermProperties) p[i];

			/* Within the result ignore terms without any annotation */
			if (prop.annotatedStudyGenes == 0)
				continue;

			studySetResult.addGOTermProperties(prop);
		}

		return studySetResult;

	}
	
	public boolean supportsTestCorrection() {
		return true;
	}


}
