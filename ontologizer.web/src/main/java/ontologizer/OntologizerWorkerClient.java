package ontologizer;

/**
 * Main class of the Ontologizer Web Worker.
 *
 * @author Sebastian Bauer
 */
public class OntologizerWorkerClient
{
	public static void main(String[] args)
	{
		Worker.current().listenMessage((evt)->{
			System.out.println("Message from main: " + evt.getDataAsString());
		});
	}
}
