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

	/**
	 * Listen on a particular worker message.
	 *
	 * @param cls
	 * @param receiver
	 */
	public <T extends WorkerMessage> void listenMessage(Class<T> cls, WorkerMessageReceiver<T> receiver)
	{
		listenMessage((ev)->{
			T wm = ev.getData().cast();
			if (wm.getType().equals(cls.getName()))
			{
				receiver.receive(wm);
			}
		});
	}

	public abstract void postMessage(String str);

	public abstract void postMessage(JSObject obj);
}
