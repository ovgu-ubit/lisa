package write;

import java.util.Calendar;
import java.util.function.Predicate;

import model.Title;
import model.Title.Copy;
import model.Title.Copy.Statistic;

/**
 * TitleWriter that stores Title objects in a CSV file one line per copy
 * 
 * @author sbosse
 *
 */
public class CSVWriter extends AbstractCSVWriter {

	/**
	 * 
	 * @param destination directory of the file to be created
	 * @param datafields  array of DataField enums that are to be exported
	 * @param sep         the separator char for CSV file, standard ';'
	 */
	public CSVWriter(String destination, DataFields[] datafields, String sep, boolean superInfos) {
		super(destination, datafields, sep, superInfos);

	}

	/**
	 * 
	 * @param destination       directory of the file to be created
	 * @param datafields        array of DataField enums that are to be exported
	 * @param sep               the separator char for CSV file, standard ';'
	 * @param predicateFunction a function for testing titles, only 'true' titles
	 *                          are reported
	 */
	public CSVWriter(String destination, DataFields[] datafields, String sep, Predicate<Title> predicateFunction,
			Predicate<Copy> predicateFunctionCopy, boolean superInfos) {
		super(destination, datafields, sep, predicateFunction, predicateFunctionCopy, superInfos);
	}

	public CSVWriter(String destination, DataFields[] datafields, String sep) {
		super(destination, datafields, sep, false);
	}

	public CSVWriter(String destination, DataFields[] datafields, String sep, String fileExt) {
		super(destination, datafields, sep, fileExt);
	}

	public void addTitle(Title title) {
		if (predFunc != null && !predFunc.test(title))
			return; // only write to CSV if the predicate is true for this title
		// create a row for every copy
		for (Copy c : title.copies) {
			if (c.epn == "" || (this.predFuncCopy != null && !this.predFuncCopy.test(c)))
				continue; // if copy numbers are not in sequence
			String s = "";
			for (DataFields df : datafields) {
				s += quot;
				switch (df) {
				case TITLE:
					s += title.title.replaceAll(quot, quot_replacement);
					if (title.volume.length() > 0)
						s += ", " + title.volume;
					break;
				case AUTHOR_FIRST:
					s += title.author.replaceAll(quot, quot_replacement);
					break;
				case AUTHOR_OTHER:
					s += title.co_authors.replaceAll(quot, quot_replacement);
					break;
				case AUTHORS:
					s += title.author.replaceAll(quot, quot_replacement) + " "
							+ title.co_authors.replaceAll(quot, quot_replacement);
					break;
				case YEAR:
					s += title.year_of_creation.replaceAll(quot, quot_replacement);
					break;
				case SIGNATURE:
					s += c.signature.replaceAll(quot, quot_replacement);
					break;
				case PPN:
					s += title.ppn.replaceAll(quot, quot_replacement);
					break;
				case EPN:
					s += c.epn.replaceAll(quot, quot_replacement);
					break;
				case LOCATION:
					s += c.location.replaceAll(quot, quot_replacement);
					break;
				case LOAN_INDICATOR:
					s += c.loan_indicator.replaceAll(quot, quot_replacement);
					break;
				case CLASSIFICATION:
					s += title.classification.replaceAll(quot, quot_replacement);
					break;
				case MATERIAL_CODE:
					s += title.material_code.replaceAll(quot, quot_replacement);
					break;
				case MATERIAL:
					s += title.material.replaceAll(quot, quot_replacement);
					break;
				case LANGUAGE:
					s += title.language_text.replaceAll(quot, quot_replacement);
					break;
				case BARCODE:
					s += c.barcode.replaceAll(quot, quot_replacement);
					break;
				case STATUS:
					s += c.status.replaceAll(quot, quot_replacement);
					break;
				case LOAN_DATE:
					s += c.loan_date.replaceAll(quot, quot_replacement);
					break;
				case SELECTION_KEY:
					s += c.selection_key.replaceAll(quot, quot_replacement);
					break;
				case REMARK:
					if (!c.remark_intern.isEmpty() && !c.remark.isEmpty())
						s += c.remark.replaceAll(quot, quot_replacement) + " | "
								+ c.remark_intern.replaceAll(quot, quot_replacement);
					else if (c.remark.isEmpty())
						s += c.remark_intern.replaceAll(quot, quot_replacement);
					else if (c.remark_intern.isEmpty())
						s += c.remark.replaceAll(quot, quot_replacement);
					break;
				case LOCAL_EXPANSION:
					s += title.local_expansion.replaceAll(quot, quot_replacement);
					break;
				case GREATER_ENTITIY:
					s += title.greater_entity.replaceAll(quot, quot_replacement);
					break;
				case GREATER_ENTITY_ISSN:
					s += title.greater_entity_issn.replaceAll(quot, quot_replacement);
					break;
				case DOI:
					s += title.doi.replaceAll(quot, quot_replacement);
					break;
				case CUM_LOANS_TITLE:
					s += title.cum_loans;
					break;
				case CUM_RESERV_TITLE:
					s += title.cum_reserv;
					break;
				case PUBLISHER:
					s += title.publisher.replaceAll(quot, quot_replacement);
					break;
				case GREATER_ENTITY_YEAR:
					s += title.greaterEntityYear.replaceAll(quot, quot_replacement);
					break;
				case LOCAL_SYSTEMATIC:
					s += c.local_sys.replaceAll(quot, quot_replacement);
					break;
				case FR:
					s += title.subject.replaceAll(quot, quot_replacement);
					break;
				case ON_LOAN:
					s += (c.status.contains("ausgeliehen"));
					break;
				case EDIT_DATE:
					s += c.edit_date.replaceAll(quot, quot_replacement);
					break;
				case ORDER_ID:
					s += c.orderID.replaceAll(quot, quot_replacement);
					break;
				case ORDER_TYPE:
					s += c.orderType.replaceAll(quot, quot_replacement);
					break;
				case ORDER_STATUS:
					s += c.orderStatus.replaceAll(quot, quot_replacement);
					break;
				case LINK:
					s += title.link.replaceAll(quot, quot_replacement);
					break;
				case LOANS_LAST_5_YEARS:
					int num = 0;
					for (Statistic st : c.stats) {
						int y;
						try {
							y = Integer.parseInt(st.year);
						} catch (NumberFormatException e) {
							y = 2007;
						}
						if (y >= (Calendar.getInstance().get(Calendar.YEAR) - 5)) {
							num += st.num_loans;
						}
					}
					s += num;
					break;
				case LOANS_LAST_10_YEARS:
					num = 0;
					for (Statistic st : c.stats) {
						int y;
						try {
							y = Integer.parseInt(st.year);
						} catch (NumberFormatException e) {
							y = 2007;
						}
						if (y >= (Calendar.getInstance().get(Calendar.YEAR) - 10)) {
							num += st.num_loans;
						}
					}
					s += num;
					break;
				case RESERVE_LAST_5_YEARS:
					num = 0;
					for (Statistic st : c.stats) {
						int y;
						try {
							y = Integer.parseInt(st.year);
						} catch (NumberFormatException e) {
							y = 2007;
						}
						if (y >= (Calendar.getInstance().get(Calendar.YEAR) - 5)) {
							num += st.num_reserv;
						}
					}
					s += num;
					break;
				case LAST_LOAN:
					int max = 0;
					for (Statistic st : c.stats) {
						int y;
						try {
							y = Integer.parseInt(st.year);
						} catch (NumberFormatException e) {
							y = 2007;
						}
						if (st.num_loans > 0 && y >= max)
							max = y;
					}
					s += max == 0 ? "" : max;
					break;
				case LOANS_TIL_2007:
					num = 0;
					for (Statistic st : c.stats) {
						if (st.year.contains("2007")) {
							num = st.num_loans;
							break;
						}
					}
					s += num;
					break;
				case EDITION:
					s += title.edition;
					break;
				case TYPE:
					s += title.type;
					break;
				case PUBLISHER_LOCATION:
					s += title.publisher_location;
					break;
				case NUM_LIBRARIES:
					s += title.num_lib;
					break;
				case BKL:
					s += title.bkl;
					break;
				case RVK:
					s += title.rvk;
					break;
				case DDC:
					s += title.ddc;
					break;
				case NUM_COPIES_AUI_U:
					s += c.loan_indicator.compareTo("u") == 0;
					break;
				case ISBN:
					s += title.isbn;
					break;
				case SUPER_PPN:
					s += title.super_ppn;
					break;
				case INTERNAL_CODES:
					s += title.internal_codes;
					break;
				case INVENTORY_STRING:
					s += c.inventory_string;
					break;
				case SCRIBAL:
					s += title.scribal;
					break;
				case SCRIBAL_LOCAL:
					s += title.scribal_local;
					break;
				case CALL_SIGN:
					s += c.call_sign;
					break;
				default:
					s += "";
				}
				s += quot + sep;
			}
			if (this.datafields.length != 0)
				s = s.substring(0, s.length() - 1);
			pw.write(s + "\n");
		}
		pw.flush();
	}

}
