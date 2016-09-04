package ontologizer.association;

import java.util.List;

import ontologizer.types.ByteString;
import sonumina.collections.ObjectIntHashMap;

public class AnnotationContext
{
	/** The symbols */
	private ByteString[] symbols;

	/** And the corresponding object ids */
	private ByteString[] objectIds;

	/** Maps object symbols to item indices within the items list */
	private ObjectIntHashMap<ByteString> objectSymbolMap;

	/** Maps object ids to item indices within the items list */
	private ObjectIntHashMap<ByteString> objectIdMap;

	/** Maps synonyms to item indices within the items list */
	private ObjectIntHashMap<ByteString> synonymMap;

	public AnnotationContext(List<ByteString> symbols, List<ByteString> objectIds, ObjectIntHashMap<ByteString> objectSymbolMap, ObjectIntHashMap<ByteString> objectIdMap, ObjectIntHashMap<ByteString> synonymMap)
	{
		if (symbols.size() != objectIds.size()) throw new IllegalArgumentException("Symbols and object ids size must match");

		this.symbols = new ByteString[symbols.size()];
		symbols.toArray(this.symbols);

		this.objectIds = new ByteString[objectIds.size()];
		objectIds.toArray(this.objectIds);

		this.objectSymbolMap = objectSymbolMap;
		this.objectIdMap = objectIdMap;
		this.synonymMap = synonymMap;
	}
}
