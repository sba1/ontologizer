package ontologizer.benchmark;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;

import ontologizer.GlobalPreferences;
import ontologizer.association.AssociationContainer;
import ontologizer.calculation.AbstractGOTermProperties;
import ontologizer.calculation.EnrichedGOTermsResult;
import ontologizer.calculation.ProbabilisticCalculation;
import ontologizer.calculation.b2g.B2GParam;
import ontologizer.calculation.b2g.Bayes2GOCalculation;
import ontologizer.calculation.b2g.Bayes2GOGOTermProperties;
import ontologizer.enumeration.GOTermEnumerator;
import ontologizer.go.Ontology;
import ontologizer.parser.AbstractItemParser;
import ontologizer.parser.IParserCallback;
import ontologizer.parser.ItemAttribute;
import ontologizer.parser.ParserFactory;
import ontologizer.set.PopulationSet;
import ontologizer.set.StudySet;
import ontologizer.statistics.None;
import ontologizer.types.ByteString;

public class Single {

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

		AbstractItemParser parser = ParserFactory.getNewInstance(new File("src/ontologizer/benchmark/yeast.study"));
		final StudySet studySet = new StudySet();

		parser.parse(new IParserCallback() {
			public void newEntry(ByteString gene, ItemAttribute attribute)
			{
				studySet.addGene(gene, attribute);
			}
		});

		final PopulationSet popSet = new PopulationSet();
		parser = ParserFactory.getNewInstance(new File("src/ontologizer/benchmark/yeast.population"));
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
		
		ProbabilisticCalculation propCalc = new ProbabilisticCalculation();
		propCalc.setDefaultP(0.33);
		propCalc.setDefaultQ(0.05);
		EnrichedGOTermsResult result = propCalc.calculateStudySet(graph, assoc, popSet, studySet, new None());
	}

}
