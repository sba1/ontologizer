package ontologizer;

import org.teavm.jso.JSProperty;
import org.teavm.jso.dom.events.Event;

public interface ProgressEvent extends Event
{
	@JSProperty
	public boolean isLengthComputable();

	@JSProperty
	public int getLoaded();

	@JSProperty
	public int getTotal();
}
