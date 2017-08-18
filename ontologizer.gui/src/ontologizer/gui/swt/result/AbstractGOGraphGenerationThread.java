package ontologizer.gui.swt.result;

import java.io.File;
import java.util.HashSet;

import org.eclipse.swt.widgets.Display;

import ontologizer.gui.swt.support.IGraphGenerationSupport;
import ontologizer.gui.swt.support.NewGraphGenerationThread;
import ontologizer.io.dot.ITermDotAttributesProvider;
import ontologizer.io.dot.OntologyDotWriter;
import ontologizer.ontology.Ontology;
import ontologizer.ontology.TermID;

/**
 * This is a skeleton class for the generation of a go graph.
 *
 * @author Sebastian Bauer
 */
public abstract class AbstractGOGraphGenerationThread extends NewGraphGenerationThread implements IGraphGenerationSupport, ITermDotAttributesProvider
{
	private Ontology graph;
	private HashSet<TermID> leafTerms;
	private TermID emanatingTerm;

	public AbstractGOGraphGenerationThread(Display display, Ontology graph, String dotCMDPath)
	{
		super(display, dotCMDPath);

		setSupport(this);
		this.graph = graph;
	}

	/**
	 * Sets the leaf terms. That are the terms inducing the graph.
	 *
	 * @param leafTerms
	 */
	public void setLeafTerms(HashSet<TermID> leafTerms)
	{
		this.leafTerms = leafTerms;
	}

	/**
	 * Sets the emanating term, i.e., the root of the resulting term.
	 *
	 * @param emanatingTerm
	 */
	public void setEmanatingTerm(TermID emanatingTerm)
	{
		this.emanatingTerm = emanatingTerm;
	}

	public final void writeDOT(File dotFile)
	{
		OntologyDotWriter.writeDOT(graph, dotFile, emanatingTerm, leafTerms, this);
	}
}
