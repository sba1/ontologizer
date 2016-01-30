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
import ontologizer.go.Term;
import ontologizer.go.TermID;
import ontologizer.internal.InternalOntology;

public class GODOTWriterTest
{
	@Test
	public void testWriteDOT() throws Exception
	{
		InternalOntology ontology = new InternalOntology();
		File tmp = File.createTempFile("ontotest", ".dot");

		HashSet<TermID> allTerms = new HashSet<TermID>();
		for (Term t : ontology.graph)
			allTerms.add(t.getID());

		System.out.println(tmp.toURI());
		GODOTWriter.writeDOT(ontology.graph,tmp,ontology.graph.getRootTerm().getID(),allTerms,new IDotAttributesProvider() {

			@Override
			public String getDotNodeAttributes(TermID id)
			{
				return "label=\"" + id.toString() + "\"";
			}

			@Override
			public String getDotEdgeAttributes(TermID id1, TermID id2)
			{
				return null;
			}
		});;

		StringBuilder dotString = new StringBuilder();
		BufferedReader reader = new BufferedReader(new FileReader(tmp));
		String line;
		while ((line = reader.readLine()) != null)
		{
			dotString.append(line);
			dotString.append('\n');
		}
		reader.close();
		System.out.println(dotString.toString());

		Parser parser = new Parser(new FileInputStream(tmp), System.err);
		parser.parse();
		att.grappa.Graph g = parser.getGraph();

		int numNodes = 0;
		GraphEnumeration e = g.elements(GrappaConstants.NODE);
		while (e.hasMoreElements())
		{
			e.nextElement();
			numNodes++;
		}
		assertEquals(ontology.graph.getNumberOfTerms(),numNodes);

	}
}
