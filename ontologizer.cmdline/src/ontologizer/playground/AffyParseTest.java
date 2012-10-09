package ontologizer.playground;


import ontologizer.playground.affy.*;


public class AffyParseTest {

	
	public static void main(String[] argv) {
		System.out.println("Affy Parse");
		String file = argv[0];
		//String file = "/home/peter/data/affy/HG-U133A_2_annot.csv";
		
		AffyParse affy = new AffyParse(file);
		affy.debug();
	}
	
}
