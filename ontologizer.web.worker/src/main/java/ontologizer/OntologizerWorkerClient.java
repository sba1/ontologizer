package ontologizer;

import static ontologizer.ProgressMessage.createProgressMessage;
import static ontologizer.WorkerMessage.createWorkerMessage;

import java.util.Arrays;
import java.util.Comparator;

import org.teavm.jso.core.JSNumber;

import ontologizer.association.AssociationContainer;
import ontologizer.calculation.AbstractGOTermProperties;
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
	public static EnrichedGOTermsResult result;
	public static AbstractGOTermProperties [] props;

	public static void main(String[] args)
	{
		loader = new DatafilesLoader();

		Worker.current().listenMessage(LoadDataMessage.class, (LoadDataMessage sm) ->
		{
			loader.load( () ->
			{
				ontology = loader.getOntology();
				associations = loader.getAnnotation();

				createProgressMessage().post(Worker.current());
			},
			/* OBO Progress */
			(int current, int max, int terms) ->
				createProgressMessage().withCurrent(current).withMax(max).post(Worker.current())
			,
			/* Association Progress */
			(int current, int max) ->
				createProgressMessage().withCurrent(current).withMax(max).post(Worker.current())
			);
		});

		Worker.current().listenMessage(GetAllGenesMessage.class, (GetAllGenesMessage gm) ->
		{
			if (ontology == null || associations == null)
				return;

			StringBuilder allGenes = new StringBuilder();
			for (ByteString gene : associations.getAllAnnotatedGenes())
			{
				allGenes.append(gene.toString());
				allGenes.append("\n");
			}
			AllGenesMessage am = createWorkerMessage(AllGenesMessage.class);
			am.setItems(allGenes.toString());
			Worker.current().postMessage(am);
		});

		Worker.current().listenMessage(OntologizeMessage.class, (OntologizeMessage om) ->
		{
			TermForTermCalculation calculation = new TermForTermCalculation();
			PopulationSet population = new PopulationSet();
			population.addGenes(associations.getAllAnnotatedGenes());
			StudySet study = new StudySet();
			for (String s : om.getItems())
				study.addGene(new ByteString(s), "");
			result = calculation.calculateStudySet(ontology, associations, population, study, new Bonferroni());

			props = new AbstractGOTermProperties[result.getSize()];
			int i = 0;
			for (AbstractGOTermProperties p : result)
				props[i++] = p;
			Arrays.sort(props, Comparator.comparingDouble(p -> p.p));

			Worker.current().postSimpleMessage(OntologizeDoneMessage.class);
		});

		/* Messages that expect replies */

		Worker.current().listenMessage2(GetNumberOfResultsMessage.class, (GetNumberOfResultsMessage gm) ->
		{
			return JSNumber.valueOf(result==null?0:result.getSize());
		});

		Worker.current().listenMessage2(GetResultMessage.class, gm ->
		{
			ResultEntry re = ResultEntry.createResultEntry();
			AbstractGOTermProperties prop = props[gm.getRank()];
			re.setName(prop.goTerm.getName());
			re.setID(prop.goTerm.getIDAsString());
			re.setAdjP(prop.p_adjusted);
			return re;
		});
	}
}
