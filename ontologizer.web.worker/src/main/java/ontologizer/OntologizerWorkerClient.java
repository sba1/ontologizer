package ontologizer;

import ontologizer.association.AssociationContainer;
import ontologizer.calculation.EnrichedGOTermsResult;
import ontologizer.calculation.TermForTermCalculation;
import ontologizer.ontology.Ontology;
import ontologizer.set.PopulationSet;
import ontologizer.set.StudySet;
import ontologizer.statistics.Bonferroni;
import ontologizer.types.ByteString;

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

		Worker.current().listenMessage(StartupMessage.class, (StartupMessage sm) ->
		{
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

		Worker.current().listenMessage(OntologizeMessage.class, (OntologizeMessage om) ->
		{
			TermForTermCalculation calculation = new TermForTermCalculation();
			PopulationSet population = new PopulationSet();
			population.addGenes(associations.getAllAnnotatedGenes());
			StudySet study = new StudySet();
			for (String s : om.getItems())
				study.addGene(new ByteString(s), "");
			EnrichedGOTermsResult result = calculation.calculateStudySet(ontology, associations, population, study, new Bonferroni());

			System.out.println(result.getSize() + " terms");
		});
	}
}
