package ontologizer;

/**
 * Interface for receiving/handling worker messages.
 *
 * @author Sebastian Bauer
 *
 * @param <T> type of the worker message that will be handled.
 */
public interface WorkerMessageHandler<T extends WorkerMessage>
{
	void handle(T msg);
}
