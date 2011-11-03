package ontologizer.go;

public class TermXref {


	private String database;
	private String xrefId;
	
	public TermXref(String database, String xrefId) {
		
		this.database 	= database;
		this.xrefId		= xrefId;
		
	}
	
	@Override
	public int hashCode() {
		return database.hashCode() + xrefId.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {

		if ( ! (obj instanceof TermXref))
			return false;
		
		TermXref otherXref = (TermXref) obj;
		
		if (this.database.equals(otherXref.database) && this.xrefId.equals(otherXref.xrefId))
			return true;
		
		return false;
		
	
	}

	public String getDatabase() {
		return database;
	}

	public String getXrefId() {
		return xrefId;
	}

	@Override
	public String toString() {
		return database+" - "+xrefId;
	}

//	public static Database getDatabaseFromString(String dbName) {
//		dbName = dbName.toUpperCase();
//		for (Database db : Database.values()){
//			if(db.toString().toUpperCase().equals(dbName))
//				return db;
//		}
//		return null;
//	}
}
