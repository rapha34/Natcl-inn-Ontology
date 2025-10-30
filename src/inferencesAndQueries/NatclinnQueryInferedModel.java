package inferencesAndQueries;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.update.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import natclinn.util.NatclinnConf;
import natclinn.util.NatclinnQueryObject;
import natclinn.util.NatclinnQueryOutputObject;

/**
 * Classe utilitaire permettant d'exécuter une liste de requêtes SPARQL
 * sur un modèle inféré Jena (InfModel).
 */
public class NatclinnQueryInferedModel {

    /**
     * Exécute des fichiers de requêtes JSON sur le modèle inféré.
     * Chaque fichier contient une liste de NatclinnQueryObject.
     */
    public static ArrayList<NatclinnQueryOutputObject> queryInferedModel(
            ArrayList<String> listQueriesFileName, InfModel infModel)
            throws IOException {

        System.out.println("Model size before inference: " + infModel.size());

        ArrayList<NatclinnQueryObject> listQueries = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();

        // Chargement de toutes les requêtes
        for (String queriesFilename : listQueriesFileName) {
            Path pathOfTheListQueries = Paths.get(NatclinnConf.folderForQueries, queriesFilename);
            ArrayList<NatclinnQueryObject> listQueriesTemp = objectMapper.readValue(
                    new File(pathOfTheListQueries.toString()),
                    new TypeReference<ArrayList<NatclinnQueryObject>>() {});
            listQueries.addAll(listQueriesTemp);
        }

        // Exécution des requêtes
        ArrayList<NatclinnQueryOutputObject> listQueriesOutputs = new ArrayList<>();

        for (NatclinnQueryObject objectQuery : listQueries) {

            NatclinnQueryOutputObject queryOutput = new NatclinnQueryOutputObject(null, "{}");
            queryOutput.setQuery(objectQuery);

            String queryString = objectQuery.getStringQuery();

            if (queryString != null && !queryString.trim().isEmpty()) {

                String type = objectQuery.getTypeQuery();

                try {
                    if ("INSERT".equalsIgnoreCase(type) || "UPDATE".equalsIgnoreCase(type)) {
                        UpdateRequest update = UpdateFactory.create(queryString);
                        UpdateAction.execute(update, infModel);

                    } else if ("SELECT".equalsIgnoreCase(type)) {
                        Query query = QueryFactory.create(queryString);
                        try (QueryExecution qe = QueryExecutionFactory.create(query, infModel)) {
                            ResultSet results = qe.execSelect();
                            ByteArrayOutputStream os = new ByteArrayOutputStream();
                            ResultSetFormatter.outputAsJSON(os, results);
                            queryOutput.setQueryResponse(os.toString("UTF-8"));
                        }
                    } else {
                        System.out.println("Type de requête inconnu : " + type);
                    }

                } catch (Exception e) {
                    System.err.println("Erreur lors de l'exécution de la requête : " + objectQuery.getTitleQuery());
                    e.printStackTrace();
                }
            }

            listQueriesOutputs.add(queryOutput);
        }

        System.out.println("Model size after inference: " + infModel.size());
        return listQueriesOutputs;
    }

    /**
     * Variante : on injecte directement la liste de requêtes.
     */
    public static ArrayList<NatclinnQueryOutputObject> queryInferedModel(
            InfModel infModel, ArrayList<NatclinnQueryObject> listQueries) {

        System.out.println("Model size before inference: " + infModel.size());
        ArrayList<NatclinnQueryOutputObject> outputs = new ArrayList<>();

        for (NatclinnQueryObject objectQuery : listQueries) {
            NatclinnQueryOutputObject output = new NatclinnQueryOutputObject(objectQuery, "{}");
            String queryString = objectQuery.getStringQuery();
            // System.err.println("Executing query: " + queryString);
            if (queryString != null && !queryString.trim().isEmpty()) {

                String type = objectQuery.getTypeQuery();

				if (objectQuery.getTitleQuery() != null && !objectQuery.getTitleQuery().isEmpty()) {
                	System.out.println("\n=== " + objectQuery.getTitleQuery() + " ===");
				}

                try {
                    if ("INSERT".equalsIgnoreCase(type) || "UPDATE".equalsIgnoreCase(type)) {
                        UpdateRequest update = UpdateFactory.create(queryString);
                        UpdateAction.execute(update, infModel);
                    } else if ("SELECT".equalsIgnoreCase(type)) {
                        Query query = QueryFactory.create(queryString);
                        try (QueryExecution qe = QueryExecutionFactory.create(query, infModel)) {
                            ResultSet results = qe.execSelect();
                            ResultSetFormatter.out(System.out, results);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Erreur dans la requête : " + objectQuery.getTitleQuery());
                    e.printStackTrace();
                }
            } else {

				System.out.println("\n=== " + objectQuery.getTitleQuery() + " ===");
			}

            outputs.add(output);
        }

        System.out.println("Model size after inference: " + infModel.size());
        return outputs;
    }
}
