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
import write.CSVWriterTitle;
import write.TitleWriter.DataFields;

/**
 * class for retrieving the list of title/copies never on loan
 * @author sbosse
 *
 */
public class FlopList {
	DatabaseConnection db;
	int max_results;
	
	XMLReader xml;
	
	public FlopList(DatabaseConnection db) {
		this.db = db;
		CSVWriterTitle csv = new CSVWriterTitle("./res/",new DataFields[] {DataFields.AUTHOR_FIRST, DataFields.AUTHOR_OTHER, DataFields.TITLE, DataFields.YEAR, 
				DataFields.NUM_COPIES_AUI_U, DataFields.PPN, DataFields.SIGNATURE, DataFields.CLASSIFICATION, DataFields.MATERIAL, DataFields.LANGUAGE, DataFields.FR}
				,";", true);
		QueryFactory qf = new QueryFactory();
		xml = new XMLReader(qf, csv);
	}
	
	public void getFlopTitles(int num, int min_ex) throws SQLException, QueryErrorException {
		if (min_ex<0) min_ex=0;
		if (num<1) num=1;
		
		String query = "select top "+num+" c.ppn, min(c.shorttitle), min(c.author), min(c.signature), min(c.type_of_material_copy), count(c.epn) as \"num_ex\" "
				+ "from ous_copy_cache c, ( select v.epn, v.volume_bar , sum(vs.cum_loans) as \"sum_loans\", sum(vs.cum_reservations) as \"sum_res\" "
				+ "from dbo.volume v, dbo.volume_statistics vs where v.volume_number = vs.volume_number and v.iln = 100  and v.type_of_material = 0 "
				+ "group by v.volume_number ) as v "
				+ "where c.epn=v.epn and c.loan_indication = 0 and c.selection_key like 'k%' and (c.signature like 'FH:%') "
				+ "group by c.ppn "
				+ "having sum(v.sum_loans) = 0 and sum(v.sum_res) = 0 and count(c.epn) >= "+min_ex+" "
				+ "order by num_ex desc";
		if (db == null) throw new RuntimeException("DB connection not ready");
		List<Title> res = new ArrayList<Title>();
		ResultSet rs = db.sqlQuery(query);
		rs.beforeFirst();
		while (rs.next()) {
			String ppn = rs.getString("ppn");
			int num_copies = rs.getInt("num_ex");
			Title t = new Title(ppn,-1 ,-1, num_copies);
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
	
	static long time;
	
	public static void main(String[] args) {
		time = System.currentTimeMillis();
		try {
			DatabaseConnection db = new DatabaseConnection(false);			
			
			FlopList t = new FlopList(db);
			t.getFlopTitles(100000, 1);
	
			t.finalize();
			
		} catch (ClassNotFoundException | SQLException | QueryErrorException e) {
			e.printStackTrace();
		}
		System.out.println("Computed in "+(System.currentTimeMillis()-time)/1000.0+" seconds");

		//System.out.println("Computation finished at "+System.currentTimeMillis());
	}
}
