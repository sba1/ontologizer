package ontologizer.gui.swt.result;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import ontologizer.go.Term;
import ontologizer.gui.swt.ISimpleAction;
import ontologizer.gui.swt.support.SWTUtil;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

/**
 * A class comprising of a text field in which the user may enter
 * a GO term. GO terms can be selected via by using a selection list.
 *
 * @author Sebastian Bauer
 */
public class GOTermFilterSelectionComposite extends Composite
{
	/** Contains the possibly selectable terms */
	private Term [] terms;

	/** Contains the terms which are currently displayed within the suggestion list */
	private ArrayList<Term> suggestionList;

	/** The currently selected term */
	private Term selectedTerm;

	/* GUI Elements */
	private Label subtermFilterLabel;
	private Text subtermFilterText;
	private Button subtermFilterButton;
	private Shell subtermFilterSuggestionShell;
	private Table subtermFilterSuggestionTable;
	private TableColumn subtermFilterSuggestionTableIDColumn;
	private TableColumn subtermFilterSuggestionTableNameColumn;
	private TableColumn subtermFilterSuggestionTableNamespaceColumn;

	/** Action performed upon new term selection */
	private ISimpleAction newTermAction; 

	/**
	 * 
	 * @param parent
	 * @param style
	 */
	public GOTermFilterSelectionComposite(Composite parent, int style)
	{
		super(parent, style);

		terms = new Term[0];

		/* Subterm filter */
		subtermFilterSuggestionShell = new Shell(getShell(),SWT.TOOL|SWT.ON_TOP);
		subtermFilterSuggestionShell.setLayout(new FillLayout());
		subtermFilterSuggestionTable = new Table(subtermFilterSuggestionShell,SWT.BORDER|SWT.VIRTUAL|SWT.FULL_SELECTION);
		subtermFilterSuggestionTableIDColumn = new TableColumn(subtermFilterSuggestionTable,0);
		subtermFilterSuggestionTableIDColumn.setText("GO ID");
		subtermFilterSuggestionTableNamespaceColumn = new TableColumn(subtermFilterSuggestionTable,0);
		subtermFilterSuggestionTableNamespaceColumn.setText("Namespace");
		subtermFilterSuggestionTableNameColumn = new TableColumn(subtermFilterSuggestionTable,0);
		subtermFilterSuggestionTableNameColumn.setText("Name");
		subtermFilterSuggestionTable.addListener(SWT.SetData,new Listener(){
			public void handleEvent(Event event)
			{
				TableItem item = (TableItem)event.item;
				int tableIndex = subtermFilterSuggestionTable.indexOf(item);
	
				item.setText(0,suggestionList.get(tableIndex).getIDAsString());
				item.setText(1,suggestionList.get(tableIndex).getNamespaceAsString());
				item.setText(2,suggestionList.get(tableIndex).getName());
			}});
	
		setLayout(SWTUtil.newEmptyMarginGridLayout(3));
		subtermFilterLabel = new Label(this,SWT.NONE);
		subtermFilterLabel.setText("Display terms emanating from");
		subtermFilterText = new Text(this,SWT.BORDER);
		subtermFilterText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL|GridData.GRAB_HORIZONTAL));
		subtermFilterText.setText("Gene Ontology");
		subtermFilterText.addFocusListener(new FocusAdapter(){
			public void focusLost(FocusEvent e)
			{
				/* Make the suggestion list invisible */
				subtermFilterSuggestionShell.setVisible(false);
			}});
		subtermFilterText.addKeyListener(new KeyAdapter(){
			public void keyPressed(KeyEvent e)
			{
				int i;
				int idx;
	
				switch (e.keyCode)
				{
					case	SWT.ARROW_DOWN:
							idx = subtermFilterSuggestionTable.getSelectionIndex();
							idx++;
							if (idx == subtermFilterSuggestionTable.getItemCount()) idx = 0;
							subtermFilterSuggestionTable.setSelection(idx);
							break;
	
					case	SWT.ARROW_UP:
							idx = subtermFilterSuggestionTable.getSelectionIndex();
							if (idx == 0) idx = subtermFilterSuggestionTable.getItemCount() - 1;
							else idx--;
							subtermFilterSuggestionTable.setSelection(idx);
							break;
	
					case	13:	/* Return */
							if (subtermFilterSuggestionShell.isVisible())
							{
								idx = subtermFilterSuggestionTable.getSelectionIndex();
								if (idx != -1)
								{
									String name = suggestionList.get(idx).getName();
									subtermFilterText.setText(name);
									subtermFilterText.setSelection(name.length());
								}
							}
	
							/* Find the proper term. This is implemented very naively */
							selectedTerm = null;
							for (i=0;i<terms.length;i++)
							{
								if (subtermFilterText.getText().equalsIgnoreCase(terms[i].getName()))
								{
									selectedTerm = terms[i];
									/* Make the suggestion list invisible */
									subtermFilterSuggestionShell.setVisible(false);
									break;
								}
							}
							if (newTermAction != null) newTermAction.act();
							break;
				}
	
			}
		});
		subtermFilterText.addModifyListener(new ModifyListener(){
			public void modifyText(ModifyEvent e)
			{
				String text = subtermFilterText.getText();
	
				/* Populate the term suggestion list */
				suggestionList = new ArrayList<Term>();
	
				for (int i=0;i<terms.length;i++)
				{
					Term term = terms[i];
					if (term.getName().startsWith(text) || 
					  (term.getIDAsString().startsWith(text) && !(text.equalsIgnoreCase("g") || text.equalsIgnoreCase("go") || text.equalsIgnoreCase("go:"))) ||
					    term.getIDAsString().substring(3).startsWith(text))
					{
						suggestionList.add(term);
					}
				}
	
				/* Sort the suggestion list according to names of the terms alphabetically */
				Collections.sort(suggestionList, new Comparator<Term>(){
					public int compare(Term o1, Term o2)
					{
						return o1.getName().compareTo(o2.getName());
					}});
				
				/* We display the list if eighter we have more than one suggestion or if the single suggestion
				 * is no exact match
				 */  
				if (suggestionList.size() > 1 || (suggestionList.size() == 1 && !text.equalsIgnoreCase(suggestionList.get(0).getName())))
				{
					subtermFilterSuggestionTable.setItemCount(suggestionList.size());
					subtermFilterSuggestionTable.clearAll();
					subtermFilterSuggestionTableIDColumn.pack();
					if (subtermFilterSuggestionTableIDColumn.getWidth() < 85)
						subtermFilterSuggestionTableIDColumn.setWidth(85);
					subtermFilterSuggestionTableNamespaceColumn.setWidth(20);
					
					subtermFilterSuggestionTableNameColumn.pack();
	
					if (!subtermFilterSuggestionShell.isVisible())
					{
						Point loc = subtermFilterText.toDisplay(0, 0);
						subtermFilterSuggestionShell.setBounds(loc.x,loc.y + subtermFilterText.getSize().y,
								subtermFilterText.getSize().x, 250);
						subtermFilterSuggestionShell.setVisible(true);
					}
				}	else
				{
					subtermFilterSuggestionTable.setItemCount(suggestionList.size());
					subtermFilterSuggestionTable.clearAll();
					if (subtermFilterSuggestionShell.isVisible())
						subtermFilterSuggestionShell.setVisible(false);
				}
	
			}
		});
		subtermFilterButton = new Button(this,0);
		subtermFilterButton.setText("All");
		subtermFilterButton.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e)
			{
				subtermFilterText.setText("Gene Ontology");
				selectedTerm = null;
				if (newTermAction != null) newTermAction.act();
			}
		});

	}

	/**
	 * Set the terms supported by this chooser.
	 * 
	 * @param supportedTerms
	 */
	public void setSupportedTerms(Term [] supportedTerms)
	{
		this.terms = supportedTerms; 
	}

	public Term getSelectedTerm()
	{
		return selectedTerm;
	}

	public void setNewTermAction(ISimpleAction act)
	{
		newTermAction = act;
	}
}
