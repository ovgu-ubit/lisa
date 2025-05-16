package services;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import retrieve.QueryErrorException;
import retrieve.QueryFactory;
import retrieve.XMLReader;
import write.CSVWriterTitle;

/**
 * class for creating reports for the UB subject specialists
 * 
 * @author sbosse
 *
 */
public class DropbillLinkCreator {

	XMLReader xr;
	int[] years;

	/**
	 * 
	 * @param db
	 * @param num_years how many years back statistics should be retrieved
	 */
	public DropbillLinkCreator() {

		QueryFactory qf = new QueryFactory();
		CSVWriterTitle tw = null;

		xr = new XMLReader(qf, tw);
	}

	String buildLink(List<String> ppns) {
		String res = "https://service.ub.uni-magdeburg.de/ub-dropbill/?ppn=";
		for (String ppn : ppns)
			res += ppn + ";";
		return res.substring(0, res.length() - 1);
	}

	List<PPNLink> getDropbillLinksFromSuperPPN(File file) throws QueryErrorException {
		List<String> ppns = new ArrayList<String>();
		List<PPNLink> links = new ArrayList<PPNLink>();

		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(file));
			String line;
			while ((line = br.readLine()) != null) {
				if (line.matches("[0-9]{8}[0-9X][0-9X]?"))
					ppns.add(line);
				else if (line.matches("[0-9]{7}[0-9X][0-9X]?"))
					ppns.add("0" + line);
			}
			System.out.println(ppns.size() + " PPNs read");
			br.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		for (String ppn : ppns) {
			List<String> subtitles = xr.getFamilyPPNs(ppn);
			if (subtitles.size() <= 100)
				links.add(new PPNLink(ppn, this.buildLink(subtitles)));
			else {
				for (int i = 0; i < Math.ceil(subtitles.size() / 100.0); i++) {
					int lastIndex = subtitles.size() - 100 * i >= 100 ? 100 * i + 100 : subtitles.size();
					links.add(new PPNLink(ppn, this.buildLink(subtitles.subList(100 * i, lastIndex))));
				}
			}
		}

		return links;
	}

	public class PPNLink {
		String ppn_c;
		String link;

		public PPNLink(String ppn_c, String link) {
			this.link = link;
			this.ppn_c = ppn_c;
		}
	}

	public static void main(String[] args) {
		DropbillLinkCreator ca = new DropbillLinkCreator();
		try {
			List<PPNLink> links = ca.getDropbillLinksFromSuperPPN(new File("./input/ppn_r.txt"));
			for (PPNLink link : links) {
				System.out.println(link.ppn_c + ": " + link.link);
			}
		} catch (QueryErrorException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
