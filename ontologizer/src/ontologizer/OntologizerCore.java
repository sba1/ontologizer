package ontologizer;

import java.io.*;
import java.util.*;

import ontologizer.association.AssociationContainer;
import ontologizer.calculation.CalculationRegistry;
import ontologizer.calculation.EnrichedGOTermsResult;
import ontologizer.calculation.ICalculation;
import ontologizer.calculation.b2g.B2GParam;
import ontologizer.calculation.b2g.Bayes2GOCalculation;
import ontologizer.filter.GeneFilter;
import ontologizer.io.ParserFileInput;
import ontologizer.io.annotation.AssociationParser;
import ontologizer.io.annotation.IAssociationParserProgress;
import ontologizer.io.obo.OBOParser;
import ontologizer.io.obo.OBOParserException;
import ontologizer.ontology.Ontology;
import ontologizer.ontology.Term;
import ontologizer.ontology.TermContainer;
import ontologizer.set.PopulationSet;
import ontologizer.set.StudySet;
import ontologizer.set.StudySetFactory;
import ontologizer.set.StudySetList;
import ontologizer.statistics.AbstractTestCorrection;
import ontologizer.statistics.IResampling;
import ontologizer.statistics.TestCorrectionRegistry;
import ontologizer.types.ByteString;

/**
 * The OntologizerCore class controls parsing and output of Gene Ontology
 * cluster information. It forms a common interface between the main methods in
 * the class OntologizerCMD (Command line) and OntoFrame (GUI version) to the
 * rest of the program logic of the Ontologizer.
 *
 * @author Peter Robinson, Sebastian Bauer
 */
public class OntologizerCore
{
	/**
	 *
	 * @author Sebastian Bauer
	 *
	 * This class is indented to pass arguments to the OntologizerCore
	 * constructor.
	 */
	public static class Arguments
	{
		/** gene_ontology.obo file (and path) */
		public String goTermsOBOFile;

		/** An optional name of a subontology */
		public String subontology;

		/** gene_association.* file (and path) */
		public String associationFile;

		/** Name of the study file (or directory) */
		public String studySet;

		/** Only study input files with this suffix are considered */
		public String suffix;

		/** Name of the main output File */
		public String mainOutputName;

		/** Name of the calculation (e.g. singlestep) */
		public String calculationName;

		/** Name of the multiple test correction procedure */
		public String correctionName;

		/**
		 * Number of resampling steps in case of a resampling
		 * based mtc procedure
		 */
		public int resamplingSteps;

		/**
		 * Number of mcmc steps to be used when running a mcmc-based calculation.
		 */
		public int mcmcSteps;

		/**
		 * Tolerance in percent for Westfall-Young-Approximate
		 */
		public int sizeTolerance;

		/** Minimum number of association to be displayed */
		public int minAssociationCount;

		/** Specifies the population file containing the whole gene population, might be null */
		public String populationFile;

		/** Should genes be filtered out, from which there exists no annotation? */
		public boolean filterOutUnannotatedGenes;

		/** Specifies the filter file */
		public String filterFile;
	};

	/** Contains all available GOTerms */
	private TermContainer goTerms;

	/** The graph to the gene ontology */
	private Ontology goGraph;

	/** List of all studies being analyzed */
	private StudySetList studySetList;

	/** Results of the analyzed studies */
	private StudySetResultList studySetResultList;

	/** Collection of all valid annotations for current dataset */
	private AssociationContainer goAssociations;

	/** The kind of calculation which should be performed */
	private ICalculation calculation;

	/** The correction which should be used */
	private AbstractTestCorrection testCorrection;

	/** The population object. If this is set we do the hypergeometric
	 * analysis based on the scheme in GeneMerge. */
	private PopulationSet populationSet;

	/**
	 * Construct the object.
	 *
	 * @param args specifies the arguments
	 * @throws IOException if an i/o operation failed
	 * @throws FileNotFoundException if a file could not be found
	 * @throws OBOParserException on failure of the obo parser
	 */
	public OntologizerCore(Arguments args) throws FileNotFoundException, IOException, OBOParserException
	{
		/* Set the desired calculation method or the default */
		calculation = CalculationRegistry.getCalculationByName(args.calculationName);
		if (calculation == null)
			calculation = CalculationRegistry.getDefault();
		if (calculation instanceof Bayes2GOCalculation) {
			Bayes2GOCalculation b2g = (Bayes2GOCalculation) calculation;
			b2g.setAlpha(B2GParam.Type.MCMC);
			b2g.setBeta(B2GParam.Type.MCMC);
			b2g.setExpectedNumber(B2GParam.Type.MCMC);
			if (args.mcmcSteps >= 100000)
			{
				b2g.setMcmcSteps(args.mcmcSteps);
			}
		}

		/* Set the desired test correction or set the default */
		testCorrection = TestCorrectionRegistry.getCorrectionByName(args.correctionName);
		if (testCorrection == null)
			testCorrection = TestCorrectionRegistry.getDefault();
		/* Empty cache for resampling based MTCs and set number of sampling steps */
		if (testCorrection instanceof IResampling) {
			IResampling resampling = (IResampling) testCorrection;
			resampling.resetCache();
			if (args.resamplingSteps > 0) {
				resampling.setNumberOfResamplingSteps(args.resamplingSteps);

			}
			if (args.sizeTolerance > 0) {
				resampling.setSizeTolerance(args.sizeTolerance);
			}
		}

		/* Parse the gene_ontology.obo file to get information about all terms.
		 * Transfer the information to a TermContainer object.
		 */
		System.err.println("Parse obo file \"" + args.goTermsOBOFile + "\"");

		OBOParser oboParser = new OBOParser(new ParserFileInput(args.goTermsOBOFile));
		System.err.println(oboParser.doParse());
		goTerms = new TermContainer(oboParser.getTermMap(), oboParser.getFormatVersion(), oboParser.getDate());
		System.err.println("Building graph");
		goGraph = Ontology.create(goTerms);
		if (args.subontology != null)
		{
			Term t = null;

			try
			{
				t = goGraph.getTerm(args.subontology);
			} catch (IllegalArgumentException e)
			{
			}

			if (t == null)
			{
				/* Find by name, slow */
				ByteString name = new ByteString(args.subontology);
				for (int i = 0; i< goGraph.getTermMap().size(); i++)
				{
					Term t2 = goGraph.getTermMap().get(i);
					if (t2.getName().equals(name))
					{
						t = t2;
						break;
					}
				}
			}

			if (t != null)
			{
				goGraph.setRelevantSubontology(t.getName().toString());
			} else
			{
				System.err.println("The term \"" + args.subontology + "\" as specified in the -g option, was not found");
				System.exit(1);
			}
		}

		/* create the study list. A directory or a single file might be given */
		File studyFile = new File(args.studySet);
		if (studyFile.isDirectory())
		{
			studySetList = new StudySetList(args.studySet,args.suffix);
		} else
		{
			/* Create a study list with a dummy name and add the study manually */
			studySetList = new StudySetList("study");
			studySetList.addStudySet(StudySetFactory.createFromFile(studyFile, false));
		}

		/* create the population set TODO: Get rid of the casting */
		populationSet = (PopulationSet)StudySetFactory.createFromFile(new File(args.populationFile), true);

		/* Apply the optional gene name mapping given by the supplied filter file */
		if (args.filterFile != null)
		{
			System.err.println("Parsing filter \"" + args.filterFile + "\"");
			GeneFilter filter = new GeneFilter(new File(args.filterFile));

			System.err.println("Appling filter");
			populationSet.applyFilter(filter);

			for (StudySet studySet : studySetList)
				studySet.applyFilter(filter);
		}

		/* Check now if all study genes are included within the population,
		 * if a partiulcar gene is not contained, add it */
		for (ByteString geneName : studySetList.getGeneSet())
		{
			if (!populationSet.contains(geneName))
				populationSet.addGene(geneName,"");
		}

		/* Parse the GO association file containing GO annotations for genes or gene
		 * products. Results are placed in associationparser.
		 */
		AssociationParser ap = new AssociationParser(new ParserFileInput(args.associationFile),goTerms,populationSet.getAllGeneNames(),
				new IAssociationParserProgress() {
					private int max;
					private long startTime;

					public void init(int max)
					{
						this.max = max;
						this.startTime = System.currentTimeMillis();
					}

					public void update(int current)
					{
						long currentTime = System.currentTimeMillis();

						if (currentTime - startTime > 20000)
						{
							/* Show progress */
							System.err.print("\033[1A\033[K");
							System.err.println("Reading annotation file: " + String.format("%.1f%%",current / (double)max * 100));
						}
					}

					@Override
					public void warning(String message)
					{

					}

				});
		goAssociations = new AssociationContainer(ap.getAssociations(), ap.getAnnotationMapping());

		/* Filter out duplicate genes (i.e. different gene names refering
		 * to the same gene) */
		populationSet.filterOutDuplicateGenes(goAssociations);
		for (StudySet study : studySetList)
			study.filterOutDuplicateGenes(goAssociations);

		if (args.filterOutUnannotatedGenes)
		{
			/* Filter out genes within the study without any annotations */
			for (StudySet study : studySetList)
				study.filterOutAssociationlessGenes(goAssociations);

			/* Filter out genes within the population which doesn't have an annotation */
			populationSet.filterOutAssociationlessGenes(goAssociations);
		}
	}

	/**
	 * Returns the iterator over all results.
	 *
	 * @return the iterator.
	 */
	public Iterator<EnrichedGOTermsResult> studySetResultIterator()
	{
		/* Create a dummy list in case no results were available */
		if (studySetResultList == null)
			studySetResultList = new StudySetResultList();

		return studySetResultList.iterator();
	}

	/**
	 * Perform the statistical calculation using the given calculation
	 * procedure accumulation the results into the studySetResultList
	 */
	public void calculate()
	{
		assert(populationSet != null);
		studySetResultList = new StudySetResultList();

		for (StudySet studySet : studySetList)
		{
			studySetResultList.addStudySetResult(
					calculation.calculateStudySet(goGraph,goAssociations,populationSet,studySet,testCorrection)
					);

			/* Reset the counter and enumerator items here. It is not necessarily
			 * nice to place it here, but for the moment it's the easiest way
			 */
			studySet.resetCounterAndEnumerator();
		}
	}

	private Iterator<StudySet> studySetIter;

	/**
	 * Perform the statistical calculation of the next study. When called
	 * first, the first study is considered as the next study.
	 *
	 * @return the result of the calculation or null, if no more studies
	 *         are available.
	 */
	public EnrichedGOTermsResult calculateNextStudy()
	{
		assert(populationSet != null);
		if (studySetIter == null) studySetIter = studySetList.iterator();
		if (!studySetIter.hasNext())
		{
			return null;
		}

		StudySet studySet = studySetIter.next();
		EnrichedGOTermsResult studySetResult = calculation.calculateStudySet(goGraph,goAssociations,populationSet,studySet,testCorrection);

		/* Reset the counter and enumerator items here. It is not necessarily
		 * nice to place it here, but for the moment it's the easiest way
		 */
		studySet.resetCounterAndEnumerator();
		return studySetResult;
	}

	public AssociationContainer getGoAssociations()
	{
		return goAssociations;
	}

	public Ontology getGoGraph()
	{
		return goGraph;
	}

	public TermContainer getGoTerms()
	{
		return goTerms;
	}

	public PopulationSet getPopulationSet()
	{
		return populationSet;
	}

	public String getCalculationName()
	{
		return calculation.getName();
	}

	public String getTestCorrectionName()
	{
		return testCorrection.getName();
	}
}
