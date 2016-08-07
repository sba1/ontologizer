package ontologizer;

/**
 * Interface for receiving/handling worker messages.
 *
 * @author Sebastian Bauer
 *
 * @param <T> type of the worker message that will be handled.
 * @param <R> type of the result
 */
public interface WorkerMessageHandlerWithResult<R, T extends WorkerMessage>
{
	R handle(T msg);
}
