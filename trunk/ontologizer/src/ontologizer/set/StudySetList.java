/*
 * Created on 10.07.2005
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ontologizer.set;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import ontologizer.types.ByteString;

/**
 * Container class of all considered study sets.
 *
 * @author Sebastian Bauer
 */
public class StudySetList implements Iterable<StudySet>
{
	/** The name of the StudySetList */
	private String name = new String();
	
	/** Container for all study sets */
	private ArrayList<StudySet> list = new ArrayList<StudySet>();

	/**
	 * Constructs the StudySetList. For every file in
	 * the given path (whose name's suffix matches suffix)
	 * a separate study set is created.
	 * 
	 * @param path
	 * 		  defines the path to the directory where all
	 *        the study files are placed.
	 *
	 * @param suffix
	 * 		  only study files with the given suffix are
	 * 		  consideres. Use an empty sting if the suffix
	 * 		  is irrelevant.
	 */
	public StudySetList(final String path, final String suffix)
	{
		/* do not accept any files that start with `.'. or
		 * end with the given suffix */
		FilenameFilter filter = new FilenameFilter()
		{
			public boolean accept(File dir, String name)
			{
				if (name.startsWith("."))
					return false;

				if (suffix == null) return true;
				if (suffix.length() > 0) return name.endsWith(suffix);
				else return true;
			}
		};

		/* List of files with gene (product) names to be annotated */
// TODO: ???
// String path = removeTrailingSlash(path);
		File directory = new File(path);
// TODO: Check if path is really a dir
		File[] files = directory.listFiles(filter);
		
		if (files == null || files.length == 0)
		{
			// Either dir does not exist or is not a directory
			System.out.println("Could not locate files in " + directory);
			System.out.println("Try entering the complete path or \".\" "
					+ "for the current working directory.");
			System.exit(-1);
		}

		for (int i = 0; i < files.length; i++)
		{
			File myfile = files[i];

			/* TODO: Can this be moved into the filter? */
			if (myfile.isDirectory())
				continue;

			/* Construct the study set */
			try
			{
				StudySet study = StudySetFactory.createFromFile(myfile,false);

				/* Enqueue the study set into the array list */
				list.add(study);
			} catch(FileNotFoundException fne)
			{
				/* Quite strange that this happnen but we must react anywhy */
				System.err.println("Ignoring study file " + myfile.getAbsolutePath() + " because it couldn't be found."); 
			} catch (IOException e)
			{
				System.err.println("Ignoring study file " + myfile.getAbsolutePath() + " because: " + e.getMessage());
			}
		}
	}

	/**
	 * A constructor for StudySetLists not coming from file systems
	 * 
	 * @param name The name of the StudySetList
	 */
	public StudySetList(String name)
	{
		this.name = name;
	}

	/**
	 * Adds a StudySet to the list
	 * 
	 * @param study The StudySet to be added
	 */
	public void addStudySet(StudySet study)
	{
		list.add(study);
	}
	
	/**
	 * @return the set of all genes participating within the studies
	 */
	public Set<ByteString> getGeneSet()
	{
		/**
		 * Contains the name of all participating genenames of all
		 * studies.
		 */
		HashSet<ByteString> geneSet = new HashSet<ByteString>();

		for (StudySet study : this)
		{
			for (ByteString geneName : study)
				geneSet.add(geneName);
		}

		return geneSet;
	}

	/**
	 * 
	 * @return the iterator over all containung study sets.
	 */
	public Iterator<StudySet> iterator()
	{
		return list.iterator();
	}
	

	/**
	 * Returns the size of the study list (i.e. the number of study sets contained
	 * within the list)
	 * 
	 * @return
	 */
	public int size()
	{
		return list.size();
	}
}
