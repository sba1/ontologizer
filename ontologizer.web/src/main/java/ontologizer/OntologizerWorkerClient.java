package ontologizer;

import ontologizer.association.AssociationContainer;
import ontologizer.ontology.Ontology;

/**
 * Main class of the Ontologizer Web Worker.
 *
 * @author Sebastian Bauer
 */
public class OntologizerWorkerClient
{
	public static Ontology ontology;
	public static AssociationContainer associations;

	public static void main(String[] args)
	{
		Worker.current().listenMessage((evt)->{
			System.out.println("Message from main: " + evt.getDataAsString());

			DatafilesLoader loader = new DatafilesLoader();
			loader.load( () -> {
				ontology = loader.getOntology();
				associations = loader.getAnnotation();
				System.out.println(ontology.getNumberOfTerms());
			});
		});
	}
}
