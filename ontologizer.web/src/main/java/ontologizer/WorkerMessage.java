package ontologizer;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;

/**
 * The type used for messages between worker and main and vice versa.
 *
 * @author Sebastian Bauer
 */
public abstract class WorkerMessage implements JSObject
{
	@JSBody(script="return {type: type}", params = {"type"})
	private static native WorkerMessage createWorkerMessage(String type);

	/**
	 * Create a worker message
	 *
	 * @param cl type of the worker message
	 * @return the newly created worker message.
	 */
	public static <T extends WorkerMessage> T createWorkerMessage(Class<T> cl) {
		return createWorkerMessage(cl.getName()).cast();
	}
}
