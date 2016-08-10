package ontologizer;

import java.io.IOException;

import org.teavm.jso.browser.Window;
import org.teavm.jso.core.JSNumber;
import org.teavm.jso.dom.html.HTMLBodyElement;
import org.teavm.jso.dom.html.HTMLButtonElement;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;
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

	public static void main(String[] args) throws IOException
	{
		final Worker worker = Worker.create("ontologizer-worker.js");

		HTMLBodyElement body = document.getBody();

		/* Study set text area */
		final HTMLTextAreaElement studySet = document.getElementById("settextarea").cast();

		studySet.listenKeyPress(evt -> studySetChanged(studySet.getValue()));

		studySetText = document.createTextNode("");
		body.appendChild(studySetText);

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

		worker.postMessage(WorkerMessage.createWorkerMessage(LoadDataMessage.class));

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
