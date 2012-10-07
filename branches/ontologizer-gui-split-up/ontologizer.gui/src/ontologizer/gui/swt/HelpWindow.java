/*
 * Created on 13.06.2006
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ontologizer.gui.swt;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ontologizer.gui.swt.support.SWTUtil;
import ontologizer.util.BareBonesBrowserLaunch;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationAdapter;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

/**
 * This class represents the help window of 
 * the Ontologizer Application.
 * 
 * @author Sebastian Bauer
 */
public class HelpWindow extends ApplicationWindow
{
	private Browser browser;
	private StyledText styledText;
	private List tocList;

	private String [] urls;
	private String [] titles;
	private String [] filenames;

	private String folder;

	private static String NOBROWSER_TOOLTIP = "The SWT browser widget could not " +
		"be instantiated. Please ensure that your system fulfills the requirements " +
		"of the SWT browser. Further information can be obtained from the FAQ at " +
		"http://www.eclipse.org/swt.";

	/**
	 * Parse the title of the html file.
	 * 
	 * @param file
	 * @return
	 */
	private String parseHTMLTitle(File file)
	{
		try
		{
			BufferedReader bfr = new BufferedReader(new FileReader(file));
			StringBuilder title = new StringBuilder();

			String line;
			boolean insideTitle = false;
			boolean leave = false;

			while (((line = bfr.readLine()) != null) && !leave)
			{
				int titleStart = -1;
				int titleEnd = -1;
				if (!insideTitle)
				{
					titleStart = line.indexOf("<title>");
					if (titleStart != -1)
					{
						titleStart += 7;
						insideTitle = true;
					}
				}

				if (insideTitle)
				{
					titleEnd = line.indexOf("</title>");
					if (titleEnd != -1)
					{
						insideTitle = false;
						leave = true;
					}
				}

				if (titleStart != -1 || titleEnd != -1)
				{
					if (titleStart == -1) titleStart = 0;
					if (titleEnd == -1) titleEnd = line.length();
					title.append(line.substring(titleStart,titleEnd));
				} else
				{
					if (insideTitle)
						title.append(line);
				}
			}

			bfr.close();
			return title.toString().trim();
		} catch (FileNotFoundException e)
		{
		} catch (IOException e)
		{
		}

		return "Unknown title";
	}

	/**
	 * Populates the table of contents.
	 */
	private void readToc()
	{
		File file = new File(folder);
		File[] files = file.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.endsWith(".html") || name.endsWith(".htm");
			}
		});
		if (files != null)
		{
			Arrays.sort(files,new Comparator<File>(){
				public int compare(File o1, File o2)
				{
					return o1.getName().compareToIgnoreCase(o2.getName());   
				}
			});

			urls = new String[files.length];
			titles = new String[files.length];
			filenames = new String[files.length];

			for (int i=0;i<files.length;i++)
			{
				try
				{
					titles[i] = parseHTMLTitle(files[i]);
					filenames[i] = files[i].getCanonicalPath();
					urls[i] = files[i].toURI().toURL().toString();
				}
				catch (MalformedURLException e) { }
				catch (IOException e2) {}
			}
		} else
		{
			urls = new String[0];
			titles = new String[0];
		}
	}

	/**
	 * Constructor.
	 * 
	 * @param display
	 */
	public HelpWindow(Display display, String helpFolder)
	{
		super(display);

		folder = helpFolder;
		readToc();

		shell.setText("Ontologizer - Help");
		shell.setLayout(new FillLayout());
		
		/* Prevent the disposal of the window on a close event,
		 * but make the window invisible */
		shell.addShellListener(new ShellAdapter(){
			public void shellClosed(ShellEvent e)
			{
				e.doit = false;
				shell.setVisible(false);
			}
		});

		Composite composite = new Composite(shell,0);
		composite.setLayout(new GridLayout(1,false));

		final ToolBar navBar = new ToolBar(composite, 0);
		GridData navBarGridData = new GridData();
		navBarGridData.grabExcessHorizontalSpace = true;
		navBarGridData.horizontalAlignment = SWT.END;
		navBarGridData.horizontalSpan = 2;
		navBar.setLayoutData(navBarGridData);

		final ToolItem back = new ToolItem(navBar, SWT.PUSH);
		back.setText("Back");
		back.setEnabled(false);
		final ToolItem forward = new ToolItem(navBar, SWT.PUSH);
		forward.setText("Forward");
		forward.setEnabled(false);

		back.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e)
			{
				if (browser != null)
					browser.back();
			}
	      });
		forward.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				if (browser != null)
					browser.forward();
			};
	      });

		SashForm sash = new SashForm(composite, SWT.HORIZONTAL);
		sash.setLayoutData(new GridData(GridData.FILL_BOTH));

		tocList = new List(sash, SWT.BORDER|SWT.SINGLE);
		tocList.addSelectionListener(new SelectionAdapter()
		{
			public void widgetSelected(SelectionEvent e)
			{
				int index = tocList.getSelectionIndex();

				if (browser != null)
				{
					browser.setUrl(urls[index]);
				} else
				{
					/* Load the file manually and display the html stripped contents
					 * well, not a real HTML parser at all, but it fulfills the needs */
					try
					{
						BufferedReader fr = new BufferedReader(new FileReader(filenames[index]));
						String line;
						StringBuilder buffer = new StringBuilder();

						Pattern liPattern = Pattern.compile(".*<li>(.*)</li>.*", Pattern.CASE_INSENSITIVE);

						while ((line = fr.readLine()) != null)
						{
							line = line.replaceAll("<H1>.*</H1>","<p>");
							line = line.replaceAll("<h1>.*</h1>","<p>");
							Matcher m = liPattern.matcher(line);
							if (m.matches())
								line = m.replaceFirst(" - " + m.group(1) + "<p>");
							
							line = line.replaceAll("<[^Pp].+?>","").trim();
							line = line.replaceAll("<[Pp]>","\n");
							line = line.replaceAll("</[Pp]>","\n");
							
							if (line.length() != 0)
							{
								buffer.append(line);
								buffer.append(" ");
							}
						}
						styledText.setText(buffer.toString());
					}
					catch (FileNotFoundException e1) { }
					catch (IOException e2) { }
				}
			}
		});

		for (int i=0;i<titles.length;i++)
		{
			tocList.add(titles[i]);
		}

		/* Use an auxiliary composite for the browser, because
		 * the browser actually is instanciated even if it fails
		 * (issue was reported and will be fixed in post Eclipse 3.2) */
		final Composite browserComposite = new Composite(sash,0);
		browserComposite.setLayout(new FillLayout());
		browserComposite.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL|GridData.GRAB_VERTICAL|GridData.FILL_BOTH));
		try {
			browser = new Browser(browserComposite, SWT.BORDER);

			browser.addLocationListener(new LocationAdapter()
			{
				public void changing(LocationEvent event)
				{
					if (event.location.startsWith("http://"))
					{
						BareBonesBrowserLaunch.openURL(event.location);
						event.doit = false;
					}
				}

				public void changed(LocationEvent event)
				{
		            back.setEnabled(browser.isBackEnabled());
		            forward.setEnabled(browser.isForwardEnabled());
				}
			});
		} catch (SWTError e) {
			browserComposite.dispose();
			browser = null;
			
			/* Create the fall back environment */
			Composite styledTextComposite = new Composite(sash,0);
			styledTextComposite.setLayout(SWTUtil.newEmptyMarginGridLayout(1));

			Label label = new Label(styledTextComposite,0);
			label.setText("No browser available! Contents has been stripped!");

			String error = e.getLocalizedMessage();
			if (error != null)
				label.setToolTipText(NOBROWSER_TOOLTIP + "\n\nReason for failing: " + error);
			else
				label.setToolTipText(NOBROWSER_TOOLTIP);

			styledText = new StyledText(styledTextComposite,SWT.BORDER|SWT.READ_ONLY|SWT.WRAP|SWT.V_SCROLL);
			styledText.setEditable(false);
			styledText.setLayoutData(new GridData(GridData.FILL_BOTH|GridData.GRAB_HORIZONTAL|GridData.GRAB_VERTICAL));
	      }

		sash.setWeights(new int[]{1,3});
	}
}
