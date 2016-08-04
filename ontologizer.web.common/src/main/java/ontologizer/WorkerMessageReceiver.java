package ontologizer;

public interface WorkerMessageReceiver<T extends WorkerMessage>
{
	void receive(T msg);
}
