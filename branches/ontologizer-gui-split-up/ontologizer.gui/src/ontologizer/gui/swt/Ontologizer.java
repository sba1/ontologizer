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
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import ontologizer.BuildInfo;
import ontologizer.FileCache;
import ontologizer.GlobalPreferences;
import ontologizer.OntologizerCore;
import ontologizer.OntologizerThreadGroups;
import ontologizer.FileCache.FileCacheUpdateCallback;
import ontologizer.calculation.CalculationRegistry;
import ontologizer.calculation.ICalculation;
import ontologizer.gui.swt.MainWindow.Set;
import ontologizer.gui.swt.images.Images;
import ontologizer.gui.swt.support.SWTUtil;
import ontologizer.gui.swt.threads.AnalyseThread;
import ontologizer.gui.swt.threads.SimilarityThread;
import ontologizer.set.PopulationSet;
import ontologizer.set.StudySet;
import ontologizer.set.StudySetFactory;
import ontologizer.set.StudySetList;
import ontologizer.statistics.TestCorrectionRegistry;
import ontologizer.worksets.WorkSet;
import ontologizer.worksets.WorkSetList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.DeviceData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;

import tools.Sleak;

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
	private static FileCacheWindow fileCache;
	private static WorkSetWindow workSet;
	private static NewProjectWizard newProjectWizard;
	private static GraphWindow graph;
	private static LinkedList<ResultWindow> resultWindowList;
	private static File workspace;

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
//				workSet.updateWorkSetList(workSetList);
			}
		};

	/**
	 * Returns the menu item for the given id or null.
	 * 
	 * @param menu
	 * @param id
	 * @return
	 */
	static MenuItem getItem(Menu menu, int id)
	{
		MenuItem[] items = menu.getItems();
		for (int i = 0; i < items.length; i++) {
			if (items[i].getID() == id) return items[i];
		}
		return null;
	}
		
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

		/* Prepare logging */
		Logger rootLogger = Logger.getLogger("");
		rootLogger.addHandler(new Handler()
		{
			public void close() throws SecurityException { }
			public void flush() { }
			@Override
			public void publish(LogRecord record)
			{
				log(record.getLevel().getName(),record.getMessage(), record.getThrown());
				
			}
			@Override
			protected void reportError(String msg, Exception ex, int code)
			{
				super.reportError(msg, ex, code);
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

				/* TODO: Find out if there is a possibility to list the files */
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
		fileCache = new FileCacheWindow(display);
		workSet = new WorkSetWindow(display);
		graph = new GraphWindow(display);
		newProjectWizard = new NewProjectWizard(display);
		resultWindowList = new LinkedList<ResultWindow>();

		/* When the analyze button is pressed */
		main.addAnalyseAction(new ISimpleAction(){public void act()
		{
			List<MainWindow.Set> list = main.getSetEntriesOfCurrentPopulation();
			if (list == null) return;

			if (list.size() > 1)
			{
				PopulationSet populationSet = getPopulationSetFromList(list);
				StudySetList studySetList = getStudySetListFromList(list);
	
				String defintionFile = main.getDefinitionFileString();
				String associationsFile = main.getAssociationsFileString();
				String mappingFile = main.getMappingFileString();
				String methodName = main.getSelectedMethodName();
				String mtcName = main.getSelectedMTCName();
				String subontologyName = main.getSubontologyString();
				String subsetName = main.getSubsetString();
				Collection<String> checkedEvidences = main.getCheckedEvidences();

				if (mappingFile != null && mappingFile.length() == 0)
					mappingFile = null;
				
				if (subontologyName != null && subontologyName.length() == 0)
					subontologyName = null;

				if (subsetName != null && subsetName.length() == 0)
					subsetName = null;

				final Display display = main.getShell().getDisplay();
				final ResultWindow result = new ResultWindow(display);
				result.setBusyPointer(true);
				resultWindowList.add(result);

				Runnable calledWhenFinished = new Runnable()
				{
					public void run()
					{
						display.syncExec(new Runnable(){ public void run()
						{
							if (!main.getShell().isDisposed())
								main.enableAnalyseButton();
						}});
					}
				};
				/* Now let's start the task...TODO: Refactor! */
				final Thread newThread = new AnalyseThread(display,calledWhenFinished,
						result,defintionFile,associationsFile,mappingFile,
						populationSet,studySetList,methodName,mtcName,
						subsetName,subontologyName,checkedEvidences,
						GlobalPreferences.getNumberOfPermutations(),
						GlobalPreferences.getAlpha(),GlobalPreferences.getUpperAlpha(),
						GlobalPreferences.getBeta(),GlobalPreferences.getUpperBeta(),
						GlobalPreferences.getExpectedNumber(), GlobalPreferences.getMcmcSteps());
				result.addCloseAction(new ISimpleAction(){public void act()
				{
					newThread.interrupt();
					resultWindowList.remove(result);
					result.dispose();
				}});
				newThread.start();
			}
		}});
		
		/* When the "Similarity" button is pressed */
		main.addSimilarityAction(new ISimpleAction()
		{
			public void act()
			{
				List<MainWindow.Set> list = main.getSetEntriesOfCurrentPopulation();
				if (list == null) return;

				if (list.size() > 1)
				{
					final Display display = main.getShell().getDisplay();

					final StudySetList studySetList = getStudySetListFromList(list);
					final WorkSet workSet = main.getSelectedWorkingSet();
					final ResultWindow result = new ResultWindow(display);

					result.open();
					result.setBusyPointer(true);
					resultWindowList.add(result);
					
					Runnable calledWhenFinished = new Runnable()
					{
						public void run()
						{
							display.syncExec(new Runnable(){ public void run()
							{
								if (!main.getShell().isDisposed())
									main.enableAnalyseButton();
							}});
						}
					};

					final SimilarityThread newThread = new SimilarityThread(display,calledWhenFinished,result,studySetList,workSet);
					result.addCloseAction(new ISimpleAction(){public void act()
					{
						newThread.interrupt();
						resultWindowList.remove(result);
						result.dispose();
					}});
					newThread.start();
				}
			}
		});
		
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

		/* On a opening the filecache window  event */
		main.addOpenFileCacheAction(new ISimpleAction()
		{
			public void act() { fileCache.open(); }
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
		
		/* When a new method is selected check if the new method
		 * supports MTC and update the state of the MTC selector
		 * accordingly. 
		 */
		main.addMethodAction(new ISimpleAction(){
			public void act()
			{
				ICalculation calc = CalculationRegistry.getCalculationByName(main.getSelectedMethodName());
				main.setMTCEnabled(calc.supportsTestCorrection());
			}});

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
			p.put("alpha", Double.toString(GlobalPreferences.getAlpha()));
			p.put("upperAlpha", Double.toString(GlobalPreferences.getUpperAlpha()));
			p.put("beta", Double.toString(GlobalPreferences.getBeta()));
			p.put("upperBeta", Double.toString(GlobalPreferences.getUpperBeta()));
			p.put("expectedNumberOfTerms", Integer.toString(GlobalPreferences.getExpectedNumber()));
			p.put("mcmcSteps", Integer.toString(GlobalPreferences.getMcmcSteps()));
			if (GlobalPreferences.getProxyHost() != null)
			{
				p.put("proxyHost",GlobalPreferences.getProxyHost());
				p.put("proxyPort", Integer.toString(GlobalPreferences.getProxyPort()));
			}
		}});

		workSet.addDownloadAction(downloadAction);
		workSet.addInvalidateAction(invalidateAction);

		fileCache.addRemoveAction(new ISimpleAction()
		{
			public void act() 
			{
				String url = fileCache.getSelectedURL();
				if (url != null)
				{
					FileCache.invalidate(url);
					fileCache.updateView();
				}
			}
		});

		FileCache.addUpdateCallback(new FileCacheUpdateCallback(){
			
			/**
			 * Indicates that a refresh is pending (no need to issue this
			 * twice)   
			 */
			private boolean pendingRefresh;
			private Object lock = new Object();

			public void update(String url)
			{
				synchronized (lock) {
					if (pendingRefresh)
						return;
					pendingRefresh = true;
				}

				main.getShell().getDisplay().asyncExec(new Runnable()
				{
					public void run()
					{
						workSet.updateWorkSetList(workSetList);
						fileCache.updateView();
						
						synchronized (lock)
						{
							pendingRefresh = false;
						}
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
				GlobalPreferences.setUpperAlpha(prefs.getUpperAlpha());
				GlobalPreferences.setAlpha(prefs.getAlpha());
				GlobalPreferences.setUpperBeta(prefs.getUpperBeta());
				GlobalPreferences.setBeta(prefs.getBeta());
				GlobalPreferences.setExpectedNumber(prefs.getExpectedNumberOfTerms());
				GlobalPreferences.setMcmcSteps(prefs.getNumberOfMCMCSteps());
			}
		});

		/* Set preferences */
		Preferences p = Preferences.userNodeForPackage(Ontologizer.class);
		main.setDefinitonFileString(p.get("definitionFile",""));
		main.setAssociationsFileString(p.get("associationsFile",""));
		main.setSelectedMTCName(p.get("mtc",TestCorrectionRegistry.getDefault().getName()));
		
		ICalculation calc = CalculationRegistry.getCalculationByName(p.get("method",CalculationRegistry.getDefault().getName()));
		if (calc == null) calc = CalculationRegistry.getDefault();
		main.setSelectedMethodName(calc.getName());
		main.setMTCEnabled(calc.supportsTestCorrection());
		
		GlobalPreferences.setDOTPath(p.get("dotCMD","dot"));
		GlobalPreferences.setNumberOfPermutations(p.getInt("numberOfPermutations", 500));
		GlobalPreferences.setProxyPort(p.get("proxyPort", "8888"));
		GlobalPreferences.setProxyHost(p.get("proxyHost", ""));
		GlobalPreferences.setWrapColumn(p.getInt("wrapColumn", 30));
		GlobalPreferences.setAlpha(p.getDouble("alpha", Double.NaN));
		GlobalPreferences.setUpperAlpha(p.getDouble("upperAlpha", 1));
		GlobalPreferences.setBeta(p.getDouble("beta", Double.NaN));
		GlobalPreferences.setUpperBeta(p.getDouble("upperBeta", 1));
		GlobalPreferences.setExpectedNumber(p.getInt("expectedNumberOfTerms", -1));
		GlobalPreferences.setMcmcSteps(p.getInt("mcmcSteps", 500000));

		/* Prepare workspace */
		workspace = new File(ontologizer.util.Util.getAppDataDirectory("ontologizer"),"workspace");
		if (!workspace.exists())
			workspace.mkdirs();
		logger.info("Workspace directory is \"" + workspace.getAbsolutePath() + "\"");

		main.setWorkspace(workspace);
		main.updateWorkSetList(workSetList);

		/* Prepare the file cache */
		FileCache.setCacheDirectory(new File(workspace,".cache").getAbsolutePath());
		fileCache.setDirectoryText(FileCache.getCacheDirectory());

		Menu systemMenu = display.getSystemMenu();
		if (systemMenu != null)
		{
			MenuItem item = getItem(systemMenu,SWT.ID_ABOUT);
			if (item != null)
			{
				item.setText("About Ontologizer");
				item.addSelectionListener(new SelectionAdapter()
				{
					@Override
					public void widgetSelected(SelectionEvent e) {
						about.open();
					}
				});
			} else logger.info("About menu entry not found!");
			
			item = getItem(systemMenu,SWT.ID_PREFERENCES);
			if (item != null)
			{
				item.addSelectionListener(new SelectionAdapter()
				{
					@Override
					public void widgetSelected(SelectionEvent e)
					{
						prefs.open();
					}
				});
			} else logger.info("Preferences menu entry not found!");
			
			item = getItem(systemMenu,SWT.ID_QUIT);
			if (item != null)
			{
				item.addSelectionListener(new SelectionAdapter()
				{
					@Override
					public void widgetSelected(SelectionEvent e)
					{
						main.getShell().dispose();
					}
				});
			} else logger.info("Quit menu entry not found!");
					
			
		} else logger.info("System menu entry not found!");

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
		 * array before, as disposing the window implicates removing them from
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
		ThreadGroup group = OntologizerThreadGroups.workerThreadGroup;
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

		if (!display.isDisposed())
			display.dispose();

		/* On MacOSX we have to explicitly exit */
		if (System.getProperty("os.name","unknown").toLowerCase().contains("mac"))
		{		
			System.exit(0);
		}
	}

	private static PopulationSet getPopulationSetFromList(List<Set> list)
	{
		if (list == null) return null;
		try
		{
			PopulationSet popSet = (PopulationSet)StudySetFactory.createFromArray(list.get(0).entries, true);
			popSet.setName(list.get(0).name);
			return popSet;
		} catch(IOException exp)
		{
			Ontologizer.logException(exp);
		}
		return new PopulationSet(list.get(0).name);
		
	}
	
	private static StudySetList getStudySetListFromList(List<Set> list)
	{
		Iterator<MainWindow.Set> iter = list.iterator();

		/* Skip population */
		iter.next();

		StudySetList studySetList = new StudySetList("guiList");

		while (iter.hasNext())
		{
			try {
				MainWindow.Set sSet = iter.next();
				StudySet studySet;

				studySet = StudySetFactory.createFromArray(sSet.entries,false);
				studySet.setName(sSet.name);
				studySetList.addStudySet(studySet);
			} catch (IOException e)
			{
				Ontologizer.logException(e);
			}
		}
		return studySetList;
	}

	/**
	 * Copies the given file to a given temporary directory.
	 * 
	 * @param file
	 * @param tempDir
	 */
	private static void copyFileToTemp(String file, String tempDir)
	{
		InputStream is = OntologizerCore.class.getResourceAsStream(file);
		if (is == null) return;
		
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
	
	/************************************************************************/
	
	public static void log(String name, String message)
	{
		log(name,message,null);
	}

	public static void log(String name, String message, Throwable thrown)
	{
		SimpleDateFormat sdf = new SimpleDateFormat();
		String date = sdf.format(new Date(System.currentTimeMillis()));
		final String line;
		
		String exceptionMessage = "";
		if (thrown != null)
			exceptionMessage = ": " + thrown.getLocalizedMessage();
		
		line = "(" + date + ") " + "[" + name + "] " + message + exceptionMessage + "\n" ;

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
		log("Exception",e.getLocalizedMessage(),e);
		logger.log(Level.SEVERE, "Exception", e);
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
