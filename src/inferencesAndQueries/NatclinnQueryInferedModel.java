package inferencesAndQueries;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.update.UpdateAction;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import natclinn.util.NatclinnConf;
import natclinn.util.NatclinnQueryObject;
import natclinn.util.NatclinnQueryOutputObject;

public class NatclinnQueryInferedModel {
	
	// On passe par les fichiers du serveur en fournissant une liste de noms de fichier
	public static ArrayList<NatclinnQueryOutputObject> queryInferedModel(ArrayList<String> listQueriesFileName, InfModel infModel) throws JsonParseException, JsonMappingException, IOException {

		System.out.println("Model size before inference:" + infModel.size());

		ArrayList<NatclinnQueryObject> listQueries = new ArrayList<NatclinnQueryObject>();
		ArrayList<NatclinnQueryObject> listQueriesTemp;	
		ArrayList<NatclinnQueryOutputObject> listQueriesOutputs = new ArrayList<NatclinnQueryOutputObject>();
		
		// On recupére les fichiers de requêtes sur le serveur
		// et on charge listQueries
		ObjectMapper objectMapper = new ObjectMapper();
		
		for(String QueriesFilename: listQueriesFileName){
			// Récupération du nom du fichier contenant les requêtes.
			Path pathOfTheListQueries = Paths.get(NatclinnConf.folderForQueries, QueriesFilename);
			//JSON from file to Object
			listQueriesTemp = objectMapper.readValue(new File(pathOfTheListQueries.toString()), new TypeReference<ArrayList<NatclinnQueryObject>>(){});
			listQueries.addAll(listQueriesTemp); 
		}
		
		for(NatclinnQueryObject objectQuery: listQueries){
			
			// Sauvegarde résultat
			NatclinnQueryOutputObject QueryOutput = new NatclinnQueryOutputObject(null, "{}");
			QueryOutput.setQuery(objectQuery);
			
			if (!(objectQuery.getStringQuery().equals(""))) {
				if (objectQuery.getTypeQuery().equalsIgnoreCase("INSERT")) {
					UpdateRequest update = UpdateFactory.create(objectQuery.getStringQuery());
					UpdateAction.execute(update, infModel);
				}
				if (objectQuery.getTypeQuery().equalsIgnoreCase("SELECT")) {	
					Query query = QueryFactory.create(objectQuery.getStringQuery());
					QueryExecution qe = QueryExecutionFactory.create(query, infModel);		
					ResultSet results = qe.execSelect();
					ByteArrayOutputStream os = new ByteArrayOutputStream();
		   			ResultSetFormatter.outputAsJSON(os, results);
					QueryOutput.setQueryResponse(os.toString("UTF-8"));		
				}
			}
			listQueriesOutputs.add(QueryOutput);
		}

		System.out.println("Model size after inference:" + infModel.size());

		return listQueriesOutputs;	

	}

	
	// Ici on injecte un liste de requêtes en entrée
    public static ArrayList<NatclinnQueryOutputObject> queryInferedModel(InfModel infModel, ArrayList<NatclinnQueryObject> listQueries) {
    	
    	System.out.println("Model size before inference:" + infModel.size());
    	
    	for(NatclinnQueryObject objectQuery: listQueries){
    		
    		if (objectQuery.getTitleQuery().length()>0) {
    			System.out.println();
    			for (int c = 0; c < objectQuery.getTitleQuery().length()+6; c++)
    				System.out.print("=");
    			System.out.println();
    			System.out.println("|  " + objectQuery.getTitleQuery() + "  |");
    			for (int c = 0; c < objectQuery.getTitleQuery().length()+6; c++)
    				System.out.print("=");
    			System.out.println();
    		}	else {
    			System.out.println();
    		}
    			
    		//System.out.println(stringQuery);
    		if (!(objectQuery.getStringQuery() == "")) {
    			if (objectQuery.getTypeQuery() == "INSERT") {
    				Query query = QueryFactory.create(objectQuery.getStringQuery());
    				QueryExecution qe = QueryExecutionFactory.create(query, infModel);		
    				ResultSet results = qe.execSelect();
    				ResultSetFormatter.out(System.out, results);
    			}
    			if (objectQuery.getTypeQuery() == "SELECT") {
    				Query query = QueryFactory.create(objectQuery.getStringQuery());
    				QueryExecution qe = QueryExecutionFactory.create(query, infModel);		
    				ResultSet results = qe.execSelect();
    				ResultSetFormatter.out(System.out, results);
    			}
    			if (objectQuery.getTypeQuery() == "UPDATE") {

    				UpdateRequest update = UpdateFactory.create(objectQuery.getStringQuery());
    				UpdateAction.execute(update, infModel);
    			}
    		}
    	}
        
    	System.out.println("Model size after inference:" + infModel.size());
		
    	return null;	
    
    }

	
	


}