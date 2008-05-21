package ontologizer.calculation;


import ontologizer.PopulationSet;
import ontologizer.StudySet;
import ontologizer.association.AssociationContainer;
import ontologizer.go.GOGraph;
import ontologizer.statistics.AbstractTestCorrection;

/**
 * 
 * @author Sebastian Baueer
 *
 */
public interface ICalculation
{
	/**
	 * Returns the name of the calculation method
	 * 
	 * @return the name of the method.
	 */
	public String getName();
	
	/**
	 * Returns a short description of the calculation method.
	 * 
	 * @return the description.
	 */
	public String getDescription();

	/**
	 * Allows performing calculations on the single StudySet level.
	 * This makes more sense than acting on whole directories.
	 * 
	 * @param graph
	 * @param goAssociations
	 * @param populationSet
	 * @param studySet
	 * @param testCorrection
	 * @return the result of the calculation
	 */
	public EnrichedGOTermsResult calculateStudySet(
			GOGraph graph,
			AssociationContainer goAssociations,
			PopulationSet populationSet,
			StudySet studySet,
			AbstractTestCorrection testCorrection);
}
