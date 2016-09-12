package ontologizer.association;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import ontologizer.types.ByteString;

public class AnnotationContextTest
{
	@Test
	public void testAnnotationContextOnlySymbols()
	{
		List<ByteString> symbols = Arrays.asList(new ByteString("ITEM1"), new ByteString("ITEM2"), new ByteString("ITEM3"));
		AnnotationContext ac = new AnnotationContext(symbols, null, null);
		assertEquals(3,  ac.getSymbols().length);
	}
}
