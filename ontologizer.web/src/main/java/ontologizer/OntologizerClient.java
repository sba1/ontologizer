package ontologizer;

import org.teavm.dom.browser.Window;
import org.teavm.dom.html.HTMLBodyElement;
import org.teavm.dom.html.HTMLDocument;
import org.teavm.dom.html.HTMLHeadElement;
import org.teavm.jso.JS;

public class OntologizerClient
{
	private static Window window = (Window)JS.getGlobal();
	private static HTMLDocument document = window.getDocument();

	public static void main(String[] args)
	{
		HTMLHeadElement head = (HTMLHeadElement) document.createElement("h1");
		head.setInnerHTML("Ontologizer");
		HTMLBodyElement body = document.getBody();
		body.appendChild(head);
	}
}
