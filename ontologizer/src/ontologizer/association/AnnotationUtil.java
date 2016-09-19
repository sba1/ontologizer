package ontologizer.association;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ontologizer.types.ByteString;

public class AnnotationUtil
{
	/**
	 * Extract the symbols from all associations.
	 *
	 * @param associations
	 * @return the collection of all symbols.
	 */
	public static Collection<ByteString> getSymbols(List<Association> associations)
	{
		Set<ByteString> symbols = new HashSet<ByteString>();
		for (Association a : associations)
			symbols.add(a.getObjectSymbol());
		return symbols;

	}
}
