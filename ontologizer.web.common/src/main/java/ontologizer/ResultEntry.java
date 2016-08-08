package ontologizer;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;

public abstract class ResultEntry implements JSObject
{
	@JSProperty
	public abstract String getName();

	@JSProperty
	public abstract void setName(String name);

	@JSProperty
	public abstract String getID();

	@JSProperty
	public abstract void setID(String name);

	@JSProperty
	public abstract double getAdjP();

	@JSProperty
	public abstract void setAdjP(double adjP);

	@JSBody(script="return {}", params = {})
	public static native ResultEntry createResultEntry();
}
