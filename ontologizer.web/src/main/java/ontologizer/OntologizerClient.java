package ontologizer;

import static ontologizer.LoadDataMessage.createLoadDataMessage;
import static ontologizer.WorkerMessage.createWorkerMessage;

import java.io.IOException;
import java.util.SortedMap;
import java.util.TreeMap;

import org.teavm.jso.JSObject;
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
	private static ACE ace;

	/** Maps human readable species names to association filenames */
	private static SortedMap<String,String> speciesMap = new TreeMap<>();

	/** Array of species as human readable names */
	private static String [] species;

	/** Species that has been selected last. Uses to see if new data files need to be loaded */
	private static String lastSelectedSpecies;

	private static Worker worker;

	public static void studySetChanged(String studySet)
	{
		loadDataForCurrentSpecies();

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
	private static void loadDataForCurrentSpecies()
	{
		String sp = species[speciesElement.getSelectedIndex()];

		/* Don't load anything if species matches */
		if (lastSelectedSpecies != null && sp.contentEquals(lastSelectedSpecies))
			return;

		initWorker();

		worker.postMessage(createLoadDataMessage(speciesMap.get(sp)));

		lastSelectedSpecies = sp;
	}

	/**
	 * Initialize the worker.
	 */
	private static void initWorker()
	{
		if (worker != null)
		{
			worker.terminate();
		}
		worker = Worker.create("ontologizer-worker.js");

		final ProgressElement progressElement = document.getElementById("progress").cast();
		final boolean hiddenRemoved [] = new boolean[1];

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

		worker.listenMessage(HideProgressMessage.class, hpm ->
		{
			if (hiddenRemoved[0])
			{
				progressElement.setAttribute("hidden", "true");
				hiddenRemoved[0] = false;
			}
		});

		worker.listenMessage(AllGenesMessage.class, (AllGenesMessage am) ->
		{
			ace.setValue(am.getItems());
		});

		worker.listenMessage(OntologizeDoneMessage.class, (OntologizeDoneMessage odm) ->
		{
			GetNumberOfResultsMessage rnrm = createWorkerMessage(GetNumberOfResultsMessage.class);
			worker.postMessage(GetNumberOfResultsMessage.class, rnrm, (JSNumber num) ->
			{
				int numberOfTerms = num.intValue();
				resultsBody.clear();
				for (int i=0; i < Math.min(30, numberOfTerms); i++)
				{
					GetResultMessage rm = createWorkerMessage(GetResultMessage.class);
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

	public static void main(String[] args) throws IOException
	{
		ACE.require("ace/ext/language_tools");

		HTMLBodyElement body = document.getBody();

		studySetText = document.createTextNode("");
		body.appendChild(studySetText);

		ace = ACE.edit("items");
		EditorOptions editorOptions = EditorOptions.createEditorOptions();
		editorOptions.setEnableBasicAutocompletion(true);
		ace.setOptions(editorOptions);

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

		speciesElement.addEventListener("change", ev -> loadDataForCurrentSpecies() );

		allGenesButton = document.getElementById("allgenes").cast();
		allGenesButton.listenClick(ev ->
		{
			loadDataForCurrentSpecies();
			GetAllGenesMessage gm = createWorkerMessage(GetAllGenesMessage.class);
			worker.postMessage(gm);
		});
		HTMLButtonElement ontologizeButton = document.getElementById("ontologize").cast();
		ontologizeButton.setType("button");
		ontologizeButton.listenClick(ev ->
		{
			loadDataForCurrentSpecies();

			String [] items = ace.getValue().split("\n");
			OntologizeMessage om = createWorkerMessage(OntologizeMessage.class);
			om.setItems(items);
			worker.postMessage(om);

			if (resultsTable == null)
			{
				resultsTable = document.getElementById("results");
				resultsBody = document.getElementById("resultsbody");
			}
		});
	}
}
