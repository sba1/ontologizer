package ontologizer;

import org.teavm.jso.JSProperty;

public abstract class ProgressMessage extends WorkerMessage
{
	@JSProperty
	public abstract void setTitle(String title);

	@JSProperty
	public abstract String getTitle(String title);

	@JSProperty
	public abstract void setCurrent(int current);

	@JSProperty
	public abstract int getCurrent();

	@JSProperty
	public abstract void setMax(int max);

	@JSProperty
	public abstract int getMax();

	public ProgressMessage withTitle(String title)
	{
		setTitle(title);
		return this;
	}

	public ProgressMessage withCurrent(int current)
	{
		setCurrent(current);
		return this;
	}

	public ProgressMessage withMax(int max)
	{
		setMax(max);
		return this;
	}

	public static ProgressMessage createProgressMessage()
	{
		return WorkerMessage.createWorkerMessage(ProgressMessage.class);
	}
}
