/*
 * Created on 29.10.2005
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ontologizer.gui.swt;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import ontologizer.ByteString;
import ontologizer.FileCache;
import ontologizer.GeneFilter;
import ontologizer.OntologizerCore;
import ontologizer.PopulationSet;
import ontologizer.StudySet;
import ontologizer.StudySetList;
import ontologizer.FileCache.FileCacheUpdateCallback;
import ontologizer.FileCache.FileDownload;
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
import ontologizer.gui.swt.images.Images;
import ontologizer.gui.swt.support.SWTUtil;
import ontologizer.statistics.AbstractResamplingTestCorrection;
import ontologizer.statistics.AbstractTestCorrection;
import ontologizer.statistics.IResampling;
import ontologizer.statistics.IResamplingProgress;
import ontologizer.statistics.TestCorrectionRegistry;
import ontologizer.worksets.WorkSetList;

import org.eclipse.swt.graphics.DeviceData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import tools.Sleak;



class AbortCalculationException extends RuntimeException
{
	private static final long serialVersionUID = 1L;
}

class AnalyseThread extends Thread
{
	private Display display;
	private MainWindow main;
	private ResultWindow result;
	private String definitionFile;
	private String associationsFile;
	private String mappingFile;
	private String methodName;
	private String mtcName;
	private PopulationSet populationSet;
	private StudySetList studySetList;
	private int numberOfPermutations;

	public AnalyseThread(Display display, MainWindow main, ResultWindow result, String definitionFile, String associationsFile, String mappingFile, PopulationSet populationSet, StudySetList studySetList, String methodName, String mtcName, int noP)
	{
		super(Ontologizer.threadGroup,"Analyze Thread");

		this.display = display;
		this.main = main;
		this.definitionFile = definitionFile;
		this.associationsFile = associationsFile;
		this.mappingFile = mappingFile;
		this.populationSet = populationSet;
		this.studySetList = studySetList;
		this.result = result;
		this.methodName = methodName;
		this.mtcName = mtcName;
		this.numberOfPermutations = noP;

		setPriority(Thread.MIN_PRIORITY);
	}

	public void run()
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
								result.appendLog("Parse OBO File ("+terms+" terms)");
							}
						}});
				}
			});
			display.asyncExec(new ResultAppendLogRunnable(diag));
			display.asyncExec(new ResultAppendLogRunnable("Building GO Graph"));
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
			display.asyncExec(new ResultAppendLogRunnable("Parse Associations"));
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
		finally
		{
			display.syncExec(new Runnable(){ public void run()
			{
				if (!main.getShell().isDisposed())
					main.enableAnalyseButton();
			}});
		}
	}

	/**
	 * Downloads a file in a synchron manner.
	 *
	 * @param filename
	 * @param message defines the message sent to the result window.
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private String downloadFile(String filename, final String message) throws IOException, InterruptedException
	{
		String newPath = FileCache.getCachedFileNameBlocking(filename,
				new FileDownload()
		{
			private boolean messageSeen;

			public void initProgress(final int max)
			{
				if (!messageSeen)
				{
					display.asyncExec(new Runnable() {
						public void run()
						{
							if (!result.isDisposed())
							{
								result.appendLog(message);
								result.updateProgress(0);
								result.showProgressBar();
							}
						}});
					messageSeen = true;
				}

				if (max == -1) return;

				if (!result.isDisposed())
				{
					display.asyncExec(new Runnable() {
						public void run()
						{
							if (!result.isDisposed())
								result.initProgress(max);
						}});
				}
			}

			public void progress(final int current)
			{
				if (!result.isDisposed())
				{
					display.asyncExec(new Runnable() {
						public void run()
						{
							if (!result.isDisposed())
								result.updateProgress(current);
						}});

				}

			}

			public void ready(Exception ex, String name) { }
		});
		return newPath;
	}
};

/**
 *
 * This is the main class of the Ontologizer SWT Application
 *
 * @author Sebastian Bauer
 *
 */
public class Ontologizer
{
	private static Logger logger = Logger.getLogger(Ontologizer.class.getName());

	private static MainWindow main;
	private static PreferencesWindow prefs;
	private static AboutWindow about;
	private static HelpWindow help;
	private static LogWindow log;
	private static WorkSetWindow workSet;
	private static NewProjectWizard newProjectWizard;
	private static GraphWindow graph;
	private static LinkedList<ResultWindow> resultWindowList;
	private static File workspace;

	public static ThreadGroup threadGroup;

	private static WorkSetList workSetList = new WorkSetList();

	static
	{
		/* Default */
		workSetList.add("C. elegans","http://www.geneontology.org/ontology/gene_ontology_edit.obo","http://cvsweb.geneontology.org/cgi-bin/cvsweb.cgi/go/gene-associations/gene_association.wb.gz?rev=HEAD");
		workSetList.add("Fruit Fly","http://www.geneontology.org/ontology/gene_ontology_edit.obo","http://cvsweb.geneontology.org/cgi-bin/cvsweb.cgi/go/gene-associations/gene_association.fb.gz?rev=HEAD");
		workSetList.add("Mouse","http://www.geneontology.org/ontology/gene_ontology_edit.obo","http://cvsweb.geneontology.org/cgi-bin/cvsweb.cgi/go/gene-associations/gene_association.mgi.gz?rev=HEAD");
		workSetList.add("Human","http://www.geneontology.org/ontology/gene_ontology_edit.obo","http://cvsweb.geneontology.org/cgi-bin/cvsweb.cgi/go/gene-associations/gene_association.goa_human.gz?rev=HEAD");
		workSetList.add("Protein Data Bank","http://www.geneontology.org/ontology/gene_ontology_edit.obo","http://cvsweb.geneontology.org/cgi-bin/cvsweb.cgi/go/gene-associations/gene_association.goa_pdb.gz?rev=HEAD");
		workSetList.add("Rice", "http://www.geneontology.org/ontology/gene_ontology_edit.obo", "http://cvsweb.geneontology.org/cgi-bin/cvsweb.cgi/go/gene-associations/gene_association.gramene_oryza.gz?rev=HEAD");
//		workSetList.add("UniProt", "http://www.geneontology.org/ontology/gene_ontology_edit.obo", "http://cvsweb.geneontology.org/cgi-bin/cvsweb.cgi/go/gene-associations/gene_association.goa_uniprot.gz?rev=HEAD");
		workSetList.add("Yeast","http://www.geneontology.org/ontology/gene_ontology_edit.obo","http://cvsweb.geneontology.org/cgi-bin/cvsweb.cgi/go/gene-associations/gene_association.sgd.gz?rev=HEAD");
	}

	/**
	 * This is the action which is executed on a download click within
	 * the workset window.
	 */
	private static ISimpleAction downloadAction =
		new ISimpleAction()
		{
			public void act()
			{
				try
				{
					if (workSet.getSelectedAddress() == null) return;
					FileCache.open(workSet.getSelectedAddress());

				} catch (IOException e)
				{
					logException(e);
				}
			}
		};

	private static ISimpleAction invalidateAction =
		new ISimpleAction()
		{
			public void act()
			{
				if (workSet.getSelectedAddress() == null) return;
				FileCache.invalidate(workSet.getSelectedAddress());
				workSet.updateWorkSetList(workSetList);
			}
		};

	public static void main(String[] args)
	{
		boolean useSleak = false;
		String os = System.getProperty("os.name");
		if (os.contains("Linux"))
		{
			/* We are running on linux which requires the MOZILLA_FIVE_HOME
			 * variable set. If this is not done, we do it now.
			 */
			String mozHome = System.getenv("MOZILLA_FIVE_HOME");
			if (mozHome == null)
			{
				File xulRunnerDir = new File("/usr/lib/xulrunner");
				if (xulRunnerDir.exists()) env.SetEnv.setenv("MOZILLA_FIVE_HOME","/usr/lib/xulrunner");
				else env.SetEnv.setenv("MOZILLA_FIVE_HOME","/usr/lib/mozilla");
			}
		}

		/* Prepare threads */
		threadGroup = new ThreadGroup("Worker");

		/* Prepare logging */
		Logger rootLogger = Logger.getLogger("");
		rootLogger.addHandler(new Handler()
		{
			public void close() throws SecurityException { }
			public void flush() { }
			public void publish(LogRecord arg0)
			{
				log(arg0.getLevel().getName(),arg0.getMessage());
			}
		});
//		rootLogger.setLevel(Level.FINEST);

		/* Prepare the help system */
		File helpFolderFile = new File(System.getProperty("user.dir"),"src/ontologizer/help");//.getAbsolutePath();
		String helpFolder = "";

		try
		{
			if (!helpFolderFile.exists())
			{
				/* The help folder doesn't exists, so try to copy the files to a
				 * directory */

				/* TODO: Find out if we there is a possibility to list the files */
				File file = File.createTempFile("onto","");
				File imgDir = new File(file,"images");
				file.delete();
				file.mkdirs();
				imgDir.mkdirs();

				copyFileToTemp("help/1_overview.html",file.getCanonicalPath());
				copyFileToTemp("help/2_requirements.html",file.getCanonicalPath());
				copyFileToTemp("help/3_howto.html",file.getCanonicalPath());
				copyFileToTemp("help/4_results.html",file.getCanonicalPath());
				copyFileToTemp("help/5_tutorial.html",file.getCanonicalPath());

				copyFileToTemp("help/images/filesets.png",imgDir.getCanonicalPath());
				copyFileToTemp("help/images/main-with-project.png",imgDir.getCanonicalPath());
				copyFileToTemp("help/images/menu-preferences.png",imgDir.getCanonicalPath());
				copyFileToTemp("help/images/name-of-the-project.png",imgDir.getCanonicalPath());
				copyFileToTemp("help/images/new-project.png",imgDir.getCanonicalPath());
				copyFileToTemp("help/images/new-project-fileset.png",imgDir.getCanonicalPath());
				copyFileToTemp("help/images/population.png",imgDir.getCanonicalPath());
				copyFileToTemp("help/images/preferences.png",imgDir.getCanonicalPath());
				copyFileToTemp("help/images/result-graph.png",imgDir.getCanonicalPath());
				copyFileToTemp("help/images/result-initial.png",imgDir.getCanonicalPath());
				copyFileToTemp("help/images/study.png",imgDir.getCanonicalPath());

				helpFolder = file.getAbsolutePath();

			} else
			{
				helpFolder = helpFolderFile.getCanonicalPath();
			}
		}
		catch (IOException e) { }

		DeviceData data = new DeviceData();
		data.tracking = useSleak;
		Display display = new Display (data);
		Images.setDisplay(display);

		Sleak sleak = null;
		if (useSleak)
		{
			sleak = new Sleak();
			sleak.open ();
		}

		main = new MainWindow(display);
		prefs = new PreferencesWindow(display);
		about = new AboutWindow(display);
		help = new HelpWindow(display,helpFolder);
		log = new LogWindow(display);
		workSet = new WorkSetWindow(display);
		graph = new GraphWindow(display);
		newProjectWizard = new NewProjectWizard(display);
		resultWindowList = new LinkedList<ResultWindow>();

		/* When the analyze button is pressed */
		main.addAnalyseAction(new ISimpleAction(){public void act()
		{
			Iterator<MainWindow.Set> iter = main.getSetEntriesOfCurrentPopulationIterator();
			if (iter != null)
			{
				if (iter.hasNext())
				{
					StudySetList studySetList = new StudySetList("guiList");

					MainWindow.Set pSet = iter.next();
					PopulationSet populationSet = new PopulationSet(pSet.name,pSet.entries);

					while (iter.hasNext())
					{
						MainWindow.Set sSet = iter.next();
						StudySet studySet = new StudySet(sSet.name, sSet.entries);
						studySetList.addStudySet(studySet);
					}

					String defintionFile = main.getDefinitionFileString();
					String associationsFile = main.getAssociationsFileString();
					String mappingFile = main.getMappingFileString();
					String methodName = main.getSelectedMethodName();
					String mtcName = main.getSelectedMTCName();

					if (mappingFile != null && mappingFile.length() == 0)
						mappingFile = null;

					Display display = main.getShell().getDisplay();

					final ResultWindow result = new ResultWindow(display);
					result.setBusyPointer(true);
					resultWindowList.add(result);

					/* Now let's start the task...TODO: Refactorize! */
					final Thread newThread = new AnalyseThread(display,main,
							result,defintionFile,associationsFile,mappingFile,
							populationSet,studySetList,methodName,mtcName,
							GlobalPreferences.getNumberOfPermutations());
					result.addCloseAction(new ISimpleAction(){public void act()
					{
						newThread.interrupt();
						resultWindowList.remove(result);
						result.dispose();
					}});
					newThread.start();
				}
			}
		}});

		/* On a new project event */
		main.addNewProjectAction(new ISimpleAction()
		{
			public void act()
			{
				newProjectWizard.open(workSetList);
			}
		});

		/* On a opening the preferences event */
		main.addOpenPreferencesAction(new ISimpleAction()
		{
			public void act() { prefs.open(); }
		});

		/* On opening the log window event */
		main.addOpenLogWindowAction(new ISimpleAction()
		{
			public void act() { log.open(); }
		});

		/* On a opening the help event */
		main.addOpenHelpContentsAction(new ISimpleAction()
		{
			public void act() { help.open(); }
		});

		/* On a about window opening event */
		main.addOpenAboutAction(new ISimpleAction()
		{
			public void act() { about.open(); }
		});

		/* On a workset window opening event */
		main.addOpenWorkSetAction(new ISimpleAction()
		{
			public void act()
			{
				workSet.updateWorkSetList(workSetList);
				workSet.open();
			}
		});

		/* Store the current settings on disposal */
		main.addDisposeAction(new ISimpleAction(){public void act()
		{
			Preferences p = Preferences.userNodeForPackage(Ontologizer.class);
			p.put("definitionFile",main.getDefinitionFileString());
			p.put("associationsFile",main.getAssociationsFileString());
			p.put("mtc", main.getSelectedMTCName());
			p.put("method", main.getSelectedMethodName());
			p.put("dotCMD",GlobalPreferences.getDOTPath());
			p.put("numberOfPermutations",Integer.toString(GlobalPreferences.getNumberOfPermutations()));
			p.put("wrapColumn", Integer.toString(GlobalPreferences.getWrapColumn()));
			if (GlobalPreferences.getProxyHost() != null)
			{
				p.put("proxyHost",GlobalPreferences.getProxyHost());
				p.put("proxyPort", Integer.toString(GlobalPreferences.getProxyPort()));
			}
		}});

		workSet.addDownloadAction(downloadAction);
		workSet.addInvalidateAction(invalidateAction);

		FileCache.addUpdateCallback(new FileCacheUpdateCallback(){
			public void update(String url)
			{
				main.getShell().getDisplay().asyncExec(new Runnable()
				{
					public void run()
					{
						workSet.updateWorkSetList(workSetList);
					}
				});
			}

			public void exception(Exception exception, String url)
			{
				Ontologizer.logException(exception);
			}
		});

		/* Remember the dot path, if it was accepted */
		prefs.addAcceptPreferencesAction(new ISimpleAction()
		{
			public void act()
			{
				GlobalPreferences.setDOTPath(prefs.getDOTPath());
				GlobalPreferences.setNumberOfPermutations(prefs.getNumberOfPermutations());
				GlobalPreferences.setProxyHost(prefs.getProxyHost());
				GlobalPreferences.setProxyPort(prefs.getProxyPort());
				GlobalPreferences.setWrapColumn(prefs.getWrapColumn());
			}
		});

		/* Set preferences */
		Preferences p = Preferences.userNodeForPackage(Ontologizer.class);
		main.setDefinitonFileString(p.get("definitionFile",""));
		main.setAssociationsFileString(p.get("associationsFile",""));
		main.setSelectedMTCName(p.get("mtc",TestCorrectionRegistry.getDefault().getName()));
		main.setSelectedMethodName(p.get("method",CalculationRegistry.getDefault().getName()));
		GlobalPreferences.setDOTPath(p.get("dotCMD","dot"));
		GlobalPreferences.setNumberOfPermutations(p.getInt("numberOfPermutations", 500));
		GlobalPreferences.setProxyPort(p.get("proxyPort", "8888"));
		GlobalPreferences.setProxyHost(p.get("proxyHost", ""));
		GlobalPreferences.setWrapColumn(p.getInt("wrapColumn", 30));

		/* Prepare workspace */
		workspace = new File(ontologizer.util.Util.getAppDataDirectory("ontologizer"),"workspace");
		if (!workspace.exists())
			workspace.mkdirs();
		logger.info("Workspace directory is \"" + workspace.getAbsolutePath() + "\"");

		main.setWorkspace(workspace);
		main.updateWorkSetList(workSetList);

		/* Prepare the file cache */
		FileCache.setCacheDirectory(new File(workspace,".cache").getAbsolutePath());

		Shell shell = main.getShell();
		shell.open();

		while (!shell.isDisposed())
		{
			try
			{
				if (!display.readAndDispatch())
					display.sleep();
			} catch(Exception ex)
			{
				logException(ex);
			}
		}

		/* Dispose the result windows but they have to be copied into a separate
		 * array before, as diposing the window implicates removing them from
		 * the list */
		ResultWindow [] resultWindowArray = new ResultWindow[resultWindowList.size()];
		int i=0;
		for (ResultWindow resultWindow : resultWindowList)
			resultWindowArray[i++] = resultWindow;
		for (i = 0;i<resultWindowArray.length;i++)
			resultWindowArray[i].dispose();

		/* Dispose the rest */
		prefs.dispose();
		about.dispose();
		help.dispose();
		log.dispose();
		graph.dispose();
		workSet.dispose();

		Images.diposeImages();

		if (useSleak)
		{
			while (!sleak.shell.isDisposed ()) {
				if (!display.readAndDispatch ()) display.sleep ();
			}
		}

		FileCache.abortAllDownloads();

		/* Ensure that all threads are finished before the main thread
		 * disposes the device. */
		ThreadGroup group = threadGroup;
		group.interrupt();

		try
		{
			synchronized(group)
			{
				while (group.activeCount() > 0)
					group.wait( 10 );
			}

		} catch (InterruptedException e)
		{
			e.printStackTrace();
		}

		display.dispose();
	}

	private static void copyFileToTemp(String file, String tempDir)
	{
		InputStream is = OntologizerCore.class.getResourceAsStream(file);

		try
		{
			byte [] buf = new byte[8192];
			int read;
			File destFile = new File(tempDir,new File(file).getName());
			BufferedOutputStream dest = new BufferedOutputStream(new FileOutputStream(destFile));

			while ((read = is.read(buf))>0)
				dest.write(buf,0,read);

			dest.close();
			destFile.deleteOnExit();
		} catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Returns whether the project name is valid.
	 *
	 * @return
	 */
	public static boolean isProjectNameValid(String name)
	{
		List<String> l = main.getProjectNames();
		for (String s : l)
		{
			if (s.equalsIgnoreCase(name))
				return false;
		}
		return true;
	}

	/**
	 * Shows the wait pointer on all application windows.
	 */
	public static void showWaitPointer()
	{
		main.showWaitPointer();
		prefs.showWaitPointer();
		about.showWaitPointer();
		help.showWaitPointer();
		for (ResultWindow r : resultWindowList)
			r.showWaitPointer();
	}

	/**
	 * Hides the wait pointer on all application windows.
	 */
	public static void hideWaitPointer()
	{
		for (ResultWindow r : resultWindowList)
			r.hideWaitPointer();
		help.hideWaitPointer();
		about.hideWaitPointer();
		prefs.hideWaitPointer();
		main.hideWaitPointer();
	}

	public static File getWorkspace()
	{
		return workspace;
	}

	public static void newProject(File project)
	{
		main.addProject(project);
	}

	public static void log(String name, String message)
	{
		SimpleDateFormat sdf = new SimpleDateFormat();
		String date = sdf.format(new Date(System.currentTimeMillis()));
		final String line = "(" + date + ") " + "[" + name + "] " + message + "\n";

		if (main != null && !main.getShell().isDisposed())
		{
			main.getShell().getDisplay().asyncExec(new Runnable()
			{
				public void run()
				{
					log.addToLog(line);
				}
			});
		}
	}

	/**
	 * Log and displays the given exception.
	 *
	 * @param e
	 */
	public static void logException(final Exception e)
	{
		log("Exception",e.getLocalizedMessage());
		if (main != null && !main.getShell().isDisposed())
		{
			main.display.syncExec(new Runnable()
			{
				public void run() {SWTUtil.displayException(main.getShell(), e);};
			});
		}
	}

	/**
	 * Log at information level.
	 *
	 * @param txt
	 */
	public static void logInfo(String txt)
	{
		log("Info",txt);
	}
}
