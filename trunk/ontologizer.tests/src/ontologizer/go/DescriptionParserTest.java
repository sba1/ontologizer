package ontologizer.go;

import java.util.ArrayList;

import junit.framework.TestCase;

public class DescriptionParserTest extends TestCase
{
	private final String testDescription = "`Distention` (PATO:0001602) of the `abdomen` (FMA:9577).";

	private final String [] txts = new String[]{
			"Distention",
			"of the",
			"abdomen",
			"."
	};
	private final TermID [] tids = new TermID[]{
			new TermID(new Prefix("PATO:"),1602),
			null,
			new TermID(new Prefix("FMA:"),9577),
			null
	};

	public void test()
	{
		final ArrayList<TermID> termIDList = new ArrayList<TermID>();

		DescriptionParser.parse(testDescription, new DescriptionParser.IDescriptionPartCallback() {
			int i;

			public boolean part(String txt, TermID tid)
			{
				termIDList.add(tid);

				assertEquals(txts[i],txt);
				assertEquals(tids[i],tid);

				i++;
				return true;
			}
		});

		assertEquals(4,termIDList.size());
	}
}
