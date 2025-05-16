package services;

import java.util.List;

import model.Title;
import retrieve.QueryErrorException;
import retrieve.QueryFactory;
import retrieve.XMLReader;
import write.TitleArray;

/**
 * class for retrieving titles from the university bibliography
 * 
 * @author sbosse
 *
 */
public class UniBibliography {

	public XMLReader xr;

	public UniBibliography(XMLReader xr) {
		this.xr = xr;
	}

	/**
	 * standard constructor using TitleArrayWriter
	 */
	public UniBibliography() {
		this.xr = new XMLReader(new QueryFactory("http://sru.k10plus.de/", "hb-magdeburg"), new TitleArray(false), null,
				null);
	}

	public List<Title> getTitles(int year, int pos, int max) throws QueryErrorException {
		String query = "(pica.sys=\"j" + year + "\")";
		xr.retrieve(query, max, pos, true, null);
		List<Title> titles = ((TitleArray) xr.getTitleWriter()).getTitles();
		return titles;
	}

	public int retrieveCount(int year) throws QueryErrorException {
		String query = "(pica.sys=\"j" + year + "\")";
		return xr.retrieveCount(query);
	}

	public void close() {
		this.xr.close();
	}

}
