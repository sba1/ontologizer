/*
 * Created on 08.02.2007
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ontologizer.calculation;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import ontologizer.association.AssociationContainer;
import ontologizer.dotwriter.AbstractDotAttributesProvider;
import ontologizer.dotwriter.GODOTWriter;
import ontologizer.go.Ontology;
import ontologizer.go.Term;
import ontologizer.go.TermID;

/**
 * An abstraction of any result containing GO terms utilizing
 * AbstractGOTermProperties.
 * 
 * @author Sebastian Bauer
 */
public class AbstractGOTermsResult implements Iterable<AbstractGOTermProperties>
{
	/** A linear list containing properties for go terms */
	protected ArrayList<AbstractGOTermProperties> list = new ArrayList<AbstractGOTermProperties>();

	/** Maps the go term to an integer (for accesses in constant time) */
	private HashMap<Term,Integer> go2Index = new HashMap<Term,Integer>();

	/** The current index for adding a new go term property */
	private int index = 0;

	/** The GO Graph */
	protected Ontology go;
	
	/** The association container */
	private AssociationContainer associations;
	
	/**
	 * Constructor. Needs to know the go structure as well as the associations of the genes.
	 * 
	 * @param newGO
	 * @param newAssociations
	 */
	public AbstractGOTermsResult(Ontology newGO, AssociationContainer newAssociations)
	{
		this.go = newGO;
		this.associations = newAssociations;
	}

	/**
	 * Returns the iterator for receiving all go term properties.
	 */
	public Iterator<AbstractGOTermProperties> iterator()
	{
		return list.iterator();
	}
	
	/**
	 * 
	 * @param prop
	 */
	public void addGOTermProperties(AbstractGOTermProperties prop)
	{
		if (prop.goTerm == null)
			throw new IllegalArgumentException("prop.goTerm mustn't be null");

		list.add(prop);
		Integer integer = new Integer(index);
		go2Index.put(prop.goTerm, integer);
		index++;
	}

	/**
	 * Return the go term properties for the given term.
	 * 
	 * @param goID
	 * @return
	 */
	public AbstractGOTermProperties getGOTermProperties(TermID goID)
	{
		Integer index = go2Index.get(go.getTerm(goID));
		if (index == null)
			return null;
		return list.get(index);
	}

	/**
	 * Returns the result of the given goTerm.
	 * 
	 * @param term
	 * @return
	 */
	public AbstractGOTermProperties getGOTermProperties(Term term)
	{
		Integer idx = go2Index.get(term);
		if (idx == null) return null;
		return list.get(idx);
	}

	/**
	 * Return the assigned associations.
	 * 
	 * @return
	 */
	public AssociationContainer getAssociations()
	{
		return associations;
	}

	/**
	 * Returns the size of the result list, i.e., through how many
	 * elements you can iterate.
	 *  
	 * @return
	 */
	public int getSize()
	{
		return list.size();
	}

	/**
	 * Returns the associated GO graph.
	 * 
	 * @return
	 */
	public Ontology getGO()
	{
		return go;
	}

	/**
	 * Writes out a basic dot file which can be used within graphviz. All terms
	 * of the terms parameter are included in the graph if they are within the
	 * sub graph originating at the rootTerm. In other words, all nodes
	 * representing the specified terms up to the given rootTerm node are
	 * included. 
	 * 
	 * @param goTerms
	 * @param graph
	 * @param file
	 * 			defines the file in which the output is written to.
	 * @param rootTerm
	 *          defines the first term of the sub graph which should
	 *          be considered.
	 *
	 * @param terms
	 * 			defines which terms should be included within the
	 *          graphs.
	 * @param provider
	 *          should provide for every property an appropiate id.
	 */
	public void writeDOT(Ontology graph, File file, TermID rootTerm, HashSet<TermID> terms, AbstractDotAttributesProvider provider)
	{
		if (list.isEmpty())
			return;
		
		GODOTWriter.writeDOT(graph, file, rootTerm, terms, provider);
	}
}
