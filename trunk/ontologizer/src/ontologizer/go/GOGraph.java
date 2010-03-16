package ontologizer.go;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import sonumina.collections.TinyQueue;
import sonumina.graph.DirectedGraph;
import sonumina.graph.Edge;
import sonumina.graph.DirectedGraph.IDistanceVisitor;

/**
 * An edge in the go graph
 * 
 * @author sba
 */
class GOEdge extends Edge<Term>
{
	/** Relation always to the parent (source) */
	private TermRelation relation;

	public void setRelation(TermRelation relation)
	{
		this.relation = relation;
	}
	
	public TermRelation getRelation()
	{
		return relation;
	}

	public GOEdge(Term source, Term dest, TermRelation relation)
	{
		super(source, dest);

		this.relation = relation;
	}
}

/**
 * Represents the whole GO Graph
 * 
 * @author Sebastian Bauer
 */
public class GOGraph implements Iterable<Term>
{
	private static Logger logger = Logger.getLogger(GOGraph.class.getCanonicalName());

	/** The graph */
	private DirectedGraph<Term> graph;

	/** We also pack a TermContainer */
	private TermContainer goTermContainer;

	/** The (possibly) artificial root term */
	private Term rootGOTerm;

	/** Level 1 terms */
	private List<Term> level1terms = new ArrayList<Term>();

	/** Available subsets */
	private HashSet <Subset> availableSubsets = new HashSet<Subset>();

	/**
	 * Construct the GO Graph.
	 * 
	 * @param goTermContainer
	 */
	public GOGraph(TermContainer goTermContainer)
	{
		this.goTermContainer = goTermContainer;

		graph = new DirectedGraph<Term>();

		/* At first add all goterms to the graph */
		for (Term goTerm : goTermContainer)
			graph.addVertex(goTerm);

		/* Now add the edges, i.e. link the terms */
		for (Term term : goTermContainer)
		{
			if (term.getSubsets() != null)
				for (Subset s : term.getSubsets())
					availableSubsets.add(s);

			for (ParentTermID parent : term.getParents())
			{
				/* Ignore loops */
				if (term.getID().equals(parent.termid))
				{
					logger.info("Detected self-loop in the definition of the ontology (term "+ term.getIDAsString()+"). This link has been ignored.");
					continue;
				}
				graph.addEdge(new GOEdge(goTermContainer.get(parent.termid), term, parent.relation));
			}
		}

		level1terms = new ArrayList<Term>(); 

		/* Find the terms without any ancestors */
		for (Term goTerm : graph)
		{
			if (graph.getInDegree(goTerm) == 0 && !goTerm.isObsolete())
				level1terms.add(goTerm);
		}

		if (level1terms.size() > 1)
		{
			StringBuilder level1StringBuilder = new StringBuilder();
			level1StringBuilder.append("\"");
			level1StringBuilder.append(level1terms.get(0).getName());
			level1StringBuilder.append("\"");
			for (int i=1;i<level1terms.size();i++)
			{
				level1StringBuilder.append(" ,\"");
				level1StringBuilder.append(level1terms.get(i).getName());
				level1StringBuilder.append("\"");
			}

			logger.info("Ontology contains multiple level-one terms: " + level1StringBuilder.toString() + ". Adding artificial root term.");

			rootGOTerm = new Term("GO:0000000", "root");
			rootGOTerm.setSubsets(new ArrayList<Subset>(availableSubsets));
			graph.addVertex(rootGOTerm);

			for (Term lvl1 : level1terms)
			{
				graph.addEdge(new GOEdge(rootGOTerm, lvl1,TermRelation.UNKOWN));
			}
		} else
		{
			if (level1terms.size() == 1)
			{
				logger.info("Ontology contains a single level-one term.");
				rootGOTerm = level1terms.get(0);
			}
		}
	}

	/**
	 * Determines whether the given id is the id of the (possible artifactial)
	 * root term
	 * 
	 * @return The root vertex as a GOVertex object
	 */
	public boolean isRootGOTermID(TermID id)
	{
		return id.equals(rootGOTerm.getID());
	}

	/**
	 * Get (possibly artificial) TermID of the root vertex of graph
	 * 
	 * @return The term representing to root
	 */
	public Term getRootGOTerm()
	{
		return rootGOTerm;
	}

	/**
	 * Returns all available subsets.
	 * 
	 * @return
	 */
	public Collection<Subset> getAvailableSubsets()
	{
		return availableSubsets;
	}

	/**
	 * Return the set of GO term IDs containing the given GO term's descendants.
	 * 
	 * @param goTerm
	 * @return the set of GOID strings of ancestors
	 */
	public Set<String> getTermsDescendantsAsStrings(String goTermID)
	{
		Term goTerm;
		if (goTermID.equals(rootGOTerm.getIDAsString()))
			goTerm = rootGOTerm;
		else
			goTerm = goTermContainer.get(goTermID);

		HashSet<String> terms = new HashSet<String>();
		Iterator<Edge<Term>> edgeIter = graph.getOutEdges(goTerm);
		while (edgeIter.hasNext())
			terms.add(edgeIter.next().getDest().getIDAsString());
		return terms;
	}

	/**
	 * Return the set of GO term IDs containing the given GO term's ancestors.
	 * 
	 * @param goTerm - the GOID as a string
	 * @return the set of GOID strings of descendants
	 */
	public Set<String> getTermsAncestorsAsStrings(String goTermID)
	{
		Term goTerm;
		if (goTermID.equals(rootGOTerm.getIDAsString()))
			goTerm = rootGOTerm;
		else
			goTerm = goTermContainer.get(goTermID);

		HashSet<String> terms = new HashSet<String>();

		Iterator<Edge<Term>> edgeIter = graph.getInEdges(goTerm);
		while (edgeIter.hasNext())
			terms.add(edgeIter.next().getSource().getIDAsString());
		return terms;
	}

	/**
	 * Return the set of GO term IDs containing the given GO term's descendants.
	 * 
	 * @param goTerm - the GOID as a TermID
	 * @return the set of GOIDs of the decendants as TermIDs
	 */
	public Set<TermID> getTermsDescendants(TermID goTermID)
	{
		Term goTerm;
		if (rootGOTerm.getID().id == goTermID.id)
			goTerm = rootGOTerm;
		else
			goTerm = goTermContainer.get(goTermID);

		HashSet<TermID> terms = new HashSet<TermID>();
		Iterator<Edge<Term>> edgeIter = graph.getOutEdges(goTerm);
		while (edgeIter.hasNext())
			terms.add(edgeIter.next().getDest().getID());
		return terms;
	}

	/**
	 * Return the set of GO term IDs containing the given GO term's ancestors.
	 * 
	 * @param goTerm
	 * @return the set of GO IDs of ancestors
	 */
	public Set<TermID> getTermsAncestors(TermID goTermID)
	{
		HashSet<TermID> terms = new HashSet<TermID>();
		if (rootGOTerm.getID().id == goTermID.id)
			return terms;

		Term goTerm;
		if (goTermID.equals(rootGOTerm.getIDAsString()))
			goTerm = rootGOTerm;
		else
			goTerm = goTermContainer.get(goTermID);

		Iterator<Edge<Term>> edgeIter = graph.getInEdges(goTerm);
		while (edgeIter.hasNext())
			terms.add(edgeIter.next().getSource().getID());
		return terms;
	}

	/**
	 * Return the set of GO term IDs containing the given GO term's ancestors.
	 * Includes the type of relationship. 

	 * @param destID
	 * @return
	 */
	public Set<ParentTermID> getTermsAncestorsWithRelation(TermID goTermID)
	{
		HashSet<ParentTermID> terms = new HashSet<ParentTermID>();
		if (rootGOTerm.getID().id == goTermID.id)
			return terms;

		Term goTerm;
		if (goTermID.equals(rootGOTerm.getIDAsString()))
			goTerm = rootGOTerm;
		else
			goTerm = goTermContainer.get(goTermID);

		Iterator<Edge<Term>> edgeIter = graph.getInEdges(goTerm);
		while (edgeIter.hasNext())
		{
			GOEdge t = (GOEdge)edgeIter.next();
			terms.add(new ParentTermID(t.getSource().getID(),t.getRelation()));
		}

		return terms;
	}

	
	/**
	 * Determines if there exists a directed path from sourceID to destID on the
	 * GO Graph.
	 * 
	 * @param sourceID
	 * @param destID
	 */
	public boolean existsPath(TermID sourceID, TermID destID)
	{
		/* Some special cases because of the artificial root */
		if (isRootGOTermID(destID))
		{
			if (isRootGOTermID(sourceID))
				return true;
			return false;
		}

		/*
		 * We walk from the destination to the source against the graph
		 * direction. Basically a breadth-depth search is done.
		 */

		/* TODO: Make this a method of DirectedGraph */
		Term source = goTermContainer.get(sourceID);
		Term dest = goTermContainer.get(destID);

		HashSet<Term> visited = new HashSet<Term>();

		LinkedList<Term> queue = new LinkedList<Term>();
		queue.offer(dest);
		visited.add(dest);

		while (!queue.isEmpty())
		{
			/* Remove head of the queue */
			Term head = queue.poll();

			/*
			 * Add not yet visited neighbours of old head to the queue and mark
			 * them as visited. If such a node is the source, return true
			 * (because than there exists a directed path between source and
			 * destination)
			 */
			Iterator<Edge<Term>> edgeIter = graph.getInEdges(head);
			while (edgeIter.hasNext())
			{
				Edge<Term> edge = edgeIter.next();
				Term ancestor = edge.getSource();

				if (ancestor == source)
					return true;

				if (!visited.contains(ancestor))
				{
					visited.add(ancestor);
					queue.offer(ancestor);
				}
			}
		}
		return false;
	}

	/**
	 * This interface is used as a callback mechanisim by the walkToRoot()
	 * method.
	 * 
	 * @see walkToRoot
	 * @author Sebastian Bauer
	 */
	public interface IVisitingGOVertex
	{
		/**
		 * Called for every Term vistited by the algorithm.
		 * 
		 * @param goTermID
		 */
		void visiting(TermID goTermID);
	};

	/**
	 * Starting at the vertex representing goTermID walk to the source of the
	 * DAG (ontology vertex) and call the method visiting of given object
	 * implementimg IVisitingGOVertex.
	 * 
	 * @param goTermID
	 *            the TermID to start with (note that visiting() is also called
	 *            for this vertex)
	 * 
	 * @param vistingVertex
	 */
	public void walkToSource(TermID goTermID, IVisitingGOVertex vistingVertex)
	{
		ArrayList<TermID> set = new ArrayList<TermID>(1);
		set.add(goTermID);
		walkToSource(set, vistingVertex);
	}

	/**
	 * Starting at the vertices within the goTermIDSet walk to the source of the
	 * DAG (ontology vertex) and call the method visiting of given object
	 * Implementing IVisitingGOVertex.
	 * 
	 * @param goTermIDSet
	 *            the set of go TermsIDs to start with (note that visiting() is
	 *            also called for those vertices/terms)
	 * 
	 * @param vistingVertex
	 */
	public void walkToSource(Collection<TermID> goTermIDSet,
			IVisitingGOVertex vistingVertex)
	{
		/*
		 * We walk from the destination to the source (ontology vertex) against
		 * the graph direction. Basically a breadth-first search is done.
		 * 
		 * TODO: Unification with exitsPath() method and make this a method of
		 * DirectedGraph.
		 */

		HashSet<Term> visited = new HashSet<Term>();

		/* Add all terms to the queue */
		TinyQueue<Term> queue = new TinyQueue<Term>();
		for (TermID id : goTermIDSet)
		{
			Term t = goTermContainer.get(id);
			assert (t != null);

			queue.offer(t);
			visited.add(t);
			vistingVertex.visiting(id);
		}

		while (!queue.isEmpty())
		{
			/* Remove head of the queue */
			Term head = queue.poll();

			/*
			 * Add not yet visited neighbours of old head to the queue and mark
			 * them as visited. Note that as we have a DAG with a single source
			 * (ontology vertex) we don't need to perform any checks about the
			 * source node
			 */

			if (true)
			{
				Object [] ancestors = graph.getAncestors(head);
				for (int i=0;i<ancestors.length;i++)
				{
					Term ancestor = (Term)ancestors[i];
					if (!visited.contains(ancestor))
					{
						visited.add(ancestor);
						queue.offer(ancestor);
						vistingVertex.visiting(ancestor.getID());
					}
				}
			} else
			{
				Iterator<Edge<Term>> edgeIter = graph.getInEdges(head);
				while (edgeIter.hasNext())
				{
					Edge<Term> edge = edgeIter.next();
					Term ancestor = edge.getSource();
	
					if (!visited.contains(ancestor))
					{
						visited.add(ancestor);
						queue.offer(ancestor);
						vistingVertex.visiting(ancestor.getID());
					}
				}
			}
		}
	}

	/**
	 * Starting at the vertices within the goTermIDSet walk to the sinks of the
	 * DAG and call the method visiting of given object implementing
	 * IVisitingGOVertex.
	 * 
	 * @param goTermID
	 *            the TermID to start with (note that visiting() is also called
	 *            for this vertex)
	 * 
	 * @param vistingVertex
	 */

	public void walkToSinks(TermID goTermID, IVisitingGOVertex vistingVertex)
	{
		ArrayList<TermID> set = new ArrayList<TermID>(1);
		set.add(goTermID);
		walkToSinks(set, vistingVertex);
	}

	/**
	 * Starting at the vertices within the goTermIDSet walk to the sinks of the
	 * DAG and call the method visiting of given object implementing
	 * IVisitingGOVertex.
	 * 
	 * @param goTermIDSet
	 *            the set of go TermsIDs to start with (note that visiting() is
	 *            also called for those vertices/terms)
	 * 
	 * @param vistingVertex
	 */
	public void walkToSinks(Collection<TermID> goTermIDSet,
			IVisitingGOVertex vistingVertex)
	{
		/* Implemented as breadth-first search */
		HashSet<Term> visited = new HashSet<Term>();

		/* Add all terms to the queue */
		TinyQueue<Term> queue = new TinyQueue<Term>();
		for (TermID id : goTermIDSet)
		{
			Term t = goTermContainer.get(id);
			assert (t != null);
			queue.offer(t);
			visited.add(t);
			vistingVertex.visiting(id);
		}

		while (!queue.isEmpty())
		{
			/* Remove head of the queue */
			Term head = queue.poll();

			/*
			 * Add not yet visited neighbours of old head to the queue and mark
			 * them as visited
			 */
			Iterator<Edge<Term>> edgeIter = graph.getOutEdges(head);
			while (edgeIter.hasNext())
			{
				Edge<Term> edge = edgeIter.next();
				Term successor = edge.getDest();

				if (!visited.contains(successor))
				{
					visited.add(successor);
					queue.offer(successor);
					vistingVertex.visiting(successor.getID());
				}
			}
		}
	}

	public TermContainer getGoTermContainer()
	{
		return goTermContainer;
	}

	/**
	 * Returns the term to a given term string or null.
	 * 
	 * @param term
	 * @return
	 */
	public Term getGOTerm(String term)
	{
		Term go = goTermContainer.get(term);
		if (go == null)
		{
			/* GO Term Container doesn't include the root term so we have to handle
			 * this case for our own.
			 */
			try
			{
				TermID id = new TermID(term);
				if (id.id == rootGOTerm.getID().id)
					return rootGOTerm;
			} catch (IllegalArgumentException iea)
			{
			}
		}
		return go;
	}

	public Term getGOTerm(TermID id)
	{
		Term go = goTermContainer.get(id);
		if (go == null && id.id == rootGOTerm.getID().id)
			return rootGOTerm;
		return go;
	}

	/**
	 * Returns a set of induced terms, that are the terms of the induced go graph.
	 * 
	 * @param rootTerm the root term (all terms up to this are included)
	 * @param term the inducing term.
	 * @return
	 */
	public Set<TermID> getTermsOfInducedGraph(TermID rootTerm, TermID term)
	{
		HashSet<TermID> nodeSet = new HashSet<TermID>();

		if (!nodeSet.contains(term))
		{
			/**
			 * Visitor which simply add all nodes to the nodeSet.
			 * 
			 * @author Sebastian Bauer
			 */
			class Visitor implements IVisitingGOVertex
			{
				public GOGraph graph;
				public TermID rootTerm;
				public HashSet<TermID> nodeSet;

				public void visiting(TermID goTermID)
				{
					if (rootTerm != null && !graph.isRootGOTermID(rootTerm))
					{
						/*
						 * Only add the goterm if there exists a path
						 * from the requested root term to the visited
						 * term.
						 * 
						 * TODO: Instead of existsPath() implement
						 * walkToGoTerm() to speed up the whole stuff
						 */
						if (rootTerm.equals(goTermID) || (!graph.isRootGOTermID(goTermID) && graph.existsPath(rootTerm, goTermID)))
							nodeSet.add(goTermID);
					} else
						nodeSet.add(goTermID);
				}
			};

			Visitor visitor = new Visitor();
			visitor.rootTerm = rootTerm;
			visitor.nodeSet = nodeSet;
			visitor.graph = this;

			walkToSource(term, visitor);
		}
		return nodeSet;
	}

	/**
	 * Returns all level 1 terms.
	 * 
	 * @return
	 */
	public Collection<Term> getLevel1Terms()
	{
		return level1terms;
	}

	/**
	 * Returns the parents shared by both t1 and t2.
	 * 
	 * @param t1
	 * @param t2
	 * @return
	 */
	public Collection<TermID> getSharedParents(TermID t1, TermID t2)
	{
		final Set<TermID> p1 = getTermsOfInducedGraph(null,t1);

		final ArrayList<TermID> sharedParents = new ArrayList<TermID>();
		walkToSource(t2, new IVisitingGOVertex()
		{
			public void visiting(TermID t2)
			{
				if (p1.contains(t2))
					sharedParents.add(t2);
			}
		});

		/* The unoptimized algorithm */
		if (false)
		{
			Set<TermID> p2 = getTermsOfInducedGraph(null,t2);
			p1.retainAll(p2);
			return p1;
		}
		
		return sharedParents; 
	}
	
	/**
	 * Determines all tupels of terms which all are unrelated, meaning that the
	 * terms are not allowed to be in the same lineage.
	 * 
	 * @param baseTerms
	 * @param tupelSize
	 * @return
	 */
	public ArrayList<HashSet<TermID>> getUnrelatedTermTupels(
			HashSet<TermID> baseTerms, int tupelSize)
	{
		ArrayList<HashSet<TermID>> unrelatedTupels = new ArrayList<HashSet<TermID>>();

		// TODO: Not sure what to implement here...
		
		return unrelatedTupels;
	}
	
	static public class GOLevels
	{
		private HashMap<Integer,HashSet<TermID>> level2terms = new HashMap<Integer,HashSet<TermID>>();
		private HashMap<TermID,Integer> terms2level = new HashMap<TermID,Integer>();
		
		private int maxLevel = -1;
		
		public void putLevel(TermID tid, int distance)
		{
			HashSet<TermID> levelTerms = level2terms.get(distance);
			if (levelTerms == null)
			{
				levelTerms = new HashSet<TermID>();
				level2terms.put(distance, levelTerms);
			}
			levelTerms.add(tid);
			terms2level.put(tid,distance);
			
			if (distance > maxLevel) maxLevel = distance;
		}

		/**
		 * Returns the level of the given term.
		 * 
		 * @param tid
		 * @return the level or -1 if the term is not included.
		 */
		public int getTermLevel(TermID tid)
		{
			Integer level = terms2level.get(tid);
			if (level == null) return -1;
			return level;
		}

		public Set<TermID> getLevelTermSet(int level)
		{
			return level2terms.get(level);
		}
		
		public int getMaxLevel()
		{
			return maxLevel;
		}
	};
	
	
	/**
	 * Returns the levels of the given terms starting from the root.
	 * 
	 * @param termids
	 * @return
	 */
	synchronized public GOLevels getGOLevels(final Set<TermID> termids)
	{
		final GOLevels levels = new GOLevels();
		
		graph.singleSourceLongestPath(rootGOTerm, new IDistanceVisitor<Term>()
				{
					public boolean visit(Term vertex, List<Term> path,
							int distance)
					{
						if (termids.contains(vertex.getID()))
							levels.putLevel(vertex.getID(),distance);
						return true;
					}});
		return levels;
	}

	/** Returns the number of terms in this ontology */
	public int numberOfTerms()
	{
		/* Don't forget the artificial term */
		return goTermContainer.termCount() + 1; 
	}

	/**
	 * Returns the highest term id used in this ontology.
	 * 
	 * @return
	 */
	public int maximumTermID()
	{
		int id=0;

		for (Term t : goTermContainer)
		{
			if (t.getID().id > id)
				id = t.getID().id;
		}

		return id;
	}

	/**
	 * Returns an iterator to iterate over all terms
	 */
	public Iterator<Term> iterator()
	{
		return goTermContainer.iterator();
	}

	private Subset relevantSubset;
	private Term relevantSubontology;

	/**
	 * Sets the relevant subset.
	 * 
	 * @param subsetName
	 */
	public void setRelevantSubset(String subsetName)
	{
		System.out.println(subsetName);

		for (Subset s : availableSubsets)
		{
			if (s.getName().equals(subsetName))
			{
				relevantSubset = s;
				return;
			}
		}
		
		relevantSubset = null;
		throw new IllegalArgumentException("Subset \"" + subsetName + "\" couldn't be found!");
	}

	/**
	 * Sets the relevant subontology.
	 * 
	 * @param subontologyName
	 */
	public void setRelevantSubontology(String subontologyName)
	{
		/* FIXME: That's so slow */
		for (Term t : goTermContainer)
		{
			if (t.getName().equals(subontologyName))
			{
				relevantSubontology = t;
				return;
			}
		}
		throw new IllegalArgumentException("Subontology \"" + subontologyName + "\" couldn't be found!");
	}
	
	/**
	 * Returns whether the given term is relevant (i.e., is contained in a relevant sub ontology and subset).
	 * 
	 * @param term
	 * @return
	 */
	public boolean isRelevantTerm(Term term)
	{
		if (relevantSubset != null)
		{
			boolean found = false;
			for (Subset s : term.getSubsets())
			{
				if (s.equals(relevantSubset))
				{
					found = true;
					break;
				}
			}
			if (!found) return false;
		}
		
		if (relevantSubontology != null)
		{
			if (term.getID().id != relevantSubontology.getID().id)
				if (!(existsPath(relevantSubontology.getID(), term.getID())))
					return false;
		}
		
		return true;
	}

	/**
	 * Returns whether the given term is relevant (i.e., is contained in a relevant sub ontology and subset).
	 * 
	 * @param goTermID
	 * @return
	 */
	public boolean isRelevantTermID(TermID goTermID)
	{
		Term t;
		if (isRootGOTermID(goTermID)) t = rootGOTerm;
		else t = goTermContainer.get(goTermID);
		
		return isRelevantTerm(t);
	}
	
}
