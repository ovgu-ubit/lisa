package services;


import model.predicates.*;
import retrieve.QueryErrorException;
import retrieve.QueryFactory;
import retrieve.XMLReader;
import write.CSVWriter;
import write.CSVWriterTitle;
import write.TitleWriter;
import write.TitleWriter.DataFields;

/**
 * class for testing diverse predicate queries
 * @author sbosse
 *
 */
public class PredTester {
	
	XMLReader xr;
	TitleWriter csv;
	
	public PredTester() {
		
	}
	
	void getMultiMagCopies() throws QueryErrorException {
		csv = new CSVWriterTitle("./res/", new DataFields[] {DataFields.AUTHOR_FIRST, DataFields.AUTHOR_OTHER, DataFields.TITLE, DataFields.YEAR, DataFields.PPN, 
				DataFields.LOCATION, DataFields.NUM_COPIES_AUI_U, 
				DataFields.SIGNATURE, DataFields.CLASSIFICATION, DataFields.FR, DataFields.MATERIAL, DataFields.LANGUAGE, DataFields.REMARK},
				";",new MultiCopy(2, Integer.MAX_VALUE, "Magazin", "k"),true,false);

		xr = new XMLReader(new QueryFactory(), csv);
		String query = "(pica.sst=Magazin*)";
		xr.retrieve(query, 150000, 1,true,null);
	}
	
	void getSingleRZCopies(String[] classifications) throws QueryErrorException {
		csv = new CSVWriterTitle("./res/", new DataFields[] {DataFields.AUTHOR_FIRST, DataFields.AUTHOR_OTHER, DataFields.TITLE, DataFields.YEAR, DataFields.PPN, DataFields.EPN, DataFields.LOCATION,
				DataFields.SIGNATURE, DataFields.CLASSIFICATION, DataFields.MATERIAL, DataFields.LANGUAGE, DataFields.REMARK, DataFields.LOAN_INDICATOR,DataFields.SELECTION_KEY,
				DataFields.STATUS},";",new MultiCopy(1, 1, "", "k"),false,false);

		xr = new XMLReader(new QueryFactory(), csv);
		
		String query = "(pica.sst=\"RZ*\")+and+(";
		for (String s : classifications) query+="pica.lsy=\""+s+"\"+or+";
		query = query.substring(0,query.length()-4)+")";
		xr.retrieve(query, 10000, 1, true,null);
	}
	
	void getNoBarcode(String[] classifications) throws QueryErrorException {
		csv = new CSVWriter("./res/",new DataFields[] {DataFields.AUTHORS, DataFields.TITLE, DataFields.EPN, DataFields.LOCATION,
				DataFields.SIGNATURE, DataFields.BARCODE, DataFields.CLASSIFICATION, DataFields.MATERIAL, DataFields.REMARK, DataFields.LOAN_INDICATOR}
			,";",new NoBarcode(), false);

		xr = new XMLReader(new QueryFactory(), csv);
		String query = "";
		for (String s : classifications) query+="pica.lsy=\""+s+"\"+or+";
		query = query.substring(0,query.length()-4);
		xr.retrieve(query, 500000, 1, true,null);
	}

	public static void main(String[] args) {
		try {
			PredTester fr = new PredTester();
			//fr.getMultiMagCopies();
			//fr.getSingleRZCopies(new String[] {"Y.*", "B. 2*"});
			fr.getNoBarcode(new String[] {"B*","Y*"});
			System.out.println("Computing finished");
		} catch (QueryErrorException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
