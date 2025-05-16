package helper.thread;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import helper.FileArchiver;

public abstract class AReportThread extends Thread {

	FileArchiver fa;
	File file;
	String file_desc;
	String user;
	String email;

	String base_path;
	AtomicInteger count;

	FileOutputStream fos;
	String filename_mask;

	@SuppressWarnings("unchecked")
	public AReportThread(AtomicInteger count, FileArchiver fa, String file_desc, String user, String email,
			String filename_mask) throws NamingException, FileNotFoundException {
		super();
		this.count = count;
		this.fa = fa;
		this.file_desc = file_desc;
		this.user = user;
		this.email = email;
		this.filename_mask = filename_mask;
		InitialContext initialContext = new InitialContext();
		if (initialContext != null) {
			Map<String, String> system = (Map<String, String>) initialContext.lookup("java:/comp/env/system");
			this.base_path = system.get("dir");
			if (!this.base_path.endsWith("/"))
				this.base_path += "/";
		}
		this.initStream(this.base_path + this.filename_mask + this.file_desc
				+ new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date()) + ".xlsx");
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
	}

	private void initStream(String filename) throws FileNotFoundException {
		this.file = new File(filename);
		this.fos = new FileOutputStream(this.file);
	}

	protected void archiveFile(String user, String email) throws IOException {
		int response = fa.archiveFile(file, user, email);
		if (response == 201)
			file.delete();
		else
			System.out.println(response);
	}
}
