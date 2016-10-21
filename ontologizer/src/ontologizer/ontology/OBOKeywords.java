package ontologizer.ontology;

public class OBOKeywords
{
	/* Stanza types */
	public final byte [] TERM_KEYWORD = "term".getBytes();
	public final byte [] TYPEDEF_KEYWORD = "typedef".getBytes();

	/* Supported header types */
	private final byte [] FORMAT_VERSION_KEYWORD = "format-version".getBytes();
	private final byte [] DATE_KEYWORD = "date".getBytes();
	private final byte [] DATA_VERSION_KEYWORD = "data-version".getBytes();
	private final byte [] SUBSETDEF_KEYWORD = "subsetdef".getBytes();

	/* Supported term types */
	private final static byte [] ID_KEYWORD = "id".getBytes();
	private final static byte [] NAME_KEYWORD = "name".getBytes();
	private final static byte [] IS_A_KEYWORD = "is_a".getBytes();
	private final static byte [] RELATIONSHIP_KEYWORD = "relationship".getBytes();
	private final static byte [] SYNONYM_KEYWORD = "synonym".getBytes();
	private final static byte [] DEF_KEYWORD = "def".getBytes();
	private final static byte [] NAMESPACE_KEYWORD = "namespace".getBytes();
	private final static byte [] ALT_ID_KEYWORD = "alt_id".getBytes();
	private final static byte [] EQUIVALENT_TO_KEYWORD = "equivalent_to".getBytes();
	private final static byte [] IS_OBSOLETE_KEYWORD = "is_obsolete".getBytes();
	private final static byte [] XREF_KEYWORD = "xref".getBytes();
	private final static byte [] SUBSET_KEYWORD = "subset".getBytes();
	private final static byte [] TRUE_KEYWORD = "true".getBytes();

	public final static byte[][] TERM_KEYWORDS =
	{
		ID_KEYWORD,
		NAME_KEYWORD,
		IS_A_KEYWORD,
		RELATIONSHIP_KEYWORD,
		SYNONYM_KEYWORD,
		DEF_KEYWORD,
		NAMESPACE_KEYWORD,
		EQUIVALENT_TO_KEYWORD,
		IS_OBSOLETE_KEYWORD,
		XREF_KEYWORD,
		SUBSET_KEYWORD
	};
}
