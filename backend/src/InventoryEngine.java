import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import guard.AccessGuard;
import helper.FileArchiver;
import helper.thread.InventoryReportThread;
import retrieve.LBSConnectionPool;
import retrieve.LBSConnectionPool.TooManyConnectionsException;
import servlet.PermissionServlet;

public class InventoryEngine extends PermissionServlet {
	FileArchiver fa;
	/**
	 * 
	 */
	private static final long serialVersionUID = 2853745314439091956L;

	boolean inProgress = false;
	
	LBSConnectionPool db_pool;

	AtomicInteger count = new AtomicInteger(0);
	final int max_count = 3;

	public InventoryEngine() throws ClassNotFoundException, SQLException {
		fa = new FileArchiver();
		db_pool = new LBSConnectionPool(false);
	}

	@Override
	@AccessGuard(permissions = {})
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		// Excel download
		// response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
		response.setContentType("text/plain");
		response.setCharacterEncoding("UTF-8");
		if (this.count.get() >= this.max_count) {
			response.sendError(HttpServletResponse.SC_CONFLICT,
					"Dienst wird gerade von mehreren Personen genutzt, bitte versuchen Sie es später erneut."); // 102
																												// Processing
			response.flushBuffer();
			return;
		}

		// param handling
		String lsy = request.getParameter("lsy");
		String sst = request.getParameter("sst");
		String print = request.getParameter("print");
		String ex_level = request.getParameter("ex_level");
		String signature = request.getParameter("signature");
		String desc = "";
		boolean ex = false;
		if ((lsy == null || lsy.isEmpty()) && (sst == null || sst.isEmpty()) && (print == null || print.isEmpty())
				&& (signature == null || signature.isEmpty())) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "{\"message\": \"ERROR: query incorrect.\"}");
			return;
		}
		try {
			if (lsy != null) {
				lsy = java.net.URLDecoder.decode(lsy, StandardCharsets.UTF_8.name());
				desc = lsy.replaceAll("[^a-zA-Z0-9-_\\.]", "") + "_";
			}
			if (sst != null) {
				sst = java.net.URLDecoder.decode(sst, StandardCharsets.UTF_8.name());
				desc += sst.replaceAll("[^a-zA-Z0-9-_\\.]", "") + "_";
			}
			if (print != null) {
				print = java.net.URLDecoder.decode(print, StandardCharsets.UTF_8.name());
				desc += "print_";
			}
			if (ex_level != null) {
				ex_level = java.net.URLDecoder.decode(ex_level, StandardCharsets.UTF_8.name());
				if (ex_level.toLowerCase().compareTo("true") == 0 || ex_level.compareTo("1") == 0)
					ex = true;
			}
			if (signature != null) {
				signature = java.net.URLDecoder.decode(signature, StandardCharsets.UTF_8.name());
				desc += signature.replaceAll("[^a-zA-Z0-9-_\\.]", "") + "_";
			}
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "{\"message\": \"ERROR: query incorrect.\"}");
			return;
		}
		String user = null;
		String email = null;
		try {
			user = request.getAttribute("user").toString();
			email = request.getAttribute("email").toString();
		} catch (Exception e) {
			//user = "sbosse";
			//email = "sbosse@ovgu.de";
			response.sendError(HttpServletResponse.SC_FORBIDDEN);
			return;
		}
		System.out.println("InventoryEngine: Get Inventory by " + user + ": (" + lsy + ", " + sst + ", " + print + ", "
				+ ex_level + signature + ")");

		try {
			this.count.incrementAndGet();
			(new InventoryReportThread(this.count, this.fa, desc, user, email, ex, lsy, sst, print, signature, db_pool.getConnection())).start();
			response.getOutputStream().println(
					"Vielen Dank für Ihre Anfrage, Sie werden über die Verfügbarkeit der Datei per E-Mail benachrichtigt.");
		} catch (NamingException | ClassNotFoundException | TooManyConnectionsException | SQLException e) {
			e.printStackTrace();
		}
	}
}
