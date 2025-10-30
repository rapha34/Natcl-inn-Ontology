package queriesForWebService;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import natclinn.util.*;
import inferencesAndQueries.NatclinnCreateInferedModel;

public class NatclinnQueryCreationInfModel {
	
    public static void creationModel() throws Exception {
    	// Initialisation de la configuration
    			// Chemin d'accès, noms fichiers...
    			new NatclinnConf(); 
    			
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
						
    			// Récupération du nom du fichier contenant les paramètres.
    			Path pathOfTheParameters = Paths.get(NatclinnConf.mainFolderNatclinn, NatclinnConf.fileNameParameters);
    			// Récupération du top spatial
    			String topSpatial = NatclinnUtil.extractParameter(pathOfTheParameters.toString(), "topSpatial"); 	
    			//System.out.println("topSpatial : " + topSpatial);
    		
    	NatclinnSingletonInfModel.setModel(NatclinnCreateInferedModel.createInferedModel(listOntologiesFileName, listRulesFileName, listPrimitivesFileName, topSpatial));
	}
}