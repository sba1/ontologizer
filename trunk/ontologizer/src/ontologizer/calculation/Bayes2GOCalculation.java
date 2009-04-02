package ontologizer.calculation;

import java.util.HashSet;

import ontologizer.ByteString;
import ontologizer.GOTermEnumerator;
import ontologizer.PopulationSet;
import ontologizer.StudySet;
import ontologizer.association.AssociationContainer;
import ontologizer.go.GOGraph;
import ontologizer.go.Prefix;
import ontologizer.go.TermID;
import ontologizer.go.GOGraph.IVisitingGOVertex;
import ontologizer.statistics.AbstractTestCorrection;

/*
# terms 2 genes
t2g = matrix( 
		c(
			rep(c(1,1,1,1,0),10),
			rep(c(0,0,1,1,1),10)
			), 
		byrow= TRUE, nr=2
		)

ng = 5000 
nt = 100
t2g = matrix( sample(0:1,ng*nt, replace=TRUE), nr=nt)
 
# gene set
G = function(s){
	t(t2g)%*%s > 0
}

alpha = 0.1
beta = 0.2


##################
#simulate data
##################

# actual state
#s = c(1,0)
s =  rep(FALSE,nt)
s[ sample(nt,3) ] = TRUE

#compute hidden gene state

h = G(s)
o = h

rnd = runif( sum(h) )
changed = rnd <= beta

o[h][ changed ] = FALSE

rnd = runif( sum(!h) )
changed = rnd <= alpha

o[!h][ changed ] = TRUE

# get llr = log likelihood ratio for every gene

llr = vector('numeric', length=length(o))
llr[o]  = log(1-beta) -log(alpha)
llr[!o] = log(beta) -  log(1-alpha)


##################
# cost function
##################

p = 0.1

# s: state vector
# llr = log likelihood ratio for every gene
cost = function(s){
	- ( sum(llr[G(s)]) + sum(s)*log(p) )
} 

## transition
transition = function(s){
	i = sample(length(s),1)
	s[i] = !s[i]
	s
}

s0 = rep(FALSE, nt) 

res <- optim(s0, cost, transition, method="SANN",
                  control = list(maxit=6000, temp=2000, trace=TRUE))
res  # Near optimum distance around 12842
*/

public class Bayes2GOCalculation implements ICalculation {

	private double alpha = 0.1;
	private double beta = 0.2;
	private double p = 0.1;

	public EnrichedGOTermsResult calculateStudySet(GOGraph graph,
			AssociationContainer goAssociations, PopulationSet populationSet,
			StudySet studySet, AbstractTestCorrection testCorrection) {
		EnrichedGOTermsResult result = new EnrichedGOTermsResult(graph,goAssociations,studySet,populationSet.getGeneCount());

		/* Simulation */
		HashSet<TermID> activeTerms = new HashSet<TermID>(); /* Terms that are active */
		activeTerms.add(new TermID("GO:0006886"));
		activeTerms.add(new TermID("GO:0048102"));

		StudySet allGenes = new StudySet("all");
		for (ByteString gene : goAssociations.getAllAnnotatedGenes())
			allGenes.addGene(gene, "");

		StudySet newStudyGenes = new StudySet("study");
		GOTermEnumerator allEnumerator = allGenes.enumerateGOTerms(graph, goAssociations);
		for (TermID t : activeTerms)
		{
			for (ByteString g : allEnumerator.getAnnotatedGenes(t).totalAnnotated)
				newStudyGenes.addGene(g, "");
		}
		newStudyGenes.filterOutDuplicateGenes(goAssociations);
		/* Obfuscate the study set, i.e., create the observed state */
		
		
//		studySet.countGOTerms(graph, associationContainer)


		return result;
	}

	public String getDescription() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getName() {
		return "Bayes2GO";
	}

}
