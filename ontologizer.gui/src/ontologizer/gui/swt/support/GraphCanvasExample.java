package ontologizer.gui.swt.support;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public class GraphCanvasExample
{
	private static String g = "digraph g {\n" +
	"test [pos=\"37,90\", width=\"0.83333\", height=\"0.5\"];\n" +
	"test2 [pos=\"37,18\", width=\"1.0278\", height=\"0.5\"];ņ" +
	"test -> test2 [pos=\"e,37,36.413 37,71.831 37,64.131 37,54.974 37,46.417\"];\n" +
	"}\n";

	public static void main(String[] args) throws Exception
	{
		final Display display = new Display();
		Shell shell = new Shell(display);
		shell.setLayout(new FillLayout());

		File f = File.createTempFile("onto","dot");
		BufferedWriter br = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f)));
		br.append(g);
		br.close();

		System.out.println(g);

		GraphCanvas g = new GraphCanvas(shell, 0);
		g.setLayoutedDotFile(f);

		shell.open();

		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		display.dispose();

	}
}
