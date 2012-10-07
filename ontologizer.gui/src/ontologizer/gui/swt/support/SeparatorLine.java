/*
 * Created on 19.04.2006
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ontologizer.gui.swt.support;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

/**
 * This class implements a simple separator line.
 * 
 * @author Sebastian Bauer
 */
final public class SeparatorLine extends Canvas
{
	private final static int TEXT_OFFSET = 4;

	private String text;

	public SeparatorLine(Composite parent, int style)
	{
		super(parent, style);
		
		addPaintListener(new PaintListener(){
			public void paintControl(org.eclipse.swt.events.PaintEvent e)
			{
				Rectangle area = getClientArea();
				Display display = getDisplay();
				GC gc = e.gc;

				Color shadow = display.getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW);
				Color highlight = display.getSystemColor(SWT.COLOR_WIDGET_HIGHLIGHT_SHADOW);
				Color foreground = display.getSystemColor(SWT.COLOR_WIDGET_FOREGROUND);

				if (text != null)
				{
					Point ext = gc.textExtent(text);
					int w = ext.x;
					int h = ext.y;
					int y = area.y + h / 2;
					drawSeparator(gc, area.x, y, area.x + TEXT_OFFSET - 1, y, shadow, highlight);
					gc.setForeground(foreground);
					gc.drawText(text, area.x + TEXT_OFFSET, area.y);
					drawSeparator(gc, area.x + TEXT_OFFSET + w, y, area.x + area.width - 1, y, shadow, highlight);
				} else
				{
					drawSeparator(gc,area.x, area.y,area.x + area.width - 1,area.y,shadow,highlight);
				}
			
			};
		});
	}
	
	private void drawSeparator(GC gc, int x1, int y1, int x2, int y2, Color shadow, Color highlight)
	{
		gc.setForeground(shadow);
		gc.drawLine(x1,y1,x2,y2);
		gc.setForeground(highlight);
		gc.drawLine(x1,y1+1,x2,y2+1);
	}

	public void setText(String text)
	{
		this.text = text;
	}
	
	public String getText()
	{
		return text;
	}
	
	@Override
	public Point computeSize(int wHint, int hHint, boolean changed)
	{
		if (text == null) return new Point(100,2);
		GC gc = new GC(getDisplay());
		Point p = gc.textExtent(text);
		p.x += 2 * TEXT_OFFSET;
		gc.dispose();
		return p;
	}
}
