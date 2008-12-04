package ontologizer.gui.swt.support;

/*************************************************************
 $Id: GraphCanvas.java 150 2007-09-14 14:54:47Z sba $
 
 This class is copyright 2006 by Sebastian Bauer and released
 under the terms of the CPL.		
 See http://www.opensource.org/licenses/cpl.php for more 
 information.
*************************************************************/

import java.io.File;
import java.io.FileInputStream;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.TypedListener;

import att.grappa.Edge;
import att.grappa.Graph;
import att.grappa.Node;
import att.grappa.Parser;

/**
 * A canvas displaying a graph.
 * 
 * @author Sebastian Bauer <mail@sebastianbauer.info>
 */
public class GraphCanvas extends Canvas implements IGraphCanvas
{
	private Graph g;
	private GraphPaint gp;
	
	private boolean scaleToFit;
	
	private float scale;
	private float xoffset;
	private float yoffset;

	private MenuItem zoomInItem;
	private MenuItem zoomOutItem;
	private MenuItem zoomReset;
	private MenuItem scaleToFitItem;
	
	private ScrollBar horizontalScrollBar;
	private ScrollBar verticalScrollBar;

	private GraphCanvas thisCanvas;

	private boolean mouseDown;
	private int mouseCenterX;
	private int mouseCenterY;
	private float oldXOffset;
	private float oldYOffset;
	private float oldScale;
	private boolean zoomMove;

	/**
	 * Constructs a new graph canvas.
	 * 
	 * @param parent
	 * @param style
	 */
	public GraphCanvas(Composite parent, int style)
	{
		super(parent, style | SWT.NO_BACKGROUND | SWT.DOUBLE_BUFFERED | SWT.HORIZONTAL | SWT.VERTICAL);

		thisCanvas = this;

		scaleToFit = true;
		horizontalScrollBar = getHorizontalBar();
		verticalScrollBar = getVerticalBar();
		
		g = new Graph("empty");
		prepareGraph();

		/* Hide the scrollbars */
		horizontalScrollBar.setVisible(false);
		verticalScrollBar.setVisible(false);

		/* Add our control listener */
		addControlListener(new ControlAdapter()
		{
			@Override
			public void controlResized(ControlEvent e)
			{
				if (scaleToFit) updateTransformation();
				updateScrollers();
			}
		});

		/* Mouse tracker */
		addMouseTrackListener(new MouseTrackAdapter()
		{
			public void mouseHover(MouseEvent e)
			{
				/* Build the inverted transformation */
				Transform transform = buildTransform();
				transform.invert();

				float [] points = new float[2];
				points[0] = (float)e.x;
				points[1] = (float)e.y;
				transform.transform(points);

				Edge edge = gp.findEdgeByCoord(points[0],points[1]);
				if (edge != null)
				{
					String tip = (String)edge.getAttributeValue("tooltip");
					setToolTipText(tip);
				} else setToolTipText(null);
			}
		});

		/* Add our mouse listener */
		addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseDown(MouseEvent e)
			{
				/* Build the inverted transformation */
				Transform transform = buildTransform();
				transform.invert();

				float [] points = new float[2];
				points[0] = (float)e.x;
				points[1] = (float)e.y;
				transform.transform(points);

				Node n = gp.findNodeByCoord(points[0],points[1]);
				if (n != null)
				{
					gp.setSelectedNode(n);
					redraw();

					/* emit event */
					Event ev = new Event();
				    ev.widget = thisCanvas;
				    ev.type = SWT.Selection;
				    ev.text = getNameOfCurrentSelectedNode();
				    notifyListeners(SWT.Selection, ev);
				} else
				{
					if (e.button == 1 || e.button == 2)
					{
						mouseDown = true;
						mouseCenterX = e.x;
						mouseCenterY = e.y;
						oldXOffset = xoffset;
						oldYOffset = yoffset;
						oldScale = scale;

						zoomMove = e.button == 2;
					}
				}
			}
			
			@Override
			public void mouseUp(MouseEvent e)
			{
				mouseDown = false;
			}
		});

		/* Add mouse move listener */
		addMouseMoveListener(new MouseMoveListener()
		{
			public void mouseMove(MouseEvent e)
			{
				if (mouseDown)
				{
					if (zoomMove)
					{
						setScale(oldScale * ((float)mouseCenterY / (float)e.y));
					} else
					{
						xoffset = oldXOffset + (mouseCenterX - e.x);
						yoffset = oldYOffset + (mouseCenterY - e.y);
						updateScrollers();
						redraw();
					}
				}
			}
		});

		/* Add our paint listener */
		addPaintListener(new PaintListener()
		{
			public void paintControl(PaintEvent e)
			{
				GC gc = e.gc;

				gc.setAntialias(SWT.ON);
				gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_WHITE));
				gc.fillRectangle(getClientArea());

				Transform transform = buildTransform();
				gc.setTransform(transform);
				gp.drawGraph(gc);
				gc.setTransform(null);
				transform.dispose();
			}
		});

		/* Add the dispose listener */
		addDisposeListener(new DisposeListener()
		{
			public void widgetDisposed(DisposeEvent e)
			{
				cleanupGraph();
			}
		});

		Menu menu = new Menu(this);
		zoomInItem = new MenuItem(menu,0);
		zoomInItem.setText("Zoom In");
		
		zoomOutItem = new MenuItem(menu,0);
		zoomOutItem.setText("Zoom Out");
		
		zoomReset = new MenuItem(menu, 0);
		zoomReset.setText("Reset Zoom");
		
		scaleToFitItem = new MenuItem(menu,SWT.CHECK|SWT.TOGGLE);
		scaleToFitItem.setText("Scale To Fit");
		scaleToFitItem.setSelection(scaleToFit);

		setMenu(menu);

		/* Add selection listener for the menu */
		SelectionListener selListener = new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				if (e.widget.equals(zoomInItem))
					zoomIn();
				else if (e.widget.equals(zoomOutItem))
					zoomOut();
				else if (e.widget.equals(scaleToFitItem))
					setScaleToFit(scaleToFitItem.getSelection());
				else if (e.widget.equals(zoomReset))
					zoomReset();
			}
		};
		zoomInItem.addSelectionListener(selListener);
		zoomOutItem.addSelectionListener(selListener);
		zoomReset.addSelectionListener(selListener);
		scaleToFitItem.addSelectionListener(selListener);
		
		/* Add selection listener for the bar */
		SelectionListener barListener = new SelectionAdapter()
		{
			public void widgetSelected(SelectionEvent e)
			{
				if (e.widget.equals(horizontalScrollBar))
					xoffset = getHorizontalBar().getSelection();
				else if (e.widget.equals(verticalScrollBar))
					yoffset = getVerticalBar().getSelection();

				redraw();
			}
		};

		horizontalScrollBar.addSelectionListener(barListener);
		verticalScrollBar.addSelectionListener(barListener);
	}

	/**
	 * Cleanup the graph.
	 */
	private void cleanupGraph()
	{
		if (gp != null)
			gp.dispose();
		gp = null;
	}
	
	/**
	 * Prepare for the current graph.
	 */
	private void prepareGraph()
	{
		cleanupGraph();
		gp = new GraphPaint(getDisplay(),g);
	}

	/**
	 * Short cut for getting the width of the displayed graph.
	 * 
	 * @return
	 */
	private float getGraphWidth()
	{
		return (float)(g.getBoundingBox().getMaxX() - g.getBoundingBox().getMinX() + 1);
	}
	
	/**
	 * Short cut for getting the height of the displayed graph.
	 * 
	 * @return
	 */
	private float getGraphHeight()
	{
		return (float)(g.getBoundingBox().getMaxY() - g.getBoundingBox().getMinY() + 1);
	}

	/**
	 * Updates transformation for scale to fit.
	 */
	private void updateTransformation()
	{
		/* Real graph Dimensions */
		float graphWidth = getGraphWidth();
		float graphHeight = getGraphHeight();

		/* Zoom factor */
		scale = Math.min(((float)getClientArea().width)/graphWidth,((float)getClientArea().height)/graphHeight)/1.005f;
		xoffset = 0;
		yoffset = 0;
	}

	/**
	 * Updates the scroller (i.e. if they are visible and their values)
	 */
	private void updateScrollers()
	{
		getHorizontalBar().setValues((int)xoffset,0,(int)(getGraphWidth()*scale),getClientArea().width,1,getClientArea().width-1);
		getVerticalBar().setValues((int)yoffset,0,(int)(getGraphHeight()*scale),getClientArea().height,1,getClientArea().height-1);

		if (getGraphWidth() * scale > getClientArea().width)
			getHorizontalBar().setVisible(true);
		else
			getHorizontalBar().setVisible(false);

		if (getGraphHeight() * scale > getClientArea().height)
			getVerticalBar().setVisible(true);
		else
			getVerticalBar().setVisible(false);
	}

	/**
	 * Returns the name of the currently selected Node.
	 * 
	 * @return
	 */
	public String getNameOfCurrentSelectedNode()
	{
		if (gp.getSelectedNode() != null)
			return gp.getSelectedNode().getName();
		return null;
	}

	/**
	 * Selectes the node with the given name. Other nodes are deselected.
	 * 
	 * @param name
	 */
	public void selectNode(String name)
	{
		Node node = g.findNodeByName(name);
		if (node != gp.getSelectedNode())
		{
			
			gp.setSelectedNode(node);
			redraw();
		}
	}

	public void setLayoutedDotFile(File file) throws Exception
	{
		Parser parser = new Parser(new FileInputStream(file), System.err);
		parser.parse();

		gp.setSelectedNode(null);
		cleanupGraph();

		g = parser.getGraph();
		g.setEditable(false);

		prepareGraph();

		updateTransformation();
		redraw();
	}

	class ZoomRunnable implements Runnable
	{
		private float oldScale;
		private float newScale;

		private long startTime;
		private long duration;
		private long endTime;

		private float centerX;
		private float centerY;

		public void initialize(float newScale)
		{
			startTime = System.currentTimeMillis();
			duration = 250;
			endTime = startTime + duration;
			oldScale = scale;

			centerX = (Math.min((float)getClientArea().width,getGraphWidth()*scale) / 2f + xoffset) / scale;
			centerY = (Math.min((float)getClientArea().height,getGraphHeight()*scale) / 2f + yoffset) / scale;
			this.newScale = newScale;
		}

		public void run()
		{
			if (isDisposed() || scaleToFit)
				return;

			long newTime = System.currentTimeMillis();
			if (newTime < endTime)
				getDisplay().timerExec(33, this);
			else newTime = endTime;
			
			long diffTime = newTime - startTime;

			scale = oldScale - (oldScale - newScale) * ((float)diffTime / (float)duration);

			/* Update the xoffset and yoffsets as follow: Determine the current
			 * center and adapt the offsets after scaling to match the center.
			 */
			xoffset = centerX * scale - Math.min((float)getClientArea().width,getGraphWidth()*scale) / 2f;
			yoffset = centerY * scale - Math.min((float)getClientArea().height,getGraphHeight()*scale) / 2f;

			updateScrollers();
			redraw();
			update();
		}
	};
	
	private ZoomRunnable zoomRunnable = new ZoomRunnable();

	
	/**
	 * Sets a new magnification. Ensures that the center point stays the same.
	 * 
	 * @param newScale
	 */
	private void setScale(float newScale)
	{
		scaleToFit = false;
		scaleToFitItem.setSelection(false);

		zoomRunnable.initialize(newScale);
		getDisplay().timerExec(10, zoomRunnable);
	}

	/**
	 * Zoom out.
	 */
	public void zoomOut()
	{
		setScale(scale / 1.5f);
	}

	/**
	 * Zoom in.
	 */
	public void zoomIn()
	{
		setScale(scale * 1.5f);
	}

	/**
	 * Reset the zoom.
	 */
	public void zoomReset()
	{
		setScale(1.0f);
	}

	/**
	 * Set whether the magnification should be chosen such that, the complete
	 * graph is visible (also after resizing).
	 * 
	 * @param selection
	 */
	public void setScaleToFit(boolean selection)
	{
		this.scaleToFit = selection;
		if (selection)
		{
			updateTransformation();
			updateScrollers();
			redraw();
		}
	}
	
	/**
	 * Add a new selection listener. The text field is used
	 * for the node's name.
	 * 
	 * @param sel
	 */
	public void addSelectionListener(SelectionListener sel)
	{
		TypedListener listener = new TypedListener(sel);
		addListener(SWT.Selection, listener);
	}

	public void addMouseListener(MouseListener mouse)
	{
		TypedListener typedListener = new TypedListener(mouse);
		addListener(SWT.MouseDown, typedListener);
		addListener(SWT.MouseUp, typedListener);
		addListener(SWT.MouseDoubleClick, typedListener);
	}

	/**
	 * Builds an transformation from the graph coordinate system to
	 * the display coordinate system.
	 * 
	 * @return
	 */
	private Transform buildTransform()
	{
		return buildTransform(xoffset, yoffset, scale);
	}


	/**
	 * Build an transformation from the graph coordinate system to
	 * the display coordinate system.
	 * 
	 * @param xoffset
	 * @param yoffset
	 * @param scale
	 * @return
	 */
	private Transform buildTransform(float xoffset, float yoffset, float scale)
	{
		Transform transform = new Transform(getDisplay());

		float alignedXOffset = -xoffset;
		float alignedYOffset = getClientArea().height - 2 - yoffset;

		if (getGraphWidth() * scale < getClientArea().width)
			alignedXOffset += (getClientArea().width - getGraphWidth() * scale)/2; 

		if (getGraphHeight() * scale < getClientArea().height)
			alignedYOffset -= (getClientArea().height - getGraphHeight() * scale)/2;
		else
		{
			alignedYOffset += getGraphHeight() * scale - getClientArea().height;
		}

		transform.translate(alignedXOffset, alignedYOffset);
		transform.scale(scale,scale);
		return transform;
	}
}
