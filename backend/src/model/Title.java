package model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import model.Title.Copy.Statistic;
import retrieve.DatabaseConnection;

/**
 * class encapsulating all relevant information of a title (norm data, title
 * data, local data) as well as a list of Copy and Statistics objects can
 * construct Title objects from raw data must be expanded for further use cases
 * 
 * @author sbosse
 *
 */
public class Title {
	// query attribute is used to encapsulate information regarding how the title
	// was retrieved
	public String query = "";

	// title attributes based on PICA+ format
	public String old_ppn = ""; // 003D $0
	public String ppn = ""; // 003@ $0
	public String material_code = ""; // 002E $b
										// //http://swbtools.bsz-bw.de/cgi-bin/k10plushelp.pl?cmd=kat&val=0503&katalog=Standard
	public String material = ""; // 002@ $0 (3 Pos)
	public String isbn = ""; // 004A $0
	public String language_text = ""; // 010@ $a
	public String language_orig = ""; // 010@ $c
	public String year_of_creation = ""; // 011@ $a
	public String type = ""; // 013D $8
	public String link = ""; // 017C $u
	public String scribal = ""; // 017L $a
	public String scribal_local = ""; // 209B/X79 $a
	// ...
	public String title = ""; // 021A $a | 036C $a (Haupttitel)
	public String super_title = ""; // 036E $a
	// ...
	// public String author_ppn = ""; // 028A $9
	public String author = ""; // 028A $8 (Expansion) $a,d $A,D
	public String co_authors = ""; // 028C $8 + 028B $8
	// public String co_authors_ppn = ""; // 028C $9 + 028B $9
	// ...
	public String greaterEntityYear = ""; // 031A $j
	// ...
	public String edition = ""; // 032@ $a
	// ...
	public String publisher = ""; // 033A $n
	public String publisher_location = ""; // 033A $p
	public String volume = ""; // 036C $l
	public String super_ppn = ""; // 036D $9
	public String greater_entity = ""; // 039B $8 (Expansion), $t (Titel), $C (Code = ISSN/ZDB/...), $6 (ID)
	public String greater_entity_issn = "";
	public String ddc = ""; // 045F $a
	public String bkl = ""; // 045Q/0X $8
	public String rvk = ""; // 045R/0X $8
	// ...
	public String local_expansion = ""; // 144Z $8
	public String internal_codes = ""; // 145B $a
	public String classification = ""; // 145Z $a
	public String doi = ""; // 004V $0

	// Copy list of title
	public List<Copy> copies;

	// statistics
	public int cum_loans;
	public int cum_reserv;
	public int num_copies_cum;
	public int num_copies_loan;

	// GVK information
	public boolean last_copy_gvk = true;
	public boolean copy_halle = false;
	public int num_lib = 0;
	public boolean copy_md = false;
	public String iln_list = "";

	// subject information
	public String subject = "";

	public String license = "";

	/**
	 * constructs an empty Title object as kind of error message
	 * 
	 * @param query the string that has been queried but not retrieved
	 */
	public Title(String query) {
		this.query = query;
	}

	/**
	 * constructs a Title object from an XML element from SRU interface
	 * 
	 * @param eElement the XML element zs:record
	 */
	public Title(Element eElement) {
		retrieveFromXML(eElement);
	}

	public Title(ResultSet rs, boolean order, boolean stats, boolean loans) throws SQLException {
		retrieveFromLocalDB(rs, order, stats, loans);
	}

	/**
	 * title constructor for retrieving single copies from local DB with already
	 * retrieved stats from LBSDB
	 * 
	 * @param rs
	 * @param copies a list of pre-filled copy objects
	 * @throws SQLException
	 */
	public Title(ResultSet rs, List<Copy> copies) throws SQLException {
		retrieveFromLocalDB(rs, false, false, false);

		List<Copy> copies_new = new ArrayList<Copy>();
		for (Copy c : this.copies) {
			Copy orig = null;
			for (Copy c1 : copies) {
				if (c.epn.matches("0?" + c1.epn + "[0-9X]")) {
					orig = c1;
					break;
				}
			}
			if (orig != null) {
				if (orig.loan_date != null)
					c.loan_date = orig.loan_date;
				copies_new.add(c);
			}
		}
		this.copies = copies_new;
	}

	/**
	 * constructs a title object from results of a DB statistics query
	 * 
	 * @param ppn        (DB) ppn of the title
	 * @param num_loans  the cumulative number of loans
	 * @param num_reserv the cumulative number of reservations
	 * @param num_cop    the number of copies with statistics
	 */
	public Title(String ppn, int num_loans, int num_reserv, int num_cop) {
		this.ppn = ppn;
		this.cum_loans = num_loans;
		this.cum_reserv = num_reserv;
		this.num_copies_cum = num_cop;
		this.copies = new ArrayList<Copy>();
	}

	public Title(String epn, String order_id, int occurrence) {
		this.copies = new ArrayList<Copy>();
		Copy c = new Copy();
		c.epn = epn;
		c.orderID = order_id;
		for (int i = 1; i < occurrence; i++)
			this.copies.add(new Copy());
		this.copies.add(c);
	}

	/**
	 * gets the title attributes from an XML element <xs:recordData>
	 * 
	 * @param eElement
	 */
	public void retrieveFromXML(Element eElement) {
		// eElement is the record element from the recordData list element
		if (this.copies == null || this.copies.isEmpty())
			this.copies = new LinkedList<Title.Copy>();
		NodeList nListRecord = eElement.getElementsByTagName("datafield");
		// nListRecord is the list of datafield elements that have the attribute "tag"
		// indicating pica+ code

		for (int temp2 = 0; temp2 < nListRecord.getLength(); temp2++) { // go over all datafield elements
			Node rNode = nListRecord.item(temp2);
			int idx; // current copy idx
			Copy c; /// current copy

			String s;
			if (rNode.getNodeType() == Node.ELEMENT_NODE) {
				Element tagElement = (Element) rNode;
				switch (tagElement.getAttribute("tag")) { // reading pica+ tag
				case "002E": // Materialcode alt
					this.material_code = getSubfield(tagElement, "b");
					break;
				case "002@": // Material
					this.material = getSubfield(tagElement, "0"); // code such as Aau
					this.material = this.material.substring(0, 2); // only first 2 positions relevant for material
					break;
				case "003D": // old PPN
					this.old_ppn = getSubfield(tagElement, "0");
					break;
				case "003@": // PPN
					/*
					 * <datafield tag="003@"> <subfield code="0">780642740</subfield> </datafield>
					 */
					this.ppn = getSubfield(tagElement, "0");
					break;
				case "004A": // ISBN
					String tmp = getSubfield(tagElement, "0");
					String tmp1 = getSubfield(tagElement, "A");
					if (tmp.length() > 0)
						this.isbn += tmp + " | ";
					if (tmp1.length() > 0)
						this.isbn += tmp1 + " | ";
					break;
				case "004V": // DOI
					this.doi = getSubfield(tagElement, "0");
					break;
				case "010@": // Sprache
					this.language_text = getSubfield(tagElement, "a");
					this.language_orig = getSubfield(tagElement, "c");
					break;
				case "011@": // Jahr
					this.year_of_creation = getSubfield(tagElement, "a");
					break;
				case "013D": // Art des Inhalts
					this.type += getSubfield(tagElement, "a") + " | ";
					break;
				case "017C": // Link
					this.link = getSubfield(tagElement, "u");
					this.license = getSubfield(tagElement, "4");
					break;
				case "017L": // Sigel
					this.scribal += getSubfield(tagElement, "a") + " " + getSubfield(tagElement, "b") + " | ";
					break;
				case "021A": // Titel und Titelzusatz
					this.title += getSubfield(tagElement, "a") + ": " + getSubfield(tagElement, "d");
					this.title = this.title.replaceAll("@", ""); // for OPAC links?
					if (this.title.substring(this.title.length() - 2).compareTo(": ") == 0)
						this.title = this.title.substring(0, this.title.length() - 2);
					this.title = this.title + " | ";
					break;
				case "028A": // Autor
					this.author = getSubfield(tagElement, "8"); // Expansion
					if (this.author == "")
						this.author = getSubfield(tagElement, "A") + ", " + getSubfield(tagElement, "D"); // Surname +
																											// First
																											// Name
					if (this.author.compareTo(", ") == 0)
						this.author = getSubfield(tagElement, "a") + ", " + getSubfield(tagElement, "d"); // Surname +
																											// First
																											// Name
					break;
				case "028B": // Other Authors
					s = "";
					s = getSubfield(tagElement, "8"); // Expansion
					if (s == "")
						s = getSubfield(tagElement, "A") + ", " + getSubfield(tagElement, "D"); // Surname + First Name
					if (s.compareTo(", ") == 0)
						s = getSubfield(tagElement, "a") + ", " + getSubfield(tagElement, "d"); // Surname + First Name
					this.co_authors += s + "|";
					break;
				case "028C": // Other Persons
					s = "";
					s = getSubfield(tagElement, "8"); // Expansion
					if (s == "")
						s = getSubfield(tagElement, "A") + ", " + getSubfield(tagElement, "D"); // Surname + First Name
					if (s.compareTo(", ") == 0)
						s = getSubfield(tagElement, "a") + ", " + getSubfield(tagElement, "d"); // Surname + First Name
					this.co_authors += s + "|";
					break;
				case "031A": // Groessere Einheit
					this.greaterEntityYear = getSubfield(tagElement, "j");
					break;
				case "032@": // edition
					this.edition = getSubfield(tagElement, "a");
					break;
				case "033A": // Verlag
					this.publisher = getSubfield(tagElement, "n");
					this.publisher_location = getSubfield(tagElement, "p");
					break;
				case "036C": // Gesamttitel
					this.title += getSubfield(tagElement, "a") + " | ";
					this.volume += getSubfield(tagElement, "l") + " | ";
					break;
				case "036D": // mehrteilige Monografie
					this.super_ppn = getSubfield(tagElement, "9");
					break;
				case "036E": // Titel der Reihe
					this.super_title = getSubfield(tagElement, "a");
					this.volume += getSubfield(tagElement, "l") + " | ";
					break;
				case "036F": // Titel der Reihe
					this.title += getSubfield(tagElement, "a") + " | ";
					break;
				case "039B": // Groessere Einheit
					this.greater_entity = getSubfield(tagElement, "t");
					this.greater_entity_issn = getSubfieldISSN(tagElement);
					if (this.publisher.isEmpty())
						this.publisher = getSubfield(tagElement, "e");
					break;
				case "045F": // DDC
					this.ddc += getSubfield(tagElement, "a");
					break;
				case "045Q": // BKL
					String first = getSubfield(tagElement, "8");
					String second = getSubfield(tagElement, "a");
					if (!first.isEmpty())
						this.bkl += first + " | ";
					if (!second.isEmpty())
						this.bkl += second + " | ";
					break;
				case "045R": // RVK
					first = getSubfield(tagElement, "8");
					second = getSubfield(tagElement, "a");
					if (!first.isEmpty())
						this.rvk += first + " | ";
					if (!second.isEmpty())
						this.rvk += second + " | ";
					break;
				case "144Z": // Lokale Schlagwoerter
					// more than one occurence possible
					first = getSubfield(tagElement, "8");
					second = getSubfield(tagElement, "a");
					if (!first.isEmpty())
						this.local_expansion += first + " | ";
					if (!second.isEmpty())
						this.local_expansion += second + " | ";
					break;
				case "145B": // UBAR
					this.internal_codes += getSubfield(tagElement, "a") + " | ";
					break;
				case "145Z": // Sachgebiet
					// more than one occurence possible
					this.classification += getSubfield(tagElement, "a") + " | ";
					break;
				case "201@": // Exemplar Infos
					idx = Integer.valueOf(tagElement.getAttribute("occurrence")) - 1; // identify current copy
					while (idx >= copies.size())
						copies.add(new Copy());
					c = copies.get(idx);
					c.status = getSubfield(tagElement, "a");
					c.loan_date = getSubfield(tagElement, "d");
					break;
				case "201B": // Aenderungsinfos
					idx = Integer.valueOf(tagElement.getAttribute("occurrence")) - 1; // identify current copy
					while (idx >= copies.size())
						copies.add(new Copy());
					c = copies.get(idx);
					c.edit_date = getSubfield(tagElement, "0");
					break;
				case "202D": // Aenderungsinfos
					idx = Integer.valueOf(tagElement.getAttribute("occurrence")) - 1; // identify current copy
					while (idx >= copies.size())
						copies.add(new Copy());
					c = copies.get(idx);
					if (c.edit_date.length() == 0)
						c.edit_date = getSubfield(tagElement, "0");
					break;
				case "203@": // EPN
					idx = Integer.valueOf(tagElement.getAttribute("occurrence")) - 1; // identify current copy
					while (idx >= copies.size())
						copies.add(new Copy());
					c = copies.get(idx);
					c.epn = getSubfield(tagElement, "0");
					// c.epn = c.epn.substring(0,c.epn.length()-1);
					break;
				case "208@": // Selektionsschlüssel
					idx = Integer.valueOf(tagElement.getAttribute("occurrence")) - 1; // identify current copy
					while (idx >= copies.size())
						copies.add(new Copy());
					c = copies.get(idx);
					c.selection_key = getSubfield(tagElement, "b");
					break;
				case "209A": // Bereich + Signatur + Ausleihindikator
					if (getSubfield(tagElement, "x").compareTo("00") == 0) { // first and relevant signature field
						idx = Integer.valueOf(tagElement.getAttribute("occurrence")) - 1; // identify current copy
						while (idx >= copies.size())
							copies.add(new Copy());
						c = copies.get(idx);
						c.location = getSubfield(tagElement, "f");
						c.signature = getSubfield(tagElement, "a") + " | ";
						c.loan_indicator = getSubfield(tagElement, "d");
						c.loan_indicator = c.loan_indicator.isEmpty() ? "u" : c.loan_indicator; // empty loan indicator
																								// means for loan in LBS
					} else if (getSubfield(tagElement, "x").compareTo("09") == 0) { // hidden signature
						idx = Integer.valueOf(tagElement.getAttribute("occurrence")) - 1; // identify current copy
						while (idx >= copies.size())
							copies.add(new Copy());
						c = copies.get(idx);
						c.signature += getSubfield(tagElement, "a") + " | ";
					} else { // other signature fields
						// currently no use
					}
					break;
				case "209B": // Sigel
					if (getSubfield(tagElement, "x").compareTo("79") == 0)
						this.scribal_local += getSubfield(tagElement, "a") + " | ";
					break;
				case "209G": // Barcode
					idx = Integer.valueOf(tagElement.getAttribute("occurrence")) - 1; // identify current copy
					while (idx >= copies.size())
						copies.add(new Copy());
					c = copies.get(idx);
					c.barcode = getSubfield(tagElement, "a");
					break;
				case "209O": // Abrufzeichen
					idx = Integer.valueOf(tagElement.getAttribute("occurrence")) - 1; // identify current copy
					while (idx >= copies.size())
						copies.add(new Copy());
					c = copies.get(idx);
					c.call_sign += getSubfield(tagElement, "a") + " | ";
					break;
				case "231B": // Bestandsangaben im Anzeigeformat
					idx = Integer.valueOf(tagElement.getAttribute("occurrence")) - 1; // identify current copy
					while (idx >= copies.size())
						copies.add(new Copy());
					c = copies.get(idx);
					c.inventory_string = getSubfield(tagElement, "a");
					break;
				case "237A": // Kommentar 1
					idx = Integer.valueOf(tagElement.getAttribute("occurrence")) - 1; // identify current copy
					while (idx >= copies.size())
						copies.add(new Copy());
					c = copies.get(idx);
					c.remark += getSubfield(tagElement, "a") + " | ";
					break;

				case "220B": // Kommentar 2 bib-intern
					idx = Integer.valueOf(tagElement.getAttribute("occurrence")) - 1; // identify current copy
					while (idx >= copies.size())
						copies.add(new Copy());
					c = copies.get(idx);
					c.remark_intern += getSubfield(tagElement, "a") + " | ";
					break;

				case "245Z": // weitere Systematik
					idx = Integer.valueOf(tagElement.getAttribute("occurrence")) - 1; // identify current copy
					while (idx >= copies.size())
						copies.add(new Copy());
					c = copies.get(idx);
					c.local_sys += getSubfield(tagElement, "a") + " | ";
					break;
				}
			}
		}
		if (!this.co_authors.isEmpty())
			this.co_authors = this.co_authors.substring(0, this.co_authors.length() - 1); // delete last pipe
		if (!this.classification.isEmpty())
			this.classification = this.classification.substring(0, this.classification.length() - 3); // delete last
																										// pipe
		if (!this.title.isEmpty())
			this.title = this.title.substring(0, this.title.length() - 3); // delete last pipe
		if (!this.local_expansion.isEmpty())
			this.local_expansion = this.local_expansion.substring(0, this.local_expansion.length() - 3); // delete last
																											// pipe
		if (!this.internal_codes.isEmpty())
			this.internal_codes = this.internal_codes.substring(0, this.internal_codes.length() - 3); // delete last
																										// pipe
		if (!this.type.isEmpty())
			this.type = this.type.substring(0, this.type.length() - 3); // delete last pipe
		if (!this.bkl.isEmpty())
			this.bkl = this.bkl.substring(0, this.bkl.length() - 3); // delete last pipe
		if (!this.isbn.isEmpty())
			this.isbn = this.isbn.substring(0, this.isbn.length() - 3); // delete last pipe
		if (!this.volume.isEmpty())
			this.volume = this.volume.substring(0, this.volume.length() - 3); // delete last pipe
		if (!this.scribal.isEmpty())
			this.scribal = this.scribal.substring(0, this.scribal.length() - 3); // delete last pipe
		if (!this.scribal_local.isEmpty())
			this.scribal_local = this.scribal_local.substring(0, this.scribal_local.length() - 3); // delete last pipe
		for (Copy c : this.copies) {
			if (!c.remark.isEmpty())
				c.remark = c.remark.substring(0, c.remark.length() - 3); // delete last pipe
			if (!c.remark_intern.isEmpty())
				c.remark_intern = c.remark_intern.substring(0, c.remark_intern.length() - 3); // delete last pipe
			if (!c.local_sys.isEmpty())
				c.local_sys = c.local_sys.substring(0, c.local_sys.length() - 3); // delete last pipe
			if (!c.call_sign.isEmpty())
				c.call_sign = c.call_sign.substring(0, c.call_sign.length() - 3); // delete last pipe
			if (!c.signature.isEmpty())
				c.signature = c.signature.substring(0, c.signature.length() - 3); // delete last pipe
		}
		List<Copy> toDel = new LinkedList<Copy>();
		// delete dummy copies
		for (Copy c : copies)
			if (c.epn.isEmpty())
				toDel.add(c);
		for (Copy c : toDel)
			copies.remove(c);
	}

	public void retrieveFromLocalDB(ResultSet rs, boolean order, boolean stats, boolean loans) throws SQLException {
		if (rs == null)
			return;
		this.ppn = rs.getString("ppn");
		this.title = rs.getString("title");
		this.volume = rs.getString("volume");
		this.author = rs.getString("author");
		this.edition = rs.getString("edition");
		this.year_of_creation = rs.getString("year_of_creation");
		this.classification = rs.getString("classification");
		this.type = rs.getString("type");
		this.material = rs.getString("material");
		this.isbn = rs.getString("isbn");
		this.language_text = rs.getString("language_text");
		this.link = rs.getString("link");
		this.publisher = rs.getString("publisher");
		this.publisher_location = rs.getString("publisher_location");
		this.super_title = rs.getString("super_title");
		this.super_ppn = rs.getString("super_ppn");
		this.ddc = rs.getString("ddc");
		this.bkl = rs.getString("bkl");
		this.rvk = rs.getString("rvk");
		this.local_expansion = rs.getString("local_expansion");
		this.internal_codes = rs.getString("internal_codes");
		this.last_copy_gvk = rs.getBoolean("last_copy_gvk");
		this.copy_halle = rs.getBoolean("copy_halle");
		this.num_lib = rs.getInt("num_lib");
		this.scribal = rs.getString("scribal");
		this.scribal_local = rs.getString("scribal_local");
		this.copies = new ArrayList<Copy>();
		this.cum_loans = 0;
		this.cum_reserv = 0;
		do {
			Copy c = new Copy();
			c.epn = rs.getString("epn");
			c.edit_date = rs.getString("edit_date");
			c.selection_key = rs.getString("selection_key");
			c.signature = rs.getString("signature");
			c.location = rs.getString("location");
			c.loan_indicator = rs.getString("loan_indicator");
			c.barcode = rs.getString("barcode");
			c.remark = rs.getString("remark");
			c.remark_intern = rs.getString("remark_intern");
			c.local_sys = rs.getString("local_sys");
			c.inventory_string = rs.getString("inventory_string");
			c.call_sign = rs.getString("call_sign");

			if (order) {
				c.orderID = rs.getString("order_id_nr");
				c.orderStatus = rs.getString("orderstatus_code");
				c.orderType = rs.getString("ordertype_code");
			}
			if (stats) {
				c.stats = new ArrayList<Statistic>();
				do {
					Statistic s = c.new Statistic(rs.getString("year"), rs.getInt("cum_loans"),
							rs.getInt("cum_reservations"));
					if (s.year != null) {
						c.stats.add(s);
						this.cum_loans += s.num_loans;
						this.cum_reserv += s.num_reserv;
					}
					if (order && c.orderID != null && !c.orderID.contains(rs.getString("order_id_nr"))) {
						c.orderID += " " + rs.getString("order_id_nr");
						c.orderStatus += " " + rs.getString("orderstatus_code");
						c.orderType += " " + rs.getString("ordertype_code");
					}
				} while (rs.next() && rs.getString("epn").compareTo(c.epn) == 0);
				if (!rs.isAfterLast())
					rs.previous();
			}
			if (loans) {
				c.loan_date = rs.getString("expiry_date_loan");
			}
			this.copies.add(c);
		} while (rs.next() && rs.getString("ppn").compareTo(this.ppn) == 0);
		if (!rs.isAfterLast())
			rs.previous();
	}

	public void addLocalDBStats(ResultSet rs) throws SQLException {
		if (rs == null)
			return;
		this.ppn = rs.getString("ppn");
		this.title = rs.getString("title");
		this.volume = rs.getString("volume");
		this.author = rs.getString("author");
		this.edition = rs.getString("edition");
		this.year_of_creation = rs.getString("year_of_creation");
		this.classification = rs.getString("classification");
		this.type = rs.getString("type");
		this.material = rs.getString("material");
		this.isbn = rs.getString("isbn");
		this.language_text = rs.getString("language_text");
		this.link = rs.getString("link");
		this.publisher = rs.getString("publisher");
		this.publisher_location = rs.getString("publisher_location");
		this.super_title = rs.getString("super_title");
		this.ddc = rs.getString("ddc");
		this.bkl = rs.getString("bkl");
		this.rvk = rs.getString("rvk");
		this.local_expansion = rs.getString("local_expansion");
		this.internal_codes = rs.getString("internal_codes");
		this.last_copy_gvk = rs.getBoolean("last_copy_gvk");
		this.copy_halle = rs.getBoolean("copy_halle");
		this.num_lib = rs.getInt("num_lib");
		do {
			Copy c = new Copy();
			c.epn = rs.getString("epn");
			c.edit_date = rs.getString("edit_date");
			c.selection_key = rs.getString("selection_key");
			c.signature = rs.getString("signature");
			c.location = rs.getString("location");
			c.loan_indicator = rs.getString("loan_indicator");
			c.barcode = rs.getString("barcode");
			c.remark = rs.getString("remark");
			c.remark_intern = rs.getString("remark_intern");
			c.local_sys = rs.getString("local_sys");

			/*
			 * c.orderID = rs.getString("order_id_nr"); c.orderStatus =
			 * rs.getString("orderstatus_code"); c.orderType =
			 * rs.getString("ordertype_code");
			 */

			/*
			 * c.stats = new ArrayList<Statistic>(); do { Statistic s = c.new
			 * Statistic(rs.getString("year"), rs.getInt("cum_loans"),
			 * rs.getInt("cum_reservations")); if (s.year != null) { c.stats.add(s);
			 * this.cum_loans+=s.num_loans; this.cum_reserv+=s.num_reserv; } } while
			 * (rs.next() && rs.getString("epn").compareTo(c.epn)==0); if
			 * (!rs.isAfterLast()) rs.previous();
			 */
			this.copies.add(c);
		} while (rs.next() && rs.getString("ppn").compareTo(this.ppn) == 0);
		if (!rs.isAfterLast())
			rs.previous();
	}

	/**
	 * enriches the Title object, or more specifically, the Copy object with loan
	 * statistics from the DB
	 * 
	 * @param db   the DatabaseConnection from which data is retrieved
	 * @param year the current year
	 * @throws SQLException
	 */
	public void addDBStats(DatabaseConnection db, int[] years) throws SQLException {
		if (ppn.isEmpty()) {
			System.out.println("DBSTATS ERROR: a title has been requested that has no PPN");
			return;
		}
		this.num_copies_cum = 0;
		this.cum_loans = 0;
		this.cum_reserv = 0;
		// Build query - PPN's last digit is not in DB (validation digit)
		String query = "SELECT ous_copy_cache.epn, ous_copy_cache.ppn,";
		for (int i : years) {
			query += "max(case when year=" + i + " then volume_statistics.cum_loans else null end)  as \"loans " + i
					+ "\",";
			query += "max(case when year=" + i
					+ " then volume_statistics.cum_reservations else null end)  as \"reservations " + i + "\",";
		}
		query = query.substring(0, query.length() - 1) + " ";
		/*
		 * query += "FROM ous_copy_cache, volume, volume_statistics "; query +=
		 * "WHERE ous_copy_cache.ppn=" + this.ppn.substring(0, this.ppn.length() - 1) +
		 * " AND ous_copy_cache.epn=volume.epn AND volume.volume_number=volume_statistics.volume_number AND ous_copy_cache.iln=100 "
		 * ;
		 */
		query += "FROM ous_copy_cache inner join volume on (ous_copy_cache.epn = volume.epn) ";
		query += " inner join volume_statistics on (volume.volume_number = volume_statistics.volume_number) ";
		query += "WHERE ous_copy_cache.iln=100 and ous_copy_cache.ppn=" + this.ppn.substring(0, this.ppn.length() - 1);
		query += "GROUP BY volume.volume_number, ous_copy_cache.epn ";
		query += "order by ous_copy_cache.epn ";

		ResultSet rs = db.sqlQuery(query);
		rs.beforeFirst();
		while (rs.next()) {
			String epn = rs.getString("epn");
			for (Copy c : this.copies) {
				if (c.epn == "")
					continue; // sometimes occurence is not in sequence
				// identify current copy - EPN's last digit is not in DB
				if ((c.epn.substring(0, c.epn.length() - 1).compareTo(epn) != 0)
						&& (c.epn.substring(0, c.epn.length() - 1).compareTo("0" + epn) != 0))
					continue;
				for (int i : years) {
					int loans = rs.getInt("loans " + i);
					int reserv = rs.getInt("reservations " + i);
					if (i == 2007)
						c.addStatistic(c.new Statistic("<=2007", loans, reserv));
					else
						c.addStatistic(c.new Statistic(i + "", loans, reserv));
					cum_loans += loans;
					cum_reserv += reserv;
				}
				break;
			}
		}

		// create empty stats if not from DB
		for (Copy c : this.copies) {
			if (c.stats.isEmpty()) {
				for (int i : years) {
					if (i == 2007)
						c.addStatistic(c.new Statistic("<=2007", 0, 0));
					else
						c.addStatistic(c.new Statistic(i + "", 0, 0));
				}
			} else
				num_copies_cum++;
		}
	}

	/**
	 * method to enrich the title object with order information
	 * 
	 * @param db
	 * @throws SQLException
	 */
	public void addOrderInfos(DatabaseConnection db) throws SQLException {
		if (ppn.isEmpty()) {
			System.out.println("DBORDER ERROR: a title has been requested that has no PPN");
			return;
		}
		String query = "SELECT occ.epn, o.order_id_nr, o.ordertype_code, o.orderstatus_code "
				+ "FROM dbo.ous_copy_cache occ JOIN dbo.orders o on (o.epn=occ.epn) " + "WHERE occ.iln=100 AND occ.ppn="
				+ ppn.substring(0, ppn.length() - 1);

		ResultSet rs = db.sqlQuery(query);
		rs.beforeFirst();
		boolean flag = false;

		for (Copy c : this.copies) {
			c.orderID = "";
			c.orderType = "";
			c.orderStatus = "";
		}
		while (rs.next()) {
			String epn = rs.getString("epn");
			for (Copy c : this.copies) {
				if (c.epn == "")
					continue; // sometimes occurence is not in sequence
				// identify current copy - EPN's last digit is not in DB
				if ((c.epn.substring(0, c.epn.length() - 1).compareTo(epn) != 0)
						&& (c.epn.substring(0, c.epn.length() - 1).compareTo("0" + epn) != 0))
					continue;
				c.orderID += rs.getString("order_id_nr") + " ";
				c.orderType += rs.getString("ordertype_code") + " ";
				c.orderStatus += rs.getString("orderstatus_code") + " ";
				flag = true;
				break;
			}
		}
		if (!flag && !super_ppn.isEmpty()) { // no order infos so try at super PPN
			query = "SELECT occ.epn, o.order_id_nr, o.ordertype_code, o.orderstatus_code "
					+ "FROM dbo.ous_copy_cache occ JOIN dbo.orders o on (o.epn=occ.epn) "
					+ "WHERE occ.iln=100 AND occ.ppn=" + super_ppn.substring(0, super_ppn.length() - 1);

			rs = db.sqlQuery(query);
			rs.beforeFirst();
			while (rs.next()) {
				// String epn = rs.getString("epn");
				for (Copy c : this.copies) {
					if (!c.orderID.isEmpty())
						continue;
					c.orderID += rs.getString("order_id_nr") + " ";
					c.orderType += rs.getString("ordertype_code") + " ";
					c.orderStatus += rs.getString("orderstatus_code") + " ";
				}
				break;
			}
		}
	}

	/**
	 * helper method to extract string from an PICA-XML datafield
	 * 
	 * @param tagElement the XML element of a PICA tag
	 * @param subfield   the character subfield to look for, e.g. "0"
	 * @return the text content of the subfield tag
	 */
	String getSubfield(Element tagElement, String subfield) {
		String res = "";
		for (int i = 0; i < tagElement.getElementsByTagName("subfield").getLength(); i++) {
			if (((Element) (tagElement.getElementsByTagName("subfield").item(i))).getAttribute("code")
					.compareTo(subfield) == 0) {
				res = tagElement.getElementsByTagName("subfield").item(i).getTextContent();
				break;
			}
		}

		return res.compareTo("null") == 0 ? "" : res;
	}

	/**
	 * helper method to extract ISSN from PICA-XML
	 * 
	 * @param tagElement the XML element for ISSN
	 * @return the ISSN as a string from subfield "C" or "6"
	 */
	String getSubfieldISSN(Element tagElement) {
		String res = "";
		boolean flag = false;
		for (int i = 0; i < tagElement.getElementsByTagName("subfield").getLength(); i++) {
			if (((Element) (tagElement.getElementsByTagName("subfield").item(i))).getAttribute("code")
					.compareTo("C") == 0) { // Code
				if (tagElement.getElementsByTagName("subfield").item(i).getTextContent().compareTo("ISSN") == 0)
					flag = true;
			} else if (flag && ((Element) (tagElement.getElementsByTagName("subfield").item(i))).getAttribute("code")
					.compareTo("6") == 0) {
				res = tagElement.getElementsByTagName("subfield").item(i).getTextContent();
				break;
			}
		}

		return res.compareTo("null") == 0 ? "" : res;
	}

	/**
	 * enriches the title object with data from its family head (using indicator
	 * "[c]" in title classification attribute)
	 * 
	 * @param eElement2
	 */
	public void addFamInfo(Element eElement2) {
		NodeList nListRecord = eElement2.getElementsByTagName("datafield");
		// nListRecord is the list of datafield elements that have the attribute "tag"
		// indicating pica+ code

		for (int temp3 = 0; temp3 < nListRecord.getLength(); temp3++) { // go over all datafield elements
			Node rNode = nListRecord.item(temp3);
			if (rNode.getNodeType() == Node.ELEMENT_NODE) {
				Element tagElement = (Element) rNode;
				if (tagElement.getAttribute("tag").compareTo("145Z") == 0) {
					this.classification += getSubfield(tagElement, "a") + "[c] | ";
				}
			}
		}
		if (this.classification.length() >= 3)
			this.classification = this.classification.substring(0, this.classification.length() - 3);
	}

	/**
	 * enriches the title with information from XML element using K10plus
	 * 
	 * @param eElement
	 */
	public void addGVKInfo(Element eElement) {
		NodeList nListRecord = eElement.getElementsByTagName("datafield");
		// nListRecord is the list of datafield elements that have the attribute "tag"
		// indicating pica+ code
		this.num_lib = 0;
		for (int temp3 = 0; temp3 < nListRecord.getLength(); temp3++) { // go over all datafield elements
			Node rNode = nListRecord.item(temp3);
			if (rNode.getNodeType() == Node.ELEMENT_NODE) {
				Element tagElement = (Element) rNode;
				// Editing library => not sufficient
				/*
				 * if (tagElement.getAttribute("tag").compareTo("202D") == 0) { int idx =
				 * Integer.valueOf(tagElement.getAttribute("occurrence")) - 1; if (idx > 1)
				 * continue; if (getSubfield(tagElement, "a").startsWith("0003")) { //ELN ULB
				 * Halle this.copyHalle = true; this.lastCopyGVK = false; //wenn in Halle, dann
				 * auch nicht letztes Ex //break; } else { if (!getSubfield(tagElement,
				 * "a").startsWith("3100")) { //ELN UB Magdeburg this.lastCopyGVK = false;
				 * //wenn min ein Ex nicht in MD, dann auch nicht letztes Ex //break; } }
				 * this.numLib++; }
				 */
				if (tagElement.getAttribute("tag").compareTo("101@") == 0) {
					if (getSubfield(tagElement, "a").compareTo("65") == 0) { // ILN ULB Halle
						this.copy_halle = true;
						this.last_copy_gvk = false; // wenn in Halle, dann auch nicht letztes Ex
						// break;
					} else {
						if (getSubfield(tagElement, "a").compareTo("100") != 0) { // ILN UB Magdeburg
							this.last_copy_gvk = false; // wenn min ein Ex nicht in MD, dann auch nicht letztes Ex
							// break;
						} else {
							this.copy_md = true;
						}
					}
					this.num_lib++;
				} else if (tagElement.getAttribute("tag").compareTo("001@") == 0) {
					this.iln_list = getSubfield(tagElement, "0");
				}
			}
		}
	}

	/**
	 * method to create title object from a XML node list of titles
	 * 
	 * @param nl the node list
	 * @return a list of title objects
	 */
	public List<Title> getSubTitles(NodeList nl, DatabaseConnection db_local) {
		List<Title> res = new ArrayList<Title>();

		if (nl != null)
			for (int temp3 = 0; temp3 < nl.getLength(); temp3++) {
				Node nNode3 = nl.item(temp3);
				if (nNode3.getNodeType() == Node.ELEMENT_NODE) {
					Element eElement3 = (Element) nNode3;
					Title t = new Title(eElement3);
					t.subject = this.getFR(db_local);
					if (t.ppn.compareTo(this.ppn) != 0)
						res.add(t);
				}
			}
		return res;
	}

	/**
	 * static method to retrieve title list of a ppn
	 * 
	 * @param nl  the node list
	 * @param ppn the ppn of the super title
	 * @return a list of title objects
	 */
	public static List<Title> getSubTitles(NodeList nl, String ppn, DatabaseConnection db_local) {
		List<Title> res = new ArrayList<Title>();

		if (nl != null)
			for (int temp3 = 0; temp3 < nl.getLength(); temp3++) {
				Node nNode3 = nl.item(temp3);
				if (nNode3.getNodeType() == Node.ELEMENT_NODE) {
					Element eElement3 = (Element) nNode3;
					Title t = new Title(eElement3);
					if (db_local != null)
						t.subject = t.getFR(db_local);
					if (t.ppn.compareTo(ppn) != 0)
						res.add(t);
				}
			}
		return res;
	}

	/**
	 * method to assign a subject to a title using a static matrix
	 * 
	 * @return
	 */
	public String getFR(DatabaseConnection db_local) {
		if (this.classification == null)
			return null;
		if (!this.subject.isEmpty())
			return this.subject;
		
		for (int i = 0; i < fr.length; i++) { 
			if (this.classification.startsWith(fr[i][0])) { 
				return fr[i][1]; 
			} 
		} 
		return "";
	}

	/**
	 * array of String tuples mapping classification to subject specialists (first
	 * hit counts), Stand 31.5.24
	 */
	static String[][] fr = new String[][] { new String[] { "B. 2", "Bo" }, new String[] { "B. 4", "Le" },
			new String[] { "B. 6", "Le" }, new String[] { "B. 7", "Le" }, new String[] { "B. 9", "Wa" },
			new String[] { "B", "Sch" }, new String[] { "C. 18", "Le" }, new String[] { "C. 3", "Le" },
			new String[] { "C. 7", "Le" }, new String[] { "C", "Wa" }, new String[] { "F.", "Wa" },
			new String[] { "D. 7", "Le" }, new String[] { "D", "Wa" }, new String[] { "E. 752", "Wa" },
			new String[] { "E", "Le" }, new String[] { "F/A", "Ah" }, new String[] { "F/C", "Ah" },
			new String[] { "F/D 202", "Ja" }, new String[] { "F/D", "Ah" }, new String[] { "F/E", "Ja" },
			new String[] { "F/F 0", "Ja" }, new String[] { "F/F 4", "Ja" }, new String[] { "F/F 8", "Ja" },
			new String[] { "F/F 9", "Ja" }, new String[] { "F/F", "Nie" }, new String[] { "F/G", "Le" },
			new String[] { "F/H", "Tor" }, new String[] { "F/K", "Tor" }, new String[] { "F/L", "Le" },
			new String[] { "F/O", "Wa" }, new String[] { "F/R", "Tor" }, new String[] { "F/Z", "Ja" },
			new String[] { "K. 5", "Sch" }, new String[] { "G", "Le" }, new String[] { "P", "Lue" },
			new String[] { "Q", "Lue" }, new String[] { "R. 667", "Le" }, new String[] { "R", "Lue" },
			new String[] { "Y", "Bo" }, };

	@Override
	public boolean equals(Object obj) {
		Title t = (Title) obj;
		return t.ppn.compareTo(this.ppn) == 0;
	}

	@Override
	public String toString() {
		return this.author + ": \"" + this.title + "\": " + copies.size() + " Copies\n";
	}

	public static class Copy {
		// copy information from PICA-CBS
		public String loan_date = ""; // 201@/XX $d (DD-MM-YYYY) [optional]
		public String status = ""; // 201@/XX $a
		public String edit_date = ""; // 202D $0 (DD-MM-YY) or 201B $0
		public String epn = ""; // 203@/XX $0
		public String selection_key = ""; // 208@/XX $b
		public String signature = ""; // 209A/XX $a
		public String location = ""; // 209A/XX $f
		public String loan_indicator = ""; // 209A/XX $d
		public String barcode = ""; // 209G/XX $a
		public String call_sign = ""; // 209O/XX $a
		public String inventory_string = ""; // 231B $a
		public String remark = ""; // 237A/XX $a
		public String remark_intern = ""; // 220B/XX $a
		public String local_sys = ""; // 245Z/XX $a

		public List<Statistic> stats;

		// order information from PICA-DB
		public String orderID = "";
		public String orderType = "";
		public String orderStatus = "";

		public Copy() {
			stats = new ArrayList<Statistic>();
		}

		public void addStatistic(Statistic s) {
			stats.add(s);
		}

		/**
		 * method for determining if a copy is available for loan
		 * 
		 * @return true if the copy is not available for loan
		 */
		public boolean isLocked() {
			return loan_indicator.startsWith("g") || loan_indicator.startsWith("a") || loan_indicator.startsWith("z")
					|| loan_indicator.startsWith("o");
		}

		public class Statistic {
			public String year;
			public int num_loans;
			public int num_reserv;
			// public int num_req;
			// public int num_renew;

			public Statistic(String year, int num_loans, int num_reserv) {
				this.year = year;
				this.num_loans = num_loans;
				this.num_reserv = num_reserv;
			}

			@Override
			public String toString() {
				return year + ": " + num_loans + ", " + num_reserv;
			}
		}
	}

}
