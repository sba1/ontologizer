package ontologizer;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;

/**
 * The type used for messages between worker and main and vice versa.
 *
 * @author Sebastian Bauer
 */
public abstract class WorkerMessage implements JSObject
{
	@JSBody(script="return {type: type, id: id}", params = {"type", "id"})
	private static native WorkerMessage createWorkerMessage(String type, int id);

	@JSProperty
	public abstract String getType();

	@JSProperty
	public abstract int getId();

	private static int currentId = 0;

	/**
	 * Create a worker message
	 *
	 * @param cl type of the worker message
	 * @return the newly created worker message.
	 */
	public static <T extends WorkerMessage> T createWorkerMessage(Class<T> cl) {
		return createWorkerMessage(cl.getName(), currentId++).cast();
	}

	public void post(Worker worker)
	{
		worker.postMessage(this);
	}
}
