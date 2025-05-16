package write;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.google.gson.Gson;

import model.Title;

/**
 * TitleWriter that creates a JSON file with defined datafields
 * 
 * @author sbosse
 *
 */
public class JSONFileWriter implements TitleWriter {

	String destination;
	DataFields[] datafields;

	PrintWriter pw;
	StringBuilder sb;

	DateTimeFormatter date = DateTimeFormatter.ofPattern("yyMMdd_HHmmss");

	Gson gson;

	public JSONFileWriter(String destination, DataFields[] datafields, boolean superInfos) {
		this.superInfos = superInfos;
		this.destination = destination;
		this.datafields = datafields;

		this.gson = new Gson();
	}

	public String init(String name) {
		if (pw == null) {
			String filename = "";
			try {
				filename = destination + this.date.format(LocalDateTime.now()) + ".json";
				File f = new File(filename);
				// System.out.println(f.getAbsolutePath());
				f.createNewFile();
				pw = new PrintWriter(f, "UTF-8");
			} catch (IOException e) {
				e.printStackTrace();
			}
			return filename;
		} else
			return null;
	}

	public void addTitle(Title title) {
		pw.write(gson.toJson(title));
		pw.flush();
	}

	public void close() {
		pw.close();
	}

	boolean superInfos;

	@Override
	public boolean superInfos() {
		return superInfos;
	}
}
