/*
 * Created on 19.11.2007
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ontologizer.gui.swt;

import java.util.ArrayList;

import ontologizer.gui.swt.support.FileGridCompositeWidgets;
import ontologizer.gui.swt.support.SWTUtil;
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

class Expander extends Composite
{
	private Composite control;
	private Button expandButton;

	private boolean visible;

	public Expander(Composite parent, int style)
	{
		super(parent, style);

		this.setLayout(SWTUtil.newEmptyMarginGridLayout(1));

		expandButton = new Button(this,0);
		expandButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				control.setVisible(!control.getVisible());

				visible = control.getVisible();
				updateButtonText();

			}
		});
		updateButtonText();
	}

	public void setText(String text)
	{
		expandButton.setText(text);
	}

	public void setControl(Composite control)
	{
		this.control = control;
		control.setVisible(visible);
		control.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL|GridData.FILL_BOTH));
	}

	private void updateButtonText()
	{
		if (!visible)
			expandButton.setText("Show Advanced Options >>");
		else
			expandButton.setText("<< Hide Advanced Options");
		expandButton.pack(true);
	}

	void setExpandedState(boolean visible)
	{
		this.visible = visible;
		control.setVisible(visible);
		updateButtonText();
	}
}

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
	private Combo subsetCombo;
	private Combo considerCombo;
	private Expander advancedExpander;

	private Button subsetCheckbox;
	private Button considerCheckbox;

	private ArrayList<ISimpleAction> definitionChangedList = new ArrayList<ISimpleAction>();

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

		assocFileGridCompositeWidgets = new FileGridCompositeWidgets(this);
		assocFileGridCompositeWidgets.setLabel("Annotations");
		assocFileGridCompositeWidgets.setToolTipText("Specifies the annotation (association) file, which assigns GO terms to the names of the gene products.");
		assocFileGridCompositeWidgets.setFilterExtensions(new String[]{"gene_association.*","*.csv","*.*"});
		assocFileGridCompositeWidgets.setFilterNames(new String[]{"Association File","Affymetrix","All files"});

/* TODO: Use ExpandableComposite comp of JFace */


		advancedExpander = new Expander(this,0);
		gd = new GridData(GridData.FILL_HORIZONTAL|GridData.GRAB_HORIZONTAL);
		gd.horizontalSpan = 3;
		advancedExpander.setLayoutData(gd);

		Composite mappingComposite = new Composite(advancedExpander, 0);
		mappingComposite.setLayout(SWTUtil.newEmptyMarginGridLayout(3));
		advancedExpander.setControl(mappingComposite);

		subsetCheckbox = new Button(mappingComposite,SWT.CHECK);
		subsetCheckbox.setText("Use Subset of Ontology");
		subsetCheckbox.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		subsetCheckbox.setEnabled(false);
		subsetCheckbox.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				updateSubsetEnabled();
			}
		});

		subsetCombo = new Combo(mappingComposite,SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL|GridData.GRAB_HORIZONTAL);
		gd.horizontalSpan = 2;
		subsetCombo.setLayoutData(gd);
		subsetCombo.setEnabled(false);

		considerCheckbox = new Button(mappingComposite,SWT.CHECK);
		considerCheckbox.setText("Consider Terms from");
		considerCheckbox.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		considerCheckbox.setEnabled(false);
		considerCheckbox.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				updateConsiderEnabled();
			}
		});
		considerCombo = new Combo(mappingComposite,SWT.BORDER);
		considerCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL|GridData.GRAB_HORIZONTAL));
		Label subOntologyLabel = new Label(mappingComposite,0);
		subOntologyLabel.setText("Subontology");
		considerCombo.setEnabled(false);

		if (mapping)
		{
//			new Label(mappingComposite,0);
//			mappingCheckBox = new Button(mappingComposite,SWT.CHECK);
//			mappingCheckBox.setText("Use Mapping");
//			gd = new GridData();
//			gd.horizontalSpan = 2;
//
//			mappingCheckBox.setLayoutData(gd);
//			mappingCheckBox.addSelectionListener(new SelectionAdapter()
//			{
//				@Override
//				public void widgetSelected(SelectionEvent e)
//				{
//					updateMappingEnabled();
//				}
//			});

			mappingFileGridCompositeWidgets = new FileGridCompositeWidgets(mappingComposite,true);
			mappingFileGridCompositeWidgets.setLabel("Mapping");
			mappingFileGridCompositeWidgets.setToolTipText("Specifies an additional mapping file in which each line consits of a single name mapping. The name of the first column is mapped to the name of the second column before the annotation process begins. Columns should be tab-separated.");
			mappingFileGridCompositeWidgets.setFilterExtensions(new String[]{"*.*"});
			mappingFileGridCompositeWidgets.setFilterNames(new String[]{"All files"});

//			updateMappingEnabled();
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
		});
	}

	/**
	 * Updates the enable status of the subset widget in accordance to
	 * the current state of the checkbox.
	 */
	private void updateSubsetEnabled()
	{
		subsetCombo.setEnabled(subsetCheckbox.getSelection() && subsetCombo.getItemCount() > 0);
	}

	/**
	 * Updates the enable status of the consider widget in accordance to
	 * the current state of the checkbox.
	 */
	private void updateConsiderEnabled()
	{
		considerCombo.setEnabled(considerCheckbox.getSelection() && considerCombo.getItemCount() > 0);
	}

	public String getDefinitionFileString()
	{
		return ontologyFileGridCompositeWidgets.getPath();
	}

	/**
	 * Add an action that is invoked if the definition file
	 * has been changed.
	 *
	 * @param act
	 */
	public void addDefinitionChanged(ISimpleAction act)
	{
		definitionChangedList.add(act);
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
	 * Returns the mapping file string.
	 *
	 * @return
	 */
	public String getMappingFileString()
	{
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
	}

	/**
	 * Returns the currently selected subset.
	 *
	 * @return
	 */
	public String getSubsetString()
	{
		if (!subsetCheckbox.getSelection()) return "";
		int idx = subsetCombo.getSelectionIndex();
		if (idx >= 0)
			return subsetCombo.getItems()[idx];
		return subsetCombo.getText();
	}

	/**
	 * Returns the currently selected subontology.
	 * @return
	 */
	public String getSubontologyString()
	{
		if (!considerCheckbox.getSelection()) return "";

		int idx = considerCombo.getSelectionIndex();
		if (idx >= 0)
			return considerCombo.getItem(idx);

		return considerCombo.getText();

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

	public void setRestrictionChoices(String[] choices)
	{
		if (choices == null) choices = new String[0];
		subsetCheckbox.setEnabled(choices.length > 0);
//		subsetCombo.setEnabled(choices.length > 0);
		subsetCombo.setItems(choices);
		updateSubsetEnabled();
	}

	public void setConsiderChoices(String [] choices)
	{
		if (choices == null) choices = new String[0];
		considerCheckbox.setEnabled(choices.length > 0);
		considerCombo.setItems(choices);
		updateConsiderEnabled();
	}

	public void setConsider(String subontology)
	{
		considerCombo.setText(subontology);
		considerCheckbox.setSelection(subontology.length() > 0);
		updateConsiderEnabled();

		if (subontology.length() > 0)
			advancedExpander.setExpandedState(true);
	}

	public void setRestriction(String subset)
	{
		subsetCombo.setText(subset);
		subsetCheckbox.setSelection(subset.length() > 0);
		updateSubsetEnabled();

		if (subset.length() > 0)
			advancedExpander.setExpandedState(true);
	}

	public void setOntologyErrorString(String error)
	{
		ontologyFileGridCompositeWidgets.setErrorString(error);
	}
}
