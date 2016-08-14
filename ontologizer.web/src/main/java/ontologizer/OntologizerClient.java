package ontologizer;

import static ontologizer.LoadDataMessage.createLoadDataMessage;

import java.io.IOException;
import java.util.SortedMap;
import java.util.TreeMap;

import org.teavm.jso.browser.Window;
import org.teavm.jso.core.JSNumber;
import org.teavm.jso.dom.html.HTMLBodyElement;
import org.teavm.jso.dom.html.HTMLButtonElement;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.html.HTMLSelectElement;
import org.teavm.jso.dom.xml.Node;
import org.teavm.jso.dom.xml.Text;

/**
 * Main class of the Ontologizer Web client.
 *
 * @author Sebastian Bauer
 */
public class OntologizerClient
{
	private static HTMLDocument document = Window.current().getDocument();

	private static Text studySetText;
	private static HTMLButtonElement allGenesButton;
	private static HTMLElement resultsTable;
	private static HTMLElement resultsBody;
	private static HTMLSelectElement speciesElement;

	private static SortedMap<String,String> speciesMap = new TreeMap<>();
	private static String [] species;

	public static void studySetChanged(String studySet)
	{
		String [] lines = studySet.split("\n");
		studySetText.setNodeValue(lines.length + " items");
	}

	public static HTMLElement createCell(Node tr, String text)
	{
		return document.createElement("td", td ->
		{
			td.appendChild(document.createTextNode(text));
			tr.appendChild(td);
		});
	}

	public static void addOption(HTMLElement parent, String text)
	{
		document.createElement("option", option ->
		{
			option.appendChild(document.createTextNode(text));
			parent.appendChild(option);
		});
	}

	/**
	 * Issue a message to the worker for loading the data files
	 * for the currently selected species.
	 *
	 * @param worker the worker to which the message is posted.
	 */
	private static void loadDataForCurrentSpecies(Worker worker)
	{
		String sp = species[speciesElement.getSelectedIndex()];
		worker.postMessage(createLoadDataMessage(speciesMap.get(sp)));
	}

	public static void main(String[] args) throws IOException
	{
		final Worker worker = Worker.create("ontologizer-worker.js");

		HTMLBodyElement body = document.getBody();

		/* Study set text area */
		final HTMLTextAreaElement studySet = document.getElementById("settextarea").cast();

		studySet.listenKeyPress(evt -> studySetChanged(studySet.getValue()));

		studySetText = document.createTextNode("");
		body.appendChild(studySetText);

		speciesMap.put("Yeast", "gene_association.sgd.gz");
		speciesMap.put("Zebrafish", "gene_association.zfin.gz");
		speciesMap.put("Mouse", "gene_association.mgi.gz");
		speciesMap.put("C. elegans", "gene_association.wb.gz");
		speciesMap.put("Fruit fly", "gene_association.fb.gz");
		speciesMap.put("Human", "goa_human.gaf.gz");
		species = speciesMap.keySet().toArray(new String[speciesMap.size()]);
		speciesElement = document.getElementById("species").cast();
		for (String sp : species)
			addOption(speciesElement, sp);
		speciesElement.addEventListener("change", ev -> loadDataForCurrentSpecies(worker) );

		allGenesButton = document.getElementById("allgenes").cast();
		allGenesButton.listenClick(ev ->
		{
			GetAllGenesMessage gm = WorkerMessage.createWorkerMessage(GetAllGenesMessage.class);
			worker.postMessage(gm);
		});
		HTMLButtonElement ontologizeButton = document.getElementById("ontologize").cast();
		ontologizeButton.setType("button");
		ontologizeButton.listenClick(ev ->
		{
			String [] items = studySet.getValue().split("\n");
			OntologizeMessage om = WorkerMessage.createWorkerMessage(OntologizeMessage.class);
			om.setItems(items);
			worker.postMessage(om);

			if (resultsTable == null)
			{
				resultsTable = document.getElementById("results");
				resultsBody = document.getElementById("resultsbody");
			}
		});

		final ProgressElement progressElement = document.getElementById("progress").cast();
		final boolean hiddenRemoved [] = new boolean[1];

		worker.postMessage(createLoadDataMessage(speciesMap.get("Yeast")));

		worker.listenMessage(ProgressMessage.class, (ProgressMessage pm) ->
		{
			if (!hiddenRemoved[0])
			{
				progressElement.removeAttribute("hidden");
				hiddenRemoved[0] = true;
			}

			int percent = (int)Math.floor(pm.getCurrent() * 100.0 / pm.getMax());
			String title = pm.getTitle();
			if (title != null && title.length() > 0)
			{
				progressElement.setLabel(title, percent);
			} else
			{
				progressElement.setPercentage(percent);
			}
		});

		worker.listenMessage(AllGenesMessage.class, (AllGenesMessage am) ->
		{
			studySet.setInnerHTML(am.getItems());
		});

		worker.listenMessage(OntologizeDoneMessage.class, (OntologizeDoneMessage odm) ->
		{
			GetNumberOfResultsMessage rnrm = WorkerMessage.createWorkerMessage(GetNumberOfResultsMessage.class);
			worker.postMessage(GetNumberOfResultsMessage.class, rnrm, (JSNumber num) ->
			{
				int numberOfTerms = num.intValue();
				resultsBody.clear();
				for (int i=0; i < Math.min(30, numberOfTerms); i++)
				{
					GetResultMessage rm = WorkerMessage.createWorkerMessage(GetResultMessage.class);
					rm.setRank(i);
					worker.postMessage(GetResultMessage.class, rm, result ->
					{
						document.createElement("tr", tr ->
						{
							createCell(tr, result.getID());
							createCell(tr, result.getName());
							createCell(tr, result.getAdjP() + "");
							resultsBody.appendChild(tr);
						});
					});
				}
			});
		});
	}
}
