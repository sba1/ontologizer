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

	public <R extends JSObject,T extends ReplyableWorkerMessage<R>> void listenMessage2(Class<T> cls, WorkerMessageHandlerWithResult<R, T> receiver)
	{
		listenMessage((ev)->{
			T wm = ev.getData().cast();
			if (wm.getType().equals(cls.getName()))
			{
				R result = receiver.handle(wm);
				wm.setResult(result);
				System.out.println("Post message: " + wm.getType());
				postMessage(wm);
			}
		});
	}

	public abstract void postMessage(String str);

	public abstract void postMessage(JSObject obj);

	public <R extends JSObject, T extends ReplyableWorkerMessage<R>> void postMessage(Class<T> cl, T message, final IWhenDone<R> whenDone)
	{
		/* FIXME: Once replied, we should remove that listener again (or use manage it
		 * on our own)
		 */
		listenMessage(cl, (T m) ->
		{
			if (m.getId() == message.getId())
			{
				whenDone.done(m.getResult());
			}
		});
		postMessage(message);
	}
}
