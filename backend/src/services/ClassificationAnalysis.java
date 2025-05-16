package services;

import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.Calendar;

import javax.xml.soap.SOAPException;

import retrieve.XMLReader;
import retrieve.DatabaseConnection;
import retrieve.QueryErrorException;
import retrieve.QueryFactory;
import write.AbstractExcelWriter;
import write.ExcelWriter;
import write.ExcelWriterTitle;
import write.TitleWriter.AggMode;
import write.TitleWriter.DataFields;

/**
 * class for creating reports for the UB subject specialists
 * 
 * @author sbosse
 *
 */
public class ClassificationAnalysis {

	QueryRetriever qr;
	XMLReader xr;
	int[] years;

	public ClassificationAnalysis(OutputStream os, DatabaseConnection db) throws SOAPException, ClassNotFoundException, SQLException {
		this(os, db, false);
	}

	public ClassificationAnalysis(OutputStream os, DatabaseConnection db, boolean ex_lvl)
			throws SOAPException, ClassNotFoundException, SQLException {
		int year = Calendar.getInstance().get(Calendar.YEAR);
		int num_years = year - 2007;
		this.years = new int[num_years];
		for (int i=0;i<num_years;i++) years[i] = year-i;
		
		AbstractExcelWriter tw;
		if (ex_lvl) {
			tw = new ExcelWriter(os,
					new DataFields[] { DataFields.AUTHORS, DataFields.TITLE, DataFields.EDITION, DataFields.YEAR,
							DataFields.SUPER_TITLE, DataFields.SUPER_PPN, DataFields.PUBLISHER,
							DataFields.PUBLISHER_LOCATION, DataFields.TYPE, DataFields.CUM_LOANS_TITLE,
							DataFields.LOANS_TIL_2007, DataFields.LOANS_LAST_10_YEARS, DataFields.LOANS_LAST_5_YEARS,
							DataFields.LAST_LOAN, DataFields.ON_LOAN, DataFields.CUM_RESERV_TITLE,
							DataFields.RESERVE_LAST_5_YEARS, DataFields.LOAN_INDICATOR, DataFields.SELECTION_KEY,
							DataFields.PPN, DataFields.LOCATION, DataFields.SIGNATURE, DataFields.INTERNAL_CODES,
							DataFields.REMARK, DataFields.CLASSIFICATION, DataFields.FR, DataFields.BKL, DataFields.RVK,
							DataFields.DDC, DataFields.MATERIAL, DataFields.INVENTORY_STRING, DataFields.LANGUAGE,
							DataFields.LAST_COPY_GVK, DataFields.COPY_HALLE, DataFields.NUM_LIBRARIES,
							DataFields.ORDER_ID, DataFields.ORDER_TYPE },
					false);
		} else {
			tw = new ExcelWriterTitle(os, new DataFields[] { DataFields.AUTHORS, DataFields.TITLE, DataFields.EDITION,
					DataFields.YEAR, DataFields.SUPER_TITLE, DataFields.SUPER_PPN, DataFields.PUBLISHER,
					DataFields.PUBLISHER_LOCATION, DataFields.TYPE, DataFields.CUM_LOANS_TITLE,
					DataFields.LOANS_TIL_2007, DataFields.LOANS_LAST_10_YEARS, DataFields.LOANS_LAST_5_YEARS,
					DataFields.LAST_LOAN, DataFields.ON_LOAN, DataFields.CUM_RESERV_TITLE,
					DataFields.RESERVE_LAST_5_YEARS, DataFields.NUM_COPIES_STATS, DataFields.NUM_COPIES_AUI_U,
					DataFields.PPN, DataFields.LOCATION, DataFields.NUM_LOCATION_FH, DataFields.NUM_LOCATION_FHP,
					DataFields.NUM_LOCATION_MG, DataFields.SIGNATURE, DataFields.SIG_NO_MAG, DataFields.SIG_MAG,
					DataFields.INTERNAL_CODES, DataFields.REMARK, DataFields.CLASSIFICATION, DataFields.FR,
					DataFields.BKL, DataFields.RVK, DataFields.DDC, DataFields.MATERIAL, DataFields.INVENTORY_STRING,
					DataFields.LANGUAGE, DataFields.LAST_COPY_GVK, DataFields.COPY_HALLE, DataFields.NUM_LIBRARIES,
					DataFields.ORDER_ID, DataFields.ORDER_TYPE }, AggMode.APPEND, true, null);
		}
		QueryFactory qf = new QueryFactory("https://sru.k10plus.de/", "opac-de-ma9"); 
		xr = new XMLReader(qf, tw, db, null);
		qr = new QueryRetriever(xr,false,1500000);

	}

	String material = "";

	public void getReports() throws FileNotFoundException, UnsupportedEncodingException, QueryErrorException {
		// bo
		getReport("B.+2"); // >16 000
		getReport("Y."); // >14 500

		// lue
		getReport("P."); // >22 500
		getReport("Q."); // >7 500
		getReport("F+A"); // >33 500

		// koe
		getReport("F+F", new String[] { "F+F+2", "F+F+3", "F+F+5", "F+F+6", "F+F+7" });
		getReport("F+Z");
		;
		// > 6 000

		// rg
		getReport("F+D"); // >43 000
		getReport("F+C+4");

		// mat
		getReport("F+E", new String[] { "F+E+0", "F+E+2", "F+E+3", "F+E+800", "F+E+81" });// 11 990

		// this.qr.retrieve("((pica.lsy=\"F E 4*\"+or+pica.lsy=\"F E
		// 82*\"+or+pica.lsy=\"F E 9*\")+and+pica.mat=\"b\")",this.years,false);
		getReport("F+H");
		getReport("F+R");
		getReport("F+L"); // 92 593
		// this.qr.retrieve("((pica.lsy=\"F H*\"+or+pica.lsy=\"F R*\"+or+pica.lsy=\"F
		// L*\")+and+pica.mat=\"b\")",this.years,false);

		// hg
		getReport("F+E", new String[] { "F+E+4", "F+E+82", "F+E+9" });// > 20 000
		getReport("R.", new String[] { "R.+667" });// 10 000

		// le
		getReport("K.+5"); // 14 364
		// this.qr.retrieve("((pica.lsy=\"K.
		// 5*\")+and+pica.mat=\"b\")",this.years,false);
		getReport("B.", new String[] { "B.+2" }); // 28 230
		// this.qr.retrieve("((pica.lsy=\"B.*\"+not+pica.lsy=\"B.
		// 2*\")+and+pica.mat=\"b\")",this.years,false);
		getReport("D.+7");
		getReport("E.", new String[] { "E.+752" });
		getReport("C.+7"); // 14 500
		// this.qr.retrieve("((pica.lsy=\"D. 7*\"+or+(pica.lsy=\"E.*\"+not+pica.lsy=\"E.
		// 752*\")+or+pica.lsy=\"C. 7*\")+and+pica.mat=\"b\")",this.years,false);
		getReport("C.+3");
		getReport("C.+18");
		getReport("G."); // 4122
		// this.qr.retrieve("((pica.lsy=\"G.*\"+or+pica.lsy=\"C. 18*\"+or+pica.lsy=\"C.
		// 3*\")+and+pica.mat=\"b\")",this.years,false);

		// wa
		getReport("F+O");
		getReport("C.", new String[] { "C.+18", "C.+7", "C.+3" });
		// 15 451
		getReport("E.+752");
		getReport("D.", new String[] { "D.+7" });
		// 19 500
		getReport("F.");
		getReport("F+G"); // 11 500

		// ilg
		getReport("F+F", new String[] { "F+F+0", "F+F+4", "F+F+8", "F+F+9" });
		// 23 414
		getReport("F+K");// 12 400

		// bue
		getReport("R.+667");

		getReport("Sammlung");
		getReport("Stiftung");
	}

	/**
	 * returns a list of titles for a given class as well as analyses
	 * 
	 * @param cla string containing the class for which titles should be reported
	 * @param not string array containing classes that should not be reported
	 * @throws QueryErrorException
	 * @throws FileNotFoundException
	 * @throws UnsupportedEncodingException
	 */
	public void getReport(String cla, String[] not)
			throws QueryErrorException, FileNotFoundException, UnsupportedEncodingException {

		String query;
		if (!material.isEmpty()) {
			if (not == null || not.length == 0)
				query = "(pica.lsy=\"" + cla + "*\"+and+pica.mat=\"" + material + "\")";
			else {
				query = "(pica.lsy=\"" + cla + "*\"";
				for (String s : not) {
					query += "+not+pica.lsy=\"" + s + "*\"";
				}
				query += "+and+pica.mat=\"" + material + "\")";
			}
		} else {
			if (not == null || not.length == 0)
				query = "(pica.lsy=\"" + cla + "*\")";
			else {
				query = "(pica.lsy=\"" + cla + "*\"";
				for (String s : not) {
					query += "+not+pica.lsy=\"" + s + "*\"";
				}
				query += ")";
			}
		}
		this.qr.retrieve(query,years,false,true,true);
		this.qr.xr.write(cla);
		 
	}

	public void getReport(String cla) throws FileNotFoundException, UnsupportedEncodingException, QueryErrorException {
		getReport(cla, new String[] {});
	}

	public void close() {
		this.xr.close();
	}

}
