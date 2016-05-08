package ontologizer.dotwriter;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.util.HashSet;

import org.junit.Test;

import att.grappa.GraphEnumeration;
import att.grappa.GrappaConstants;
import att.grappa.Parser;
import ontologizer.internal.InternalOntology;
import ontologizer.ontology.Term;
import ontologizer.ontology.TermID;

public class DOTParserTest
{
	@Test
	public void testParseDOTwithFloat() throws Exception
	{
		Parser parser = new Parser(new FileInputStream("data/test.dot"), System.err);
		parser.debug_parse(10);
		att.grappa.Graph g = parser.getGraph();

		int numNodes = 0;
		GraphEnumeration e = g.elements(GrappaConstants.NODE);
		while (e.hasMoreElements())
		{
			e.nextElement();
			numNodes++;
		}
	}
}
