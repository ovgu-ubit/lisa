package helper.thread;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.naming.NamingException;
import javax.xml.soap.SOAPException;

import helper.FileArchiver;
import retrieve.DatabaseConnection;
import retrieve.QueryErrorException;
import services.ClassificationAnalysis;

public class InventoryReportThread extends AReportThread {

	boolean ex;
	String lsy;
	String sst;
	String print;
	String signature;
	DatabaseConnection db;

	public InventoryReportThread(AtomicInteger count, FileArchiver fa, String file_desc, String user, String email,
			boolean ex, String lsy, String sst, String print, String signature, DatabaseConnection db)
			throws NamingException, FileNotFoundException {
		super(count, fa, file_desc, user, email, "download/inventory_report_");
		this.ex = ex;
		this.lsy = lsy;
		this.sst = sst;
		this.print = print;
		this.signature = signature;
		this.db = db;
	}

	@Override
	public void run() {
		ClassificationAnalysis ca = null;
		try {
			ca = new ClassificationAnalysis(this.fos, this.db, this.ex);
			ca.getReport(this.lsy, new String[] {});

			this.archiveFile(this.user, this.email);
		} catch (IOException | ClassNotFoundException | SOAPException | SQLException | QueryErrorException e) {
			e.printStackTrace();
		} finally {
			System.out.println("Fin: " + this.file_desc);
			this.count.decrementAndGet();
			if (ca != null) ca.close();
		}
	}

}
