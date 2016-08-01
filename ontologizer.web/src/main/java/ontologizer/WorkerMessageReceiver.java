package ontologizer;

public interface WorkerMessageReceiver<T>
{
	void receive(T msg);
}
