package ontologizer.benchmark;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import ontologizer.FileCache;
import ontologizer.association.AssociationContainer;
import ontologizer.go.Ontology;
import ontologizer.worksets.WorkSet;
import ontologizer.worksets.WorkSetLoadThread;

/**
 * Wrapper for data loading.
 * 
 * @author Sebastian Bauer
 */
public class Datafiles
{
	public Ontology graph;
	public AssociationContainer assoc;

	public Datafiles()
	{
	}

	public Datafiles(String oboName, String assocName) throws InterruptedException, IOException
	{
		File workspace = new File(ontologizer.util.Util.getAppDataDirectory("ontologizer"),"workspace");
		if (!workspace.exists())
			workspace.mkdirs();
		FileCache.setCacheDirectory(new File(workspace,".cache").getAbsolutePath());
		final WorkSet ws = new WorkSet("Test");
		ws.setOboPath(oboName);
		ws.setAssociationPath(assocName);
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
		
		if (graph == null) throw new IOException("Couldn't open file \"" + oboName + "\"");
		if (assoc == null) throw new IOException("Couldn't open file \"" + assoc + "\"");
	}
}
