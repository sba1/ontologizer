/**
 * Timer related functions grabbed from org.teavm.jso.browser/Window.java
 */

package ontologizer;

import org.teavm.jso.JSBody;
import org.teavm.jso.browser.TimerHandler;

public abstract class Timer
{
	@JSBody(params = { "handler", "delay" }, script = "return setTimeout(handler, delay);")
	public static native int setTimeout(TimerHandler handler, int delay);

	@JSBody(params = { "timeoutId" }, script = "clearTimeout(timeoutId);")
	public static native void clearTimeout(int timeoutId);

	@JSBody(params = { "handler", "delay" }, script = "return setInterval(handler, delay);")
	public static native int setInterval(TimerHandler handler, int delay);

	@JSBody(params = { "timeoutId" }, script = "clearInterval(timeoutId);")
	public static native void clearInterval(int timeoutId);
}
