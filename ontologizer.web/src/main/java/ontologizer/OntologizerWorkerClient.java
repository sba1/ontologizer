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

				ProgressMessage msg = WorkerMessage.createWorkerMessage(ProgressMessage.class);
				Worker.current().postMessage(msg);
			},
			(int current, int max, int terms) ->
			{
				ProgressMessage msg = WorkerMessage.createWorkerMessage(ProgressMessage.class);
				msg.setCurrent(current);
				msg.setMax(max);
				Worker.current().postMessage(msg);
			},
			(int current, int max) ->
			{
				ProgressMessage msg = WorkerMessage.createWorkerMessage(ProgressMessage.class);
				msg.setCurrent(current);
				msg.setMax(max);
				Worker.current().postMessage(msg);
			});
		});
	}
}
