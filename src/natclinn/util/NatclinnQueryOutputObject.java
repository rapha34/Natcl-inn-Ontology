package natclinn.util;

public class NatclinnQueryOutputObject {
	private NatclinnQueryObject query;
	private String queryResponse;
	
	public NatclinnQueryOutputObject(NatclinnQueryObject query, String queryResponse) {
		super();
		this.query = query;
		this.queryResponse = queryResponse;
	}

	public NatclinnQueryObject getQuery() {
		return query;
	}

	public void setQuery(NatclinnQueryObject query) {
		this.query = query;
	}

	public String getQueryResponse() {
		return queryResponse;
	}

	public void setQueryResponse(String queryResponse) {
		this.queryResponse = queryResponse;
	}
	
	
	
}
