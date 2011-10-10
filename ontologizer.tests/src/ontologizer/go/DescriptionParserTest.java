package ontologizer.go;

import java.util.ArrayList;

import junit.framework.TestCase;

public class DescriptionParserTest extends TestCase
{
	private final String testDescription = "`Distention` (PATO:0001602) of the `abdomen` (FMA:9577).";

	private final String [] txts = new String[]{
			"Distention",
			" of the ",
			"abdomen",
			"."
	};
	private final String [] tids = new String[]{
			"PATO:0001602",
			null,
			"FMA:9577",
			null
	};

	public void test()
	{
		final ArrayList<String> refList = new ArrayList<String>();

		DescriptionParser.parse(testDescription, new DescriptionParser.IDescriptionPartCallback() {
			int i;

			public boolean part(String txt, String ref)
			{
				refList.add(ref);

				assertEquals(txts[i],txt);
				assertEquals(tids[i],ref);

				i++;
				return true;
			}
		});
		assertEquals(4,refList.size());
	}

	public void test2()
	{
		final ArrayList<String> refList = new ArrayList<String>();
		DescriptionParser.parse("Single Line", new DescriptionParser.IDescriptionPartCallback() {
			int i;

			public boolean part(String txt, String ref)
			{
				refList.add(ref);

				assertEquals("Single Line", txt);
				assertNull(ref);

				i++;
				return true;
			}
		});
		assertEquals(1,refList.size());
	}
}
