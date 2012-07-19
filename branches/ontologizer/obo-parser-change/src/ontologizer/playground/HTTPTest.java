package ontologizer.playground;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;

/**
 * A test for the HTTP connection (will be used to download
 * GO files and so on)
 * 
 * @author Sebastian Bauer
 *
 */
public class HTTPTest
{
	public static void main(String[] args)
	{
		try
		{
			Proxy proxy = new Proxy(Proxy.Type.HTTP,new InetSocketAddress("realproxy.charite.de",888));
//			URL url = new URL("http://www.geneontology.org/ontology/gene_ontology.obo");
			URL url = new URL("http://www.heise.de");
			HttpURLConnection http = (HttpURLConnection)url.openConnection(proxy);
			http.connect();

			BufferedInputStream input = new BufferedInputStream(http.getInputStream());
			byte [] buffer = new byte[16384];
			int len;
			while ((len = input.read(buffer))!=-1)
			{
				for (int i=0;i<len;i++)
					System.out.print((char)buffer[i]);
			}
		} catch (MalformedURLException e)
		{
			e.printStackTrace();
		} catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
