package webService;

import java.util.ArrayList;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAccessType;

@XmlRootElement(name = "product")
@XmlAccessorType(XmlAccessType.FIELD)
public class Product {
	private String id;
	private String eAN;
    private ArrayList<String> prefLabels;

    public Product() { }

    public Product(String id, String eAN, ArrayList<String> prefLabels) {
        this.id = id;
        this.eAN = eAN;
        this.prefLabels = new ArrayList<String>(prefLabels);
    }

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public ArrayList<String> getPrefLabels() {
		return prefLabels;
	}

	public void setPrefLabels(ArrayList<String> prefLabels) {
		this.prefLabels = new ArrayList<String>(prefLabels);
	}

	public String getEAN() {
		return eAN;
	}

	public void setEAN(String eAN) {
		this.eAN = eAN;
	}
}
