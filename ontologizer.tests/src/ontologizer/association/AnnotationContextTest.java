package ontologizer.association;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.hamcrest.CustomMatcher;
import org.junit.Test;

import ontologizer.types.ByteString;

/**
 * Simple matcher checking if value is between two other ones (inclusive).
 *
 * @author Sebastian Bauer
 */
class Between extends CustomMatcher<Integer>
{
	public Between(String description)
	{
		super(description);
	}

	private int min;
	private int max;

	public static Between between(int min, int max)
	{
		Between b = new Between("Value being between " + min + " and " + max);
		b.min = min;
		b.max = max;
		return b;
	}

	@Override
	public boolean matches(Object item)
	{
		if (!(item instanceof Integer))
		{
			return false;
		}
		int value = (Integer)item;

		return value >= min && value <= max;
	}
}


public class AnnotationContextTest
{
	public static Between between(int min, int max)
	{
		return Between.between(min, max);
	}

	private static ByteString ITEM1 = new ByteString("ITEM1");
	private static ByteString ITEM2 = new ByteString("ITEM2");
	private static ByteString ITEM3 = new ByteString("ITEM3");

	@Test
	public void testAnnotationContextOnlySymbols()
	{
		List<ByteString> symbols = Arrays.asList(ITEM1, ITEM2, ITEM3);
		AnnotationContext ac = new AnnotationContext(symbols, null, null);
		assertEquals(3,  ac.getSymbols().length);

		int id1 = ac.mapSymbol(ITEM1);
		int id2 = ac.mapSymbol(ITEM2);
		int id3 = ac.mapSymbol(ITEM3);

		assertThat(id1, between(0,2));
		assertThat(id2, between(0,2));
		assertThat(id3, between(0,2));

		assertTrue(id1 != id2 && id2 != id3);
	}
}
