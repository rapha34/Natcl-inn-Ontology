package queriesForWebService;

import java.time.Duration;
import java.time.Instant;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.InfModel;

import natclinn.util.NatclinnConf;


public class NatclinnQueryProducts {

	public static void main(String[] args) throws Exception {
		Instant start = Instant.now();
		// Initialisation de la configuration
		// Chemin d'accès, noms fichiers...
		new NatclinnConf();
		String UserLanguage = NatclinnConf.preferredLanguage;
		
		NatclinnQueryCreationInfModel.creationModel();
		
		ResultSet resultSet = productsList(UserLanguage);
		
		Instant end2 = Instant.now();
		System.out.println("Durée d'exécution : " + Duration.between(start, end2).getSeconds() + " secondes");
		ResultSetFormatter.out(System.out, resultSet);
	}

	public static ResultSet productsList(String UserLanguage) throws Exception {
		System.out.println("Starting productsList method...");
		try {
			// Initialisation de la configuration
			// Chemin d'accès, noms fichiers...
			new NatclinnConf();
			String prefix = NatclinnConf.queryPrefix;
			System.out.println("Configuration initialized with prefix: " + prefix);

			////////////////////////////////
			// Recupèration des parcelles //
			////////////////////////////////
			
			// Initialize model if not already done
			try {
				NatclinnQueryCreationInfModel.creationModel();
				System.out.println("Model initialized");
			} catch (Exception e) {
				System.err.println("Warning: Model initialization error (might be already initialized): " + e.getMessage());
			}
			
			InfModel infModel = NatclinnSingletonInfModel.getModel();
			if (infModel == null) {
				throw new Exception("InfModel is null - Model needs to be initialized first");
			}
			System.out.println("InfModel retrieved successfully");
			
			String stringQuery = prefix + "SELECT DISTINCT ?id ?eAN ?prefLabel WHERE { " +
				"?id rdf:type ncl:Product. " +	
				"?id ncl:isProductIAA 'true'^^xsd:boolean. " +
				"OPTIONAL { ?id ncl:hasEAN13 ?eAN } . " +
				"OPTIONAL { ?id skos:prefLabel ?prefLabel . } " +
				"}" +
				" ORDER BY ?id ";
			System.out.println("Executing query: " + stringQuery);
			Query query = QueryFactory.create(stringQuery);
			QueryExecution qe = QueryExecutionFactory.create(query, infModel);		
			ResultSet resultSet = qe.execSelect();
			System.out.println("Query executed successfully");
			return resultSet;
		} catch (Exception e) {
			System.err.println("Error in productsList: " + e.getMessage());
			e.printStackTrace();
			throw e;
		}
	}
	
	
	protected static void showModelSize( InfModel m ) {
        System.out.println( String.format( "The model contains %d triples", m.size() ) );
    }
}