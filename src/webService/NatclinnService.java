package webService;

import java.util.ArrayList;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;

import natclinn.util.NatclinnUtil;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.GenericEntity;

import queriesForWebService.NatclinnQueryProducts;

@Path("/products")
public class NatclinnService {
	
	@GET
	@Path("/list")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getProducts() throws Exception {
		ArrayList<Product> listProducts = new ArrayList<Product>();
		ResultSet resultSet = null;
		try {
			System.out.println("Starting getProducts() method...");
			resultSet = NatclinnQueryProducts.productsList("fr");
			System.out.println("Query executed successfully");
		} catch (Exception e) {
			System.err.println("Error in getProducts(): " + e.getMessage());
			e.printStackTrace();
			return Response.serverError()
				.entity("Error fetching products: " + e.getMessage())
				.build();
		}
		String id = null;
		String idOld = "firstTime";
	    String eAN = null;
	    ArrayList<String> listLabels = new ArrayList<String>();
		if (resultSet != null) {
			while (resultSet.hasNext()) {
				QuerySolution result = resultSet.next();
				System.out.println(result);
				
				if (idOld.contains("firstTime")) {
					idOld = result.get("id") != null ? result.get("id").toString() : "";
				}
				
				id = result.get("id") != null ? result.get("id").toString() : "";
	
				
				if (!id.contentEquals(idOld)) {
					Product product = new Product();
					product = createProduct(idOld,eAN,listLabels);
					listProducts.add(product);
					idOld=id;
					eAN = result.get("eAN") != null ? result.get("eAN").toString() : "";
					listLabels.clear();
					if (result.get("prefLabel") != null) {
						listLabels.add(result.get("prefLabel").toString());
					}
				} else {
					eAN = result.get("eAN") != null ? result.get("eAN").toString() : "";
					if (result.get("prefLabel") != null) {
						listLabels.add(result.get("prefLabel").toString());
					}
				}
				
				if (!resultSet.hasNext()) {
					Product product = new Product();
					product = createProduct(idOld,eAN,listLabels);
					listProducts.add(product);
				}
				
			} 
		}
		System.out.println(listProducts);
		GenericEntity<ArrayList<Product>> entity = new GenericEntity<ArrayList<Product>>(listProducts) {};
		return Response.ok(entity, MediaType.APPLICATION_JSON).build();
	}  

	@GET
	@Path("/{id:.+}")
	@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
	public Response getProduct(@PathParam("id") String id) {
		System.out.println("getProduct called with id: " + id);
		String decodedId = id;
		
		// Skip hex decoding if the ID looks like a URL
		if (!id.startsWith("http://") && !id.startsWith("https://")) {
			try {
				// try the existing hex decoder (some clients encode the id)
				decodedId = NatclinnUtil.decodeHexToStringUTF8(id);
			} catch (Exception e) {
				// not hex-encoded, use raw id
				System.out.println("decodeHexToStringUTF8 skipped: " + e.getMessage());
				decodedId = id;
			}
		}

		// strip surrounding angle brackets if present
		if (decodedId.startsWith("<") && decodedId.endsWith(">")) {
			decodedId = decodedId.substring(1, decodedId.length() - 1);
		}

		try {
			// Ensure configuration and model are initialized
			new natclinn.util.NatclinnConf();
			queriesForWebService.NatclinnSingletonInfModel.initModel();
			org.apache.jena.rdf.model.InfModel infModel = queriesForWebService.NatclinnSingletonInfModel.getModel();

			String prefix = natclinn.util.NatclinnConf.queryPrefix;
	    // Build a valid SPARQL query – do not leave a bare <id> token alone.
	    String stringQuery = prefix + "SELECT ?eAN ?prefLabel WHERE { "
		    + "OPTIONAL { <" + decodedId + "> ncl:hasEAN13 ?eAN } "
		    + "OPTIONAL { <" + decodedId + "> skos:prefLabel ?prefLabel } "
		    + "}";

			org.apache.jena.query.Query query = org.apache.jena.query.QueryFactory.create(stringQuery);
			org.apache.jena.query.QueryExecution qe = org.apache.jena.query.QueryExecutionFactory.create(query, infModel);
			org.apache.jena.query.ResultSet rs = qe.execSelect();

			String eAN = null;
			ArrayList<String> labels = new ArrayList<>();
			boolean found = false;
			while (rs.hasNext()) {
				found = true;
				org.apache.jena.query.QuerySolution sol = rs.next();
				if (sol.get("eAN") != null) {
					eAN = sol.get("eAN").toString();
				}
				if (sol.get("prefLabel") != null) {
					labels.add(sol.get("prefLabel").toString());
				}
			}
			qe.close();

			if (!found) {
				return Response.status(Response.Status.NOT_FOUND)
						.entity("Product not found for id: " + decodedId)
						.build();
			}

			Product product = createProduct(decodedId, eAN, labels);
			return Response.ok(product, MediaType.APPLICATION_JSON).build();

		} catch (Exception e) {
			e.printStackTrace();
			return Response.serverError().entity("Error retrieving product: " + e.getMessage()).build();
		}
	}
	
	@GET
	@Path("/search")
	@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
	public Response searchProduct(@jakarta.ws.rs.QueryParam("id") String id) {
		if (id == null || id.trim().isEmpty()) {
			return Response.status(Response.Status.BAD_REQUEST)
					.entity("Query parameter 'id' is required")
					.build();
		}

		try {
			// Decode URL-encoded characters
			String decodedId = java.net.URLDecoder.decode(id, "UTF-8");
			System.out.println("Decoded search id: " + decodedId);
			return getProduct(decodedId);
		} catch (java.io.UnsupportedEncodingException e) {
			return Response.serverError()
					.entity("Error decoding id parameter: " + e.getMessage())
					.build();
		}
	}
	
	public Product createProduct(String id, String eAN, ArrayList<String> listLabels) {
		Product product = new Product();
		product.setId(id);
		product.setEAN(eAN);
		product.setPrefLabels(listLabels);
		return product;
	}
		// Pour tester
	    // http://127.0.0.1:8080/NatclinnWebService/api_natclinn/products/list
		// http://localhost:8080/NatclinnWebService/api_natclinn/products/https://w3id.org/NCL/ontology/P-3250392814908
		// http://localhost:8080/NatclinnWebService/api_natclinn/products/search?id=https://w3id.org/NCL/ontology/P-3564700423196
}
