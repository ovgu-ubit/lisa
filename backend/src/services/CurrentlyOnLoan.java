package services;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import model.Title;
import retrieve.DatabaseConnection;
import retrieve.QueryErrorException;
import retrieve.QueryFactory;
import retrieve.XMLReader;
import write.CSVWriter;
import write.CSVWriterTitle;
import write.TitleWriter;
import write.TitleWriter.DataFields;

/**
 * retrieve all titles or copies currently on loan
 * @author sbosse
 *
 */
public class CurrentlyOnLoan {
	DatabaseConnection db;
	int max_results;
	
	XMLReader xml;

	/**
	 * 
	 * @param db
	 * @param titleLvl if the csv file should have one row a title or one row a copy
	 */
	public CurrentlyOnLoan(DatabaseConnection db, boolean titleLvl) {
		this.db = db;
		TitleWriter csv;
		if (!titleLvl) csv = new CSVWriter("./res/",new DataFields[] {DataFields.AUTHORS, DataFields.TITLE, DataFields.EDITION, DataFields.YEAR, DataFields.PPN, 
				DataFields.LOCATION, DataFields.NUM_COPIES_AUI_U, DataFields.ON_LOAN, DataFields.LOAN_DATE,
				DataFields.SIGNATURE, DataFields.CLASSIFICATION, DataFields.FR, DataFields.MATERIAL, DataFields.LANGUAGE, DataFields.REMARK}
				,";", true);
		else csv = new CSVWriterTitle("./res/",new DataFields[] {DataFields.AUTHORS, DataFields.TITLE, DataFields.EDITION, DataFields.YEAR, DataFields.PPN, 
				DataFields.LOCATION, DataFields.NUM_COPIES_AUI_U, DataFields.ON_LOAN, DataFields.LOAN_DATE,
				DataFields.SIGNATURE, DataFields.CLASSIFICATION, DataFields.FR, DataFields.MATERIAL, DataFields.LANGUAGE, DataFields.REMARK}
				,";", true);
		QueryFactory qf = new QueryFactory();
		xml = new XMLReader(qf, csv);
	}
	
	public void getLended(String whereClause) throws SQLException, QueryErrorException {
		
		String query = "SELECT DISTINCT occ.ppn FROM dbo.ous_copy_cache occ	JOIN dbo.volume v ON (occ.epn = v.epn) JOIN dbo.loans_requests lr ON (v.volume_number = lr.volume_number) "
				+ "WHERE lr.iln = 100 AND lr.loan_status = 5 AND v.loan_indication = 0 "+whereClause;
		if (db == null) throw new RuntimeException("DB connection not ready");
		List<Title> res = new ArrayList<Title>();
		ResultSet rs = db.sqlQuery(query);
		rs.beforeFirst();
		while (rs.next()) {
			String ppn = rs.getString("ppn");
			Title t = new Title(ppn,-1,-1,-1);
			res.add(t);
		}
		//System.out.println("Database finished in "+(System.currentTimeMillis()-time)/1000.0+" seconds");
		xml.retrieveFromTitles(res, false);
		xml.write();
	}
	
	@Override
	protected void finalize() throws SQLException {
		xml.close();
		db.finalize();
	}
	
	
	public static void main(String[] args) {
		double time = System.currentTimeMillis();
		try {
			DatabaseConnection db = new DatabaseConnection(false);			
			
			CurrentlyOnLoan t = new CurrentlyOnLoan(db, false);
			//t.getLended("AND lr.borrower_type = 70");
			//t.getLended("");
			t.getLended("AND lr.expiry_date_loan < '2021-07-01 00:00:00'");
			t.finalize();
			
		} catch (ClassNotFoundException | SQLException | QueryErrorException e) {
			e.printStackTrace();
		}
		System.out.println("Computed in "+(System.currentTimeMillis()-time)/1000.0+" seconds");

		//System.out.println("Computation finished at "+System.currentTimeMillis());
	}

}
