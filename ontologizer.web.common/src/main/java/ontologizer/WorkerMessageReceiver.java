package ontologizer;

/**
 * Interface for receiving/handling worker messages.
 *
 * @author Sebastian Bauer
 *
 * @param <T> type of the worker message that will be handled.
 */
public interface WorkerMessageReceiver<T extends WorkerMessage>
{
	void receive(T msg);
}
