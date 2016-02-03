package ontologizer;

import org.teavm.jso.JSProperty;
import org.teavm.jso.dom.html.HTMLElement;

/**
 * Simple TextArea type.
 *
 * @author Sebastian Bauer
 */
public interface HTMLTextAreaElement extends HTMLElement
{
    @JSProperty
    String getValue();
}
