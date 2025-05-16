package write;

import model.Title;

/**
 * interface for processing title objects
 * 
 * @author sbosse
 *
 */
public interface TitleWriter {

	/**
	 * main method in which processing is done
	 * 
	 * @param title the current title that has been constructed
	 */
	public void addTitle(Title title);

	/**
	 * method called when a query has been sent and retrieved
	 */
	public void close();

	/**
	 * method called before a query is sent
	 * 
	 * @param query the CQL query that is to be sent
	 * @return the filename that has been created
	 */
	public String init(String query);

	public boolean superInfos();

	/**
	 * enum containing possible data fields of title object to allow for efficient
	 * handling of the required data fields
	 * 
	 * @author sbosse
	 *
	 */
	public enum DataFields {
		TITLE {
			public String toString() {
				return "Titel";
			}
		},
		AUTHOR_FIRST {
			public String toString() {
				return "Verfasser";
			}
		},
		AUTHOR_OTHER {
			public String toString() {
				return "Weitere Verfasser";
			}
		},
		EDITION {
			public String toString() {
				return "Ausgabe";
			}
		},
		YEAR {
			public String toString() {
				return "Erscheinungsjahr";
			}
		},
		SIGNATURE {
			public String toString() {
				return "Signatur";
			}
		},
		PPN, EPN, LOCATION {
			public String toString() {
				return "Standort";
			}
		},
		LOAN_INDICATOR {
			public String toString() {
				return "Ausleihindikator";
			}
		},
		CLASSIFICATION {
			public String toString() {
				return "Systematik";
			}
		},
		MATERIAL_CODE, MATERIAL {
			public String toString() {
				return "Bibliografische Gattung";
			}
		},
		LANGUAGE {
			public String toString() {
				return "Sprache";
			}
		},
		BARCODE, REMARK {
			public String toString() {
				return "Bemerkung";
			}
		},
		LOAN_DATE {
			public String toString() {
				return "Ausleihdatum";
			}
		},
		SELECTION_KEY {
			public String toString() {
				return "Selektionsschlüssel";
			}
		},
		STATUS, LOCAL_EXPANSION {
			public String toString() {
				return "Lokale Erweiterung";
			}
		},
		GREATER_ENTITIY {
			public String toString() {
				return "Größere Einheit";
			}
		},
		GREATER_ENTITY_ISSN {
			public String toString() {
				return "ISSN der größeren Einheit";
			}
		},
		DOI, CUM_LOANS_TITLE {
			public String toString() {
				return "Kumulierte Entleihungen";
			}
		},
		CUM_RESERV_TITLE {
			public String toString() {
				return "Kumulierte Vormerkungen";
			}
		},
		NUM_COPIES_STATS {
			public String toString() {
				return "Anzahl der Exemplare mit Statistiken";
			}
		},
		NUM_COPIES_AUI_U {
			public String toString() {
				return "Anzahl der ausleihbaren Exemplare";
			}
		},
		CUM_LOAN_RATIO {
			public String toString() {
				return "Entleihungen pro Exemplar";
			}
		},
		CUM_RESERV_RATIO {
			public String toString() {
				return "Vormerkungen pro Exemplar";
			}
		},
		PUBLISHER {
			public String toString() {
				return "Verlag";
			}
		},
		GREATER_ENTITY_YEAR {
			public String toString() {
				return "Jahr der größeren Einheit";
			}
		},
		LOCAL_SYSTEMATIC {
			public String toString() {
				return "Lokale Systematik";
			}
		},
		FR {
			public String toString() {
				return "Fachreferat";
			}
		},
		ON_LOAN {
			public String toString() {
				return "Anzahl der ausgeliehenen Exemplare";
			}
		},
		EDIT_DATE {
			public String toString() {
				return "Änderungsdatum";
			}
		},
		ORDER_ID {
			public String toString() {
				return "Bestellnr.";
			}
		},
		ORDER_TYPE {
			public String toString() {
				return "Bestelltyp";
			}
		},
		ORDER_STATUS {
			public String toString() {
				return "Bestellstatus";
			}
		},
		AUTHORS {
			public String toString() {
				return "Autoren";
			}
		},
		LINK {
			public String toString() {
				return "Link";
			}
		},
		LOANS_LAST_5_YEARS {
			public String toString() {
				return "Anzahl Entleihungen der letzten 5 Jahre";
			}
		},
		LOANS_TIL_2007 {
			public String toString() {
				return "Anzahl Entleihungen bis 2007";
			}
		},
		LOANS_LAST_10_YEARS {
			public String toString() {
				return "Anzahl Entleihungen der letzten 10 Jahre";
			}
		},
		RESERVE_LAST_5_YEARS {
			public String toString() {
				return "Anzahl Vormerkungen der letzten 5 Jahre";
			}
		},
		LAST_LOAN {
			public String toString() {
				return "Zuletzt entliehen";
			}
		},
		NUM_COPIES_NOT_G {
			public String toString() {
				return "Anzahl der nicht gesperrten Exemplare";
			}
		},
		NUM_LOCATION_FH {
			public String toString() {
				return "Anzahl FH";
			}
		},
		NUM_LOCATION_FHP {
			public String toString() {
				return "Anzahl FH-Präsenz";
			}
		},
		NUM_LOCATION_MG {
			public String toString() {
				return "Anzahl Magazin";
			}
		},
		LAST_COPY_GVK {
			public String toString() {
				return "Letztes Exemplar im GVK";
			}
		},
		COPY_HALLE {
			public String toString() {
				return "Exemplar in ULB Halle";
			}
		},
		TYPE {
			public String toString() {
				return "Art";
			}
		},
		PUBLISHER_LOCATION {
			public String toString() {
				return "Ort";
			}
		},
		NUM_LIBRARIES {
			public String toString() {
				return "Anzahl besitzender Bibliotheken";
			}
		},
		BKL {
			public String toString() {
				return "BKL";
			}
		},
		COPY_MD {
			public String toString() {
				return "Exemplar in UB Magdeburg";
			}
		},
		RVK {
			public String toString() {
				return "RVK";
			}
		},
		DDC {
			public String toString() {
				return "DDC";
			}
		},
		ILN_LIST {
			public String toString() {
				return "Liste der besitztenden ILNs";
			}
		},
		ISBN {
			public String toString() {
				return "ISBN";
			}
		},
		SUPER_PPN {
			public String toString() {
				return "PPN der c-Stufe";
			}
		},
		SUPER_TITLE {
			public String toString() {
				return "Reihentitel";
			}
		},
		SIG_MAG {
			public String toString() {
				return "Signaturen (Magazin)";
			}
		},
		SIG_NO_MAG {
			public String toString() {
				return "Signaturen (ohne Magazin)";
			}
		},
		INTERNAL_CODES {
			public String toString() {
				return "Interne Zeichen";
			}
		},
		INVENTORY_STRING {
			public String toString() {
				return "Bestandsinformationen";
			}
		},
		SCRIBAL {
			public String toString() {
				return "Sigel";
			}
		},
		SCRIBAL_LOCAL {
			public String toString() {
				return "Sigel lokal";
			}
		},
		CALL_SIGN {
			public String toString() {
				return "Abrufzeichen";
			}
		};
	}

	public enum AggMode {
		FIRST, MAX, MIN, APPEND
	}
}
