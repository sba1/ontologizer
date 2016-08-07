package ontologizer;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;

/**
 * A replyable worker message is a message from a poster who also expect a result.
 *
 * @author Sebastian Bauer
 *
 * @param <R> the type of the result
 */
public abstract class ReplyableWorkerMessage<R extends JSObject> extends WorkerMessage
{
	@JSProperty
	public abstract R getResult();

	@JSProperty
	public abstract void setResult(R result);
}
