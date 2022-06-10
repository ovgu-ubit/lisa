package retrieve;

public class QueryErrorException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 9080154489151822786L;

	int code;
	String msg;
	String query;
	
	public QueryErrorException(int code, String msg, String query) {
		this.msg = msg;
		this.code = code;
		this.query = query;
	}
	
	@Override
	public String getMessage() {
		String json = "[{";
		json+="\"errorcode\":\""+code+"\",";
		json+="\"query\":\""+query+"\"}]";
		
		return json;
	}
}
