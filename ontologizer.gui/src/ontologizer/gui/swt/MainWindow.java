/*
 * Created on 01.11.2005
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ontologizer.gui.swt;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.custom.TreeEditor;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.events.TreeEvent;
import org.eclipse.swt.events.TreeListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import ontologizer.FileCache;
import ontologizer.FileCache.FileCacheUpdateCallback;
import ontologizer.association.AssociationContainer;
import ontologizer.calculation.CalculationRegistry;
import ontologizer.gui.swt.ProjectSettingsComposite.InfoTextClickListener;
import ontologizer.gui.swt.images.Images;
import ontologizer.gui.swt.support.SWTUtil;
import ontologizer.ontology.Ontology;
import ontologizer.ontology.Subset;
import ontologizer.ontology.Term;
import ontologizer.statistics.TestCorrectionRegistry;
import ontologizer.worksets.WorkSet;
import ontologizer.worksets.WorkSetList;
import ontologizer.worksets.WorkSetLoadThread;
import ontologizer.workspace.ItemSet;
import ontologizer.workspace.Project;
import ontologizer.workspace.ProjectSettings;

class TreeItemData
{
	/** The project where this entry belongs to */
	public Project project;

	/** The possible items associated to the entry */
	public ItemSet items;
};

/**
 * Ontologizer's main window.
 *
 * TODO: Separate out the data.
 *
 * @author Sebastian Bauer
 *
 */
public class MainWindow extends ApplicationWindow
{
	/** The logger */
	private static Logger logger = Logger.getLogger(MainWindow.class.getCanonicalName());

	public static final String PROJECT_SETTINGS_NAME = ".project";

	/* Manually added attributes */
	private File workspaceDirectory;
	private TreeItem currentSelectedItem = null;
	private String currentImportFileName = null;
	private String currentExportFileName = null;

	private TreeItem treeItemWhenWorkSetIsChanged;

	private Menu menuBar = null;
	private Menu submenu = null;
	private Composite composite = null;
	private SashForm sashForm = null;
	private TreeEditor workspaceTreeEditor = null;
	private Tree workspaceTree = null;
	private Composite rightComposite = null;
	private Composite leftComposite = null;
	private GeneEditor setTextArea = null;
	private Menu submenu1 = null;

	private ProjectSettingsComposite settingsComposite;

	private Composite emptyComposite = null;

	private Text statusText = null;
	private ProgressBar statusProgressBar = null;

	private StackLayout rightStackedLayout;

	/* ToolBar */
	private ToolBar toolbar = null;
	private Combo methodCombo = null;
	private Combo mtcCombo = null;
	private ToolItem newPopulationToolItem = null;
	private ToolItem newStudyToolItem = null;
	private ToolItem removeToolItem = null;
	private ToolItem analyzeToolItem = null;
	private ToolItem newProjectToolItem = null;
	private ToolItem similarityToolItem = null;

	/** Action to be called when a new method is selected */
	private List<ISimpleAction> methodAction = new LinkedList<ISimpleAction>();

	/* Menu Items */
	private MenuItem preferencesMenuItem;
	private MenuItem helpContentsMenuItem;
	private MenuItem workSetsMenuItem;
	private MenuItem fileCacheMenutItem;
	private MenuItem helpAboutMenuItem;
	private MenuItem exportMenuItem;
	private MenuItem logMenuItem;
	private MenuItem newProjectItem;
	private MenuItem newPopulationItem;
	private MenuItem newStudyItem;

	/* String constants */
	private final String methodToolTip = "Specifies the calculation method which is used to get the raw p-value.";
	private final String mtcToolTip = "Specifies the multiple test correction procedure which is used to adjust the p-value.";

	private final String populationTip = "The population set. It consists of all instances that\ncan be selected in an experiment. ";
	private final String studyTip = "The study set. It consits of instances that have\nbeen selected due to an experiment. Genes that are\nnot included in the population set are added during\nthe calculation.";

	/* Manually added methods */

	/**
	 * Sets the workspace to the given file.
	 *
	 * @param newWorkspaceDirectory
	 * 				must point to a directory.
	 */
	public void setWorkspace(File newWorkspaceDirectory)
	{
		if (!newWorkspaceDirectory.isDirectory())
			throw new IllegalArgumentException();

		workspaceDirectory = newWorkspaceDirectory;

		String [] projects = newWorkspaceDirectory.list(new FilenameFilter()
		{
			public boolean accept(File dir, String name)
			{
				if (name.startsWith(".")) return false;
				return true;
			}
		});
		Arrays.sort(projects,String.CASE_INSENSITIVE_ORDER);

		for (String project : projects)
		{
			if (project.equals(".cache"))
				continue;

			addProject(new File(newWorkspaceDirectory,project));
		}
	}

	/**
	 * Adds a new project.
	 *
	 * @param projectDirectory
	 */
	public void addProject(File projectDirectory)
	{
		addProject(projectDirectory, false);
	}

	/**
	 * Adds a new project.
	 *
	 * @param projectDirectory
	 * @param activate indicate whether the project should be active
	 */
	public void addProject(File projectDirectory, boolean activate)
	{
		TreeItem projectTreeItem = newProjectItem(projectDirectory,projectDirectory.getName());
		TreeItemData tid = getTreeItemData(projectTreeItem);

		for (ItemSet items : tid.project.itemSets())
		{
			if (items.population) newPopItem(projectTreeItem, items);
			else newStudyItem(projectTreeItem, items);
		}

		projectTreeItem.setExpanded(!tid.project.settings.isClosed);

		if (activate)
		{
			workspaceTree.setSelection(projectTreeItem);
			updateGenes();
		}
	}

	/**
	 * Create a new project item using the given name.
	 *
	 * @param directory
	 * @param name
	 * @return
	 */
	private TreeItem newProjectItem(File directory, String name)
	{
		TreeItemData newItemData = new TreeItemData();
		newItemData.project = new Project(directory);
		applyCurrentProjectSettings(newItemData.project);

		TreeItem newItem = new TreeItem(workspaceTree,0);
		newItem.setData(newItemData);
		updateTextOfItem(newItem);
		return newItem;
	}

	/**
	 * Apply currently chosen project settings on the given project.
	 *
	 * @param project
	 */
	private void applyCurrentProjectSettings(Project project)
	{
		ProjectSettings settings = project.settings;

		settings.annotationsFileName = getAssociationsFileString();
		settings.ontologyFileName = getDefinitionFileString();
		settings.mappingFileName = getMappingFileString();
		settings.subontology = getSubontologyString();
		settings.subset = getSubsetString();
	}

	/**
	 * Create a new popitem using the given name and reads in the file
	 * given by name.
	 *
	 * @param param
	 * @param items
	 * @return
	 */
	private TreeItem newPopItem(TreeItem parent, ItemSet items)
	{
		if (getPopulationItem(parent) != null)
			return null;

		Project project = getTreeItemData(parent).project;
		TreeItemData newItemData = new TreeItemData();
		newItemData.project = project;
		newItemData.items = items;

		TreeItem newItem = new TreeItem(parent,0);
		newItem.setData(newItemData);
		updateTextOfItem(newItem);
		return newItem;
	}

	/**
	 * Create a new study set item using the given name. If the given
	 * name exits within in the directory, it is read in.
	 *
	 * @param parent
	 * 				the parent, i.e. where this study set belongs to.
	 *
	 * @param name
	 * 				the name of the study set (equals the filename)
	 * @return
	 */
	private TreeItem newStudyItem(TreeItem parent, ItemSet items)
	{
		Project project = getTreeItemData(parent).project;
		TreeItemData newItemData = new TreeItemData();
		newItemData.project = project;
		newItemData.items = items;

		TreeItem newItem = new TreeItem(parent,0);
		newItem.setData(newItemData);
		updateTextOfItem(newItem);
		return newItem;
	}

	/**
	 * Updates the text of the tree item.
	 *
	 * @param item
	 */
	private void updateTextOfItem(TreeItem item)
	{
		TreeItemData tid = getTreeItemData(item);
		if (isTreeItemProject(item))
		{
			item.setText(tid.project.projectDirectory.getName());
		} else
		{
			/* Workaround to ensure that the item is really redrawn */
			item.setText(tid.items.name + " ");
			item.setText(tid.items.name);
		}
	}

	/**
	 * Tries to rename the given item.
	 *
	 * @param item
	 * @param name
	 * @return
	 */
	private boolean renameItem(TreeItem item, String name)
	{
		TreeItemData tid = getTreeItemData(item);

		if (isTreeItemProject(item))
		{
			tid.project.rename(name);
		}	else
		{
			tid.items.rename(name);
		}
		updateTextOfItem(item);
		return true;
	}

	/**
	 * Removes the given tree item and the associated files and directories
	 *
	 * @param item the item which is going to be removed.
	 */
	private boolean removeItem(TreeItem item)
	{
		TreeItemData tid = getTreeItemData(item);
		if (isTreeItemProject(item))
		{
			if (!tid.project.remove())
				return false;
		} else
		{
			if (!(tid.project.remove(tid.items)))
				return false;
		}
		item.dispose();
		return true;
	}

	/**
	 * @return a list of the names of all projects.
	 */
	public List<String> getProjectNames()
	{
		List<String> l = new LinkedList<String>();
		TreeItem [] tis = workspaceTree.getItems();
		for (TreeItem ti : tis)
		{
			TreeItemData tid = getTreeItemData(ti);
			l.add(tid.project.projectDirectory.getName());
		}
		return l;
	}

	/**
	 * Stores the current genes (context of the setTextArea)
	 * within the current selected StudySet
	 */
	protected void storeGenes()
	{
		if (currentSelectedItem != null)
		{
			TreeItemData tid;

			tid = getTreeItemData(currentSelectedItem);
			if (isTreeItemProject(currentSelectedItem))
			{
				applyCurrentProjectSettings(tid.project);
				tid.project.settings.isClosed = !currentSelectedItem.getExpanded();
				storeProjectSettings(tid);
				return;
			}

			tid.items.entries = setTextArea.getText();
			tid.items.numEntries = setTextArea.getNumberOfEntries();
			tid.items.numKnownEntries = setTextArea.getNumberOfKnownEntries();
			updateTextOfItem(currentSelectedItem);

			try
			{
				/* TODO: Add an explicit saving mechanism */
				File out = new File(tid.project.projectDirectory,tid.items.name);
				BufferedWriter fw = new BufferedWriter(new FileWriter(out));
				fw.write(tid.items.entries);
				fw.close();
			} catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**
	 * Stores the project settings of item represented by the TreeItemData.
	 *
	 * @param tid
	 */
	private void storeProjectSettings(TreeItemData tid)
	{
		if (tid.items != null) return;

		Properties prop = tid.project.settings.toProperties();
		try
		{
			FileOutputStream fos = new FileOutputStream(new File(tid.project.projectDirectory,PROJECT_SETTINGS_NAME));
			prop.storeToXML(fos,"Ontologizer Project File");
			fos.close();
		} catch (FileNotFoundException e)
		{
			e.printStackTrace();
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Returns the project item of the given item.
	 * If item is the project item, item is returned.
	 *
	 * @param item
	 * @return
	 */
	private TreeItem getProjectItem(TreeItem item)
	{
		TreeItem parent = item;
		if (getTreeItemData(parent).items != null)
			parent = parent.getParentItem();
		return parent;
	}

	/**
	 * Returns the population item.
	 *
	 * @param item
	 * @return
	 */
	private TreeItem getPopulationItem(TreeItem item)
	{
		TreeItem [] items = item.getItems();
		for (int i=0;i<items.length;i++)
		{
			if (getTreeItemData(items[i]).items.population)
				return items[i];
		}
		return null;
	}

	private static WorkSet currentWorkSet;

	/**
	 * Update the setTextArea to the genes of the current
	 * selected set.
	 */
	private void updateGenes()
	{
		boolean populationCreateable = false;
		boolean studyCreateable = false;
		boolean ontologizable = false;

		TreeItem[] items = workspaceTree.getSelection();
		if (items != null && items.length>0)
		{
			currentSelectedItem = items[0];

			TreeItem projectItem = getProjectItem(currentSelectedItem);
			if (projectItem != null)
			{
				ProjectSettings settings = getTreeItemData(projectItem).project.settings;

				setAssociationsFileString(settings.annotationsFileName);
				setDefinitonFileString(settings.ontologyFileName);
				setMappingFileString(settings.mappingFileName);
				setSubontology(settings.subontology);
				setSubset(settings.subset);

				final String subontology = settings.subontology;
				final String subset = settings.subset;

				if (currentWorkSet != null) WorkSetLoadThread.releaseDatafiles(currentWorkSet);
				currentWorkSet = settingsComposite.getSelectedWorkset();

				settingsComposite.setRestrictionChoices(null);
				settingsComposite.setConsiderChoices(null);

				updateSettingsCompositeInfoText();

				WorkSetLoadThread.obtainDatafiles(currentWorkSet,
						new Runnable(){
							public void run()
							{
								settingsComposite.getDisplay().asyncExec(new Runnable()
								{
									public void run()
									{
										Ontology graph = WorkSetLoadThread.getGraph(currentWorkSet.getOboPath());
										AssociationContainer assoc = WorkSetLoadThread.getAssociations(currentWorkSet.getAssociationPath());

										if (graph != null)
										{
											String [] subsetChoices = new String[graph.getAvailableSubsets().size()];
											int i=0;
											for (Subset s : graph.getAvailableSubsets())
												subsetChoices[i++] = s.getName();
											settingsComposite.setRestrictionChoices(subsetChoices);
											settingsComposite.setRestriction(subset);

											String [] subontologyChoices = new String[graph.getLevel1Terms().size()];
											i = 0;
											for (Term t : graph.getLevel1Terms())
												subontologyChoices[i++] = t.getName();
											settingsComposite.setConsiderChoices(subontologyChoices);
											settingsComposite.setConsider(subontology);
											settingsComposite.setOntologyErrorString(null);
										} else
										{
											settingsComposite.setRestrictionChoices(new String[]{});
											settingsComposite.setConsiderChoices(new String[]{});
											settingsComposite.setOntologyErrorString("Error in obtaining the definition file.");
										}

										if (assoc == null)
										{
											settingsComposite.setAssociationErrorString("Error in obtaining the association file.");
										} else
										{
											settingsComposite.setEvidences(assoc.getAllEvidenceCodes());
											settingsComposite.setAssociationErrorString(null);
										}
									}
								});
							}});
				if (getPopulationItem(projectItem) == null)
					populationCreateable = true;
			}

			if (isTreeItemProject(currentSelectedItem))
			{
				if (rightStackedLayout.topControl != settingsComposite)
				{
					rightStackedLayout.topControl = settingsComposite;
					settingsComposite.getParent().layout();
				}
			} else
			{
				String genes = getTreeItemData(currentSelectedItem).items.entries;
				if (genes == null) genes = "";

				if (getTreeItemData(currentSelectedItem).items.population)
					setTextArea.setToolTipText(populationTip);
				else setTextArea.setToolTipText(studyTip);

				setTextArea.setText(genes);
				treeItemWhenWorkSetIsChanged = currentSelectedItem;
				setTextArea.setWorkSet(settingsComposite.getSelectedWorkset(),settingsComposite.getMappingFileString());
				if (rightStackedLayout.topControl != rightComposite)
				{
					rightStackedLayout.topControl = rightComposite;
					rightComposite.getParent().layout();
				}
			}

			List<Set> sets = getSetEntriesOfCurrentPopulation();
			if (sets != null && sets.size() > 1)
				ontologizable = true;
			removeToolItem.setEnabled(true);
			studyCreateable = true;
		} else
		{
			currentSelectedItem = null;
			setTextArea.setText("");
			setTextArea.setToolTipText(null);

			setAssociationsFileString("");
			setDefinitonFileString("");
			setMappingFileString("");
			setSubontology("");
			setSubset("");
			settingsComposite.setEvidences(new HashMap<String,Integer>());

			if (rightStackedLayout.topControl != emptyComposite)
			{
				rightStackedLayout.topControl = emptyComposite;
				settingsComposite.getParent().layout();
			}

			removeToolItem.setEnabled(false);
		}

		analyzeToolItem.setEnabled(ontologizable);
		similarityToolItem.setEnabled(ontologizable);
		newPopulationToolItem.setEnabled(populationCreateable);
		newPopulationItem.setEnabled(populationCreateable);

		newStudyToolItem.setEnabled(studyCreateable);
		newStudyItem.setEnabled(studyCreateable);
	}

	/**
	 * Updates the info text within the settings composite.
	 */
	private void updateSettingsCompositeInfoText()
	{
		StringBuilder info = new StringBuilder();

		switch (FileCache.getState(currentWorkSet.getOboPath()))
		{
			case CACHED:
				info.append("Remote definition file was downloaded at " + FileCache.getDownloadTime(currentWorkSet.getOboPath()) + " (<a href=\"ontology\">reload</a>). ");
				break;

			case DOWNLOADING:
				info.append("Remote definition file is being downloaded. ");
				break;

			default:
				break;
		}

		switch (FileCache.getState(currentWorkSet.getAssociationPath()))
		{
			case CACHED:
				info.append("Remote annotation file was downloaded at " + FileCache.getDownloadTime(currentWorkSet.getAssociationPath()) + " (<a href=\"assoc\">reload</a>). ");
				break;

			case DOWNLOADING:
				info.append("Remote annotation file is being downloaded. ");
				break;

			default:
				break;
		}

		settingsComposite.setInfoText(info.toString());
	}

	private String [] split(String str)
	{
		String [] strs = str.split("\n");
		for (int i=0;i<strs.length;i++)
			strs[i] = strs[i].trim();
		return strs;
	}

	/**
	 * @return the entries of the current selected population/study set.
	 */
	public String [] getCurrentSetEntries()
	{
		return split(setTextArea.getText());
	}

	/**
	 * @return the definition file string.
	 */
	public String getDefinitionFileString()
	{
		return settingsComposite.getDefinitionFileString();
	}

	/**
	 * Sets the definition file string.
	 *
	 * @param string
	 */
	public void setDefinitonFileString(String string)
	{
		settingsComposite.setDefinitonFileString(string);
	}

	/**
	 * @return the mapping file string.
	 */
	public String getMappingFileString()
	{
		return settingsComposite.getMappingFileString();
	}

	/**
	 * Sets the mapping file string.
	 *
	 * @param string
	 */
	public void setMappingFileString(String string)
	{
		settingsComposite.setMappingFileString(string);
	}

	/**
	 * @return the association file string.
	 */
	public String getAssociationsFileString()
	{
		return settingsComposite.getAssociationsFileString();
	}

	public void setAssociationsFileString(String string)
	{
		settingsComposite.setAssociationsFileString(string);
	}

	public class Set
	{
		public String [] entries;
		public String name;
	};

	/**
	 * Get all the set entries of the current selected population.
	 * This includes the population which is always the first
	 * entry accessible via the iterator. If no population was given
	 * the set is empty.
	 *
	 * @return might return null if no set is selected
	 */
	public List<Set> getSetEntriesOfCurrentPopulation()
	{
		/* Store current genes */
		storeGenes();

		if (currentSelectedItem != null)
		{
			LinkedList<Set> list = new LinkedList<Set>();

			/* Find proper population and add to the list */
			TreeItem project = getProjectItem(currentSelectedItem);
			TreeItemData projectData = getTreeItemData(project);
			TreeItem pop = getPopulationItem(project);

			if (pop != null)
			{
				String entries = getTreeItemData(pop).items.entries;
				if (entries == null) entries = "";
				Set set = new Set();
				set.name = pop.getText();
				set.entries = split(entries);
				list.add(set);
			} else
			{
				/* Empty population */
				Set set = new Set();
				set.name = projectData.project.projectDirectory.getName();
				set.entries = new String[0];
				list.add(set);
			}

			TreeItem children[] = project.getItems();
			for (int i=0;i<children.length;i++)
			{
				if (getTreeItemData(children[i]).items.population)
					continue;

				String entries = getTreeItemData(children[i]).items.entries;
				if (entries == null) entries = "";
				Set set = new Set();
				set.name = children[i].getText();
				set.entries = split(entries);
				list.add(set);
			}

			return list;
		}
		return null;
	}

	/**
	 * Returns the tree item data of the given tree item.
	 *
	 * @param ti
	 * @return
	 */
	private static TreeItemData getTreeItemData(TreeItem ti)
	{
		return (TreeItemData)ti.getData();
	}

	/**
	 * Return the project settings of the project to which the given ti is
	 * associated.
	 *
	 * @param ti
	 * @return
	 */
	private static Project getTreeItemProject(TreeItem ti)
	{
		return getTreeItemData(ti).project;
	}

	/**
	 * Return whether the given tree item is a population
	 * item.
	 *
	 * @param pop
	 * @return
	 */
	private static boolean isTreeItemPopulation(TreeItem pop)
	{
		if (isTreeItemProject(pop))
		{
			return false;
		}

		return getTreeItemData(pop).items.population;
	}

	/**
	 * Return whether the given tree item is a project
	 * item.
	 *
	 * @param project
	 * @return
	 */
	private static boolean isTreeItemProject(TreeItem project)
	{
		TreeItemData tid = getTreeItemData(project);
		return tid.project != null && tid.items == null;
	}

	/**
	 * Add a new action which is executed when the "Ontologize" button
	 * is pressed.
	 *
	 * @param ba
	 */
	public void addAnalyseAction(final ISimpleAction ba)
	{
		analyzeToolItem.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter()
		{
			public void widgetSelected(org.eclipse.swt.events.SelectionEvent e)
			{
				ba.act();
			}
		});
	}

	/**
	 * Add a new action which is executed when the "Similarity" button
	 * is pressed.
	 *
	 * @param ba
	 */
	public void addSimilarityAction(final ISimpleAction ba)
	{
		similarityToolItem.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				ba.act();
			}
		});
	}

	/**
	 * Add a new action which is executed when the new project menu
	 * item is pressed.
	 *
	 * @param a
	 */
	public void addNewProjectAction(ISimpleAction a)
	{
		addSimpleSelectionAction(newProjectToolItem, a);
		addSimpleSelectionAction(newProjectItem, a);
	}

	/**
	 * Add a new action which is executed on the window's disposal.
	 *
	 * @param a
	 * 			the action
	 */
	public void addDisposeAction(final ISimpleAction a)
	{
		shell.addDisposeListener(new DisposeListener(){
			public void widgetDisposed(DisposeEvent e)
			{
				a.act();
			}});
	}

	/**
	 * Add a new action which is executed on selecting the preferences window
	 * menu item.
	 *
	 * @param a
	 */
	public void addOpenPreferencesAction(final ISimpleAction a)
	{
		preferencesMenuItem.addSelectionListener(new SelectionAdapter()
		{
			public void widgetSelected(SelectionEvent e)
			{
				a.act();
			}
		});
	}

	/**
	 * Add a new action which is executed on selecting the workset window
	 * menu item.
	 *
	 * @param a
	 */
	public void addOpenFileCacheAction(final ISimpleAction a)
	{
		fileCacheMenutItem.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				a.act();
			}
		});
	}


	/**
	 * Add a new action which is executed on selecting the workset window
	 * menu item.
	 *
	 * @param a
	 */
	public void addOpenWorkSetAction(final ISimpleAction a)
	{
		workSetsMenuItem.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				a.act();
			}
		});
	}
	/**
	 * Add a new action which is executed on selecting the log window menu
	 * item.
	 * @param a
	 */
	public void addOpenLogWindowAction(final ISimpleAction a)
	{
		logMenuItem.addSelectionListener(new SelectionAdapter()
		{
			public void widgetSelected(SelectionEvent e)
			{
				a.act();
			}
		});
	}

	/**
	 * Add a new action which is executed on selecting the help contents
	 * menu item.
	 *
	 * @param a
	 */
	public void addOpenHelpContentsAction(final ISimpleAction a)
	{
		helpContentsMenuItem.addSelectionListener(new SelectionAdapter()
		{
			public void widgetSelected(SelectionEvent e)
			{
				a.act();
			}
		});
	}

	/**
	 * Add a new action which is executed on selecting the about
	 * menu item.
	 *
	 * @param a
	 */
	public void addOpenAboutAction(final ISimpleAction a)
	{
		helpAboutMenuItem.addSelectionListener(new SelectionAdapter()
		{
			public void widgetSelected(SelectionEvent e)
			{
				a.act();
			}
		});
	}

	/**
	 * Disable the analyse button.
	 */
	public void disableAnalyseButton()
	{
		analyzeToolItem.setEnabled(false);
	}

	/**
	 * Enable the analyse button.
	 */
	public void enableAnalyseButton()
	{
		analyzeToolItem.setEnabled(true);
	}

	public void appendLog(String str)
	{
		System.out.println(str);
	}

	/**
	 * Sets the currently selected method to the method
	 * with the given name.
	 *
	 * @param string
	 */
	public void setSelectedMethodName(String string)
	{
		String [] items = methodCombo.getItems();
		for (int i=0;i<items.length;i++)
		{
			if (items[i].equalsIgnoreCase(string))
			{
				methodCombo.setText(items[i]);
				break;
			}
		}
	}

	/**
	 * Add a new action that is called when a new method is selected.
	 *
	 * @param action
	 */
	public void addMethodAction(ISimpleAction action)
	{
		methodAction.add(action);
	}

	/**
	 * @return the name of the currently selected method.
	 */
	public String getSelectedMethodName()
	{
		return methodCombo.getItem(methodCombo.getSelectionIndex());
	}

	public String getSelectedMTCName()
	{
		return mtcCombo.getItem(mtcCombo.getSelectionIndex());
	}

	public void setSelectedMTCName(String string)
	{
		String [] items = mtcCombo.getItems();
		for (int i=0;i<items.length;i++)
		{
			if (items[i].equalsIgnoreCase(string))
			{
				mtcCombo.setText(items[i]);
				break;
			}
		}
	}

	/* Generated methods */

	/**
	 * This method initializes sShell
	 * @param display
	 */
	private void createSShell(Display display)
	{
		shell.setText("Ontologizer");
		shell.setLayout(new FillLayout());
		createComposite();
		shell.pack();

		menuBar = new Menu(shell, SWT.BAR);

		/* Project menu */
		MenuItem submenuItem = new MenuItem(menuBar, SWT.CASCADE);
		submenuItem.setText("Project");
		submenu = new Menu(submenuItem);
		submenuItem.setMenu(submenu);
		MenuItem newMenuItem = new MenuItem(submenu, SWT.CASCADE);
		newMenuItem.setText("New");
		Menu newMenu = new Menu(shell, SWT.DROP_DOWN);
		newMenuItem.setMenu(newMenu);
		newProjectItem = new MenuItem(newMenu, SWT.PUSH);
		newProjectItem.setText("Project...");
		newProjectItem.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				storeGenes();
			}
		});
		new MenuItem(newMenu,SWT.SEPARATOR);
		newPopulationItem = new MenuItem(newMenu, SWT.PUSH);
		newPopulationItem.setText("Population Set");
		newPopulationItem.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				newPopulationAction();
			}
		});
		newStudyItem = new MenuItem(newMenu, SWT.PUSH);
		newStudyItem.setText("Study Set");
		newStudyItem.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				newStudyAction();
			}
		});

		new MenuItem(submenu,SWT.SEPARATOR);
		MenuItem importMenuItem = new MenuItem(submenu, SWT.PUSH);
		importMenuItem.setText("Import...");
		exportMenuItem = new MenuItem(submenu, SWT.PUSH);
		exportMenuItem.setText("Export...");
		new MenuItem(submenu, SWT.SEPARATOR);
		MenuItem quitMenuItem = new MenuItem(submenu, SWT.PUSH);
		quitMenuItem.setText("Quit");

		/* Window sub menu */
		MenuItem windowSubMenuItem = new MenuItem(menuBar, SWT.CASCADE);
		windowSubMenuItem.setText("Window");
		Menu windowSubMenu = new Menu(windowSubMenuItem);
		windowSubMenuItem.setMenu(windowSubMenu);
		fileCacheMenutItem = new MenuItem(windowSubMenu, SWT.PUSH);
		fileCacheMenutItem.setText("File Cache...");
		workSetsMenuItem = new MenuItem(windowSubMenu, SWT.PUSH);
		workSetsMenuItem.setText("File Sets...");
		preferencesMenuItem = new MenuItem(windowSubMenu, SWT.PUSH);
		preferencesMenuItem.setText("Preferences...");
		logMenuItem = new MenuItem(windowSubMenu, SWT.PUSH);
		logMenuItem.setText("Log...");

		/* Help sub menu */
		MenuItem submenuItem1 = new MenuItem(menuBar, SWT.CASCADE);
		submenuItem1.setText("Help");
		submenu1 = new Menu(submenuItem1);
		submenuItem1.setMenu(submenu1);
		helpContentsMenuItem = new MenuItem(submenu1, SWT.PUSH);
		helpContentsMenuItem.setText("Help Contents...");
		new MenuItem(submenu1, SWT.SEPARATOR);
		helpAboutMenuItem = new MenuItem(submenu1, SWT.PUSH);
		helpAboutMenuItem.setText("About Ontologizer...");

		/* Listener */
		importMenuItem.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				FileDialog fileDialog = new FileDialog(shell,SWT.OPEN);
				fileDialog.setFilterExtensions(new String[]{"*.onto","*.*"});
				if (currentImportFileName != null)
				{
					File f = new File(currentImportFileName);
					fileDialog.setFileName(f.getName());
					fileDialog.setFilterPath(f.getParent());
				}

				String zipName = fileDialog.open();
				if (zipName != null)
				{
					try
					{
						ZipFile zipFile = new ZipFile(zipName);
						Enumeration<? extends ZipEntry> entries = zipFile.entries();

						boolean overwrite = false;
						boolean projectExisted = false;

						String projectName = null;

						while (entries.hasMoreElements())
						{
							ZipEntry target = entries.nextElement();
							String targetName = target.getName();
							File f = new File(workspaceDirectory.getAbsolutePath(),target.getName());

							if (target.isDirectory() && projectName == null)
								projectName = target.getName().replace("/","");

							if (f.exists() && !overwrite)
							{
								MessageBox mb = new MessageBox(getShell(),SWT.YES|SWT.NO);
								if (target.isDirectory())
								{
									projectExisted = true;
									mb.setMessage("The project \"" + projectName + "\" already " +
										      "exists. Do you really wish to import the selected project? Note, that this may overwrite " +
										      "existing settings and study sets with the project.");
									if (mb.open() == SWT.NO)
										break;
									overwrite = true;
								} else
								{
									if (targetName.equals(".settings"))
									{
										mb.setMessage("Do you really which to overwrite the settings of the project \"" + projectName + "\"?");
									} else
									{
										mb.setMessage("Do you really which to overwrite study set " + target.getName() + " of project \"" + projectName + "\"?");
									}
									if (mb.open() == SWT.NO)
										continue;
								}
							}
							saveEntry(zipFile,target,workspaceDirectory.getAbsolutePath());
						}

						/* Finally, add the new project */
						if (!projectExisted && projectName != null)
						{
							addProject(new File(workspaceDirectory,projectName));
						}
						currentImportFileName = zipName;
					} catch (IOException e1)
					{
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
			}
		});
		exportMenuItem.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				TreeItem projectItem = getProjectItem(currentSelectedItem);
				if (projectItem == null) return;
				TreeItemData projectItemData = getTreeItemData(projectItem);
				File projectDirectory = projectItemData.project.projectDirectory;

				FileDialog fileDialog = new FileDialog(shell,SWT.SAVE);
				fileDialog.setFilterExtensions(new String[]{"*.onto","*.*"});
				if (currentExportFileName != null)
				{
					File f = new File(currentExportFileName);
					fileDialog.setFilterPath(f.getParent());
				}

				fileDialog.setFileName(projectItemData.project.projectDirectory.getName() + ".onto");

				String newName = fileDialog.open();
				if (newName != null)
				{
					try
					{
						exportProject(projectDirectory, newName);
					} catch (Exception e1)
					{
						MessageBox mb = new MessageBox(getShell());
						mb.setMessage(e1.getLocalizedMessage());
						mb.setText("Error");
						mb.open();
					}
					currentExportFileName = newName;
				}
			}
		});
		quitMenuItem.addSelectionListener(new SelectionAdapter()
		{
			public void widgetSelected(SelectionEvent e)
			{
				shell.dispose();
			}
		});

		shell.setMenuBar(menuBar);
	}
	/**
	 * This method initializes composite
	 *
	 */
	private void createComposite()
	{
		Composite mainComposite = new Composite(shell, SWT.NONE);
		GridLayout gl = SWTUtil.newEmptyMarginGridLayout(1);
		gl.marginTop = 3;
		gl.marginBottom = 3;
		mainComposite.setLayout(gl);

		createToolBar(mainComposite);

		composite = new Composite(mainComposite, SWT.NONE);
		gl = new GridLayout();
		gl.marginHeight = 0;
		gl.marginTop = 2;

		composite.setLayout(gl);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = shell.getDisplay().getClientArea().height / 2;
		composite.setLayoutData(gd);
		createSashForm();

		Composite statusComp = new Composite(mainComposite,0);
		statusComp.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL|GridData.FILL_HORIZONTAL));
		statusComp.setLayout(new GridLayout(2,false));

		statusText = new Text(statusComp, SWT.READ_ONLY);
		gd = new GridData(GridData.GRAB_HORIZONTAL|GridData.FILL_HORIZONTAL);
		gd.horizontalIndent = 2;
		statusText.setLayoutData(gd);
		statusText.setBackground(shell.getBackground());

		statusProgressBar = new ProgressBar(statusComp,0);
		gd = new GridData();
		gd.widthHint = 150;
		statusProgressBar.setLayoutData(gd);
		statusProgressBar.setVisible(false);
	}

	/**
	 * Constructor.
	 *
	 * @param display
	 */
	public MainWindow(final Display display)
	{
		super(display);

		createSShell(display);

		workspaceTree.setFocus();

		/* Update states */
		updateGenes();

		/**
		 * Add file cache update callbacks.
		 */
		class UpdateSettingsCompositeInfoText implements Runnable
		{
			private String url;

			public UpdateSettingsCompositeInfoText(String url)
			{
				this.url = url;
			}

			@Override
			public void run()
			{
				if (shell.isDisposed())
					return;

				if (currentWorkSet == null)
					return;

				if (!currentWorkSet.getOboPath().equals(url) && !currentWorkSet.getAssociationPath().equals(url))
					return;

				updateSettingsCompositeInfoText();
			}
		}

		FileCache.addUpdateCallback(
				new FileCacheUpdateCallback()
				{
					@Override
					public void update(String url)
					{
						display.asyncExec(new UpdateSettingsCompositeInfoText(url));
					}

					@Override
					public void exception(Exception exception, String url)
					{
						display.asyncExec(new UpdateSettingsCompositeInfoText(url));
					}
				});
	}

	public Shell getShell()
	{
		return shell;
	}

	/**
	 *
	 * @param parent
	 */
	private void createToolBar(Composite parent)
	{
		toolbar = new ToolBar(parent,SWT.FLAT);
		newProjectToolItem = new ToolItem(toolbar,0);
		newProjectToolItem.setText("New Project");
		newProjectToolItem.setImage(Images.loadImage("projects.png"));
		newProjectToolItem.addSelectionListener(new SelectionAdapter()
		{
			public void widgetSelected(SelectionEvent e)
			{
				storeGenes();
			}
		});

		newPopulationToolItem = new ToolItem(toolbar,0);
		newPopulationToolItem.setText("New Population");
		newPopulationToolItem.setImage(Images.loadImage("newpop.png"));
		newPopulationToolItem.addSelectionListener(new SelectionAdapter()
		{
			public void widgetSelected(SelectionEvent e)
			{
				newPopulationAction();
			}
		});

		newStudyToolItem = new ToolItem(toolbar,0);
		newStudyToolItem.setText("New Study");
		newStudyToolItem.setImage(Images.loadImage("newstudy.png"));
		newStudyToolItem.addSelectionListener(new SelectionAdapter()
		{
			public void widgetSelected(SelectionEvent e)
			{
				newStudyAction();
			}
		});

		removeToolItem = new ToolItem(toolbar,0);
		removeToolItem.setText("Remove");
		removeToolItem.setImage(Images.loadImage("delete_obj.gif"));
		removeToolItem.addSelectionListener(new SelectionAdapter()
		{
			public void widgetSelected(SelectionEvent e)
			{
				TreeItem[] items = workspaceTree.getSelection();

				if (items != null && items.length>0)
					removeItem(items[0]);
				updateGenes();
			}
		});

		new ToolItem(toolbar,SWT.SEPARATOR);
		similarityToolItem = new ToolItem(toolbar,0);
		similarityToolItem.setText("Similarity");
		similarityToolItem.setImage(Images.loadImage("sim.png"));
		similarityToolItem.setToolTipText("Calculates the Semantic Similarity");

		new ToolItem(toolbar,SWT.SEPARATOR);

		analyzeToolItem = new ToolItem(toolbar,0);
		analyzeToolItem.setText("Ontologize");
		analyzeToolItem.setImage(Images.loadImage("ontologize.png"));

		new ToolItem(toolbar,SWT.SEPARATOR);

		ToolItem methodToolItem = new ToolItem(toolbar,SWT.SEPARATOR);
		methodCombo = new Combo(toolbar, SWT.READ_ONLY);
		methodCombo.setItems(CalculationRegistry.getAllRegistered());
		methodCombo.setText(CalculationRegistry.getDefault().getName());
		methodCombo.setToolTipText(methodToolTip);
		methodCombo.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e) {
				for (ISimpleAction act : methodAction)
					act.act();
			}
		});
		methodCombo.pack();
		methodToolItem.setWidth(methodCombo.getSize().x);
		methodToolItem.setControl(methodCombo);

		new ToolItem(toolbar,SWT.SEPARATOR);

		ToolItem mtcToolItem = new ToolItem(toolbar,SWT.SEPARATOR);
		mtcCombo = new Combo(toolbar, SWT.READ_ONLY);
		mtcCombo.setItems(TestCorrectionRegistry.getRegisteredCorrections());
		mtcCombo.setText(TestCorrectionRegistry.getDefault().getName());
		mtcCombo.setToolTipText(mtcToolTip);
		mtcCombo.pack();
		mtcToolItem.setWidth(Math.min(mtcCombo.getSize().x,200));
		mtcToolItem.setControl(mtcCombo);
	}

	/**
	 * This method initializes sashForm.
	 */
	private void createSashForm()
	{
		sashForm = new SashForm(composite, SWT.NONE);
		sashForm.setLayoutData(new GridData(GridData.FILL_BOTH));
		createLeftComposite();
		createRightGroup();
		sashForm.setWeights(new int[]{2,3});
	}

	/**
	 * This method initializes right composite which displays the
	 * details of the current selection.
	 */
	private void createRightGroup()
	{
		final CTabFolder detailsFolder = new CTabFolder(sashForm,SWT.BORDER);
		detailsFolder.setSingle(true);
		detailsFolder.setMaximizeVisible(true);
		detailsFolder.setSelectionBackground(leftComposite.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
		detailsFolder.setLayoutData(new GridData(GridData.FILL_BOTH));

		/* First details page */
		CTabItem genesCTabItem = new CTabItem(detailsFolder,0);
		genesCTabItem.setText("Currently Selected Set");
		detailsFolder.setSelection(0);

		Composite stackComposite = new Composite(detailsFolder, SWT.NONE);
		rightStackedLayout = new StackLayout();
		stackComposite.setLayout(rightStackedLayout);
		genesCTabItem.setControl(stackComposite);

		rightComposite = new Composite(stackComposite, SWT.NONE);
		rightComposite.setLayout(SWTUtil.newEmptyMarginGridLayout(1));
		rightStackedLayout.topControl = rightComposite;

		setTextArea = new GeneEditor(rightComposite, 0);
		setTextArea.setLayoutData(new GridData(GridData.FILL_BOTH));
		setTextArea.setToolTipText("");
		setTextArea.addNewNameListener(new GeneEditor.INewNameListener()
		{
			public void newName(String name)
			{
				if (currentSelectedItem != null && !isTreeItemPopulation(currentSelectedItem))
				{
					renameItem(currentSelectedItem,name.replaceFirst("\\..*",""));
				}
			}
		});
		setTextArea.addDatafilesLoadedListener(new ISimpleAction()
		{
			public void act()
			{
				/* Update the number of known entries of the current selection, if
				 * the current selection is still the same as when the workset was changed.
				 */
				if (currentSelectedItem != null && currentSelectedItem == treeItemWhenWorkSetIsChanged)
				{
					if (isTreeItemProject(currentSelectedItem))
					{
						TreeItemData tid = getTreeItemData(currentSelectedItem);
						if (tid.items.numKnownEntries == -1)
						{
							tid.items.numKnownEntries = setTextArea.getNumberOfKnownEntries();
							updateTextOfItem(currentSelectedItem);
						}
					}
				}
			}
		});


		/* Second details page */
		GridData gridData10 = new org.eclipse.swt.layout.GridData();
		gridData10.grabExcessHorizontalSpace = true;
		gridData10.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gridData10.verticalAlignment = org.eclipse.swt.layout.GridData.CENTER;
		GridData settingsSeparatorLineGridData = new GridData();
		settingsSeparatorLineGridData.grabExcessHorizontalSpace = true;
		settingsSeparatorLineGridData.horizontalAlignment = GridData.FILL;
		settingsSeparatorLineGridData.horizontalSpan = 3;

		emptyComposite = new Composite(stackComposite, SWT.NONE);

		settingsComposite = new ProjectSettingsComposite(stackComposite, SWT.NONE);
		settingsComposite.setLayoutData(gridData10);

		/* TODO: Move the controller logic into Ontologizer class */
		settingsComposite.addOntologyChangedAction(new ISimpleAction()
		{
			public void act() {
				storeGenes();
				updateGenes();
			}
		});
		settingsComposite.addAssociationChangedAction(new ISimpleAction()
		{
			public void act() {
				storeGenes();
				updateGenes();
			}
		});
		settingsComposite.addInfoTextClickListener(new InfoTextClickListener()
		{
			@Override
			public void click(String href)
			{
				try
				{
					if (href.equals("ontology"))
					{
						String url = getDefinitionFileString();
						FileCache.invalidate(url);
						FileCache.open(url);
					} else if (href.equals("assoc"))
					{
						String url = getAssociationsFileString();
						FileCache.invalidate(url);
						FileCache.open(url);
					}
				} catch(IOException e)
				{
					logger.log(Level.FINE, "Exception after click", e);
				}
				updateSettingsCompositeInfoText();
			}
		});
	}

	/**
	 * This method initializes left composite.
	 */
	private void createLeftComposite()
	{
		GridLayout gridLayout2 = new GridLayout();
		gridLayout2.marginWidth = 0;
		gridLayout2.marginHeight = 0;
		gridLayout2.numColumns = 1;
		gridLayout2.verticalSpacing = 5;
		gridLayout2.horizontalSpacing = 5;
		leftComposite = new Composite(sashForm, SWT.NONE);
		leftComposite.setLayout(gridLayout2);
		createWorkspaceGroup();
	}

	/**
	 * This method initializes workspace composite.
	 */
	private void createWorkspaceGroup()
	{
		/* Workspace Composite */
		final CTabFolder workspaceFolder = new CTabFolder(leftComposite,SWT.BORDER);
		workspaceFolder.setSingle(true);
		workspaceFolder.setMaximizeVisible(true);
		workspaceFolder.setSelectionBackground(leftComposite.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
		workspaceFolder.setLayoutData(new GridData(GridData.FILL_BOTH));

		CTabItem workspaceCTabItem = new CTabItem(workspaceFolder,0);
		workspaceCTabItem.setText("Workspace");
		workspaceFolder.setSelection(0);

		Composite workspaceComposite = new Composite(workspaceFolder,0);
		workspaceComposite.setLayout(SWTUtil.newEmptyMarginGridLayout(1));
		workspaceCTabItem.setControl(workspaceComposite);

		workspaceTree = new Tree(workspaceComposite, SWT.BORDER);
		workspaceTree.setLayoutData(new GridData(GridData.FILL_BOTH));
		workspaceTree.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(org.eclipse.swt.events.SelectionEvent e)
			{
				storeGenes();
				updateGenes();
			}
		});
		workspaceTree.addTreeListener(new TreeListener()
		{
			public void treeExpanded(TreeEvent e)
			{
				TreeItem ti = (TreeItem)e.item;
				TreeItemData tid = getTreeItemData(ti);
				if (isTreeItemProject(ti))
				{
					tid.project.settings.isClosed = false;
					storeProjectSettings(tid);
				}
			}

			public void treeCollapsed(TreeEvent e)
			{
				TreeItem ti = (TreeItem)e.item;
				TreeItemData tid = getTreeItemData((TreeItem) e.item);
				if (isTreeItemProject(ti))
				{
					tid.project.settings.isClosed = true;
					storeProjectSettings(tid);
				}
			}
		});
		workspaceTree.addListener(SWT.PaintItem, new Listener()
		{
			public void handleEvent(Event event) {
				TreeItem item = (TreeItem)event.item;
				TreeItemData tid = getTreeItemData(item);

				if (!isTreeItemProject(item))
				{
					int itemHeight = workspaceTree.getItemHeight();
					int x = event.x;
					int y = event.y + (itemHeight - event.gc.getFontMetrics().getHeight())/2;

					event.gc.setForeground(workspaceTree.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));
					if (tid.items.numKnownEntries != -1)
					{
						event.gc.drawText(tid.items.numKnownEntries + "/" + tid.items.numEntries, x + event.width + 5, y, true);
					} else event.gc.drawText(tid.items.numEntries + "", x + event.width + 5, y, true);
				}
			}
		});

		workspaceTreeEditor = new TreeEditor(workspaceTree);
		workspaceTreeEditor.grabHorizontal = true;
		workspaceTreeEditor.horizontalAlignment = SWT.LEFT;
		workspaceTreeEditor.minimumWidth = 50;
		workspaceTree.addMouseListener(new MouseAdapter(){
			public void mouseDoubleClick(MouseEvent ev)
			{
				/* Clean up any previous editor control */
				Control oldEditor = workspaceTreeEditor.getEditor();
				if (oldEditor != null) oldEditor.dispose();

				/* Identify the selected row */
				TreeItem[] items = workspaceTree.getSelection();
				if (items.length == 0) return;

				/* The control that will be the editor must be a child of the Tree */
				final Text text = new Text(workspaceTree, SWT.NONE);
				final TreeItem item = items[0];

				TreeItemData tid = getTreeItemData(item);

				if (isTreeItemProject(item)) text.setText(tid.project.projectDirectory.getName());
				else text.setText(tid.items.name);

				text.addFocusListener(new FocusAdapter(){
					public void focusLost(FocusEvent ev)
					{
						renameItem(item,text.getText());
						text.dispose();
					}});
				text.addTraverseListener(new TraverseListener(){
					public void keyTraversed(TraverseEvent ev)
					{
						switch (ev.detail)
						{
							case	SWT.TRAVERSE_RETURN:
									renameItem(item,text.getText());
									/* FALL THROUGH */
							case	SWT.TRAVERSE_ESCAPE:
									text.dispose();
									ev.doit = false;
									break;
						}
					}});
				text.selectAll();
				text.setFocus();

				workspaceTreeEditor.setEditor(text, items[0]);
			}
		});
	}

	/**
	 * Save entry.
	 *
	 * @param zf
	 * @param target
	 * @param dest
	 *
	 * @throws ZipException
	 * @throws IOException
	 */
	public static void saveEntry(ZipFile zf, ZipEntry target, String dest ) throws ZipException,IOException
    {
		File file = new File(dest, target.getName());
		if (target.isDirectory())
			file.mkdirs();
		else
		{
			InputStream is = zf.getInputStream(target);
			BufferedInputStream bis = new BufferedInputStream(is);
//			new File(dest,file.getParent()).mkdirs();
			FileOutputStream fos = new FileOutputStream( file );
			BufferedOutputStream bos = new BufferedOutputStream(fos);
			final int EOF = -1;
			for ( int c; ( c = bis.read() ) != EOF; )
				bos.write( (byte)c );
			bos.close();
			fos.close();
		}
    }

	/**
	 * Sets the status text.
	 *
	 * @param txt
	 */
	public void setStatusText(String txt)
	{
		statusText.setText(txt);
	}

	public void updateWorkSetList(WorkSetList wsl)
	{
		settingsComposite.updateWorkSetList(wsl);
	}

	public void initProgressBar(int max)
	{
		statusProgressBar.setMaximum(max);
	}

	public void updateProgressBar(int current)
	{
		statusProgressBar.setSelection(current);
	}

	public void showProgressBar()
	{
		statusProgressBar.setVisible(true);
	}

	public void hideProgressBar()
	{
		statusProgressBar.setVisible(false);
	}

	public String getSelectedWorkSet()
	{
		return settingsComposite.getSelectedWorksetName();
	}

	private void newPopulationAction()
	{
		TreeItem[] items = workspaceTree.getSelection();
		if (items != null && items.length>0)
		{
			storeGenes();

			/* Find proper parent which must be a project */
			TreeItem parent = getProjectItem(items[0]);
			ItemSet pop = getTreeItemProject(parent).newPopulation();
			if (pop != null)
			{
				TreeItem newPopItem = newPopItem(parent,pop);
				workspaceTree.setSelection(new TreeItem[]{newPopItem});
				parent.setExpanded(true);

				updateGenes();
			}
		}
	}

	private void newStudyAction()
	{
		TreeItem[] items = workspaceTree.getSelection();
		if (items != null && items.length>0)
		{
			storeGenes();

			/* Find proper parent which must be a project */
			TreeItem parent = getProjectItem(items[0]);
			ItemSet study = getTreeItemProject(parent).newStudySet();

			TreeItem newStudyItem = newStudyItem(parent,study);
			workspaceTree.setSelection(new TreeItem[]{newStudyItem});
			parent.setExpanded(true);

			updateGenes();
		}
	}

	/**
	 * @return the selected working set.
	 */
	public WorkSet getSelectedWorkingSet()
	{
		return settingsComposite.getSelectedWorkset();
	}

	/**
	 * @return the currently selected subontology.
	 */
	public String getSubontologyString()
	{
		return settingsComposite.getSubontologyString();
	}

	/**
	 * Set the selected subset ontology string.
	 *
	 * @param subontology
	 */
	public void setSubontology(String subontology)
	{
		settingsComposite.setConsider(subontology);
	}

	public void setSubset(String subset)
	{
		settingsComposite.setRestriction(subset);
	}

	public String getSubsetString()
	{
		return settingsComposite.getSubsetString();
	}

	/**
	 * Sets whether the MTC selection is enabled.
	 *
	 * @param supportsTestCorrection
	 */
	public void setMTCEnabled(boolean supportsTestCorrection)
	{
		mtcCombo.setEnabled(supportsTestCorrection);
	}

	/**
	 * @return the currently selected evidences.
	 */
	public Collection<String> getCheckedEvidences()
	{
		return settingsComposite.getCheckedEvidences();
	}

	/**
	 * Export the project of the given project directory into
	 * a zip file called archiveName.
	 *
	 * @param projectDirectory
	 * @param archiveName
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private void exportProject(File projectDirectory, String archiveName) throws FileNotFoundException, IOException
	{
		/* Write out a zip archive containing all the data of the project */
		String projectName = projectDirectory.getName();
		ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(archiveName));
		ZipEntry entry = new ZipEntry(projectName + "/");
		zip.putNextEntry(entry);
		zip.closeEntry();

		byte [] buffer = new byte[4096];

		String [] names = projectDirectory.list();
		for (String name : names)
		{
			File f = new File(projectDirectory,name);
			FileInputStream in = new FileInputStream(f);

			try
			{
				/* Add zip entry to the output stream */
				zip.putNextEntry(new ZipEntry(projectName + "/" + name));
				int len;
				while ((len = in.read(buffer)) > 0)
					zip.write(buffer, 0, len);
				zip.closeEntry();
			} finally
			{
				in.close();
			}
		}

		zip.close();
	}
}
