package ontologizer.calculation.svd;

import ontologizer.association.AssociationContainer;
import ontologizer.calculation.AbstractGOTermsResult;
import ontologizer.go.Ontology;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.colt.matrix.linalg.SingularValueDecomposition;


/** 
 * This class encapsulates the results of an SVD calculation
 * and is intended to be used to help display results in the GUI.
 * @author peter
 *
 */
public class SVDResult extends AbstractGOTermsResult
{
	private SingularValueDecomposition svd;
	
	/** A Matrix with the normalized, centered annotation counts */
	private DoubleMatrix2D ddm;
	
	private String [] colNames;

	/** Number of arrays/sets */
	private int L;

	/**
	 * The Shannon entropy of the dataset. The value -1.0 is a
	 * flag that the calculation has not yet been performed.
	 */
	private double d = -1.0; 

	/**
	 * The P_k values corresponding to the eigenvalues, i.e.,
	 * the variance explained by each eigenvector.
	 */

	private double [] p_k = null;
	/** The cumulative sum of the variances as stored in p_k */
	private double [] cum_sum_of_variances = null;
	
	private boolean pValues;
	/**
	 * The constructor expects to receive a
	 * SingularValueDecomposition object resulting from the
	 * analysis of GO counts. It performs calculations needed to 
	 * present a Scree plot. 
	 * 
	 * @param go The DAG representing the GO Graph
	 * @param associations A container of all associations to a dataset
	 * @param svd An object representing the SVD analysis of the GO counts
	 * @param ddm A matrix containing the normalized and centered count data used to perform the SVD
	 * @param colNames the names of the columns
	 */
	public SVDResult(Ontology go, AssociationContainer associations, SingularValueDecomposition svd, DoubleMatrix2D ddm, String [] colNames, boolean pValues)
	{
		super(go,associations);

		this.pValues = pValues;
		this.svd = svd;
		this.ddm = ddm;
		this.L = svd.getSingularValues().length;
		this.colNames = colNames;

		calculate_p_k();
		calculate_Shannon_Entropy();
	}
	
	/** Return the left singular vectors */
	public DoubleMatrix2D getU()
	{
		return svd.getU();
	}

	/** Return the right singular vectors */
	public DoubleMatrix2D getV()
	{
		return svd.getV();
	}

	/** Return the singular values */
	public double[] getSigma()
	{
		return svd.getSingularValues();
	}
	
	public int getRank()
	{
		return svd.rank();
	}
	
	public int getSize()
	{
		return L;
	}
	
	/**
	 * Calculate variances using the squares of the 
	 * singular values. This initiates the arrays of values
	 * for p_k (cf. equation 2 in Alter et al., 2000).
	 * Also calculate the cumulative sum of variances for the
	 * Scree plot.
	 *
	 */
	private void calculate_p_k()
	{
		int i;
		double sv[];
		sv = svd.getSingularValues();
		this.p_k = new double[L];
		this.cum_sum_of_variances = new double[L];

		for (i=0;i<L;++i)
		{
			p_k[i] = sv[i]*sv[i];
		}
		cum_sum_of_variances[0] = p_k[0];

		for (i=1;i<L;++i)
		{
			cum_sum_of_variances[i] = p_k[i] + cum_sum_of_variances[i-1];
		}
	}
	
	public double get_Shannon_Entropy()
	{
		if (d < 0) calculate_Shannon_Entropy();
		return d;
	}
	
	private void calculate_Shannon_Entropy()
	{
		int i;
		double log2 = Math.log(2.0);
		double EPSILON = 0.000001;
		if (p_k == null) calculate_p_k();
		d = 0.0;
		for (i=0;i<L;++i){
			double v;
			if (Math.abs(p_k[i]) < EPSILON ) v = 0;
			else {
				v = p_k[i] * Math.log(p_k[i])/ log2; 
			}
			d += v;
		}
		d *= (-1*log2)/Math.log(L);
	}
	
	/**
	 * Get a matrix with the correlations of the original
	 * data matrix columns with the left-singular vectors
	 * at columns axis1 and axis2. 
	 * These values can be used to plot the first two principle 
	 * components by setting axis1=0, axis2=1.
	 * @param axis1 index of first left singular vector
	 * @param axis2 index of second left singular vector
	 * @param data Data matrix, with which correlations will be calculated. Usually, we will be
	 * interested in correlations with the first and second principle components.
	 * @return an Mx2 matrix with the correlations.
	 * TODO check indices, throw Exception etc.
	 */
	public DoubleMatrix2D getCorrelation(int axis1, int axis2)
	{
		DoubleMatrix2D data = ddm;
		DoubleMatrix2D correlation = null;
		int nrow,ncol,i,j;
		double sum_of_squares;
		
		nrow = svd.getU().rows(); /* Same as n rows (terms) in data*/
		ncol = data.columns();    /* Should be same as data except if rank < groups */
		
		correlation = new DenseDoubleMatrix2D(ncol,2);

		if (axis1 >= ncol || axis2 >= ncol)
			return correlation;

		/* First calculate inner product of arrays (list of annotation counts) with one another */
		double iproduct[] = new double[ncol];
		for (j=0;j<ncol;j++) {
			sum_of_squares = 0.0;
			for (i=0;i<nrow;++i)
				sum_of_squares += data.get(i,j)*data.get(i,j);
			iproduct[j] = sum_of_squares;
			
		}
		
		/* TODO need to check for zero values in iproduct[], which should
		 * never happen.
		 */
		DoubleMatrix2D u = svd.getU();

		/* Calculate correlation for first eigenvector */
		sum_of_squares = 0.0;
		for (j=0;j<ncol;++j) {
			for (i=0;i<nrow;++i) {
				sum_of_squares += u.get(i,axis1) * data.getQuick(i, j);
			}
			correlation.set(j, 0, sum_of_squares/iproduct[j]);
		}
		
		sum_of_squares = 0.0;
		for (j=0;j<ncol;++j) {
			for (i=0;i<nrow;++i) {
				sum_of_squares += u.get(i,axis2) * data.getQuick(i, j);
			}
			correlation.set(j, 1, sum_of_squares/iproduct[j]);
		}
		return correlation;
	}
	
	public String [] getCorrelationLabels()
	{
		return colNames;
	}

	public double [] getVariances()
	{
		return p_k;
	}

	public double [] getCumSumOfVariances()
	{
		return cum_sum_of_variances;
	}
	
	/** Calculate the correlation with the nth principle component (eigenterm).
	 * Note there are L arrays/study sets in the data.*/
	public double [] getCorrelationWithPCn(int n)
	{
		double [] corr = new double[L];
		double normalizationfactor = 0.0;
		double numerator = 0.0;
		DoubleMatrix2D U = svd.getU();
		for (int j=0;j<ddm.columns();++j) {
			normalizationfactor = 0.0;
			numerator = 0.0;
			for (int i=0;i < ddm.rows();++i) {
				double v = ddm.get(i,j);
				normalizationfactor += v*v;
				numerator += U.get(i,n) * v;
			}
			if (normalizationfactor == 0) corr[j] = 0.0; /* In this case, all values are zero anyway */
			else corr[j] = numerator/normalizationfactor;
		}
		return corr;
	}

	/**
	 * Is a SVD of pvalues?
	 * 
	 * @return
	 */
	public boolean isPValues()
	{
		return pValues;
	}
}
