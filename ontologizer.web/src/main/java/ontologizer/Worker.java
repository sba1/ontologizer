package ontologizer;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.dom.events.EventListener;
import org.teavm.jso.dom.events.EventTarget;
import org.teavm.jso.dom.events.MessageEvent;

public abstract class Worker implements JSObject, EventTarget
{
	@JSBody(script="return worker", params = {})
	public static native Worker current();

	public void listenMessage(EventListener<MessageEvent> listener)
	{
		addEventListener("message", listener);
	}

	public abstract void postMessage(String str);

	public abstract void postMessage(JSObject obj);
}
