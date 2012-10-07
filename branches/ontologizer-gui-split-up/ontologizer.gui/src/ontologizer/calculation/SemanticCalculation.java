package ontologizer.calculation;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import java.util.logging.Logger;

import ontologizer.DOTDumper;
import ontologizer.association.Association;
import ontologizer.association.AssociationContainer;
import ontologizer.association.Gene2Associations;
import ontologizer.dotwriter.AbstractDotAttributesProvider;
import ontologizer.dotwriter.GODOTWriter;
import ontologizer.enumeration.GOTermEnumerator;
import ontologizer.go.Ontology;
import ontologizer.go.ParentTermID;
import ontologizer.go.Term;
import ontologizer.go.TermContainer;
import ontologizer.go.TermID;
import ontologizer.go.TermRelation;
import ontologizer.set.StudySet;
import ontologizer.types.ByteString;

/**
 * Stripped-down-version of the general HashMap function suitable
 * for integer keys and double values.
 * 
 * @author Sebastian Bauer
 * @see java.util.HashMap
 */
class IntHashMapForDoubles
{
	/**
	 * The default initial capacity - MUST be a power of two.
	 */
	static final int DEFAULT_INITIAL_CAPACITY = 16;

	/**
	 * The maximum capacity, used if a higher value is implicitly specified
	 * by either of the constructors with arguments.
	 * MUST be a power of two <= 1<<30.
	 */
	static final int MAXIMUM_CAPACITY = 1 << 30;

	/**
	 * The load factor used when none specified in constructor.
	 */
	static final float DEFAULT_LOAD_FACTOR = 0.75f;

	/**
	 * The table, resized as necessary. Length MUST Always be a power of two.
	 */
	transient Entry[] table;

	/**
	 * The number of key-value mappings contained in this map.
	 */
	transient int size;

	/**
	 * The next size value at which to resize (capacity * load factor).
	 * @serial
	 */
	int threshold;

	/**
	 * The load factor for the hash table.
	 *
	 * @serial
	 */
	final float loadFactor;

	/**
	 * The number of times this HashMap has been structurally modified
	 * Structural modifications are those that change the number of mappings in
	 * the HashMap or otherwise modify its internal structure (e.g.,
	 * rehash).  This field is used to make iterators on Collection-views of
	 * the HashMap fail-fast.  (See ConcurrentModificationException).
	 */
	transient volatile int modCount;

	/**
	 * Constructs an empty <tt>HashMap</tt> with the specified initial
	 * capacity and load factor.
	 *
	 * @param  initialCapacity the initial capacity
	 * @param  loadFactor      the load factor
	 * @throws IllegalArgumentException if the initial capacity is negative
	 *         or the load factor is nonpositive
	 */
	public IntHashMapForDoubles(int initialCapacity, float loadFactor)
	{
	    if (initialCapacity < 0)
	        throw new IllegalArgumentException("Illegal initial capacity: " +
	                                           initialCapacity);
	    if (initialCapacity > MAXIMUM_CAPACITY)
	        initialCapacity = MAXIMUM_CAPACITY;
	    if (loadFactor <= 0 || Float.isNaN(loadFactor))
	        throw new IllegalArgumentException("Illegal load factor: " +
	                                           loadFactor);
	
	    // Find a power of 2 >= initialCapacity
	    int capacity = 1;
	    while (capacity < initialCapacity)
	        capacity <<= 1;
	
	    this.loadFactor = loadFactor;
	    threshold = (int)(capacity * loadFactor);
	    table = new Entry[capacity];
	    init();
	}

	/**
	 * Constructs an empty <tt>HashMap</tt> with the specified initial
	 * capacity and the default load factor (0.75).
	 *
	 * @param  initialCapacity the initial capacity.
	 * @throws IllegalArgumentException if the initial capacity is negative.
	 */
	public IntHashMapForDoubles(int initialCapacity)
	{
	    this(initialCapacity, DEFAULT_LOAD_FACTOR);
	}

	/**
	 * Constructs an empty <tt>HashMap</tt> with the default initial capacity
	 * (16) and the default load factor (0.75).
	 */
	public IntHashMapForDoubles()
	{
	    this.loadFactor = DEFAULT_LOAD_FACTOR;
	    threshold = (int)(DEFAULT_INITIAL_CAPACITY * DEFAULT_LOAD_FACTOR);
	    table = new Entry[DEFAULT_INITIAL_CAPACITY];
	    init();
	}


	// internal utilities

	/**
	 * Initialization hook for subclasses. This method is called
	 * in all constructors and pseudo-constructors (clone, readObject)
	 * after HashMap has been initialized but before any entries have
	 * been inserted.  (In the absence of this method, readObject would
	 * require explicit knowledge of subclasses.)
	 */
	void init()
	{
	}

	/**
	 * Applies a supplemental hash function to a given hashCode, which
	 * defends against poor quality hash functions.  This is critical
	 * because HashMap uses power-of-two length hash tables, that
	 * otherwise encounter collisions for hashCodes that do not differ
	 * in lower bits. Note: Null keys always map to hash 0, thus index 0.
	 */
	static int hash(int h) {
	    // This function ensures that hashCodes that differ only by
	    // constant multiples at each bit position have a bounded
	    // number of collisions (approximately 8 at default load factor).
	    h ^= (h >>> 20) ^ (h >>> 12);
	    return h ^ (h >>> 7) ^ (h >>> 4);
	}

	/**
	 * Returns index for hash code h.
	 */
	static int indexFor(int h, int length) {
	    return h & (length-1);
	}

	/**
	 * Returns the number of key-value mappings in this map.
	 *
	 * @return the number of key-value mappings in this map
	 */
	public int size() {
	    return size;
	}

	/**
	 * Returns <tt>true</tt> if this map contains no key-value mappings.
	 *
	 * @return <tt>true</tt> if this map contains no key-value mappings
	 */
	public boolean isEmpty() {
	    return size == 0;
	}

	/**
	 * Returns the value to which the specified key is mapped,
	 * or {@code null} if this map contains no mapping for the key.
	 *
	 * <p>More formally, if this map contains a mapping from a key
	 * {@code k} to a value {@code v} such that {@code (key==null ? k==null :
	 * key.equals(k))}, then this method returns {@code v}; otherwise
	 * it returns {@code null}.  (There can be at most one such mapping.)
	 *
	 * <p>A return value of {@code null} does not <i>necessarily</i>
	 * indicate that the map contains no mapping for the key; it's also
	 * possible that the map explicitly maps the key to {@code null}.
	 * The {@link #containsKey containsKey} operation may be used to
	 * distinguish these two cases.
	 *
	 * @see #put(Object, Object)
	 */
	public double get(int key)
	{
	    int hash = hash(key);
	    for (Entry e = table[indexFor(hash, table.length)]; e != null; e = e.next)
	    {
	    	
	        if (/*e.hash == hash &&*/ e.key == key)
	            return e.value;
	    }
	    return Double.NaN;
	}

	/**
	 * Returns <tt>true</tt> if this map contains a mapping for the
	 * specified key.
	 *
	 * @param   key   The key whose presence in this map is to be tested
	 * @return <tt>true</tt> if this map contains a mapping for the specified
	 * key.
	 */
	public boolean containsKey(int key)
	{
		int hash = hash(key);
	    for (Entry e = table[indexFor(hash, table.length)]; e != null; e = e.next)
	    {
	        if (/*e.hash == hash && */e.key == key)
	            return true;
	    }
	    return false;
	}

	/**
	 * Associates the specified value with the specified key in this map.
	 * If the map previously contained a mapping for the key, the old
	 * value is replaced.
	 *
	 * @param key key with which the specified value is to be associated
	 * @param value value to be associated with the specified key
	 * @return the previous value associated with <tt>key</tt>, or
	 *         <tt>null</tt> if there was no mapping for <tt>key</tt>.
	 *         (A <tt>null</tt> return can also indicate that the map
	 *         previously associated <tt>null</tt> with <tt>key</tt>.)
	 */
	public void put(int  key, double value)
	{
	    int hash = hash(key);
	    int i = indexFor(hash, table.length);
	    for (Entry e = table[i]; e != null; e = e.next)
	    {
	        if (/*e.hash == hash && */e.key == key)
	        {
	            e.value = value;
	            return;
	        }
	    }
	
	    modCount++;
	    addEntry(hash, key, value, i);
	}

	/**
	 * Rehashes the contents of this map into a new array with a
	 * larger capacity.  This method is called automatically when the
	 * number of keys in this map reaches its threshold.
	 *
	 * If current capacity is MAXIMUM_CAPACITY, this method does not
	 * resize the map, but sets threshold to Integer.MAX_VALUE.
	 * This has the effect of preventing future calls.
	 *
	 * @param newCapacity the new capacity, MUST be a power of two;
	 *        must be greater than current capacity unless current
	 *        capacity is MAXIMUM_CAPACITY (in which case value
	 *        is irrelevant).
	 */
	void resize(int newCapacity)
	{
	    Entry[] oldTable = table;
	    int oldCapacity = oldTable.length;
	    if (oldCapacity == MAXIMUM_CAPACITY) {
	        threshold = Integer.MAX_VALUE;
	        return;
	    }
	
	    Entry[] newTable = new Entry[newCapacity];
	    transfer(newTable);
	    table = newTable;
	    threshold = (int)(newCapacity * loadFactor);
	}

	/**
	 * Transfers all entries from current table to newTable.
	 */
	void transfer(Entry[] newTable) {
	    Entry[] src = table;
	    int newCapacity = newTable.length;
	    for (int j = 0; j < src.length; j++) {
	        Entry e = src[j];
	        if (e != null)
	        {
	            src[j] = null;
	            do
	            {
	                Entry next = e.next;
	                int i = indexFor(e.hash, newCapacity);
	                e.next = newTable[i];
	                newTable[i] = e;
	                e = next;
	            } while (e != null);
	        }
	    }
	}
	/**
	 * Removes all of the mappings from this map.
	 * The map will be empty after this call returns.
	 */
	public void clear() {
	    modCount++;
	    Entry[] tab = table;
	    for (int i = 0; i < tab.length; i++)
	        tab[i] = null;
	    size = 0;
	}

	private static final class Entry
	{
	    final int key;
	    double value;
	    Entry next;
	    final int hash;
	
	    /**
	     * Creates new entry.
	     */
	    Entry(int h, int k, double v, Entry n)
	    {
	        value = v;
	        next = n;
	        key = k;
	        hash = h;
	    }

	    public final int getKey()
	    {
	        return key;
	    }
	
	    public final double getValue()
	    {
	        return value;
	    }
	
	    public final double setValue(double newValue)
	    {
	    	double oldValue = value;
	        value = newValue;
	        return oldValue;
	    }
	}

	/**
	 * Adds a new entry with the specified key, value and hash code to
	 * the specified bucket.  It is the responsibility of this
	 * method to resize the table if appropriate.
	 *
	 * Subclass overrides this to alter the behavior of put method.
	 */
	void addEntry(int hash, int key, double value, int bucketIndex)
	{
		Entry e = table[bucketIndex];
		
	    table[bucketIndex] = new Entry(hash, key, value, e);
	    if (size++ >= threshold)
	        resize(2 * table.length);
	}

	/**
	 * Like addEntry except that this version is used when creating entries
	 * as part of Map construction or "pseudo-construction" (cloning,
	 * deserialization).  This version needn't worry about resizing the table.
	 *
	 * Subclass overrides this to alter the behavior of HashMap(Map),
	 * clone, and readObject.
	 */
	void createEntry(int hash, int key, double value, int bucketIndex)
	{
		Entry e = table[bucketIndex];
	    table[bucketIndex] = new Entry(hash, key, value, e);
	    size++;
	}
}


public class SemanticCalculation
{
	private static Logger logger = Logger.getLogger(SemanticCalculation.class.getCanonicalName());

	public static interface ISemanticCalculationProgress
	{
		void init(int max);
		void update(int update);
	};
	
	private int numberOfProcessors = 1;//Runtime.getRuntime().availableProcessors();
	
	private Ontology graph;
	private AssociationContainer goAssociations;

	private StudySet allGenesStudy;
	private GOTermEnumerator enumerator;
	private int totalAnnotated;
	
	/** Similarity cache (indexed by term) */
	private IntHashMapForDoubles [] cache;

	private ReentrantReadWriteLock cacheLock;
	private ReadLock readLock;
	private WriteLock writeLock;

	/**
	 * Non-redundant associations (indexed by genes).
	 * Objects is an array of terms
	 */
	private Object [] associations;

	private HashMap<ByteString,Integer> gene2index = new HashMap<ByteString,Integer>();

	public SemanticCalculation(Ontology g, AssociationContainer assoc)
	{
		this.graph = g;
		this.goAssociations = assoc;

		allGenesStudy = new StudySet("population");
		for (ByteString gene : goAssociations.getAllAnnotatedGenes())
			allGenesStudy.addGene(gene,"");

		enumerator = allGenesStudy.enumerateGOTerms(graph, goAssociations);
		totalAnnotated = enumerator.getAnnotatedGenes(graph.getRootTerm().getID()).totalAnnotated.size();

		cache = new IntHashMapForDoubles[g.maximumTermID()];

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
				for (TermID desc : g.getTermChildren(tid))
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

	/**
	 * Returns the similarity of the two given terms.
	 * 
	 * @param t1
	 * @param t2
	 * @return
	 */
	private double sim(TermID t1, TermID t2)
	{
		/* Similarity of terms is symmetric */
		if (t1.id > t2.id)
		{
			TermID s = t2;
			t2 = t1;
			t1 = s;
		}

		if (cacheLock != null)
			readLock.lock();

		IntHashMapForDoubles map2 = cache[t1.id];
		if (map2 != null)
		{
			double val = map2.get(t2.id);
			if (!Double.isNaN(val))
			{
				if (cacheLock != null) cacheLock.readLock().unlock();	
				return val;
			}
		}

		if (cacheLock != null)
		{
			/* Upgrade lock, must unlock the read lock manually before */
			readLock.unlock();
			writeLock.lock();
		}

		/* Create HashMap when needed, but the value is definitively not there */
		if (map2 == null)
		{
			map2 = new IntHashMapForDoubles();
			cache[t1.id] = map2;
		}

		double p = -Math.log(p(t1,t2));
		map2.put(t2.id,p);
		
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
		
		/* TODO: Research if we can employ sorting omit some or many of
		 * the pairs. 
		 */
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
		
		int entries = study.getGeneCount();

		double [][] mat =  new double[entries][entries];
		int i=0,counter=0;
		
		if (progress != null)
			progress.init(entries * (entries + 1) / 2);
		
		long millis = System.currentTimeMillis();

		/* Create the association mapping, i.e, which gene maps to which entry in the array
		 * of non-redundant association  */
		int [] indices = new int[study.getGeneCount()];
		int k=0;
		for (ByteString g : study)
		{
			Integer idx = gene2index.get(g);
			if (idx == null)
			{
				/* Maybe we can find the gene via a mapping */
				Gene2Associations o2a = goAssociations.get(g);
				if (o2a != null)
					idx = gene2index.get(o2a.name());
			}
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
				for (int j=i;j<indices.length;j++)
				{
					mat[i][j] = mat[j][i] = sim(indices[i],indices[j]);

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
			
			if (progress != null)
				progress.update(counter);

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

	/**
	 * Upcoming test driver.
	 * 
	 * @param args
	 */
	public static void main(String[] args)
	{
		/* Go Graph */
		HashSet<Term> terms = new HashSet<Term>();
		Term c1 = new Term("GO:0000001", "C1");
		Term c2 = new Term("GO:0000002", "C2",new ParentTermID(c1.getID(),TermRelation.IS_A));
		Term c3 = new Term("GO:0000003", "C3",new ParentTermID(c1.getID(),TermRelation.IS_A));
		Term c4 = new Term("GO:0000004", "C4",new ParentTermID(c2.getID(),TermRelation.IS_A));
		Term c5 = new Term("GO:0000005", "C5",new ParentTermID(c2.getID(),TermRelation.IS_A));
		Term c6 = new Term("GO:0000006", "C6", new ParentTermID(c3.getID(),TermRelation.IS_A),new ParentTermID(c2.getID(),TermRelation.IS_A));
		Term c7 = new Term("GO:0000007", "C7", new ParentTermID(c5.getID(),TermRelation.IS_A),new ParentTermID(c6.getID(),TermRelation.IS_A));
		Term c8 = new Term("GO:0000008", "C8", new ParentTermID(c7.getID(),TermRelation.IS_A));
		Term c9 = new Term("GO:0000009", "C9", new ParentTermID(c7.getID(),TermRelation.IS_A));
		Term c10 = new Term("GO:0000010", "C10", new ParentTermID(c9.getID(),TermRelation.IS_A));
		Term c11 = new Term("GO:0000011", "C11", new ParentTermID(c9.getID(),TermRelation.IS_A));

		terms.add(c1);
		terms.add(c2);
		terms.add(c3);
		terms.add(c4);
		terms.add(c5);
		terms.add(c6);
		terms.add(c7);
		terms.add(c8);
		terms.add(c9);
		terms.add(c10);
		terms.add(c11);
		TermContainer termContainer = new TermContainer(terms,"","");
		Ontology graph = new Ontology(termContainer);
		
		HashSet<TermID> tids = new HashSet<TermID>();
		for (Term term : terms)
			tids.add(term.getID());

		/* Associations */
		AssociationContainer assocContainer = new AssociationContainer();
		Random r = new Random(1);

		/* Randomly assign the items (note that redundant associations are filtered out later) */
		for (int i=1;i<=30;i++)
		{
			String itemName = "item" + i;
			int numTerms = r.nextInt(4) + 1;
			
			for (int j=0;j<numTerms;j++)
			{
				int tid = r.nextInt(terms.size())+1;
				assocContainer.addAssociation(new Association(new ByteString(itemName),tid));
			}
		}

		GODOTWriter.writeDOT(graph, new File("graph.dot"), null, tids, new AbstractDotAttributesProvider()
		{
			public String getDotNodeAttributes(TermID id)
			{
				return "label=\""+id.toString()+"\"";
			}
		});
		
	}
}
