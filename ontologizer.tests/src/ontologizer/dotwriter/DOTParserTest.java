package ontologizer.dotwriter;

import static org.junit.Assert.assertEquals;

import java.io.FileInputStream;

import org.junit.Test;

import att.grappa.GraphEnumeration;
import att.grappa.GrappaConstants;
import att.grappa.Parser;

public class DOTParserTest
{
	@Test
	public void testParseDOTwithFloat() throws Exception
	{
		Parser parser = new Parser(new FileInputStream("data/test.dot"), System.err);
		parser.debug_parse(0);
		att.grappa.Graph g = parser.getGraph();

		int numNodes = 0;
		GraphEnumeration e = g.elements(GrappaConstants.NODE);
		while (e.hasMoreElements())
		{
			e.nextElement();
			numNodes++;
		}
		assertEquals(0, numNodes);
	}
}
