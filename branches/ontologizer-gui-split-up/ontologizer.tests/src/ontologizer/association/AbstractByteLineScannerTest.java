package ontologizer.association;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

import junit.framework.Assert;
import junit.framework.TestCase;

public class AbstractByteLineScannerTest extends TestCase
{
	public void testBigFile() throws FileNotFoundException, IOException
	{
		InputStream	is = new GZIPInputStream(new FileInputStream("data/gene_ontology.1_2.obo.gz"));

		final BufferedReader br = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream("data/gene_ontology.1_2.obo.gz"))));

		class TestByteLineScanner extends AbstractByteLineScanner
		{
			public int actualLineCount;
			public int expectedLineCount;

			public TestByteLineScanner(InputStream is)
			{
				super(is);
			}

			@Override
			public boolean newLine(byte[] buf, int start, int len)
			{
				actualLineCount++;

				StringBuilder actualString = new StringBuilder();
				for (int i=start;i<start+len;i++)
					actualString.append((char)buf[i]);
				String expectedString;
				try {
					expectedString = br.readLine();
					expectedLineCount++;
				} catch (IOException e) { throw new RuntimeException(e);}
				Assert.assertEquals(expectedString,actualString.toString());
				return true;
			}
		};

		TestByteLineScanner tbls = new TestByteLineScanner(is);
		tbls.scan();

		assertEquals(tbls.expectedLineCount,tbls.actualLineCount);
		assertNull(br.readLine());

		br.close();
	}

	public void testMissingNewLineAtLineEnd() throws IOException
	{
		ByteArrayInputStream bais = new ByteArrayInputStream("test\ntest2".getBytes());
		class TestByteLineScanner extends AbstractByteLineScanner
		{
			public int lines;

			public TestByteLineScanner(InputStream is)
			{
				super(is);
			}

			@Override
			public boolean newLine(byte[] buf, int start, int len)
			{
				lines++;
				return true;
			}
		}

		TestByteLineScanner tbls = new TestByteLineScanner(bais);
		tbls.scan();
		assertEquals(2, tbls.lines);
	}
}
