package services;

import retrieve.QueryErrorException;
import retrieve.QueryFactory;
import retrieve.XMLReader;
import write.CSVWriterTitle;
import write.TitleWriter.DataFields;

public class AcquisitionProposals {
	
	QueryRetriever qr;
	
	public AcquisitionProposals() {
		QueryFactory qf = new QueryFactory("https://sru.k10plus.de/","gvk");
		CSVWriterTitle tw = new CSVWriterTitle("./res/",new DataFields[] {DataFields.AUTHORS, DataFields.TITLE, DataFields.EDITION, DataFields.YEAR, 
			DataFields.PUBLISHER, DataFields.PUBLISHER_LOCATION, DataFields.TYPE, DataFields.PPN, 
			DataFields.BKL, DataFields.RVK, DataFields.DDC, DataFields.MATERIAL, DataFields.LANGUAGE, 
			DataFields.LAST_COPY_GVK, DataFields.COPY_HALLE, DataFields.COPY_MD, DataFields.NUM_LIBRARIES, DataFields.ILN_LIST}
			,";", "" ,true);
		
		XMLReader xr = new XMLReader(qf,tw);
		qr = new QueryRetriever(xr,false,1000);
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		AcquisitionProposals ap = new AcquisitionProposals();
		try {
			//ap.qr.retrieve("(pica.bkl=\"71*\"+or+pica.bkl=\"83*\"+or+pica.bkl=\"85*\"+or+pica.rvk=\"MS*\"+or+pica.rvk=\"Q*\")", null, false, true, false);
			ap.qr.retrieve("(pica.bkl=\"54.31\"+or+pica.rvk=\"ST%2015*\")", null, false, true, false);
			ap.qr.xr.write();
		} catch (QueryErrorException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
