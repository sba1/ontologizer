package ontologizer.calculation;

import ontologizer.ByteString;
import ontologizer.association.AssociationContainer;
import ontologizer.go.GOGraph;

public class SemanticResult
{
	public GOGraph g;
	public AssociationContainer assoc;

	public ByteString [] names;
	public double [][] mat;
	public String name;
}
