package write;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.function.Predicate;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

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
public class ExcelWriterTitle extends AbstractExcelWriter {

	AggMode aggMode;
	boolean analysis = false;

	/**
	 * 
	 * @param destination directory of the file to be created
	 * @param datafields  array of DataField enums that are to be exported
	 * @param sep         the separator char for CSV file, standard ';'
	 */
	public ExcelWriterTitle(String destination, DataFields[] datafields, boolean superInfos, AggMode aggMode) {
		super(destination, datafields, superInfos);
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
	public ExcelWriterTitle(String destination, DataFields[] datafields, Predicate<Title> predicateFunction,
			Predicate<Copy> predicateFunctionCopy, boolean superInfos, boolean analysis, AggMode aggMode) {
		super(destination, datafields, predicateFunction, predicateFunctionCopy, superInfos, analysis);
		this.aggMode = aggMode;
	}

	public ExcelWriterTitle(String destination, DataFields[] datafields, AggMode aggMode) {
		super(destination, datafields, false);
		this.aggMode = aggMode;
	}

	public ExcelWriterTitle(OutputStream os, DataFields[] datafields, AggMode aggMode) {
		super(os, datafields);
		this.aggMode = aggMode;
	}

	public ExcelWriterTitle(OutputStream os, DataFields[] datafields, AggMode aggMode, boolean analysis,
			Predicate<Copy> predicateFunctionCopy) {
		super(os, datafields, analysis);
		this.aggMode = aggMode;
		this.analysis = analysis;
		this.predFuncCopy = predicateFunctionCopy;
		if (this.analysis)
			this.collected = new ArrayList<Title>();
	}

	public ExcelWriterTitle(OutputStream os, DataFields[] datafields, AggMode aggMode,
			Predicate<Title> predicateFunction) {
		super(os, datafields);
		this.aggMode = aggMode;
		this.predFunc = predicateFunction;
	}

	public void addTitle(Title title) {
		if (predFunc != null && !predFunc.test(title))
			return; // only write to CSV if the predicate is true for this title
		if (this.analysis)
			this.collected.add(title);
		Row row = this.sheet.createRow(this.idx++);
		int num = 0;
		int columnIdx = 0;
		for (DataFields df : datafields) {
			Cell cell = row.createCell(columnIdx++);
			String s = "";
			switch (df) {
			case TITLE:
				s = title.title;
				if (title.volume.length() > 0)
					s += ", " + title.volume;
				cell.setCellValue(s);
				break;
			case AUTHOR_FIRST:
				cell.setCellValue(title.author);
				break;
			case AUTHOR_OTHER:
				cell.setCellValue(title.co_authors);
				break;
			case AUTHORS:
				if (title.author.isEmpty())
					s = title.co_authors;
				else
					s = title.author + " | " + title.co_authors;
				cell.setCellValue(s);
				break;
			case YEAR:
				try {
					cell.setCellValue(Integer.parseInt(title.year_of_creation));
				} catch (NumberFormatException e) {
					cell.setCellValue(title.year_of_creation);
				}
				break;
			case SIGNATURE:
				if (aggMode == AggMode.FIRST && !title.copies.isEmpty())
					s = title.copies.get(0).signature;
				else {
					String res = "";
					for (Copy c : title.copies)
						if (!c.isLocked() && (this.predFuncCopy == null || this.predFuncCopy.test(c)))
							res += c.signature + " | ";
					if (!res.isEmpty())
						res = res.substring(0, res.length() - 3);
					s = res;
				}
				if (s.isEmpty())
					s = null;
				cell.setCellValue(s);
				break;
			case SIG_MAG:
				if (aggMode == AggMode.FIRST && !title.copies.isEmpty())
					s = title.copies.get(0).location.startsWith("Mag") ? title.copies.get(0).signature : "";
				else {
					String res = "";
					for (Copy c : title.copies)
						if (!c.isLocked() && (this.predFuncCopy == null || this.predFuncCopy.test(c))
								&& c.location.startsWith("Mag"))
							res += c.signature + " | ";
					if (!res.isEmpty())
						res = res.substring(0, res.length() - 3);
					s = res;
				}
				if (s.isEmpty())
					s = null;
				cell.setCellValue(s);
				break;
			case SIG_NO_MAG:
				if (aggMode == AggMode.FIRST && !title.copies.isEmpty())
					s = !title.copies.get(0).location.startsWith("Mag") ? title.copies.get(0).signature : "";
				else {
					String res = "";
					for (Copy c : title.copies)
						if (!c.isLocked() && (this.predFuncCopy == null || this.predFuncCopy.test(c))
								&& !c.location.startsWith("Mag"))
							res += c.signature + " | ";
					if (!res.isEmpty())
						res = res.substring(0, res.length() - 3);
					s = res;
				}
				if (s.isEmpty())
					s = null;
				cell.setCellValue(s);
				break;
			case PPN:
				cell.setCellValue(title.ppn);
				break;
			case EPN:
				if (aggMode == AggMode.FIRST && !title.copies.isEmpty())
					s = title.copies.get(0).epn;
				else {
					String res = "";
					for (Copy c : title.copies)
						if (!c.isLocked() && (this.predFuncCopy == null || this.predFuncCopy.test(c)))
							res += c.epn + " | ";
					if (!res.isEmpty())
						res = res.substring(0, res.length() - 3);
					s = res;
				}
				if (s.isEmpty())
					s = null;
				cell.setCellValue(s);
				break;
			case LOCATION:
				if (aggMode == AggMode.FIRST && !title.copies.isEmpty())
					s = title.copies.get(0).location;
				else {
					String res = "";
					for (Copy c : title.copies)
						if (!c.isLocked() && (this.predFuncCopy == null || this.predFuncCopy.test(c)))
							res += c.location + " | ";
					if (!res.isEmpty())
						res = res.substring(0, res.length() - 3);
					s = res;
				}
				if (s.isEmpty())
					s = null;
				cell.setCellValue(s);
				break;
			case LOAN_INDICATOR:
				if (aggMode == AggMode.FIRST && !title.copies.isEmpty())
					s = title.copies.get(0).loan_indicator;
				else {
					String res = "";
					for (Copy c : title.copies)
						if (!c.isLocked() && (this.predFuncCopy == null || this.predFuncCopy.test(c)))
							res += c.loan_indicator + " | ";
					if (!res.isEmpty())
						res = res.substring(0, res.length() - 3);
					s = res;
				}
				if (s.isEmpty())
					s = null;
				cell.setCellValue(s);
				break;
			case CLASSIFICATION:
				cell.setCellValue(title.classification);
				break;
			case MATERIAL_CODE:
				cell.setCellValue(title.material_code);
				break;
			case MATERIAL:
				cell.setCellValue(title.material);
				break;
			case LANGUAGE:
				cell.setCellValue(title.language_text);
				break;
			case BARCODE:
				if (aggMode == AggMode.FIRST && !title.copies.isEmpty())
					s = title.copies.get(0).barcode;
				else {
					String res = "";
					for (Copy c : title.copies)
						if (!c.isLocked() && (this.predFuncCopy == null || this.predFuncCopy.test(c)))
							res += c.barcode + " | ";
					if (!res.isEmpty())
						res = res.substring(0, res.length() - 3);
					s = res;
				}
				if (s.isEmpty())
					s = null;
				cell.setCellValue(s);
				break;
			case STATUS:
				if (aggMode == AggMode.FIRST && !title.copies.isEmpty())
					s = title.copies.get(0).status;
				else {
					String res = "";
					for (Copy c : title.copies)
						if (!c.isLocked() && (this.predFuncCopy == null || this.predFuncCopy.test(c)))
							res += c.status + " | ";
					if (!res.isEmpty())
						res = res.substring(0, res.length() - 3);
					s = res;
				}
				if (s.isEmpty())
					s = null;
				cell.setCellValue(s);
				break;
			case LOAN_DATE:
				if (aggMode == AggMode.FIRST && !title.copies.isEmpty())
					for (Copy c : title.copies)
						if (c.status.contains("ausgeliehen"))
							s = c.loan_date;
						else {
							String res = "";
							for (Copy co : title.copies)
								if (!c.isLocked() && (this.predFuncCopy == null || this.predFuncCopy.test(c)))
									res += co.loan_date + " | ";
							if (!res.isEmpty())
								res = res.substring(0, res.length() - 3);
							s = res;
						}
				if (s.isEmpty())
					s = null;
				cell.setCellValue(s);
				break;
			case SELECTION_KEY:
				if (aggMode == AggMode.FIRST && !title.copies.isEmpty())
					s = title.copies.get(0).selection_key;
				else {
					String res = "";
					for (Copy c : title.copies)
						if (!c.isLocked() && (this.predFuncCopy == null || this.predFuncCopy.test(c)))
							res += c.selection_key + " | ";
					if (!res.isEmpty())
						res = res.substring(0, res.length() - 3);
					s = res;
				}
				if (s.isEmpty())
					s = null;
				cell.setCellValue(s);
				break;
			case REMARK:
				if (aggMode == AggMode.FIRST && !title.copies.isEmpty()) {
					Copy c = title.copies.get(0);
					if (!c.remark_intern.isEmpty() && !c.remark.isEmpty())
						s = c.remark + " | " + c.remark_intern;
					else if (c.remark.isEmpty())
						s = c.remark_intern;
					else if (c.remark_intern.isEmpty())
						s = c.remark;
				} else {
					String res = "";
					for (Copy c : title.copies) {
						if (c.isLocked() || (this.predFuncCopy != null && !this.predFuncCopy.test(c)))
							continue;
						if (!c.remark_intern.isEmpty() && !c.remark.isEmpty())
							res += c.remark + " | " + c.remark_intern;
						else if (c.remark.isEmpty())
							res += c.remark_intern + " | ";
						else if (c.remark_intern.isEmpty())
							res += c.remark + " | ";
					}
					if (!res.isEmpty() && res.endsWith(" | "))
						res = res.substring(0, res.length() - 3);
					s = res;
				}
				if (s.isEmpty())
					s = null;
				cell.setCellValue(s);
				break;
			case LOCAL_EXPANSION:
				cell.setCellValue(title.local_expansion);
				break;
			case GREATER_ENTITIY:
				cell.setCellValue(title.greater_entity);
				break;
			case GREATER_ENTITY_ISSN:
				cell.setCellValue(title.greater_entity_issn);
				break;
			case DOI:
				cell.setCellValue(title.doi);
				break;
			case CUM_LOANS_TITLE:
				cell.setCellValue(title.cum_loans);
				break;
			case CUM_RESERV_TITLE:
				cell.setCellValue(title.cum_reserv);
				break;
			case NUM_COPIES_AUI_U:
				title.num_copies_loan = 0;
				for (Copy c : title.copies)
					if (c.loan_indicator.compareTo("u") == 0)
						title.num_copies_loan++;
				cell.setCellValue(title.num_copies_loan);
				break;
			case NUM_COPIES_STATS:
				cell.setCellValue(title.num_copies_cum);
				break;
			case CUM_LOAN_RATIO:
				cell.setCellValue(title.cum_loans / (title.num_copies_loan + 0.0));
				break;
			case CUM_RESERV_RATIO:
				cell.setCellValue(title.cum_reserv / (title.num_copies_loan + 0.0));
				break;
			case PUBLISHER:
				cell.setCellValue(title.publisher);
				break;
			case GREATER_ENTITY_YEAR:
				cell.setCellValue(title.greaterEntityYear);
				break;
			case LOCAL_SYSTEMATIC:
				if (aggMode == AggMode.FIRST && !title.copies.isEmpty())
					s = title.copies.get(0).local_sys;
				else {
					String res = "";
					for (Copy c : title.copies)
						if (!c.isLocked() && (this.predFuncCopy == null || this.predFuncCopy.test(c)))
							res += c.local_sys + " | ";
					if (!res.isEmpty())
						res = res.substring(0, res.length() - 3);
					s = res;
				}
				if (s.isEmpty())
					s = null;
				cell.setCellValue(s);
				break;
			case FR:
				cell.setCellValue(title.subject);
				break;
			case ON_LOAN:
				num = 0;
				for (Copy c : title.copies)
					if (c.status.contains("ausgeliehen"))
						num++;
				cell.setCellValue(num);
				break;
			case EDIT_DATE:
				if (aggMode == AggMode.FIRST && !title.copies.isEmpty())
					s = title.copies.get(0).edit_date;
				else {
					String res = "";
					for (Copy c : title.copies)
						if (!c.isLocked() && (this.predFuncCopy == null || this.predFuncCopy.test(c)))
							res += c.edit_date + " | ";
					if (!res.isEmpty())
						res = res.substring(0, res.length() - 3);
					s = res;
				}
				if (s.isEmpty())
					s = null;
				cell.setCellValue(s);
				break;
			case ORDER_ID:
				if (aggMode == AggMode.FIRST && !title.copies.isEmpty() && title.copies.get(0).orderID.length() > 0)
					s = title.copies.get(0).orderID;
				else {
					String res = "";
					for (Copy c : title.copies)
						if (c.orderID != null && c.orderID.length() > 0
								&& (this.predFuncCopy == null || this.predFuncCopy.test(c)))
							res += c.orderID + " | ";
					if (!res.isEmpty())
						res = res.substring(0, res.length() - 3);
					s = res;
				}
				if (s.isEmpty())
					s = null;
				cell.setCellValue(s);
				break;
			case ORDER_TYPE:
				if (aggMode == AggMode.FIRST && !title.copies.isEmpty() && title.copies.get(0).orderID.length() > 0)
					s = title.copies.get(0).orderType;
				else {
					String res = "";
					for (Copy c : title.copies)
						if (c.orderID != null && c.orderID.length() > 0
								&& (this.predFuncCopy == null || this.predFuncCopy.test(c)))
							res += c.orderType + " | ";
					if (!res.isEmpty())
						res = res.substring(0, res.length() - 3);
					s = res;
				}
				if (s.isEmpty())
					s = null;
				cell.setCellValue(s);
				break;
			case ORDER_STATUS:
				if (aggMode == AggMode.FIRST && !title.copies.isEmpty() && title.copies.get(0).orderID.length() > 0)
					s = title.copies.get(0).orderStatus;
				else {
					String res = "";
					for (Copy c : title.copies)
						if (c.orderID != null && c.orderID.length() > 0
								&& (this.predFuncCopy == null || this.predFuncCopy.test(c)))
							res += c.orderStatus + " | ";
					if (!res.isEmpty())
						res = res.substring(0, res.length() - 3);
					s = res;
				}
				if (s.isEmpty())
					s = null;
				cell.setCellValue(s);
				break;
			case LINK:
				cell.setCellValue(title.link);
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
				cell.setCellValue(num);
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
				cell.setCellValue(num);
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
				cell.setCellValue(num);
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
				if (max != 0)
					cell.setCellValue(max);
				else {
					String a = null;
					cell.setCellValue(a);
				}
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
				cell.setCellValue(num);
				break;
			case NUM_COPIES_NOT_G:
				num = 0;
				for (Copy c : title.copies)
					if (!c.isLocked() && (this.predFuncCopy == null || this.predFuncCopy.test(c)))
						num++;
				cell.setCellValue(num);
				break;
			case NUM_LOCATION_FH:
				int numFH = 0;
				for (Copy c : title.copies) {
					if (!c.isLocked() && (this.predFuncCopy == null || this.predFuncCopy.test(c))) {
						if ((c.location.startsWith("FH") && !c.location.startsWith("FH-P"))
								|| (c.location.startsWith("FGSE-FH") && !c.location.startsWith("FGSE-FH-P")))
							numFH++;
					}
				}
				cell.setCellValue(numFH);
				break;
			case NUM_LOCATION_FHP:
				int numFHP = 0;
				for (Copy c : title.copies) {
					if (!c.isLocked() && (this.predFuncCopy == null || this.predFuncCopy.test(c))) {
						if (c.location.startsWith("FH-Präsenz") || c.location.startsWith("FGSE-Präsenz"))
							numFHP++;
					}
				}
				cell.setCellValue(numFHP);
				break;
			case NUM_LOCATION_MG:
				int numMg = 0;
				for (Copy c : title.copies) {
					if (!c.isLocked() && (this.predFuncCopy == null || this.predFuncCopy.test(c))) {
						if (c.location.startsWith("Magazin") || c.location.startsWith("FGSE-Magazin"))
							numMg++;
					}
				}
				cell.setCellValue(numMg);
				break;
			case LAST_COPY_GVK:
				cell.setCellValue(title.last_copy_gvk);
				break;
			case COPY_HALLE:
				cell.setCellValue(title.copy_halle);
				break;
			case EDITION:
				cell.setCellValue(title.edition);
				break;
			case TYPE:
				cell.setCellValue(title.type);
				break;
			case PUBLISHER_LOCATION:
				cell.setCellValue(title.publisher_location);
				break;
			case NUM_LIBRARIES:
				cell.setCellValue(title.num_lib);
				break;
			case ILN_LIST:
				cell.setCellValue(title.iln_list);
				break;
			case BKL:
				cell.setCellValue(title.bkl);
				break;
			case COPY_MD:
				cell.setCellValue(title.copy_md);
				break;
			case RVK:
				cell.setCellValue(title.rvk);
				break;
			case DDC:
				cell.setCellValue(title.ddc);
				break;
			case ISBN:
				cell.setCellValue(title.isbn);
				break;
			case SUPER_PPN:
				cell.setCellValue(title.super_ppn);
				break;
			case SUPER_TITLE:
				cell.setCellValue(title.super_title + " " + title.volume);
				break;
			case INTERNAL_CODES:
				cell.setCellValue(title.internal_codes);
				break;
			case INVENTORY_STRING:
				if (aggMode == AggMode.FIRST && !title.copies.isEmpty()
						&& title.copies.get(0).inventory_string.length() > 0)
					s = title.copies.get(0).inventory_string;
				else {
					String res = "";
					for (Copy c : title.copies)
						if (!c.isLocked() && c.inventory_string != null && c.inventory_string.length() > 0
								&& (this.predFuncCopy == null || this.predFuncCopy.test(c)))
							res += c.inventory_string + " | ";
					if (!res.isEmpty())
						res = res.substring(0, res.length() - 3);
					s = res;
				}
				if (s.isEmpty())
					s = null;
				cell.setCellValue(s);
				break;
			case SCRIBAL:
				cell.setCellValue(title.scribal);
				break;
			case SCRIBAL_LOCAL:
				cell.setCellValue(title.scribal_local);
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
					s = res;
				}
				if (s.isEmpty())
					s = null;
				cell.setCellValue(s);
				break;
			default:

			}
		}

	}

}
