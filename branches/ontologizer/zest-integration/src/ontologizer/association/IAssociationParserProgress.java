package ontologizer.association;

public interface IAssociationParserProgress
{
	/**
	 * Called upon initialization.
	 * 
	 * @param max maximal number of steps.
	 */
	void init(int max);

	/**
	 * Called arbitrary.
	 * 
	 * @param current the current number of steps.
	 */
	void update(int current);
}
