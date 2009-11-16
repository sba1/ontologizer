package ontologizer.gui.swt.result;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;

import ontologizer.calculation.AbstractGOTermProperties;
import ontologizer.calculation.EnrichedGOTermsResult;
import ontologizer.go.TermID;
import ontologizer.gui.swt.support.GraphPaint;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.widgets.Display;

import att.grappa.Element;
import att.grappa.Graph;
import att.grappa.GraphEnumeration;
import att.grappa.GrappaConstants;
import att.grappa.GrappaPoint;
import att.grappa.Node;
import att.grappa.Parser;
import att.grappa.Subgraph;

public class EnrichedGOTermsResultLatexWriter
{
	/**
	 * @param htmlFile
	 * @param dotFile 
	 * @param pngFile 
	 */
	public static void write(EnrichedGOTermsResult result, File texFile) 
	{
		try
		{
			PrintWriter out = new PrintWriter(texFile);

			out.println("\\documentclass{article}");
			out.println("\\title{Ontologizer GO Term Overrepresentation Analysis}");
			out.println("\\begin{document}");
			out.println("\\maketitle");
			out.println("\\begin{table}[th]");
			out.println("\\begin{center}");
			out.println("\\begin{footnotesize}");
			out.println("\\begin{tabular}{llllll}");
			out.println("\\hline\\\\[-2ex]");
			out.println("ID & Name & p-Value & p-Value (Adj) & Study Count & Population Count \\\\");
			out.println("\\hline\\\\[-2ex]");

			AbstractGOTermProperties [] sortedProps = new AbstractGOTermProperties[result.getSize()];
			int i=0;
			for (AbstractGOTermProperties prop : result)
				sortedProps[i++] = prop;
			Arrays.sort(sortedProps);

			for (AbstractGOTermProperties props : sortedProps)
			{
				out.print (props.goTerm.getIDAsString());
				out.print (" & ");
				
				String name = props.goTerm.getName();
				name = name.replaceAll("_", " ");
				out.print(name);
				
				out.print(" & ");				
				out.printf("%.4f", props.p);
				out.println(" & ");
				out.printf("%.4f", props.p_adjusted);
				out.println(" & ");
				out.println(props.annotatedStudyGenes);
				out.println(" & ");
				out.println(props.annotatedPopulationGenes);
				out.println(" \\\\");
		
			}
			out.println("\\hline\\\\[-2ex]");
			
			out.println("\\end{tabular}");
			out.println("\\end{footnotesize}");
			out.println("\\end{center}");
			out.println("\\caption{GO overrepresentation analysis with the Ontologizer V2, Bauer et al., Bioinformatics 24(14):1650-1.}");
			out.println("\\end{table}");

			out.println("\\end{document}");

			out.flush();
			out.close();

		} catch (IOException e)
		{
			e.printStackTrace();
		} catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}

