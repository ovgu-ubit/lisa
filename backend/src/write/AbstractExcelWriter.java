package write;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFTable;
import org.apache.poi.xssf.usermodel.XSSFTableStyleInfo;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTTableColumn;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTTableColumns;

import model.Title;
import model.Title.Copy;
import model.Title.Copy.Statistic;

public abstract class AbstractExcelWriter implements TitleWriter {

	String destination;
	DataFields[] datafields;
	boolean superInfos = false;
	Predicate<Title> predFunc;
	Predicate<Copy> predFuncCopy;

	DateTimeFormatter date = DateTimeFormatter.ofPattern("yyMMdd_HHmmss_SSS");
	DateTimeFormatter toDate = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	CellStyle cellStyle;

	OutputStream outputStream;
	Workbook workbook;
	Sheet sheet;
	XSSFTable table;
	int idx;

	boolean analysis;
	List<Title> collected;

	public AbstractExcelWriter(String destination, DataFields[] datafields, boolean superInfos) {
		this.superInfos = superInfos;
		this.destination = destination;
		this.datafields = datafields;
	}

	public AbstractExcelWriter(String destination, DataFields[] datafields, Predicate<Title> predicateFunction,
			Predicate<Copy> predicateFunctionCopy, boolean superInfos, boolean analysis) {
		this(destination, datafields, superInfos);
		this.predFunc = predicateFunction;
		this.predFuncCopy = predicateFunctionCopy;
		this.analysis = analysis;
		if (this.analysis)
			collected = new ArrayList<Title>();
	}

	public AbstractExcelWriter(String destination, DataFields[] datafields) {
		this(destination, datafields, false);
	}

	public AbstractExcelWriter(OutputStream os, DataFields[] datafields) {
		this.outputStream = os;
		this.datafields = datafields;
	}

	public AbstractExcelWriter(OutputStream os, DataFields[] datafields, boolean analysis) {
		this.outputStream = os;
		this.datafields = datafields;
		this.analysis = analysis;
		if (this.analysis)
			collected = new ArrayList<Title>();
	}

	public String init(String name) {
		try {
			this.idx = 0;
			// if (outputStream!=null) outputStream.close();
			this.workbook = new SXSSFWorkbook(100);

			this.cellStyle = this.workbook.createCellStyle();
			this.cellStyle.setDataFormat((short) 22);

			this.sheet = workbook.createSheet("Bestandsliste");
			// Header
			Row header = this.sheet.createRow(this.idx++);
			for (int i = 0; i < datafields.length; i++) {
				Cell headerCell = header.createCell(i);
				headerCell.setCellValue(datafields[i].toString());
			}

			String filename = "";
			// creates a file name using query and current DateTime
			if (name == null)
				name = "";
			if (name.length() > 100)
				name = name.substring(0, 100);
			filename = destination + name.replaceAll("[*<>\"()\\?]", "") + "_" + this.date.format(LocalDateTime.now())
					+ ".xlsx";
			if (outputStream == null)
				outputStream = new FileOutputStream(filename);
			return filename;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public void close() {
		try {
			AreaReference reference = workbook.getCreationHelper().createAreaReference(new CellReference(0, 0),
					new CellReference(idx - 1, this.datafields.length - 1));
			this.table = ((SXSSFWorkbook) this.workbook).getXSSFWorkbook().getSheetAt(0).createTable(reference);
			if (table == null || table.getArea() == null) {
				// no results retrieved
				Cell cell = ((SXSSFWorkbook) this.workbook).getXSSFWorkbook().getSheetAt(0).createRow(1).createCell(1);
				cell.setCellValue("Leider wurden keine Ergebnisse gefunden, überprüfen Sie bitte die Abfrage.");
				System.out.println("Error: Table empty");
			} else {
				table.getCTTable().addNewAutoFilter().setRef(table.getArea().formatAsString());
				table.setName("Inventory");
				table.setDisplayName("Inventory_Table");

				// For now, create the initial style in a low-level way
				table.getCTTable().addNewTableStyleInfo();
				table.getCTTable().getTableStyleInfo().setName("TableStyleMedium2");

				// Style the table
				XSSFTableStyleInfo style = (XSSFTableStyleInfo) table.getStyle();
				style.setName("TableStyleMedium2");
				style.setShowColumnStripes(false);
				style.setShowRowStripes(true);
				style.setFirstColumn(false);
				style.setLastColumn(false);
				style.setShowRowStripes(true);
				style.setShowColumnStripes(false);

				CTTableColumns columns = table.getCTTable().getTableColumns();
				for (int c = 0; c < this.datafields.length; c++) {
					CTTableColumn column = columns.getTableColumnArray(c);
					column.setName(datafields[c].toString());
				}

				if (this.analysis) {
					System.out.println("Analyzing data according to local systematic");
					this.analyzeLSY();
				}
			}
			this.workbook.write(outputStream);
			outputStream.close();

			((SXSSFWorkbook) this.workbook).dispose();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void finalize() throws Throwable {
		System.out.println("Closing stream...");
		if (outputStream != null)
			outputStream.close();
	}

	@Override
	public boolean superInfos() {
		return superInfos;
	}

	protected LocalDate getDate(String s) {
		int idx = s.lastIndexOf(".");
		if (idx != -1)
			s = s.substring(0, idx);
		return LocalDate.parse(s, this.toDate);
	}

	/**
	 * method to provide an analysis on the set of collected titles based on their
	 * classification
	 * 
	 * @return a csv-style String with classes and statistics
	 */
	public void analyzeLSY() {
		List<String> classes = new ArrayList<String>();
		// find all classes
		for (Title t : collected) {
			if (t.classification.isEmpty())
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
		Sheet sheet1 = workbook.createSheet("Sachgebietsanalyse");
		// header
		String[] tableHeadings = new String[] { "Sachgebiet", "Anzahl Titel", "Anzahl Exemplare nicht gesperrt",
				"Anzahl Entleihungen", "Anzahl Entleihungen letzte 5 Jahre", "Anzahl Vormerkungen",
				"Anzahl Titel nicht ausgeliehen", "Anzahl Exemplare Magazin", "Anzahl Titel älter als 20 Jahre",
				"davon nicht ausgeliehen", "mit Exemplarzahl", "davon 1-3 Mal ausgeliehen", "Mehrfachexemplare" };
		Row header = sheet1.createRow(0);
		for (int i = 0; i < tableHeadings.length; i++) {
			Cell headerCell = header.createCell(i);
			headerCell.setCellValue(tableHeadings[i]);
		}

		// rows
		for (int i = 0; i < classes.size(); i++) {
			Row row = sheet1.createRow(i + 1);
			Cell cell = row.createCell(0);
			cell.setCellValue(classes.get(i));
			cell = row.createCell(1);
			cell.setCellValue(titlesTotal.get(i));
			cell = row.createCell(2);
			cell.setCellValue(copiesTotal.get(i));
			cell = row.createCell(3);
			cell.setCellValue(loans.get(i));
			cell = row.createCell(4);
			cell.setCellValue(loans5.get(i));
			cell = row.createCell(5);
			cell.setCellValue(reserv.get(i));
			cell = row.createCell(6);
			cell.setCellValue(titlesNever.get(i));
			cell = row.createCell(7);
			cell.setCellValue(copiesMag.get(i));
			cell = row.createCell(8);
			cell.setCellValue(titles20.get(i));
			cell = row.createCell(9);
			cell.setCellValue(titles20not.get(i));
			cell = row.createCell(10);
			cell.setCellValue(copies20not.get(i));
			cell = row.createCell(11);
			cell.setCellValue(titles20three.get(i));
			cell = row.createCell(12);
			cell.setCellValue(copies20three.get(i));
		}
		// sum

		Row row = sheet1.createRow(classes.size() + 1);
		Cell cell = row.createCell(0);
		cell.setCellValue("Summe");
		cell = row.createCell(1);
		cell.setCellValue(titlesTotal.stream().collect(Collectors.summingInt(Integer::intValue)));
		cell = row.createCell(2);
		cell.setCellValue(copiesTotal.stream().collect(Collectors.summingInt(Integer::intValue)));
		cell = row.createCell(3);
		cell.setCellValue(loans.stream().collect(Collectors.summingInt(Integer::intValue)));
		cell = row.createCell(4);
		cell.setCellValue(loans5.stream().collect(Collectors.summingInt(Integer::intValue)));
		cell = row.createCell(5);
		cell.setCellValue(reserv.stream().collect(Collectors.summingInt(Integer::intValue)));
		cell = row.createCell(6);
		cell.setCellValue(titlesNever.stream().collect(Collectors.summingInt(Integer::intValue)));
		cell = row.createCell(7);
		cell.setCellValue(copiesMag.stream().collect(Collectors.summingInt(Integer::intValue)));
		cell = row.createCell(8);
		cell.setCellValue(titles20.stream().collect(Collectors.summingInt(Integer::intValue)));
		cell = row.createCell(9);
		cell.setCellValue(titles20not.stream().collect(Collectors.summingInt(Integer::intValue)));
		cell = row.createCell(10);
		cell.setCellValue(copies20not.stream().collect(Collectors.summingInt(Integer::intValue)));
		cell = row.createCell(11);
		cell.setCellValue(titles20three.stream().collect(Collectors.summingInt(Integer::intValue)));
		cell = row.createCell(12);
		cell.setCellValue(copies20three.stream().collect(Collectors.summingInt(Integer::intValue)));

		String column_letter = CellReference.convertNumToColString(12);

		AreaReference reference = workbook.getCreationHelper().createAreaReference(
				new CellReference("Sachgebietsanalyse!A1"),
				new CellReference("Sachgebietsanalyse!" + column_letter + (classes.size() + 2)));
		XSSFTable table = ((SXSSFWorkbook) this.workbook).getXSSFWorkbook().getSheetAt(1).createTable(reference);
		table.getCTTable().addNewAutoFilter().setRef(table.getArea().formatAsString());
		String tableName = "Analysis";
		table.setName(tableName);
		table.setDisplayName("Analysis_Table");

		// For now, create the initial style in a low-level way
		table.getCTTable().addNewTableStyleInfo();
		table.getCTTable().getTableStyleInfo().setName("TableStyleMedium2");

		// Style the table
		XSSFTableStyleInfo style = (XSSFTableStyleInfo) table.getStyle();
		style.setName("TableStyleMedium2");
		style.setShowColumnStripes(false);
		style.setShowRowStripes(true);
		style.setFirstColumn(false);
		style.setLastColumn(false);
		style.setShowRowStripes(true);
		style.setShowColumnStripes(false);

		CTTableColumns columns = table.getCTTable().getTableColumns();
		for (int c = 0; c < tableHeadings.length; c++) {
			CTTableColumn column = columns.getTableColumnArray(c);
			column.setName(tableHeadings[c]);
		}

		// footer row
		((SXSSFWorkbook) this.workbook).getXSSFWorkbook().setCellFormulaValidation(false);
		table.getCTTable().getTableColumns().getTableColumnArray(0).setTotalsRowLabel("Summe");
		for (int i = 1; i < 13; i++) {
			table.getCTTable().getTableColumns().getTableColumnArray(i)
					.setTotalsRowFunction(org.openxmlformats.schemas.spreadsheetml.x2006.main.STTotalsRowFunction.SUM);
		}
		table.getCTTable().setTotalsRowCount(1);
	}
}
