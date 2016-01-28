package ontologizer;

import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLBodyElement;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLHeadElement;

public class OntologizerClient
{
	private static HTMLDocument document = Window.current().getDocument();

	public static void main(String[] args)
	{
		HTMLHeadElement head = document.createElement("h1").cast();
		head.setInnerHTML("Ontologizer");
		HTMLBodyElement body = document.getBody();
		body.appendChild(head);
	}
}
