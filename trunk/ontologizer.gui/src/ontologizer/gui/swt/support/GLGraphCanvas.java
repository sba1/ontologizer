package ontologizer.gui.swt.support;

import java.awt.geom.PathIterator;
import java.io.File;
import java.io.FileInputStream;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Path;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.opengl.GLCanvas;
import org.eclipse.swt.opengl.GLData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.util.glu.GLU;

import att.grappa.Edge;
import att.grappa.Element;
import att.grappa.Graph;
import att.grappa.GraphEnumeration;
import att.grappa.GrappaConstants;
import att.grappa.GrappaPoint;
import att.grappa.GrappaStyle;
import att.grappa.Node;
import att.grappa.Parser;
import att.grappa.Subgraph;

public class GLGraphCanvas extends Composite implements IGraphCanvas
{
	private GLCanvas canvas;
	private Graph g;
	
	public GLGraphCanvas(Composite parent, int style)
	{
		super(parent, style);
		
		setLayout(new FillLayout());
		
		GLData data = new GLData();
		data.doubleBuffer = true;

		canvas = new GLCanvas(this, 0, data);
		canvas.addPaintListener(new PaintListener() {
			
			@Override
			public void paintControl(PaintEvent e) {
				drawGraph(g);
			}
		});
		
//		canvas.addListener(SWT.Resize, new Listener() {
//			public void handleEvent(Event event) {
////				Rectangle bounds = canvas.getBounds();
////				float fAspect = (float) bounds.width / (float) bounds.height;
////				canvas.setCurrent();
////				try {
////					GLContext.useContext(canvas);
////				} catch(LWJGLException e) { e.printStackTrace(); }
////				GL11.glViewport(0, 0, bounds.width, bounds.height);
////				GL11.glMatrixMode(GL11.GL_PROJECTION);
////				GL11.glLoadIdentity();
////				GLU.gluPerspective(45.0f, fAspect, 0.5f, 400.0f);
////				GL11.glMatrixMode(GL11.GL_MODELVIEW);
////				GL11.glLoadIdentity();
//				drawGraph(g);
//			}
//		});

	}

	@Override
	public void setLayoutedDotFile(File dotFile) throws Exception
	{
		Parser parser = new Parser(new FileInputStream(dotFile), System.err);
		parser.parse();

		g = parser.getGraph();
		g.setEditable(false);

		drawGraph(g);
	}

	private void drawGraph(Graph g)
	{
		if (g == null)
			return;

		canvas.setCurrent();
		try
		{
			GLContext.useContext(canvas);

			GL11.glMatrixMode(GL11.GL_PROJECTION);
			GL11.glLoadIdentity();
			GL11.glOrtho(-100, 800, 100, -600, 1, -1);
			GL11.glMatrixMode(GL11.GL_MODELVIEW);

			GL11.glClearColor(1.0f, 1.0f, 1.0f, 0.0f);
			GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

			drawGraphElement(g);

			canvas.swapBuffers();
		} catch (LWJGLException e) { e.printStackTrace(); }
	}

	private void drawNode(Node node)
	{
		GrappaPoint center = node.getCenterPoint();

		int x = (int)center.x;
		int y = (int)center.y;
		double h = (Double)node.getAttributeValue(Node.HEIGHT_ATTR);
		String label = (String)node.getAttributeValue(Node.LABEL_ATTR);
		GrappaStyle style = (GrappaStyle)node.getAttributeValue(Node.STYLE_ATTR);
		@SuppressWarnings("unchecked")
		List<java.awt.Color> fillColorAWT = (List<java.awt.Color>)node.getAttributeValue(Node.FILLCOLOR_ATTR);
		int shape = (Integer)node.getAttributeValue(Node.SHAPE_ATTR);

		GL11.glColor3f(0f,0f,0.0f);
		PathIterator iter = node.getGrappaNexus().getPathIterator();
		float [] coords = new float[6]; 
		while (!iter.isDone())
		{
			int type = iter.currentSegment(coords);

			System.out.print(type + " ");
			for (int i=0;i<6;i++) System.out.print(coords[i] + " ");
			System.out.println();

			switch (type)
			{
				case	PathIterator.SEG_MOVETO:
						GL11.glBegin(GL11.GL_POLYGON);
						GL11.glVertex2f(coords[0], coords[1]);
						
						break;

				case	PathIterator.SEG_LINETO:
						GL11.glVertex2f(coords[0], coords[1]);
						break;

				case	PathIterator.SEG_CLOSE:
						GL11.glEnd();
						break;

				case	PathIterator.SEG_QUADTO:
						break;

				case	PathIterator.SEG_CUBICTO:
						GL11.glVertex2f(coords[0], coords[1]);
						GL11.glVertex2f(coords[2], coords[3]);
						GL11.glVertex2f(coords[4], coords[5]);
						break;
			}
				
			iter.next();
		}

		
		if (fillColorAWT != null && fillColorAWT.size() > 0)
		{
			GL11.glShadeModel(GL11.GL_SMOOTH);
			GL11.glEnable(GL11.GL_BLEND);
			
			GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE);
			GL11.glBegin(GL11.GL_POLYGON);
			GL11.glColor3ub((byte)fillColorAWT.get(0).getRed(),(byte)fillColorAWT.get(0).getGreen(),(byte)fillColorAWT.get(0).getBlue());
			GL11.glVertex2f(node.getGrappaNexus().getBounds().x,node.getGrappaNexus().getBounds().y);
			GL11.glVertex2f(node.getGrappaNexus().getBounds().x+node.getGrappaNexus().getBounds().width,node.getGrappaNexus().getBounds().y);
			if (fillColorAWT.size() > 1)
				GL11.glColor3ub((byte)fillColorAWT.get(1).getRed(),(byte)fillColorAWT.get(1).getGreen(),(byte)fillColorAWT.get(1).getBlue());
			GL11.glVertex2f(node.getGrappaNexus().getBounds().x+node.getGrappaNexus().getBounds().width,node.getGrappaNexus().getBounds().y+node.getGrappaNexus().getBounds().height);
			GL11.glVertex2f(node.getGrappaNexus().getBounds().x,node.getGrappaNexus().getBounds().y+node.getGrappaNexus().getBounds().height);
			GL11.glEnd();
	
			GL11.glColor3f(0.5f,0.5f,1.0f);
			GL11.glDisable(GL11.GL_BLEND);
		}

	}

	private void drawGraphElement(Element e)
	{
		switch (e.getType())
		{
			case	GrappaConstants.NODE:
					drawNode((Node)e);
					break;

//			case	GrappaConstants.EDGE:
//					paintEdge((Edge)e,gc);
//					break;

			case	GrappaConstants.SUBGRAPH:
					{
						GraphEnumeration en = ((Subgraph)e).elements();

						while (en.hasMoreElements())
						{
							Element ne = en.nextGraphElement();
							if (e != ne)
								drawGraphElement(ne);
						}
					}
					break;
		}

	}

	@Override
	public void zoomReset()
	{
	}

	@Override
	public void setScaleToFit(boolean fit)
	{
	}

	@Override
	public void zoomIn()
	{
	}

	@Override
	public void zoomOut()
	{
	}

}
