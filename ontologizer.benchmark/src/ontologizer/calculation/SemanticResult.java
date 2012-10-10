package ontologizer.calculation;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Logger;

import ontologizer.association.AssociationContainer;
import ontologizer.go.Ontology;
import ontologizer.types.ByteString;

public class SemanticResult
{
	private static Logger logger = Logger.getLogger(EnrichedGOTermsResult.class.getCanonicalName());

	public Ontology g;
	public AssociationContainer assoc;

	public ByteString [] names;
	public double [][] mat;
	public String name;

	public SemanticCalculation calculation;

	public void writeTable(File file)
	{
		try
		{
			logger.info("Writing to \"" + file.getCanonicalPath() + "\".");

			PrintWriter out = new PrintWriter(file);

			if (names != null && names.length>0)
			{
				out.print(names[0]);
				for (int i=1;i<names.length;i++)
				{
					out.print("\t");
					out.print(names[i]);
				}
				
				out.println();
				
				for (int i=0;i<names.length;i++)
				{
					out.print(names[i]);
					
					for (int j=0;j<names.length;j++)
					{
						out.print("\t");
						out.print(mat[i][j]);
					}

					out.println();
				}
			}
			
			out.close();
		} catch (IOException e)
		{
		}

	}
}
