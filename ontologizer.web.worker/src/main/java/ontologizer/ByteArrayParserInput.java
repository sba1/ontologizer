package ontologizer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import ontologizer.ontology.IParserInput;

final class ByteArrayParserInput implements IParserInput
{
	private int size;
	private ByteArrayInputStream bais;
	private InputStream is;

	public ByteArrayParserInput(byte [] buf)
	{
		size = buf.length;
		bais = new ByteArrayInputStream(buf);

		if (buf.length >= 2 && buf[0] == (byte)0x1f && buf[1] == (byte)0x8b)
		{
			try
			{
				is = new GZIPInputStream(bais);
			} catch (IOException e)
			{
				is = bais;
			}
		} else
		{
			is = bais;
		}
	}

	@Override
	public InputStream inputStream()
	{
		return is;
	}

	@Override
	public void close()
	{
	}

	@Override
	public int getSize()
	{
		return size;
	}

	@Override
	public int getPosition()
	{
		return size - bais.available();
	}

	@Override
	public String getFilename()
	{
		return "";
	}
}
