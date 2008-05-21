/*
 * Created on 04.08.2005
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ontologizer.calculation;

import ontologizer.GOTermCounter;
import ontologizer.PopulationSet;
import ontologizer.StudySet;
import ontologizer.association.AssociationContainer;
import ontologizer.go.GOGraph;
import ontologizer.statistics.AbstractTestCorrection;

public class DirectFDRCalculation extends AbstractHypergeometricCalculation
{

	public String getName()
	{
		return "Direct FDR";
	}

	public String getDescription()
	{
		return "Nothing yet";
	}

	@SuppressWarnings("unused")
	public EnrichedGOTermsResult calculateStudySet(
			GOGraph graph,
			AssociationContainer goAssociations,
			PopulationSet populationSet,
			StudySet studySet, AbstractTestCorrection testCorrection)
	{
		GOTermCounter populationTermCounter = populationSet.countGOTerms(graph.getGoTermContainer(),goAssociations);
		GOTermCounter studyTermCounter = studySet.countGOTerms(graph.getGoTermContainer(),goAssociations);
		EnrichedGOTermsResult studySetResult = new EnrichedGOTermsResult(graph, goAssociations, studySet, populationSet.getGeneCount());



		return studySetResult;
	}

}
