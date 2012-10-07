package ontologizer.gui.swt.support;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

/**
 * A widget to draw a basic xy chart.
 * 
 * @author Sebastian Bauer
 *
 */
public class ScatterPlot extends Canvas
{
	/* Context menu support */
	private Menu contextMenu;
	private ArrayList<MenuItem> contextMenuItemList;

	/**
	 * Class for the chart model.
	 * 
	 * @author Sebastian Bauer
	 */
	public static class XYSeries
	{
		private double [] x;
		private double [] y;
		private String [] labels;
		
		private double maxX;
		private double maxY;

		private String name;

		private boolean showMe = true;

		public void setX(double [] x)
		{
			this.x = x;
			maxX = Double.MIN_VALUE;
			for (int i=0;i<x.length;i++)
			{
				if (x[i] > maxX)
					maxX = x[i];
			}
		}

		public void setX(float[] x)
		{
			this.x = new double[x.length];
			maxX = Double.MIN_VALUE;
			for (int i=0;i<x.length;i++)
			{
				this.x[i] = x[i];
				if (x[i] > maxX)
					maxX = x[i];
			}
		}

		public void setLabels(String [] labels)
		{
			this.labels = labels;
		}

		public void setY(double [] y)
		{
			this.y = y;
			maxY = Double.MIN_VALUE;
			for (int i=0;i<y.length;i++)
			{
				if (y[i] > maxY)
					maxY = y[i];
			}

			if (x == null)
			{
				x = new double[y.length];
				for (int i=0;i<y.length;i++)
					x[i] = i;
				maxX = y.length;
			}
		}

		public void setY(float [] y)
		{
			this.y = new double[y.length];
			
			maxY = Double.MIN_VALUE;
			for (int i=0;i<y.length;i++)
			{
				this.y[i] = y[i];
				if (y[i] > maxY)
					maxY = y[i];
			}

			if (x == null)
			{
				x = new double[y.length];
				for (int i=0;i<y.length;i++)
					x[i] = i;
				maxX = y.length;
			}
		}

		public void setY(int [] y)
		{
			this.y = new double[y.length];
			
			maxY = Double.MIN_VALUE;
			for (int i=0;i<y.length;i++)
			{
				this.y[i] = y[i];
				if (y[i] > maxY)
					maxY = y[i];
			}

			if (x == null)
			{
				x = new double[y.length];
				for (int i=0;i<y.length;i++)
					x[i] = i;
				maxX = y.length;
			}
		}

		public void setName(String name)
		{
			this.name = name;
		}

		public String getName()
		{
			return name;
		}
		
		public void store(FileOutputStream fs)
		{
			PrintStream ps = new PrintStream(fs);
			for (int i = 0; i < x.length; i++)
				ps.print(x[i] + "\t" + y[i] + "\n");
			ps.flush();
			ps.close();
		}
		
		public boolean hasShowMeFlag()
		{
			return showMe;
		}
		
		public void setShowMeFlag(boolean disp)
		{
			showMe = disp;
		}
	};

	private LinkedList<XYSeries> seriesList = new LinkedList<XYSeries>();

	private double maxX = Double.MIN_VALUE;
	private double maxY = Double.MIN_VALUE;

	private String xAxisTitle;
	private String yAxisTitle;

	public ScatterPlot(Composite parent, int style)
	{
		super(parent, style | SWT.NO_BACKGROUND);

		/* context menu */
		contextMenuItemList = new ArrayList<MenuItem>();
		contextMenu = new Menu(this);
		setMenu(contextMenu);
		contextMenu.addMenuListener(new MenuListener()
		{
			private void disposeItems()
			{
				for (MenuItem item : contextMenuItemList)
					item.dispose();
				contextMenuItemList.clear();
			}

			public void menuHidden(MenuEvent e)
			{
			}
			
			public void menuShown(MenuEvent e)
			{
				disposeItems();
				for (XYSeries xySeries : seriesList)
				{
					MenuItem mi = new MenuItem(contextMenu,SWT.CHECK|SWT.TOGGLE);
					mi.setText("Show " + xySeries.getName());
					mi.setSelection(xySeries.hasShowMeFlag());
					mi.setData("xySeries", xySeries);
					mi.addSelectionListener(new SelectionAdapter()
					{
						@Override
						public void widgetSelected(SelectionEvent e)
						{
							XYSeries xySeries = (XYSeries)e.widget.getData("xySeries");
							xySeries.setShowMeFlag(((MenuItem)e.widget).getSelection());
							calculateDomain();
							redraw();
						}
					});
					contextMenuItemList.add(mi);
				}
			}
		});
	
		/* The paint listener */
		addPaintListener(new PaintListener(){
			public void paintControl(PaintEvent e)
			{
				Rectangle clientArea = getClientArea();
				e.gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_WHITE));
				e.gc.fillRectangle(clientArea);

				int axesLeft = clientArea.x + 2;
				int axesTop = clientArea.y + 2;
				int axesRight = clientArea.x + clientArea.width - 4;
				int axesBottom = clientArea.y + clientArea.height - 4;

				Point xAxisTitleDimension;
				if (xAxisTitle != null)
					xAxisTitleDimension = e.gc.textExtent(xAxisTitle);
				else xAxisTitleDimension = new Point(0,0);

				Point yAxisTitleDimension;
				if (yAxisTitle != null)
					yAxisTitleDimension = e.gc.textExtent(yAxisTitle);
				else yAxisTitleDimension = new Point(0,0);

				/* Draw title of y-axis */
				if (yAxisTitle != null)
				{
					Transform yAxisTransform = new Transform(getDisplay());
					yAxisTransform.rotate(90);
					yAxisTransform.translate((axesBottom - axesTop + 1 - yAxisTitleDimension.x) / 2, -yAxisTitleDimension.y);
					e.gc.setTransform(yAxisTransform);
					e.gc.drawText(yAxisTitle,0,0);
					e.gc.setTransform(null);
					yAxisTransform.dispose();
				}

				/* Draw title of x-axis */
				if (xAxisTitle != null)
				{
					e.gc.drawText(xAxisTitle, (axesRight - axesLeft + 1 - xAxisTitleDimension.x)/2, axesBottom - xAxisTitleDimension.y + 1);
				}

				/* Adjust axes */
				axesLeft += yAxisTitleDimension.y;
				axesBottom -= xAxisTitleDimension.y;

				Iterator<XYSeries> iter = seriesList.iterator();

				Color [] colors = new Color[5];
				colors[0] = getDisplay().getSystemColor(SWT.COLOR_BLACK);
				colors[1] = getDisplay().getSystemColor(SWT.COLOR_BLUE);
				colors[2] = getDisplay().getSystemColor(SWT.COLOR_GREEN);
				colors[3] = getDisplay().getSystemColor(SWT.COLOR_RED);
				colors[4] = getDisplay().getSystemColor(SWT.COLOR_GRAY);

				int currentColor = 0;

				boolean drawYTicks = true;
				
				int areaHeight = axesBottom - axesTop + 1;

				/* Draw ticks of y-axis */
				if (drawYTicks && maxY != Double.MIN_VALUE)
				{
					int fontHeight = e.gc.getFontMetrics().getHeight();

					/* Number of ticks */
					int nTicks = (int)(axesBottom - axesTop) / (fontHeight + 12);

					/* Spaces between the ticks insise the coordinate domain */
					double space = maxY / nTicks;
					double roundedSpace = Math.pow(10,Math.ceil(Math.log10(space)));
					int newNTicks = (int)(maxY / roundedSpace);

					if (newNTicks < 2 && nTicks > 5)
					{
						roundedSpace /= 2;
						newNTicks = (int)(maxY / roundedSpace);
					}

					/* Space of a tick inside pixel domain */
					double areaSpace = areaHeight * roundedSpace / maxY;

					int labelWidth = 0;
					
					final String fmtString = "%.2g";
					
					for (int i=1;i<=newNTicks;i++)
					{
						int nlw = e.gc.textExtent(String.format(fmtString,roundedSpace * i)).x;
						if (nlw > labelWidth) labelWidth = nlw;
					}

					axesLeft += labelWidth + 2;

					for (int i=1;i<=newNTicks;i++)
					{
						e.gc.drawLine(axesLeft - 1, (int)(axesBottom - i * areaSpace), axesLeft + 1, (int)(axesBottom - i * areaSpace));
						
						int w = e.gc.textExtent(String.format(fmtString,roundedSpace * i)).x;
						e.gc.drawText(String.format(fmtString,roundedSpace * i),axesLeft - w - 2,(int)(axesBottom - i * areaSpace) - fontHeight/2);
					}
				}

				int areaLeft = axesLeft;
				int areaTop = axesTop;
				int areaWidth = axesRight - axesLeft + 1;

				/* Draw axes */
				e.gc.drawLine(axesLeft, axesTop, axesLeft, axesBottom);
				e.gc.drawLine(axesLeft, axesBottom, axesRight, axesBottom);
				
				while (iter.hasNext())
				{
					XYSeries series = iter.next();
					
					String [] labels = series.labels;
					
					if (!series.hasShowMeFlag())
					{
						if (++currentColor == colors.length) currentColor = 0;
						continue;
					}
					
					e.gc.setForeground(colors[currentColor++]);
					if (currentColor == colors.length) currentColor = 0;

					e.gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_GRAY));

					for (int i = 0; i<series.x.length;i++)
					{
						int x = (int)(series.x[i] * areaWidth / maxX);
						int y = (int)(series.y[i] * areaHeight / maxY);
						int x1 = areaLeft + (int)x;
						int y1 = areaTop + areaHeight - (int)y - 1;
							
						if (labels != null)
						{
							Point ext = e.gc.textExtent(labels[i]);
							
							x1 -= (ext.x/2);
							
							if (x1 < areaLeft) x1 = areaLeft;
							else if (x1 + ext.x > areaLeft + areaWidth) x1 = areaLeft + areaWidth - ext.x;
							
							e.gc.drawText(labels[i], x1, y1, true);
						}
					}

					e.gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_BLACK));
					e.gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_BLACK));

					for (int i = 0; i<series.x.length;i++)
					{
						int x = (int)(series.x[i] * areaWidth / maxX);
						int y = (int)(series.y[i] * areaHeight / maxY);

						int x1 = areaLeft + (int)x;
						int y1 = areaTop + areaHeight - (int)y - 1;

						e.gc.fillOval(x1-1, y1-1, 3, 3);
					}
				}

				/* Determine the dimension of the legend */
				Point legendDims = new Point(0,0);
				iter = seriesList.iterator();
				while (iter.hasNext())
				{
					XYSeries series = iter.next();
					String name = series.getName();
					if (name != null && name.length() > 0)
					{
						Point p = e.gc.textExtent(name);
						if (p.x > legendDims.x) legendDims.x = p.x;

						legendDims.y += p.y;
					}
				}

				if (legendDims.x > 0)
				{
					legendDims.x += 20;
					legendDims.y += (seriesList.size() - 1)*2;
					
					int legendX = axesRight - legendDims.x - 2;
					int legendY = axesBottom - legendDims.y - 2;

					currentColor = 0;
					iter = seriesList.iterator();
					while (iter.hasNext())
					{
						XYSeries series = iter.next();
						String name = series.getName();

						if (name != null)
						{
							Point dim = e.gc.textExtent(name);
							e.gc.setForeground(colors[currentColor]);
							e.gc.drawLine(legendX + 2, legendY + dim.y / 2, legendX + 14, legendY + dim.y / 2);
							e.gc.setForeground(colors[0]);
							e.gc.drawText(name, legendX + 18, legendY);
							legendY +=  dim.y + 2;
						}
						if (++currentColor == colors.length)
							currentColor = 0;
					}
				}
			}
		});
	}

	private void calculateDomain()
	{
		maxX = Double.MIN_VALUE;
		maxY = Double.MIN_VALUE;

		for (XYSeries series : seriesList)
		{
			if (series.hasShowMeFlag())
			{
				if (series.maxX > maxX) maxX = series.maxX;
				if (series.maxY > maxY) maxY = series.maxY;
			}
		}
	}

	public void removeAllXYSeries()
	{
		maxX = Double.MIN_VALUE;
		maxY = Double.MIN_VALUE;

		seriesList.clear();
		redraw();
	}

	/**
	 * Add a new series to the chart.
	 * 
	 * @param series
	 */
	public void addXYSeries(XYSeries series)
	{
		seriesList.add(series);
		if (series.hasShowMeFlag())
		{
			if (series.maxX > maxX) maxX = series.maxX;
			if (series.maxY > maxY) maxY = series.maxY;
		}
		redraw();
	}
	
	/**
	 * Set the title for the x-axis.
	 * 
	 * @param title
	 */
	public void setXAxisTitle(String title)
	{
		xAxisTitle = title;
	}

	/**
	 * Set the title for the y-axis.
	 * 
	 * @param title
	 */
	public void setYAxisTitle(String title)
	{
		yAxisTitle = title;
	}
}
