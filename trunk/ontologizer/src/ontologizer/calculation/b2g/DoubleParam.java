package ontologizer.calculation.b2g;

import ontologizer.calculation.b2g.B2GParam.Type;

class DoubleParam extends B2GParam
{
	private double val;

	public DoubleParam(Type type, double val)
	{
		super(type);
		
		this.val = val;
	}
	
	public DoubleParam(DoubleParam p)
	{
		super(p);
		
		this.val = p.val;
	}

	public DoubleParam(Type type)
	{
		super(type);

		if (type == Type.FIXED) throw new IllegalArgumentException("Parameter could not be instanciated of type Fixed.");
	}

	double getValue()
	{
		return val;
	}
	
	void setValue(double newVal)
	{
		this.val = newVal;
		setType(Type.FIXED);
	}
	
	@Override
	public String toString()
	{
		if (isFixed()) return String.format("%g",val);
		return getType().toString();
	}
}
