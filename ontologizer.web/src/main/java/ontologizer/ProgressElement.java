package ontologizer;

import org.teavm.jso.dom.html.HTMLElement;

/**
 * Simple progress bar element.
 *
 * @author Sebastian Bauer
 */
public interface ProgressElement extends HTMLElement
{
	default void setPercentage(int percent)
	{
		HTMLElement inner = getChildNodes().get(1).cast();
		inner.setAttribute("style", "width: " + percent + "%;" );
		inner.setAttribute("aria-value", percent + "");
		HTMLElement span = getChildNodes().get(3).cast();
		span.setInnerHTML(percent + "%");
	}
}
