/**
 * 
 */
package ontologizer.playground;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import ontologizer.StudySetResultList;
import ontologizer.association.AssociationContainer;
import ontologizer.association.AssociationParser;
import ontologizer.calculation.AbstractGOTermProperties;
import ontologizer.calculation.EnrichedGOTermsResult;
import ontologizer.calculation.ICalculation;
import ontologizer.calculation.ParentChildCalculation;
import ontologizer.go.Ontology;
import ontologizer.go.OBOParser;
import ontologizer.go.OBOParserException;
import ontologizer.go.TermContainer;
import ontologizer.set.PopulationSet;
import ontologizer.set.StudySet;
import ontologizer.set.StudySetList;
import ontologizer.statistics.TestCorrectionRegistry;
import ontologizer.types.ByteString;

/**
 * @author grossman
 *
 */
public class SteffenExec
{

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException
	{
// TODO: Remove local dependencies
		String GOpath = "/project/gene-regulation/public_datasets/GO/050728/";
		//String GOpath = "/home/grossman/project/GO/050728/";
		
		/*
		 * reading in an OBO file
		 */
		String obofilename =
			GOpath + "gene_ontology.obo";

		System.out.println("Reading in OBO file" + obofilename);
		OBOParser oboParser = new OBOParser(obofilename);
		try
		{
			System.out.println(oboParser.doParse());
		TermContainer goTermCont = new TermContainer(oboParser.getTermMap(), oboParser.getFormatVersion(), oboParser.getDate());

		/*
		 * building GO graph from terms
		 */
		System.out.println("Building graph");
		Ontology goGraph = new Ontology(goTermCont);
		
		/*
		 * Getting annotations
		 */
		String associationFile =
			GOpath + "gene_association_test.goa_human";
		/* Parse the GO association file containing GO annotations for genes or gene
		 * products. Results are placed in associationparser.
		 */
		AssociationParser ap =
			new AssociationParser(
					associationFile,
					goTermCont,
					null
			);
		AssociationContainer goAssociations =
			new AssociationContainer(
					ap.getAssociations(),
					ap.getSynonym2gene(),
					ap.getDbObject2gene()					
			);
		
		
		/*
		 * Building a PopulationSet from all genes for which we have annotation
		 */
		PopulationSet allAnnotated = new PopulationSet("all annotated genes");
		for (ByteString gene : goAssociations.getAllAnnotatedGenes()) {
			allAnnotated.addGene(gene, "NA");
		}
		int nAnnotated = allAnnotated.getGeneCount();
		System.out.println("We have a total of " + nAnnotated + " genes in our Population Set.");
		
		/*
		 * Building a StudySet by sampling 10% of genes from the PopulationSet
		 */
		int SSsize =  (int) (0.1 * nAnnotated);
		int SSnum = 10;
		
		System.out.println("We generate "
				+ SSnum + " StudySets of "
				+ SSsize + " genes each");
		
		StudySetList SSL = new StudySetList("Some random StudySets");

		for (int i = 0; i < SSnum; i++)
		{
			System.out.println("Generating StudySet No." + i);
			StudySet SS = allAnnotated.generateRandomStudySet(SSsize);
			SSL.addStudySet(SS);
		}
		
		
		/*
		 * Now we try to make a calculation
		 */
		//ICalculation calc = new SingleTermCalculation();
		//ICalculation calc = new ChiSquareCalculation();
		ICalculation calc = new ParentChildCalculation();

		
		StudySetResultList allresults = new StudySetResultList();
		for (StudySet studySet : SSL)
		{
			allresults.addStudySetResult(calc.calculateStudySet(
					goGraph,
					goAssociations,
					allAnnotated,
					studySet,
					TestCorrectionRegistry.getDefault()));
		}
		
		String outFileName =
			GOpath + "Testout.txt";
		File outFile = new File(outFileName);
		FileWriter out = new FileWriter(outFile);
		
		boolean noHeaderYet = true;
		
		for (EnrichedGOTermsResult result : allresults)
		{
			int popGeneCount = result.getPopulationGeneCount();
			int studGeneCount = result.getStudyGeneCount();
			
			if (noHeaderYet) 
			{
				// we basically need the first element, but I don't no any other way to get it
				for (AbstractGOTermProperties prop : result)
				{
					out.write(prop.propHeaderToString());
					noHeaderYet = false;
					break;
				}
			}
			
			for (AbstractGOTermProperties prop : result)
			{
				out.write(prop.propLineToString(popGeneCount, studGeneCount));
			}
			
		}
		
/*		// A file to hold some results
		String outFileName =
			GOpath + "Testout.txt";
		File outFile = new File(outFileName);
		FileWriter outFileWriter = new FileWriter(outFile);
		
		for (String term : goTerms) {
			// getting name
			String goName = goTermCont.getGOName(term);
			// getting children
			Set<String> children = goGraph.getTermsDescendants(term);
			
			String line = term + "\t'"
				+ goName + "'\t"
				+ children.size() + "\n";
			
			//System.out.println("Term: " + term + " Name: " + goName + " Children: " + children.size());
			//System.out.println(line);
			outFileWriter.write(line);
			
		}
		outFileWriter.close();
*/		
		System.out.println("Done!");
		} catch (OBOParserException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
