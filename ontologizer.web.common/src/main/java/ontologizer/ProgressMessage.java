package ontologizer;

import org.teavm.jso.JSProperty;

public abstract class ProgressMessage extends WorkerMessage
{
	@JSProperty
	public abstract void setCurrent(int current);

	@JSProperty
	public abstract void getCurrent();

	@JSProperty
	public abstract void setMax(int max);

	@JSProperty
	public abstract int getMax();
}
