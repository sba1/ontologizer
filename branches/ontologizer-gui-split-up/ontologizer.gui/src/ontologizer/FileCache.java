package ontologizer;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Thread-class for downloading.
 * 
 * @author Sebastian Bauer
 */
class DownloadThread extends Thread
{
	private static Logger logger = Logger.getLogger(DownloadThread.class.getName());

	private List<FileCache.FileDownload> callbackSubscriberList = new LinkedList<FileCache.FileDownload>();
	
	private FileCache.FileDownload downloadCallback;
	private File destFile;
	private URL u;
	private Proxy proxy;

	private URLConnection urlConnection;

	private int contentLength = -1;
	private int contentActual = 0;

	/**
	 * @param u defines the URL where to download. 
	 * @param destFile defines the file to which the download is written.
	 * @param threadCallback basic callback.
	 * @param callback callback from the issuer.
	 */
	public DownloadThread(URL u, File destFile)
	{
		super(FileCache.downloadThreadGroup,"Download Thread");
		
		this.u = u;
		this.destFile = destFile;

		String proxyHost = GlobalPreferences.getProxyHost();
		if (proxyHost != null && proxyHost.length()>0)
			proxy = new Proxy(Proxy.Type.HTTP,new InetSocketAddress(proxyHost,GlobalPreferences.getProxyPort()));
	}

	/**
	 * Sets the download callback.
	 * 
	 * @param downloadCallback
	 */
	public void setDownloadCallback(FileCache.FileDownload downloadCallback)
	{
		this.downloadCallback = downloadCallback;
	}

	/**
	 * Returns the content length or -1 if it has not been defined yet.
	 * 
	 * @return
	 */
	public int getContentLength()
	{
		synchronized (this)
		{
			return contentLength;
		}
	}

	/**
	 * Returns the currently transfered amount.
	 * 
	 * @return
	 */
	public int getContentActual()
	{
		synchronized (this)
		{
			return contentActual;
		}
	}

	/**
	 * Returns the list of subscribers.
	 * 
	 * @return
	 */
	public List<FileCache.FileDownload> getCallbackSubscriberList()
	{
		return callbackSubscriberList;
	}

	/**
	 * Abort the connection.
	 */
	public void abort()
	{
		interrupt();

		if (urlConnection instanceof HttpURLConnection)
		{
			HttpURLConnection httpConnection = (HttpURLConnection) urlConnection;
			httpConnection.disconnect();
		}
	}

	/**
	 * The download runner.
	 */
	public void run()
	{
		byte [] buf = new byte[32768];
		int read;
			
		InputStream stream;
		try
		{
			logger.fine("Open connection");

			if (proxy != null) urlConnection = u.openConnection(proxy);
			else urlConnection = u.openConnection();

			urlConnection.setConnectTimeout(10000);

			int cl = urlConnection.getContentLength();

			logger.fine("Content-Length = " + cl);
			synchronized (this)
			{
				contentLength = cl;
			}
			/* Forward content length */
			downloadCallback.initProgress(contentLength);
			
			stream = urlConnection.getInputStream();
			BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(destFile));

			while ((read = stream.read(buf)) > 0)
			{
				bos.write(buf,0,read);
				synchronized (this)
				{
					contentActual += read;
				}

				/* Forward current state */
				downloadCallback.progress(contentActual);
			}
			bos.close();
			
			/* Forward ready status */
			downloadCallback.ready(null, destFile.getCanonicalPath());
		} catch (Exception e)
		{
			logger.log(Level.SEVERE, "Exception while downloading a file.", e);
			/* Forward ready status */
			downloadCallback.ready(e, null);
		}
	}

}

/**
 * Private class which contains informations about the cached File.
 *
 * @author Sebastian Bauer
 */
class CachedFile implements Serializable
{
	private static final long serialVersionUID = 1L;

	public String cachedFilename;
	public String url;
}

/**
 * This class handles the file i/o. Files are cached in the
 * cache directory.
 * 
 * @author Sebastian Bauer
 */
public class FileCache
{
	/** For our logging */
	private static Logger logger = Logger.getLogger(FileCache.class.getCanonicalName());

	/** That's the thread group */
	protected static ThreadGroup downloadThreadGroup;

	static public interface FileDownload
	{
		public void initProgress(int max);
		public void progress(int current);
		public void ready(Exception ex, String name);
	}

	/**
	 * The global callback for the file cache.
	 * 
	 * @author Sebastian Bauer
	 */
	static public interface FileCacheUpdateCallback
	{
		void update(String url);
		void exception(Exception exception, String url);
	};

	static public enum FileState
	{
		NOT_CACHED,
		WAITING,
		DOWNLOADING,
		CACHED,
		LOCAL
	};

	private static Map<String,CachedFile> fileCache;
	
	/** Also used the arbitrate access to fileCache and other stuff */
	private static Map<String,DownloadThread> downloadHashMap;
	private static List<FileCacheUpdateCallback> cacheUpdateCallbackList;

	static
	{
		fileCache = new HashMap<String, CachedFile>();
		downloadHashMap = new HashMap<String,DownloadThread>();
		cacheUpdateCallbackList = new LinkedList<FileCacheUpdateCallback>();
		downloadThreadGroup = new ThreadGroup("Download Thread Group");
	};
	
	private static String cacheDirectory;

	public static void abortAllDownloads()
	{
		try
		{
			synchronized(downloadThreadGroup)
			{
				downloadThreadGroup.interrupt();

				for (DownloadThread dt : downloadHashMap.values())
					dt.abort();

				while ( downloadThreadGroup.activeCount() > 0 )
					downloadThreadGroup.wait( 10 );
			}
		
		} catch (InterruptedException e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Sets the directory in which remote files are cached.
	 * 
	 * @param cachePath
	 */
	public static void setCacheDirectory(String cachePath)
	{
		FileCache.cacheDirectory = cachePath;
		logger.info("Cache directory set to \"" + cachePath + "\"");
		new File(FileCache.cacheDirectory).mkdirs();
		
		File index = new File(cacheDirectory,".index");
		try
		{
			BufferedReader br = new BufferedReader(new FileReader(index));
			String line;
			while ((line = br.readLine()) != null)
			{
				int idx = line.indexOf('=');
				if (idx != -1)
				{
					String cacheName = line.substring(0,idx);
					String url = line.substring(idx+1);
					
					CachedFile cf = new CachedFile();
					cf.cachedFilename = cacheName;
					cf.url = url;

					fileCache.put(url, cf);
				}
			}
		} catch (FileNotFoundException e)
		{
			logger.log(Level.WARNING, "", e);
		} catch (IOException e)
		{
			logger.log(Level.WARNING, "", e);
		}
	}
	
	/**
	 * Returns the full path of the cache directory.
	 * 
	 * @return
	 */
	public static String getCacheDirectory()
	{
		return cacheDirectory;
	}
	
	/**
	 * Stores the contents of the cache.
	 */
	private static void storeCache()
	{
		File index = new File(cacheDirectory,".index");
	
		synchronized (downloadHashMap)
		{
			try
			{
				PrintWriter bw = new PrintWriter(index);

				for (String key : fileCache.keySet())
				{
					CachedFile cf = fileCache.get(key);
					bw.println(cf.cachedFilename + "=" + cf.url);
				}
				
				bw.close();
			} catch (FileNotFoundException e)
			{
			}		
		}
	}

	/**
	 * Add a new callback which is invoked on a cache update.
	 * 
	 * @param callback
	 */
	public static void addUpdateCallback(FileCacheUpdateCallback callback)
	{
		synchronized (cacheUpdateCallbackList)
		{
			cacheUpdateCallbackList.add(callback);
		}
	}
	
	/**
	 * Removes the given callback added with addUpdateCallback().
	 * @param callback
	 * @see addUpdateCallback()
	 */
	public static void removeUpdateCallback(FileCacheUpdateCallback callback)
	{
		synchronized (cacheUpdateCallbackList)
		{
			cacheUpdateCallbackList.remove(callback);
		}
	}

	private static boolean isRemoteFile(String url)
	{
		if (url.startsWith("http://"))
			return true;
		if (url.startsWith("ftp://"))
			return true;
		return false;
	}

	/**
	 * Returns the name of the local file representing
	 * the url. 
	 * 
	 * @param url
	 * @return null if file is not stored locally.
	 */
	public static String getLocalFileName(String url)
	{
		if (!isRemoteFile(url))
			return url;
		
		synchronized (downloadHashMap)
		{
			CachedFile local = fileCache.get(url);
			if (local != null)
			{
				return local.cachedFilename;
			}
			return null;
		}
	}

	/**
	 * Starts to open the given url. If file is in cache, the file name in this
	 * cache is returned otherwise null.
	 * @param url
	 * @return
	 * @throws IOException
	 */
	public static String open(final String url) throws IOException
	{
		return open(url,null);
	}

	/**
	 * 
	 * @param url
	 * @param basePath
	 * @param run
	 * @return
	 * @throws IOException
	 */
	public static String open(final String url, FileDownload ready) throws IOException
	{
		if (cacheDirectory == null)
			return url;

		if (!url.startsWith("http://"))
			return url;

		/* If there is currently a download thread for this file download
		 * subscribe to its callbacks.
		 */
		synchronized (downloadHashMap)
		{
			DownloadThread dt = downloadHashMap.get(url);
			if (dt != null)
			{
				synchronized (dt)
				{
					logger.fine("Added another request for the download for URL \"" + url + "\"");

					if (ready != null)
						dt.getCallbackSubscriberList().add(ready);
					return null;
				}
			}

			/* The file could be in the cache as well */
			if (fileCache.containsKey(url))
			{
				String cachedFilename = fileCache.get(url).cachedFilename;
				if (new File(cachedFilename).exists())
				{
					logger.fine("URL \"" + url + "\" has already been cached.");
					return cachedFilename;
				}
				fileCache.remove(url);
			}
		}

		/* Okay, it's neither about to be downloaded nor in the cache. So,
		 * start the download process now.
		 */
		File destFile;
		int hashCode = url.hashCode();
		int t = 0;

		/* Find appropriate cached file name */
		do
		{
			String name = String.format("%x_%d",hashCode,t);
			destFile = new File(cacheDirectory,name);
			t++;
		} while (destFile.exists());


		logger.fine("Starting new download thread for URL \"" + url + "\" (cached as \""+destFile.getAbsolutePath()+"\"");

		/* Leave the process of downloading to the separate thread */
		final DownloadThread dt = new DownloadThread(new URL(url),destFile);

		FileDownload cfd = new FileDownload()
		{
			private long lastProgressMillis;

			public void initProgress(int max)
			{
				synchronized (dt)
				{
					for (FileDownload fd : dt.getCallbackSubscriberList())
						fd.initProgress(max);
				}
				
				/* Notify the global updates */
				synchronized (cacheUpdateCallbackList)
				{
					for (FileCacheUpdateCallback fcuc : cacheUpdateCallbackList)
						fcuc.update(url);
				}
			}
			
			public void progress(int current)
			{
				long progressMillis = System.currentTimeMillis();
				if (progressMillis - lastProgressMillis > 200)
				{
					synchronized (dt)
					{
						for (FileDownload fd : dt.getCallbackSubscriberList())
							fd.progress(current);
					}
	
					/* Notify the global updates. TODO: The progress update should be global here */
					synchronized (cacheUpdateCallbackList)
					{
						for (FileCacheUpdateCallback fcuc : cacheUpdateCallbackList)
							fcuc.update(url);
					}
				}
			};
			
			public void ready(Exception ex, String name)
			{
				synchronized (downloadHashMap)
				{
					downloadHashMap.remove(url);
					if (name != null)
					{
						CachedFile cf = new CachedFile();
						cf.cachedFilename = name;
						cf.url = url;
						fileCache.put(cf.url, cf);
						storeCache();
					}
				}

				synchronized (dt)
				{
					for (FileDownload fd : dt.getCallbackSubscriberList())
						fd.ready(ex, name);
				}

				/* Notify the global updates */
				if (ex != null)
				{
					for (FileCacheUpdateCallback fcuc : cacheUpdateCallbackList)
						fcuc.exception(ex,url);
				} else
				{
					synchronized (cacheUpdateCallbackList)
					{
						for (FileCacheUpdateCallback fcuc : cacheUpdateCallbackList)
							fcuc.update(url);
					}
				}
			}
		};
		
		if (ready != null)
			dt.getCallbackSubscriberList().add(ready);
		dt.setDownloadCallback(cfd);
		downloadHashMap.put(url, dt);
		
		/* Notify the global updates */
		synchronized (cacheUpdateCallbackList)
		{
			for (FileCacheUpdateCallback fcuc : cacheUpdateCallbackList)
				fcuc.update(url);
		}

		dt.start();
		return null;		
	}

	/**
	 * Returns the cached filename. Note that the function may block the calling task
	 * until the file is retrieved!
	 * 
	 * @param url
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static String getCachedFileNameBlocking(String url) throws IOException, InterruptedException
	{
		return getCachedFileNameBlocking(url, null);
	}

	/**
	 * Returns the cached filename. Note that the function may block the calling task
	 * until the file is retrieved!
	 * 
	 * @param url
	 * @param ready
	 * @return
	 * 
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	public static String getCachedFileNameBlocking(String url, final FileDownload ready) throws IOException, InterruptedException
	{
		Object lock = new Object();
		
		/**
		 * Handles the FileDownload Callback. The given lock object is notified
		 * once the download has been completed, thus, a synchron download can be
		 * mimiced.
		 * 
		 * @author Sebastian Bauer
		 */
		class SynchronDownloaderCallback implements	FileCache.FileDownload
		{
			private final Object lock;
			private String name;
			private Exception ex;
		
			private SynchronDownloaderCallback(Object lock) { this.lock = lock; }

			public String getName() { return name; }
			public Exception getException() { return ex; }
			
			public void initProgress(int max) { if (ready!=null) ready.initProgress(max); }
			public void progress(int current) { if (ready!=null) ready.progress(current); }
			public void ready(Exception ex, String name)
			{
				if (name != null) this.name = name;
				if (ex != null) this.ex = ex; 

				synchronized (lock)
				{
					lock.notifyAll();
				}
				if (ready != null) ready.ready(ex, name);
			}
		}

		synchronized (lock)
		{
			SynchronDownloaderCallback sdc = new SynchronDownloaderCallback(lock);
			String newPath = FileCache.open(url, sdc);
			
			if (newPath == null)
			{
				Exception ex;

				lock.wait();
				
				ex = sdc.getException();
				if (ex != null)
				{
					if (ex instanceof IOException)
						throw (IOException)ex;
					throw new RuntimeException(sdc.getException());
				}
				return sdc.getName();
			} else return newPath;
		}
	}

	/**
	 * Returns the state of the URL with respect to the cache.
	 * 
	 * @param url
	 * @return
	 */
	public static FileState getState(String url)
	{
		if (!url.startsWith("http://"))
			return FileState.LOCAL;

		synchronized (downloadHashMap)
		{
			DownloadThread dt = downloadHashMap.get(url);
			if (dt != null)
			{
				if (dt.getContentActual() == 0)
					return FileState.WAITING;
				return FileState.DOWNLOADING;
			}

			CachedFile cf = fileCache.get(url);
			if (cf != null && new File(cf.cachedFilename).exists())
				return FileState.CACHED;
		}
		
		return FileState.NOT_CACHED;
	}
	
	/**
	 * Returns whether an open XXXBlocking call would block
	 * or not.
	 * 
	 * @param url
	 * @return
	 */
	public static boolean isNonBlocking(String url)
	{
		FileState fs = getState(url);
		if (fs == FileState.LOCAL || fs == FileState.CACHED)
			return true;
		return false;

	}

	/**
	 * Returns a formatted string that represents the time
	 * at which the given URL was downloaded.
	 * 
	 * @param url
	 * @return
	 */
	public static String getDownloadTime(String url)
	{
		synchronized (downloadHashMap)
		{
			FileState fs = getState(url);
			if (fs == FileState.CACHED)
			{
				CachedFile cf = fileCache.get(url);
				if (cf != null)
				{
					File f = new File(cf.cachedFilename);
					long modTime = f.lastModified();
					SimpleDateFormat sdf = new SimpleDateFormat();
					Date date = new Date(modTime);
					return sdf.format(date);
					
				}
				return "Unknown";
			}
			
			return getPathInfo(url);
		}
	}
	
	public static String getPathInfo(String url)
	{
		synchronized (downloadHashMap)
		{
			FileState fs = getState(url);
			if (fs == FileState.CACHED) return "Cached";
			if (fs == FileState.NOT_CACHED) return "Not cached";
			if (fs == FileState.WAITING) return "Waiting for download";
			if (fs == FileState.LOCAL) return "Local";

			DownloadThread dt = downloadHashMap.get(url);
			if (dt != null)
			{
				int length = dt.getContentLength();
				if (length != -1)
				{
					int currentPos = dt.getContentActual();

					return String.format("Downloading (%d%%)",currentPos * 100 / length);
				}
			}
			
			return "Downloading";
		}
	}

	public static void invalidate(String url)
	{
		synchronized (downloadHashMap)
		{
			if (getState(url) == FileState.CACHED)
			{
				CachedFile cf = fileCache.get(url);
				if (cf != null)
				{
					new File(cf.cachedFilename).delete();
					fileCache.remove(url);
					
					/* Notify the global updates */
					synchronized (cacheUpdateCallbackList)
					{
						for (FileCacheUpdateCallback fcuc : cacheUpdateCallbackList)
							fcuc.update(url);
					}
				}
			}
		}
	}

	/**
	 * Visitor interface for browsing all files in the cache.
	 * 
	 * @author sba
	 *
	 */
	public static interface IFileVisitor
	{
		public boolean visit(String filename, String url, String info, String downloadedAt);
	};

	public static void visitFiles(IFileVisitor visitor)
	{
		synchronized (downloadHashMap)
		{
			for (String name : fileCache.keySet())
			{
				CachedFile file = fileCache.get(name);
				visitor.visit(file.cachedFilename, file.url, getPathInfo(file.url), getDownloadTime(file.url));
			}
		}
	}
}
