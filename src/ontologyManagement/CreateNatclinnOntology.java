package ontologyManagement;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.jena.ontology.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.DC;
import org.apache.jena.vocabulary.RDFS;
import natclinn.util.NatclinnConf;


public class CreateNatclinnOntology {

	public static <ValuesFromRestriction> void main( String[] args ) {
		
		new NatclinnConf(); 
		String folderForOntologies = NatclinnConf.folderForOntologies;
		String inputFileTbox = NatclinnConf.folderForOntologies + "/NatclinnTbox.xml";   
		String inputFileTboxMyChoice = NatclinnConf.folderForOntologies + "/MyChoiceTbox.xml";  
		String inputFileTboxAIF = NatclinnConf.folderForOntologies + "/AIFTbox.xml";  
		String inputFileAbox = NatclinnConf.folderForOntologies + "/NatclinnAbox.xml"; 
	 	

		// OWL_DL_MEM : Une spécification pour les modéles OWL DL qui sont stockés en mémoire 
		//  et qui ne nécessitent pas de raisonnement supplémentaire.
		OntModel om = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
		OntModel modelTemp = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
			
		String ncl = new String("https://w3id.org/Natclinn/ontology/");
		om.setNsPrefix("ncl", ncl);
		String mch = new String("https://w3id.org/MCH/ontology/");
	    om.setNsPrefix("mch", mch);
		String skos = new String("http://www.w3.org/2004/02/skos/core#");
	    om.setNsPrefix("skos", skos); 
		 
		Ontology ont = om.createOntology(ncl + "NatclinnOntology");
		om.add(ont, RDFS.label,"Ontology of Natclinn");
		om.add(ont, DC.description,"Ontology of Product");
		om.add(ont, DC.creator,"Raphaël CONDE SALAZAR");		
            
			StmtIterator stmtIterator = null;
			
			// use the class loader to find the input file
			InputStream inTbox = RDFDataMgr.open(inputFileTbox);
			if (inTbox == null) {
				throw new IllegalArgumentException( "File: " + inputFileTbox + " not found");
			}  
			// read the RDF/XML files
			modelTemp.read( inTbox, "" );
			// deletes the description of the ontology
			stmtIterator = modelTemp.listStatements(modelTemp.getResource(ncl + "NatclinnTbox"),(Property) null,(RDFNode) null);
			modelTemp.remove(stmtIterator);
			// merge the graphs
			om.add(modelTemp);
			modelTemp.removeAll();


			// use the class loader to find the input file
			InputStream inTboxMyChoice = RDFDataMgr.open(inputFileTboxMyChoice);
			if (inTboxMyChoice == null) {
				throw new IllegalArgumentException( "File: " + inputFileTboxMyChoice + " not found");
			}  
			// read the RDF/XML files
			modelTemp.read( inTboxMyChoice, "" );
			// deletes the description of the ontology
			stmtIterator = modelTemp.listStatements(modelTemp.getResource(mch + "MyChoiceTbox"),(Property) null,(RDFNode) null);
			modelTemp.remove(stmtIterator);
			// merge the graphs
			om.add(modelTemp);
			modelTemp.removeAll();

			// use the class loader to find the input file
			InputStream inTboxAIF = RDFDataMgr.open(inputFileTboxAIF);
			if (inTboxAIF == null) {
				throw new IllegalArgumentException( "File: " + inputFileTboxAIF + " not found");
			}  
			// read the RDF/XML files
			modelTemp.read( inTboxAIF, "" );
			// deletes the description of the ontology
			stmtIterator = modelTemp.listStatements(modelTemp.getResource(mch + "AIFTbox"),(Property) null,(RDFNode) null);
			modelTemp.remove(stmtIterator);
			// merge the graphs
			om.add(modelTemp);
			modelTemp.removeAll();


			// use the class loader to find the input file
			InputStream inAbox = RDFDataMgr.open(inputFileAbox);
			if (inAbox == null) {
				throw new IllegalArgumentException( "File: " + inputFileAbox + " not found");
			}  
			// read the RDF/XML files
			modelTemp.read( inAbox, "" );
			// deletes the description of the ontology
			stmtIterator = modelTemp.listStatements(modelTemp.getResource(ncl + "NatclinnAbox"),(Property) null,(RDFNode) null);
			modelTemp.remove(stmtIterator);
			// merge the graphs
			om.add(modelTemp);


	        //////////////////////////////
	        // Sorties fichiers         //
	        ////////////////////////////// 
	        
	        try {       
		    	  FileOutputStream outStream = new FileOutputStream(folderForOntologies + "/NatclinnOntology.owl");
		             // exporte le resultat dans un fichier
		             om.write(outStream, "RDF/XML");
		             
		             //FileOutputStream outStream2 = new FileOutputStream(folderForOntologies + "NatclinnOntology.ttl");
		             // exporte le resultat dans un fichier
		             //om.write(outStream2, "N3");
		     
		             // N3 N-TRIPLE RDF/XML RDF/XML-ABBREV
		             om.write(System.out, "N3");
		             
		             outStream.close();
		             //outStream2.close();
		    }
	        catch (FileNotFoundException e) {System.out.println("File not found");}
	        catch (IOException e) {System.out.println("IO problem");}

	}
}