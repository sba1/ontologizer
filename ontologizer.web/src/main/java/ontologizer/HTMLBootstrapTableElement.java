package ontologizer;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSIndexer;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.dom.html.HTMLElement;

abstract class Column implements JSObject
{
	@JSProperty
	public native void setField(String field);

	@JSProperty
	public native void setTitle(String field);

	@JSBody(script="return {field: id, title: title}", params = {"id", "title"})
	public static native JSObject createColumn(String id, String title);
}

abstract class Row implements JSObject
{
	public Row setColumn(String col, String value)
	{
		setProperty(col, value);
		return this;
	}

	@JSIndexer
	private native void setProperty(String prop, String value);

	@JSBody(script="return {}", params = {})
	public static native Row createRow();
}

public abstract class HTMLBootstrapTableElement implements HTMLElement
{
	@JSProperty
	public abstract String getSummary();

	public void bootstrapTable(Column...col)
	{
		bootstrapTable_(this, col);
	}

	public void showLoading()
	{
		showLoading_(this);
	}

	public void hideLoading()
	{
		hideLoading_(this);
	}

	public void removeAll()
	{
		removeAll_(this);
	}

	@JSBody(script="$(this).bootstrapTable('append', data)", params="data")
	public native void append(JSObject data);

	@JSBody(script="$(obj).bootstrapTable({	columns: col})", params = { "obj", "col"})
	private static native void bootstrapTable_(HTMLBootstrapTableElement obj, Column... col);

	@JSBody(script="$(obj).bootstrapTable('showLoading')", params = { "obj" })
	private static native void showLoading_(HTMLBootstrapTableElement obj);

	@JSBody(script="$(obj).bootstrapTable('hideLoading')", params = { "obj" })
	private static native void hideLoading_(HTMLBootstrapTableElement obj);

	@JSBody(script="$(obj).bootstrapTable('removeAll')", params = { "obj" })
	private static native void removeAll_(HTMLBootstrapTableElement obj);
}

