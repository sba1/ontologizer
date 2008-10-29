package ontologizer.calculation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import java.util.logging.Logger;

import ontologizer.ByteString;
import ontologizer.GOTermEnumerator;
import ontologizer.StudySet;
import ontologizer.association.AssociationContainer;
import ontologizer.go.GOGraph;
import ontologizer.go.TermID;

public class SemanticCalculation
{
	private static Logger logger = Logger.getLogger(SemanticCalculation.class.getCanonicalName());

	public static interface ISemanticCalculationProgress
	{
		void init(int max);
		void update(int update);
	};
	
	private int numberOfProcessors = 1;//Runtime.getRuntime().availableProcessors();
	
	private GOGraph graph;
	private AssociationContainer goAssociations;

	private StudySet allGenesStudy;
	private GOTermEnumerator enumerator;
	private int totalAnnotated;
	
	/** Similarity cache (indexed by term) */
	private Object [] cache;

	private ReentrantReadWriteLock cacheLock;
	private ReadLock readLock;
	private WriteLock writeLock;

	/**
	 * Non-redundant associations (indexed by genes).
	 * Objects is an array of terms
	 */
	private Object [] associations;

	private HashMap<ByteString,Integer> gene2index = new HashMap<ByteString,Integer>();

	public SemanticCalculation(GOGraph g, AssociationContainer assoc)
	{
		this.graph = g;
		this.goAssociations = assoc;

		allGenesStudy = new StudySet("population");
		for (ByteString gene : goAssociations.getAllAnnotatedGenes())
			allGenesStudy.addGene(gene,"");

		enumerator = allGenesStudy.enumerateGOTerms(graph, goAssociations);
		totalAnnotated = enumerator.getAnnotatedGenes(graph.getRootGOTerm().getID()).totalAnnotated.size();

		cache = new Object[g.maximumTermID()];

		/* Making associations non-redundant */
		associations = new Object[allGenesStudy.getGeneCount()];
		int i = 0;
		for (ByteString gene : allGenesStudy)
		{
			gene2index.put(gene,i);

			ArrayList<TermID> assocList = assoc.get(gene).getAssociations();
			HashSet<TermID> inducedNodes = new HashSet<TermID>();
			for (TermID tid : assocList)
				inducedNodes.addAll(g.getTermsOfInducedGraph(null, tid));
			HashSet<TermID> nonRedundantTerms = new HashSet<TermID>();

			termloop:
			for (TermID tid : assocList)
			{
				for (TermID desc : g.getTermsDescendants(tid))
				{
					if (inducedNodes.contains(desc))
						continue termloop;
				}
				nonRedundantTerms.add(tid);
			}

			TermID terms[] = new TermID[nonRedundantTerms.size()];
			int j=0;
			for (TermID t : nonRedundantTerms)
				terms[j++]=t;

			/* TODO: Sort terms according to their information content */
			associations[i] = terms;
			i++;
		}

		if (numberOfProcessors > 1)
		{
			cacheLock = new ReentrantReadWriteLock();
			readLock = cacheLock.readLock();
			writeLock = cacheLock.writeLock();
		}
	}

	/**
	 * Returns the information content of the given term.
	 * 
	 * @param id
	 * @return
	 */
	public double p(TermID id)
	{
		return (double)enumerator.getAnnotatedGenes(id).totalAnnotatedCount() / totalAnnotated;
	}
	
	/**
	 * Returns the shared information content of two given terms
	 *  
	 * @param t1
	 * @param t2
	 * @return
	 */
	private double p(TermID t1, TermID t2)
	{
		Collection<TermID> sharedParents = graph.getSharedParents(t1, t2);
		double p = 1.0;
	
		/* The information content of two terms is defined as the minimum of
		 * the information content of the shared parents.
		 */
		for (TermID t : sharedParents)
		{
			double newP = p(t);
			if (newP < p) p = newP; 
		}
		return p;
	}

//	private int queries;
//	private int waitsForReading;
//	private int waitsForWriting;

//	private int misses;
//	private int lastMisses;
//	private long millis = System.currentTimeMillis();
		
	/**
	 * Returns the similarity of the two given terms.
	 * 
	 * @param t1
	 * @param t2
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private double sim(TermID t1, TermID t2)
	{
		if (cacheLock != null)
			readLock.lock();

		HashMap<TermID,Double> map = (HashMap<TermID, Double>) cache[t1.id];
		if (map != null)
		{
			Double val = map.get(t2);
			if (val != null)
			{
				if (cacheLock != null) cacheLock.readLock().unlock();	
				return val;
			}
		}

//		long newMillis = System.currentTimeMillis();
////		lastMisses++;
//		if (newMillis - millis > 250)
//		{
//			millis = newMillis;
////			misses += lastMisses;
////			System.out.println(Thread.currentThread().getName() + " lastMisses=" + lastMisses + " misses=" + misses + " queries=" + queries + " ratio=" + ((float)misses / queries));
////			lastMisses = 0;
//			System.out.println(Thread.currentThread().getName() + "  " + queries + "  " + waitsForReading + "  " + waitsForWriting);
//		}


		if (cacheLock != null)
		{
			/* Upgrade lock, must unlock the read lock manually before */
			readLock.unlock();
			writeLock.lock();
		}

		/* Create HashMap when needed, but the value is definitively not there */
		if (map == null)
		{
			map = new HashMap<TermID,Double>();
			cache[t1.id] = map;
		}

		double p = -Math.log(p(t1,t2));
		map.put(t2,p);
		
		if (cacheLock != null)
			writeLock.unlock();

		return p;
	}

	
	/**
	 * Returns the similarity of two given genes.
	 * 
	 * @param index of gene1
	 * @param index of gene2
	 * @return
	 */
	private double sim(int g1, int g2)
	{
		double sim;
		
		if (g1 < 0 || g2 < 0) return 0;
		
		sim = 0.0;

		TermID [] tl1 = (TermID[])associations[g1]; 
		TermID [] tl2 = (TermID[])associations[g2];
		
		for (TermID t1 : tl1)
		{
			for (TermID t2 : tl2)
			{
				double newSim = sim(t1,t2);
				if (newSim > sim) sim = newSim;
			}
		}
		return sim;
	}
	
	/**
	 * Returns the similarity of two given genes.
	 * 
	 * @param g1
	 * @param g2
	 * @return
	 */
	public double sim(ByteString g1, ByteString g2)
	{
		double sim = 0.0;

		if (!(goAssociations.containsGene(g1))) return 0;
		if (!(goAssociations.containsGene(g2))) return 0;
		
		List<TermID> tl1 = goAssociations.get(g1).getAssociations();
		List<TermID> tl2 = goAssociations.get(g2).getAssociations();
		
//		System.out.println(tl1.size() + "  " + tl2.size());

		for (TermID t1 : tl1)
		{
			for (TermID t2 : tl2)
			{
				double newSim = sim(t1,t2);
				if (newSim > sim) sim = newSim;
			}
		}
		return sim;
	}

	/**
	 * Calculates the similarity of genes of the study set.
	 * 
	 * @param study
	 * @return
	 */
	public SemanticResult calculate(StudySet study)
	{
		return calculate(study,null);
	}

	class WorkerThread extends Thread
	{
		class Message {};
		class BeginWorkMessage extends Message{};
		class FinishMessage extends Message{};

		/** Where the thread is put in when it is unemployed */
		private BlockingQueue<WorkerThread> unemployedQueue;

		private BlockingQueue<Message> messageQueue = new LinkedBlockingQueue<Message>();

		private int addPairCount;
		private int pairsDone;
		
		/** Array of entries that need to be processed by this thread */
		private int [] work;
		
		private final static int WORK_LENGTH = 4000;

		private double [][] mat;
		private int [] indices;

		/**
		 * 
		 * @param unemployedQueue defines the queue in which the threads adds itself when a job has been finished.
		 * @param mat the result matrix
		 * @param indices matrix coordinates to coordinates used by the sim() method. 
		 */
		public WorkerThread(BlockingQueue<WorkerThread> unemployedQueue, double [][] mat, int [] indices)
		{
			work = new int[WORK_LENGTH];
			this.unemployedQueue = unemployedQueue;
			this.mat = mat;
			this.indices = indices;
		}

		@Override
		public void run()
		{
			try {
				unemployedQueue.put(this);

				while (true)
				{
					Message msg =  messageQueue.take();

					if (msg instanceof BeginWorkMessage)
					{
						for (int i=0;i<addPairCount;i+=2)
						{
							int i1 = indices[work[i]];
							int i2 = indices[work[i+1]];

							if (i1 >= 0 || i2 >=0)
								mat[work[i]][work[i+1]] = sim(i1,i2);							
						}

						pairsDone = addPairCount / 2;
						addPairCount = 0;
						unemployedQueue.put(this);
					} else
					{
						if (msg instanceof FinishMessage)
							break;
					}
				}
			}
			catch (InterruptedException e)
			{
			}
		}

		/**
		 * 
		 * @param g1
		 * @param g2
		 * @return whether there is more place in the queue.
		 * 
		 */
		public boolean addPairForWork(int g1, int g2)
		{
			work[addPairCount] = g1;
			work[addPairCount+1] = g2;
			addPairCount += 2;
			return addPairCount < WORK_LENGTH;
		}

		/**
		 * Start working on the feeded pairs.
		 * @param unemployedQueue 
		 * @throws InterruptedException 
		 */
		public void fire() throws InterruptedException
		{
			messageQueue.put(new BeginWorkMessage());
		}
		
		public int getPairsDone()
		{
			return pairsDone;
		}

		public void finish() throws InterruptedException
		{
			messageQueue.put(new FinishMessage());
		}
	}

	public SemanticResult calculate(StudySet study, ISemanticCalculationProgress progress)
	{
		SemanticResult sr = new SemanticResult();
		
		long start = System.currentTimeMillis();

		double [][] mat =  new double[study.getGeneCount()][study.getGeneCount()];
		int i=0,counter=0;
		
		if (progress != null)
			progress.init(study.getGeneCount() * study.getGeneCount());
		
		long millis = System.currentTimeMillis();

		/* Create the association mapping, i.e, which gene maps to which entry in the array
		 * of non-redundant association  */
		int [] indices = new int[study.getGeneCount()];
		int k=0;
		for (ByteString g : study)
		{
			Integer idx = gene2index.get(g);
			if (idx != null) indices[k] = idx;
			else indices[k] = -1;
			k++;
		}

		if (numberOfProcessors > 1)
		{
			try
			{
				/* Create and start the worker threads and put them in the queue */
				WorkerThread [] wt = new WorkerThread[numberOfProcessors];
				BlockingQueue<WorkerThread> unemployedQueue = new LinkedBlockingQueue<WorkerThread>();		
				for (int j=0;j<numberOfProcessors;j++)
				{
					wt[j] = new WorkerThread(unemployedQueue,mat,indices);
					wt[j].start();
				}

				/* Take first unemployed thread */
				WorkerThread currentWorker = unemployedQueue.take();
	
				for (i=0;i<indices.length;i++)
				{
					for (int j=0;j<indices.length;j++)
					{
						if (!currentWorker.addPairForWork(i,j))
						{
							currentWorker.fire();
							
							/* Take next unemployed thread (may wait if there is no unemployed thread left) */
							currentWorker = unemployedQueue.take();
							counter += currentWorker.pairsDone;
							currentWorker.pairsDone = 0;

							long newMillis = System.currentTimeMillis();
							if (newMillis - millis > 200)
							{
								millis = newMillis;
								progress.update(counter);
							}
						}
					}
				}

				for (int j=0;j<numberOfProcessors;j++)
				{
					wt[j].finish();
					wt[j].join();
				}

			} catch(InterruptedException e)
			{
				e.printStackTrace();
			}
			
		} else
		{
			/* Single threaded */
			for (i=0;i<indices.length;i++)
			{
				for (int j=0;j<indices.length;j++)
				{
					mat[i][j] = sim(indices[i],indices[j]);

					if (progress != null)
					{
						if (counter++ % 1000 == 0)
						{
							long newMillis = System.currentTimeMillis();
							if (newMillis - millis > 200)
							{
								millis = newMillis;
								progress.update(counter);
							}
						}
					}
				}
			}

		}
		sr.mat = mat;
		sr.names = study.getGenes();
		sr.name = study.getName();
		sr.assoc = goAssociations;
		sr.g = graph;
		sr.calculation = this;
		
		long end = System.currentTimeMillis();

		logger.info("Took " + ((end - start) / 1000.0f) + "s for the analysis");

		return sr;
	}

	public void calculate()
	{
		long millis = System.currentTimeMillis();
		int gene = 0;
		
		for (int i=0;i<associations.length;i++)
		{
			for (int j=0;j<associations.length;j++)
			{
				sim(i,j);
			}
			long newMillis = System.currentTimeMillis();
			if (newMillis > millis + 250)
			{
				System.out.println(gene * 100.0 / allGenesStudy.getGeneCount() + "%");
				millis = newMillis;
			}
			gene++;
		}
	}
}
