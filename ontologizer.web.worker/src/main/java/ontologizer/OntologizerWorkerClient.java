package ontologizer;

import static ontologizer.ProgressMessage.createProgressMessage;
import static ontologizer.WorkerMessage.createWorkerMessage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import org.teavm.jso.core.JSArray;
import org.teavm.jso.core.JSNumber;
import org.teavm.jso.core.JSString;

import ontologizer.association.AssociationContainer;
import ontologizer.calculation.AbstractGOTermProperties;
import ontologizer.calculation.EnrichedGOTermsResult;
import ontologizer.calculation.ICalculation;
import ontologizer.calculation.ICalculationProgress;
import ontologizer.calculation.IProgressFeedback;
import ontologizer.calculation.TermForTermCalculation;
import ontologizer.calculation.b2g.Bayes2GOCalculation;
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

	public static ICalculation [] supportedCalculations;

	public static void main(String[] args)
	{
		supportedCalculations = new ICalculation[2];
		supportedCalculations[0] = new TermForTermCalculation();
		supportedCalculations[1] = new Bayes2GOCalculation();

		Worker.current().listenMessage(LoadDataMessage.class, ldm ->
		{
			loader = new DatafilesLoader(ldm.getAssociationFilename());

			loader.load( () ->
			{
				ontology = loader.getOntology();
				associations = loader.getAnnotation();

				createWorkerMessage(HideProgressMessage.class).post(Worker.current());
			},
			/* Download Progress */
			(int current, int max, String name) ->
				createProgressMessage().withTitle("Downloading " + name).withCurrent(current).withMax(max).post(Worker.current())
			,
			/* OBO Progress */
			(int current, int max, int terms) ->
				createProgressMessage().withTitle("Parsing Ontology").withCurrent(current).withMax(max).post(Worker.current())
			,
			/* Association Progress */
			(int current, int max) ->
				createProgressMessage().withTitle("Parsing Annotations").withCurrent(current).withMax(max).post(Worker.current())
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
			if (om.getCalculationType() >= 0 && om.getCalculationType() < supportedCalculations.length)
			{
				/* Type is unknown */
				return;
			}

			ICalculation calculation = supportedCalculations[om.getCalculationType()];
			PopulationSet population = new PopulationSet();
			population.addGenes(associations.getAllAnnotatedGenes());
			StudySet study = new StudySet();
			for (String s : om.getItems())
				study.addGene(new ByteString(s), "");

			final int [] maxP = new int[1];

			if (calculation instanceof IProgressFeedback)
			{
				IProgressFeedback progressFeedback = (IProgressFeedback)calculation;
				progressFeedback.setProgress(new ICalculationProgress()
				{
					private long lastNano = 0;

					@Override
					public void update(int current)
					{
						long newNano = System.nanoTime();
						if (newNano - lastNano > 250*1000*100)
						{
							createProgressMessage().withTitle("Ontologizing").withCurrent(current).withMax(maxP[0]).post(Worker.current());

							lastNano = newNano;
						}
					}

					@Override
					public void init(int max)
					{
						createProgressMessage().withTitle("Ontologizing").withCurrent(0).withMax(max).post(Worker.current());
						maxP[0] = max;

						lastNano = System.nanoTime();
					}
				});
			}

			result = calculation.calculateStudySet(ontology, associations, population, study, new Bonferroni());

			props = new AbstractGOTermProperties[result.getSize()];
			int i = 0;
			for (AbstractGOTermProperties p : result)
				props[i++] = p;
			Arrays.sort(props, Comparator.comparingDouble(p -> p.p));

			createProgressMessage().withTitle("Ontologizing").withCurrent(3).withMax(3).post(Worker.current());

			Worker.current().postSimpleMessage(HideProgressMessage.class);
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
			/* FIXME: Avoid te useless conversions */
			re.setName(prop.goTerm.getName().toString());
			re.setID(prop.goTerm.getIDAsString());
			re.setAdjP(prop.p_adjusted);
			return re;
		});

		Worker.current().listenMessage2(AutoCompleteMessage.class, acm -> {
			AutoCompleteResults acr = Utils.createObject();
			ArrayList<String> resultList = new ArrayList<>();

			if (associations != null)
			{
				String prefix = acm.getPrefix();
				for (ByteString item : associations.getAllAnnotatedGenes())
				{
					if (item.startsWithIgnoreCase(prefix))
					{
						resultList.add(item.toString());
					}
				}
			}
			JSArray<JSString> results = JSArray.create(resultList.size());
			if (resultList.size() > 0)
			{
				for (int i=0; i < resultList.size(); i++)
				{
					results.set(i, JSString.valueOf(resultList.get(i)));
				}
				results.sort();
			}

			acr.setResults(results);
			return acr;
		});
	}
}
