package ontologizer.association;

import java.io.IOException;
import java.io.InputStream;

/**
 * This is a simple class that can be used to read an input stream
 * in byte representation in a line-based manner.
 * 
 * @author Sebastian Bauer
 */
abstract public class AbstractByteLineScanner
{
	private InputStream is;
	private final int BUF_SIZE = 65536;
	
	byte [] byteBuf = new byte[2*BUF_SIZE];

	public AbstractByteLineScanner(InputStream is)
	{
		this.is = is;
	}
	
	public void scan() throws IOException
	{
		int read;
		int read_offset = 0;
		while ((read = is.read(byteBuf, read_offset, BUF_SIZE) + read_offset) > 0)
		{
			int line_start = 0;
			int pos = 0;
			
			while (pos < read)
			{
				if (byteBuf[pos] == '\n')
				{
					newLine(byteBuf, line_start, pos - line_start);
					line_start = pos + 1;
				}
				pos++;
			}

			
			System.arraycopy(byteBuf, line_start, byteBuf,0, read - line_start);
			read_offset = read - line_start;
		}
	}
	
	/**
	 * Called whenever a new line was encountered.
	 * 
	 * @param buf
	 * @param start
	 * @param len
	 * @return
	 */
	abstract public boolean newLine(byte [] buf, int start, int len);
}
