package ontologyManagement;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
// utiliser le modele ontologique
import org.apache.jena.ontology.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.vocabulary.DC;
import org.apache.jena.vocabulary.RDFS;
import natclinn.util.NatclinnConf;


public class CreateAIFTbox {


	public static <ValuesFromRestriction> void main( String[] args ) {

		String jsonString = CreationTBox();
		
		OntModel om = ModelFactory.createOntologyModel();
		
		RDFParser.fromString(jsonString,Lang.JSONLD11).parse(om);

		try {   

			//////////////////////////////
			// Sorties fichiers         //
			////////////////////////////// 

			FileOutputStream outStream = new FileOutputStream(NatclinnConf.folderForOntologies + "/AIFTbox.xml");
			// exporte le resultat dans un fichier
			om.write(outStream, "RDF/XML");

			// N3 N-TRIPLE RDF/XML RDF/XML-ABBREV
			om.write(System.out, "N3");

			outStream.close();
		}
		catch (FileNotFoundException e) {System.out.println("File not found");}
		catch (IOException e) {System.out.println("IO problem");}
	}

	public static String CreationTBox() {

		String jsonString = null;

		// Initialisation de la configuration
		// Chemin d'accès, noms fichiers...	
		new NatclinnConf();  

		OntModel om = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
		
	    ///////////////////////////////
	    //Définition des namespaces  //
	    ///////////////////////////////
	    
		String aif = new String("http://www.arg.dundee.ac.uk/aif#");
	    om.setNsPrefix("aif", aif);
	    String dcat = new String("http://www.w3.org/ns/dcat#/");
	    om.setNsPrefix("dcat", dcat);
		String prov = new String("http://www.w3.org/ns/prov#");
	    om.setNsPrefix("prov", prov);
		String dct = new String("http://purl.org/dc/terms/"); 
	    om.setNsPrefix("dct", dct);
		String skos = new String("http://www.w3.org/2004/02/skos/core#");
	    om.setNsPrefix("skos", skos); 
	    String foaf = new String("http://xmlns.com/foaf/0.1/");
	    om.setNsPrefix("foaf", foaf);
	    String rdfs = new String("http://www.w3.org/2000/01/rdf-schema#");
	    om.setNsPrefix("rdfs", rdfs);

		/////////////////////////////////////
	    //Description de l'ontologie       //
	    /////////////////////////////////////
	    
	    Ontology ont = om.createOntology(aif + "AIFTbox");
		om.add(ont, RDFS.label,"Model Ontology of AIF", "en");
		om.add(ont, DC.description,"Tbox for the AIF ontology");
		om.add(ont, DC.creator,"Pierre BISQUERT");	
		om.add(ont, DC.creator,"Raphaël CONDE SALAZAR");	
		om.add(ont, DC.creator,"Rallou THOMOPOULOS");	
		om.add(ont, RDFS.comment,"Ontologie AIF", "fr");


	    //////////////////////////////////////////////////////////////////////
	    //														            //
	    //				TBOX = terminological box				            //				
	    //	ensemble de formules relatives aux informations terminologiques	//
	    //	(i.e. notions de bases et relations entre elles)                //
	    //       													        //
	    //////////////////////////////////////////////////////////////////////

//////////////////////////////////////////////////
//Axiomisation des propriétés et des relations	//
//terminologie DL: concepts et rôles	        // 
//////////////////////////////////////////////////

	    ////////////////////////////////////////////
	    // Définition des concepts atomiques      //
	    ////////////////////////////////////////////		

	    // AIF est l'ensemble des classes et propriétés de l'ontologie AIF
	    OntClass AIF = om.createClass(aif + "AIF");
	    AIF.addComment("AIF is the set of product and arguments pour My Choice.", "en");
		AIF.addComment("AIF est l'ensemble des produits et des arguments pour My Choice.", "fr");
	    
		// Pour les arguments avec AIF

		// Abstract class Node
        OntClass Node = om.createClass(aif + "Node");
        Node.addComment("Abstract class for argumentation nodes.", "en");
		Node.addComment("Un nœud dans un graphe d'argumentation.", "fr");

        // Subclasses of Node
        OntClass INode = om.createClass(aif + "I-Node");
        INode.addSuperClass(Node);
        INode.addComment("Information node (assertion, statement, etc.)", "en");
		INode.addComment("Un nœud d'information dans un graphe d'argumentation.", "fr");


        OntClass SNode = om.createClass(aif + "S-Node");
        SNode.addSuperClass(Node);
        SNode.addComment("Scheme node (inference, attack, preference).", "en");
		SNode.addComment("Un nœud de schéma dans un graphe d'argumentation.", "fr");

        // Subclasses of S-Node
        OntClass RANode = om.createClass(aif + "RA-Node");
        RANode.addSuperClass(SNode);
        RANode.addComment("Rule of inference node (reasoning scheme).", "en");
		RANode.addComment("Règle du nœud d'inférence (schéma de raisonnement).", "fr");


        OntClass CANode = om.createClass(aif + "CA-Node");
        CANode.addSuperClass(SNode);
        CANode.addComment("Conflict application node (attack between arguments).", "en");
		CANode.addComment("Conflit nœud d'application (attaque entre arguments).", "fr");

        OntClass MANode = om.createClass(aif + "MA-Node");
        MANode.addSuperClass(SNode);
        MANode.addComment("Preference application node (expresses preferences).", "en");
		MANode.addComment("Nœud d'application des préférences (exprime les préférences).", "fr");
        
		
		
	    ////////////////////////////////////////////
	    // Définition des disjonctions de classes //
	    ////////////////////////////////////////////

	    // add disjoint individuals axiom assertion:
	   
	    // Disjoint entre INode et SNode (leurs sous-classes hériteront de cette disjonction)
        INode.addDisjointWith(SNode);

        // Disjoint entre les sous-classes directes de SNode
        RANode.addDisjointWith(CANode);
        RANode.addDisjointWith(MANode);
        CANode.addDisjointWith(MANode);

	    //////////////////////////////////////////////////////////
	    // Définition des object property                       //
	    //////////////////////////////////////////////////////////
		
	    // Property: hasPremise (S-Node → I-Node)
        ObjectProperty hasPremise = om.createObjectProperty(aif + "hasPremise");
        hasPremise.addDomain(SNode);
        hasPremise.addRange(INode);
        hasPremise.addComment("Links a scheme node to its premises (I-Nodes).", "en");
		hasPremise.addComment("Relie un nœud de schéma à ses prémisses (I-Nodes).", "fr");

        // Property: hasConclusion (S-Node → I-Node)
        ObjectProperty hasConclusion = om.createObjectProperty(aif + "hasConclusion");
        hasConclusion.addDomain(SNode);
        hasConclusion.addRange(INode);
        hasConclusion.addComment("Links a scheme node to its conclusion.", "en");
		hasConclusion.addComment("Relie un nœud de schéma à sa conclusion.", "fr");


	    
	    //////////////////////////////////////////////////////////
	    // Définition des data property                         //
	    //////////////////////////////////////////////////////////
	   
	
		//////////////////////////////////////////////////////////
	    // Définition des annotation property                   //
	    //////////////////////////////////////////////////////////
		om.createAnnotationProperty(skos + "prefLabel");
	    om.createAnnotationProperty(skos + "altLabel");
		om.createAnnotationProperty(skos + "definition");
		om.createAnnotationProperty(rdfs + "label");
		om.createAnnotationProperty(rdfs + "comment");
		om.createAnnotationProperty(dct + "created");
//////////////////////////////////////////////////////////
// Imchusion/Equivalence de concepts                    //
//////////////////////////////////////////////////////////


	/////////////////////////////
	// Imchusion de concepts   //
	/////////////////////////////

		// AIF est l'ensemble de tous les concepts définis
		AIF.addSubClass(Node);

		// exporte le resultat dans un fichier au format RDF/JSON
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		RDFDataMgr.write(out, om, RDFFormat.JSONLD11);
		try {
			jsonString = out.toString("UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		return jsonString;
	}
}