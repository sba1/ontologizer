package ontologizer;

import org.teavm.jso.JSProperty;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLBodyElement;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.html.HTMLHeadElement;
import org.teavm.jso.dom.xml.Text;

public class OntologizerClient
{
	private static HTMLDocument document = Window.current().getDocument();

	private static Text studySetText;

	/**
	 * Simple TextArea type.
	 *
	 * @author Sebastian Bauer
	 */
	public interface HTMLTextAreaElement extends HTMLElement {
	    @JSProperty
	    String getValue();
	}

	public static void studySetChanged(String studySet)
	{
		String [] lines = studySet.split("\n");
		studySetText.setNodeValue(lines.length + " items");
	}

	public static void main(String[] args)
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
	}
}
