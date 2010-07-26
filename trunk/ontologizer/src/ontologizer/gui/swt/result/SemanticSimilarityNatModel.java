package ontologizer.gui.swt.result;

import net.sourceforge.nattable.config.DefaultBodyConfig;
import net.sourceforge.nattable.config.DefaultColumnHeaderConfig;
import net.sourceforge.nattable.config.DefaultRowHeaderConfig;
import net.sourceforge.nattable.config.SizeConfig;
import net.sourceforge.nattable.data.IColumnHeaderLabelProvider;
import net.sourceforge.nattable.data.IDataProvider;
import net.sourceforge.nattable.model.DefaultNatTableModel;
import net.sourceforge.nattable.renderer.DefaultRowHeaderRenderer;
import ontologizer.types.ByteString;

public class SemanticSimilarityNatModel extends DefaultNatTableModel
{
	private double [][] values;
	private ByteString [] names;

	public SemanticSimilarityNatModel()
	{
		IDataProvider dataProvider = new IDataProvider() {
			public int getColumnCount() {if (values==null) return 0; return values[0].length;};
			public int getRowCount() { if (values==null) return 0; return values.length;};
			public Object getValue(int x, int y) {
				return String.format("%g",values[x][y]);
			}};
		DefaultBodyConfig dbc = new DefaultBodyConfig(dataProvider);
		SizeConfig sc = new SizeConfig();
		sc.setDefaultSize(75);
		sc.setDefaultResizable(true);
		dbc.setColumnWidthConfig(sc);
		setBodyConfig(dbc);
		
		DefaultRowHeaderConfig rowHeaderConfig = new DefaultRowHeaderConfig();
		rowHeaderConfig.setRowHeaderColumnCount(1);
		rowHeaderConfig.setCellRenderer(new DefaultRowHeaderRenderer()
		{
			@Override
			public String getDisplayText(int row, int col)
			{
				return names[row].toString();
			}
		});
		setRowHeaderConfig(rowHeaderConfig);

		DefaultColumnHeaderConfig columnHeaderConfig = new DefaultColumnHeaderConfig(new IColumnHeaderLabelProvider()
		{
			public String getColumnHeaderLabel(int col)
			{
				if (names == null) return "";
				return names[col].toString();
			}
		});
		setColumnHeaderConfig(columnHeaderConfig);
	}

	public void setValues(double[][] values)
	{
		this.values = values;
	}
	
	public void setNames(ByteString[] names)
	{
		this.names = names;
	}
	
	public double getValue(int x, int y)
	{
		if (x < 0 || y < 0) return Double.NaN;
		return values[x][y];
	}
}
