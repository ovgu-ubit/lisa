package write;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Predicate;

import model.Title;
import model.Title.Copy;

public abstract class AbstractCSVWriter implements TitleWriter {

	String destination;
	DataFields[] datafields;
	boolean superInfos = false;
	Predicate<Title> predFunc;
	Predicate<Copy> predFuncCopy;

	PrintWriter pw;
	StringBuilder sb;
	String sep = ";";
	String quot = "\"";
	String quot_replacement = "'";
	DateTimeFormatter date = DateTimeFormatter.ofPattern("yyMMdd_HHmmss_SSS");
	String file_ext = "";

	public AbstractCSVWriter(String destination, DataFields[] datafields, String sep, boolean superInfos) {
		this.superInfos = superInfos;
		this.destination = destination;
		this.datafields = datafields;
		this.sep = sep;
	}

	public AbstractCSVWriter(String destination, DataFields[] datafields, String sep,
			Predicate<Title> predicateFunction, Predicate<Copy> predicateFunctionCopy, boolean superInfos) {
		this(destination, datafields, sep, superInfos);
		this.predFunc = predicateFunction;
		this.predFuncCopy = predicateFunctionCopy;
	}

	public AbstractCSVWriter(String destination, DataFields[] datafields, String sep) {
		this(destination, datafields, sep, false);
	}

	public AbstractCSVWriter(String destination, DataFields[] datafields, String sep, String fileExt) {
		this(destination, datafields, sep, false);
		this.file_ext = fileExt;
	}

	public String init(String name) {
		if (pw != null)
			pw.close();
		String filename = "";
		// writes header with DataField name
		String s = getHeader() + "\n";
		try {
			// creates a file name using query and current DateTime
			if (name.length() > 100)
				name = name.substring(0, 100);
			filename = destination + name.replaceAll("[*<>\"()\\?]", "") + "_" + file_ext
					+ this.date.format(LocalDateTime.now()) + ".csv";
			pw = new PrintWriter(new File(filename), "UTF-16LE"); // "UTF-8"
			pw.write(s);
			return filename;
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			System.out.println(e);
			return null;
		}
	}

	String getHeader() {
		String res = quot;
		for (int i = 0; i < datafields.length; i++) {
			res += datafields[i].toString().replaceAll(";", sep).replaceAll("\"", quot) + quot + sep + quot;
		}
		if (this.datafields.length != 0)
			res = res.substring(0, res.length() - sep.length() - quot.length());
		return res;
	}

	public void close() {
		pw.close();
	}

	@Override
	protected void finalize() throws Throwable {
		if (pw != null)
			pw.close();
	}

	@Override
	public boolean superInfos() {
		return superInfos;
	}

}
