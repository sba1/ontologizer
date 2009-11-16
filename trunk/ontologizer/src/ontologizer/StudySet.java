package ontologizer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ontologizer.GOTermEnumerator.GOTermAnnotatedGenes;
import ontologizer.association.Association;
import ontologizer.association.AssociationContainer;
import ontologizer.association.Gene2Associations;
import ontologizer.go.GOGraph;
import ontologizer.go.TermContainer;
import ontologizer.go.TermID;
import ontologizer.go.GOGraph.IVisitingGOVertex;
import ontologizer.parser.IGeneNameParser;
import ontologizer.parser.OneOnALineParser;
import ontologizer.parser.ParserFactory;
import ontologizer.sampling.StudySetSampler;

/**
 * This class holds all gene names of a study and their associated
 * (optional) descriptions. The names are extracted directly by the
 * constructor given study file.
 *
 * The class implements the Iterable interface so you can
 * conveniently iterate over all includes gene names.
 *
 * @author Peter Robinson, Sebastian Bauer
 */
public class StudySet implements Iterable<ByteString>
{
	private static Logger logger = Logger.getLogger(StudySet.class.getCanonicalName());

	/**
	 * HashMap containing the names of genes (or gene products) of the study
	 * and their optional description.
	 */
	private HashMap<ByteString, String> gene2Description;

	/**
	 * List containing genes which are not annotated
	 */
	private LinkedList<ByteString> unannotatedGeneNames = new LinkedList<ByteString>();

	/** The name of the study set */
	private String name;

	/** Cached GOTermCounter */
	private GOTermCounter goTermCounter;
	
	/** Cached GOTermEnumerator */
	private GOTermEnumerator goTermEnumerator;

	/** The current random ID. Used for unique study set names */
	private int randomID = 0;

	/**
	 * Constructs a study set with genes listed in the specified
	 * file.
	 * 
	 * @param file
	 *  specifies the file (a simple list of strings or FASTA format)
	 *  where the names are extracted from.
	 */
	public StudySet(File file) throws FileNotFoundException, IOException
	{
		/* We use the fileName as the name of the study set with path components
		 * removed */
		name = file.getName();
		
		// Removing suffix from filename
		Pattern suffixPat = Pattern.compile("\\.[a-zA-Z0-9]+$");
		Matcher m = suffixPat.matcher(name);
		name = m.replaceAll("");

		IGeneNameParser parser = ParserFactory.getNewInstance(file);
		
		logger.info("Processing studyset " + file.toString());

		gene2Description = parser.getNames();
	}

	/**
	 * Construct an empty study set with the given name.
	 * 
	 * @param name specifies the name of the studyset
	 * 
	 */
	public StudySet(String name)
	{
		this.name = name;
		gene2Description = new HashMap<ByteString,String>();
	}

	public StudySet()
	{
		this.name = generateUniqueName();
		gene2Description = new HashMap<ByteString,String>();
	}
	
	public StudySet(String name, String [] entries)
	{
		this.name = name;
		gene2Description = new HashMap<ByteString,String>();
		OneOnALineParser parser = new OneOnALineParser(entries);
		gene2Description = parser.getNames();
	}

	/**
	 * Obtain the number of genes or gene products within this studyset.
	 * 
	 * @return the desired count.
	 */
	public int getGeneCount()
	{
		return gene2Description.size();
	}

	/**
	 * @return name of the Study
	 */
	public String getName()
	{
		return name;
	}

	/* for debugging */
	public String toString()
	{
		String str = name + " (n=" + (getGeneCount()) + ")";
		return str;
	}

	/**
	 * The iterator over gene names considered within this study
	 */
	public Iterator<ByteString> iterator()
	{
		return gene2Description.keySet().iterator(); 
	}
	
	/**
	 * Returns the genes in order of the iterator as an array.
	 * 
	 * @return
	 */
	public ByteString [] getGenes()
	{
		ByteString [] genes = new ByteString[gene2Description.size()];
		int i=0;
		for (ByteString gene : this)
			genes[i++]=gene;
		return genes;
	}

	/**
	 * Returns the gene description of the specified gene.
	 * 
	 * @return the gene description or null if the gene is not contained.
	 */
	public String getGeneDescription(ByteString name)
	{
		return gene2Description.get(name);
	}

	/**
	 * After all the calculation has been done, you should
	 * call this method in order to allow the garbage
	 * collector to free the enumerator and counter associated
	 * memory.
	 */
	public void resetCounterAndEnumerator()
	{
		goTermCounter = null;
		goTermEnumerator = null;
	}

	/**
	 * Add an additional gene to the population set.
	 * 
	 * NOTE: After a gene has been added, countGOTerms() should
	 * be called again.
	 * 
	 * @param geneName
	 * @param description
	 */
	public void addGene(ByteString geneName, String description)
	{
		gene2Description.put(geneName,description);
		resetCounterAndEnumerator();
	}

	/**
	 * Checks whether study set contains the given gene.
	 * 
	 * @param geneName
	 * 		  the name of the gene which inclusion should be checked.
	 * @return true if study contains gene, else false.
	 */
	public boolean contains(ByteString geneName)
	{
		return gene2Description.containsKey(geneName);
	}

	/**
	 * Returns a hashset containing all the gene names represented
	 * by this study set. Note that the returned hashset is decoupled
	 * from the study set, so any change within the hashset doesn't
	 * have any effect on the study set's genes.
	 *  
	 * @return
	 */
	public HashSet<ByteString> getAllGeneNames()
	{
		HashSet<ByteString> geneSet = new HashSet<ByteString>();
		for (ByteString term : this)
			geneSet.add(term);
		return geneSet;
	}
	
	/**
	 * Some datasets contain gene entries which refer to the same gene
	 * but differ in their name (i.e. synonyms). This method filters
	 * them out by using the association container.
	 *
	 * @param associationContainer
	 */
	public void filterOutDuplicateGenes(AssociationContainer associationContainer)
	{
		/* This will be filled with unique genes */
		HashMap<ByteString,String> uniqueGenes = new HashMap<ByteString,String>();

		for (ByteString geneName : gene2Description.keySet())
		{
			Gene2Associations gene2Association = associationContainer.get(geneName);
			if (gene2Association != null)
			{
				boolean add;
				String desc = uniqueGenes.get(gene2Association.name());
				
				if (!(add = (desc == null)))
				{
					/* We prefer the longer description */
					String newDesc = gene2Description.get(geneName); 
					add = newDesc != null && newDesc.length() > desc.length();
				}
				
				if (add)
					uniqueGenes.put(gene2Association.name(),desc);
			} else
			{
				/* We don't want to filter out genes without an association here */
				uniqueGenes.put(geneName,gene2Description.get(geneName));
			}
		}

		if (uniqueGenes.size() != gene2Description.size())
		{
			logger.info((gene2Description.size() - uniqueGenes.size()) + " duplicate gene entries have been filtered out");
			gene2Description = uniqueGenes;
		}

		/* Reset counter and enumerator */
		this.resetCounterAndEnumerator();
	}
	
	/**
	 * Filters out genes which don't contain an association
	 * 
	 * @param associationContainer
	 */
	public void filterOutAssociationlessGenes(AssociationContainer associationContainer)
	{
		/* Iterate over all gene names and put those who doesn't have an association
		 * into the unannotatedGeneNames list */
		for (ByteString geneName : gene2Description.keySet())
		{
			Gene2Associations gene2Association = associationContainer.get(geneName);
			if (gene2Association == null)
			{
				unannotatedGeneNames.add(geneName);
			}
		}
		
		/* Now remove them really (we can't do this in the former loop, because
		 * it is unclear whether this will conflict with the iterating) */
		for (ByteString geneName : unannotatedGeneNames)
			gene2Description.remove(geneName);

		logger.info(unannotatedGeneNames.size() + " genes of " + getName() + " without any association have been filtered out. Now there are " + gene2Description.size() + " genes"); 

		/* Reset counter and enumerator */
		this.resetCounterAndEnumerator();
	}
	
	/**
	 * Count the number of genes annotated for every term. Multiple
	 * calls to this method are fast, if the gene set has not
	 * been changed inbetween.
	 *  
	 * @param goTerms
     * @param associationContainer
	 */
	public synchronized GOTermCounter countGOTerms(GOGraph graph, AssociationContainer associationContainer)
	{
		/* Return cached enumerator if available */
		if (goTermCounter != null) return goTermCounter;

		goTermCounter =  new GOTermCounter(graph);

		/* Iterate over all gene names and add their annotations to the goTermCounter */
		for (ByteString geneName : gene2Description.keySet())
		{
			Gene2Associations gene2Association = associationContainer.get(geneName);
			if (gene2Association != null)
				goTermCounter.add(gene2Association.getAssociations());
		}
		
		return goTermCounter;
	}
	
	/**
	 * Enumerate genes annotated for every term. Multiple
	 * calls to this method are fast, if the gene set has not
	 * been changed inbetween.
	 *  
	 * @param goTerms
     * @param associationContainer
	 */
	public synchronized GOTermEnumerator enumerateGOTerms(GOGraph graph, AssociationContainer associationContainer)
	{
		/* Return cached enumerator if available */
		if (goTermEnumerator != null) return goTermEnumerator;

		goTermEnumerator =  new GOTermEnumerator(graph);

		/* Iterate over all gene names and add their annotations to the goTermCounter */
		for (ByteString geneName : gene2Description.keySet())
		{
			Gene2Associations geneAssociations = associationContainer.get(geneName);
			if (geneAssociations != null) goTermEnumerator.push(geneAssociations);
		}

		return goTermEnumerator;
	}

	/**
	 * Generates an unique name derived from the study sets' name
	 * @return
	 */
	private String generateUniqueName()
	{
		String name = getName() + "-random-" + randomID;
		randomID++;
		return name;
	}
	
	/**
	 * Generate a studyset which contains desiredSize random
	 * selected genes of the population.
	 * 
	 * @param desiredSize specifies the desired size of
	 *        the studyset.
	 *
	 * @return the generated random studyset.
	 */
	public StudySet generateRandomStudySet(int desiredSize)
	{		
		StudySetSampler sampler = new StudySetSampler(this);
		
		return sampler.sampleRandomStudySet(desiredSize);
	}

	/**
	 * Given an array of study sets this method creates new studysets
	 * where the genes have been shuffled across the studysets. 
	 * 
	 * @param studySetArray
	 * @return
	 */
	public static StudySet [] generateShuffledStudySets(StudySet [] studySetArray)
	{
		int i,j,k;

		StudySet [] array = new StudySet[studySetArray.length];

		/* Determine number of genes (we allow duplicates) */
		int genes = 0;
		for (i=0;i<studySetArray.length;i++)
			genes += studySetArray[i].getGeneCount();
		
		/* Now build an array of all genes */
		ByteString [] genesArray = new ByteString[genes];
		String [] descriptionArray = new String[genes];
		for (i=0,j=0;i<studySetArray.length;i++)
		{
			for (ByteString gene : studySetArray[i])
			{
				genesArray[j] = gene;
				descriptionArray[j] = studySetArray[i].getGeneDescription(gene);
				j++;
			}
		}
		
		/* Shuffle the array. This is a naive implementation, and it could
		 * be that not all permutations are equally likely.
		 * 
		 * TODO: We should research the mentioned issue
		 */
        for (j=0; j<genes; j++)
        {
        	/* generate a random position number */
        	int r = (int)(Math.random()*genes);
        	assert(r < genes);

        	/* exchange the element at the random number with the current one */
        	ByteString temp = genesArray[j];
        	genesArray[j] = genesArray[r];
        	genesArray[r] = temp;

        	String tempD = descriptionArray[j];
        	descriptionArray[j] = descriptionArray[r];
        	descriptionArray[r] = tempD;
        }

		/* Build new studysets */
		for (i=0,j=0;i<studySetArray.length;i++)
		{
			StudySet newStudySet = new StudySet(studySetArray[i].generateUniqueName()); 
			for (k=0;k<studySetArray[i].getGeneCount();k++,j++)
				newStudySet.addGene(genesArray[j],descriptionArray[j]);
			array[i] = newStudySet; 
		}
		
		/* FIXME: The current studyset implementation cannot hold duplicates of genes.
		 * However this might happen, if gene lists which aren't distinct are shuffled
		 * randomly arround. Eighter we ensure that this doesn't happen, gene lists
		 * have to be distinct or we allow duplicates of genes within the studyset
		 */
		return array;
	}

	/**
	 * Write minimum subsumer matrix.
	 *
	 * @param graph
	 * @param associations
	 * @param file
	 */
	public void writeMinimumSubsumerMatrix(final GOGraph graph,  AssociationContainer associations, File file)
	{
		GOTermEnumerator enumerator = goTermEnumerator;

		/* If terms weren't already annotated do it now, but
		 * remove the local reference then
		 */
		if (enumerator == null)
		{
			enumerator = enumerateGOTerms(graph,associations);
			goTermEnumerator = null;
		}
		
		class ParentFetcher implements IVisitingGOVertex
		{
			private HashSet<TermID> set = new HashSet<TermID>();


			public void visiting(TermID goTermID)
			{

//	TODO:
//				if (goTermID.equals(graph.getBpTerm().getID())) return;
//				if (goTermID.equals(graph.getMfTerm().getID())) return;
//				if (goTermID.equals(graph.getCcTerm().getID())) return;
				if (graph.isRootGOTermID(goTermID)) return;

				set.add(goTermID);
			}
			
			public HashSet<TermID> getSet()
			{
				return set;
			}
		};

		ArrayList<TermID> terms = new ArrayList<TermID>();
		for (TermID t : enumerator)
		{
			if (!graph.isRootGOTermID(t))
				terms.add(t);
		}

		int totalTerms = terms.size();
		int [][] matrix = new int[totalTerms][totalTerms];
		
		for (int i=0;i<totalTerms;i++)
		{
			for (int j=0;j<totalTerms;j++)
			{
				TermID ti = terms.get(i);
				TermID tj = terms.get(j);

				ParentFetcher pi = new ParentFetcher();
				ParentFetcher pj = new ParentFetcher();

				graph.walkToSource(ti,pi);
				graph.walkToSource(tj,pj);

				HashSet<TermID> sharedParents = new HashSet<TermID>();
				for (TermID t : pi.getSet())
				{
					if (pj.getSet().contains(t))
						sharedParents.add(t);
				}
				
				for (TermID t : pj.getSet())
				{
					if (pi.getSet().contains(t))
						sharedParents.add(t);
				}

				int min = Integer.MAX_VALUE;

				for (TermID t : sharedParents)
				{
					int c = enumerator.getAnnotatedGenes(t).totalAnnotatedCount();
					if (c < min) min = c;
				}

				matrix[i][j] = min;
			}
		}

		try
		{
			BufferedWriter out = new BufferedWriter(new FileWriter(file));

			out.write("GOID");
			for (int i=0;i<terms.size();i++)
			{
				out.write("\t");
				out.write(terms.get(i).toString());
			}

			for (int i=0;i<matrix.length;i++)
			{
				out.write(terms.get(i).toString());
				
				for (int j=0;j < matrix[i].length;j++)
				{
					out.write("\t");
					out.write(matrix[i][j] + "");
				}
				out.write("\n");
			}
			out.close();
		} catch (IOException e)
		{
		}

	}
	
	/**
	 * Write out a list of all terms and the names of the genes annotated to them.
	 * 
	 * @param graph
	 * @param associations
	 * @param file
	 */
	public void writeTermAnnotatedGenes(GOGraph graph, AssociationContainer associations, File file)
	{
		GOTermEnumerator enumerator = goTermEnumerator;

		/* If terms weren't already annotated do it now, but
		 * remove the local reference then
		 */
		if (enumerator == null)
		{
			enumerator = enumerateGOTerms(graph,associations);
			goTermEnumerator = null;
		}
		
		try
		{
			BufferedWriter out = new BufferedWriter(new FileWriter(file));
			
			for (TermID id : enumerator)
			{
				out.write(id.toString());
				out.write('\t');
				out.write("genes={");
				GOTermAnnotatedGenes genes = enumerator.getAnnotatedGenes(id);
				boolean first = true;
				for (ByteString gene : genes.totalAnnotated)
				{
					if (!first) out.write(',');
					else first = false;
					out.write(gene.toString());
				}
				out.write("}");
			}
			out.flush();
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * 
	 * @param graph specifies the graph.
	 * @param associations specifies the associations
	 * @param file defines the file to which is written to
	 */
	public void writeSetWithAnnotations(GOGraph graph, AssociationContainer associations, File file)
	{
		try
		{
			BufferedWriter out = new BufferedWriter(new FileWriter(file));

			for (ByteString gene : this)
			{
				/* gene name */
				out.write(gene.toString());
				out.write('\t');
				
				/* description */
				String desc = this.getGeneDescription(gene); 
				if (desc != null)
					out.write(desc);
				out.write('\t');
				
				Gene2Associations geneAssociations = associations.get(gene);
				if (geneAssociations != null)
				{
					final HashSet<TermID> direct = new HashSet<TermID>();
					final HashSet<TermID> indirect = new HashSet<TermID>();

					/* direct associations */
					boolean first = true;
					out.write("annotations={");
					for (Association assoc : geneAssociations)
					{
						if (first == false) out.write(',');
						else first = false;
						out.write(assoc.getTermID().toString());

						direct.add(assoc.getTermID());
					}
					out.write("}");

					/* indirect associations are those located upstream (nearer to the root) */
					graph.walkToSource(direct, new IVisitingGOVertex()
					{
						public void visiting(TermID goTermID)
						{
							if (!direct.contains(goTermID))
								indirect.add(goTermID);
						}
					});

					out.write(" parental_annotations={");
					first = true;
					for (TermID t : indirect)
					{
						if (first == false) out.write(',');
						else first = false;
						out.write(t.toString());
					}
					out.write("}");
				}

				
				out.write('\n');
			}
			out.flush();
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Applies the given filter to this study set. Only simple mapping
	 * is supported for now. Genes which have no entry withing the map
	 * will remain unaffected while genes which map to "-" will be
	 * discarded.
     *
	 * @param filter specifies the filter object to use 
	 */
	public void applyFilter(GeneFilter filter)
	{
		int unmappedGenes = 0;
		int mappedGenes = 0;
		int discaredGenes = 0;

		resetCounterAndEnumerator();

		HashMap<ByteString, String> newGeneNames = new HashMap<ByteString,String>();
		
		for (Entry<ByteString,String> entry : gene2Description.entrySet())
		{
			ByteString newName = filter.mapGene(entry.getKey());

			/* If no mapping exists we'll accept the original gene name */
			if (newName == null)
			{
				newGeneNames.put(entry.getKey(),entry.getValue());
				unmappedGenes++;
			}
			else
			{
				/* Decide whether gene should be discarded or mapped */
				if (!newName.equals("-"))
				{
					/* It's possible that more than one gene map to another
					 * but we wouldn't like to loss the information */
					String val = newGeneNames.get(newName);
					if (val == null) val = "";
					else val += ",";

					val += "mapped_from=" + entry.getKey();
					if (entry.getValue() != null && entry.getValue().trim().length()>0)
						val += ",comment="+entry.getValue();

					newGeneNames.put(newName,val);
					mappedGenes++;
				} else discaredGenes++;
			}
		}

		System.err.println("In studyset " + getName() + " mapped " + mappedGenes + " to new genes, "
					+ unmappedGenes + " remained unaffected, "
					+ discaredGenes + " were discarded");

		gene2Description = newGeneNames; 
	}

	public void removeGenes(Collection<ByteString> toBeRemoved)
	{
		for (ByteString g : toBeRemoved)
			gene2Description.remove(g);
	}

	public void addGenes(Collection<ByteString> toBeAdded)
	{
		for (ByteString g : toBeAdded)
			gene2Description.put(g,"");
	}
}
