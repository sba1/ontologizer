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
		setLabel("", percent);
	}

	default void setLabel(String label, int percent)
	{
		HTMLElement inner = getChildNodes().get(1).cast();
		inner.setAttribute("style", "width: " + percent + "%;" );
		inner.setAttribute("aria-value", percent + "");

		if (label == null || label.length() == 0)
		{
			setLabel(percent + "%");
		} else
		{
			setLabel(label + " (" + percent + "%)");
		}
	}

	default void setLabel(String label)
	{
		HTMLElement span = getChildNodes().get(3).cast();
		span.setInnerHTML(label);
	}
}
