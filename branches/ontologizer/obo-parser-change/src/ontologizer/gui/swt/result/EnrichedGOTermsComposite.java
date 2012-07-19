/*
 * Created on 15.04.2006
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ontologizer.gui.swt.result;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import ontologizer.GlobalPreferences;
import ontologizer.association.Gene2Associations;
import ontologizer.calculation.AbstractGOTermProperties;
import ontologizer.calculation.EnrichedGOTermsResult;
import ontologizer.calculation.b2g.Bayes2GOEnrichedGOTermsResult;
import ontologizer.calculation.b2g.Bayes2GOGOTermProperties;
import ontologizer.calculation.b2g.FixedAlphaBetaScore;
import ontologizer.enumeration.GOTermEnumerator;
import ontologizer.enumeration.GOTermEnumerator.GOTermAnnotatedGenes;
import ontologizer.go.Namespace;
import ontologizer.go.Term;
import ontologizer.go.TermID;
import ontologizer.gui.swt.Ontologizer;
import ontologizer.gui.swt.support.GraphCanvas;
import ontologizer.gui.swt.support.IGraphGenerationFinished;
import ontologizer.gui.swt.support.SWTUtil;
import ontologizer.types.ByteString;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationAdapter;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabFolder2Adapter;
import org.eclipse.swt.custom.CTabFolderEvent;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.swtchart.Chart;
import org.swtchart.ILineSeries;
import org.swtchart.ISeries;
import org.swtchart.ISeries.SeriesType;

/**
 * 
 * This is the composite for the result for a EnrichedGOTermsResult results.
 * 
 * @author Sebastian Bauer
 *
 */
public class EnrichedGOTermsComposite extends AbstractResultComposite implements IGraphAction, ITableAction
{
	private static Logger logger = Logger.getLogger(EnrichedGOTermsComposite.class.getCanonicalName());

	/** Defines the significance resolution */
	private static final int SIGNIFICANCE_RESOLUTION = 10000;

	private static final int ACTIVITY = 0;
	private static final int GOID = 1;
	private static final int NAME = 2;
	private static final int NAMESPACE = 3;
	private static final int PVAL = 4;
	private static final int ADJPVAL = 5;
	private static final int MARG = 6;
	private static final int RANK = 7;
	private static final int POP = 8;
	private static final int STUDY = 9;
	private static final int LAST = 10;

	/** Indicates whether pvalues should be handled as marginals
	 * (TODO: needs to be done in a more abstract way) */
	private static boolean useMarginal = false;
	
	/* Texts */
	private static String NOBROWSER_TOOLTIP = "The SWT browser widget could not " +
	"be instantiated. Please ensure that your system fulfills the requirements " +
	"of the SWT browser. Further information can be obtained from the FAQ at " +
	"http://www.eclipse.org/swt.";

	/* Attributes */
	private SashForm verticalSashForm;

	private Composite tableComposite = null;
	private Table table = null;
	private TableColumn [] columns = new TableColumn[LAST];

	private SashForm termSashForm;
	private CTabFolder tableFolder;
	private GraphCanvas graphVisual = null;

	private Composite significanceComposite;
	private Label significanceLabel;
	private Spinner significanceSpinner;
	private Link significanceLink;

	/** The browser displaying information about currently selected term */
	private Browser browser = null;

	/** Uses as fallback if browser is not available */
	private StyledText styledText = null;

	private int sortColumn = -1;
	private int sortDirection = SWT.UP;
	
	/** Maps the terms to a P-value rank */
	private HashMap<TermID,Integer> termID2PValueRank;
	
	/** Used to find the position of a table line given the term id */
	private HashMap<Integer,Integer> termID2ListLine;

	/** Used to find the proper term position given the table position */
	private HashMap<Integer,Integer> line2TermPos;

	/** Used to get the color of a term. The color is determined by the term's significance */
	private HashMap<TermID,Color> termID2Color;

	/** Color for the alpha series */
	private Color alphaColor;
	
	/** Color for the beta series */
	private Color betaColor;
	
	/** Has the user changed set set of checked terms manually? */
	private boolean checkedTermsChanged;
	
	/** The results associated to this composite */
	private EnrichedGOTermsResult result;


	/**
	 * Constructor.
	 * 
	 * @param parent
	 * @param style
	 * @param go
	 */
	public EnrichedGOTermsComposite(Composite parent, int style)
	{
		super(parent, style);

		setLayout(new GridLayout());
		setSize(new Point(500, 500));

		createSubtermFilter(this);
		createSashForm();
		verticalSashForm.setLayoutData(new GridData(SWT.FILL,SWT.FILL,true,true));

		alphaColor = getDisplay().getSystemColor(SWT.COLOR_RED);
		betaColor = getDisplay().getSystemColor(SWT.COLOR_BLUE);
		
		/* Add the dispose listener */
		addDisposeListener(new DisposeListener(){
			public void widgetDisposed(DisposeEvent e)
			{
				disposeSignificanceColors();
			}});

	}

	/**
	 * Sets the displayed results.
	 * 
	 * @param result
	 */
	public void setResult(EnrichedGOTermsResult result)
	{
		super.setResult(result);

		this.result = result;

		if (result instanceof Bayes2GOEnrichedGOTermsResult)
		{
			Bayes2GOEnrichedGOTermsResult b2gResult = (Bayes2GOEnrichedGOTermsResult)result;
			FixedAlphaBetaScore fixedScore;
			if (b2gResult.getScore() instanceof FixedAlphaBetaScore)
			{
				fixedScore = (FixedAlphaBetaScore) b2gResult.getScore();
				tableFolder.setSingle(false);
				
				Composite chartComposite = new Composite(tableFolder, 0);
				chartComposite.setLayout(new FillLayout());

				Chart chart = new Chart(chartComposite, SWT.NONE);
				chart.getTitle().setVisible(false);
				chart.getAxisSet().getYAxis(0).getTitle().setText("Posterior Probability");
				chart.getAxisSet().getXAxis(0).getTitle().setVisible(false);
				ILineSeries alphaSeries = (ILineSeries)chart.getSeriesSet().createSeries(SeriesType.LINE, "alpha");
				alphaSeries.setXSeries(fixedScore.getAlphaValues());
				alphaSeries.setYSeries(fixedScore.getAlphaDistribution());
				alphaSeries.setAntialias(SWT.ON);
				alphaSeries.setLineColor(alphaColor);
				alphaSeries.setLineColor(alphaColor);
				
				ILineSeries betaSeries = (ILineSeries)chart.getSeriesSet().createSeries(SeriesType.LINE, "beta");
				betaSeries.setXSeries(fixedScore.getBetaValues());
				betaSeries.setYSeries(fixedScore.getBetaDistribution());
				betaSeries.setAntialias(SWT.ON);
				betaSeries.setLineColor(betaColor);
				betaSeries.setLineColor(betaColor);

				CTabItem parameterItem = new CTabItem(tableFolder,0);
				parameterItem.setText("Parameter");
				parameterItem.setControl(chartComposite);
				
			}
			
			useMarginal = true;
			
			/* This hides the given columns. Should perhaps find better variant to hide them
			 * (e.g., on creation time)
			 */
			columns[PVAL].setWidth(0);
			columns[PVAL].setResizable(false);
			columns[ADJPVAL].setWidth(0);
			columns[ADJPVAL].setResizable(false);
			
			/* Also set a new default significance selection */
			significanceSpinner.setSelection(SIGNIFICANCE_RESOLUTION/2);
			significanceLabel.setText("Threshold (higher is more important)");
			

		} else
		{
			useMarginal = false;
			columns[MARG].setWidth(0);
			columns[MARG].setResizable(false);
			significanceLabel.setText("Threshold (lower is more important)");
		}
		initializeCheckedTerms();

		/* We sort the array now by adjusted p-values (if no decision could be made,
		 * by normal p-values) in order to get the terms' rank */
		Arrays.sort(props, new Comparator<AbstractGOTermProperties>(){
			public int compare(AbstractGOTermProperties o1, AbstractGOTermProperties o2)
			{
				if (o1.p_adjusted < o2.p_adjusted) return -1;
				if (o1.p_adjusted > o2.p_adjusted) return 1;
				if (o1.p < o2.p) return -1;
				if (o1.p > o2.p) return 1;
				return 0;
			}});
		termID2PValueRank = new HashMap<TermID,Integer>();
		for (int rank = 0;rank < props.length; rank++)
			termID2PValueRank.put(props[rank].goTerm.getID(), rank + 1);

		prepareSignificanceColors();
		buildCheckedTermHashSet();
		populateTable();
		updateSignificanceText();
	}

	/**
	 * Populate the table. Respects sorting settings.
	 */
	void populateTable()
	{
		int entryNumber;
		final int direction;

		if (sortDirection == SWT.UP) direction = 1;
		else direction = -1;
		
		switch (sortColumn)
		{
			case	ACTIVITY: Arrays.sort(props, getCheckedComparator(direction)); break;
			case	GOID: Arrays.sort(props, new GOIDComparator(direction)); break;
			case	NAME: Arrays.sort(props, new GONameComparator(direction)); break;

			case	RANK:
					Arrays.sort(props, new Comparator<AbstractGOTermProperties>(){
						public int compare(AbstractGOTermProperties o1, AbstractGOTermProperties o2)
						{
							int r;
						
							r = termID2PValueRank.get(o1.goTerm.getID()) - termID2PValueRank.get(o2.goTerm.getID());

							r *= direction;
							return r;
						}});
					break;
			
			case	ADJPVAL:
					Arrays.sort(props, new Comparator<AbstractGOTermProperties>(){
						public int compare(AbstractGOTermProperties o1, AbstractGOTermProperties o2)
						{
							int r;
							
							if (o1.p_adjusted < o2.p_adjusted) r = -1;
							else if (o1.p_adjusted > o2.p_adjusted) r = 1;
							else if (o1.p < o2.p) r = -1;
							else if (o1.p > o2.p) r = 1;
							else r = 0;

							r *= direction;
							return r;
						}});
					break;

			case	MARG:
					Arrays.sort(props,new Comparator<AbstractGOTermProperties>()
					{
							public int compare(AbstractGOTermProperties o1, AbstractGOTermProperties o2)
							{
								int r;
	
								if (o1 instanceof Bayes2GOGOTermProperties && o2 instanceof Bayes2GOGOTermProperties)
								{
									double m1 = ((Bayes2GOGOTermProperties)o1).marg;
									double m2 = ((Bayes2GOGOTermProperties)o2).marg;
									
									if (m1 < m2) r = -1;
									else if (m1 > m2) r = 1;
									else r = 0;
								} else
								{
									if (o1.p < o2.p) r = -1;
									else if (o1.p > o2.p) r = 1;
									else r = 0;
								}
								r *= direction;
								return r;
							}
					});
					break;
			case	PVAL:
					Arrays.sort(props,new Comparator<AbstractGOTermProperties>()
					{
						public int compare(AbstractGOTermProperties o1, AbstractGOTermProperties o2)
						{
							int r;

							if (o1.p < o2.p) r = -1;
							else if (o1.p > o2.p) r = 1;
							else r = 0;
							r *= direction;
							return r;
						}
					});
					break;

			case	POP:
					Arrays.sort(props,new Comparator<AbstractGOTermProperties>()
					{
						public int compare(AbstractGOTermProperties o1, AbstractGOTermProperties o2)
						{
							return (o1.annotatedPopulationGenes - o2.annotatedPopulationGenes)*direction;
						}
					});
					break;

			case	STUDY:
					Arrays.sort(props,new Comparator<AbstractGOTermProperties>()
					{
						public int compare(AbstractGOTermProperties o1, AbstractGOTermProperties o2)
						{
							return (o1.annotatedStudyGenes - o2.annotatedStudyGenes)*direction;
						}
					});
					break;
		}


		termID2ListLine = new HashMap<Integer,Integer>();
		line2TermPos = new HashMap<Integer,Integer>();

		entryNumber = 0;

		for (int i=0;i<props.length;i++)
		{
			if (!shouldTermDisplayed(props[i].goTerm))
				continue;
			termID2ListLine.put(props[i].goTerm.getID().id, entryNumber);
			line2TermPos.put(entryNumber, i);
			entryNumber++;
		}

		table.clearAll();
		table.setItemCount(entryNumber);
	}

	/**
	 * Updates the graph. I.e. selects the node currently selected within the
	 * table.
	 */
	private void updateGraph()
	{
		int idx = table.getSelectionIndex();
		if (idx < 0) return;

		TableItem item = table.getItem(idx);
		Term goTerm = (Term)item.getData("term");
		if (goTerm != null)
		{
			graphVisual.selectNode(Integer.toString(goTerm.getID().id));
		}
	}

	/**
	 * Update the browser according to the currently selected table entry.
	 */
	private void updateBrowser()
	{
		int idx = table.getSelectionIndex();
		if (idx < 0) return;

		TableItem item = table.getItem(idx);
		StringBuilder str = new StringBuilder();

		if (browser != null)
		{
			str.append("<html><body>");
			if (item != null)
			{
				str.append(item.getText(NAME));
				str.append(" (");
				str.append(item.getText(GOID));
				str.append(")");
				str.append("<br />");

				Term goTerm = (Term)item.getData("term");
				if (goTerm != null)
				{
					Set<TermID> ancestors = go.getTermParents(goTerm.getID());
					if (ancestors != null)
					{
						str.append("<br />Parents: ");
						str.append("<div style=\"margin-left:20px;\">");
						str.append(createTermString(ancestors));
						str.append("</div>");
					}

					Set<TermID> siblings = go.getTermsSiblings(goTerm.getID());
					if (siblings != null)
					{
						str.append("<br />Siblings: ");
						str.append("<div style=\"margin-left:20px;\">");
						str.append(createTermString(siblings));
						str.append("</div>");
					}

					Set<TermID> descendants = go.getTermChildren(goTerm.getID());
					if (descendants != null)
					{
						str.append("<br />Children: ");
						str.append("<div style=\"margin-left:20px;\">");
						str.append(createTermString(descendants));
						str.append("</div>");
					}

					String def = goTerm.getDefinition();
					if (def == null) def = "No definition available";
					str.append("<br />Definition: ");
					str.append("<font size=\"-1\">");
					str.append(def);
					str.append("</font>");
					
					str.append("<br /><br />");

					str.append("<h3>Annotated Gene Products</h3>");
					/* Enumerate the genes */
					GOTermEnumerator enumerator = result.getStudySet().enumerateGOTerms(go,associationContainer);
					GOTermAnnotatedGenes annotatedGenes = enumerator.getAnnotatedGenes(goTerm.getID());
					
					HashSet<String> directGenes = new HashSet<String>();
					for (ByteString gene : annotatedGenes.directAnnotated)
						directGenes.add(gene.toString());

					int i = 0;
					int totalGenesCount = annotatedGenes.totalAnnotatedCount();
					String [] totalGenes = new String[totalGenesCount];

					for (ByteString gene : annotatedGenes.totalAnnotated)
						totalGenes[i++] = gene.toString();
					Arrays.sort(totalGenes);

					for (i = 0; i < totalGenesCount; i++)
					{
						if (i!=0) str.append(", ");
						str.append("<A HREF=\"gene:");
						str.append(totalGenes[i]);
						str.append("\">");
						if (directGenes.contains(totalGenes[i]))
						{
							str.append("<b>");
							str.append(totalGenes[i]);
							str.append("</b>");
						} else str.append(totalGenes[i]);
						str.append("</A>");
					}
				}
			}
			str.append("</body></html>");
			browser.setText(str.toString());
		} else
		{
			str.append(item.getText(1));
			str.append(" (");
			str.append(item.getText(0));
			str.append(")");
			str.append("\n\n");
			Term goTerm = (Term)item.getData("term");
			if (goTerm != null)
			{
				String def = goTerm.getDefinition();
				if (def == null) def = "No definition available";
				str.append(def);
			}
			
			styledText.setText(str.toString());
		}
	}

	/**
	 * Updates the browser for the given gene.
	 * 
	 * @param gene
	 */
	private void updateBrowserWithGene(String gene)
	{
		StringBuilder str = new StringBuilder();
		str.append("<html><body>");
		str.append(gene);
		str.append("<br /><br />");

		Gene2Associations assocs = associationContainer.get(new ByteString(gene));
		if (assocs != null)
		{
			str.append("Directly annotated by:");
			str.append("<div style=\"margin-left:20px;\">");
			HashSet<TermID> set = new HashSet<TermID>();
			set.addAll(assocs.getAssociations());
			str.append(createTermString(set));
			str.append("</div>");
		}

		str.append("</body></html>");
		browser.setText(str.toString());
	}

	/**
	 * Updates the number of significance terms.
	 */
	void updateSignificanceText()
	{
		double level = getSignificanceLevel();
		int count = 0;
		int total = line2TermPos.size();
		
		/* count the number of significant entries */
		for (Integer i : line2TermPos.keySet())
		{
			AbstractGOTermProperties prop = props[line2TermPos.get(i)];
			if (prop.isSignificant(level)) count++;
		}
		
		/* display */
		StringBuilder str = new StringBuilder();
		
		str.append(getNumberOfCheckedTerms());
		str.append(" (");
		str.append("<a href=\"none\">None</a>");
		str.append(") / ");
		str.append("<a href=\"significant\">");
		str.append(count);
		str.append("</a>");
		str.append(" / ");
		str.append("<a href=\"all\">");
		str.append(total);
		str.append("</a>");
		
		significanceLink.setText(str.toString());
		
		str.setLength(0);
		str.append("The first value shows the number of checked terms.\n");
		str.append("The second value shows the number of terms ");
		if (useMarginal) str.append("above");
		else str.append("below");
		str.append(" the given threshold.\n");
		str.append("The third value shows the total number of terms that are displayed within the table.");
		significanceLink.setToolTipText(str.toString());
	}

	private void buildCheckedTermHashSet()
	{
		initializeCheckedTerms();

		double level = getSignificanceLevel();
		
		/* count the number of significant entries */
		for (int i=0;i<props.length;i++)
		{
			if (props[i].isSignificant(level))
				addToCheckedTerms(props[i].goTerm.getID());
		}
	}

	/**
	 * Dispose all colors used for significant terms.
	 */
	private void disposeSignificanceColors()
	{
		if (termID2Color == null) return;
		for (Color col : termID2Color.values())
			col.dispose();
		termID2Color = null;
	}
	
	/**
	 * Allocate colors used for significant terms.
	 */
	private void prepareSignificanceColors()
	{
		if (termID2Color != null) disposeSignificanceColors();

		double level = getSignificanceLevel();
		int count = 0;
		
		/* count the number of significant entries */
		for (int i=0;i<props.length;i++)
		{
			if (props[i].isSignificant(level))
				count++;
		}

		termID2Color = new HashMap<TermID,Color>();

		for (AbstractGOTermProperties prop : props)
		{
			if (prop.isSignificant(level))
			{
				/* See class StudySetResult */
				float hue,saturation,brightness;

				/* Use the rank to determine the saturation
				 * We want that more significant nodes have more saturation, but
				 * we avoid having significant nodes with too less saturation (at
				 * least 0.2) */
				int rank = termID2PValueRank.get(prop.goTerm.getID()) - 1;
				assert(rank < count);
				saturation = 1.0f - (((float)rank  + 1)/count)*0.8f;

				/* Always full brightness */
				brightness = 1.0f;

				/* Hue depends on namespace */
				switch (Namespace.getNamespaceEnum(prop.goTerm.getNamespace()))
				{
					case BIOLOGICAL_PROCESS: hue = 120.f;break;
					case MOLECULAR_FUNCTION: hue = 60.f;break;
					case CELLULAR_COMPONENT: hue = 300.f;break;
					default: hue = 0.f; saturation = 0.f;
				}

				termID2Color.put(prop.goTerm.getID(), new Color(getDisplay(),new RGB(hue,saturation,brightness)));
			}
		}
	}


	/**
	 * Returns the proper formatted html string derived from the ids to the given
	 * str. Only terms, which actually have annotations are emitted.
	 * 
	 * @param str
	 * @param termids
	 */
	private String createTermString(final Set<TermID> termids)
	{
		StringBuilder str = new StringBuilder();

		ArrayList<Term> terms = new ArrayList<Term>();

		for (TermID termid : termids)
		{
			/* Is the term displayed? */
			Integer lineIdx = termID2ListLine.get(termid.id); 
			if (lineIdx != null)
			{
				Term term = go.getTerm(termid);
				if (term != null)
					terms.add(term);
			}
		}

		Collections.sort(terms,new Comparator<Term>()
				{
					public int compare(Term o1, Term o2)
					{
						return o1.getName().compareTo(o2.getName());
					}
				});

		for (Term term : terms)
		{
			TermID termid = term.getID();
			str.append("<A HREF=\"termid:");
			str.append(termid.id);
			str.append("\">");
			str.append(term.getName());
			str.append("</A>");

			if (term != null)
			{
				AbstractGOTermProperties prop = result.getGOTermProperties(term);
				if (prop.isSignificant(getSignificanceLevel()))
					str.append("(*)");
			}

			str.append("<br />");
		}
		
		return str.toString();
	}

	/**
	 * This method initializes sashForm.	
	 */
	private void createSashForm()
	{
		verticalSashForm = new SashForm(this, SWT.VERTICAL);

		createTableComposite();
		createBrowser();

		verticalSashForm.setWeights(new int[]{2,1});
	}

	/**
	 * This method initializes table.	
	 */
	private void createTableComposite()
	{
		/* Create the sorting listener used for the table */
		SelectionAdapter sortingListener = new SelectionAdapter()
		{
			public void widgetSelected(SelectionEvent ev)
			{
				TableColumn col = (TableColumn)ev.widget;
				if (table.getSortColumn() == col)
				{
					if (table.getSortDirection() == SWT.UP) sortDirection = SWT.DOWN;
					else sortDirection = SWT.UP;
				} else sortDirection = SWT.UP;

				sortColumn = (Integer)col.getData("column");

				TableItem selectedItem = null;
				TableItem [] selectedItems = table.getSelection();
				if (selectedItems != null && selectedItems.length > 0)
					selectedItem = selectedItems[0];

				table.setSortColumn(col);
				table.setSortDirection(sortDirection);
				
				populateTable();

				if (selectedItem != null)
				{
					Term selectedTerm = (Term)selectedItem.getData("term");
					table.setSelection(termID2ListLine.get(selectedTerm.getID().id));
				}

			}
		};

		/* Term Overview Sash Form */
		termSashForm = new SashForm(verticalSashForm, SWT.HORIZONTAL);

		tableFolder = new CTabFolder(termSashForm,SWT.BORDER);
		tableFolder.setSingle(true);
		tableFolder.setMaximizeVisible(true);
		tableFolder.setSelectionBackground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));

		CTabItem tableCTabItem = new CTabItem(tableFolder,0);
		tableCTabItem.setText("Table");
		tableFolder.setSelection(0);
		tableFolder.addCTabFolder2Listener(new CTabFolder2Adapter()
		{
			public void maximize(CTabFolderEvent event)
			{
				tableFolder.setMaximized(true);
				verticalSashForm.setRedraw(false);
				verticalSashForm.setMaximizedControl(termSashForm);
				termSashForm.setMaximizedControl(tableFolder);
				verticalSashForm.setRedraw(true);
			}
			
			public void restore(CTabFolderEvent event)
			{
				verticalSashForm.setRedraw(false);
				tableFolder.setMaximized(false);
				verticalSashForm.setMaximizedControl(null);
				termSashForm.setMaximizedControl(null);
				verticalSashForm.setRedraw(true);
			}
		});

		tableComposite = new Composite(tableFolder, SWT.NONE);
		tableCTabItem.setControl(tableComposite);
		tableComposite.setLayout(SWTUtil.newEmptyMarginGridLayout(1));

		createGraphComposite(termSashForm);

		/* Initially, we hide the graph */
		termSashForm.setMaximizedControl(tableFolder);

		/* Table widget */
		table = new Table(tableComposite, SWT.BORDER | SWT.FULL_SELECTION | SWT.CHECK | SWT.VIRTUAL);
		GridData tableGridData = new GridData(GridData.FILL_BOTH|GridData.GRAB_HORIZONTAL|GridData.GRAB_VERTICAL);
		table.setLayoutData(tableGridData);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		table.addSelectionListener(new SelectionAdapter()
		{
			public void widgetSelected(SelectionEvent e)
			{
				if (e.detail == SWT.CHECK)
				{
					TableItem ti = (TableItem)e.item;
					Term term = (Term)ti.getData("term");
					if (ti.getChecked()) addToCheckedTerms(term.getID());
					else removeFromCheckedTerms(term.getID());
					checkedTermsChanged = true;
					updateSignificanceText();
				}
				updateBrowser();
				updateGraph();
			}
		});
		table.addListener(SWT.SetData, new Listener() {
			public void handleEvent(Event e) {
				TableItem item = (TableItem) e.item;
				Integer index = line2TermPos.get(e.index);
				if (index != null)
				{
					AbstractGOTermProperties prop = props[index];
					item.setText(GOID, prop.goTerm.getIDAsString());
					item.setText(NAME, prop.goTerm.getName());
					item.setText(NAMESPACE,prop.goTerm.getNamespaceAsAbbrevString());
					if (useMarginal)
					{
						if (prop instanceof Bayes2GOGOTermProperties) {
							Bayes2GOGOTermProperties b2gp = (Bayes2GOGOTermProperties) prop;
							item.setText(MARG,String.format("%.3g",b2gp.marg));	
						}
						
					}	else
					{
						item.setText(PVAL,String.format("%.3g",prop.p));
						item.setText(ADJPVAL,String.format("%.3g",prop.p_adjusted));
					}
					item.setText(RANK,termID2PValueRank.get(prop.goTerm.getID()).toString());
					item.setText(POP,Integer.toString(prop.annotatedPopulationGenes));
					item.setText(STUDY,Integer.toString(prop.annotatedStudyGenes));
	
					item.setData("term",prop.goTerm);
	
					if (isCheckedTerm(prop.goTerm.getID()))
						item.setChecked(true);
	
					Color background = termID2Color.get(prop.goTerm.getID());
					if (useMarginal)
					{
						item.setBackground(MARG,background);	
					} else
					{
						item.setBackground(PVAL,background);
						item.setBackground(ADJPVAL,background);
					}

				}
			}
		});

		/* Table columns */
		for (int i=0;i<LAST;i++)
		{
			columns[i] = new TableColumn(table, SWT.NONE);
			columns[i].addSelectionListener(sortingListener);
			columns[i].setData("column",new Integer(i));
		}
		columns[ACTIVITY].setText("");
		columns[GOID].setText("GO ID");
		columns[NAME].setText("Name");
		columns[NAMESPACE].setText("NSP");
		columns[NAMESPACE].setAlignment(SWT.CENTER);
		columns[NAMESPACE].setToolTipText("Namespace or sub ontology");
		columns[PVAL].setText("P-Value");
		columns[PVAL].setAlignment(SWT.RIGHT);
		columns[ADJPVAL].setText("Adj. P-Value");
		columns[ADJPVAL].setToolTipText("Adjusted P-Value");
		columns[ADJPVAL].setAlignment(SWT.RIGHT);
		columns[MARG].setText("Marginal");
		columns[MARG].setToolTipText("Marginal probability");
		columns[MARG].setAlignment(SWT.RIGHT);
		columns[RANK].setText("Rank");
		columns[RANK].setToolTipText("The rank of the term in the list");
		columns[RANK].setAlignment(SWT.RIGHT);
		columns[POP].setText("Pop. Count");
		columns[POP].setAlignment(SWT.RIGHT);
		columns[POP].setToolTipText("Number of entries within the population set annotated to the term");
		columns[STUDY].setText("Study Count");
		columns[STUDY].setAlignment(SWT.RIGHT);
		columns[STUDY].setToolTipText("Number of entries within the study set annotated to the term");
		for (int i=0;i<LAST;i++)
			columns[i].pack();
		/* Ensure useful columns sizes */
		if (columns[ACTIVITY].getWidth() < 20)
			columns[ACTIVITY].setWidth(20);
		if (columns[GOID].getWidth() < 80)
			columns[GOID].setWidth(90);
		if (columns[NAME].getWidth() < 250)
			columns[NAME].setWidth(250);

		/* Significance composite */
		significanceComposite = new Composite(tableComposite,SWT.NONE);
		significanceComposite.setLayout(SWTUtil.newEmptyMarginGridLayout(3));
		significanceComposite.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL|GridData.FILL_HORIZONTAL));
		significanceLink = new Link(significanceComposite,SWT.READ_ONLY);
		significanceLink.setBackground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
		significanceLink.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL|GridData.FILL_HORIZONTAL));
		significanceLink.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				if (e.text.equals("none"))
				{
					/* Clear the selections */
					initializeCheckedTerms();
					checkedTermsChanged = true;
				}
				else if (e.text.equals("significant"))
				{
					/* Adjust the selection to the significant ones */
					buildCheckedTermHashSet();
					checkedTermsChanged = false;
				}
				else if (e.text.equals("all"))
				{
					/* Select all terms */
					initializeCheckedTerms();
					for (int i=0;i<props.length;i++)
						addToCheckedTerms(props[i].goTerm.getID());
					checkedTermsChanged = true;
				}
				table.clearAll();
				updateSignificanceText();
			}
		});
		significanceLabel = new Label(significanceComposite,SWT.NONE);
		significanceLabel.setText("Threshold");
		significanceSpinner = new Spinner(significanceComposite,SWT.BORDER);
		significanceSpinner.setMaximum(SIGNIFICANCE_RESOLUTION);
		significanceSpinner.setDigits(4);
		significanceSpinner.setSelection(SIGNIFICANCE_RESOLUTION/10);
		significanceSpinner.setIncrement(10);
		significanceSpinner.setPageIncrement(1);
		significanceSpinner.setToolTipText("The significance level determines the threshold of terms\nthat are considered as significantly enriched, and thus\nbeing colored.");
		significanceSpinner.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e)
			{
				updateSignificanceText();
				disposeSignificanceColors();
				prepareSignificanceColors();
				if (!checkedTermsChanged)
					buildCheckedTermHashSet();
				table.clearAll();
			}});
	}

	/**
	 * Initialized the graph composite.
	 * 
	 * @param parent
	 */
	private void createGraphComposite(Composite parent)
	{
		final CTabFolder graphFolder = new CTabFolder(parent,SWT.BORDER);
		graphFolder.setMaximizeVisible(true);
		graphFolder.setSingle(true);
		graphFolder.setSelectionBackground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
		CTabItem graphItem = new CTabItem(graphFolder,0);
		graphItem.setText("Graph Preview");

		graphVisual = new GraphCanvas(graphFolder,SWT.BORDER);
		graphVisual.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e)
			{
				try
				{
					int id = Integer.parseInt(e.text);
					Integer selection = termID2ListLine.get(id);
					if (selection != null)
					{
						table.setSelection(selection);
						updateBrowser();
					}
				} catch (Exception ex) {}
			}
		});
		graphItem.setControl(graphVisual);

		/* The context menu */
		Menu contextMenu = graphVisual.getMenu();
		new MenuItem(contextMenu,SWT.SEPARATOR);
		final MenuItem childTermsMenuItem = new MenuItem(contextMenu,0);
		childTermsMenuItem.setText("Enable Child Terms");
		final MenuItem annotateMenuItem = new MenuItem(contextMenu,0);
		annotateMenuItem.setText("Copy Annotated Genes");
		final MenuItem notAnnotatedMenuItem = new MenuItem(contextMenu,0);
		notAnnotatedMenuItem.setText("Copy Not Annotated Genes");

		SelectionAdapter menuItemAdapter = new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				try
				{
					String stringId = graphVisual.getNameOfCurrentSelectedNode();
					int id = Integer.parseInt(stringId);

					if (e.widget.equals(childTermsMenuItem))
					{
						Set<TermID> termIDs = go.getTermChildren(new TermID(id));
						for (TermID termID : termIDs)
						{
							Integer selection = termID2ListLine.get(termID.id);
							if (selection != null)
							{
								addToCheckedTerms(termID);
								table.getItem(selection).setChecked(true);
								table.clear(selection);
							}
						}
					} else if (e.widget.equals(annotateMenuItem))
					{
						GOTermEnumerator enumerator = result.getStudySet().enumerateGOTerms(go,associationContainer);

						Clipboard clipboard = new Clipboard(getDisplay());
						StringWriter sw = new StringWriter();
						PrintWriter pw = new PrintWriter(sw);

						for (ByteString gene : enumerator.getAnnotatedGenes(new TermID(id)).totalAnnotated)
						{
							String desc = result.getStudySet().getGeneDescription(gene); 
							
							pw.append(gene.toString());
							pw.append("\t");
							if (desc != null)
								pw.append(desc);
							pw.println();
						}

						pw.flush();

						clipboard.setContents(new Object[]{sw.toString()},new Transfer[]{TextTransfer.getInstance()});
						clipboard.dispose();
					} else if (e.widget.equals(notAnnotatedMenuItem))
					{
						GOTermEnumerator enumerator = result.getStudySet().enumerateGOTerms(go,associationContainer);

						/* Build hashset in order to have constant time access */
						HashSet<ByteString> annotatedGenes = new HashSet<ByteString>();
						annotatedGenes.addAll(enumerator.getAnnotatedGenes(new TermID(id)).totalAnnotated);

						Clipboard clipboard = new Clipboard(getDisplay());
						StringBuilder str = new StringBuilder();
						
						for (ByteString gene : result.getStudySet())
						{
							if (!annotatedGenes.contains(gene))
							{
								String desc = result.getStudySet().getGeneDescription(gene); 

								str.append(gene.toString());
								str.append("\t");
								if (desc != null)
									str.append(result.getStudySet().getGeneDescription(gene));
								str.append("\n");
							}
						}

						clipboard.setContents(new Object[]{str.toString()},new Transfer[]{TextTransfer.getInstance()});
						clipboard.dispose();
					}
				} catch (Exception ex) {}
			}
		};
		
		childTermsMenuItem.addSelectionListener(menuItemAdapter);
		annotateMenuItem.addSelectionListener(menuItemAdapter);
		notAnnotatedMenuItem.addSelectionListener(menuItemAdapter);

		graphItem.setControl(graphVisual);
		graphFolder.setSelection(0);
		graphFolder.addCTabFolder2Listener(new CTabFolder2Adapter(){
			public void maximize(CTabFolderEvent event)
			{
				graphFolder.setMaximized(true);
				verticalSashForm.setRedraw(false);
				verticalSashForm.setMaximizedControl(termSashForm);
				termSashForm.setMaximizedControl(graphFolder);
				verticalSashForm.setRedraw(true);
			}

			public void restore(CTabFolderEvent event)
			{ 
				verticalSashForm.setRedraw(false);
				graphFolder.setMaximized(false);
				verticalSashForm.setMaximizedControl(null);
				termSashForm.setMaximizedControl(null);
				verticalSashForm.setRedraw(true);
			}
	
		});
	}

	/**
	 * This method initializes browser	
	 */
	private void createBrowser()
	{
		/* Use an auxiliary composite for the browser, because
		 * the browser actually is instanciated even if it fails
		 * (issue was reported and will be fixed in post Eclipse 3.2) */
		final Composite browserComposite = new Composite(verticalSashForm,0);
		browserComposite.setLayout(new FillLayout());
		try {
			final CTabFolder browserFolder = new CTabFolder(browserComposite, SWT.BORDER);
			browserFolder.setMaximizeVisible(true);
			browserFolder.setSingle(true);
			browserFolder.setSelectionBackground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));

			CTabItem browserTabItem = new CTabItem(browserFolder,0);
			browserTabItem.setText("Browser");
			browser = new Browser(browserFolder, SWT.BORDER);
			browser.addLocationListener(new LocationAdapter() {
				public void changing(LocationEvent event)
				{
					if (event.location.startsWith("termid:"))
					{
						/* TODO handle the root term in a better way */
						try
						{
							int id = Integer.parseInt(event.location.substring(7));
							Integer selection = termID2ListLine.get(id);
							if (selection != null)
							{
								table.setSelection(selection);
								updateBrowser();
								updateGraph();
							}
						} catch(Exception e)
						{
						}
						
						/* We return such that we won't accept the url change.
						 * This disables the usage of the back button which is
						 * of course not intuitive. But I found no way how
						 * to make the back button usable as setting the text
						 * of the browser is handled like a new url */
						event.doit = false;
					} else
					{
						if (event.location.startsWith("gene:"))
						{
							updateBrowserWithGene(event.location.substring(5));
							event.doit = false;
						}
					}
				}});
			browserTabItem.setControl(browser);
			browserFolder.setSelection(0);

			browserFolder.addCTabFolder2Listener(new CTabFolder2Adapter()
			{
				public void maximize(CTabFolderEvent event)
				{
					browserFolder.setMaximized(true);
					verticalSashForm.setMaximizedControl(browserComposite);
				}

				public void restore(CTabFolderEvent event)
				{ 
					browserFolder.setMaximized(false);
					verticalSashForm.setMaximizedControl(null);
				}

			});
		} catch (SWTError e) {
			browserComposite.dispose();
			browser = null;

			/* Create the fall back environment */
			Composite styledTextComposite = new Composite(verticalSashForm,0);
			styledTextComposite.setLayout(SWTUtil.newEmptyMarginGridLayout(1));

			Label label = new Label(styledTextComposite,0);
			label.setText("No browser available!");

			String error = e.getLocalizedMessage();
			if (error != null)
				label.setToolTipText(NOBROWSER_TOOLTIP + "\n\nReason for failing: " + error);
			else
				label.setToolTipText(NOBROWSER_TOOLTIP);

			styledText = new StyledText(styledTextComposite,SWT.BORDER|SWT.READ_ONLY|SWT.WRAP);
			styledText.setEditable(false);
			styledText.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL|GridData.GRAB_VERTICAL|GridData.FILL_BOTH));
	      }
	}

	@Override
	protected void newEmanatingTermSelected(Term term)
	{
		super.newEmanatingTermSelected(term);
		
		populateTable();
		updateSignificanceText();
	}

	/**
	 * Returns the currently choosen significance level.
	 * 
	 * @return the significance level (between 0 and 1)
	 */
	public double getSignificanceLevel()
	{
		double level = significanceSpinner.getSelection() / (double)SIGNIFICANCE_RESOLUTION;
		return level;
	}
	
	/**
	 * Helper function to create a new graph generation thread.
	 * 
	 * @param finished
	 * 
	 * @return
	 */
	private EnrichedGraphGenerationThread createGraphGenerationThread(IGraphGenerationFinished finished)
	{
		EnrichedGraphGenerationThread ggt = new EnrichedGraphGenerationThread(getDisplay(),GlobalPreferences.getDOTPath(),finished);
		ggt.go = go;
		ggt.emanatingTerm = getEmanatingTerm();
		ggt.significanceLevel = getSignificanceLevel();
		ggt.leafTerms.addAll(getCheckedTermsCollection());
		ggt.result = result;
		return ggt;
	}

	public void setScaleToFit(boolean fit)
	{
		graphVisual.setScaleToFit(fit);
	}

	public void zoomIn()
	{
		graphVisual.zoomIn();
	}

	public void zoomOut()
	{
		graphVisual.zoomOut();
	}

	public void resetZoom()
	{
		graphVisual.zoomReset();
	}

	/**
	 * Preview the graph.
	 */
	public void updateDisplayedGraph()
	{
		EnrichedGraphGenerationThread ggt = createGraphGenerationThread(new IGraphGenerationFinished(){
			public void finished(boolean success, String message, File pngFile, File dotFile)
			{
				if (success)
				{
					logger.info("Layouted graph successful.");

					/* make the graph display visible */
					termSashForm.setMaximizedControl(null);

					try
					{
						graphVisual.setLayoutedDotFile(dotFile);
					} catch(Exception e)
					{
						e.printStackTrace();
					}
				} else
				{
					logger.warning("Layouting graph failed.");

					MessageBox mbox = new MessageBox(getShell(), SWT.ICON_ERROR | SWT.OK);
					mbox.setMessage("Unable to execute the 'dot' tool!\nPlease check the preferences, and ensure that GraphViz (available from http://www.graphviz.org/) is installed properly\n\n" + message);
					mbox.setText("Ontologizer - Error");
					mbox.open();
				}
			}
		});
		logger.info("Layouting graph.");
		ggt.start();
	}
	
	/**
	 * Stores the result as graph.
	 */
	public void saveGraph(String path)
	{
		EnrichedGraphGenerationThread ggt = createGraphGenerationThread(new IGraphGenerationFinished(){
			public void finished(boolean success, String message, File pngFile, File dotFile)
			{
				if (!success && !isDisposed())
				{
					MessageBox mbox = new MessageBox(getShell(), SWT.ICON_ERROR | SWT.OK);
					mbox.setMessage("Unable to execute the 'dot' tool!\n\n" + message);
					mbox.setText("Ontologizer - Error");
					mbox.open();
				}
			}
		});

		ggt.setGfxOutFilename(path);
		ggt.start();
	}

	/**
	 * Store the result as ascii table.
	 * 
	 * @param path defines the path where the file should be written to.
	 */
	public void tableSave(String path)
	{
		File tableFile = new File(path);
		result.writeTable(tableFile);
	}

	/**
	 * Store the result as a latex file.
	 */
	public void latexSave(String path)
	{
		EnrichedGOTermsResultLatexWriter.write(result,new File(path),getCheckedTermsCollection());
	}

	/**
	 * Store the result as html site.
	 * 
	 * @param path defines the path where the files should be written to.
	 */
	public void htmlSave(String path)
	{
		final File htmlFile = new File(path);
		EnrichedGraphGenerationThread ggt = createGraphGenerationThread(new IGraphGenerationFinished()
		{
			public void finished(boolean success, String message, File pngFile,	File dotFile)
			{
				if (!isDisposed())
				{
					if (success)
					{
						logger.info("HTML preparation finished with success!");
						Ontologizer.showWaitPointer();
						EnrichedGOTermsResultHTMLWriter.write(result, htmlFile, dotFile);
						Ontologizer.hideWaitPointer();
					} else
					{
						logger.warning("HTML preparation finished with failure!");
						MessageBox mbox = new MessageBox(getShell(), SWT.ICON_ERROR | SWT.OK);
						mbox.setMessage("Unable to execute the 'dot' tool!\n\n" + message);
						mbox.setText("Ontologizer - Error");
						mbox.open();
					}
				}
				else
					logger.info("HTML preparation finished with success = " +success);
			}
		});
		
		logger.info("Preparing to store html file \"" + path + "\".");
		ggt.start();
	}

	/**
	 * Store the annotated results as ascii text.
	 *  
	 * @param path defines the path where the file should be written to.
	 */
	public void tableAnnotatedSetSave(String path)
	{
		File tableFile = new File(path);
		result.getStudySet().writeSetWithAnnotations(result.getGO(),associationContainer,tableFile);
	}
	
	public EnrichedGOTermsResult getResult()
	{
		return result;
	}

	@Override
	public String getTitle()
	{
		String correctionName = getResult().getCorrectionName();
		String calculationName = getResult().getCalculationName();
		
		 if (correctionName != null)
			 return getResult().getStudySet().getName() + " (" + calculationName + "/" + correctionName + ")";
		 else
			 return getResult().getStudySet().getName() + " (" + calculationName + ")";
	}

}
