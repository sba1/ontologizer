package ontologizer;

import org.teavm.jso.JSProperty;

public abstract class LoadDataMessage extends WorkerMessage
{
	@JSProperty
	public abstract void setAssociationFilename(String filename);

	@JSProperty
	public abstract String getAssociationFilename();

	public static LoadDataMessage createLoadDataMessage(String filename)
	{
		LoadDataMessage ldm = WorkerMessage.createWorkerMessage(LoadDataMessage.class);
		ldm.setAssociationFilename(filename);
		return ldm;
	}
}
