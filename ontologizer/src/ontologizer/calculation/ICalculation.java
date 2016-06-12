package ontologizer.calculation;


import ontologizer.association.AssociationContainer;
import ontologizer.ontology.Ontology;
import ontologizer.set.PopulationSet;
import ontologizer.set.StudySet;
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
	 * Perform the enrichment calculation on the given study set.
	 *
	 * @param graph graph that defines the ontology
	 * @param associations the association to use
	 * @param populationSet the population set (contains all possible items)
	 * @param studySet the study set (contains the all "special" items)
	 * @param testCorrection the test correction that should be used.
	 * @return the result of the calculation
	 */
	public EnrichedGOTermsResult calculateStudySet(
			Ontology graph,
			AssociationContainer associations,
			PopulationSet populationSet,
			StudySet studySet,
			AbstractTestCorrection testCorrection);

	/**
	 * @return whether the calculation method supports multiple test correction.
	 */
	public boolean supportsTestCorrection();
}
