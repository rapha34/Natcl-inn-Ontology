package natclinn.util;

public class NatclinnQueryObject {
	private String titleQuery;
	private String commentQuery;
	private String typeQuery;
	private String stringQuery;
	private Integer idQuery;

	
	
	public NatclinnQueryObject() {
		super();
	}

	public NatclinnQueryObject(String titleQuery, String commentQuery, String typeQuery, String stringQuery, Integer idQuery) {
		super();
		this.titleQuery = titleQuery;
		this.commentQuery = commentQuery;
		this.typeQuery = typeQuery;
		this.stringQuery = stringQuery;
		this.idQuery = idQuery;
		
	}
	public String getCommentQuery() {
		return commentQuery;
	}
	public void setCommentQuery(String commentQuery) {
		this.commentQuery = commentQuery;
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
	public Integer getIdQuery() {
		return idQuery;
	}
	public void setIdQuery(Integer idQuery) {
		this.idQuery = idQuery;
	}

}
