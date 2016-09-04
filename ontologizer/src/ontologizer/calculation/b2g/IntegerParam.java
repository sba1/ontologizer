package ontologizer.calculation.b2g;

class IntegerParam extends B2GParam
{
	private int val;

	public IntegerParam(Type type, int val)
	{
		super(type);

		this.val = val;
	}

	public IntegerParam(Type type)
	{
		super(type);

		if (type == Type.FIXED) throw new IllegalArgumentException("Parameter could not be instanciated of type Fixed.");
	}

	public IntegerParam(IntegerParam p)
	{
		super(p);

		this.val = p.val;
	}

	int getValue()
	{
		return val;
	}

	void setValue(int newVal)
	{
		this.val = newVal;
		setType(Type.FIXED);
	}

	@Override
	public String toString()
	{
		if (isFixed()) return Integer.toString(val);
		return getType().toString();
	}
}
