/*
 * Created on 13.06.2006
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ontologizer.gui.swt;

import ontologizer.BuildInfo;
import ontologizer.util.BareBonesBrowserLaunch;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;

/**
 * This class represents the help window of
 * the Ontologizer Application.
 *
 * @author Sebastian Bauer
 */
public class AboutWindow extends ApplicationWindow
{
	final static String aboutText = "Ontologizer\n\nVersion %s\nCopyright (c) Ontologizer Development Team 2005-2009.\nAll rights reserved.";
	final static String visitText = "Visit %s for more information.";

	final static String verisonText = "2.1";
	final static String homepageText = "http://compbio.charite.de/ontologizer/";

	public AboutWindow(Display display)
	{
		super(display);

		shell.setText("Ontologizer - About");
		shell.setLayout(new GridLayout());

		/* Prevent the disposal of the window on a close event,
		 * but make the window invisible */
		shell.addShellListener(new ShellAdapter(){
			public void shellClosed(ShellEvent e)
			{
				e.doit = false;
				shell.setVisible(false);
			}
		});

		/* Styled text */
		final StyledText text = new StyledText(shell,SWT.BORDER|SWT.READ_ONLY);
		text.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL|GridData.GRAB_VERTICAL|GridData.HORIZONTAL_ALIGN_FILL|GridData.VERTICAL_ALIGN_FILL));
		text.setEditable(false);
		text.setText(String.format(aboutText,verisonText));
		text.append("\n");
		text.append("\n");
		text.append("Build: " + BuildInfo.date + "-" + BuildInfo.revisionNumber);
		text.append("\n");
		text.append("\n");
		text.append(String.format(visitText,homepageText));

		String txt = text.getText();
		final int homepageLinkStart = txt.indexOf(homepageText);
		text.setStyleRange(new StyleRange(homepageLinkStart,homepageText.length(),display.getSystemColor(SWT.COLOR_BLUE),null));
		text.addMouseListener(new MouseAdapter(){
			public void mouseDown(MouseEvent e)
			{
				try
				{
					int offset = text.getOffsetAtLocation(new Point(e.x,e.y));
					if (offset >= homepageLinkStart && offset < homepageLinkStart + homepageText.length())
						BareBonesBrowserLaunch.openURL(homepageText);
				} catch(IllegalArgumentException e1)
				{
				}
			}
		});

		Button okButton = new Button(shell, 0);
		okButton.setText("Ok");
		okButton.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL|GridData.HORIZONTAL_ALIGN_END));
		okButton.addSelectionListener(new SelectionAdapter()
		{
			public void widgetSelected(SelectionEvent e)
			{
				shell.setVisible(false);
			}
		});
		shell.pack();


	}
}
