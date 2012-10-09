package ontologizer.playground;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;

import ontologizer.association.AssociationContainer;
import ontologizer.association.AssociationParser;
import ontologizer.enumeration.GOTermEnumerator;
import ontologizer.enumeration.GOTermEnumerator.GOTermAnnotatedGenes;
import ontologizer.go.Ontology;
import ontologizer.go.OBOParser;
import ontologizer.go.OBOParserException;
import ontologizer.go.TermContainer;
import ontologizer.go.TermID;
import ontologizer.set.PopulationSet;
import ontologizer.types.ByteString;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.Parser;

/**
 * 
 * List genes to which thee given term is annotated to.
 * 
 * @author Sebastian Bauer
 *
 */
public class GeneLister
{
	/**
	 * The required argument stuff of the jakarta cli didn't work
	 * as expeced, so we have to do this manually. If specifed
	 * argument is not found an appropriate error message is written
	 * the program exited.
     *
	 * @param cmd
	 * @param name
	 * @return
	 */
	private static String getRequiredOptionValue(CommandLine cmd, char name)
	{
		String val = cmd.getOptionValue(name);
		if (val == null)
		{
			System.err.println("Aborting because the required argument \"-" + name + "\" wasn't specified! Use the -h for more help.");
			System.exit(-1);
		}
		return val;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		try
		{
			String oboFileName;
			String assocFileName;
			String termIdStr;
			TermID termId;
			boolean direct;

			Options options = new Options();

			options.addOption("h","help",false,"Shows this help");
			options.addOption("g","go",true,"File containig GO terminology and structure (.obo format). Required");
			options.addOption("a","association",true,"File containing associations from genes to GO terms. Required");
			options.addOption("t","termid",true,"Specifies the term from which the annotated genes are listed. Required");
			options.addOption("d","direct",false,"Only those genes which are directly annotated are listed.");

			Parser parser = new GnuParser();
			CommandLine cmd = parser.parse(options,args);
			if (cmd.hasOption("h"))
			{
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("java -jar GeneLister.jar [options]", options);
				System.exit(0);
			}

			oboFileName = getRequiredOptionValue(cmd,'g');
			assocFileName = getRequiredOptionValue(cmd,'a');
			termIdStr = getRequiredOptionValue(cmd,'t');
			direct = cmd.hasOption("d");

			/* Verfiy arguments */
			try
			{
				int termidInt = Integer.parseInt(termIdStr);
				termId = new TermID(termidInt);
			} catch (NumberFormatException nfe)
			{
				termId = new TermID(termIdStr);
			}
			
			/* loading GO graph */
			System.err.println("Parse obo file");
			OBOParser oboParser = new OBOParser(oboFileName);
			System.err.println(oboParser.doParse());
			TermContainer goTerms = new TermContainer(oboParser.getTermMap(), oboParser.getFormatVersion(), oboParser.getDate());
			System.err.println("Building graph");
			Ontology graph = new Ontology(goTerms);
			
			/* association */
			AssociationParser assocParser = new AssociationParser(assocFileName,goTerms,null);
			AssociationContainer assocs = new AssociationContainer(
					assocParser.getAssociations(),
					assocParser.getSynonym2gene(),
					assocParser.getDbObject2gene());

			
			/* build custom population containing all genes */
			Set<ByteString> allAnnotatedGenes = assocs.getAllAnnotatedGenes();
			PopulationSet completePop = new PopulationSet("AllAnnotated");
			for (ByteString gene : allAnnotatedGenes)
				completePop.addGene(gene, "None");

			GOTermEnumerator popTermEnumerator = completePop.enumerateGOTerms(graph,assocs);

			/* Now find out which genes are annotated to the given term and output them */
			GOTermAnnotatedGenes annotatedGenes = popTermEnumerator.getAnnotatedGenes(termId);
			Collection<ByteString> genes;
			if (direct) genes = annotatedGenes.directAnnotated;
			else genes = annotatedGenes.totalAnnotated;

			for (ByteString gene : genes)
				System.out.println(gene.toString());
		} catch (ParseException e)
		{
			System.err.println("Unable to parse the command line: " + e.getLocalizedMessage());
			System.exit(-1);
		} catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (OBOParserException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
