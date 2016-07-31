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
	public static DatafilesLoader loader;
	public static Ontology ontology;
	public static AssociationContainer associations;

	public static void main(String[] args)
	{
		loader = new DatafilesLoader();

		Worker.current().listenMessage((evt)->{
			loader.load( () ->
			{
				ontology = loader.getOntology();
				associations = loader.getAnnotation();

				Worker.current().postMessage(WorkerMessage.createWorkerMessage(ProgressMessage.class));
			},
			(int current, int max, int terms) ->
			{
				System.out.println(current + " / " + max);
			},
			(int current, int max) ->
			{
				System.out.println(current + " / " + max);
			});
		});
	}
}
