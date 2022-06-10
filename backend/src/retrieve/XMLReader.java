package retrieve;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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
import model.Title.Copy.Statistic;
import write.TitleWriter;

/**
 * main class for accessing SRU and DB interface, should be called by service
 * classes
 * 
 * @author sbosse
 *
 */
public class XMLReader {

	QueryFactory qf;
	TitleWriter tw;

	DatabaseConnection db;

	int numPerQuery = 500;

	/**
	 * constructor to use if only SRU should be called
	 * 
	 * @param qf
	 * @param tw
	 */
	public XMLReader(QueryFactory qf, TitleWriter tw) {
		this(qf, tw, null);
	}

	/**
	 * constructor to use if DB should also be called for loan statistics
	 * 
	 * @param qf
	 * @param tw
	 * @param db
	 */
	public XMLReader(QueryFactory qf, TitleWriter tw, DatabaseConnection db) {
		this.qf = qf;
		this.tw = tw;
		this.db = db;
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
		return retrieve(searchStringCQL, max_results, 1, write, stat_years, false, false, false);
	}

	public boolean retrieve(String searchStringCQL, int max_results, int pos, boolean write, int[] stat_years)
			throws QueryErrorException {
		return retrieve(searchStringCQL, max_results, pos, write, stat_years, false, false, false);
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
			boolean orderInfos, boolean GVK_infos, boolean classInfosFromSuperTitle) throws QueryErrorException {
		return retrieve(searchStringCQL, max_results, 1, write, stat_years, orderInfos, GVK_infos,
				classInfosFromSuperTitle);
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
		this.retrieveXML(query, true);
		return count;
	}

	/**
	 * retrieve function using a starting position for the XML response
	 * 
	 * @param pos the first result to be retrieved
	 * @return if results have been retrieved successfully
	 */
	public boolean retrieve(String searchStringCQL, int max_results, int pos, boolean write, int[] stat_years,
			boolean orderInfos, boolean GVK_infos, boolean classInfosFromSuperTitle) throws QueryErrorException {

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
			NodeList nListRecords = this.retrieveXML(query, true);

			// System.out.println("XML retrieval finished at "+System.currentTimeMillis());

			if (nListRecords == null) {
				return false;
			}

			for (int temp = 0; temp < nListRecords.getLength(); temp++) {
				Node nNode = nListRecords.item(temp);
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {
					Element eElement = (Element) nNode;

					Title t;
					List<Title> subtitles = null;
					if (tmpTitle == null)
						t = new Title(eElement);
					else { // title already from DB, only adding infos from SRU
						t = tmpTitle;
						t.retrieveFromXML(eElement);
					}

					// if DB connection is defined, the loan statistic is retrieved from the DB
					if (this.db != null && stat_years != null && stat_years.length != 0) {
						try {
							t.addDBStats(this.db, stat_years);
						} catch (SQLException e) {
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

					// Holen der Familieninfos
					if (tw.superInfos()) {
						if (!t.superPPN.isEmpty() && t.classification.isEmpty()) {
							URL query2 = qf.getQueryCQL("pica.ppn=" + t.superPPN, 1, 1);
							// retrieve XML document from URL response
							NodeList nListRecords2 = this.retrieveXML(query2, false);
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

					// Holen der GVK-Infos
					if (GVK_infos) {
						if (this.qf.databaseURI.compareTo("gvk") == 0) { // already retrieving from GVK
							t.addGVKInfo(eElement);
						} else {
							try {
								URL query3 = new URL(
										"http://sru.k10plus.de/gvk?version=1.2&operation=searchRetrieve&query=pica.ppn=\""
												+ t.ppn + "\"&recordSchema=picaxml&maximumRecords=1&startRecord=1");
								NodeList nListRecords3 = this.retrieveXML(query3, false);
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

					// hole (unklassifizierte) f-Stufen einer klassifizierten c-Stufe
					boolean must_be_unclassified = true;

					if (t.material.toLowerCase().charAt(1) == 'c' && classInfosFromSuperTitle
							&& !t.classification.isEmpty()) {
						URL query4 = qf.getQueryCQL(
								"pica.1049=" + t.ppn + "+and+pica.1001=\"b\"+and+pica.1045=\"rel-nt\"", 500, 1);
						NodeList nListRecords4 = this.retrieveXML(query4, false);
						subtitles = t.getSubTitles(nListRecords4);
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
								if (GVK_infos) {
									try {
										URL query3 = new URL(
												"http://sru.k10plus.de/gvk?version=1.2&operation=searchRetrieve&query=pica.ppn=\""
														+ t1.ppn
														+ "\"&recordSchema=picaxml&maximumRecords=1&startRecord=1");
										NodeList nListRecords3 = this.retrieveXML(query3, false);
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

					// TitleWriter is called to process the title or titles are collected
					if (write) {
						tw.addTitle(t);
						if (subtitles != null)
							for (Title t1 : subtitles)
								if (!must_be_unclassified || t1.classification.contains("(c)"))
									tw.addTitle(t1);
					} else if (!collected.contains(t)) {
						collected.add(t);
						if (subtitles != null)
							for (Title t1 : subtitles)
								if (!must_be_unclassified || t1.classification.contains("(c)"))
									collected.add(t1);
					}
				}
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
	public void write() {
		this.write("");
	}

	/**
	 * writes the currently retrieved title list to the TitleWriter and resets the
	 * list
	 */
	public void write(String prefix) {
		System.out.println("Writing titles, "+errors+" titles not found");
		tw.init(prefix + "_collected");
		for (Title t : collected) {
			tw.addTitle(t);
		}
		tw.close();
		collected.clear();
		errors = 0;
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
			if (!retrieve("pica.ppn=" + t.ppn + "?", 1, false, new int[] {}, orderInfos, false, false)) { // DB does not
																											// include
																											// leading
																											// zeros
				boolean flag = retrieve("pica.ppn=0" + t.ppn + "?", 1, false, new int[] {}, orderInfos, false, false);
				if (!flag) errors++;
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
	NodeList retrieveXML(URL query, boolean changeCount) throws QueryErrorException {
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
			System.out.println("Error while retrieving count, probably query not supported");
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
				return retrieveXML(query, changeCount);
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
			query4 = qf.getQueryCQL("pica.1049=\"" + t.superPPN + "\"+and+pica.1001=\"b\"+and+pica.1045=\"fam\"", 500,
					1);
		} else if (t.material.toLowerCase().charAt(1) == 'c') {
			query4 = qf.getQueryCQL("pica.1049=\"" + t.ppn + "\"+and+pica.1001=\"b\"+and+pica.1045=\"rel-nt\"", 500, 1);
		} else
			return res;

		NodeList nListRecords4 = this.retrieveXML(query4, false);
		List<Title> subtitles = Title.getSubTitles(nListRecords4, t.ppn);
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

		NodeList nListRecords4 = this.retrieveXML(query4, false);
		List<Title> subtitles = Title.getSubTitles(nListRecords4, ppn);
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
				db.dbDisconnect();
			} catch (SQLException e) {
				System.err.println("There was an error trying to close the DB connection");
			}
	}

	@Override
	public void finalize() {
		this.close();
	}

	/**
	 * method to provide an analysis on the set of collected titles based on their
	 * classification
	 * 
	 * @param prefix the classification prefix to be considered
	 * @return a csv-style String with classes and statistics
	 */
	public String analyzeLSY(String prefix) {
		String res = "";
		List<String> classes = new ArrayList<String>();
		// find all classes
		for (Title t : collected) {
			if (t.classification.isEmpty()
					|| !t.classification.replaceAll("[/+]", " ").startsWith(prefix.replaceAll("[/+]", " ")))
				continue;
			try {
				String lsy = t.classification.split(" ")[0] + " " + t.classification.split(" ")[1];
				if (!classes.contains(lsy))
					classes.add(lsy);
			} catch (ArrayIndexOutOfBoundsException e) {
			}
		}
		// sort classification by length of String to avoid higher class assignment
		Collections.sort(classes, Collections.reverseOrder());

		// numbers are summed up for loans, loans of last 5 years, reservations
		List<Integer> loans = new ArrayList<Integer>(classes.size());
		List<Integer> loans5 = new ArrayList<Integer>(classes.size());
		List<Integer> reserv = new ArrayList<Integer>(classes.size());
		// total titles, total copies, titles never on loan and copies in stacks
		List<Integer> titlesTotal = new ArrayList<Integer>(classes.size());
		List<Integer> copiesTotal = new ArrayList<Integer>(classes.size());
		List<Integer> titlesNever = new ArrayList<Integer>(classes.size());
		List<Integer> copiesMag = new ArrayList<Integer>(classes.size());
		// titles which are older than 20 years, which of them (and copies) were not on
		// loan for 20 years; or had not more than 3 loans
		List<Integer> titles20 = new ArrayList<Integer>(classes.size());
		List<Integer> titles20not = new ArrayList<Integer>(classes.size());
		List<Integer> copies20not = new ArrayList<Integer>(classes.size());
		List<Integer> titles20three = new ArrayList<Integer>(classes.size());
		List<Integer> copies20three = new ArrayList<Integer>(classes.size());

		for (int i = 0; i < classes.size(); i++) {
			loans.add(0);
			loans5.add(0);
			reserv.add(0);
			titlesTotal.add(0);
			copiesTotal.add(0);
			titlesNever.add(0);
			copiesMag.add(0);

			titles20.add(0);
			titles20not.add(0);
			copies20not.add(0);
			titles20three.add(0);
			copies20three.add(0);
		}

		// assign title values to classes
		int year = Calendar.getInstance().get(Calendar.YEAR);
		for (Title t : collected) {
			for (int i = 0; i < classes.size(); i++) {
				if (!t.classification.startsWith(classes.get(i)))
					continue;
				else {
					loans.set(i, loans.get(i) + t.cum_loans);
					reserv.set(i, reserv.get(i) + t.cum_reserv);
					titlesTotal.set(i, titlesTotal.get(i) + 1);
					int co = 0;
					int m = 0;
					int l = 0;
					for (Copy c : t.copies) {
						if (!c.isLocked()) {
							co++;
							if (c.location.startsWith("Magazin"))
								m++;
						}
						for (Statistic s : c.stats) {
							if (s.year.contains("2007") || ((year - Integer.parseInt(s.year)) > 5))
								continue;
							l += s.num_loans;
						}
					}
					loans5.set(i, loans5.get(i) + l);
					copiesTotal.set(i, copiesTotal.get(i) + co);
					copiesMag.set(i, copiesMag.get(i) + m);
					if (t.cum_loans == 0 && co > 0)
						titlesNever.set(i, titlesNever.get(i) + 1);

					try {
						if (year - Integer.parseInt(t.year_of_creation) > 20) {
							titles20.set(i, titles20.get(i) + 1);
							if (t.cum_loans == 0) {
								titles20not.set(i, titles20not.get(i) + 1);
								copies20not.set(i, copies20not.get(i) + co); // alle Ex
							} else if (t.cum_loans <= 3) {
								titles20three.set(i, titles20three.get(i) + 1);
								if (co > 1)
									copies20three.set(i, copies20three.get(i) + co - 1); // nur Mehrfach
							}
						}
					} catch (NumberFormatException e) {
						System.out.println("Creation year " + t.year_of_creation + " could not be parsed, skipping...");
					}

					break;
				}
			}
		}
		// header
		res += "Sachgebiet;" + "Anzahl Titel;" + "Anzahl Exemplare nicht gesperrt;" + "Anzahl Entleihungen;"
				+ "Anzahl Entleihungen letzte 5 Jahre;" + "Anzahl Vormerkungen;" + "Anzahl Titel nicht ausgeliehen;"
				+ "Anzahl Exemplare Magazin;" + "Anzahl Titel älter als 20 Jahre;" + "davon nicht ausgeliehen;"
				+ "mit Exemplarzahl;" + "davon 1-3 Mal ausgeliehen;" + "Mehrfachexemplare\n";
		// rows
		for (int i = 0; i < classes.size(); i++) {
			res += classes.get(i) + ";" + titlesTotal.get(i) + ";" + copiesTotal.get(i) + ";" + loans.get(i) + ";"
					+ loans5.get(i) + ";" + reserv.get(i) + ";" + titlesNever.get(i) + ";" + copiesMag.get(i) + ";"
					+ titles20.get(i) + ";" + titles20not.get(i) + ";" + copies20not.get(i) + ";" + titles20three.get(i)
					+ ";" + +copies20three.get(i) + "\n";
		}
		// sum
		res += "\n";
		res += "Summe;" + titlesTotal.stream().collect(Collectors.summingInt(Integer::intValue)) + ";"
				+ copiesTotal.stream().collect(Collectors.summingInt(Integer::intValue)) + ";"
				+ loans.stream().collect(Collectors.summingInt(Integer::intValue)) + ";"
				+ loans5.stream().collect(Collectors.summingInt(Integer::intValue)) + ";"
				+ reserv.stream().collect(Collectors.summingInt(Integer::intValue)) + ";"
				+ titlesNever.stream().collect(Collectors.summingInt(Integer::intValue)) + ";"
				+ copiesMag.stream().collect(Collectors.summingInt(Integer::intValue)) + ";"
				+ titles20.stream().collect(Collectors.summingInt(Integer::intValue)) + ";"
				+ titles20not.stream().collect(Collectors.summingInt(Integer::intValue)) + ";"
				+ copies20not.stream().collect(Collectors.summingInt(Integer::intValue)) + ";"
				+ titles20three.stream().collect(Collectors.summingInt(Integer::intValue)) + ";"
				+ copies20three.stream().collect(Collectors.summingInt(Integer::intValue)) + "\n";
		return res;
	}
}
