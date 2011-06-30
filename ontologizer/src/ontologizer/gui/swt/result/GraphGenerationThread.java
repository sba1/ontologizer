package ontologizer.gui.swt.result;

import java.io.File;
import java.util.HashSet;

import ontologizer.calculation.AbstractGOTermsResult;
import ontologizer.dotwriter.AbstractDotAttributesProvider;
import ontologizer.dotwriter.GODOTWriter;
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
public class GraphGenerationThread extends NewGraphGenerationThread
{
	public Ontology go;
	public Term emanatingTerm;
	public HashSet<TermID> leafTerms = new HashSet<TermID>();
	public AbstractGOTermsResult result;
	
	private IGraphGenerationFinished finished;
	private AbstractDotAttributesProvider provider;

	public GraphGenerationThread(Display display, String dotCMDPath, IGraphGenerationFinished f, AbstractDotAttributesProvider p)
	{
		super(display, dotCMDPath);
		
		this.finished = f;
		this.provider = p;
		
		setSupport(new IGraphGenerationSupport()
		{
			public void writeDOT(File dotFile)
			{
				if (result != null)
				{
					result.writeDOT(go, dotFile,
						emanatingTerm != null ? emanatingTerm.getID() : null,
						leafTerms, provider);
				} else
				{
					GODOTWriter.writeDOT(go, dotFile,
						emanatingTerm != null ? emanatingTerm.getID() : null,
						leafTerms, provider);
				}
			}

			public void layoutFinished(boolean success, String msg,
					File pngFile, File dotFile)
			{
				finished.finished(success, msg, pngFile, dotFile);
			}
		});
	}
};
