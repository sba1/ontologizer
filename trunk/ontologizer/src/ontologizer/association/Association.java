package ontologizer.association;

import java.util.regex.*;

import ontologizer.ByteString;
import ontologizer.go.TermID;

/**
 * <P>
 * Objects of this class represent individual associations as defined by GO
 * association files.
 * </P>
 * <P>
 * The meaning of the attributes is described in detail at:
 * http://www.geneontology.org/doc/GO.annotation.html#file
 * </P>
 * <P>
 * The file format is (in brief)
 * <OL>
 * <LI> DB (database contributing the association file; cardinality=1; example:
 * WB)</LI>
 * <LI> DB_Object (unique identifier in DB for the item ebing annotated;
 * cardinality=1; example CE00429)</LI>
 * <LI> DB_Object_Symbol (unique symbol for object being matched that has
 * meaning to biologist, e.g., a gene name; Cardinality=1, example: cdc-25.3</LI>
 * <LI> NOT: annotators are allowed to prefix NOT if a gene product is <B>not</B>
 * associated with some GO term. cardinality=0,1, example "NOT GO:nnnnnnnn"</LI>
 * <LI> GOid: The GO identifier. Cardinality=1, example = GO:0007049</LI>
 * <LI> DB:Reference database ref. Cardinality 1, >1 (separate by |),
 * example:PUBMED:9651482</LI>
 * <LI> Evidence: one of IMP, IGI, IPI,ISS, IDA, IEP, IEA, TAS, NAS, ND, IC.
 * Cardinality = 1</LI>
 * <LI> With (or) from, cardinality 0,1,>1</LI>
 * <LI> Aspect: One of P(biological process), F (molecular function), C
 * (cellular componment). Cardinality=1</LI>
 * <LI> DB_Object_Name: Name of Gene or Gene Product. Cardinality 0,1, >1 (e.g.,
 * ZK637.11)</LI>
 * <LI> Synonym: Gene symbol or other text. cardinality 0,1,>1 </LI>
 * <LI> DB_Object_Type: One of gene, protein, protein_structure. Cardinality 1
 * </LI>
 * <LI> Taxon taxonomic identifiers, Cardinality 1,2</LI>
 * <LI> ???????????? DATE HERE ????????? </LI>
 * <LI> Assigned_by The database which made the annotation. Cardinality 1.</LI>
 * </OL>
 * Objects of this class are used for one line of an annotation file. We are
 * interested in parsing the DB_Object_Symbol, NOT, aspect, and synonyms. The
 * English name of a GO term corresponding to the GOid is not provided in the
 * association file, but has to be supplied from the GO termdb.xml file. See the
 * Controller class for details. Note that not all entries in association files
 * conform entirely to this scheme. For instance, in some cases, DB_Object and
 * DB_Object_Symbol are null.
 * </P>
 * 
 * @author Peter Robinson
 * @version 0.2 (2005-02-28)
 */

public class Association
{
	/** A unique identifier in the database such as an accession number */
	private ByteString DB_Object;

	/** A unique symbol such as a gene name (primary id) */
	private ByteString DB_Object_Symbol;

	/** The evidence */
	private ByteString evidence;
	
	/** The aspect */
	private ByteString aspect;

	/** e.g., GO:0015888 */
	private TermID termID;

	/** Has a not qualifier? */
	private boolean notQualifier;

	/* TODO: Add "contributes_to" or "colocalizes_with" qualifier */

	/** A synonym for the identifier */
	private ByteString synonym;

	/** Used to hold the tab-separated fields of each line during parsing */
	private final static String DELIM = "\t";

	/** Number of fields in each gene_association.*** line */
	private final static int FIELDS = 15;

	/** Index of dbObject field */
	private final static int DBOBJECTFIELD = 1;

	/** Index of dbObjectSymbol field */
	private final static int DBOBJECTSYMBOLFIELD = 2;

	/** Index of NOT field */
	private final static int QUALIFIERFIELD = 3;
//	private final static String QUALIFIERVALS[] =
//		new String[] {"", "NOT", "contributes_to", "colocalizes_with"};

	/** Index of GO:id field */
	private final static int GOFIELD = 4;

	/** Index of evidence field */
	private final static int EVIDENCEFIELD = 7;

	/** Index of aspect field */
	private final static int ASPECTFIELD = 8;

	/** Index of synonym field */
	private final static int SYNONYMFIELD = 10;

	/** Index fo dbObjectType field */
	private final static int DBOBJECTTYPEFIELD = 11;

	/** Use this pattern to split tab-separated fields on a line */
	private static final Pattern pattern = Pattern.compile(DELIM);

	private static final ByteString emptyString = new ByteString("");
	/**
	 * @param line :
	 *            line from a gene_association file
	 * @throws Exception which contains a failure message
	 */
	public Association(String line) throws Exception
	{
		DB_Object = DB_Object_Symbol = synonym = emptyString;
		termID = null;
		parseLine(line);
	}

	public Association(ByteString db_object_symbol, int goIntID)
	{
		DB_Object = synonym = new ByteString("");
		DB_Object_Symbol = db_object_symbol;
		termID = new TermID(goIntID);
	}

	public Association(ByteString db_object_symbol, TermID goID)
	{
		DB_Object = synonym = new ByteString("");
		DB_Object_Symbol = db_object_symbol;
		this.termID = goID;
	}

	public Association(ByteString db_object_symbol, String goTerm)
	{
		DB_Object = synonym = new ByteString("");
		DB_Object_Symbol = db_object_symbol;
		termID = new TermID(goTerm);
	}

	/**
	 * Parse one line and distribute extracted values. Note that we use the
	 * String class method trim to remove leading and trailing whitespace, which
	 * occasionally is found (mistakenly) in some GO association files (for
	 * instance, in 30 entries of one such file that will go nameless :-) ).
	 * 
	 * We are interested in 2) DB_Object, 3) DB_Object_Symbol, NOT, GOid,
	 * Aspect, synonym Use Java 1.4's String split method, which is similar to
	 * the Perl split function.
	 * 
	 * @param line
	 *            A line from a gene_association file
	 * @throws Exception which contains a failure message
	 */
	private void parseLine(String line) throws Exception
	{
		/* Split the tab-separated line: */
		String[] fields = pattern.split(line, FIELDS);

		this.DB_Object = new ByteString(fields[DBOBJECTFIELD].trim());

		/*
		 * DB_Object_Symbol should always be at 2 (or is missing, then this
		 * entry wont make sense for this program anyway)
		 */
		this.DB_Object_Symbol = new ByteString(fields[DBOBJECTSYMBOLFIELD].trim());

		this.evidence = new ByteString(fields[EVIDENCEFIELD].trim());
		this.aspect = new ByteString(fields[ASPECTFIELD].trim());
		
		/* TODO: There are new fields (colocalizes_with (a component) and 
		 * contributes_to (a molecular function term) ), checkout how
		 * these should be fitted into this framework */

		String [] qualifiers = fields[QUALIFIERFIELD].trim().split("|");
		for (String qual : qualifiers)
			if (qual.equalsIgnoreCase("not")) notQualifier = true;   
		
		/* Find GO:nnnnnnn */
		fields[GOFIELD] = fields[GOFIELD].trim();
		this.termID = new TermID(fields[GOFIELD]);

		/* aspect can be P, F or C */
/*		if (fields[ASPECTFIELD].equals("P") 
				|| fields[ASPECTFIELD].equals("F")
				|| fields[ASPECTFIELD].equals("C"))
		{
			this.aspect = fields[ASPECTFIELD];
		} else
		{
			throw new Exception("Parsing the aspect field failed");
		}*/

		this.synonym = new ByteString(fields[SYNONYMFIELD].trim());

	}

	/**
	 * Returns the Term ID of this association.
	 * 
	 * @return the term id.
	 */
	public TermID getTermID()
	{
		return termID;
	}

	/**
	 * Returns the objects symbol (primary id).
	 * 
	 * @return
	 */
	public ByteString getObjectSymbol()
	{
		return DB_Object_Symbol;
	}

	/**
	 * Returns the association's synonym.
	 * 
	 * @return
	 */
	public ByteString getSynonym()
	{
		return synonym;
	}

	/**
	 * Returns whether this association is qualified as "NOT".
	 * @return
	 */
	public boolean hasNotQualifier()
	{
		return notQualifier;
	}

	/**
	 * @return name of DB_Object, usually a name that has meaning in a database,
	 *         for instance, a swissprot accession number
	 */
	public ByteString getDB_Object()
	{
		return DB_Object;
	}
	
	/**
	 * Returns the aspect.
	 * 
	 * @return
	 */
	public ByteString getAspect()
	{
		return aspect;
	}
	
	/**
	 * Returns the evidence code of the annotation.
	 * 
	 * @return
	 */
	public ByteString getEvidence()
	{
		return evidence;
	}
}
