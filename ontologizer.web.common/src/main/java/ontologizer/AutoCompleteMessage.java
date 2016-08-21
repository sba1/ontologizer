package ontologizer;

import org.teavm.jso.JSProperty;

/**
 * Message to request auto completion results.
 *
 * @author Sebastian Bauer
 */
public abstract class AutoCompleteMessage extends ReplyableWorkerMessage<AutoCompleteResults>
{
	@JSProperty
	public abstract void setPrefix(String prefix);

	@JSProperty
	public abstract String getPrefix();
}
