package ontologizer.worksets;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import ontologizer.FileCache;
import ontologizer.OntologizerThreadGroups;
import ontologizer.FileCache.FileCacheUpdateCallback;
import ontologizer.association.AssociationContainer;
import ontologizer.association.AssociationParser;
import ontologizer.association.IAssociationParserProgress;
import ontologizer.go.Ontology;
import ontologizer.go.IOBOParserProgress;
import ontologizer.go.OBOParser;
import ontologizer.go.OBOParserException;
import ontologizer.go.TermContainer;
import ontologizer.util.MemoryWarningSystem;

/**
 * Thread which is responsible for loading work set files.
 * 
 * @author Sebastian Bauer
 *
 */
public class WorkSetLoadThread extends Thread
{
	private static Logger logger = Logger.getLogger(WorkSetLoadThread.class.getName());

	private static class Message { }
	private static class WorkSetMessage extends Message { public WorkSet workset; }
	private static class ObtainWorkSetMessage extends WorkSetMessage { Runnable callback; public IWorkSetProgress progress;}
	private static class ReleaseWorkSetMessage extends WorkSetMessage { }
	private static class CleanCacheMessage extends Message { }
	private static class CallbackMessage extends Message { Runnable run;}

	/**
	 * The task of downloading obo and association files. 
	 * 
	 * @author Sebastian Bauer
	 *
	 */
	private static class Task
	{
		public String obo;
		public String assoc;
		
		public boolean oboDownloaded;
		public boolean assocDownloaded;

		private List<Runnable> callbacks = new LinkedList<Runnable>();
		private List<IWorkSetProgress> progresses = new LinkedList<IWorkSetProgress>();
		
		/**
		 * Add a new callback.
		 * 
		 * @param run
		 */
		public void addCallback(Runnable run)
		{
			callbacks.add(run);
		}
		
		/**
		 * Issue all callbacks.
		 */
		public void issueCallbacks()
		{
			for (Runnable run : callbacks)
				run.run();
		}
		
		@Override
		public int hashCode()
		{
			return obo.hashCode() + assoc.hashCode();
		}
		
		@Override
		public boolean equals(Object arg0)
		{
			Task t = (Task)arg0;
			return t.obo.equals(obo) && t.assoc.equals(assoc);
		}
		
		/**
		 * Checks whether the work set describes this task.
		 * 
		 * @param ws
		 * @return
		 */
		public boolean matches(WorkSet ws)
		{
			return ws.getAssociationPath().equals(assoc) && ws.getOboPath().equals(obo); 
		}
	}
	
	static private WorkSetLoadThread wslt;
	
	static
	{
		wslt = new WorkSetLoadThread();
		wslt.start();
	}

	static private IWorkSetProgress dummyWorkSetProgress = new IWorkSetProgress()
	{
		public void initGauge(int maxWork) {}
		public void message(String message) {}
		public void updateGauge(int currentWork){}
	};

	/**
	 * Obtain the data files for the given WorkSet. Calls run
	 * (in a context of another thread) on completion.
	 * 
	 * @param df
	 * @param run
	 */
	static public void obtainDatafiles(WorkSet df, Runnable run)
	{
		obtainDatafiles(df, null, run);
	}
	
	/**
	 * Obtain the data files for the given WorkSet. Calls run
	 * (in a context of another thread) on completion.
	 * 
	 * @param df
	 * @param run
	 */
	static public void obtainDatafiles(WorkSet df, IWorkSetProgress progress, Runnable run)
	{
		ObtainWorkSetMessage owsm = new ObtainWorkSetMessage();
		owsm.workset = df;
		owsm.callback = run;
		if (progress != null) owsm.progress = progress;
		else owsm.progress = dummyWorkSetProgress;

		wslt.messageQueue.add(owsm);
	}
	
	/**
	 * Release the data files for the given WorkSet.
	 * 
	 * @param df
	 */
	public static void releaseDatafiles(WorkSet df)
	{
		ReleaseWorkSetMessage rwsm = new ReleaseWorkSetMessage();
		rwsm.workset = df;
		wslt.messageQueue.add(rwsm);
	}

	public static void cleanCache()
	{
		CleanCacheMessage cwsm = new CleanCacheMessage();
		wslt.messageQueue.add(cwsm);
	}

	public static AssociationContainer getAssociations(String associationPath)
	{
		String localPath = FileCache.getLocalFileName(associationPath);
		if (localPath == null) return null;
		return wslt.assocMap.get(localPath);
	}

	public static Ontology getGraph(String oboPath)
	{
		String localPath = FileCache.getLocalFileName(oboPath);
		if (localPath == null) return null;
		return wslt.graphMap.get(localPath);
	}

	/* Private attributes */
	private Map<String,Ontology> graphMap = Collections.synchronizedMap(new HashMap<String,Ontology>());
	private Map<String,AssociationContainer> assocMap = Collections.synchronizedMap(new HashMap<String,AssociationContainer>());

	private BlockingQueue<Message> messageQueue = new LinkedBlockingQueue<Message>();

	private List<Task> taskList = new LinkedList<Task>(); 
	private FileCacheUpdateCallback fileCacheUpdateCallback;

	public WorkSetLoadThread()
	{
		super(OntologizerThreadGroups.workerThreadGroup,"Work Set Loader Thread");
	}

	@Override
	public void run()
	{
		/* Lower priority */
		setPriority(Thread.MIN_PRIORITY);

		/* Low memory handler */
		MemoryWarningSystem.setPercentageUsageThreshold(0.80);
	    MemoryWarningSystem mws = new MemoryWarningSystem();
	    mws.addListener(new MemoryWarningSystem.Listener() {
	      public void memoryUsageLow(long usedMemory, long maxMemory) {
	    	  logger.warning("Low memory condition! Trying to clean some caches");
	    	  cleanCache();
	      }
	    });

		/* Our callback function */
		fileCacheUpdateCallback = new FileCacheUpdateCallback()
		{
			/**
			 * This runnable has to be called from the workset load thread.
			 * It is called whenever a new state of an URL is given. Here,
			 * we check whether this URL affects any download tasks, and, if so
			 * handle this event.
			 */
			class CleanupTasksRunnable implements Runnable
			{
				private String url;
				private Exception exception;

				public CleanupTasksRunnable(String url)
				{
					this.url = url;
				}

				public CleanupTasksRunnable(Exception exception, String url)
				{
					this.url = url;
					this.exception = exception;
				}

				public void run()
				{
					/* This is performed inside the work set load thread */
					List <Task> toBeRemoved = new LinkedList<Task>();

					/* Go through all task, check whether the URL affects any tasks */
					for (Task t : taskList)
					{
						if (url.equals(t.obo))
							t.oboDownloaded = true;
						if (url.equals(t.assoc))
							t.assocDownloaded = true;


						if (t.oboDownloaded)
						{
							try
							{
								if (FileCache.isNonBlocking(t.obo))
								{
									loadGraph(FileCache.getLocalFileName(t.obo), dummyWorkSetProgress);
								}
							} catch (Exception ex)
							{
								
							}

							if (t.assocDownloaded)
							{
								/* Both, the definition and the assoc file for this task has been at least downloaded.
								 * Note that loading the graph is double-work here but it is not as it is cached.
								 * TODO: Load only the assoc here. */
	
								if (FileCache.isNonBlocking(t.obo) && FileCache.isNonBlocking(t.assoc))
								{
									loadFiles(FileCache.getLocalFileName(t.obo), FileCache.getLocalFileName(t.assoc), dummyWorkSetProgress);
								}
	
								t.issueCallbacks();
								toBeRemoved.add(t);
							}
						}
					}
							
					taskList.removeAll(toBeRemoved);

					/* We are not interested in further events */
					if (taskList.size() == 0)
						FileCache.removeUpdateCallback(fileCacheUpdateCallback);
				}
			}
			
			public void update(String url)
			{
				if (FileCache.getState(url) == FileCache.FileState.CACHED)
				{
					execAsync(new CleanupTasksRunnable(url));
				}
			}
			
			public void exception(Exception exception, String url)
			{
				/* An error occurred when downloading the specified URL */
				execAsync(new CleanupTasksRunnable(exception, url));
			}
		};

		try
		{
			/* Message loop */
			again:
			while (true)
			{
				Message msg =  messageQueue.take();
				
				if (msg instanceof CallbackMessage)
				{
					((CallbackMessage)msg).run.run();
				} else
				if (msg instanceof CleanCacheMessage)
				{
					graphMap.clear();
					assocMap.clear();
				} else
				if (msg instanceof WorkSetMessage)
				{
					WorkSetMessage wsm = (WorkSetMessage) msg;
					WorkSet ws = wsm.workset;

					if (wsm instanceof ObtainWorkSetMessage)
					{
						/* Check whether stuff has already been loaded. Fire if positive */
						ObtainWorkSetMessage owsm = (ObtainWorkSetMessage) msg;	
						if (graphMap.containsKey(ws.getOboPath()) && assocMap.containsKey(ws.getAssociationPath()))
						{
							owsm.callback.run();
							continue again;
						}

						/* Check whether a similar task is pending. Add the callback if positive. */
						for (Task task : taskList)
						{
							if (task.matches(ws))
							{
								task.addCallback(owsm.callback);
								continue again;
							}
						}

						logger.info("The name of the obo file to be loaded is \"" + ws.getOboPath() + "\"");
						logger.info("The name of the association file to be loaded is \"" + ws.getAssociationPath() + "\"");

						/* open() returns null, if files are about to be downloaded, otherwise
						 * it returns the local file cache. We can check that way whether files have already
						 * been downloaded */ 
						String oboName = FileCache.open(ws.getOboPath());
						String assocName = FileCache.open(ws.getAssociationPath());

						if (oboName != null && assocName != null)
						{
							loadFiles(oboName,assocName,owsm.progress);
							owsm.callback.run();
							continue again;
						}

						/* Task wasn't found. The file weren't loaded yet. Create a new task. */
						Task newTask = new Task();
						newTask.assoc = ws.getAssociationPath();
						newTask.assocDownloaded = assocName != null;
						newTask.obo = ws.getOboPath();
						newTask.oboDownloaded = oboName != null;
						newTask.addCallback(owsm.callback);
						addTask(newTask);
					} else
					{
						if (wsm instanceof ReleaseWorkSetMessage)
						{
						}
					}
				}
			}
		}
		catch (InterruptedException e)
		{
		}
		catch(Exception e)
		{
			if (!Thread.interrupted())
				e.printStackTrace();
		}
	}
	
	/**
	 * Add a new task to the task list.
	 * 
	 * @param newTask
	 */
	private void addTask(Task newTask)
	{
		if (taskList.size() == 0)
			FileCache.addUpdateCallback(fileCacheUpdateCallback);
			
		taskList.add(newTask);
	}

	/**
	 * Load the graph.
	 * 
	 * @param oboName
	 * @param workSetProgress
	 * @return
	 * @throws IOException
	 * @throws OBOParserException
	 */
	private Ontology loadGraph(String oboName, final IWorkSetProgress workSetProgress) throws IOException, OBOParserException
	{
		Ontology graph;
		if (!graphMap.containsKey(oboName))
		{
			OBOParser oboParser = new OBOParser(oboName, OBOParser.IGNORE_SYNONYMS);
			workSetProgress.message("Parsing OBO file");
			oboParser.doParse(new IOBOParserProgress()
			{

				public void init(int max)
				{
					workSetProgress.initGauge(max);
				}

				public void update(int current, int terms)
				{
					workSetProgress.message("Parsing OBO file ("+terms+")");
					workSetProgress.updateGauge(current);
				}
			});
			TermContainer goTerms = new TermContainer(oboParser.getTermMap(), oboParser.getFormatVersion(), oboParser.getDate());
			workSetProgress.message("Building GO graph");
			graph = new Ontology(goTerms);
			graphMap.put(oboName,graph);
		} else
		{
			graph = graphMap.get(oboName);
		}
		return graph;
	}


	/**
	 * Load the given files. Add them as loaded.
	 * 
	 * @param oboName real file names
	 * @param assocName real file names
	 * @param workSetProgress 
	 */
	private void loadFiles(String oboName, String assocName, final IWorkSetProgress workSetProgress)
	{
		if (!new File(oboName).exists()) return;
		if (!new File(assocName).exists()) return;

		try
		{
			Ontology graph = loadGraph(oboName, workSetProgress);
			
			if (!assocMap.containsKey(assocName))
			{
				logger.info("Parse local association file \"" + assocName + "\"");

				workSetProgress.message("Parsing association file");
				workSetProgress.updateGauge(0);
				AssociationParser ap = new AssociationParser(assocName,graph.getTermContainer(),null,new IAssociationParserProgress()
				{
					public void init(int max)
					{
						workSetProgress.initGauge(max);
					}

					public void update(int current)
					{
						workSetProgress.updateGauge(current);
					}
				});
				
				AssociationContainer ac = new AssociationContainer(ap.getAssociations(), ap.getSynonym2gene(), ap.getDbObject2gene());
				assocMap.put(assocName, ac);
				workSetProgress.message("");
				workSetProgress.initGauge(0);
			}
		} catch (Exception e)
		{
			logger.log(Level.SEVERE, "Failed to load files", e);
		}
	}


	/**
	 * Execute a runnable in the context of this thread.
	 * 
	 * @param run
	 */
	private void execAsync(Runnable run)
	{
		CallbackMessage cmsg = new CallbackMessage();
		cmsg.run = run;
		messageQueue.add(cmsg);
	}

}
