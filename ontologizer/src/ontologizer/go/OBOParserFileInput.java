package ontologizer.go;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.util.zip.GZIPInputStream;

public class OBOParserFileInput implements IOBOParserInput
{
	FileInputStream fis;
	private InputStream is;
	private FileChannel fc;

	public OBOParserFileInput(String filename) throws IOException
	{
		fis = new FileInputStream(filename);

		try
		{
			is = new GZIPInputStream(fis);
		} catch (IOException exp)
		{
			fis.close();
			is = fis = new FileInputStream(filename);
		}

		fc = fis.getChannel();
	}

	@Override
	public InputStream inputStream()
	{
		return is;
	}

	@Override
	public void close()
	{
		try
		{
			fis.close();
		} catch (IOException e)
		{
		}
	}

	@Override
	public int getSize()
	{
		try
		{
			return (int)fc.size();
		} catch (IOException e)
		{
		}
		return -1;
	}

	@Override
	public int getPosition()
	{
		try
		{
			return (int)fc.position();
		} catch (IOException e)
		{
		}
		return -1;
	}
}
