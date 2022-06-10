import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import model.Title;
import retrieve.QueryErrorException;
import services.DropbillRetriever;
import write.ResponseFactoryJSON;

public class SRU_Engine extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private DropbillRetriever retriever;
	private String sep = ";";

	public SRU_Engine() {
		try {
			retriever = new DropbillRetriever();
		} catch (ClassNotFoundException e) {
			System.err.println("Error: DB Driver not found");
		} catch (SQLException e) {
			System.err.println("Error: DB Connection could not be established");
		}
	}

	/**
	 * main method for answering jetty requests
	 * request query may contain parameters
	 * ppn a set of semicolon-separated ppn strings to be retrieved
	 * barcode a set of semicolon-separated barcode strings to be retrieved
	 * signature a set of semicolon-separated signature strings to be retrieved
	 * auto a set of semicolon-separated ppn/barcode/signature strings to be retrieved
	 * fam boolean value if related titles should be retrieved
	 */
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		response.setContentType("text/html; charset=UTF-8");
		response.setCharacterEncoding("UTF-8");

		String ppn = request.getParameter("ppn");
		String barcode = request.getParameter("barcode");
		String signature = request.getParameter("signature");
		String auto = request.getParameter("auto");
		
		String fam = request.getParameter("fam");

		List<Title> titles = null;

		String[] ppns;
		if (ppn != null)
			ppns = ppn.split(sep);
		else
			ppns = new String[0];
		String[] barcodes;
		if (barcode != null)
			barcodes = barcode.split(sep);
		else
			barcodes = new String[0];
		String[] signatures;
		if (signature != null)
			signatures = signature.split(sep);
		else
			signatures = new String[0];
		String[] autos;
		if (auto != null)
			autos = auto.split(sep);
		else
			autos = new String[0];
		
		boolean family = false;
		if (fam != null) {
			try {
				family = Boolean.valueOf(fam);
			} catch (Exception e) {
				System.out.println(e);
			}
		}
		
		try {
			titles = retriever.retrievePPN(ppns, barcodes, signatures, autos, family);
		} catch (QueryErrorException e) {
			response.getOutputStream().println(e.getMessage());
		}

		if (titles != null) {
			String result = ResponseFactoryJSON.getJSON(titles);
			response.getOutputStream().println(result);
		} else {
			response.getOutputStream().println("");
		}
	}
}
