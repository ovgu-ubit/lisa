package services;

import retrieve.QueryErrorException;
import retrieve.XMLReader;

public class QueryRetriever {
	XMLReader xr;
	
	boolean write;
	int max = 1500000;
	
	public QueryRetriever(XMLReader xr, boolean directWrite, int max) {
		this.xr = xr;
		this.write = directWrite;
		this.max = max;
	}
	
	public void setMaxRes(int max) {
		this.max = max;
	}
	
	public int getMaxRes() {
		return max;
	}
	
	public boolean retrieve(String query, int stat_years[], boolean orderInfos, boolean GVK_infos, boolean classInfosFromSuperTitle) throws QueryErrorException {
		return xr.retrieve(query, max, write,stat_years,orderInfos,GVK_infos,classInfosFromSuperTitle);
	}
	
	public boolean retrieve(String query, int pos, int stat_years[], boolean orderInfos, boolean GVK_infos, boolean classInfosFromSuperTitle) throws QueryErrorException {
		return xr.retrieve(query, max, pos, write,stat_years,orderInfos,GVK_infos,classInfosFromSuperTitle);
	}
}
