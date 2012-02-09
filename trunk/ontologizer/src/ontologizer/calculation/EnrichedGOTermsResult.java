package ontologizer.calculation;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import ontologizer.GlobalPreferences;
import ontologizer.association.AssociationContainer;
import ontologizer.dotwriter.AbstractDotAttributesProvider;
import ontologizer.go.Ontology;
import ontologizer.go.Namespace;
import ontologizer.go.Term;
import ontologizer.go.TermID;
import ontologizer.go.Ontology.IVisitingGOVertex;
import ontologizer.set.StudySet;
import ontologizer.util.Util;

/**
 * This class is a container for all the results derived
 * from a term enrichment calculation.
 *
 * @author Sebastian Bauer
 */
public class EnrichedGOTermsResult extends AbstractGOTermsResult
{
	private static Logger logger = Logger.getLogger(EnrichedGOTermsResult.class.getCanonicalName());

	private int populationGeneCount;
	private int studyGeneCount;
	private StudySet studySet;
	
	private String calculationName;
	private String correctionName;

	/**
	 * 
	 * @param studySet
	 *            the study set where this result should belong to.
	 * @param populationGeneCount
	 *            number of genes of the populations (FIXME: This infact is
	 *            redundant)
	 */
	public EnrichedGOTermsResult(Ontology go, AssociationContainer associations, StudySet studySet, int populationGeneCount)
	{
		super(go, associations);

		this.populationGeneCount = populationGeneCount;
		this.studySet = studySet;
		this.studyGeneCount = studySet.getGeneCount();
	}

	/**
	 * Returns the name of the calculation method used for this
	 * result.
	 * 
	 * @return
	 */
	public String getCalculationName()
	{
		return calculationName;
	}

	/**
	 * Sets the name of the calculation method used for this
	 * result.
	 * 
	 * @param calculationName
	 */
	public void setCalculationName(String calculationName)
	{
		this.calculationName = calculationName;
	}

	/**
	 * Returns the name of the multiple test correction used
	 * for this result.
	 * 
	 * @return
	 */
	public String getCorrectionName()
	{
		return correctionName;
	}

	public void setCorrectionName(String correctionName)
	{
		this.correctionName = correctionName;
	}

	/**
	 * 
	 * @param file
	 */
	public void writeTable(File file)
	{
		if (list.isEmpty())
			return;

		try
		{
			logger.info("Writing to \"" + file.getCanonicalPath() + "\".");

			PrintWriter out = new PrintWriter(file);

			/* Write out the table header */
			AbstractGOTermProperties first = list.get(0);

			out.write(first.propHeaderToString());

			/* Place the result into an own list, so we can sort the results */
			ArrayList<AbstractGOTermProperties> propsList = new ArrayList<AbstractGOTermProperties>();
			for (AbstractGOTermProperties props : this)
				propsList.add(props);
			Collections.sort(propsList);

			/* Write out table contents */
			for (AbstractGOTermProperties props : propsList)
			{
				out.println(props.propLineToString(populationGeneCount,
						studyGeneCount));
			}

			out.flush();
			out.close();
			
			logger.info("\"" + file.getCanonicalPath() + "\"" + " successfully written.");
		} catch (IOException e)
		{
			logger.log(Level.SEVERE, "Exception occured when writing the table.", e);
		}
	}

	/**
	 * Returns the set of terms for which the all-subset minimal p-value is
	 * below the given cutoff. Those are the "good" terms.
	 * 
	 * @param pvalCutoff
	 *            the cutoff to use
	 * @return the list of good terms
	 */
	public HashSet<TermID> getGoodTerms(double pvalCutoff)
	{
		HashSet<TermID> goodTerms = new HashSet<TermID>();

		for (AbstractGOTermProperties goProp : this)
		{
			TermID curTerm = goProp.goTerm.getID();
			if (goProp.p_min < pvalCutoff)
				goodTerms.add(curTerm);
		}

		return goodTerms;
	}

	/**
	 * Writes out a basic dot file which can be used within graphviz. All terms
	 * of the terms parameter are included in the graph if they are within the
	 * sub graph originating at the rootTerm. In other words, all nodes
	 * representing the specified terms up to the given rootTerm node are
	 * included. 
	 * 
	 * @param graph
	 * @param file
	 * 			defines the file in which the output is written to.
	 * @param alpha
	 * 			defines the significance level. Used to colorize
	 *          nodes of significant terms.
	 * @param counts
	 *          if true, the nodes will be labeled with the gene
	 *          counts.
	 * @param rootTerm
	 *          defines the first term of the sub graph which should
	 *          be considered.
	 *
	 * @param terms
	 * 			defines which terms should be included within the
	 *          graphs.
	 */
	public void writeDOT(final Ontology graph, File file,
			final double alpha, final boolean counts, TermID rootTerm, HashSet<TermID> terms)
	{
		/* Build the props Array and count the number significant p values */
		int i = 0;
		int scount = 0;
		AbstractGOTermProperties propArray[] = new AbstractGOTermProperties[list.size()];
		for (AbstractGOTermProperties props : this)
		{
			propArray[i++] = props;
			if (props.p_adjusted < alpha)
				scount++;
		}
		Arrays.sort(propArray);

		final int significants_count = scount;

		/*
		 * Traverse through the sorted props remembering the GO Terms index
		 * in a hash
		 */
		final HashMap<Term, Integer> goTermRank = new HashMap<Term, Integer>();
		for (i = 0; i < propArray.length; i++)
			goTermRank.put(propArray[i].goTerm, i);

		writeDOT(graph, file, rootTerm, terms, new AbstractDotAttributesProvider()
		{
			public String getDotNodeAttributes(TermID id)
			{
				StringBuilder attributes = new StringBuilder();
				attributes.append("shape=\"box\",label=\"");

				if (graph.isRootTerm(id))
				{
					attributes.append("Gene Ontology");
				} else
				{
					attributes.append(id.toString());
					attributes.append("\\n");
					
					String label = graph.getTerm(id).getName();
					if (GlobalPreferences.getWrapColumn() != -1)
						label = Util.wrapLine(label,"\\n",GlobalPreferences.getWrapColumn());
					
					attributes.append(label);
				}

				AbstractGOTermProperties prop = getGOTermProperties(id);
				if (prop != null && counts)
				{
					attributes.append(String.format("\\n%d/%d, %d/%d",
							prop.annotatedPopulationGenes, populationGeneCount,
							prop.annotatedStudyGenes, studyGeneCount));
				}

				/* TODO: tip attribute */
				attributes.append("\"");
				
				if (prop != null && prop.p_adjusted < alpha)
				{
					/* A term is "extremal" if it is significant and no one of its children is significant */
					boolean isExtremal;

					class ExtremalVisitor implements IVisitingGOVertex
					{
						public boolean isExtremal = true;

						public boolean visited(Term term)
						{
							AbstractGOTermProperties subtermProp = getGOTermProperties(term.getID());
							if (subtermProp != null && subtermProp.p_adjusted < alpha)
								isExtremal = false;
							return true;
						}
					}

					ExtremalVisitor visitor = new ExtremalVisitor();
					graph.walkToSinks(graph.getTermChildren(id), visitor);
					isExtremal = visitor.isExtremal;

					float hue, saturation, brightness;

					/*
					 * Use the rank to determine the saturation We want that
					 * more significant nodes have more saturation, but we avoid
					 * having significant nodes with too less saturation (at
					 * least 0.2)
					 */
					int rank = goTermRank.get(prop.goTerm);
					assert (rank < significants_count);
					saturation = 1.0f - (((float) rank + 1) / significants_count) * 0.8f;

					/* Always full brightness */
					brightness = 1.0f;

					/* Hue depends on namespace */
					switch (Namespace.getNamespaceEnum(prop.goTerm.getNamespace()))
					{
						case BIOLOGICAL_PROCESS: hue = 120.f / 360; break;
						case MOLECULAR_FUNCTION: hue = 60.f / 360; break;
						case CELLULAR_COMPONENT: hue = 300.f / 360; 	break;
						default:
							hue = 0.f;
							saturation = 0.f;
							break;
					}

					String style = "filled,gradientfill";
					if (isExtremal) style += ",setlinewidth(3)";
					String fillcolor = String.format(Locale.US, "%f,%f,%f", hue, saturation, brightness);
					attributes.append(",style=\""+ style + "\",color=\"white\",fillcolor=\"" + fillcolor + "\"");
				}
				return attributes.toString();
			}
		});
	}


	/**
	 * Writes out a basic dot file which can be used within graphviz.
	 * 
	 * @param graph
	 * @param file
	 * @param thresh
	 * @param counts
	 * @param rootTerm
	 */
	public void writeDOT(Ontology graph, File file, double thresh, boolean counts, TermID rootTerm)
	{
		HashSet<TermID> nodes = new HashSet<TermID>();
		
		for(AbstractGOTermProperties props : this)
		{
			if (props.isSignificant(thresh))
				nodes.add(props.goTerm.getID());
		}
		
		writeDOT(graph,file,thresh,counts,rootTerm,nodes);
	}
	
	public Iterator<AbstractGOTermProperties> iterator()
	{
		return list.iterator();
	}

	/**
	 * Returns the studyset where these results are belonging to.
	 * 
	 * @return the study set.
	 */
	public StudySet getStudySet()
	{
		return studySet;
	}

	public int getPopulationGeneCount()
	{
		return populationGeneCount;
	}

	public int getStudyGeneCount()
	{
		return studyGeneCount;
	}

}
