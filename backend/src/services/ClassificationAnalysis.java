package services;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import retrieve.DatabaseConnection;
import retrieve.QueryErrorException;
import retrieve.QueryFactory;
import retrieve.XMLReader;
import write.CSVWriterTitle;
import write.TitleWriter.DataFields;

/**
 * class for creating reports for the UB subject specialists
 * @author sbosse
 *
 */
public class ClassificationAnalysis {

	QueryRetriever qr;
	int[] years;
	
	/**
	 * 
	 * @param db
	 * @param num_years how many years back statistics should be retrieved
	 */
	public ClassificationAnalysis(DatabaseConnection db, int num_years, boolean currentYear) {

		int year = Calendar.getInstance().get(Calendar.YEAR);
		this.years = new int[num_years];
		if (!currentYear) for (int i=0;i<num_years;i++) years[i] = year-i-1;
		else for (int i=0;i<num_years;i++) years[i] = year-i;
		
		QueryFactory qf = new QueryFactory();
		CSVWriterTitle tw = new CSVWriterTitle("./res/",new DataFields[] {DataFields.AUTHORS, DataFields.TITLE, DataFields.EDITION, DataFields.YEAR, 
				DataFields.PUBLISHER, DataFields.PUBLISHER_LOCATION, DataFields.TYPE, DataFields.CUM_LOANS_TITLE, DataFields.LOANS_TIL_2007, DataFields.LOANS_LAST_10_YEARS, DataFields.LOANS_LAST_5_YEARS, DataFields.LAST_LOAN,
				DataFields.CUM_RESERV_TITLE, DataFields.RESERVE_LAST_5_YEARS, DataFields.NUM_COPIES_STATS, DataFields.NUM_COPIES_AUI_U, DataFields.PPN, 
				DataFields.LOCATION, DataFields.NUM_LOCATIONS, DataFields.SIGNATURE, DataFields.CLASSIFICATION, DataFields.FR, DataFields.MATERIAL, DataFields.LANGUAGE, 
				DataFields.LAST_COPY_GVK, DataFields.COPY_HALLE, DataFields.NUM_LIBRARIES}
				,";", num_years+"_year_report_",true);
		
		/*CSVWriterTitle tw = new CSVWriterTitle("./res/",new DataFields[] {DataFields.AUTHORS, DataFields.TITLE, DataFields.EDITION, DataFields.YEAR, 
				DataFields.PUBLISHER, DataFields.PUBLISHER_LOCATION, DataFields.TYPE, DataFields.LOANS_TOTAL, DataFields.RESERVE_TOTAL,
				DataFields.NUM_COPIES_STATS, DataFields.NUM_COPIES_AUI_U, DataFields.PPN, 
				DataFields.LOCATION, DataFields.NUM_LOCATIONS, DataFields.SIGNATURE, DataFields.CLASSIFICATION, DataFields.FR, DataFields.MATERIAL, DataFields.LANGUAGE, 
				DataFields.LAST_COPY_GVK, DataFields.COPY_HALLE, DataFields.NUM_LIBRARIES}
				,";", num_years+"_year_report_",true);*/
		
		
		XMLReader xr = new XMLReader(qf,tw,db);
		qr = new QueryRetriever(xr,false,1500000);
	}
	
	String material = "";
	
	public void getReports() throws FileNotFoundException, UnsupportedEncodingException, QueryErrorException {
		this.getReports("b");
	}

	public void getReports(String material) throws QueryErrorException, FileNotFoundException, UnsupportedEncodingException {
		this.material = material;
		//TODO collect reports by calling getReport of specific classifications
		// e.g. getReport("B.+2"); 
	}
	
	/**
	 * returns a list of titles for a given class as well as analyses
	 * @param cla string containing the class for which titles should be reported
	 * @param not string array containing classes that should not be reported
	 * @throws QueryErrorException
	 * @throws FileNotFoundException
	 * @throws UnsupportedEncodingException
	 */
	void getReport(String cla, String[] not) throws QueryErrorException, FileNotFoundException, UnsupportedEncodingException {
		String query;
		if (!material.isEmpty()) {
			if (not == null || not.length==0) query = "(pica.lsy=\""+cla+"*\"+and+pica.mat=\""+material+"\")";
			else {
				query = "(pica.lsy=\""+cla+"*\"";
				for (String s : not) {
					query+="+not+pica.lsy=\""+s+"*\"";
				}
				query+="+and+pica.mat=\""+material+"\")";
			}
		} else {
			if (not == null || not.length==0) query = "(pica.lsy=\""+cla+"*\")";
			else {
				query = "(pica.lsy=\""+cla+"*\"";
				for (String s : not) {
					query+="+not+pica.lsy=\""+s+"*\"";
				}
				query+=")";
			}
		}
			
			
		this.qr.retrieve(query,years,false,true,true);
		String analy = this.qr.xr.analyzeLSY(cla);
		PrintWriter out = new PrintWriter("./res/analyse_"+cla.replaceAll("[*<>\"()\\?]", "")+"_"+DateTimeFormatter.ofPattern("yyMMdd_HHmmss_SSS").format(LocalDateTime.now())+".csv","UTF-16LE");
		out.print(analy);
		out.close();
		this.qr.xr.write(cla);
	}
	
	void getReport(String cla) throws QueryErrorException, FileNotFoundException, UnsupportedEncodingException {
		getReport(cla,null);
	}
	
	boolean getTitlesFromPPN(File file) throws QueryErrorException {
		List<String> ppns = new ArrayList<String>();
		
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(file));
			String line;
			while ((line=br.readLine())!=null) {
				if (line.matches("[0-9]{8}[0-9X][0-9X]?"))	ppns.add(line);
				else if (line.matches("[0-9]{7}[0-9X][0-9X]?")) ppns.add("0"+line);
			}
			System.out.println(ppns.size()+" PPNs read");
			br.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		for (String ppn : ppns) {
			if (!this.qr.retrieve("pica.ppn=\""+ppn+"\"", years, false, true, true)) {
				this.qr.retrieve("pica.ppn=\"0"+ppn+"\"", years, false, true, true);
			}
		}
		this.qr.xr.write();
		return true;
	}

	public static void main(String[] args) {
		DatabaseConnection db;
		try {
			double time = System.currentTimeMillis();
			db = new DatabaseConnection(false);
			//retrieve current year and construct array for all years
			ClassificationAnalysis ca = new ClassificationAnalysis(db, 15, true);
			//ca.getReports("");
			//ca.getReports("e");
			ca.getReport("Y.");			
			
			db.finalize();
			System.out.println("Computing finished in "+(System.currentTimeMillis()-time)/1000.0+" seconds");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
