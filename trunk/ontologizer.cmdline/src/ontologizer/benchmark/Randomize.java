package ontologizer.benchmark;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import ontologizer.GlobalPreferences;
import ontologizer.OntologizerThreadGroups;
import ontologizer.association.AssociationContainer;
import ontologizer.calculation.AbstractGOTermProperties;
import ontologizer.calculation.EnrichedGOTermsResult;
import ontologizer.calculation.b2g.B2GParam;
import ontologizer.calculation.b2g.Bayes2GOCalculation;
import ontologizer.calculation.b2g.Bayes2GOGOTermProperties;
import ontologizer.enumeration.GOTermEnumerator;
import ontologizer.go.Ontology;
import ontologizer.go.TermID;
import ontologizer.parser.AbstractItemParser;
import ontologizer.parser.IParserCallback;
import ontologizer.parser.ItemAttribute;
import ontologizer.parser.ParserFactory;
import ontologizer.sampling.StudySetSampler;
import ontologizer.set.PopulationSet;
import ontologizer.set.StudySet;
import ontologizer.types.ByteString;

public class Randomize {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws IOException, InterruptedException
	{
		int numProcessors = Runtime.getRuntime().availableProcessors();

		GlobalPreferences.setProxyPort(888);
		GlobalPreferences.setProxyHost("realproxy.charite.de");

		String oboPath = "http://www.geneontology.org/ontology/gene_ontology_edit.obo";
		String assocPath = "http://cvsweb.geneontology.org/cgi-bin/cvsweb.cgi/go/gene-associations/gene_association.sgd.gz?rev=HEAD";

		AbstractItemParser parser = ParserFactory.getNewInstance(new File("yeast.study"));
		final StudySet studySet = new StudySet();

		parser.parse(new IParserCallback() {
			public void newEntry(ByteString gene, ItemAttribute attribute)
			{
				studySet.addGene(gene, attribute);
			}
		});

		final PopulationSet popSet = new PopulationSet();
		parser = ParserFactory.getNewInstance(new File("yeast.population"));
		parser.parse(new IParserCallback() {
			public void newEntry(ByteString gene, ItemAttribute attribute)
			{
				popSet.addGene(gene, attribute);
			}
		});

		Datafiles df = new Datafiles(oboPath,assocPath);
		final AssociationContainer assoc = df.assoc;
		final Ontology graph = df.graph;
		
		graph.setRelevantSubontology("biological_process");

		GOTermEnumerator popEnumerator = popSet.enumerateGOTerms(graph, assoc);
		GOTermEnumerator studyEnumerator = studySet.enumerateGOTerms(graph, assoc);
		
		ArrayList<ByteString> studyGenes = new ArrayList<ByteString>(studyEnumerator.getGenes());

		final int cutoff = studyGenes.size() * 9 / 10;
		
		final PrintWriter out = new PrintWriter(new File("randomized.txt"));
		
		out.print("term\t");
		out.print("marg\t");
		out.print("run\t");
		out.print("term.name\n");

		ExecutorService es = Executors.newFixedThreadPool(numProcessors - 1);
		
		for (int i=0;i<2000;i++)
		{
			Collections.shuffle(studyGenes);
			
			final int index = i;
			final StudySet randomizedStudySet = new StudySet("randomized");

			for (int j=0;j<cutoff;j++)
				randomizedStudySet.addGene(studyGenes.get(j),"");
			
			es.execute(new Runnable()
			{
				public void run()
				{
					
					Bayes2GOCalculation mgsa = new Bayes2GOCalculation();
					mgsa.setAlpha(B2GParam.Type.MCMC);
					mgsa.setBeta(B2GParam.Type.MCMC);
					mgsa.setExpectedNumber(B2GParam.Type.MCMC);
					mgsa.setBetaBounds(0,0.8);
					mgsa.setMcmcSteps(5000000);
		
					EnrichedGOTermsResult result = mgsa.calculateStudySet(graph, assoc, popSet, randomizedStudySet);
		
					synchronized (out)
					{
						for (AbstractGOTermProperties p : result)
						{
							Bayes2GOGOTermProperties b2gP = (Bayes2GOGOTermProperties)p;
							
							out.print(p.goTerm.getID().id + "\t");
							out.print(b2gP.marg + "\t");
							out.print(index + "\t");
							out.println("\"" + p.goTerm.getName() + "\"");
						}
					}
				}
			});
		}

		es.shutdown();
		while (!es.awaitTermination(60, TimeUnit.SECONDS));

		synchronized (out) {
			out.flush();
			out.close();
		}

		OntologizerThreadGroups.workerThreadGroup.interrupt();
	}

}
