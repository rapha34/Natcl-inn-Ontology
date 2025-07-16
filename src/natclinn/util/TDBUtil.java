package natclinn.util;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.jena.dboe.base.file.Location;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.tdb2.TDB2Factory;


public class TDBUtil {
	
	public static Dataset CreateTDBDataset() throws Exception {
    System.out.println("=== Début de createTDBDataset ===");

    // Étape 1 : Initialisation de la configuration
    new NatclinnConf();

    // Étape 2 : Vérification des paramètres
    if (NatclinnConf.folderForTDB == null || NatclinnConf.fileNameTDBdatabase == null) {
        throw new IllegalStateException("Configuration invalide : folderForTDB ou fileNameTDBdatabase est null.");
    }

    // Étape 3 : Création du chemin
    Path pathDataBase = Paths.get(NatclinnConf.folderForTDB, NatclinnConf.fileNameTDBdatabase);
    // System.out.println("Chemin base TDB2 : " + pathDataBase.toAbsolutePath());

    // Étape 4 : Vérification que le dossier existe (Jena TDB2 peut en créer un, mais on teste quand même)
    File dbDir = pathDataBase.toFile();
    if (!dbDir.exists()) {
        System.out.println("⚠️ Le dossier de la base n'existe pas encore. Il sera créé si nécessaire.");
    } else if (!dbDir.isDirectory()) {
        throw new IllegalStateException("Le chemin TDB2 n'est pas un dossier.");
    }

    // Étape 5 : Création de l'objet Dataset
    Location location = Location.create(pathDataBase.toString());
    Dataset dataset = TDB2Factory.connectDataset(location);
    // System.out.println("Dataset créé avec succès.");

    // Étape 6 : Test rapide d'opération
    dataset.begin(ReadWrite.READ);
    try {
        //System.out.println("Graph par défaut : " + dataset.getDefaultModel().size() + " triplets.");
    } finally {
        dataset.end();
    }

    System.out.println("=== Fin de createTDBDataset ===");
    return dataset;
}


	
	public static void DeleteStatementToTDBGraph(List<Statement> listStmt, String graphURI) throws Exception {
		
		Dataset dataset = CreateTDBDataset();
		dataset.begin(ReadWrite.WRITE);
		try {   
			
			Model model = dataset.getNamedModel(graphURI);
			model.remove(listStmt);
			dataset.commit();    
			model.close();
		}
		finally { dataset.end() ; }
	}
	
	
	public static void InputInputStreamContentToTDB(String serializationType, InputStream inputStream) throws Exception {
		try {   
			Dataset dataset = CreateTDBDataset();
			dataset.begin(ReadWrite.WRITE);
			Model model = dataset.getDefaultModel();
			// Read inputStream and put it in temporary model
			Model modelTemp = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);    
			modelTemp.read( inputStream, serializationType );
			// Add temporary model to model
			model.add(modelTemp);
			dataset.commit();    
			dataset.close();
			modelTemp.close();
		}
        catch (IOException e) {System.out.println("IO problem");}
	}
	
	public static void InputInputStreamContentToTDB(String serializationType, InputStream inputStream, String graphURI) throws Exception {
		try {   
			Dataset dataset = CreateTDBDataset();
			dataset.begin(ReadWrite.WRITE);
			Model model = dataset.getNamedModel(graphURI);
			// Read JSON File and put it in temporary model
			Model modelTemp = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);    
			// read the RDF/JSON files in temporary model
			modelTemp.read(inputStream, serializationType );
			// Add temporary model to model
			model.add(modelTemp);
			dataset.commit();    
			dataset.close();
			modelTemp.close();
		}
		catch (FileNotFoundException e) {System.out.println("File not found");}
        catch (IOException e) {System.out.println("IO problem");}
	}
	
	
	
	public static void InputJsonStringToTDB(String serializationType, String inJsonString) throws Exception {
		try {   
			Dataset dataset = CreateTDBDataset();
			dataset.begin(ReadWrite.WRITE);
			Model model = dataset.getDefaultModel();
			// Read JSON File and put it in temporary model
			Model modelTemp = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);    
			StringReader in = new StringReader(inJsonString);
			// read the RDF/JSON in temporary model
			modelTemp.read(in, null, serializationType);
			// Add temporary model to model
			model.add(modelTemp);
			dataset.commit();    
			dataset.close();
			modelTemp.close();
		}
		catch (FileNotFoundException e) {System.out.println("File not found");}
        catch (IOException e) {System.out.println("IO problem");}
	}
	
	
	public static void InputJsonStringToTDB(String serializationType, String inJsonString, String graphURI) throws Exception {
		try {   
			Dataset dataset = CreateTDBDataset();
			dataset.begin(ReadWrite.WRITE);
			Model model = dataset.getNamedModel(graphURI);
			// Read JSON File and put it in temporary model
			Model modelTemp = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);    
			StringReader in = new StringReader(inJsonString);
			// read the RDF/JSON in temporary model
			modelTemp.read(in, null, serializationType);
			// Add temporary model to model
			model.add(modelTemp);
			dataset.commit();    
			dataset.close();
			modelTemp.close();
		}
		catch (FileNotFoundException e) {System.out.println("File not found");}
        catch (IOException e) {System.out.println("IO problem");}
	}
	
	public static void InputFileContentToTDB(String serializationType, String inFile) throws Exception {
		try {   
			Dataset dataset = CreateTDBDataset();
			dataset.begin(ReadWrite.WRITE);
			Model model = dataset.getDefaultModel();
			// Read JSON File and put it in temporary model
			Model modelTemp = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);    
			// read the RDF/JSON files in temporary model
			modelTemp.read( inFile, serializationType );
			// Add temporary model to model
			model.add(modelTemp);
			dataset.commit();    
			dataset.close();
			modelTemp.close();
		}
		catch (FileNotFoundException e) {System.out.println("File not found");}
        catch (IOException e) {System.out.println("IO problem");}
	}
	
	
	public static void InputFileContentToTDB(String serializationType, String inFile, String graphURI) throws Exception {
		try {   
			Dataset dataset = CreateTDBDataset();
			dataset.begin(ReadWrite.WRITE);
			Model model = dataset.getNamedModel(graphURI);
			// Read JSON File and put it in temporary model
			Model modelTemp = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);    
			// read the RDF/JSON files in temporary model
			modelTemp.read( inFile, serializationType );
			// Add temporary model to model
			model.add(modelTemp);
			dataset.commit();    
			dataset.close();
			modelTemp.close();
		}
		catch (FileNotFoundException e) {System.out.println("File not found");}
        catch (IOException e) {System.out.println("IO problem");}
	}
	
	public static void InputModelToTDB(Model modelIn, String graphURI) throws Exception {
		try {   
			Dataset dataset = CreateTDBDataset();
			dataset.begin(ReadWrite.WRITE);
			Model model = dataset.getNamedModel(graphURI);
			// Add modelIn to model
			model.add(modelIn);
			dataset.commit();    
			dataset.close();
		}
		catch (FileNotFoundException e) {System.out.println("File not found");}
        catch (IOException e) {System.out.println("IO problem");}
	}
	
	public static void OutputOfTDBContentToConsole(String serializationType) throws Exception {

		Dataset dataset = CreateTDBDataset();
		// Create transaction for writing
		dataset.begin(ReadWrite.READ);
		Model model = dataset.getDefaultModel();
		// Sortie console N3 N-TRIPLE RDF/XML RDF/XML-ABBREV
		model.write(System.out, serializationType);
		dataset.commit();    
		dataset.close();
	}
	
	public static void OutputOfTDBContentToConsole(String serializationType, String graphURI) throws Exception {

		Dataset dataset = CreateTDBDataset();
		// Create transaction for writing
		dataset.begin(ReadWrite.READ);
		Model model = dataset.getNamedModel(graphURI);
		// Sortie console N3 N-TRIPLE RDF/XML RDF/XML-ABBREV
		model.write(System.out, serializationType);
		dataset.commit();    
		dataset.close();
	}
	
	public static void OutputOfTDBContentToFile(String serializationType, String outFile) throws Exception {

		try {   
			Dataset dataset = CreateTDBDataset();
			dataset.begin(ReadWrite.READ);
			Model model = dataset.getDefaultModel();
			// Sortie fichier  
			FileOutputStream outStream = new FileOutputStream(outFile);
			// exporte le resultat dans un fichier
			model.write(outStream, serializationType);
			outStream.close();
			dataset.commit(); 
			dataset.close();
		}
		catch (FileNotFoundException e) {System.out.println("File not found");}
        catch (IOException e) {System.out.println("IO problem");}
	}
	
	public static void OutputOfTDBContentToFile(String serializationType, String outFile, String graphURI) throws Exception {
		
		Dataset dataset = CreateTDBDataset();
		dataset.begin(ReadWrite.READ);
		
		try { 
			
			Model model = dataset.getNamedModel(graphURI);
			// Sortie fichier 
			FileOutputStream outStream = new FileOutputStream(outFile);
			// exporte le resultat dans un fichier
			model.write(outStream, serializationType);
			dataset.commit(); 
			outStream.close();   
			model.close();
		}
		finally { 
			dataset.end() ; 
		}
	}
	
	
	public static long SizeOfTDBContent() throws Exception {
		long sizeOfTDB = 0;
		try {
			Dataset dataset = CreateTDBDataset();
			dataset.begin(ReadWrite.READ);
			Model model = dataset.getDefaultModel();
			sizeOfTDB = model.size();
			dataset.commit();    
			dataset.close();
		}
		catch (FileNotFoundException e) {System.out.println("File not found");}
        catch (IOException e) {System.out.println("IO problem");}
		
		return sizeOfTDB;	
	}
	
	public static long SizeOfTDBContent(String graphURI) throws Exception {
		long sizeOfTDB = 0;
		try {
			Dataset dataset = CreateTDBDataset();
			dataset.begin(ReadWrite.READ);
			Model model = dataset.getNamedModel(graphURI);
			sizeOfTDB = model.size();
			dataset.commit();    
		    dataset.close();
		}
		catch (FileNotFoundException e) {System.out.println("File not found");}
        catch (IOException e) {System.out.println("IO problem");}
		
		return sizeOfTDB;
		
	}
	
	
    public static void PrintAllSizeOfTDBContent(ArrayList<String> listGraphURI) throws Exception {
		
		System.out.println("Taille des différents graphes de TDB : " );
		// Taille du graphe par d�faut de TDB 
		System.out.println("- Taille du graphe par défaut de TDB : " + SizeOfTDBContent());
		
		// Taille des autres graphes de TDB donn�s par la liste 
		for (int i = 0; i < listGraphURI.size(); i++) {
		System.out.println("- Taille du graphe " + listGraphURI.get(i) + " de TDB : " + TDBUtil.SizeOfTDBContent(listGraphURI.get(i)));
		}
	}
	
	public static void DeleteTDBContent() throws Exception {
		try { 
		Dataset dataset = CreateTDBDataset();
		// Create transaction for writing
		dataset.begin(ReadWrite.WRITE);
		Model model = dataset.getDefaultModel();
		model.removeAll();
		model.clearNsPrefixMap();
		dataset.commit();
		dataset.close();
		}
		catch (Exception e) {System.out.println("problem" + e );}
	}
	
	public static void DeleteTDBContent(String graphURI) throws Exception {
		Dataset dataset = CreateTDBDataset();
		// Create transaction for writing
		dataset.begin(ReadWrite.WRITE);
		try { 
			Model model = dataset.getNamedModel(graphURI);
			model.removeAll();
			model.clearNsPrefixMap();
			dataset.commit();
		}
		finally {dataset.end();
		}
	}
	
	public static void DeleteTDBContent(ArrayList<String> listGraphURI) throws Exception {
		try { 
			Dataset dataset = CreateTDBDataset();
			// Create transaction for writing
			dataset.begin(ReadWrite.WRITE);
			Model model = dataset.getDefaultModel();
			model.removeAll();
			model.clearNsPrefixMap();
			
			for (int i = 0; i < listGraphURI.size(); i++) {
			    model = dataset.getNamedModel(listGraphURI.get(i));
				model.removeAll();
				model.clearNsPrefixMap();
			}
			dataset.commit();
			model.close(); 
			dataset.close();
		}
		catch (Exception e) {System.out.println("problem" + e );}
	}

	public static void DifferencesBetween2Graphs(String graphURI1, String graphURI2, String resultGraphURI) throws Exception {
		Dataset dataset = CreateTDBDataset();
		dataset.begin(ReadWrite.WRITE);	
		try {   
			Model model1 = dataset.getNamedModel(graphURI1);
			Model model2 = dataset.getNamedModel(graphURI2);
			Model resultModel = dataset.getNamedModel(resultGraphURI);
			  
			resultModel.add(model1.difference(model2));
			
			dataset.commit();
			model1.close();
			model2.close();
			resultModel.close();
		}
		finally { 
			dataset.end() ; 
		}
	}
	

}