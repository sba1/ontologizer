package ontologizer.gui.swt.threads;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import ontologizer.ByteString;
import ontologizer.GeneFilter;
import ontologizer.PopulationSet;
import ontologizer.StudySet;
import ontologizer.StudySetList;
import ontologizer.association.AssociationContainer;
import ontologizer.association.AssociationParser;
import ontologizer.association.IAssociationParserProgress;
import ontologizer.calculation.AbstractGOTermsResult;
import ontologizer.calculation.CalculationRegistry;
import ontologizer.calculation.EnrichedGOTermsResult;
import ontologizer.calculation.ICalculation;
import ontologizer.go.GOGraph;
import ontologizer.go.IOBOParserProgress;
import ontologizer.go.OBOParser;
import ontologizer.go.TermContainer;
import ontologizer.gui.swt.Ontologizer;
import ontologizer.gui.swt.ResultWindow;
import ontologizer.statistics.AbstractResamplingTestCorrection;
import ontologizer.statistics.AbstractTestCorrection;
import ontologizer.statistics.IResampling;
import ontologizer.statistics.IResamplingProgress;
import ontologizer.statistics.TestCorrectionRegistry;

import org.eclipse.swt.widgets.Display;

public class AnalyseThread extends AbstractOntologizerThread
{
	private String definitionFile;
	private String associationsFile;
	private String mappingFile;
	private String methodName;
	private String mtcName;
	private PopulationSet populationSet;
	private StudySetList studySetList;
	private int numberOfPermutations;

	public AnalyseThread(Display display, Runnable calledWhenFinished, ResultWindow result, String definitionFile, String associationsFile, String mappingFile, PopulationSet populationSet, StudySetList studySetList, String methodName, String mtcName, int noP)
	{
		super("Analyze Thread",calledWhenFinished,display,result);

		this.definitionFile = definitionFile;
		this.associationsFile = associationsFile;
		this.mappingFile = mappingFile;
		this.populationSet = populationSet;
		this.studySetList = studySetList;
		this.methodName = methodName;
		this.mtcName = mtcName;
		this.numberOfPermutations = noP;

		setPriority(Thread.MIN_PRIORITY);
	}

	public void perform()
	{
		try
		{
			/**
			 * Basic runnable which appends a given text to the result window.
			 *
			 * @author Sebastian Bauer
			 *
			 */
			class ResultAppendLogRunnable implements Runnable
			{
				String log;
				ResultAppendLogRunnable(String log){this.log = log;}
				public void run() { result.appendLog(log); }
			}

			/**
			 * Runnable to be used for adding a new result into the result window
			 *
			 * @author Sebastian Bauer
			 */
			class AddResultRunnable implements Runnable
			{
				private AbstractGOTermsResult theResult;

				public AddResultRunnable(AbstractGOTermsResult result)
				{
					this.theResult = result;
				}

				public void run()
				{
					if (!result.isDisposed())
					{
						result.addResults(theResult);
						result.updateProgress(0);
					}
				}
			};

			definitionFile = downloadFile(definitionFile,"Download OBO File");
			associationsFile = downloadFile(associationsFile,"Download Association File");

			/* Initial progress stuff */
			display.asyncExec(new Runnable(){public void run() {
//				main.disableAnalyseButton();
/* TODO: Add this to the proper location */
				if (!result.isDisposed())
				{
					result.appendLog("Parse OBO File");
					result.updateProgress(0);
					result.showProgressBar();
				}
			};});

			/* TODO: Merge or change to use OntologizerCore */

			ICalculation calculation = CalculationRegistry.getCalculationByName(methodName);
			if (calculation == null)
				calculation = CalculationRegistry.getDefault();

			/* Set the desired test correction or set the default */
			AbstractTestCorrection testCorrection = TestCorrectionRegistry.getCorrectionByName(mtcName);
			if (testCorrection == null)
				testCorrection = TestCorrectionRegistry.getDefault();

			if (testCorrection instanceof IResampling) {
				IResampling resampling = (IResampling) testCorrection;

				/* TODO: Probably, invalidating the cache doesn't make much sense here */

				resampling.resetCache();
				if (numberOfPermutations > 0) {
					resampling.setNumberOfResamplingSteps(numberOfPermutations);
				}
			}


			/* OBO */
			OBOParser oboParser = new OBOParser(definitionFile,OBOParser.PARSE_DEFINITIONS);
			String diag = oboParser.doParse(new IOBOParserProgress(){
				public void init(final int max)
				{
					display.asyncExec(new Runnable() {
						public void run()
						{
							if (!result.isDisposed())
								result.initProgress(max);
						}});
				}
				public void update(final int current, final int terms)
				{
					/* Abort condition */
					if (isInterrupted())
						throw new AbortCalculationException();

					display.asyncExec(new Runnable() {
						public void run()
						{
							if (!result.isDisposed())
							{
								result.updateProgress(current);
								result.appendLog("Parse OBO file ("+terms+" terms)");
							}
						}});
				}
			});
			display.asyncExec(new ResultAppendLogRunnable(diag));
			display.asyncExec(new ResultAppendLogRunnable("Building GO graph"));
			TermContainer goTerms = new TermContainer(oboParser.getTermMap(), oboParser.getFormatVersion(), oboParser.getDate());
			GOGraph goGraph = new GOGraph(goTerms);

			if (mappingFile != null)
			{
				GeneFilter filter = new GeneFilter(new File(mappingFile));

				populationSet.applyFilter(filter);

				for (StudySet studySet : studySetList)
					studySet.applyFilter(filter);
			}


			boolean popWasEmpty = populationSet.getGeneCount() == 0;

			/* Check now if all study genes are included within the population,
			 * if a particular gene is not contained, add it */
			for (ByteString geneName : studySetList.getGeneSet())
			{
				if (!populationSet.contains(geneName))
					populationSet.addGene(geneName,"");
			}

			/* Parse the GO association file containing GO annotations for genes or gene
			 * products. Results are placed in association parser.
			 */
			display.asyncExec(new ResultAppendLogRunnable("Parse associations"));
			AssociationParser ap = new AssociationParser(associationsFile,goTerms,populationSet.getAllGeneNames(), new IAssociationParserProgress()
			{
				public void init(final int max)
				{
					display.asyncExec(new Runnable() {
						public void run()
						{
							if (!result.isDisposed())
								result.initProgress(max);
						}});
				}

				public void update(final int current)
				{
					/* Abort condition */
					if (isInterrupted())
						throw new AbortCalculationException();

					display.asyncExec(new Runnable() {
						public void run()
						{
							if (!result.isDisposed())
								result.updateProgress(current);
						}});
				}
			});
			AssociationContainer goAssociations = new AssociationContainer(ap.getAssociations(), ap.getSynonym2gene(), ap.getDbObject2gene());

			if (popWasEmpty)
			{
				/* If population set was empty we add all genes whose associations
				 * are know to the poupulation set. */
				List<ByteString> l = ap.getListOfObjectSymbols();
				for (ByteString bs : l)
					populationSet.addGene(bs,"");
			}

			/* Filter out duplicate genes (i.e. different gene names refering
			 * to the same gene) */
			display.asyncExec(new Runnable(){public void run() {
				result.appendLog("Filter out duplicate genes");
			}});
			populationSet.filterOutDuplicateGenes(goAssociations);
			for (StudySet study : studySetList)
				study.filterOutDuplicateGenes(goAssociations);

			/* Filter out genes within the study without any annotations */
			display.asyncExec(new Runnable(){public void run() {
				result.appendLog("Removing unannotated genes");
			}});
			for (StudySet study : studySetList)
				study.filterOutAssociationlessGenes(goAssociations);
			/* Filter out genes within the population which doesn't have an annotation */
			populationSet.filterOutAssociationlessGenes(goAssociations);

			/* Reset progress bar */
			display.asyncExec(new Runnable(){public void run() {
				result.updateProgress(0);
			}});

			/* Perform calculation */
			int studyNum = 0;
			ArrayList<EnrichedGOTermsResult> studySetResultList = new ArrayList<EnrichedGOTermsResult>();

			for (StudySet studySet : studySetList)
			{
				/* Abort condition */
				if (isInterrupted())
					throw new AbortCalculationException();

				studyNum++;

				/* If procedure is a resampling test
				 * TODO: Enclose testCorrection by synchonize statement */
				if (testCorrection instanceof AbstractResamplingTestCorrection)
				{
					AbstractResamplingTestCorrection rtc = (AbstractResamplingTestCorrection)testCorrection;
					final AnalyseThread t = this;

					rtc.setProgressUpdate(new IResamplingProgress(){
						public void init(final int max)
						{
							display.asyncExec(new Runnable(){public void run() { if (!result.isDisposed()) result.initProgress(max); }});
						}

						public void update(final int current)
						{
							/* If thread is interrupted throw a Runtime Exception */
							if (t.isInterrupted()) throw new AbortCalculationException();

							display.asyncExec(new Runnable(){public void run() { if (!result.isDisposed()) result.updateProgress(current); }});
						}
					});
				}

				display.asyncExec(new ResultAppendLogRunnable("Perform analysis on study set " + studyNum + " (out of " + studySetList.size() + ")"));

				EnrichedGOTermsResult studySetResult = calculation.calculateStudySet(
						goGraph, goAssociations, populationSet, studySet,
						testCorrection);

				/* Reset the counter and enumerator items here. It is not necessarily
				 * nice to place it here, but for the moment it's the easiest way
				 */
				studySet.resetCounterAndEnumerator();

				if (testCorrection instanceof AbstractResamplingTestCorrection)
				{
					AbstractResamplingTestCorrection rtc = (AbstractResamplingTestCorrection)testCorrection;
					rtc.setProgressUpdate(null);
				}

				display.asyncExec(new AddResultRunnable(studySetResult));

				studySetResultList.add(studySetResult);
			}

			/* Eigen stuff */
/*			try
			{
				SVDResult svdResult = SVD.doSVD(goTerms, goGraph, studySetResultList, populationSet, false, false);
				display.asyncExec(new AddResultRunnable(svdResult));

				svdResult = SVD.doSVD(goTerms, goGraph, studySetResultList, populationSet, true, false);
				display.asyncExec(new AddResultRunnable(svdResult));

				svdResult = SVD.doSVD(goTerms, goGraph, studySetResultList, populationSet, false, true);
				display.asyncExec(new AddResultRunnable(svdResult));

				svdResult = SVD.doSVD(goTerms, goGraph, studySetResultList, populationSet, true, true);
				display.asyncExec(new AddResultRunnable(svdResult));
			}
			catch (final Exception e)
			{
				display.syncExec(new Runnable()
				{
					public void run()
					{
						SWTUtil.displayException(main.getShell(), e, "Error while performing SVD. No results are displayed.\n");
					}
				});
			}
*/
			display.asyncExec(new Runnable(){public void run() {
				if (!result.isDisposed())
				{
					result.setBusyPointer(false);
					result.appendLog("Calculation finished");
					result.clearProgressText();
					result.hideProgressBar();
				}
			};});

		} catch (AbortCalculationException e)
		{
			/* Do nothing */
		} catch (InterruptedException e)
		{
			/* Do nothing */
		} catch (final Exception e)
		{
			if (!interrupted())
			{
				display.syncExec(new Runnable()
				{
					public void run()
					{
						result.dispose();
						Ontologizer.logException(e);
					}
				});
			}
		}
	}
};
