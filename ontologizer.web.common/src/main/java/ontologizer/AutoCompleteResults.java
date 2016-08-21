package ontologizer;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSArray;
import org.teavm.jso.core.JSString;

public abstract class AutoCompleteResults implements JSObject
{
	@JSProperty
	public abstract JSArray<JSString> getResults();

	@JSProperty
	public abstract void setResults(JSArray<JSString> results);
}
