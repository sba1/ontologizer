package ontologizer;

import org.teavm.jso.JSProperty;

public abstract class GetResultMessage extends ReplyableWorkerMessage<ResultEntry>
{
	@JSProperty
	public abstract void setRank(int rank);

	@JSProperty
	public abstract int getRank();
}
