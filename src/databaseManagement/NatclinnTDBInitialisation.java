package databaseManagement;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import natclinn.util.NatclinnConf;
import natclinn.util.NatclinnUtil;
import natclinn.util.TDBUtil;

public class NatclinnTDBInitialisation {

	public static void main(final String[] args) throws Exception {

		initialisation();

	}
	public static void initialisation() throws Exception {

		// Initialisation de la configuration
		// Chemin d'accès, noms fichiers...
		new NatclinnConf(); 

		// Récupération du nom du fichier contenant la liste des ontologies à traiter.
		Path pathOfTheListOntologies = Paths.get(NatclinnConf.mainFolderNatclinn, NatclinnConf.fileNameListOntologiesForInitTDB);
			
		// Récupération du nom du répertoire des ontologies à traiter dans la configuration
		Path pathOfTheDirectory = Paths.get(NatclinnConf.folderForOntologies);
	
		// Récupération du nom des fichiers d'ontologies dans listOntologiesFileName
		ArrayList<String> listOntologiesFileName = new ArrayList<String>();	
		listOntologiesFileName = NatclinnUtil.makeListFileName(pathOfTheListOntologies.toString()); 

		// Pour toutes les ontologies
		for (int i = 0; i < listOntologiesFileName.size(); i++) {
			String fileName = listOntologiesFileName.get(i);
			// Un nom pour la création du graphe TDB à partir du nom de l'ontologie
			String nameForGraphURI = fileName.replaceFirst("[.][^.]+$", "");
			
			System.out.println("The ontology " + fileName + " is being loaded");   
			
			try {			

				// Effacement des statements contenus dans le graphe TDB
				TDBUtil.DeleteTDBContent(nameForGraphURI);

				//lecture du fichier json	
				Path pathFileOntology = Paths.get(pathOfTheDirectory.toString() , fileName);
				String jsonString = NatclinnUtil.readFileAsString(pathFileOntology.toString());

				TDBUtil.InputJsonStringToTDB("JSON-LD", jsonString , nameForGraphURI);
				
				System.out.println("Graph size " + nameForGraphURI + " : " + TDBUtil.SizeOfTDBContent(nameForGraphURI));
				
			} 
			catch (Exception e) {System.out.println("big problem :" + e.toString());
			}	
		}   
		System.out.println("End of the transfer of ontologies to TDB");
	}

}




