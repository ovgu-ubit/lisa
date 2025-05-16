package retrieve;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import model.Title;
import model.Title.Copy;
import write.TitleWriter;

/**
 * main class for accessing SRU and DB interface, should be called by service
 * classes
 * 
 * @author sbosse
 *
 */
public class XMLReader implements TitleReader {

	QueryFactory qf;
	TitleWriter tw;

	DatabaseConnection db;
	DatabaseConnection db_local;

	int numPerQuery = 500;

	/**
	 * constructor to use if only SRU should be called
	 * 
	 * @param qf
	 * @param tw
	 */
	public XMLReader(QueryFactory qf, TitleWriter tw) {
		this(qf, tw, null, null);
	}

	/**
	 * constructor to use if DB should also be called for loan statistics
	 * 
	 * @param qf
	 * @param tw
	 * @param db
	 */
	public XMLReader(QueryFactory qf, TitleWriter tw, DatabaseConnection db, DatabaseConnection db_local) {
		this.qf = qf;
		this.tw = tw;
		this.db = db;
		this.db_local = db_local;
	}

	/**
	 * sets the maximum number of retrieved title (standard 500)
	 * 
	 * @param numPerQuery
	 */
	public void setMaxNumPerQuery(int numPerQuery) {
		this.numPerQuery = numPerQuery;
	}

	public List<Title> collected;

	public boolean retrieve(String searchStringCQL, int max_results, boolean write, int[] stat_years)
			throws QueryErrorException {
		return retrieve(searchStringCQL, max_results, 1, write, stat_years, false, false, false, true);
	}

	public boolean retrieve(String searchStringCQL, int max_results, int pos, boolean write, int[] stat_years)
			throws QueryErrorException {
		return retrieve(searchStringCQL, max_results, pos, write, stat_years, false, false, false, true);
	}

	/**
	 * main method to retrieve titles from SRU and DB, calls the TitleWriter at the
	 * beginning, at the end, and after a single title has been created
	 * 
	 * @param searchStringCQL          a search string in the Contextual Query
	 *                                 Language
	 *                                 https://www.loc.gov/standards/sru/cql/
	 * @param max_results              the maximum number of results that is
	 *                                 retrieved (if lower than results to be
	 *                                 expected)
	 * @param write                    if the results should be written out
	 *                                 constantly by the TitleWriter, use false if
	 *                                 you want to collect results of multiple
	 *                                 queries
	 * @param stat_years               an array of year values for which statistics
	 *                                 should be retrieved from DB
	 * @param orderInfos               if info of the respective order should be
	 *                                 retrieved from DB
	 * @param GVK_infos                if info of the content from GVK should be
	 *                                 accessed (last copy and Ex ULB Halle)
	 * @param classInfosFromSuperTitle if the child titles of a title should be
	 *                                 reported if they have no classification
	 * @throws QueryErrorException
	 */
	public boolean retrieve(String searchStringCQL, int max_results, boolean write, int[] stat_years,
			boolean orderInfos, boolean GVK_infos, boolean classInfosFromSuperTitle, boolean distinct)
			throws QueryErrorException {
		return retrieve(searchStringCQL, max_results, 1, write, stat_years, orderInfos, GVK_infos,
				classInfosFromSuperTitle, distinct);
	}

	DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	DocumentBuilder doc_builder;
	int count;
	Title tmpTitle;

	public int getCount() {
		return count;
	}

	/**
	 * method to retrieve the number of results by making a request for one result
	 * 
	 * @param searchStringCQL
	 * @return the number of results which is also stored in the count class
	 *         variable
	 * @throws QueryErrorException
	 */
	public int retrieveCount(String searchStringCQL) throws QueryErrorException {
		URL query = qf.getQueryCQL(searchStringCQL, 1, 1);
		this.retrieveXML(query, true, false);
		return count;
	}

	boolean debug = false;

	/**
	 * retrieve function using a starting position for the XML response
	 * 
	 * @param pos the first result to be retrieved
	 * @return if results have been retrieved successfully
	 */
	public boolean retrieve(String searchStringCQL, int max_results, int pos, boolean write, int[] stat_years,
			boolean orderInfos, boolean GVK_infos, boolean classInfosFromSuperTitle, boolean distinct)
			throws QueryErrorException {

		if (write)
			tw.init(searchStringCQL);
		else if (collected == null)
			collected = new ArrayList<Title>();

		// for every <numPerQuery> titles, a new query is created
		int queries = 1;

		if (max_results > 1 || pos > 1) {
			System.out.print("Retrieving total count: ");
			this.count = this.retrieveCount(searchStringCQL);
			System.out.println(this.count);
			if (this.count == 0) {
				System.out.println("No results could be retrieved");
				return false;
			}
			// resize the max_results to count
			max_results = this.count < (max_results + pos) ? this.count - pos + 1 : max_results;
		}
		queries = (int) Math.ceil(max_results / (this.numPerQuery + 0.0));

		for (int i = 0; i < queries; i++) {
			URL query = qf.getQueryCQL(searchStringCQL, max_results, pos);
			if (max_results > this.numPerQuery)
				query = qf.getQueryCQL(searchStringCQL,
						(i + 1) * this.numPerQuery > max_results ? max_results - i * this.numPerQuery
								: this.numPerQuery,
						pos + i * this.numPerQuery);
			// System.out.println("Retrieving "+searchStringCQL+", iteration "+i);

			// retrieve XML document from URL response
			this.tries = 0;
			Date start = new Date();
			NodeList nListRecords = this.retrieveXML(query, true, true);

			if (debug)
				System.out.println(
						"XML retrieval finished at " + ((System.currentTimeMillis() - start.getTime()) / 1000.0));

			if (nListRecords == null) {
				return false;
			}
			if (debug)
				start = new Date();
			double XMLtime = 0;
			double DBtime = 0;
			double writeTime = 0;
			double familyTime = 0;
			double GVKtime = 0;
			for (int temp = 0; temp < nListRecords.getLength(); temp++) {
				Node nNode = nListRecords.item(temp);
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {
					Element eElement = (Element) nNode;

					Title t;
					List<Title> subtitles = null;
					if (debug)
						start = new Date();
					if (tmpTitle == null)
						t = new Title(eElement);
					else { // title already from DB, only adding infos from SRU
						t = tmpTitle;
						t.retrieveFromXML(eElement);
					}
					if (debug)
						XMLtime += (System.currentTimeMillis() - start.getTime()) / 1000.0;
					if (debug)
						start = new Date();
					// if DB connection is defined, the loan statistic is retrieved from the DB
					if (this.db != null && stat_years != null && stat_years.length != 0) {
						try {
							t.addDBStats(this.db, stat_years);
						} catch (SQLException e) {
							e.printStackTrace();
							throw new QueryErrorException(300,
									"There was an error retrieving loan statistics from the DB.", "unknown");
						}
					}

					// get order infos
					if (this.db != null && orderInfos) {
						try {
							t.addOrderInfos(this.db);
						} catch (SQLException e) {
							throw new QueryErrorException(301, "There was an error retrieving order Infos from the DB.",
									"unknown");
						}
					}
					if (debug)
						DBtime += (System.currentTimeMillis() - start.getTime()) / 1000.0;

					if (debug)
						start = new Date();
					// Holen der Familieninfos
					if (tw.superInfos()) {
						if (!t.super_ppn.isEmpty() && t.classification.isEmpty()) {
							URL query2 = qf.getQueryCQL("pica.ppn=" + t.super_ppn, 1, 1);
							// retrieve XML document from URL response
							NodeList nListRecords2 = this.retrieveXML(query2, false, false);
							if (nListRecords2 != null)
								for (int temp2 = 0; temp2 < nListRecords2.getLength(); temp2++) {
									Node nNode2 = nListRecords2.item(temp2);
									if (nNode2.getNodeType() == Node.ELEMENT_NODE) {
										Element eElement2 = (Element) nNode2;
										t.addFamInfo(eElement2);
									}
								}
						}
					}
					if (debug)
						familyTime += (System.currentTimeMillis() - start.getTime()) / 1000.0;

					if (debug)
						start = new Date();
					// Holen der GVK-Infos
					if ((GVK_infos || this.qf.databaseURI.compareTo("k10plus") == 0) && !t.ppn.isEmpty()
							&& (t.material != null && t.material.charAt(0) != 'O')) {
						if (this.qf.databaseURI.compareTo("k10plus") == 0) { // already retrieving from GVK
							t.addGVKInfo(eElement);
						} else {
							try {
								URL query3 = new URL(
										"https://sru.k10plus.de/k10plus!levels=0,1?version=1.2&operation=searchRetrieve&query=pica.ppn=\""
												+ t.ppn + "\"&recordSchema=picaxml&maximumRecords=1&startRecord=1");
								NodeList nListRecords3 = this.retrieveXML(query3, false, false);
								if (nListRecords3 != null)
									for (int temp3 = 0; temp3 < nListRecords3.getLength(); temp3++) {
										Node nNode3 = nListRecords3.item(temp3);
										if (nNode3.getNodeType() == Node.ELEMENT_NODE) {
											Element eElement3 = (Element) nNode3;
											t.addGVKInfo(eElement3);
										}
									}
							} catch (MalformedURLException e) {
								throw new QueryErrorException(302, "There was an error in the GVK URL", "unknown");
							}
						}
					}
					if (debug)
						GVKtime += (System.currentTimeMillis() - start.getTime()) / 1000.0;

					// hole (unklassifizierte) f-Stufen einer klassifizierten c-Stufe
					boolean must_be_unclassified = true;

					if (!t.material.isEmpty() && t.material.length() > 1 && t.material.toLowerCase().charAt(1) == 'c'
							&& classInfosFromSuperTitle && !t.classification.isEmpty()) {
						URL query4 = qf.getQueryCQL(
								"pica.1049=" + t.ppn + "+and+pica.1001=\"b\"+and+pica.1045=\"rel-nt\"", 500, 1);
						NodeList nListRecords4 = this.retrieveXML(query4, false, false);
						subtitles = t.getSubTitles(nListRecords4, this.db_local);
						for (Title t1 : subtitles) {
							if ((!must_be_unclassified || t1.classification.isEmpty())) {
								if (t1.classification.isEmpty())
									t1.classification = t.classification + " [c]";
								if (this.db != null && stat_years != null && stat_years.length != 0) {
									try {
										t1.addDBStats(this.db, stat_years);
									} catch (SQLException e) {
										throw new QueryErrorException(300,
												"There was an error retrieving loan statistics from the DB.",
												"unknown");
									}
								}
								if (GVK_infos && (t.material != null && t.material.charAt(0) != 'O')) {
									try {
										URL query3 = new URL(
												"https://sru.k10plus.de/k10plus!levels=0,1?version=1.2&operation=searchRetrieve&query=pica.ppn=\""
														+ t1.ppn
														+ "\"&recordSchema=picaxml&maximumRecords=1&startRecord=1");
										NodeList nListRecords3 = this.retrieveXML(query3, false, false);
										if (nListRecords3 != null)
											for (int temp3 = 0; temp3 < nListRecords3.getLength(); temp3++) {
												Node nNode3 = nListRecords3.item(temp3);
												if (nNode3.getNodeType() == Node.ELEMENT_NODE) {
													Element eElement3 = (Element) nNode3;
													t1.addGVKInfo(eElement3);
												}
											}
									} catch (MalformedURLException e) {
										throw new QueryErrorException(302, "There was an error in the GVK URL",
												"unknown");
									}
								}
							}
						}
					}
					if (debug)
						start = new Date();

					// TitleWriter is called to process the title or titles are collected
					if (write) {
						tw.addTitle(t);
						if (subtitles != null)
							for (Title t1 : subtitles)
								if (!must_be_unclassified || t1.classification.contains("(c)"))
									tw.addTitle(t1);
						if (debug)
							writeTime += (System.currentTimeMillis() - start.getTime()) / 1000.0;
					} else if (!collected.contains(t) || !distinct) {
						collected.add(t);
						if (subtitles != null)
							for (Title t1 : subtitles)
								if (!must_be_unclassified || t1.classification.contains("(c)"))
									collected.add(t1);
					}
				}
			}
			if (debug) {
				System.out.println("XML time: " + XMLtime);
				System.out.println("DB time: " + DBtime);
				System.out.println("Write time: " + writeTime);
				System.out.println("Family time: " + familyTime);
				System.out.println("GVK time: " + GVKtime);
			}
		}
		// System.out.println("Query finished");
		if (write)
			tw.close();
		return true;
	}

	/**
	 * writes the currently retrieved title list to the TitleWriter and resets the
	 * list
	 */
	public String write() {
		return this.write("");
	}

	/**
	 * writes the currently retrieved title list to the TitleWriter and resets the
	 * list
	 */
	public String write(String prefix) {
		System.out.println("Writing titles, " + errors + " titles not found");
		String filename = tw.init(prefix + "_collected");
		for (Title t : collected) {
			tw.addTitle(t);
		}
		tw.close();
		collected.clear();
		errors = 0;
		return filename;
	}

	public String write(List<Title> titles) {
		System.out.println("Writing titles, " + errors + " titles not found");
		String filename = tw.init("_collected");
		for (Title t : titles) {
			tw.addTitle(t);
		}
		tw.close();
		errors = 0;
		return filename;
	}

	/**
	 * function to retrieve SRU information for already created title objects
	 * (usually from DB query)
	 * 
	 * @param titles
	 * @throws QueryErrorException
	 */
	public void retrieveFromTitles(List<Title> titles, boolean orderInfos) throws QueryErrorException {
		System.out.println("Retrieving " + titles.size() + " titles...");
		for (Title t : titles) {
			tmpTitle = t;
			if (!retrieve("pica.ppn=" + t.ppn + "?", 1, false, new int[] {}, orderInfos, false, false, true)) { // DB
																												// does
																												// not
				// include
				// leading
				// zeros
				boolean flag = retrieve("pica.ppn=0" + t.ppn + "?", 1, false, new int[] {}, orderInfos, false, false,
						true);
				if (!flag)
					errors++;
			}
			tmpTitle = null;
		}
	}

	public void retrieveFromEPNs(List<Title> titles, boolean orderInfos) throws QueryErrorException {
		System.out.println("Retrieving " + titles.size() + " titles...");
		for (Title t : titles) {
			tmpTitle = t;
			String epn = "";
			for (Copy c : t.copies) {
				if (!c.epn.isEmpty()) {
					epn = c.epn;
					break;
				}
			}
			if (epn.isEmpty()) {
				System.out.println("Error while processing EPN");
				continue;
			}
			if (!retrieve("pica.epn=" + epn + "?", 1, false, new int[] {}, orderInfos, false, false, false)) { // DB
																												// does
																												// not
																												// include
																												// leading
																												// zeros
				boolean flag = retrieve("pica.epn=0" + epn + "?", 1, false, new int[] {}, orderInfos, false, false,
						false);
				if (!flag)
					errors++;
			}
			tmpTitle = null;
		}
	}

	int errors = 0;

	int tries;

	/**
	 * function that sends the query and retrieves a NodeList
	 * 
	 * @param query
	 * @param changeCount if the count of the query is resetted with the count in
	 *                    this XML object
	 * @return
	 * @throws QueryErrorException
	 */
	NodeList retrieveXML(URL query, boolean changeCount, boolean printout) throws QueryErrorException {
		if (printout)
			System.out.println(query);
		try {
			doc_builder = dbf.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			throw new QueryErrorException(200, "There was an error in the XML configuration.", "unknown");
		}
		// retrieve XML document from URL response
		Document doc = null;

		InputSource is;
		try {
			is = new InputSource(new InputStreamReader(query.openStream(), "UTF-8"));
			is.setEncoding("UTF-8");
			doc = doc_builder.parse(is);
		} catch (IOException | SAXException e) {
			System.out.println(e);
			throw new QueryErrorException(201, "There was an error opening the SRU interface", "unknown");
		}
		int c = 0;
		try {
			c = Integer.parseInt(doc.getElementsByTagName("zs:numberOfRecords").item(0).getTextContent());
		} catch (Exception e) {
			System.out.println(e);
			System.out.println("Error while retrieving count, probably query not supported: " + query);
		}
		if (changeCount)
			count = c;

		if (c == 0)
			return null;

		// for each retrieved title, a zs:record tag is created within the zs:records
		// tag
		// if zs:records does not contain any element, throw a query exception
		if (doc.getElementsByTagName("zs:records").item(0) == null) {
			// System.out.println(doc.getElementsByTagName("zs:records"));
			// System.out.println(doc.getTextContent());
			System.out.println("Problems retrieving XML document, trial " + (++this.tries));
			if (this.tries > 2)
				throw new QueryErrorException(202,
						"The retrieved document does not match the expected syntax. Likely there was an error in the SRU interface",
						"unknown");
			else
				return retrieveXML(query, changeCount, printout);
		}

		doc.getDocumentElement().normalize();

		// tag zs:recordData within zs:record includes all relevant information and is
		// given to Title constructor
		return doc.getElementsByTagName("zs:recordData");
	}

	// https://wiki.k10plus.de/display/K10PLUS/SRU
	/**
	 * method retrieving a list of related PPNs based on a title object
	 * 
	 * @param t the title object
	 * @return a list of ppn strings from related titles
	 * @throws QueryErrorException
	 */
	public List<String> getFamilyPPNs(Title t) throws QueryErrorException {
		List<String> res = new ArrayList<String>();
		URL query4 = null;
		try {
			if (doc_builder == null)
				doc_builder = dbf.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			throw new QueryErrorException(200, "There was an error in the XML configuration.", "unknown");
		}

		if (t.material.toLowerCase().charAt(1) == 'f') {
			query4 = qf.getQueryCQL("pica.1049=\"" + t.super_ppn + "\"+and+pica.1001=\"b\"+and+pica.1045=\"fam\"", 500,
					1);
		} else if (t.material.toLowerCase().charAt(1) == 'c' || t.material.toLowerCase().charAt(1) == 'd') {
			query4 = qf.getQueryCQL("pica.1049=\"" + t.ppn + "\"+and+pica.1001=\"b\"+and+pica.1045=\"rel-nt\"", 500, 1);
		} else
			return res;

		NodeList nListRecords4 = this.retrieveXML(query4, false, true);
		List<Title> subtitles = Title.getSubTitles(nListRecords4, t.ppn, this.db_local);
		for (Title t1 : subtitles) {
			res.add(t1.ppn);
		}
		return res;
	}

	/**
	 * method retrieving a list of related PPNs based on a ppn string
	 * 
	 * @param ppn the ppn of the title
	 * @return a list of ppns from related titles
	 * @throws QueryErrorException
	 */
	public List<String> getFamilyPPNs(String ppn) throws QueryErrorException {
		List<String> res = new ArrayList<String>();
		URL query4 = null;
		try {
			if (doc_builder == null)
				doc_builder = dbf.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			throw new QueryErrorException(200, "There was an error in the XML configuration.", "unknown");
		}
		query4 = qf.getQueryCQL("pica.1049=\"" + ppn + "\"+and+pica.1001=\"b\"+and+pica.1045=\"rel-nt\"", 500, 1);

		NodeList nListRecords4 = this.retrieveXML(query4, false, true);
		List<Title> subtitles = Title.getSubTitles(nListRecords4, ppn, this.db_local);
		for (Title t1 : subtitles) {
			res.add(t1.ppn);
		}
		return res;
	}

	public TitleWriter getTitleWriter() {
		return this.tw;
	}

	/**
	 * closes DB connection
	 */
	public void close() {
		if (this.db != null)
			try {
				db.finalize();
			} catch (SQLException e) {
				e.printStackTrace();
			}
	}

	@Override
	public void finalize() {
		this.close();
	}

	public boolean isDBvalid() throws SQLException {
		return db != null && db.isValid();
	}
}
