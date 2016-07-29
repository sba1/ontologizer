package ontologizer;

import org.teavm.jso.JSBody;
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
}
