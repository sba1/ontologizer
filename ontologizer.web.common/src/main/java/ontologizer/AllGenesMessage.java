package ontologizer;

import org.teavm.jso.JSProperty;

public abstract class AllGenesMessage extends WorkerMessage
{
	@JSProperty
	public abstract void setItems(String items);

	@JSProperty
	public abstract String getItems();
}
