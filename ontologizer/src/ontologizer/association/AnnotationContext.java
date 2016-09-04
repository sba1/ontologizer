package ontologizer.association;

import java.util.HashMap;
import java.util.List;

import ontologizer.types.ByteString;
import sonumina.collections.ObjectIntHashMap;
import sonumina.collections.ObjectIntHashMap.ObjectIntProcedure;

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

	/**
	 * Construct and return the mapping from synonyms to symbols.
	 *
	 * @return the constructed map.
	 */
	public HashMap<ByteString, ByteString> getSynonym2Symbol()
	{
		final HashMap<ByteString, ByteString> synonym2symbol = new HashMap<ByteString, ByteString>(synonymMap.size());
		synonymMap.forEachKeyValue(new ObjectIntProcedure<ByteString>()
		{
			@Override
			public void keyValue(ByteString key, int value)
			{
				synonym2symbol.put(key, symbols[value]);
			}
		});
		return synonym2symbol;
	}

	/**
	 * Construct and return the mapping from object ids to symbols.
	 *
	 * @return the constructed map.
	 */
	public HashMap<ByteString, ByteString> getDbObjectID2Symbol()
	{
		final HashMap<ByteString, ByteString> dbObjectID2gene = new HashMap<ByteString, ByteString>(synonymMap.size());
		objectIdMap.forEachKeyValue(new ObjectIntProcedure<ByteString>()
		{
			@Override
			public void keyValue(ByteString key, int value)
			{
				dbObjectID2gene.put(key, symbols[value]);
			}
		});
		return dbObjectID2gene;
	}

}
