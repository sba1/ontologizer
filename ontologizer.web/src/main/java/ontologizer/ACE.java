package ontologizer;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;

interface EditSession extends JSObject
{
}

interface LangTools extends JSObject
{
}

abstract class EditorOptions implements JSObject
{
	@JSBody(params = {}, script = "return {};")
	public static native EditorOptions createEditorOptions();

	@JSProperty
	public abstract void setEnableBasicAutocompletion(boolean enableBasicAutocompletion);
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

	@JSBody(script="return ace.require(req);", params = { "req" })
	public static native JSObject require(String req);

	public static LangTools requireLangTools()
	{
		return require("ace/ext/language_tools").cast();
	}

	public abstract String getValue();

	public abstract void setValue(String text);

	public abstract void setOptions(EditorOptions options);

	@JSProperty
	public abstract EditSession getSession();
}
