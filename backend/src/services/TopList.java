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
 * class for retrieving the top loaned titles
 * @author sbosse
 *
 */
public class TopList {
	DatabaseConnection db;
	int max_results;
	
	XMLReader xml;
	
	public TopList(DatabaseConnection db, int max_results) {
		this.db = db;
		this.max_results = max_results;
		CSVWriterTitle csv = new CSVWriterTitle("./res/",new DataFields[] {
				DataFields.AUTHORS, DataFields.TITLE, DataFields.EDITION, DataFields.YEAR, 
				DataFields.PUBLISHER, DataFields.PUBLISHER_LOCATION, DataFields.TYPE, 
				DataFields.CUM_LOANS_TITLE, DataFields.CUM_RESERV_TITLE, DataFields.NUM_COPIES_STATS, DataFields.NUM_COPIES_AUI_U, DataFields.CUM_LOAN_RATIO, DataFields.CUM_RESERV_RATIO,
				DataFields.PPN, DataFields.LOCATION, DataFields.SIGNATURE, DataFields.CLASSIFICATION, DataFields.FR, DataFields.MATERIAL, DataFields.LANGUAGE, DataFields.ORDER_ID}
				,";", true);
		QueryFactory qf = new QueryFactory();
		xml = new XMLReader(qf, csv, db);
	}
	
	public void getTopReserved(int[] years, int[] sig_years, int min_ex, int min_reserv) throws SQLException, QueryErrorException {
		if (years.length==0) throw new QueryErrorException(999, "", "");//TODO
		if (min_ex<1) min_ex=1;
		if (min_reserv<1) min_reserv=1;
		
		String query = "SELECT top "+this.max_results+" "
				+ "	ous_copy_cache.ppn as ppn,"
				+ "	sum(volume_statistics.cum_loans) as num_loans,"
				+ "	sum(volume_statistics.cum_reservations) as num_reserv,"
				+ "	count(distinct volume.epn) as num_copies"
				+ " FROM (ous_copy_cache"
				+ "	left join volume on ous_copy_cache.epn=volume.epn)"
				+ "	left join volume_statistics on volume_statistics.volume_number=volume.volume_number"
				+ " WHERE ous_copy_cache.iln=100 and (";
		for (int year : years) query+="volume_statistics.year="+year+" or ";
		query = query.substring(0,query.length()-4)+")";
		query+=" and (substring(signature,1,2) in ('FH'))";
		if (sig_years!=null && sig_years.length!=0) {
			query+=" and (";
			for (int sig : sig_years) query+="(substring(signature,4,4) in ('"+sig+"')) or ";
			query = query.substring(0,query.length()-4)+")";
		}
		query+=" GROUP BY ppn"
				+ " HAVING (count(distinct volume.epn)>="+min_ex+")"
				+ "	and (sum(volume_statistics.cum_reservations)>="+min_reserv+")"
				+ " ORDER BY sum(volume_statistics.cum_reservations) DESC";
		
		if (db == null) throw new RuntimeException("DB connection not ready");
		List<Title> res = new ArrayList<Title>();
		ResultSet rs = db.sqlQuery(query);
		rs.beforeFirst();
		while (rs.next()) {
			String ppn = rs.getString("ppn");
			int num_loans = rs.getInt("num_loans");
			int num_reserv = rs.getInt("num_reserv");
			int num_copies = rs.getInt("num_copies");
			Title t = new Title(ppn,num_loans ,num_reserv, num_copies);
			res.add(t);
		}
		//System.out.println("Database finished in "+(System.currentTimeMillis()-time)/1000.0+" seconds");
		xml.retrieveFromTitles(res, true);
		//xml.write();
	}
	
	public void getTopLoaned(int[] years, int[] sig_years, int min_ex, int min_loans) throws SQLException, QueryErrorException {
		if (years.length==0) throw new QueryErrorException(999, "", "");//TODO
		if (min_ex<1) min_ex=1;
		if (min_loans<1) min_loans=1;
		
		String query = "SELECT top "+this.max_results+" "
				+ "	ous_copy_cache.ppn as ppn,"
				+ "	sum(volume_statistics.cum_loans) as num_loans,"
				+ "	sum(volume_statistics.cum_reservations) as num_reserv,"
				+ "	count(distinct volume.epn) as num_copies"
				+ " FROM (ous_copy_cache"
				+ "	left join volume on ous_copy_cache.epn=volume.epn)"
				+ "	left join volume_statistics on volume_statistics.volume_number=volume.volume_number"
				+ " WHERE ous_copy_cache.iln=100 and (";
		for (int year : years) query+="volume_statistics.year="+year+" or ";
		query = query.substring(0,query.length()-4)+")";
				//+ "	and (volume_statistics.year="+(year-2)+" or volume_statistics.year="+(year-1)+" or volume_statistics.year="+year+")"
		//query+=" and (substring(signature,1,2) in ('FH'))";
		if (sig_years!=null && sig_years.length!=0) {
			query+=" and (";
			for (int sig : sig_years) query+="(substring(signature,4,4) in ('"+sig+"')) or ";
			query = query.substring(0,query.length()-4)+")";
		}
		query+=" GROUP BY ppn"
				+ " HAVING (count(distinct volume.epn)>="+min_ex+")"
				+ "	and (sum(volume_statistics.cum_loans)>="+min_loans+")"
				+ " ORDER BY sum(volume_statistics.cum_loans) DESC";
		
		System.out.println(query);
		
		if (db == null) throw new RuntimeException("DB connection not ready");
		List<Title> res = new ArrayList<Title>();
		ResultSet rs = db.sqlQuery(query);
		rs.beforeFirst();
		while (rs.next()) {
			String ppn = rs.getString("ppn");
			int num_loans = rs.getInt("num_loans");
			int num_res = rs.getInt("num_reserv");
			int num_copies = rs.getInt("num_copies");
			Title t = new Title(ppn,num_loans,num_res,num_copies);
			res.add(t);
		}
		//System.out.println("Database finished in "+(System.currentTimeMillis()-time)/1000.0+" seconds");
		xml.retrieveFromTitles(res, true);
		//xml.write();
	}
	
	void write() {
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

			// Variante 1: hol dir erst die DB-Daten, dann die einzelnen Title-Objekte => längere Laufzeit aber effektiverer Erkenntnisgewinn
			TopList t = new TopList(db, 20000);
			t.getTopLoaned(new int[] {2022}, new int[] {}, 1, 1);
			t.getTopReserved(new int[] {2022}, new int[] {}, 1, 1);
			t.write();
			t.finalize();
			
		} catch (ClassNotFoundException | SQLException | QueryErrorException e) {
			e.printStackTrace();
		}
		System.out.println("Computed in "+(System.currentTimeMillis()-time)/1000.0+" seconds");
	}
}
