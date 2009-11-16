/*
 * Created on 19.11.2007
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ontologizer.gui.swt;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import ontologizer.gui.swt.support.SWTUtil;
import ontologizer.worksets.WorkSet;
import ontologizer.worksets.WorkSetList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * Wizard used to create a new project.
 * 
 * @author Sebastian Bauer
 */
public class NewProjectWizard extends WizardWindow
{
	private static class NewStudySet
	{
		public String name;
		public String contents;
	}
	

	private Text projectNameText;

	private ProjectSettingsComposite projectSettingsComposite;
	private GeneEditor populationEditor;

	private GeneEditor studyEditor;
	private Text studyNameText;
	private Label studyNameLabel;

	private List<NewStudySet> studySetList = new LinkedList<NewStudySet>();
	
	public NewProjectWizard(Display display)
	{
		super(display);
		
		shell.setText("Ontologizer - New Project Wizard");
		shell.pack();
		
		if (shell.getClientArea().height < 400)
			shell.setSize(shell.getClientArea().width,400);
	}

	public void open(WorkSetList wsl)
	{
		studySetList.clear();
		projectSettingsComposite.updateWorkSetList(wsl);
		super.open();
	}

	@Override
	protected void addPages(Composite parent)
	{
		/* First page */
		Composite first = new Composite(parent,0);
		first.setLayout(new GridLayout(2,false));
		
		Label l = new Label(first,0);
		l.setText("Project Name");
		
		projectNameText = new Text(first,SWT.BORDER);
		projectNameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL|GridData.GRAB_HORIZONTAL));
		projectNameText.addModifyListener(new ModifyListener()
		{
			public void modifyText(ModifyEvent e)
			{
				verifyProjectName();
			}
		});
		
		addPage(new SinglePage(first,"Welcome! This wizard guides you through the creation of a new Ontologizer project.\n"+
				                     "First, please specifiy the name of the new project. Then press \"Next\" to proceed to the next page."));

		/* Second page */
		Composite second = new Composite(parent,0);

		second.setLayout(new GridLayout(1,false));
		projectSettingsComposite = new ProjectSettingsComposite(second,0,false);
		projectSettingsComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL|GridData.HORIZONTAL_ALIGN_FILL));
		PageCallback secondCallback = new PageCallback()
		{
			public boolean completed()
			{
				WorkSet ws = projectSettingsComposite.getSelectedWorkset();
				populationEditor.setWorkSet(ws);
				studyEditor.setWorkSet(ws);
				return true;
			}
		};
		
		addPage(new SinglePage(second, "Now please select a set of ontology and association files. "+
				                       "An ontology file contains the plain definitions of  terms and their mutual relation. " +
				                       "An association file assigns identifiers (e.g., gene names) to the terms and often depends on the organism in question. " +
				                       "You may specify the files manually or via predefined work sets which contain suitable settings of frequently used species.",
				               secondCallback));

		/* Third page */
		Composite third = new Composite(parent,0);
		third.setLayout(new GridLayout());
		populationEditor = new GeneEditor(third,0);
		populationEditor.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		addPage(new SinglePage(third, "Here you are asked to specify the population set of the analysis. "+
				                      "The population set specifies the identifiers for all instances that are the selectable canditates in an experiment. " +
				                      "For instance, an appropriate population set for a downstream microarray analysis consists of all the genes on the microarray."));
		

		/* Fourth page */
		Composite fourth = new Composite(parent,0);
		fourth.setLayout(new GridLayout());
		Composite nameComposite = new Composite(fourth,0);
		nameComposite.setLayout(SWTUtil.newEmptyMarginGridLayout(2));
		nameComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL|GridData.GRAB_HORIZONTAL));
		studyNameLabel = new Label(nameComposite,0);
		studyNameLabel.setText("Study Set Name");
		studyNameText = new Text(nameComposite,SWT.BORDER);
		studyNameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL|GridData.GRAB_HORIZONTAL));
		studyEditor = new GeneEditor(fourth,0);
		studyEditor.setLayoutData(new GridData(GridData.FILL_BOTH));
		studyEditor.addNewNameListener(new GeneEditor.INewNameListener()
		{
			public void newName(String name)
			{
				if (name.endsWith(".txt"))
					name = name.substring(0,name.length()-4);
				studyNameText.setText(name);
			}
		});

		PageCallback fourthCallback = new PageCallback()
		{
			public boolean completed()
			{
				NewStudySet sset = new NewStudySet();
				sset.name = studyNameText.getText();
				sset.contents = studyEditor.getText();
				
				if (currentPage - 3 < studySetList.size())
				{
					studySetList.set(currentPage - 3, sset);
				} else
				{
					studySetList.add(sset);
				}
				
				return true;
			}
		};
		
		addPage(new SinglePage(fourth, "Now please specify the study set. " +
				                       "A study set contains the identifiers for all instances that actually were selected due to the experiment. " +
				                       "For instance, an appropriate study set for a downstream microarray analysis are all the genes that were identfied to be differentially expressed. " +
				                       "Note that the study set should be a subset of the population set, otherwise the population set gets automatically extended during the calculation. " +
				                       "If you wish to specify another study set, please press \"Next\" otherwise \"Finish\".",
				               fourthCallback));

		
	}
	
	/**
	 * Verify the project's name.
	 * 
	 */
	private void verifyProjectName()
	{
		if (currentPage == 0)
		{
			String txt = projectNameText.getText();
			if (txt.length()==0)
				displayError(null);
			else
			{
				if (!Ontologizer.isProjectNameValid(txt))
					displayError("A project with name \"" + txt + "\" already exists.");
				else
					clearError();
			}
		}
	}

	@Override
	protected int getDisplayedPageNumber(int which)
	{
		if (which > 3)
			return 3;
		return which;
	}
	
	@Override
	protected void showPage(int which)
	{
		if (which == 0)
		{
			projectNameText.forceFocus();
		}
		/* This is called when a page is about to be shown. We clear
		 * set up new defaults. */
		if (which > 2)
		{
			int w = which - 3;
			if (w < studySetList.size())
			{
				NewStudySet studySet = studySetList.get(w);
				studyNameText.setText(studySet.name);
				studyEditor.setText(studySet.contents);
			} else
			{
				studyNameText.setText("Study " + (w+1));
				studyEditor.clear();
			}
			
			studyNameLabel.setText("Name of study set " + (w + 1));
			studyNameLabel.getParent().layout();
		}
		super.showPage(which);
	}

	@Override
	protected boolean finish()
	{
		File projectDrawer = new File(Ontologizer.getWorkspace(),projectNameText.getText());
		try
		{
			projectDrawer.mkdirs();
			File populationFile = new File(projectDrawer,"Population");
			PrintWriter pw = new PrintWriter(populationFile);
			pw.write(populationEditor.getText());
			pw.close();
			
			Properties prop = new Properties();
			prop.setProperty("annotationsFileName",projectSettingsComposite.getAssociationsFileString());
			prop.setProperty("ontologyFileName",projectSettingsComposite.getDefinitionFileString());
			FileOutputStream fos = new FileOutputStream(new File(projectDrawer,MainWindow.PROJECT_SETTINGS_NAME));
			prop.storeToXML(fos,"Ontologizer Project File");
			fos.close();

			for (NewStudySet nss : studySetList)
			{
				File studyFile = new File(projectDrawer,nss.name);
				pw = new PrintWriter(studyFile);
				pw.write(nss.contents);
				pw.close();
			}

			Ontologizer.newProject(projectDrawer);
		} catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
			Ontologizer.newProject(projectDrawer);
			return false;
		}
		return true;
	}
	
	@Override
	protected void reset()
	{
		projectNameText.setText("");
		verifyProjectName();
	}
}
