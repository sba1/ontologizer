package ontologizer;

import org.teavm.jso.JSBody;
import org.teavm.jso.ajax.XMLHttpRequest;
import org.teavm.jso.dom.events.EventTarget;

public abstract class ArrayBufferHttpRequest extends XMLHttpRequest implements EventTarget
{
	/**
	 * Return the response as a byte array.
	 *
	 * @return the byte array of the result
	 */
	public byte [] getResponseBytes()
	{
		return getResponseBytes_(this);
	}

	@JSBody(script="return new Int8Array(request.response)", params={"request"})
	private static native byte [] getResponseBytes_(ArrayBufferHttpRequest request);

	public static ArrayBufferHttpRequest create()
	{
		XMLHttpRequest req = XMLHttpRequest.create();
		req.setResponseType("arraybuffer");
		return req.cast();
	}
}

