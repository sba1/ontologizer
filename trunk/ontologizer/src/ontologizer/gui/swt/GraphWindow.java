package ontologizer.gui.swt;

import java.io.File;
import java.util.Locale;
import java.util.Set;

import ontologizer.GlobalPreferences;
import ontologizer.IDotNodeAttributesProvider;
import ontologizer.go.GOGraph;
import ontologizer.go.Namespace;
import ontologizer.go.Term;
import ontologizer.go.TermID;
import ontologizer.gui.swt.result.GraphGenerationThread;
import ontologizer.gui.swt.support.GraphCanvas;
import ontologizer.gui.swt.support.IGraphGenerationFinished;
import ontologizer.util.Util;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;

class GraphWindow extends ApplicationWindow
{
	private GraphCanvas graphCanvas;
	
	public GraphWindow(Display display)
	{
		super(display);

		/* Prevent the disposal of the window on a close event,
		 * but make the window invisible */
		shell.addShellListener(new ShellAdapter(){
			public void shellClosed(ShellEvent e)
			{
				e.doit = false;
				shell.setVisible(false);
			}
		});

		shell.getShell().setLayout(new FillLayout());
		shell.setText("Ontologizer - Graph");
		graphCanvas = new GraphCanvas(shell,0);
		
		shell.pack();
		Rectangle rect = shell.getBounds();
		if (rect.width < 400) rect.width = 400;
		if (rect.height < 300) rect.height = 300;
		shell.setBounds(rect);
	}

	public void setVisibleTerms(final GOGraph graph, final Set<TermID> terms)
	{
		GraphGenerationThread ggt = new GraphGenerationThread(shell.getDisplay(),GlobalPreferences.getDOTPath(),new IGraphGenerationFinished()
		{
			public void finished(boolean success, String msg, File pngFile, File dotFile)
			{
				if (success)
				{
					try
					{
						graphCanvas.setLayoutedDotFile(dotFile);
					} catch(Exception e)
					{
						e.printStackTrace();
					}
				} else
				{
					MessageBox mbox = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
					mbox.setMessage("Unable to execute the 'dot' tool!\nPlease check the preferences, and ensure that GraphViz (available from http://www.graphviz.org/) is installed properly\n\n" + msg);
					mbox.setText("Ontologizer - Error");
					mbox.open();
				}
	
			}
		}, new IDotNodeAttributesProvider()
		{

			public String getDotNodeAttributes(TermID id)
			{
				StringBuilder builder = new StringBuilder();
				
				Term t = graph.getGOTerm(id);
				if (t == null) return "";
				
				builder.append("label=\"");
				String label = t.getName();
				if (GlobalPreferences.getWrapColumn() != -1)
					label = Util.wrapLine(label,"\\n",GlobalPreferences.getWrapColumn());
				builder.append(label);
				builder.append("\"");

				if (terms.contains(id))
				{
					float hue;
					float saturation = 1.f;

					switch (Namespace.getNamespaceEnum(t.getNamespace()))
					{
						case BIOLOGICAL_PROCESS: hue = 120.f / 360; break;
						case MOLECULAR_FUNCTION: hue = 60.f / 360; break;
						case CELLULAR_COMPONENT: hue = 300.f / 360; 	break;
						default:
							hue = 0.f;
							saturation = 0.f;
							break;

					}
					
					String style = "filled,gradientfill";
					String fillcolor = String.format(Locale.US, "%f,%f,%f", hue, saturation, 1.0f);
					builder.append(",style=\""+ style + "\",color=\"white\",fillcolor=\"" + fillcolor + "\"");
				}
				
				return builder.toString();
			}
		});
		ggt.go = graph;
		ggt.emanatingTerm = null;
		ggt.leafTerms.addAll(terms);
		ggt.start();
	}
}


