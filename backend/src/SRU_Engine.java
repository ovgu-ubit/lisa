import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import helper.ParamParsing;
import model.Title;
import model.serialize.DropbillSerializer;
import retrieve.DatabaseConnection;
import retrieve.LBSConnectionPool;
import retrieve.LBSConnectionPool.TooManyConnectionsException;
import retrieve.QueryErrorException;
import services.DropbillRetriever;

public class SRU_Engine extends HttpServlet {

	private static final long serialVersionUID = 1L;
	boolean test = false;
	LBSConnectionPool db_pool = null;
	DatabaseConnection db_local = null;
	private final String sep = ";";
	private Gson gson;

	public SRU_Engine() {
		try {
			db_pool = new LBSConnectionPool(test);
			gson = new GsonBuilder().registerTypeAdapter(Title.class, new DropbillSerializer()).create();
		} catch (ClassNotFoundException | SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * main method for answering jetty requests request query may contain parameters
	 * ppn a set of semicolon-separated ppn strings to be retrieved barcode a set of
	 * semicolon-separated barcode strings to be retrieved signature a set of
	 * semicolon-separated signature strings to be retrieved auto a set of
	 * semicolon-separated ppn/barcode/signature strings to be retrieved fam boolean
	 * value if related titles should be retrieved
	 */
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		System.out.println("SRUEngine: Get by " + request.getRemoteHost());
		response.setContentType("application/json; charset=UTF-8");
		response.setCharacterEncoding("UTF-8");

		DropbillRetriever retriever = null;
		DatabaseConnection db = null;
		try {
			db = db_pool.getConnection();
			// db_local = new DatabaseConnection(false, true);//currently, FR information is
			// not needed
			retriever = new DropbillRetriever(db, db_local);
		} catch (ClassNotFoundException e) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
					"{\"message\": \"Error: DB Driver not found\"}");
			e.printStackTrace();
			return;
		} catch (SQLException e) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
					"{\"message\": \"Error: DB Connection could not be established\"}");
			e.printStackTrace();
			return;
		} catch (TooManyConnectionsException e) {
			response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE,
					"{\"message\": \"ERROR: Too many connections\"}");
		}

		// param handling
		String[] ppns, barcodes, signatures, autos;
		try {
			ppns = ParamParsing.parseStringArray(request, "ppn", sep);
			barcodes = ParamParsing.parseStringArray(request, "barcode", sep);
			signatures = ParamParsing.parseStringArray(request, "signature", sep);
			autos = ParamParsing.parseStringArray(request, "auto", sep);

		} catch (Exception e) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "{\"message\": \"ERROR while parsing parameters\"}");
			return;
		}

		if ((ppns == null || ppns.length == 0) && (barcodes == null || barcodes.length == 0) && (signatures == null || signatures.length == 0) && (autos == null || autos.length == 0)) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "{\"message\": \"ERROR: Wrong parameters\"}");
			return;
		}

		List<Title> titles = null;

		boolean family = ParamParsing.parseBoolean(request, "fam");

		// processing
		try {
			titles = retriever.retrievePPN(ppns, barcodes, signatures, autos, family);
		} catch (QueryErrorException e) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
					"{\"message\": \"" + e.getMessage() + "\"}");
			e.printStackTrace();
			return;
		} finally {
			try {
				if (db != null)
					db_pool.releaseConnection(db);
			} catch (Exception e) {
			}
		}

		if (titles != null) {
			String result = gson.toJson(titles);
			response.getOutputStream().println(result);
		} else {
			response.getOutputStream().println("");
		}

		if (db != null)
			db_pool.releaseConnection(db);
	}
}
