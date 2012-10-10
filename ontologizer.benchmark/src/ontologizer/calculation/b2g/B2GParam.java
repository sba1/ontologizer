package ontologizer.calculation.b2g;

/**
 * A basic class to represent different settings parameter.
 * 
 * @author sba
 *
 */
abstract public class B2GParam
{
	static public enum Type
	{
		FIXED,
		EM,
		MCMC
	} 

	private Type type;
	
	B2GParam(Type type)
	{
		this.type = type;
	}
	
	B2GParam(B2GParam p)
	{
		this.type = p.type;
	}

	public Type getType()
	{
		return type;
	}
	
	public boolean isFixed()
	{
		return type == Type.FIXED;
	}
	
	public boolean isMCMC()
	{
		return type == Type.MCMC;
	}

	public boolean isEM()
	{
		return type == Type.EM;
	}
	
	public void setType(Type type)
	{
		this.type = type;
	}
}

