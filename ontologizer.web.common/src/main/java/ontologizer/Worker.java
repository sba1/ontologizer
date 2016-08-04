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

	@JSBody(script="return new Worker(name);", params={"name"})
	public static native Worker create(String name);

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
	public <T extends WorkerMessage> void listenMessage(Class<T> cls, WorkerMessageHandler<T> receiver)
	{
		listenMessage((ev)->{
			T wm = ev.getData().cast();
			if (wm.getType().equals(cls.getName()))
			{
				receiver.handle(wm);
			}
		});
	}

	public abstract void postMessage(String str);

	public abstract void postMessage(JSObject obj);
}
