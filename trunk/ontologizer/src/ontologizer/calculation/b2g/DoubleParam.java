package ontologizer.calculation.b2g;

import ontologizer.calculation.b2g.B2GParam.Type;

class DoubleParam extends B2GParam
{
	private double min;
	private double max;
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

	/**
	 * Applicable for Variables of type MCMC or EM.
	 * 
	 * @param min
	 */
	public void setMin(double min)
	{
		this.min = min;
	}
	
	/**
	 * Applicable for Variables of type MCMC or EM.
	 * 
	 * @param max
	 */
	public void setMax(double max)
	{
		this.max = max;
	}
	
	/**
	 * Applicable for Variables of type MCMC or EM.
	 * 
	 * @return
	 */
	public double getMin()
	{
		return min;
	}
	
	/**
	 * Applicable for Variables of type MCMC or EM.
	 * 
	 * @return
	 */
	public double getMax()
	{
		return max;
	}
	
	@Override
	public String toString()
	{
		if (isFixed()) return String.format("%g",val);
		return getType().toString();
	}
}
