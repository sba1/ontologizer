package ontologizer.gui.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;

public class LogWindow extends ApplicationWindow
{
	private StyledText logStyledText;

	public LogWindow(Display display)
	{
		super(display);
		
		shell.setText("Ontologizer - Log");
		shell.setLayout(new FillLayout());

		logStyledText = new StyledText(shell,SWT.BORDER|SWT.READ_ONLY|SWT.V_SCROLL|SWT.H_SCROLL);
		
		shell.addShellListener(new ShellAdapter()
		{
			@Override
			public void shellClosed(ShellEvent e)
			{
				e.doit = false;
				shell.setVisible(false);
			}
		});
	}
	

	public void dispose()
	{
		shell.dispose();
	}
	
	public void addToLog(String txt)
	{
		logStyledText.append(txt);
		
		int docLength = logStyledText.getCharCount();
		if (docLength > 0)
		{
			logStyledText.setCaretOffset(docLength);
			logStyledText.showSelection();
		}
	}
}
