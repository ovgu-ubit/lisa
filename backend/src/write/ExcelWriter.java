package write;

import java.io.OutputStream;
import java.util.Calendar;
import java.util.function.Predicate;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

import model.Title;
import model.Title.Copy;
import model.Title.Copy.Statistic;

/**
 * TitleWriter that stores Title objects in a CSV file one line per copy,
 * statistics are cumulated
 * 
 * @author sbosse
 *
 */
public class ExcelWriter extends AbstractExcelWriter {

	/**
	 * 
	 * @param destination directory of the file to be created
	 * @param datafields  array of DataField enums that are to be exported
	 * @param sep         the separator char for CSV file, standard ';'
	 */
	public ExcelWriter(String destination, DataFields[] datafields, boolean superInfos) {
		super(destination, datafields, superInfos);
	}

	/**
	 * 
	 * @param destination       directory of the file to be created
	 * @param datafields        array of DataField enums that are to be exported
	 * @param sep               the separator char for CSV file, standard ';'
	 * @param predicateFunction a function for testing titles, only 'true' titles
	 *                          are reported
	 */
	public ExcelWriter(String destination, DataFields[] datafields, Predicate<Title> predicateFunction,
			Predicate<Copy> predicateFunctionCopy, boolean superInfos, boolean analysis) {
		super(destination, datafields, predicateFunction, predicateFunctionCopy, superInfos, analysis);
	}

	public ExcelWriter(String destination, DataFields[] datafields) {
		super(destination, datafields, false);
	}

	public ExcelWriter(OutputStream os, DataFields[] datafields) {
		super(os, datafields);
	}

	public ExcelWriter(OutputStream os, DataFields[] datafields, boolean analysis) {
		super(os, datafields, analysis);
	}

	public void addTitle(Title title) {
		if (predFunc != null && !predFunc.test(title))
			return; // only write to CSV if the predicate is true for this title
		if (this.analysis)
			this.collected.add(title);
		for (Copy c : title.copies) {
			if (c.epn == "" || (this.predFuncCopy != null && !this.predFuncCopy.test(c)))
				continue; // if copy numbers are not in sequence
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
					cell.setCellValue(c.signature);
					break;
				case SIG_MAG:
					if (c.location.startsWith("Mag"))
						cell.setCellValue(c.signature);
					break;
				case SIG_NO_MAG:
					if (!c.location.startsWith("Mag"))
						cell.setCellValue(c.signature);
					break;
				case PPN:
					cell.setCellValue(title.ppn);
					break;
				case EPN:
					cell.setCellValue(c.epn);
					break;
				case LOCATION:
					cell.setCellValue(c.location);
					break;
				case LOAN_INDICATOR:
					cell.setCellValue(c.loan_indicator);
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
					cell.setCellValue(c.barcode);
					break;
				case STATUS:
					cell.setCellValue(c.status);
					break;
				case LOAN_DATE:
					s = "";
					// if (c.status.contains("ausgeliehen")) s=c.loan_date;
					// YYYY-MM-DD HH:mm:ss.SS
					s = c.loan_date;
					cell.setCellStyle(this.cellStyle);
					cell.setCellValue(this.getDate(s));
					break;
				case SELECTION_KEY:
					cell.setCellValue(c.selection_key);
					break;
				case REMARK:
					s = "";
					if (!c.remark_intern.isEmpty() && !c.remark.isEmpty())
						s = c.remark + " | " + c.remark_intern;
					else if (c.remark.isEmpty())
						s = c.remark_intern;
					else if (c.remark_intern.isEmpty())
						s = c.remark;
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
				case PUBLISHER:
					cell.setCellValue(title.publisher);
					break;
				case GREATER_ENTITY_YEAR:
					cell.setCellValue(title.greaterEntityYear);
					break;
				case LOCAL_SYSTEMATIC:
					cell.setCellValue(c.local_sys);
					break;
				case FR:
					cell.setCellValue(title.subject);
					break;
				case ON_LOAN:
					if (c.status.contains("ausgeliehen"))
						cell.setCellValue(true);
					break;
				case EDIT_DATE:
					cell.setCellValue(c.edit_date);
					break;
				case ORDER_ID:
					cell.setCellValue(c.orderID);
					break;
				case ORDER_TYPE:
					cell.setCellValue(c.orderType);
					break;
				case ORDER_STATUS:
					cell.setCellValue(c.orderStatus);
					break;
				case LINK:
					cell.setCellValue(title.link);
					break;
				case LOANS_LAST_5_YEARS:
					num = 0;
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
					cell.setCellValue(num);
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
					cell.setCellValue(num);
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
					cell.setCellValue(num);
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
					if (max != 0)
						cell.setCellValue(max);
					else
						cell.setCellValue("");
					break;
				case LOANS_TIL_2007:
					num = 0;
					for (Statistic st : c.stats) {
						if (st.year.contains("2007")) {
							num += st.num_loans;
							break;
						}
					}
					cell.setCellValue(num);
					break;
				case NUM_COPIES_NOT_G:
					cell.setCellValue(!c.isLocked());
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
					cell.setCellValue(c.inventory_string);
					break;
				case SCRIBAL:
					cell.setCellValue(title.scribal);
					break;
				case SCRIBAL_LOCAL:
					cell.setCellValue(title.scribal_local);
					break;
				case CALL_SIGN:
					cell.setCellValue(c.call_sign);
					break;
				default:

				}
			}
		}

	}

}
