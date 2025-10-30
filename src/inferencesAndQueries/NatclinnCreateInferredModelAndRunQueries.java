package inferencesAndQueries;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import org.apache.jena.rdf.model.InfModel;

import com.fasterxml.jackson.databind.ObjectMapper;

import natclinn.util.NatclinnConf;
import natclinn.util.NatclinnQueryObject;
import natclinn.util.NatclinnQueryOutputObject;
import natclinn.util.NatclinnUtil;

public class NatclinnCreateInferredModelAndRunQueries {
	
	public static void InferencesAndQuery() throws Exception {
	    
		ArrayList<NatclinnQueryOutputObject> listQueriesOutputs = new ArrayList<NatclinnQueryOutputObject>();
		
		// Initialisation de la configuration
		// Chemin d'accès, noms fichiers...
		new NatclinnConf(); 
		
		//System.out.println("mainFolderNatclinn : " + NatclinnConf.mainFolderNatclinn);
		//System.out.println("fileNameListOntologies : " + NatclinnConf.fileNameListOntologies);

		// Récupération du nom du fichier contenant la liste des ontologies à traiter.
		Path pathOfTheListOntologies = Paths.get(NatclinnConf.mainFolderNatclinn, NatclinnConf.fileNameListOntologies);					
		// Récupération du nom des fichiers d'ontologies dans listOntologiesFileName
		ArrayList<String> listOntologiesFileName = new ArrayList<String>();	
		listOntologiesFileName = NatclinnUtil.makeListFileName(pathOfTheListOntologies.toString()); 

		// Récupération du nom du fichier contenant la liste des règles à traiter.
		Path pathOfTheListRules = Paths.get(NatclinnConf.mainFolderNatclinn, NatclinnConf.fileNameListRules);
		// Récupération du nom des fichiers des régles dans listRulesFileName
		ArrayList<String> listRulesFileName = new ArrayList<String>();	
		listRulesFileName = NatclinnUtil.makeListFileName(pathOfTheListRules.toString());
		
		// Récupération du nom du fichier contenant la liste des primitives à traiter.
		Path pathOfTheListPrimitives = Paths.get(NatclinnConf.mainFolderNatclinn, NatclinnConf.fileNameListPrimitives);					
		// Récupération du nom des fichiers d'ontologies dans listOntologiesFileName
		ArrayList<String> listPrimitivesFileName = new ArrayList<String>();	
		listPrimitivesFileName = NatclinnUtil.makeListFileName(pathOfTheListPrimitives .toString());
		
		// Récupération du nom du fichier contenant la liste des requêtes à traiter.
		Path pathOfTheListQueries = Paths.get(NatclinnConf.mainFolderNatclinn, NatclinnConf.fileNameListQueries);
		// Récupération du nom des fichiers des régles dans listQueriesFileName
		ArrayList<String> listQueriesFileName = new ArrayList<String>();	
		listQueriesFileName = NatclinnUtil.makeListFileName(pathOfTheListQueries.toString());
		
		// Récupération du nom du fichier pour les résultat.
		Path pathOfTheFileResults = Paths.get(NatclinnConf.mainFolderNatclinn, NatclinnConf.fileNameResultsQueries);
		// Récupération du nom du fichier des résultat dans fileNameResults
		String fileNameResults = NatclinnUtil.makeFileName(pathOfTheFileResults.toString()); 
		
		// Récupération du nom du fichier contenant les paramètres.
		Path pathOfTheParameters = Paths.get(NatclinnConf.mainFolderNatclinn, NatclinnConf.fileNameParameters);
		// Récupération du top spatial
		String topSpatial = NatclinnUtil.extractParameter(pathOfTheParameters.toString(), "topSpatial"); 	
		//System.out.println("topSpatial : " + topSpatial);
		
		// Création du model inféré
		InfModel infModel = NatclinnCreateInferedModel.createInferedModel(listOntologiesFileName, listRulesFileName, listPrimitivesFileName, topSpatial);
		// Execution des requêtes sur le modèle inféré
		listQueriesOutputs = NatclinnQueryInferedModel.queryInferedModel(listQueriesFileName, infModel);
	    
		// Sauvegarde des résultats dans fichier JSON	
		Path pathOfTheResultsFile = Paths.get(NatclinnConf.folderForResults, fileNameResults);
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.writeValue(new File(pathOfTheResultsFile.toString()), listQueriesOutputs);
		
	}
	
	public static void InferencesAndQuery(ArrayList<String> listOntologies, ArrayList<String> listRules, ArrayList<String> listPrimitives, String topSpatial, ArrayList<NatclinnQueryObject> listQueries) throws Exception {
	    InfModel infModel = NatclinnCreateInferedModel.createInferedModel(listOntologies, listRules, listPrimitives, topSpatial);
	    NatclinnQueryInferedModel.queryInferedModel(infModel, listQueries);
	}

}