package ontologizer;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;

interface EditSession extends JSObject
{
}

/**
 * Simple ACE type.
 *
 * @author Sebastian Bauer
 */
public abstract class ACE implements JSObject
{
	@JSBody(script="return ace.edit(id);", params = { "id" })
	public static native ACE edit(String id);

	public abstract String getValue();

	public abstract void setValue(String text);

	@JSProperty
	public abstract EditSession getSession();
}
