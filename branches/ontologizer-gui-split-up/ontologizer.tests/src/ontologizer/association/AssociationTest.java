package ontologizer.association;

import ontologizer.go.TermID;
import ontologizer.types.ByteString;
import junit.framework.TestCase;

public class AssociationTest extends TestCase
{
	private final String EXAMPLE =
			"SGD\t"+
			"S000004660\t"+
			"AAC1\t\t"+
			"GO:0015886\t"+
			"SGD_REF:S000127569|PMID:18728780\t" +
			"IPI\t\t"+
			"P\t"+
			"Mitochondrial inner membrane ADP/ATP translocator	"+
			"YMR056C\t"+
			"gene\t"+
			"taxon:4932\t"+
			"20100308\t"+
			"SGD";		

	public void testLine()
	{
		Association a = Association.createFromGAFLine(EXAMPLE);

		assertEquals("S000004660", a.getDB_Object().toString());
		assertEquals(new TermID("GO:0015886"),a.getTermID());
		assertEquals("IPI",a.getEvidence().toString());
		assertEquals("YMR056C", a.getSynonym().toString());
		assertEquals("AAC1", a.getObjectSymbol().toString());
	}
	
	public void testByteStringLine()
	{
		Association a = Association.createFromGAFLine(new ByteString(EXAMPLE));

		assertEquals("S000004660", a.getDB_Object().toString());
		assertEquals(new TermID("GO:0015886"),a.getTermID());
		assertEquals("IPI",a.getEvidence().toString());
		assertEquals("YMR056C", a.getSynonym().toString());
		assertEquals("AAC1", a.getObjectSymbol().toString());
	}

}
