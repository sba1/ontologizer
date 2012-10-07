package ontologizer.calculation.svd;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.text.*;

import ontologizer.association.AssociationContainer;
import ontologizer.calculation.AbstractGOTermProperties;
import ontologizer.calculation.EnrichedGOTermsResult;
import ontologizer.go.Ontology;
import ontologizer.go.Namespace;
import ontologizer.go.Term;
import ontologizer.go.TermContainer;
import ontologizer.go.TermID;
import ontologizer.set.StudySet;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.colt.matrix.linalg.SingularValueDecomposition;

/**
 * This class performs a Singular Value Decomposition of the GO annotations
 * using the CERN Colt library.
 *  
 * @author Sebastian Bauer, Peter Robinson
 *
 */
public class SVD
{
	public static SVDResult doSVD(TermContainer goTerms, Ontology goGraph,
			ArrayList<EnrichedGOTermsResult> studySetResultList, StudySet populationSet, boolean pValues, boolean onlySignificant) throws IOException
	{
		HashSet<TermID> includedTerms;

		if (onlySignificant)
		{
			includedTerms = new HashSet<TermID>();
			for (EnrichedGOTermsResult termsResult : studySetResultList)
			{
				for (AbstractGOTermProperties prop : termsResult)
				{
					if (prop.p_adjusted < 0.05)
					{
						TermID id = prop.goTerm.getID();
						if (!includedTerms.contains(id))
							includedTerms.addAll(goGraph.getTermsOfInducedGraph(null, id));
					}
				}
			}

		} else includedTerms = null;

		/** To which row the given term belongs to */
		HashMap<TermID,Integer> term2row = new HashMap<TermID,Integer>();
		
		/** Row to term */
		HashMap<Integer,TermID> row2term = new HashMap<Integer,TermID>();
		
		/** Counter */
		int currentTermNo = 0;
		AssociationContainer associations = null;
		
		/** Number of studies */
		int numberOfStudies = studySetResultList.size();

		String [] colNames = new String[numberOfStudies];

		/* Get all terms */
		int s = 0;
		for (EnrichedGOTermsResult termsResult : studySetResultList)
		{
			if (associations == null) associations = termsResult.getAssociations();
			else if (associations != termsResult.getAssociations())
				throw new IllegalArgumentException("The associations belonging to the result must be all the same!");

			colNames[s++] = termsResult.getStudySet().getName();

			for (AbstractGOTermProperties prop : termsResult)
			{
				TermID tid = prop.goTerm.getID();

				if (includedTerms == null || includedTerms.contains(tid))
				{
					if (!term2row.containsKey(tid))
					{
						term2row.put(tid, currentTermNo);
						row2term.put(currentTermNo, tid);
						currentTermNo++;
					}
				}
			}
		}

		/* Build count matrix */
		int [][] counts = new int[term2row.size()][studySetResultList.size()];
		double [][] pVals = new double[term2row.size()][studySetResultList.size()];
		DoubleMatrix2D ddm = new DenseDoubleMatrix2D(term2row.size(),studySetResultList.size());
		for (int i=0;i<ddm.rows();i++)
		{
			for (int j=0;j<ddm.columns();j++)
				ddm.setQuick(i,j,0);
		}

		int i=0;
		for (EnrichedGOTermsResult studySetResult : studySetResultList)
		{
			for (AbstractGOTermProperties prop : studySetResult)
			{
				TermID tid = prop.goTerm.getID();
				
				Integer rowInt = term2row.get(tid);
				if (rowInt == null) continue;

				if (pValues) ddm.setQuick(rowInt,i,-Math.log10(prop.p));
				else ddm.setQuick(rowInt,i,prop.annotatedStudyGenes);

				counts[term2row.get(tid)][i] = prop.annotatedStudyGenes;
				pVals[term2row.get(tid)][i] = prop.p;
			}
			i++;
		}

		/* Write out overview */
		PrintWriter out = new PrintWriter(new FileWriter("overview.txt"));
		for (i=0;i<ddm.rows();i++)
		{
			Term t = goTerms.get(row2term.get(i));
			if (t != null)
			{
				out.print("\"" + t.getIDAsString() + "\"");
				for (int j=0;j<ddm.columns();j++)
				{
					out.print("\t");
					out.print(ddm.get(i,j));
				}
				out.print("\t\"" + t.getName() + "\"\n");
			} else
			{
				System.out.println(i + " " + row2term.get(i));
			}
		}
		out.close();
		
		if (!pValues)
		{
			ddm = SVD.normalizeDataBySubontology(ddm, goTerms, row2term);
		//ddm = SVD.normalizeDataByRoot(ddm,goTerms, row2term);
		
			out = new PrintWriter(new FileWriter("overviewNORMALIZED.txt"));
			for (i=0;i<ddm.rows();i++)
			{
				Term t = goTerms.get(row2term.get(i));
				if (t != null)
				{
					out.print("\"" + t.getIDAsString() + "\"");
					for (int j=0;j<ddm.columns();j++)
					{
						out.print("\t");
						out.print(ddm.get(i,j));
					}
					out.print("\t\"" + t.getName() + "\"\n");
				} else
				{
					System.out.println(i + " " + row2term.get(i));
				}
			}
			out.close();
		}
		
		/* Center the matrix */
		ddm = SVD.centerMatrix(ddm);
		out = new PrintWriter(new FileWriter("overviewCENTERED.txt"));
		for (i=0;i<ddm.rows();i++)
		{
			Term t = goTerms.get(row2term.get(i));
			if (t != null)
			{
				out.print("\"" + t.getIDAsString() + "\"");
				for (int j=0;j<ddm.columns();j++)
				{
					out.print("\t");
					out.print(ddm.get(i,j));
				}
				out.print("\n");
				//out.print("\t\"" + t.getName() + "\"\n");
			} else
			{
				System.out.println(i + " " + row2term.get(i));
			}
		}
		out.close();
		
		System.out.println("******About to do SVD");

		SingularValueDecomposition svd = new SingularValueDecomposition(ddm);
		DoubleMatrix2D u = svd.getU();

		double SV[] = svd.getSingularValues();
		double SVsum = 0.0;
		double SVcumsum[] = new double[SV.length];
		for (i=0;i<SV.length;++i) SV[i] = SV[i]*SV[i]; // square <-> variance
		for (i=0;i<SV.length;++i) SVsum += SV[i];
		SVcumsum[0] = SV[0];
		for (i=1;i<SV.length;++i) {
			SVcumsum[i] = SVcumsum[i-1]+SV[i];
		}
		NumberFormat formatter = new DecimalFormat("#.#");
		for (i=0;i<SV.length;++i){
			String percentage = formatter.format(100*SVcumsum[i]/SVsum);
			System.out.println("Sing Val " + (i+1) + " = " 
					+ formatter.format(SV[i]) + "(" + percentage + "%)");
		}

		/* Now create the svd result */
		SVDResult svdResult = new SVDResult(goGraph, associations, svd, ddm, colNames, pValues);
		for (i=0;i<counts.length;i++)
		{
			Term t = goTerms.get(row2term.get(i));
			if (t != null)
			{
				SVDGOTermProperties termProp = new SVDGOTermProperties(numberOfStudies);
				termProp.rowInMatrix = i;
				termProp.goTerm = t;

				for (int j=0;j<counts[0].length;j++)
				{
					termProp.counts[j] = counts[i][j];
					termProp.weights[j] = u.get(i, j);
					termProp.pVals[j] = pVals[i][j];
				}

				svdResult.addGOTermProperties(termProp);
			}
		}

/*		double min = Double.POSITIVE_INFINITY;
		double max = Double.NEGATIVE_INFINITY;

		for (i=0;i<u.rows();i++)
		{
			double v = Math.abs(u.get(i, 0));
			if (v < min) min = v;
			if (v > max) max = v;
		}
		
		EnrichedGOTermsResult ssr = new EnrichedGOTermsResult(populationSet,populationSet.getGeneCount());
		for (i=0;i<u.rows();i++)
		{
			TermForTermGOTermProperties prop = new TermForTermGOTermProperties();
			double v = Math.abs(u.get(i,0));

			double m = 2./(max - min);
			double n = -1 - 2 * min/(max - min);

			prop.goTerm = goGraph.getGOTerm(row2term.get(i));
			prop.p_adjusted = 1-((v * m + n)+1)/2;
			prop.p = u.get(i,0);
			ssr.addGOTermProperties(prop);
		}*/
		return svdResult;
	}
	
	/**
	 * Subtract the row mean from each entry.
	 * @param ddm A matrix with normalized GO term counts.
	 * @return A centered normalized matrix with GO term counts. 
	 */
	private static DoubleMatrix2D centerMatrix(DoubleMatrix2D ddm){
		int i,j;
		int nrow = ddm.rows();
		int ncol = ddm.columns();

		for (i=0;i<nrow;++i) {
			double mean = 0.0;
			for (j=0;j<ncol;++j) {
				mean += ddm.get(i, j);
			}
			mean /= ncol;
			for (j=0;j<ncol;++j) {
				double v = ddm.get(i,j);
				ddm.set(i, j, v-mean);
			}
		}

		return ddm;
	}
	
	/**
	 * Normalize so that the column sum for terms in each of the three
	 * subontologies biological process, cellular component, and molecular
	 * function is the same for each column
	 * @param ddm
	 * @param goTerms
	 * @param row2term
	 * @return
	 */
	private static DoubleMatrix2D normalizeDataBySubontology(DoubleMatrix2D ddm,TermContainer goTerms,HashMap<Integer,TermID> row2term){
		int i,j;
		int ncol = ddm.columns();
		double biological_process[] = new double[ncol];
		double cellular_component[] = new double[ncol];
		double molecular_function[] = new double[ncol];
		double avg_BP,avg_CC,avg_MF;avg_BP=avg_CC=avg_MF=0.0;
		
		for (j=0;j<ddm.columns();++j) {biological_process[j]=0.0;cellular_component[j]=0.0; molecular_function[j]=0.0;}
		
		/* Indices of roots of subontologies */
		Integer BPidx=null, CCidx=null, MFidx=null; 
		TermID BioProID = new TermID(8150);
		TermID CellCompID = new TermID("GO:0005575");
		TermID  MolFunID = new TermID(3674);
		
		for (i=0;i<ddm.rows();++i) {
			//System.out.println("Got term id " + row2term.get(i));
			if (row2term.get(i).equals(BioProID)){ BPidx = new Integer(i); System.out.println("Found BP "+BPidx); } 
			if (row2term.get(i).equals(CellCompID)) { CCidx = new Integer(i);System.out.println("Found CC "+CCidx); } 
			if (row2term.get(i).equals(MolFunID)){ MFidx = new Integer(i); System.out.println("Found MF "+MFidx); } 
			
		}
		
		/* Find total number of annotations to terms from the three subontologies
		 * for each of the columns. */
		for (j=0;j<ncol;++j)
		{
			biological_process[j] = BPidx==null?1:ddm.get(BPidx,j);
			molecular_function[j] = MFidx==null?1:ddm.get(MFidx,j);
			cellular_component[j] = CCidx==null?1:ddm.get(CCidx,j);
			System.out.println("BP is " + biological_process[j] + " CC is "+
					cellular_component[j] + " MF is " + molecular_function[j]);	
		}
		
			
		for (j=0;j<ncol;++j) {
			avg_BP += biological_process[j];
			avg_CC += cellular_component[j]; 
			avg_MF += molecular_function[j];
		}
		avg_BP /= ncol;
		avg_CC /= ncol;
		avg_MF /= ncol;

		System.out.println("******Got average counts");

		/* Calculate correction factors */
		double factor_BP[] = new double[ncol];
		double factor_CC[] = new double[ncol];
		double factor_MF[] = new double[ncol];

		for (j=0;j<ncol;++j) {
			factor_BP[j] = avg_BP / biological_process[j]; System.out.println("BP Factor is " + factor_BP[j]);
			factor_CC[j] = avg_CC / cellular_component[j];System.out.println("CC Factor is " + factor_CC[j]);
			factor_MF[j] = avg_MF / molecular_function[j];System.out.println("MF Factor is " + factor_MF[j]);
		}

		/* Now normalize so that the proportion of counts is the same
		 * for all subontologies and all arrays.
		 */
		for (j=0;j<ncol;++j)
		{
			for (i=0;i<ddm.rows();i++)
			{
				Term t = goTerms.get(row2term.get(i));
				if (t != null)
				{
					double v = ddm.get(i,j);
					Namespace.NamespaceEnum nsp = Namespace.getNamespaceEnum(t.getNamespace());
					if (nsp == Namespace.NamespaceEnum.BIOLOGICAL_PROCESS) 
						ddm.set(i,j,v*factor_BP[j]);
					else if (nsp == Namespace.NamespaceEnum.MOLECULAR_FUNCTION)
						ddm.set(i,j,v*factor_MF[j]);
					else if (nsp == Namespace.NamespaceEnum.CELLULAR_COMPONENT)
						ddm.set(i,j,v*factor_CC[j]);

				}
			}
		}
		System.out.println("******Finished normalizing");
		return ddm;
	}
	
	
	private static DoubleMatrix2D normalizeDataByRoot(DoubleMatrix2D ddm,TermContainer goTerms,HashMap<Integer,TermID> row2term){
		int i,j;
		int ncol = ddm.columns();
		double biological_process[] = new double[ncol];
		double cellular_component[] = new double[ncol];
		double molecular_function[] = new double[ncol];
		double avg_BP,avg_CC,avg_MF;avg_BP=avg_CC=avg_MF=0.0;
		double avg_GO;
		
		for (j=0;j<ddm.columns();++j) {biological_process[j]=0.0;cellular_component[j]=0.0; molecular_function[j]=0.0;}
		
		/* Indices of roots of subontologies */
		Integer BPidx=null, CCidx=null, MFidx=null; 
		TermID BioProID = new TermID(8150);
		TermID CellCompID = new TermID("GO:0005575");
		TermID  MolFunID = new TermID(3674);
		
		for (i=0;i<ddm.rows();++i) {
			//System.out.println("Got term id " + row2term.get(i));
			if (row2term.get(i).equals(BioProID)){ BPidx = new Integer(i); System.out.println("Found BP "+BPidx); } 
			if (row2term.get(i).equals(CellCompID)) { CCidx = new Integer(i);System.out.println("Found CC "+CCidx); } 
			if (row2term.get(i).equals(MolFunID)){ MFidx = new Integer(i); System.out.println("Found MF "+MFidx); } 
			
		}
		if (BPidx == null) { System.out.println("BXidx is null"); System.exit(1); }
		
		
		/* Find total number of annotations to terms from the three subontologies
		 * for each of the columns. */
		for (j=0;j<ncol;++j)
		{
			biological_process[j] = ddm.get(BPidx,j);
			molecular_function[j] = ddm.get(MFidx,j);
			cellular_component[j] = ddm.get(CCidx,j);
			System.out.println("BP is " + biological_process[j] + " CC is "+
					cellular_component[j] + " MF is " + molecular_function[j]);	
		}
		
			
		for (j=0;j<ncol;++j) {
			avg_BP += biological_process[j];
			avg_CC += cellular_component[j]; 
			avg_MF += molecular_function[j];
		}
		
		avg_GO = (avg_BP + avg_CC + avg_MF) / ncol;
		

		System.out.println("******Got average counts");

		/* Calculate correction factors */
		double factor[] = new double[ncol];

		for (j=0;j<ncol;++j) {
			factor[j] = avg_GO / (biological_process[j]+cellular_component[j]+molecular_function[j]);
		}

		/* Now normalize so that the proportion of counts is the same
		 * for all subontologies and all arrays.
		 */
		for (j=0;j<ncol;++j)
		{
			for (i=0;i<ddm.rows();i++)
			{
				Term t = goTerms.get(row2term.get(i));
				if (t != null)
				{
					double v = ddm.get(i,j);
					ddm.set(i,j,v*factor[j]);
				}
			}
		}
		System.out.println("******Finished normalizing");
		return ddm;
	}
	
	
	
	
	
	/**
	 * Normalize so that the column sum is the same for each column
	 * @param ddm
	 * @param goTerms
	 * @param row2term
	 * @return
	 */
	private static DoubleMatrix2D normalizeData(DoubleMatrix2D ddm,TermContainer goTerms,HashMap<Integer,TermID> row2term){
		int i;
		/* Subtract the mean of the row from every row */
		for (int colidx=0;colidx<ddm.columns();++colidx)
		{
			double colSum = 0.0;

			for (i=0;i<ddm.rows();i++)
			{
				Term t = goTerms.get(row2term.get(i));
				if (t != null)
				{
					double v = ddm.get(i,colidx);
					colSum += v;
				}
			}
			colSum /= ddm.rows();

			for (i=0;i<ddm.rows();i++)
			{
				Term t = goTerms.get(row2term.get(i));
				if (t != null)
				{
					double v = ddm.get(i,colidx);
					v -= colSum;
					ddm.set(i,colidx,v);
				}
			}
		}
		return ddm;
	}
	
	
	/**
	 * Scale data such that the codomain for values in every row spans [-1 1] 
	 * Leave in for testing, probably not a good way to normalize this data
	 **/
	private DoubleMatrix2D scaleData(DoubleMatrix2D ddm,TermContainer goTerms,HashMap<Integer,TermID> row2term){
		int i;
		ArrayList<Integer> ignoreList = new ArrayList<Integer>();
		
		for (i=0;i<ddm.rows();i++)
		{
			Term t = goTerms.get(row2term.get(i));
			if (t != null)
			{
				double min = Double.POSITIVE_INFINITY;
				double max = Double.NEGATIVE_INFINITY;

				for (int j=0;j<ddm.columns();j++)
				{
					double v = ddm.get(i,j);
					if (v < min) min = v;
					if (v > max) max = v;
				}



				if (max != min)
				{
					double m = 2./(max - min);
					double n = -1 - 2 * min/(max - min);

					for (int j=0;j<ddm.columns();j++)
					{
						double v = ddm.get(i,j);
						v = v * m + n;
						ddm.set(i,j,v);
					}
				} else
				{
					ignoreList.add(i);
					for (int j=0;j<ddm.columns();j++)
					{
						ddm.set(i,j,0);
					}
				}
			} else
			{
				System.err.println("Warning: Got a Null term at ontologizer.calculation.svd.SVD");
			}
		}
		return ddm;

	}

}
