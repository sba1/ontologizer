package ontologizer.ontology;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ontologizer.ontology.IParserInput;
import ontologizer.ontology.OBOParserFileInput;

public class OBOParserFileInputTest
{
	@Rule
	public TemporaryFolder tmpFolder = new TemporaryFolder();

	@Test
	public void testUncompressed() throws IOException
	{
		File outFile = tmpFolder.newFile();
		PrintWriter out = new PrintWriter(new FileWriter(outFile));
		out.println("line1");
		out.println("line2");
		out.println("line3");
		out.println("line4");
		out.close();

		IParserInput input = new OBOParserFileInput(outFile.getAbsolutePath());
		BufferedReader in = new BufferedReader(new InputStreamReader(input.inputStream()));
		assertEquals("line1", in.readLine());
		assertEquals("line2", in.readLine());
		assertEquals("line3", in.readLine());
		assertEquals("line4", in.readLine());

	}
}
