package write;

import java.util.Calendar;
import java.util.Locale;
import java.util.function.Predicate;

import model.Title;
import model.Title.Copy;
import model.Title.Copy.Statistic;

/**
 * TitleWriter that stores Title objects in a CSV file
 * one line per title, statistics are cumulated
 * copy details are shown for the first copy
 * @author sbosse
 *
 */
public class CSVWriterTitle extends AbstractCSVWriter{	
	
	boolean append = false;
	
	/**
	 * 
	 * @param destination directory of the file to be created
	 * @param datafields array of DataField enums that are to be exported
	 * @param sep the separator char for CSV file, standard ';'
	 */
	public CSVWriterTitle(String destination, DataFields[] datafields, String sep, boolean superInfos, boolean append) {
		super(destination,datafields,sep,superInfos);
		this.append = append;
	}
	
	/**
	 * 
	 * @param destination directory of the file to be created
	 * @param datafields array of DataField enums that are to be exported
	 * @param sep the separator char for CSV file, standard ';'
	 * @param predicateFunction a function for testing titles, only 'true' titles are reported
	 */
	public CSVWriterTitle(String destination, DataFields[] datafields, String sep, Predicate<Title> predicateFunction, boolean superInfos, boolean append) {
		super(destination,datafields,sep,predicateFunction,superInfos);		
		this.append = append;
	}
	
	
	public CSVWriterTitle(String destination, DataFields[] datafields, String sep, boolean append){
		super(destination,datafields,sep,false);
		this.append = append;
	}
	
	public CSVWriterTitle(String destination, DataFields[] datafields, String sep, String fileExt, boolean append){
		super(destination,datafields,sep,fileExt);
		this.append = append;
	}
	
	public void addTitle(Title title) {
		if (predFunc!=null && !predFunc.test(title)) return; //only write to CSV if the predicate is true for this title
		String s = "";
		int num = 0;
		boolean quotflag = true;
		for (DataFields df : datafields) {
			s+=quot;
			switch (df) {
			case TITLE:
				s+=title.title.replaceAll(quot, quot_replacement).replaceAll("\t", "\\t");
				if (title.volume.length() > 0) s+=", "+title.volume;
				break;
			case AUTHOR_FIRST:
				s+=title.author.replaceAll(quot, quot_replacement);
				break;
			case AUTHOR_OTHER:
				s+=title.co_authors.replaceAll(quot, quot_replacement);
				break;
			case AUTHORS:
				if (title.author.isEmpty()) s+=title.co_authors.replaceAll(quot, quot_replacement);
				else s+=title.author.replaceAll(quot, quot_replacement)+" | "+title.co_authors.replaceAll(quot, quot_replacement);
				break;
			case YEAR:
				s+=title.year_of_creation.replaceAll(quot, quot_replacement);
				break;
			case SIGNATURE:
				if (!append && !title.copies.isEmpty()) s+=title.copies.get(0).signature.replaceAll(quot, quot_replacement);
				else {
					String res = "";
					for (Copy c : title.copies) if (!c.isLocked()) res+=c.signature.replaceAll(quot, quot_replacement)+" | ";
					if (!res.isEmpty()) res = res.substring(0,res.length()-3);
					s+=res;
				}				
				break;
			case PPN:
				s+=title.ppn.replaceAll(quot, quot_replacement);
				break;
			case EPN:
				if (!append && !title.copies.isEmpty()) s+=title.copies.get(0).epn.replaceAll(quot, quot_replacement);
				else {
					String res = "";
					for (Copy c : title.copies) res+=c.epn.replaceAll(quot, quot_replacement)+" | ";
					if (!res.isEmpty()) res = res.substring(0,res.length()-3);
					s+=res;
				}	
				break;
			case LOCATION:
				if (!append && !title.copies.isEmpty()) s+=title.copies.get(0).location.replaceAll(quot, quot_replacement);
				else {
					String res = "";
					for (Copy c : title.copies) if (!c.isLocked())  res+=c.location.replaceAll(quot, quot_replacement)+" | ";
					if (!res.isEmpty()) res = res.substring(0,res.length()-3);
					s+=res;
				}	
				break;
			case LOAN_INDICATOR:
				if (!append && !title.copies.isEmpty()) s+=title.copies.get(0).loan_indicator.replaceAll(quot, quot_replacement);
				else {
					String res = "";
					for (Copy c : title.copies) res+=c.loan_indicator.replaceAll(quot, quot_replacement)+" | ";
					if (!res.isEmpty()) res = res.substring(0,res.length()-3);
					s+=res;
				}	
				break;
			case CLASSIFICATION:
				s+=title.classification.replaceAll(quot, quot_replacement);
				break;
			case MATERIAL_CODE:
				s+=title.material_code.replaceAll(quot, quot_replacement);
				break;
			case MATERIAL:
				s+=title.material.replaceAll(quot, quot_replacement);
				break;
			case LANGUAGE:
				s+=title.language_text.replaceAll(quot, quot_replacement);
				break;
			case BARCODE:
				if (!append && !title.copies.isEmpty()) s+=title.copies.get(0).barcode.replaceAll(quot, quot_replacement);
				else {
					String res = "";
					for (Copy c : title.copies) res+=c.barcode.replaceAll(quot, quot_replacement)+" | ";
					if (!res.isEmpty()) res = res.substring(0,res.length()-3);
					s+=res;
				}	
				break;
			case STATUS:
				if (!append && !title.copies.isEmpty()) s+=title.copies.get(0).status.replaceAll(quot, quot_replacement);
				else {
					String res = "";
					for (Copy c : title.copies) res+=c.status.replaceAll(quot, quot_replacement)+" | ";
					if (!res.isEmpty()) res = res.substring(0,res.length()-3);
					s+=res;
				}	
				break;
			case LOAN_DATE:
				if (!append && !title.copies.isEmpty()) for (Copy c : title.copies) if (c.status.contains("ausgeliehen")) s+=c.loan_date.replaceAll(quot, quot_replacement);
				else {
					String res = "";
					for (Copy co : title.copies) if (c.status.contains("ausgeliehen")) res+=co.loan_date.replaceAll(quot, quot_replacement)+" | ";
					if (!res.isEmpty()) res = res.substring(0,res.length()-3);
					s+=res;
				}	
				break;
			case SELECTION_KEY:
				if (!append && !title.copies.isEmpty()) s+=title.copies.get(0).selection_key.replaceAll(quot, quot_replacement);
				else {
					String res = "";
					for (Copy c : title.copies) res+=c.selection_key.replaceAll(quot, quot_replacement)+" | ";
					if (!res.isEmpty()) res = res.substring(0,res.length()-3);
					s+=res;
				}	
				break;
			case REMARK:
				if (!append && !title.copies.isEmpty()) {
					Copy c = title.copies.get(0);
					if (!c.remark_intern.isEmpty() && !c.remark.isEmpty()) s+=c.remark.replaceAll(quot, quot_replacement)+" | "+c.remark_intern.replaceAll(quot, quot_replacement);
					else if (c.remark.isEmpty()) s+=c.remark_intern.replaceAll(quot, quot_replacement);
					else if (c.remark_intern.isEmpty()) s+=c.remark.replaceAll(quot, quot_replacement);
				}
				else {
					String res = "";
					for (Copy c : title.copies) {
						if (!c.remark_intern.isEmpty() && !c.remark.isEmpty()) s+=c.remark.replaceAll(quot, quot_replacement)+" | "+c.remark_intern.replaceAll(quot, quot_replacement);
						else if (c.remark.isEmpty()) s+=c.remark_intern.replaceAll(quot, quot_replacement);
						else if (c.remark_intern.isEmpty()) s+=c.remark.replaceAll(quot, quot_replacement);
					}
					if (!res.isEmpty()) res = res.substring(0,res.length()-3);
					s+=res;
				}	
				break;
			case LOCAL_EXPANSION:
				s+=title.local_expansion.replaceAll(quot, quot_replacement);
				break;
			case GREATER_ENTITIY:
				s+=title.greaterEntity.replaceAll(quot, quot_replacement);
				break;
			case GREATER_ENTITY_ISSN:
				s+=title.greaterEntityISSN.replaceAll(quot, quot_replacement);
				break;
			case DOI:
				s+=title.doi.replaceAll(quot, quot_replacement);
				break;
			case CUM_LOANS_TITLE:
				s+=title.cum_loans;
				break;
			case CUM_RESERV_TITLE:
				s+=title.cum_reserv;
				break;
			case NUM_COPIES_AUI_U:
				title.num_copies_loan = 0;
				for (Copy c : title.copies) if (c.loan_indicator.compareTo("u")==0) title.num_copies_loan++;
				s+=title.num_copies_loan;
				break;
			case NUM_COPIES_STATS:
				s+=title.num_copies_cum;
				break;
			case CUM_LOAN_RATIO:
				/*if (num==0) {
					for (Copy c : title.copies) {
						if (c.loan_indicator.compareTo("u")==0) num++;
					}
				}
				double r = title.cum_loans / (num+0.0);*/
				s+=String.format(Locale.US, "%.2f", title.cum_loans/ (title.num_copies_loan + 0.0));
				break;
			case CUM_RESERV_RATIO:
				s+=String.format(Locale.US, "%.2f", title.cum_reserv/ (title.num_copies_loan + 0.0));
				break;
			case PUBLISHER:
				s+=title.publisher.replaceAll(quot, quot_replacement);
				break;
			case GREATER_ENTITY_YEAR:
				s+=title.greaterEntityYear;
				break;
			case LOCAL_SYSTEMATIC:
				if (!append && !title.copies.isEmpty()) s+=title.copies.get(0).local_sys.replaceAll(quot, quot_replacement);
				else {
					String res = "";
					for (Copy c : title.copies) res+=c.local_sys.replaceAll(quot, quot_replacement)+" | ";
					if (!res.isEmpty()) res = res.substring(0,res.length()-3);
					s+=res;
				}	
				break;
			case FR:
				s+=title.getFR();
				break;
			case ON_LOAN:
				num=0;
				for (Copy c : title.copies) if (c.status.contains("ausgeliehen")) num++;
				s+=num;
				break;
			case EDIT_DATE:
				if (!append && !title.copies.isEmpty()) s+=title.copies.get(0).edit_date.replaceAll(quot, quot_replacement);
				else {
					String res = "";
					for (Copy c : title.copies) res+=c.edit_date.replaceAll(quot, quot_replacement)+" | ";
					if (!res.isEmpty()) res = res.substring(0,res.length()-3);
					s+=res;
				}	
				break;
			case ORDER_ID:
				if (!append && !title.copies.isEmpty()) s+=title.copies.get(0).orderID.replaceAll(quot, quot_replacement);
				else {
					String res = "";
					for (Copy c : title.copies) res+=c.orderID.replaceAll(quot, quot_replacement)+" | ";
					if (!res.isEmpty()) res = res.substring(0,res.length()-3);
					s+=res;
				}	
				break;
			case ORDER_TYPE:
				if (!append && !title.copies.isEmpty()) s+=title.copies.get(0).orderType.replaceAll(quot, quot_replacement);
				else {
					String res = "";
					for (Copy c : title.copies) res+=c.orderType.replaceAll(quot, quot_replacement)+" | ";
					if (!res.isEmpty()) res = res.substring(0,res.length()-3);
					s+=res;
				}	
				break;
			case ORDER_STATUS:
				if (!append && !title.copies.isEmpty()) s+=title.copies.get(0).orderStatus.replaceAll(quot, quot_replacement);
				else {
					String res = "";
					for (Copy c : title.copies) res+=c.orderStatus.replaceAll(quot, quot_replacement)+" | ";
					if (!res.isEmpty()) res = res.substring(0,res.length()-3);
					s+=res;
				}	
				break;
			case LINK:
				s+=title.link.replaceAll(quot, quot_replacement);
				break;			
			case LOANS_TOTAL:
				num = 0;
				int[] loans = new int[Calendar.getInstance().get(Calendar.YEAR)-2007+1];
				for (Copy c : title.copies) for (Statistic st : c.stats) {
					int y;
					try {
						y = Integer.parseInt(st.year);
					} catch (NumberFormatException e) {
						y = 2007;
					}
					if (y >= (Calendar.getInstance().get(Calendar.YEAR)-5)) {
						num+=st.num_loans;
					}
					loans[y-2007] += st.num_loans;
				}
				for (int i=0;i<loans.length;i++) s+=loans[i]+quot+sep+quot;
				s = s.substring(0, s.length()-sep.length()-quot.length()-quot.length());
				break;				
			case RESERVE_TOTAL:
				num = 0;
				int[] ress = new int[Calendar.getInstance().get(Calendar.YEAR)-2007+1];
				for (Copy c : title.copies) for (Statistic st : c.stats) {
					int y;
					try {
						y = Integer.parseInt(st.year);
					} catch (NumberFormatException e) {
						y = 2007;
					}
					if (y >= (Calendar.getInstance().get(Calendar.YEAR)-5)) {
						num+=st.num_reserv;
					}
					ress[y-2007] += st.num_reserv;
				}
				for (int i=0;i<ress.length;i++) s+=ress[i]+quot+sep+quot;
				s = s.substring(0, s.length()-sep.length()-quot.length()-quot.length());
				break;			
			case LOANS_LAST_5_YEARS:
				num = 0;
				for (Copy c : title.copies) for (Statistic st : c.stats) {
					int y;
					try {
						y = Integer.parseInt(st.year);
					} catch (NumberFormatException e) {
						y = 2007;
					}
					if (y >= (Calendar.getInstance().get(Calendar.YEAR)-5)) {
						num+=st.num_loans;
					}
				}
				s+=num;
				break;			
			case LOANS_LAST_10_YEARS:
				num = 0;
				for (Copy c : title.copies) for (Statistic st : c.stats) {
					int y;
					try {
						y = Integer.parseInt(st.year);
					} catch (NumberFormatException e) {
						y = 2007;
					}
					if (y >= (Calendar.getInstance().get(Calendar.YEAR)-10)) {
						num+=st.num_loans;
					}
				}
				s+=num;
				break;				
			case RESERVE_LAST_5_YEARS:
				num = 0;
				for (Copy c : title.copies) for (Statistic st : c.stats) {
					int y;
					try {
						y = Integer.parseInt(st.year);
					} catch (NumberFormatException e) {
						y = 2007;
					}
					if (y >= (Calendar.getInstance().get(Calendar.YEAR)-5)) {
						num+=st.num_reserv;
					}
				}
				s+=num;
				break;				
			case LAST_LOAN:
				int max = 0;
				for (Copy c : title.copies) for (Statistic st : c.stats) {
					int y;
					try {
						y = Integer.parseInt(st.year);
					} catch (NumberFormatException e) {
						y = 2007;
					}
					if (st.num_loans>0 && y >= max) max = y;
				}
				s+=max==0? "" : max;
				break;				
			case LOANS_TIL_2007:
				num = 0;
				for (Copy c : title.copies) for (Statistic st : c.stats) {
					if (st.year.contains("2007")) {
						num += st.num_loans;
						break;
					}
				}
				s+=num;
				break;
			case NUM_COPIES_NOT_G:
				num = 0;
				for (Copy c : title.copies) if (!c.isLocked()) num++;
				s+=num;
				break;
			case NUM_LOCATIONS:
				int numFH = 0;
				int numFHP = 0;
				int numMg = 0;
				for (Copy c : title.copies) {
					if (!c.isLocked()) {
						if (c.location.startsWith("FH-Präsenz") || c.location.startsWith("FGSE-Präsenz")) numFHP++;
						else if (c.location.startsWith("FH") || c.location.startsWith("FGSE-FH")) numFH++;
						else if (c.location.startsWith("Magazin") || c.location.startsWith("FGSE-Magazin")) numMg++;
					}
				}
				s+=numFH+quot+sep+quot+numFHP+quot+sep+quot+numMg;
				break;
			case LAST_COPY_GVK:
				s+=title.lastCopyGVK;
				break;
			case COPY_HALLE:
				s+=title.copyHalle;
				break;
			case EDITION:
				s+=title.edition.replaceAll(quot, quot_replacement);
				break;
			case TYPE:
				s+=title.type.replaceAll(quot, quot_replacement);
				break;
			case PUBLISHER_LOCATION:
				s+=title.publisher_location.replaceAll(quot, quot_replacement);
				break;
			case NUM_LIBRARIES:
				s+=title.numLib;
				break;
			case ILN_LIST:
				s+=title.iln_list;
				break;
			case BKL:
				s+=title.bkl;
				break;
			case COPY_MD:
				s+=title.copyMD;
				break;
			case RVK:
				s+=title.rvk.replaceAll(quot, quot_replacement);
				break;
			case DDC:
				s+=title.ddc.replaceAll(quot, quot_replacement);
				break;
			case ISBN:
				s+=title.isbn;
				break;
			default:
				s+="";
			}
			if (quotflag) s+=quot+sep;
			else {
				s+=sep;
				quotflag=true;
			}
		}
		if (this.datafields.length != 0) s = s.substring(0, s.length()-1);
		pw.write(s+"\n");
		
		pw.flush();
		
	}
	
}
