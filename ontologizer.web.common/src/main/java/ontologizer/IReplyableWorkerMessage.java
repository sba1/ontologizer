package ontologizer;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;

public interface IReplyableWorkerMessage<R>
{
	@JSProperty
	public R getResult();
}
