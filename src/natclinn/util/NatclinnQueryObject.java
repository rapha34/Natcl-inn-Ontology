package natclinn.util;

public class NatclinnQueryObject {
	private String titleQuery;
	private String typeQuery;
	private String stringQuery;
	
	
	public NatclinnQueryObject() {
		super();
	}

	public NatclinnQueryObject(String titleQuery, String typeQuery, String stringQuery) {
		this.titleQuery = titleQuery;
		this.typeQuery = typeQuery;
		this.stringQuery = stringQuery;
	}

	public String getTypeQuery() {
		return typeQuery;
	}

	public void setTypeQuery(String typeQuery) {
		this.typeQuery = typeQuery;
	}

	public String getTitleQuery() {
		return titleQuery;
	}

	public void setTitleQuery(String titleQuery) {
		this.titleQuery = titleQuery;
	}

	public String getStringQuery() {
		return stringQuery;
	}

	public void setStringQuery(String stringQuery) {
		this.stringQuery = stringQuery;
	}

}
