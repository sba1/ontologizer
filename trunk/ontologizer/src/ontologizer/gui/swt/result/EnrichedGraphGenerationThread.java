package ontologizer.gui.swt.result;

import java.io.File;
import java.util.HashSet;

import ontologizer.calculation.EnrichedGOTermsResult;
import ontologizer.go.Ontology;
import ontologizer.go.Term;
import ontologizer.go.TermID;
import ontologizer.gui.swt.support.IGraphGenerationFinished;
import ontologizer.gui.swt.support.IGraphGenerationSupport;
import ontologizer.gui.swt.support.NewGraphGenerationThread;

import org.eclipse.swt.widgets.Display;

/**
 * Generates the graph by executing DOT. When finished
 * the finished method of the specified constructor argument
 * is executed in the context of the GUI thread.
 * 
 * @author Sebastian Bauer
 */
public class EnrichedGraphGenerationThread extends NewGraphGenerationThread
{
	public Ontology go;
	public Term emanatingTerm;
	public double significanceLevel;
	public HashSet<TermID> leafTerms = new HashSet<TermID>();
	public EnrichedGOTermsResult result;

	private IGraphGenerationFinished finished;

	private IGraphGenerationSupport support = new IGraphGenerationSupport()
	{
		public void writeDOT(File dotFile)
		{
			result.writeDOT(go, dotFile,
					significanceLevel, true,
					emanatingTerm != null ? emanatingTerm.getID() : null,
					leafTerms);
		}
		
		public void layoutFinished(boolean success, String msg, File pngFile,
				File dotFile)
		{
			finished.finished(success, msg, pngFile, dotFile);
		}
	};

	public EnrichedGraphGenerationThread(Display display, String dotCMDPath, IGraphGenerationFinished finished)
	{
		super(display, dotCMDPath);
		
		setSupport(support);

		this.finished = finished;
	}
};
