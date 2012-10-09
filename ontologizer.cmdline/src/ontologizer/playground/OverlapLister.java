/*
 * Created on 18.09.2005
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ontologizer.playground;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import ontologizer.association.AssociationContainer;
import ontologizer.association.AssociationParser;
import ontologizer.enumeration.GOTermEnumerator;
import ontologizer.enumeration.GOTermEnumerator.GOTermAnnotatedGenes;
import ontologizer.go.Ontology;
import ontologizer.go.OBOParser;
import ontologizer.go.OBOParserException;
import ontologizer.go.Term;
import ontologizer.go.TermContainer;
import ontologizer.set.PopulationSet;
import ontologizer.types.ByteString;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.Parser;

/**
 * For each term lists the terms which have an overlap in annotation.
 * 
 * @author Sebastian Bauer
 * @author Steffen Grossmann
 */
public class OverlapLister
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

	public static void main(String[] args)
	{
		try
		{
			String oboFileName;
			String assocFileName;
			String outDirName;

			Options options = new Options();

			options.addOption("h","help",false,"Shows this help");
			options.addOption("g","go",true,"File containig GO terminology and structure (.obo format). Required");
			options.addOption("o","outdir",true,"Directory to hold results. Optional, defaults to .");
			options.addOption("a","association",true,"File containing associations from genes to GO terms. Required");

			Parser parser = new GnuParser();
			CommandLine cmd = parser.parse(options,args);
			if (cmd.hasOption("h"))
			{
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("java -jar OverlapLister.jar [options]", options);
				System.exit(0);
			}

			oboFileName = getRequiredOptionValue(cmd,'g');
			assocFileName = getRequiredOptionValue(cmd, 'a');
			outDirName = cmd.getOptionValue("o", null);

			/* loading GO graph */
			System.err.println("Parse obo file");
			OBOParser oboParser = new OBOParser(oboFileName);
			System.err.println(oboParser.doParse());
			TermContainer goTerms = new TermContainer(oboParser.getTermMap(), oboParser.getFormatVersion(), oboParser.getDate());
			System.err.println("Building graph");
			Ontology graph = new Ontology(goTerms);

			// loading associations
			AssociationParser assocParser = new AssociationParser(assocFileName, goTerms, null);
			AssociationContainer assocs = new AssociationContainer(assocParser.getAssociations(), assocParser.getSynonym2gene(), assocParser.getDbObject2gene());
			
			/* build custom population containing all genes */
			Set<ByteString> allAnnotatedGenes = assocs.getAllAnnotatedGenes();
			PopulationSet completePop = new PopulationSet("AllAnnotated");
			for (ByteString gene : allAnnotatedGenes)
				completePop.addGene(gene, "None");

			GOTermEnumerator popTermEnumerator = completePop.enumerateGOTerms(graph,assocs);
			
			/* making outDir if necessary */
			File outDir = null;
			if (outDirName != null)
			{
				outDir = new File(outDirName);
				outDir.mkdirs();
			}

			int todo = goTerms.termCount();

			/* For every term find out its subterms and write them into
			 * a separate file.
			 */
			for (Term term : goTerms)
			{
				System.err.print(todo + " terms todo\r");
				todo--;
				
				GOTermAnnotatedGenes termGenes =
					popTermEnumerator.getAnnotatedGenes(term.getID());
				
				if (termGenes.totalAnnotatedCount() > 0) {


					String termfilename = term.getIDAsString().replace(':','_');
					File termFile = new File(outDir,termfilename);
					BufferedWriter termOut = new BufferedWriter(new FileWriter(termFile));
					List<ByteString> termGenesList = termGenes.totalAnnotated;
					
					for (Term otherTerm : goTerms) {
						GOTermAnnotatedGenes otherTermGenes =
							popTermEnumerator.getAnnotatedGenes(otherTerm.getID());

						if (otherTermGenes.totalAnnotatedCount() > 0) {
							List<ByteString> otherTermGenesList = otherTermGenes.totalAnnotated;
							
							for (ByteString gene : otherTermGenesList) {
								if (termGenesList.contains(gene)) {
									termOut.write(otherTerm.getIDAsString());
									termOut.write("\n");
									break;
								}
							}
						}
					}
					termOut.close();
				}
			}
			System.err.println("Finished");
		} catch (ParseException e)
		{
			System.err.println("Unable to parse the command line: " + e.getLocalizedMessage());
			System.exit(-1);
		} catch (IOException e)
		{
			e.printStackTrace();
		} catch (OBOParserException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
