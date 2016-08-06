package ontologizer;

import org.teavm.jso.JSProperty;

public abstract class OntologizeMessage extends WorkerMessage
{
	@JSProperty
	public abstract String [] getItems();

	@JSProperty
	public abstract void setItems(String [] items);
}
