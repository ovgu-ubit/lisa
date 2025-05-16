package write;

import java.util.Calendar;
import java.util.Locale;
import java.util.function.Predicate;

import model.Title;
import model.Title.Copy;
import model.Title.Copy.Statistic;

/**
 * TitleWriter that stores Title objects in a CSV file one line per title,
 * statistics are cumulated copy details are shown for the first copy
 * 
 * @author sbosse
 *
 */
public class CSVWriterTitle extends AbstractCSVWriter {

	AggMode aggMode;

	/**
	 * 
	 * @param destination directory of the file to be created
	 * @param datafields  array of DataField enums that are to be exported
	 * @param sep         the separator char for CSV file, standard ';'
	 */
	public CSVWriterTitle(String destination, DataFields[] datafields, String sep, boolean superInfos,
			AggMode aggMode) {
		super(destination, datafields, sep, superInfos);
		this.aggMode = aggMode;
	}

	/**
	 * 
	 * @param destination       directory of the file to be created
	 * @param datafields        array of DataField enums that are to be exported
	 * @param sep               the separator char for CSV file, standard ';'
	 * @param predicateFunction a function for testing titles, only 'true' titles
	 *                          are reported
	 */
	public CSVWriterTitle(String destination, DataFields[] datafields, String sep, Predicate<Title> predicateFunction,
			Predicate<Copy> predicateFunctionCopy, boolean superInfos, AggMode aggMode) {
		super(destination, datafields, sep, predicateFunction, predicateFunctionCopy, superInfos);
		this.aggMode = aggMode;
	}

	public CSVWriterTitle(String destination, DataFields[] datafields, String sep, AggMode aggMode) {
		super(destination, datafields, sep, false);
		this.aggMode = aggMode;
	}

	public CSVWriterTitle(String destination, DataFields[] datafields, String sep, String fileExt, AggMode aggMode) {
		super(destination, datafields, sep, fileExt);
		this.aggMode = aggMode;
	}

	public void addTitle(Title title) {
		if (predFunc != null && !predFunc.test(title))
			return; // only write to CSV if the predicate is true for this title
		String s = "";
		int num = 0;
		boolean quotflag = true;
		for (DataFields df : datafields) {
			s += quot;
			switch (df) {
			case TITLE:
				s += title.title.replaceAll(quot, quot_replacement).replaceAll("\t", "\\t");
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
				if (title.author.isEmpty())
					s += title.co_authors.replaceAll(quot, quot_replacement);
				else
					s += title.author.replaceAll(quot, quot_replacement) + " | "
							+ title.co_authors.replaceAll(quot, quot_replacement);
				break;
			case YEAR:
				s += title.year_of_creation.replaceAll(quot, quot_replacement);
				break;
			case SIGNATURE:
				if (aggMode == AggMode.FIRST && !title.copies.isEmpty())
					s += title.copies.get(0).signature.replaceAll(quot, quot_replacement);
				else {
					String res = "";
					for (Copy c : title.copies)
						if (!c.isLocked() && (this.predFuncCopy == null || this.predFuncCopy.test(c)))
							res += c.signature.replaceAll(quot, quot_replacement) + " | ";
					if (!res.isEmpty())
						res = res.substring(0, res.length() - 3);
					s += res;
				}
				break;
			case PPN:
				s += title.ppn.replaceAll(quot, quot_replacement);
				break;
			case EPN:
				if (aggMode == AggMode.FIRST && !title.copies.isEmpty())
					s += title.copies.get(0).epn.replaceAll(quot, quot_replacement);
				else {
					String res = "";
					for (Copy c : title.copies)
						if (this.predFuncCopy == null || this.predFuncCopy.test(c))
							res += c.epn.replaceAll(quot, quot_replacement) + " | ";
					if (!res.isEmpty())
						res = res.substring(0, res.length() - 3);
					s += res;
				}
				break;
			case LOCATION:
				if (aggMode == AggMode.FIRST && !title.copies.isEmpty())
					s += title.copies.get(0).location.replaceAll(quot, quot_replacement);
				else {
					String res = "";
					for (Copy c : title.copies)
						if (!c.isLocked() && (this.predFuncCopy == null || this.predFuncCopy.test(c)))
							res += c.location.replaceAll(quot, quot_replacement) + " | ";
					if (!res.isEmpty())
						res = res.substring(0, res.length() - 3);
					s += res;
				}
				break;
			case LOAN_INDICATOR:
				if (aggMode == AggMode.FIRST && !title.copies.isEmpty())
					s += title.copies.get(0).loan_indicator.replaceAll(quot, quot_replacement);
				else {
					String res = "";
					for (Copy c : title.copies)
						if (this.predFuncCopy == null || this.predFuncCopy.test(c))
							res += c.loan_indicator.replaceAll(quot, quot_replacement) + " | ";
					if (!res.isEmpty())
						res = res.substring(0, res.length() - 3);
					s += res;
				}
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
				if (aggMode == AggMode.FIRST && !title.copies.isEmpty())
					s += title.copies.get(0).barcode.replaceAll(quot, quot_replacement);
				else {
					String res = "";
					for (Copy c : title.copies)
						if (this.predFuncCopy == null || this.predFuncCopy.test(c))
							res += c.barcode.replaceAll(quot, quot_replacement) + " | ";
					if (!res.isEmpty())
						res = res.substring(0, res.length() - 3);
					s += res;
				}
				break;
			case STATUS:
				if (aggMode == AggMode.FIRST && !title.copies.isEmpty())
					s += title.copies.get(0).status.replaceAll(quot, quot_replacement);
				else {
					String res = "";
					for (Copy c : title.copies)
						if (this.predFuncCopy == null || this.predFuncCopy.test(c))
							res += c.status.replaceAll(quot, quot_replacement) + " | ";
					if (!res.isEmpty())
						res = res.substring(0, res.length() - 3);
					s += res;
				}
				break;
			case LOAN_DATE:
				if (aggMode == AggMode.FIRST && !title.copies.isEmpty())
					for (Copy c : title.copies)
						if (c.status.contains("ausgeliehen"))
							s += c.loan_date.replaceAll(quot, quot_replacement);
						else {
							String res = "";
							for (Copy co : title.copies)
								if (/* c.status.contains("ausgeliehen") && */ (this.predFuncCopy == null
										|| this.predFuncCopy.test(c)))
									res += co.loan_date.replaceAll(quot, quot_replacement) + " | ";
							if (!res.isEmpty())
								res = res.substring(0, res.length() - 3);
							s += res;
						}
				break;
			case SELECTION_KEY:
				if (aggMode == AggMode.FIRST && !title.copies.isEmpty())
					s += title.copies.get(0).selection_key.replaceAll(quot, quot_replacement);
				else {
					String res = "";
					for (Copy c : title.copies)
						if (this.predFuncCopy == null || this.predFuncCopy.test(c))
							res += c.selection_key.replaceAll(quot, quot_replacement) + " | ";
					if (!res.isEmpty())
						res = res.substring(0, res.length() - 3);
					s += res;
				}
				break;
			case REMARK:
				if (aggMode == AggMode.FIRST && !title.copies.isEmpty()) {
					Copy c = title.copies.get(0);
					if (!c.remark_intern.isEmpty() && !c.remark.isEmpty())
						s += c.remark.replaceAll(quot, quot_replacement) + " | "
								+ c.remark_intern.replaceAll(quot, quot_replacement);
					else if (c.remark.isEmpty())
						s += c.remark_intern.replaceAll(quot, quot_replacement);
					else if (c.remark_intern.isEmpty())
						s += c.remark.replaceAll(quot, quot_replacement);
				} else {
					String res = "";
					for (Copy c : title.copies) {
						if (this.predFuncCopy != null && !this.predFuncCopy.test(c))
							continue;
						if (!c.remark_intern.isEmpty() && !c.remark.isEmpty())
							s += c.remark.replaceAll(quot, quot_replacement) + " | "
									+ c.remark_intern.replaceAll(quot, quot_replacement);
						else if (c.remark.isEmpty())
							s += c.remark_intern.replaceAll(quot, quot_replacement);
						else if (c.remark_intern.isEmpty())
							s += c.remark.replaceAll(quot, quot_replacement);
					}
					if (!res.isEmpty())
						res = res.substring(0, res.length() - 3);
					s += res;
				}
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
			case NUM_COPIES_AUI_U:
				title.num_copies_loan = 0;
				for (Copy c : title.copies)
					if (c.loan_indicator.compareTo("u") == 0)
						title.num_copies_loan++;
				s += title.num_copies_loan;
				break;
			case NUM_COPIES_STATS:
				s += title.num_copies_cum;
				break;
			case CUM_LOAN_RATIO:
				/*
				 * if (num==0) { for (Copy c : title.copies) { if
				 * (c.loan_indicator.compareTo("u")==0) num++; } } double r = title.cum_loans /
				 * (num+0.0);
				 */
				s += String.format(Locale.US, "%.2f", title.cum_loans / (title.num_copies_loan + 0.0));
				break;
			case CUM_RESERV_RATIO:
				s += String.format(Locale.US, "%.2f", title.cum_reserv / (title.num_copies_loan + 0.0));
				break;
			case PUBLISHER:
				s += title.publisher.replaceAll(quot, quot_replacement);
				break;
			case GREATER_ENTITY_YEAR:
				s += title.greaterEntityYear;
				break;
			case LOCAL_SYSTEMATIC:
				if (aggMode == AggMode.FIRST && !title.copies.isEmpty())
					s += title.copies.get(0).local_sys.replaceAll(quot, quot_replacement);
				else {
					String res = "";
					for (Copy c : title.copies)
						if (this.predFuncCopy == null || this.predFuncCopy.test(c))
							res += c.local_sys.replaceAll(quot, quot_replacement) + " | ";
					if (!res.isEmpty())
						res = res.substring(0, res.length() - 3);
					s += res;
				}
				break;
			case FR:
				s += title.subject;
				break;
			case ON_LOAN:
				num = 0;
				for (Copy c : title.copies)
					if (c.status.contains("ausgeliehen"))
						num++;
				s += num;
				break;
			case EDIT_DATE:
				if (aggMode == AggMode.FIRST && !title.copies.isEmpty())
					s += title.copies.get(0).edit_date.replaceAll(quot, quot_replacement);
				else {
					String res = "";
					for (Copy c : title.copies)
						if (this.predFuncCopy == null || this.predFuncCopy.test(c))
							res += c.edit_date.replaceAll(quot, quot_replacement) + " | ";
					if (!res.isEmpty())
						res = res.substring(0, res.length() - 3);
					s += res;
				}
				break;
			case ORDER_ID:
				if (aggMode == AggMode.FIRST && !title.copies.isEmpty() && title.copies.get(0).orderID.length() > 0)
					s += title.copies.get(0).orderID.replaceAll(quot, quot_replacement);
				else {
					String res = "";
					for (Copy c : title.copies)
						if (c.orderID.length() > 0 && (this.predFuncCopy == null || this.predFuncCopy.test(c)))
							res += c.orderID.replaceAll(quot, quot_replacement) + " | ";
					if (!res.isEmpty())
						res = res.substring(0, res.length() - 3);
					s += res;
				}
				break;
			case ORDER_TYPE:
				if (aggMode == AggMode.FIRST && !title.copies.isEmpty() && title.copies.get(0).orderID.length() > 0)
					s += title.copies.get(0).orderType.replaceAll(quot, quot_replacement);
				else {
					String res = "";
					for (Copy c : title.copies)
						if (c.orderID.length() > 0 && (this.predFuncCopy == null || this.predFuncCopy.test(c)))
							res += c.orderType.replaceAll(quot, quot_replacement) + " | ";
					if (!res.isEmpty())
						res = res.substring(0, res.length() - 3);
					s += res;
				}
				break;
			case ORDER_STATUS:
				if (aggMode == AggMode.FIRST && !title.copies.isEmpty() && title.copies.get(0).orderID.length() > 0)
					s += title.copies.get(0).orderStatus.replaceAll(quot, quot_replacement);
				else {
					String res = "";
					for (Copy c : title.copies)
						if (c.orderID.length() > 0 && (this.predFuncCopy == null || this.predFuncCopy.test(c)))
							res += c.orderStatus.replaceAll(quot, quot_replacement) + " | ";
					if (!res.isEmpty())
						res = res.substring(0, res.length() - 3);
					s += res;
				}
				break;
			case LINK:
				s += title.link.replaceAll(quot, quot_replacement);
				break;
			case LOANS_LAST_5_YEARS:
				num = 0;
				for (Copy c : title.copies) {
					if (this.predFuncCopy == null || this.predFuncCopy.test(c))
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
				}
				s += num;
				break;
			case LOANS_LAST_10_YEARS:
				num = 0;
				for (Copy c : title.copies) {
					if (this.predFuncCopy == null || this.predFuncCopy.test(c))
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
				}
				s += num;
				break;
			case RESERVE_LAST_5_YEARS:
				num = 0;
				for (Copy c : title.copies) {
					if (this.predFuncCopy == null || this.predFuncCopy.test(c))
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
				}
				s += num;
				break;
			case LAST_LOAN:
				int max = 0;
				for (Copy c : title.copies) {
					if (this.predFuncCopy == null || this.predFuncCopy.test(c))
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
				}
				s += max == 0 ? "" : max;
				break;
			case LOANS_TIL_2007:
				num = 0;
				for (Copy c : title.copies) {
					if (this.predFuncCopy == null || this.predFuncCopy.test(c))
						for (Statistic st : c.stats) {
							if (st.year.contains("2007")) {
								num += st.num_loans;
								break;
							}
						}
				}
				s += num;
				break;
			case NUM_COPIES_NOT_G:
				num = 0;
				for (Copy c : title.copies)
					if (!c.isLocked() && (this.predFuncCopy == null || this.predFuncCopy.test(c)))
						num++;
				s += num;
				break;
			case NUM_LOCATION_FH:
				int numFH = 0;
				for (Copy c : title.copies) {
					if (!c.isLocked() && (this.predFuncCopy == null || this.predFuncCopy.test(c))) {
						if (c.location.startsWith("FH") || c.location.startsWith("FGSE-FH"))
							numFH++;
					}
				}
				s += numFH;
				break;
			case NUM_LOCATION_FHP:
				int numFHP = 0;
				for (Copy c : title.copies) {
					if (!c.isLocked() && (this.predFuncCopy == null || this.predFuncCopy.test(c))) {
						if (c.location.startsWith("FH-Präsenz") || c.location.startsWith("FGSE-Präsenz"))
							numFHP++;
					}
				}
				s += numFHP;
				break;
			case NUM_LOCATION_MG:
				int numMg = 0;
				for (Copy c : title.copies) {
					if (!c.isLocked() && (this.predFuncCopy == null || this.predFuncCopy.test(c))) {
						if (c.location.startsWith("Magazin") || c.location.startsWith("FGSE-Magazin"))
							numMg++;
					}
				}
				s += numMg;
				break;
			case LAST_COPY_GVK:
				s += title.last_copy_gvk;
				break;
			case COPY_HALLE:
				s += title.copy_halle;
				break;
			case EDITION:
				s += title.edition.replaceAll(quot, quot_replacement);
				break;
			case TYPE:
				s += title.type.replaceAll(quot, quot_replacement);
				break;
			case PUBLISHER_LOCATION:
				s += title.publisher_location.replaceAll(quot, quot_replacement);
				break;
			case NUM_LIBRARIES:
				s += title.num_lib;
				break;
			case ILN_LIST:
				s += title.iln_list;
				break;
			case BKL:
				s += title.bkl;
				break;
			case COPY_MD:
				s += title.copy_md;
				break;
			case RVK:
				s += title.rvk.replaceAll(quot, quot_replacement);
				break;
			case DDC:
				s += title.ddc.replaceAll(quot, quot_replacement);
				break;
			case ISBN:
				s += title.isbn;
				break;
			case SUPER_PPN:
				s += title.super_ppn;
				break;
			case SUPER_TITLE:
				s += title.super_title + " " + title.volume;
				break;
			case INTERNAL_CODES:
				s += title.internal_codes;
				break;
			case INVENTORY_STRING:
				if (aggMode == AggMode.FIRST && !title.copies.isEmpty()
						&& title.copies.get(0).inventory_string.length() > 0)
					s += title.copies.get(0).inventory_string.replaceAll(quot, quot_replacement);
				else {
					String res = "";
					for (Copy c : title.copies)
						if (c.inventory_string.length() > 0 && (this.predFuncCopy == null || this.predFuncCopy.test(c)))
							res += c.inventory_string.replaceAll(quot, quot_replacement) + " | ";
					if (!res.isEmpty())
						res = res.substring(0, res.length() - 3);
					s += res;
				}
				break;
			case SCRIBAL:
				s += title.scribal;
				break;
			case SCRIBAL_LOCAL:
				s += title.scribal_local;
				break;
			case CALL_SIGN:
				if (aggMode == AggMode.FIRST && !title.copies.isEmpty() && title.copies.get(0).call_sign.length() > 0)
					s = title.copies.get(0).call_sign;
				else {
					String res = "";
					for (Copy c : title.copies)
						if (!c.isLocked() && c.call_sign != null && c.call_sign.length() > 0
								&& (this.predFuncCopy == null || this.predFuncCopy.test(c)))
							res += c.call_sign + " | ";
					if (!res.isEmpty())
						res = res.substring(0, res.length() - 3);
					s += res;
				}
				break;
			default:
				s += "";
			}
			if (quotflag)
				s += quot + sep;
			else {
				s += sep;
				quotflag = true;
			}
		}
		if (this.datafields.length != 0)
			s = s.substring(0, s.length() - 1);
		pw.write(s + "\n");

		pw.flush();

	}

}
