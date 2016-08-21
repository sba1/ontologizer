package ontologizer;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.ajax.XMLHttpRequest;

public class Utils
{
	/**
	 * Return the result of an http request as a byte array.
	 *
	 * @param request
	 * @return the byte array of the result
	 */
	@JSBody(script="return new Int8Array(request.response)", params={"request"})
	public static native byte [] getByteResult(final XMLHttpRequest request);

	@JSBody(script="return {};", params= {})
	public static native <T extends JSObject> T createObject();

	@JSBody(script="console.log(obj)", params = {"obj"})
	public static native void log(JSObject obj);
}
