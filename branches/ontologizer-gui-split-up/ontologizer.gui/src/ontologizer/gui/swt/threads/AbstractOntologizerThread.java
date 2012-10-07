package ontologizer.gui.swt.threads;

import java.io.IOException;

import ontologizer.FileCache;
import ontologizer.OntologizerThreadGroups;
import ontologizer.FileCache.FileDownload;
import ontologizer.gui.swt.ResultWindow;

import org.eclipse.swt.widgets.Display;

/**
 * This is the parent class of all Ontologizer analyse threads.
 * 
 * @author Sebastian Bauer
 *
 */
public abstract class AbstractOntologizerThread extends Thread
{
	protected Display display;
	protected ResultWindow result;
	private Runnable calledWhenFinished;

	public AbstractOntologizerThread(String threadName, Runnable calledWhenFinnished, Display d, ResultWindow r)
	{
		super(OntologizerThreadGroups.workerThreadGroup, threadName);
		
		this.display = d;
		this.result = r;
		this.calledWhenFinished = calledWhenFinnished;
	}
	
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
	
	@Override
	final public void run()
	{
		perform();
		calledWhenFinished.run();
	}

	/**
	 * Method to be implemented by subclasses.
	 */
	public abstract void perform();

	/**
	 * Downloads a file in a synchronous manner.
	 * 
	 * @param filename
	 * @param message defines the message sent to the result window.
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 */
	protected String downloadFile(String filename, final String message)
			throws IOException, InterruptedException
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

}