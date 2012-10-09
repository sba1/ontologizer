/*
 * Created on 03.04.2009
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ontologizer.go;

import java.util.HashMap;

/**
 * A basic class representing a name space.
 * 
 * @author Sebastian Bauer
 */
public class Namespace
{
	/* This static mapping stuff is for historical reasons. This enum should be removed very soon */
	static public enum NamespaceEnum
	{
		BIOLOGICAL_PROCESS,
		MOLECULAR_FUNCTION,
		CELLULAR_COMPONENT,
		UNSPECIFIED;
	};
	static private HashMap<Namespace,NamespaceEnum> namespaceMap = new HashMap<Namespace,NamespaceEnum>();
	static
	{
		namespaceMap.put(new Namespace("biological_process"), NamespaceEnum.BIOLOGICAL_PROCESS);
		namespaceMap.put(new Namespace("molecular_function"), NamespaceEnum.MOLECULAR_FUNCTION);
		namespaceMap.put(new Namespace("cellular_component"), NamespaceEnum.CELLULAR_COMPONENT);
	};
	static public NamespaceEnum getNamespaceEnum(Namespace namespace)
	{
		NamespaceEnum e = namespaceMap.get(namespace);
		if (e == null) return NamespaceEnum.UNSPECIFIED;
		return e;
	}

	/** The global unknown namespace */
	public static Namespace UNKOWN_NAMESPACE = new Namespace("unknown");

	private String name;

	public Namespace(String newName)
	{
		this.name = newName;
	}

	public String getName()
	{
		return name;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		return name.equals(((Namespace)obj).name);
	}

	@Override
	public int hashCode()
	{
		return name.hashCode();
	}
}
