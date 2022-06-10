package retrieve;

import java.net.MalformedURLException;
import java.net.URL;


/**
 * class that creates URLs for requests
 * @author sbosse
 *
 */
public class QueryFactory {
	
	String databaseURI;
	String sru_server = "http://sru.gbv.de/";
	String version ="1.2";
	String operation = "searchRetrieve";
	String recordSchema = "picaxml";

	/**
	 * constructor for accessing other stocks
	 * @param databaseURI a database URI from http://uri.gbv.de/database/ 
	 */
	public QueryFactory(String sru_server, String databaseURI) {
		this.sru_server = sru_server;
		this.databaseURI = databaseURI;
	}
	
	/**
	 * standard constructor using OPAC of UB MD
	 */
	public QueryFactory() {
		this.databaseURI = "opac-de-ma9"; //UB MD
		//this.databaseURI = "hb-magdeburg"; //Universitaetsbibliografie
	}
	
	/**
	 * method creating URL from CQL search string
	 * @param searchString CQL style search string https://www.loc.gov/standards/sru/cql/
	 * @param max_results the max_results parameter of the query, should not exceed 1000
	 * @param pos the position of the result set to start, e.g. 1 or 1001
	 * @return the URL to retrieve Pica-XML document
	 */
	public URL getQueryCQL(String searchString, int max_results, int pos) {
		try {
			return new URL(sru_server+
					databaseURI+
					"?version="+version+
					"&operation="+operation+
					"&query=" + searchString	+ 
					"&recordSchema="+recordSchema+
					"&maximumRecords=" + max_results + 
					"&startRecord=" + pos);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return null;
		}
	}
	
}
