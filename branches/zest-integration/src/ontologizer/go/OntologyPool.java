package ontologizer.go;

class Pool<T>
{
}

/**
 *
 *
 * @author Sebastian Bauer
 */
public class OntologyPool
{
	static private Pool<Prefix> prefixPool = new Pool();
	static private Pool<TermID> termidPool = new Pool();
}

