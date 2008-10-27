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
				GOGraph graph = WorkSetLoadThread.getGraph(workSet.getOboPath());
				AssociationContainer assoc = WorkSetLoadThread.getAssociations(workSet.getAssociationPath());

				SemanticCalculation s = new SemanticCalculation(graph,assoc);

				for (StudySet studySet : studySetList)
				{
					final SemanticResult sr = s.calculate(studySet);
					double [][] mat = sr.mat;

					for (int i=0;i<mat.length;i++)
					{
						for (int j=0;j<mat.length;j++)
						{
							System.out.print(mat[i][j] + "  ");
						}
						System.out.println();
					}

					display.asyncExec(new Runnable()
					{
						public void run()
						{
							result.addResults(sr);
						}
					});

				}

				WorkSetLoadThread.releaseDatafiles(workSet);

				display.asyncExec(new Runnable(){public void run() {
					if (!result.isDisposed())
					{
						result.setBusyPointer(false);
						result.appendLog("Calculation finished");
						result.clearProgressText();
						result.hideProgressBar();
					}
				};});

			}
		});

	}
}
