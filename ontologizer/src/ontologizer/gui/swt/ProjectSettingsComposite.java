/*
 * Created on 19.11.2007
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ontologizer.gui.swt;

import ontologizer.gui.swt.support.FileGridCompositeWidgets;
import ontologizer.worksets.WorkSet;
import ontologizer.worksets.WorkSetList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

/**
 * The Composite of project settings.
 *
 * TODO: Add a ProjectSettings class
 *
 * @author Sebastian Bauer
 */
public class ProjectSettingsComposite extends Composite
{
	private Combo workSetCombo = null;
	private FileGridCompositeWidgets ontologyFileGridCompositeWidgets = null;
	private FileGridCompositeWidgets assocFileGridCompositeWidgets = null;
	private FileGridCompositeWidgets mappingFileGridCompositeWidgets = null;
	private Combo restrictToCombo;
	private Button mappingCheckBox;

	private WorkSetList wsl;

	public ProjectSettingsComposite(Composite parent, int style)
	{
		this(parent,style,true);
	}

	public ProjectSettingsComposite(Composite parent, int style, boolean mapping)
	{
		super(parent, style);

		wsl = new WorkSetList();

		this.setLayout(new GridLayout(3,false));

		Label workSetLabel = new Label(this,0);
		workSetLabel.setText("File Set");
		workSetLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

		workSetCombo = new Combo(this,SWT.BORDER);
		workSetCombo.setToolTipText("Choose files from predefined file sets.");
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		workSetCombo.setLayoutData(gd);

		ontologyFileGridCompositeWidgets = new FileGridCompositeWidgets(this);
		ontologyFileGridCompositeWidgets.setLabel("Ontology");
		ontologyFileGridCompositeWidgets.setToolTipText("Specifies the ontology file (OBO file format) which defines the GO terms and their structure.");
		ontologyFileGridCompositeWidgets.setFilterExtensions(new String[]{"*.obo","*.*"});
		ontologyFileGridCompositeWidgets.setFilterNames(new String[]{"OBO File","All files"});

		Label restrictLabel = new Label(this,0);
		restrictLabel.setText("Restrict to");
		restrictLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		restrictToCombo = new Combo(this,SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		restrictToCombo.setLayoutData(gd);
		restrictToCombo.setEnabled(false);

		assocFileGridCompositeWidgets = new FileGridCompositeWidgets(this);
		assocFileGridCompositeWidgets.setLabel("Annotations");
		assocFileGridCompositeWidgets.setToolTipText("Specifies the annotation (association) file, which assigns GO terms to the names of the gene products.");
		assocFileGridCompositeWidgets.setFilterExtensions(new String[]{"gene_association.*","*.csv","*.*"});
		assocFileGridCompositeWidgets.setFilterNames(new String[]{"Association File","Affymetrix","All files"});

		if (mapping)
		{
			new Label(this,0);

			mappingCheckBox = new Button(this,SWT.CHECK);
			mappingCheckBox.setText("Use Mapping");
			gd = new GridData();
			gd.horizontalSpan = 2;
			mappingCheckBox.setLayoutData(gd);
			mappingCheckBox.addSelectionListener(new SelectionAdapter()
			{
				@Override
				public void widgetSelected(SelectionEvent e)
				{
					updateMappingEnabled();
				}
			});

			mappingFileGridCompositeWidgets = new FileGridCompositeWidgets(this);
			mappingFileGridCompositeWidgets.setLabel("Mapping");
			mappingFileGridCompositeWidgets.setToolTipText("Specifies an additional mapping file in which each line consits of a single name mapping. The name of the first column is mapped to the name of the second column before the annotation process begins. Columns should be tab-separated.");
			mappingFileGridCompositeWidgets.setFilterExtensions(new String[]{"*.*"});
			mappingFileGridCompositeWidgets.setFilterNames(new String[]{"All files"});


			updateMappingEnabled();
		}

		/* If a new work set has been selected */
		workSetCombo.addSelectionListener(new SelectionAdapter()
		{
			public void widgetSelected(SelectionEvent e)
			{
				String currentWorkSet = getSelectedWorksetName();
				if (wsl != null && currentWorkSet.length() > 0)
				{
					WorkSet ws = wsl.get(currentWorkSet);
					if (ws != null)
					{
						setAssociationsFileString(ws.getAssociationPath());
						setDefinitonFileString(ws.getOboPath());
					}
				}
			}
/*			public void act()
			{
			}*/
		});
	}

	/**
	 * Updates the enable status of the mapping widgets in accordance to
	 * the current state of the checkbox.
	 */
	protected void updateMappingEnabled()
	{
		mappingFileGridCompositeWidgets.setEnabled(mappingCheckBox.getSelection());
	}

	public String getDefinitionFileString()
	{
		return ontologyFileGridCompositeWidgets.getPath();
	}

	/**
	 * Sets the definition file string.
	 *
	 * @param string
	 */
	public void setDefinitonFileString(String string)
	{
		ontologyFileGridCompositeWidgets.setPath(string!=null?string:"");
	}

	/**
	 * Retunrs the mapping file string.
	 *
	 * @return
	 */
	public String getMappingFileString()
	{
		if (!mappingCheckBox.getSelection())
			return "";
		return mappingFileGridCompositeWidgets.getPath();
	}

	/**
	 * Sets the mapping file string.
	 *
	 * @param string
	 */
	public void setMappingFileString(String string)
	{
		string = string!=null?string:"";
		mappingFileGridCompositeWidgets.setPath(string);

		if (string.length()==0)
			mappingCheckBox.setSelection(false);
		else
			mappingCheckBox.setSelection(true);

		updateMappingEnabled();
	}

	/**
	 * Returns the association file string.
	 *
	 * @return
	 */
	public String getAssociationsFileString()
	{
		return assocFileGridCompositeWidgets.getPath();
	}

	public void setAssociationsFileString(String string)
	{
		assocFileGridCompositeWidgets.setPath(string!=null?string:"");
	}

	public void updateWorkSetList(WorkSetList wsl)
	{
		workSetCombo.removeAll();
		this.wsl.clear();

		for (WorkSet ws : wsl)
		{
			this.wsl.add(ws.clone());
			workSetCombo.add(ws.getName());
		}
	}

	public String getSelectedWorksetName()
	{
		return workSetCombo.getText();
	}

	public WorkSet getSelectedWorkset()
	{
		WorkSet ws = new WorkSet(workSetCombo.getText());
		ws.setAssociationPath(assocFileGridCompositeWidgets.getPath());
		ws.setOboPath(ontologyFileGridCompositeWidgets.getPath());
		return ws;
	}
}
