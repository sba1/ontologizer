package ontologizer;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSArray;
import org.teavm.jso.core.JSString;

interface EditSession extends JSObject
{
}

interface LangTools extends JSObject
{
	public void addCompleter(EditorCompleter completer);
}

abstract class EditorCompletionEntry implements JSObject
{
	@JSBody(params = {"name", "value", "score", "meta"}, script = "return {name : name, value : value, score : score, meta : meta};")
	public static native EditorCompletionEntry createEditorCompletionEntry(String name, String value, int score, String meta);

	@JSProperty
	public abstract String getName();

	@JSProperty
	public abstract String getValue();

	@JSProperty
	public abstract int getScorce();

	@JSProperty
	public abstract String getMeta();
}

interface EditorCompletionCallback extends JSObject
{
	@JSBody(params = {"err", "results"}, script = "this(err, results);")
	public void results(int err, JSArray<EditorCompletionEntry> results);
}

@JSFunctor
interface GetCompletionHandler extends JSObject
{
	public void getCompletion(ACE editor, EditSession session, int pos, JSString prefix, EditorCompletionCallback callback);
}

abstract class EditorCompleter implements JSObject
{
	@JSBody(params = { }, script = "return { };")
	public static native EditorCompleter createEditorCompleter();

	@JSProperty("getCompletions")
	public abstract void setCompletionHandler(GetCompletionHandler editorCompletion);
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
