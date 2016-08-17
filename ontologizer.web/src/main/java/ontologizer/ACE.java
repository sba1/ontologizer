package ontologizer;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;

/**
 * Simple ACE type.
 *
 * @author Sebastian Bauer
 */
public abstract class ACE implements JSObject
{
	@JSBody(script="return ace.edit(id);", params = { "id" })
	public static native ACE edit(String id);
}
