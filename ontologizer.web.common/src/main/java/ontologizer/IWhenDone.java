package ontologizer;

/**
 * Simple interface that is used for in the context of the poster of a
 * replyable message to implement the retrieval of the result.
 *
 * @author Sebastian Bauer
 *
 * @param <R> the result type
 */
public interface IWhenDone<R>
{
	public void done(R result);
}
