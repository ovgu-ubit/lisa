package services;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import model.Title;
import model.Title.Copy;
import retrieve.DatabaseConnection;
import retrieve.QueryErrorException;
import retrieve.QueryFactory;
import retrieve.XMLReader;
import write.TitleArray;

/**
 * service that allows for retrieving catalogue data and loan statistics of a list of PPNs, barcodes and signatures
 * @author sbosse
 *
 */
public class DropbillRetriever {

	XMLReader xr;
	
	public DropbillRetriever(XMLReader xr) {
		this.xr = xr;
	}
	
	/**
	 * standard constructor using SingleTitleWriter
	 * @throws SQLException if DBconnection fails
	 * @throws ClassNotFoundException if DB driver not found
	 */
	public DropbillRetriever() throws ClassNotFoundException, SQLException {
		this.xr = new XMLReader(new QueryFactory(), new TitleArray(true), new DatabaseConnection());
	}
	
	/**
	 * main method to retrieve a list of Titles based on PPN, Barcodes or Signatures
	 * @param ppns the PPNs to be retrieved
	 * @param barcodes the barcodes to be retrieved
	 * @param signatures the signatures to be retrieved
	 * @return a list of filled Title object 
	 * @throws QueryErrorException
	 * @throws SAXException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 */
	public List<Title> retrievePPN(String[] ppns, String[] barcodes, String[] signatures, String[] autos, boolean familyTitles) throws QueryErrorException {
		int num = ppns.length + barcodes.length + signatures.length + autos.length;
		System.out.println("UB-Dropbill: "+num+" titles requested");
		if (num > 100) throw new QueryErrorException(100,"Too many titles have been requested, use multiple queries instead","unknown");

		String regexPPN = "[0-9]{8}[0-9X][0-9X]?";
		String regexBarcode = "MA9(\\$[0-9]{8}[0-9X]|/1\\$[0-9]{6}[0-9X])";
		String regexSignature = "(CD |DVD |Di )?(19|20)?[0-9]{2}( [a-c] |\\.)[0-9]{1,5}[0-9X]?(\\([0-9]*\\))?(-[0-9]{2}|\\/[0-9a-zA-Z\\.,()-]*)?";

		ArrayList<String> ppnsAll = new ArrayList<String>();
		ArrayList<String> barcodesAll = new ArrayList<String>();
		ArrayList<String> sigsAll = new ArrayList<String>();
		for (String ppn : ppns) {
			ppn = ppn.trim();
			if (!ppn.matches(regexPPN)) throw new QueryErrorException(101,"Requested PPN has wrong format","ppn="+ppn);
			ppnsAll.add(ppn);
		}
		for (String barcode : barcodes) {
			barcode = barcode.trim();
			if (!barcode.matches(regexBarcode)) throw new QueryErrorException(102,"Requested barcode has wrong format","barcode="+barcode);
			barcodesAll.add(barcode);
		}
		for (String sig : signatures) {
			sig = sig.trim();
			if (!sig.matches(regexSignature)) throw new QueryErrorException(103,"Requested signature has wrong format","signature="+sig);
			sigsAll.add(sig);
		}
		
		for (String auto : autos) {
			auto = auto.trim();
			if (auto.matches(regexPPN)) ppnsAll.add(auto);
			else if (auto.matches(regexBarcode)) barcodesAll.add(auto);
			else if (auto.matches(regexSignature)) sigsAll.add(auto);
			else throw new QueryErrorException(104,"Auto recognition failed due to wrong format","auto="+auto);
		}
		
		// Query is constructed as OR clause
		String query = "(";
		for (int i=0;i<ppnsAll.size();i++) {		
			query+="pica.ppn="+ppnsAll.get(i);
			if (i<ppnsAll.size()-1) query+="+or+";
		}
		if (ppnsAll.size()>0 && barcodesAll.size()>0) query+="+or+";
		
		for (int i=0;i<barcodesAll.size();i++) {
			query+="pica.bar="+barcodesAll.get(i).replace('/', '?'); // SRU cannot search for '/' so '?' is used instead
			if (i<barcodesAll.size()-1) query+="+or+";
		}
		if ((ppnsAll.size()>0 || barcodesAll.size()>0) && sigsAll.size()>0) query+="+or+";
		
		for (int i=0;i<sigsAll.size();i++) {
			query+="pica.sgb=\""+sigsAll.get(i).replaceAll("[/,()]", " ")+"\""; //SGB with blanks and dot
			if (i<sigsAll.size()-1) query+="+or+";
		}
		query+=")";
		
		//retrieve current year and construct array for all years
		int year = Calendar.getInstance().get(Calendar.YEAR);
		int[] years = new int[year-2007+1];
		for (int i=2007;i<=year;i++) years[i-2007]=i;
		
		// retrieve Titles
		xr.retrieve(query, num, true, years);
		List<Title> titles = ((TitleArray) xr.getTitleWriter()).getTitles();
				
		//if (titles.isEmpty()) throw new QueryErrorException(105, "Empty Title list", "unknown");
		
		ArrayList<String> foundSig = new ArrayList<String>();
		
		// assign queries to Title objects
		for (Title t : titles) {
			String queryPPN = "";;
			for (String ppn : ppnsAll) {
				if (t.ppn.toLowerCase().compareTo(ppn.toLowerCase()) == 0) {
					queryPPN = ppn;
					break;
				}
			}
			if (!queryPPN.isEmpty()) {
				ppnsAll.remove(queryPPN);
				t.query = "ppn="+queryPPN;
			} else {
				String queryBar = "";;
				for (String bar : barcodesAll) {
					for (Copy c : t.copies) {
						if (c.barcode.toLowerCase().compareTo(bar.toLowerCase()) == 0) {
							queryBar = bar;
							break;
						}
					}
					if (!queryBar.isEmpty()) break;
				}
				if (!queryBar.isEmpty()) {
					barcodesAll.remove(queryBar);
					t.query = "bar="+queryBar;
				} else {
					String querySig = "";;
					for (String sig : sigsAll) {
						for (Copy c : t.copies) {
							if (c.signature.toLowerCase().contains(sig.toLowerCase())) {
								querySig = sig;
								break;
							}
						}
						if (!querySig.isEmpty()) break;
					}
					if (!querySig.isEmpty()) {
						//sigsAll.remove(querySig); //should not be done as signature search strings may be ambigious
						foundSig.add(querySig);
						t.query = "sig="+querySig;
					}
				}
			}
		}
		
		//retrieve Titles in Family
		if (familyTitles) {
			//get family PPNs
			List<List<String>> allFamilyPPNs = new ArrayList<List<String>>();
			for (int i=0;i<titles.size();i++) {
				Title t = titles.get(i);
				allFamilyPPNs.add(new ArrayList<String>());
				List<String> family = xr.getFamilyPPNs(t);
				allFamilyPPNs.get(i).addAll(family);
			}
			//remove duplicates
			for (int i=0;i<titles.size();i++) {
				Title t = titles.get(i);
				String duble = "";
				for (int j=0;j<allFamilyPPNs.size();j++) {
					for (String ppn : allFamilyPPNs.get(j)) {
						if (t.ppn.compareTo(ppn)==0) {
							duble = ppn;
							break;
						}
					}
					if (duble.length()>0) break;
				}
				
				if (duble.length()>0) for (int j=0;j<allFamilyPPNs.size();j++) allFamilyPPNs.get(j).remove(duble);
			}
			//request titles
			for (int j=0;j<allFamilyPPNs.size();j++) for (String ppn : allFamilyPPNs.get(j)) {
				xr.retrieve("(pica.ppn=\""+ppn+"\")", 1, true, years);
				Title t = ((TitleArray) xr.getTitleWriter()).getTitles().get(0);
				t.query = "fam="+titles.get(j).ppn;
				titles.add(t);
			}
		}
		
		// create missing queries
		for (String ppn : ppnsAll) titles.add(new Title("ppn="+ppn));
		for (String bar : barcodesAll) titles.add(new Title("bar="+bar));
		for (String sig : sigsAll) if (!foundSig.contains(sig)) titles.add(new Title("sig="+sig));
		
		Collections.sort(titles, new Comparator<Title>() {
		    public int compare(Title obj1, Title obj2) {
		        return obj2.year_of_creation.compareTo(obj1.year_of_creation);
		    }});
		
		return titles;
	}
	
	public void close() {
		this.xr.close();
	}
	
	public static void main(String[] args) {
		try {
			DropbillRetriever dr = new DropbillRetriever();
			//9 Bände Ac
			System.out.println(dr.retrievePPN(new String[] {}, new String[] {}, new String[] {}, new String[] {"037397818"}, true));
			//2 Bände Af
			System.out.println(dr.retrievePPN(new String[] {}, new String[] {}, new String[] {}, new String[] {"1758123745"}, true));
			//3 Bände AF
			System.out.println(dr.retrievePPN(new String[] {}, new String[] {}, new String[] {}, new String[] {"362253285"}, true));
			//2 Serien (selbe 2 Bände)
			System.out.println(dr.retrievePPN(new String[] {}, new String[] {}, new String[] {}, new String[] {"227167236"}, true));
			//2 Serien (2 Bände richtig, 5 Bände falsch)
			System.out.println(dr.retrievePPN(new String[] {}, new String[] {}, new String[] {}, new String[] {"031067344"}, true));
		} catch (ClassNotFoundException | SQLException | QueryErrorException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println();
	}

}
