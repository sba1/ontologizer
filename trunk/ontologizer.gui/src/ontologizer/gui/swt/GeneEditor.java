/*
 * Created on 19.11.2007
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ontologizer.gui.swt;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ontologizer.association.Association;
import ontologizer.association.AssociationContainer;
import ontologizer.association.Gene2Associations;
import ontologizer.go.Ontology;
import ontologizer.go.Term;
import ontologizer.go.TermID;
import ontologizer.types.ByteString;
import ontologizer.worksets.WorkSet;
import ontologizer.worksets.WorkSetLoadThread;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.Bullet;
import org.eclipse.swt.custom.LineStyleEvent;
import org.eclipse.swt.custom.LineStyleListener;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GlyphMetrics;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

/**
 * Class used for editing genes.
 * 
 * @author Sebastian Bauer
 */
public class GeneEditor extends Composite
{
	private static String currentImportStudyFileName;

	private Ontology graph;
	private AssociationContainer assoc;

	private Shell tipShell;
	private StyledText tipShellStyledText;
	private String staticToolTipText;
	private StyledText text;

	private Composite setButtonComposite;
	private Button setAllButton;
	private Button setAppendButton;
	private Button setClearButton;

	private FontData data;
	private Font smallFont;
	
	private WorkSet displayedWorkSet;
	
	private ISimpleAction datasetsLoadedAction;
	
	private GraphWindow graphWindow;
	
	public interface INewNameListener
	{
		public void newName(String name);
	}
	
	private List<INewNameListener> newNameListeners;
	
	/** The anchor of the tooltip (as in carret) */
	private int tooltipCarret = -1;
	
	/**
	 * Constructor for the GeneEditor class.
	 * 
	 * @param parent
	 * @param style
	 */
	public GeneEditor(Composite parent, int style)
	{
		super(parent, style);

		newNameListeners = new ArrayList<INewNameListener>();

		tipShell = new Shell(parent.getShell(),SWT.ON_TOP|SWT.TOOL);
		tipShell.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
		FillLayout fl = new FillLayout();
		fl.marginHeight = 2;
		fl.marginWidth = 2;
		tipShell.setLayout(fl);
		tipShellStyledText = new StyledText(tipShell,SWT.WRAP);
		tipShellStyledText.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
		tipShellStyledText.setForeground(parent.getDisplay().getSystemColor(SWT.COLOR_INFO_FOREGROUND));

		graphWindow = new GraphWindow(parent.getDisplay());

		data = getShell().getFont().getFontData()[0];
		smallFont = new Font(getShell().getDisplay(), data.getName(), data.getHeight() * 9 / 10, data.getStyle());

		/* The composite's contents */
		setLayout(new GridLayout());

		text = new StyledText(this,SWT.MULTI | SWT.V_SCROLL | SWT.BORDER);
		text.setLayoutData(new GridData(GridData.FILL_BOTH));

		GridLayout gridLayout5 = new GridLayout();
		gridLayout5.numColumns = 3;  // Generated
		gridLayout5.marginHeight = 0;  // Generated
		gridLayout5.marginWidth = 0;  // Generated
		gridLayout5.horizontalSpacing = 2;  // Generated

		/* set button composite */
		setButtonComposite = new Composite(this, SWT.NONE);
		setButtonComposite.setLayout(gridLayout5);
		setButtonComposite.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL|GridData.FILL_HORIZONTAL));

		setAppendButton = new Button(setButtonComposite, SWT.NONE);
		setAppendButton.setText("Append Set...");
		setAppendButton.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL|GridData.FILL_HORIZONTAL));
		setAppendButton.setToolTipText("Opens a file dialog where an ASCII file can be choosen whose contents is appended to the gene set editor.");
		setAppendButton.addSelectionListener(new SelectionAdapter()
		{
			public void widgetSelected(SelectionEvent e)
			{
				FileDialog fileDialog = new FileDialog(getShell(),SWT.OPEN);
				if (currentImportStudyFileName != null)
				{
					File f = new File(currentImportStudyFileName);
					fileDialog.setFilterPath(f.getParent());
					fileDialog.setFileName(f.getName());
				}

				String fileName = fileDialog.open();
				if (fileName != null)
				{
					appendFileContents(fileName);
				}
			}
		});
		
		setAllButton = new Button(setButtonComposite, SWT.NONE);
		setAllButton.setText("Take Them All");
		setAllButton.setToolTipText("Pastes all available identifieres");
		setAllButton.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL|GridData.FILL_HORIZONTAL));
		setAllButton.setEnabled(false);
		setAllButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				if (text.getContent().getCharCount()!=0 && assoc != null)
				{
					MessageBox mb = new MessageBox(setAllButton.getShell(),SWT.YES|SWT.NO);
					mb.setMessage("Do you really want to replace the current content\nwith all available identifieres?");
					if (mb.open() == SWT.NO)
						return;
				}
				StringBuilder str = new StringBuilder();
					
				for (ByteString g2a : assoc.getAllAnnotatedGenes())
				{
					str.append(g2a.toString());
					str.append("\n");
				}
				text.setText(str.toString());
			}
		});
		
		setClearButton = new Button(setButtonComposite, SWT.NONE);
		setClearButton.setText("Clear");
		setClearButton.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL|GridData.FILL_HORIZONTAL));
		setClearButton.setToolTipText("Erases all the contents of the gene set editor.");
		setClearButton.addSelectionListener(new SelectionAdapter()
		{
			public void widgetSelected(SelectionEvent e)
			{
				clear();
			}
		});


		/* Listeners */

		text.addLineStyleListener(new LineStyleListener()
		{
			public void lineGetStyle(LineStyleEvent event)
			{
				if (assoc != null)
				{
					String gene = getGeneName(event.lineText);
					Gene2Associations gene2Associations = assoc.get(new ByteString(gene));
					if (gene2Associations != null)
					{
						event.styles = new StyleRange[1];
						event.styles[0] = new StyleRange(event.lineOffset, gene.length(), null, null);
						event.styles[0].fontStyle = SWT.BOLD;
					}
				}
				
			}
		});
		text.addKeyListener(new KeyAdapter()
		{
			@Override
			public void keyPressed(KeyEvent e)
			{
				if (e.keyCode == SWT.F2 && graph != null && assoc != null)
				{
					int carret;
					
					/* If a tooltip is active we use the stored anchor */
					if (tooltipCarret != -1) carret = tooltipCarret;
					else carret = text.getCaretOffset();

					int lineIdx = text.getLineAtOffset(carret);
					
					int offset1 = text.getOffsetAtLine(lineIdx);
					int offset2;
					
					if (lineIdx < text.getLineCount() - 1)
						offset2 = text.getOffsetAtLine(lineIdx+1) - 1;
					else
						offset2 = text.getCharCount() - 1;

					if (offset1 <= offset2 && offset2 < text.getCharCount())
					{
						String line = text.getText(offset1,offset2).trim();
						String geneName = getGeneName(line);

						Gene2Associations gene2Associations = assoc.get(new ByteString(geneName));
						if (gene2Associations != null)
						{
							Set<TermID> set = new HashSet<TermID>();
							for (Association a : gene2Associations)
								set.add(a.getTermID());
							
							if (set.size() > 0)
							{
								graphWindow.setVisibleTerms(graph, set);
								graphWindow.setVisible(true);
							}
						}
					}
				} else
				{
					tooltipCarret = -1;
					tipShell.setVisible(false);
				}
					
			}
		});
		
		Menu contextMenu = new Menu(text);
		MenuItem cutItem = new MenuItem(contextMenu,0);
		cutItem.setText("Cut");
		cutItem.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				text.cut();
			}
		});

		MenuItem copyItem = new MenuItem(contextMenu,0);
		copyItem.setText("Copy");
		copyItem.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				text.copy();
			}
		});

		MenuItem pasteItem = new MenuItem(contextMenu,0);
		pasteItem.setText("Paste");
		pasteItem.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				text.paste();
			}
		});
		

		MenuItem eraseItem = new MenuItem(contextMenu,0);
		eraseItem.setText("Erase");
		eraseItem.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				Point sel = text.getSelection();
				text.replaceTextRange(sel.x, sel.y - sel.x, "");
			}
		});

		MenuItem selectAllItem = new MenuItem(contextMenu, 0);
		selectAllItem.setText("Select All");
		selectAllItem.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				text.selectAll();
			}
		});
		text.setMenu(contextMenu);

		text.addMouseMoveListener(new MouseMoveListener()
		{
			public void mouseMove(MouseEvent e)
			{
				if (tipShell.isVisible())
					tipShell.setVisible(false);
			}
		});
		text.addMouseTrackListener(new MouseTrackAdapter()
		{
			public void mouseHover(MouseEvent e)
			{
				boolean visible = false;

				Point p = text.toDisplay(e.x, e.y);
				

				if (assoc == null)
				{
					displayStaticToolTip(p.x,p.y);
					return;
				}

				try
				{
					int lineIdx = text.getLineIndex(e.y);
					int offset1 = text.getOffsetAtLine(lineIdx);
					int offset2;
					
					if (lineIdx < text.getLineCount() - 1)
						offset2 = text.getOffsetAtLine(lineIdx+1) - 1;
					else
						offset2 = text.getCharCount() - 1;
	
					if (offset1 <= offset2 && offset2 < text.getCharCount())
					{
						String line = text.getText(offset1,offset2).trim();
						String geneName = getGeneName(line);
						
						int offsetAtLocation = text.getOffsetAtLocation(new Point(e.x,e.y));
						if ((offsetAtLocation - offset1) < geneName.length())
						{
							Gene2Associations gene2Associations = assoc.get(new ByteString(geneName));
							if (gene2Associations != null)
							{
								StringBuilder str = new StringBuilder();
		
								int size = gene2Associations.getAssociations().size();
								
								str.append(geneName + " has " + size + " direct ");
								if (size == 1) str.append("annotation.");
								else str.append("annotations.");
		
								int count = 0;
								for (Association ga : gene2Associations)
								{
									str.append("\n");
									
									Term t = graph.getTerm(ga.getTermID());
									if (t != null)
									{
										str.append(t.getName());
										str.append(" (");
									}
									str.append(ga.getTermID().toString());
									if (t != null)
										str.append(")");
									
									count++;
								}
							
								tipShellStyledText.setText(str.toString());
								tipShellStyledText.append("\n");
		
								/* Add bullets */
								StyleRange sr = new StyleRange();
								sr.metrics = new GlyphMetrics(0,0,10);
								Bullet b = new Bullet(sr);
								tipShellStyledText.setLineBullet(1, count, b);
		
								/* Add info */
								int start = tipShellStyledText.getCharCount();
								tipShellStyledText.append("Press 'F2' for induced graph.");
								int end = tipShellStyledText.getCharCount();
		
								sr = new StyleRange();
								sr.font = smallFont; 
								sr.start = start;
								sr.length = end - start;
								tipShellStyledText.setStyleRange(sr);
		
								tipShellStyledText.setLineAlignment(count+1, 1, SWT.RIGHT);
								
								tipShell.layout();
								tipShell.pack();
								tipShell.setLocation(p.x - tipShell.getSize().x / 2, p.y + text.getLineHeight(offset1));
								
								visible = true;
							}
							
						}
					}
				} catch (IllegalArgumentException ex)
				{
				}
				tipShell.setVisible(visible);

				/* If no tooltip is displayed, display the standard one */
				if (visible == false)
					displayStaticToolTip(p.x,p.y);
			}
		});
		/* Turn the attachment into a drop target, we support link operations only */ 
		DropTarget attachmentDropTarget = new DropTarget(text,DND.DROP_COPY|DND.DROP_DEFAULT);
		attachmentDropTarget.setTransfer(new Transfer[]{FileTransfer.getInstance()});
		attachmentDropTarget.addDropListener(new DropTargetAdapter()
		{
			@Override
			public void dragEnter(DropTargetEvent event) {
				if (event.detail == DND.DROP_DEFAULT) {
					event.detail = DND.DROP_COPY;
				}
			}

			@Override
			public void drop(DropTargetEvent event)
			{
				if (event.data == null)
				{
					event.detail = DND.DROP_NONE;
					return;
				}
				String [] names = (String [])event.data;
				
				for (int i=0;i<names.length;i++)
				appendFileContents(names[i]);
			}
		});

		addDisposeListener(new DisposeListener()
		{
			public void widgetDisposed(DisposeEvent e)
			{
				smallFont.dispose();
			}
		});
	}

	/**
	 * Returns the total number of entries.
	 * 
	 * @return
	 */
	public int getNumberOfEntries()
	{
		int chars = text.getCharCount();
		int lineCount = text.getLineCount();
		if (chars > 0)
		{
			String last = text.getText(chars-1,chars-1);
			if (last.equals("\n"))
				lineCount--;
		}
		return lineCount;
	}
	
	/**
	 * Returns the number of known entries.
	 * 
	 * @return
	 */
	public int getNumberOfKnownEntries()
	{
		int known = -1;
		
		if (assoc != null)
		{
			known = 0;
			for (String l : getLines())
			{
				String gene = getGeneName(l);
				Gene2Associations gene2Associations = assoc.get(new ByteString(gene));
				if (gene2Associations != null)
					known++;
			}
		}
		
		return known;
	}
	
	public String getText()
	{
		return text.getText();
	}
	
	public void setText(String text)
	{
		this.text.setText(text);
	}
	
	/**
	 * Returns whether the document is empty.
	 * 
	 * @return
	 */
	public boolean isEmpty()
	{
		return this.text.getCharCount()==0;
	}
	
	/**
	 * Returns the genename part of the given line.
	 * @param line
	 * @return
	 */
	private String getGeneName(String line)
	{
		/* If full line is gene name than we accept it as gene name */
		if (assoc.containsGene(new ByteString(line.trim())))
			return line;
		
		int pos = line.indexOf(' ');
		if (pos == -1) pos = line.indexOf('\t');
		if (pos != -1) line = line.substring(0, pos);

		return line; 
	}
	
	/**
	 * Appends the contents of the given file.
	 * 
	 * @param name
	 */
	public void appendFileContents(String name)
	{
		boolean wasEmpty = text.getCharCount() == 0;

		BufferedReader is;
		try
		{
			is = new BufferedReader(new FileReader(name));
			StringBuilder sb = new StringBuilder();
			String inputLine;

			while ((inputLine = is.readLine()) != null)
			{
				sb.append(inputLine);
				sb.append("\n");
			}
			text.append(sb.toString());

			if (wasEmpty)
			{
				for (INewNameListener newNameListener : newNameListeners)
					newNameListener.newName(new File(name).getName());
			}
		} catch (Exception e)
		{
			Ontologizer.logException(e);
		}
	}
	
	public void setWorkSet(WorkSet ws)
	{
		if (displayedWorkSet != null)
			WorkSetLoadThread.releaseDatafiles(displayedWorkSet);

		displayedWorkSet = ws.clone();
		graph = null;
		assoc = null;
		setAllButton.setEnabled(false);

		WorkSetLoadThread.obtainDatafiles(ws, 
				new Runnable(){
					public void run()
					{
						text.getDisplay().asyncExec(new Runnable()
						{
							public void run()
							{
								graph = WorkSetLoadThread.getGraph(displayedWorkSet.getOboPath());
								assoc = WorkSetLoadThread.getAssociations(displayedWorkSet.getAssociationPath());
								text.redraw();
								setAllButton.setEnabled(true);
								if (datasetsLoadedAction != null) datasetsLoadedAction.act();
							}
						});

					}});
	}
	
	/**
	 * Add an action that is called as soon as a datafile has been successfully loaded.
	 * 
	 * @param loadedAction
	 */
	public void addDatafilesLoadedListener(ISimpleAction loadedAction)
	{
		datasetsLoadedAction = loadedAction;
	}
	
	/**
	 * Returns the contents as an array of lines.
	 * 
	 * @return
	 */
	public String [] getLines()
	{
		String [] genes = text.getText().split("\n");
		for (int i=0;i<genes.length;i++)
			genes[i] = genes[i].trim();
		return genes;
	}

	/**
	 * Clears the contents of this editor.
	 */
	public void clear()
	{
		text.replaceTextRange(0, text.getCharCount(), "");
	}
	
	/**
	 * Adds a new name listener which is called whenever the
	 * name of the gene set would change (only the case, if the
	 * document was empty and a file was appended/loaded)
	 * 
	 * @param newNameListener
	 */
	public void addNewNameListener(INewNameListener newNameListener)
	{
		newNameListeners.add(newNameListener);
	}
	
	/**
	 * Removes the new name listener.
	 * 
	 * @param newNameListener
	 */
	public void removeNewNameListener(INewNameListener newNameListener)
	{
		newNameListeners.remove(newNameListener);
	}
	
	/**
	 * Sets a standard tool tip text which is displayed, if no
	 * other tool tip can be displayed. 
	 */
	@Override
	public void setToolTipText(String string)
	{
		staticToolTipText = string;
	}

	/**
	 * Displays the static tool tip.
	 * 
	 * @param x
	 * @param y
	 */
	private void displayStaticToolTip(int x, int y)
	{
		if (staticToolTipText != null)
		{
			tipShellStyledText.setText(staticToolTipText);
			tipShell.layout();
			tipShell.pack();
			tipShell.setLocation(x - tipShell.getSize().x / 2, y);
			tipShell.setVisible(true);
		}
	}
}
