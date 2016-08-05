package ontologizer;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;

import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLBodyElement;
import org.teavm.jso.dom.html.HTMLButtonElement;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.html.HTMLHeadElement;
import org.teavm.jso.dom.xml.Text;

import ontologizer.association.AssociationContainer;
import ontologizer.calculation.AbstractGOTermProperties;
import ontologizer.calculation.EnrichedGOTermsResult;
import ontologizer.calculation.TermForTermCalculation;
import ontologizer.ontology.OBOParserException;
import ontologizer.ontology.Ontology;
import ontologizer.set.PopulationSet;
import ontologizer.set.StudySet;
import ontologizer.statistics.Bonferroni;
import ontologizer.types.ByteString;

/**
 * Main class of the Ontologizer Web client.
 *
 * @author Sebastian Bauer
 */
public class OntologizerClient
{
	private static final String COL_ID = "id";
	private static final String COL_NAME = "name";
	private static final String COL_PVAL = "pval";

	private static HTMLDocument document = Window.current().getDocument();

	private static Text studySetText;
	private static HTMLButtonElement allGenesButton;
	private static HTMLBootstrapTableElement resultsTable;

	private static Ontology ontology;
	private static AssociationContainer annotation;

	public static void studySetChanged(String studySet)
	{
		String [] lines = studySet.split("\n");
		studySetText.setNodeValue(lines.length + " items");
	}

	public static void main(String[] args) throws IOException, OBOParserException
	{
		/* Title */
		HTMLHeadElement head = document.createElement("h1").cast();
		head.setInnerHTML("Ontologizer");
		HTMLBodyElement body = document.getBody();
		body.appendChild(head);

		/* Study set text area */
		final HTMLTextAreaElement studySet = document.createElement("textarea").cast();
		body.appendChild(studySet);
		studySet.appendChild(document.createTextNode("text"));

		studySet.listenKeyPress(evt -> studySetChanged(studySet.getValue()));

		studySetText = document.createTextNode("");
		body.appendChild(studySetText);
		HTMLElement input = document.createElement("form").cast();
		body.appendChild(input);
		allGenesButton = document.createElement("button").withText("All Genes").cast();
		allGenesButton.setType("button");
		allGenesButton.listenClick(ev ->
		{
			if (annotation == null) return;

			StringBuilder allGenes = new StringBuilder();
			for (ByteString gene : annotation.getAllAnnotatedGenes())
			{
				allGenes.append(gene.toString());
				allGenes.append("\n");
			}
			studySet.setInnerHTML(allGenes.toString());
		});
		input.appendChild(allGenesButton);
		HTMLButtonElement ontologizeButton = document.createElement("button").withText("Ontologize").cast();
		ontologizeButton.setType("button");
		ontologizeButton.listenClick(ev ->
		{
			if (annotation == null) return;

			TermForTermCalculation calculation = new TermForTermCalculation();
			PopulationSet population = new PopulationSet();
			population.addGenes(annotation.getAllAnnotatedGenes());
			StudySet study = new StudySet();
			for (String s : studySet.getValue().split("\n"))
				study.addGene(new ByteString(s), "");
			EnrichedGOTermsResult result = calculation.calculateStudySet(ontology, annotation, population, study, new Bonferroni());
			System.out.println(result.getSize() + " terms");

			int i = 0;
			AbstractGOTermProperties [] results = new AbstractGOTermProperties[result.getSize()];
			for (AbstractGOTermProperties p : result)
				results[i++] = p;
			Arrays.sort(results, Comparator.comparingDouble(p -> p.p));

			if (resultsTable == null)
			{
				Column idColumn = Column.createColumn(COL_ID, "GO ID").cast();
				Column nameColumn = Column.createColumn(COL_NAME, "Name").cast();
				Column pvalColumn = Column.createColumn(COL_PVAL, "P value").cast();

				resultsTable = document.createElement("table").cast();
				resultsTable.bootstrapTable(idColumn,nameColumn,pvalColumn);
				resultsTable.hideLoading();
				body.appendChild(resultsTable);
			}

			i = 0;
			for (AbstractGOTermProperties p : results)
			{
				if (i++ > 30) break;

				resultsTable.append(Row.createRow().
						setColumn(COL_ID, p.goTerm.getIDAsString()).
						setColumn(COL_NAME, p.goTerm.getName()).
						setColumn(COL_PVAL, p.p_adjusted + ""));
			}
		});
		input.appendChild(ontologizeButton);

		final ProgressElement progressElement = document.getElementById("progress").cast();
		final boolean hiddenRemoved [] = new boolean[1];

		Worker worker = Worker.create("ontologizer-worker.js");
		worker.postMessage(WorkerMessage.createWorkerMessage(StartupMessage.class));

		worker.listenMessage(ProgressMessage.class, (ProgressMessage pm) ->
		{
			if (!hiddenRemoved[0])
			{
				progressElement.removeAttribute("hidden");
				hiddenRemoved[0] = true;
			}

			int percent = (int)Math.floor(pm.getCurrent() * 100.0 / pm.getMax());
			progressElement.setPercentage(percent);
		});
	}
}
