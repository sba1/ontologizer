package ontologizer;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;

public abstract class ReplyableWorkerMessage<R extends JSObject> extends WorkerMessage
{
	@JSProperty
	public abstract R getResult();

	@JSProperty
	public abstract void setResult(R result);
}
