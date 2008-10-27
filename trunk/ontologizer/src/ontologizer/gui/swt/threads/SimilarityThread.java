package ontologizer.gui.swt.threads;

import org.eclipse.swt.widgets.Display;

import ontologizer.StudySet;
import ontologizer.StudySetList;
import ontologizer.association.AssociationContainer;
import ontologizer.calculation.SemanticCalculation;
import ontologizer.calculation.SemanticResult;
import ontologizer.go.GOGraph;
import ontologizer.gui.swt.ResultWindow;
import ontologizer.worksets.IWorkSetProgress;
import ontologizer.worksets.WorkSet;
import ontologizer.worksets.WorkSetLoadThread;

public class SimilarityThread extends AbstractOntologizerThread
{
	private StudySetList studySetList;
	private WorkSet workSet;

	public SimilarityThread(Display display, Runnable calledWhenFinished, ResultWindow result, StudySetList studySetList, WorkSet workSet)
	{
		super("Similarity Thread", calledWhenFinished, display, result);
		
		this.studySetList = studySetList;
		this.workSet = workSet;
	}
	
	@Override
	public void perform()
	{
		final Object lock = new Object();
		
		synchronized (lock)
		{
			WorkSetLoadThread.obtainDatafiles(workSet, new IWorkSetProgress()
			{
				public void message(final String message)
				{
					display.asyncExec(new Runnable() {
						public void run()
						{
							if (!result.isDisposed())
							{
								result.appendLog(message);
							}
						}});
				}
	
				public void initGauge(final int maxWork)
				{
					display.asyncExec(new Runnable() {
						public void run()
						{
							if (!result.isDisposed())
							{
								result.updateProgress(0);
								
								if (maxWork > 0)
								{
									result.initProgress(maxWork);
									result.showProgressBar();
								} else
									result.hideProgressBar();
							}
						}});
				}
	
				public void updateGauge(final int currentWork)
				{
					display.asyncExec(new Runnable() {
						public void run()
						{
							if (!result.isDisposed())
							{
								result.updateProgress(currentWork);
							}
						}});
					
				}
			},new Runnable()
			{
				public void run()
				{
					synchronized (lock)
					{
						lock.notifyAll();
					}
				}
			});

			try
			{
				lock.wait();

				/* Stuff should have been loaded at this point */

				GOGraph graph = WorkSetLoadThread.getGraph(workSet.getOboPath());
				AssociationContainer assoc = WorkSetLoadThread.getAssociations(workSet.getAssociationPath());

				display.asyncExec(new ResultAppendLogRunnable("Preparing semantic calculation"));

				SemanticCalculation s = new SemanticCalculation(graph,assoc);

				for (StudySet studySet : studySetList)
				{
					display.asyncExec(new ResultAppendLogRunnable("Analyzing study set " + studySet.getName()));

					final SemanticResult sr = s.calculate(studySet,new SemanticCalculation.ISemanticCalculationProgress()
					{
						public void init(final int max)
						{
							display.asyncExec(new Runnable()
							{
								public void run()
								{
									result.showProgressBar();
									result.initProgress(max);
								}
							});
						}
						
						public void update(final int update)
						{
							display.asyncExec(new Runnable()
							{
								public void run()
								{
									result.updateProgress(update);
								}
							});
						}
					});

					display.asyncExec(new Runnable()
					{
						public void run()
						{
							result.addResults(sr);
						}
					});

				}
				
				display.asyncExec(new Runnable(){public void run() {
					if (!result.isDisposed())
					{
						result.setBusyPointer(false);
						result.appendLog("Calculation finished");
						result.clearProgressText();
						result.hideProgressBar();
					}
				};});

				WorkSetLoadThread.releaseDatafiles(workSet);
			} catch (InterruptedException e)
			{
			}
		}
	}
}
