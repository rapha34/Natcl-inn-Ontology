package ontologyManagement;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.jena.ontology.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.query.*;
import org.apache.jena.vocabulary.DC;
import org.apache.jena.vocabulary.RDFS;

import inferencesAndQueries.NatclinnCreateInferedModel;
import natclinn.util.NatclinnConf;
import natclinn.util.NatclinnUtil;

/**
 * Crée des projets MyChoice à partir des produits Natclinn et de leurs arguments inférés.
 * 
 * Ce programme :
 * 1. Charge le modèle inféré avec les règles product-argument
 * 2. Récupère les produits et leurs arguments liés (via ncl:hasArgument)
 * 3. Crée un projet MyChoice (mch:Project) pour chaque groupe de produits
 * 4. Convertit chaque produit en alternative (mch:Alternative)
 * 5. Associe les arguments Natclinn aux alternatives MyChoice
 * 
 * @author Natclinn
 */
public class CreateMychoiceProjectFromProducts {

    public static void main(String[] args) {
        try {
            new NatclinnConf();
            
            System.out.println("=== Création de projets MyChoice à partir des produits Natclinn ===");
            
            // Création du modèle inféré avec les règles product-argument
            System.out.println("\n1. Chargement du modèle inféré...");
            InfModel infModel = loadInferredModel();
            
            // Création des projets MyChoice
            System.out.println("\n2. Création des projets MyChoice...");
            OntModel mychoiceModel = createMychoiceProjectsFromProducts(infModel);
            
            // Sauvegarde du modèle MyChoice
            System.out.println("\n3. Sauvegarde du modèle MyChoice...");
            saveModel(mychoiceModel, "MychoiceProjectsFromProducts.xml");
            
            System.out.println("\n=== Terminé avec succès ===");
            
        } catch (Exception e) {
            System.err.println("Erreur lors de la création des projets MyChoice : " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Crée des projets MyChoice à partir d'un modèle inféré déjà existant.
     * Cette méthode est appelée depuis NatclinnQueryStatistics pour réutiliser le modèle déjà chargé.
     * 
     * @param infModel Le modèle inféré contenant les produits et leurs arguments
     */
    public static void createFromInferredModel(InfModel infModel) {
        try {
            new NatclinnConf();
            
            System.out.println("\n=== Création de projets MyChoice depuis le modèle inféré ===");
            
            // Création des projets MyChoice
            System.out.println("Création des projets MyChoice...");
            OntModel mychoiceModel = createMychoiceProjectsFromProducts(infModel);
            
            // Sauvegarde du modèle MyChoice
            System.out.println("Sauvegarde du modèle MyChoice...");
            saveModel(mychoiceModel, "MychoiceProjectsFromProducts.xml");
            
            System.out.println("=== Projets MyChoice créés avec succès ===\n");
            
        } catch (Exception e) {
            System.err.println("Erreur lors de la création des projets MyChoice : " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Charge le modèle inféré avec les ontologies et règles Natclinn
     */
    private static InfModel loadInferredModel() throws Exception {
        // Récupération de la liste des ontologies
        Path pathListOntologies = Paths.get(NatclinnConf.mainFolderNatclinn, NatclinnConf.fileNameListOntologies);
        ArrayList<String> listOntologies = NatclinnUtil.makeListFileName(pathListOntologies.toString());
        
        // Récupération de la liste des règles
        Path pathListRules = Paths.get(NatclinnConf.mainFolderNatclinn, NatclinnConf.fileNameListRules);
        ArrayList<String> listRules = NatclinnUtil.makeListFileName(pathListRules.toString());
        
        // Récupération de la liste des primitives
        Path pathListPrimitives = Paths.get(NatclinnConf.mainFolderNatclinn, NatclinnConf.fileNameListPrimitives);
        ArrayList<String> listPrimitives = NatclinnUtil.makeListFileName(pathListPrimitives.toString());
        
        // Récupération du top spatial
        Path pathParameters = Paths.get(NatclinnConf.mainFolderNatclinn, NatclinnConf.fileNameParameters);
        String topSpatial = NatclinnUtil.extractParameter(pathParameters.toString(), "topSpatial");
        
        // Création du modèle inféré
        System.out.println("   - Chargement des ontologies : " + listOntologies.size() + " fichiers");
        System.out.println("   - Chargement des règles : " + listRules.size() + " fichiers");
        
        return NatclinnCreateInferedModel.createInferedModel(listOntologies, listRules, listPrimitives, topSpatial);
    }
    
    /**
     * Crée des projets MyChoice à partir des produits et arguments du modèle inféré
     */
    private static OntModel createMychoiceProjectsFromProducts(InfModel infModel) {
        // Création du modèle MyChoice
        OntModel mychoiceModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
        
        // Définition des namespaces
        String mch = "https://w3id.org/MCH/ontology/";
        mychoiceModel.setNsPrefix("mch", mch);
        String ncl = "https://w3id.org/NCL/ontology/";
        mychoiceModel.setNsPrefix("ncl", ncl);
        
        // Création de l'ontologie
        Ontology ont = mychoiceModel.createOntology(mch + "MyChoiceProjectsFromProducts");
        mychoiceModel.add(ont, RDFS.label, "Projets MyChoice générés depuis produits Natclinn");
        mychoiceModel.add(ont, DC.description, 
            "Projets MyChoice créés automatiquement à partir des produits Natclinn et leurs arguments inférés");
        
        // Définition des classes et propriétés MyChoice
        Resource Project = mychoiceModel.createResource(mch + "Project");
        Resource Alternative = mychoiceModel.createResource(mch + "Alternative");
        Resource Argument = mychoiceModel.createResource(mch + "Argument");
        
        DatatypeProperty projectName = mychoiceModel.createDatatypeProperty(mch + "projectName");
        DatatypeProperty projectDescription = mychoiceModel.createDatatypeProperty(mch + "projectDescription");
        DatatypeProperty alternativeName = mychoiceModel.createDatatypeProperty(mch + "alternativeName");
        DatatypeProperty alternativeDescription = mychoiceModel.createDatatypeProperty(mch + "alternativeDescription");
        DatatypeProperty assertionProp = mychoiceModel.createDatatypeProperty(mch + "assertion");
        DatatypeProperty explanationProp = mychoiceModel.createDatatypeProperty(mch + "explanation");
        
        ObjectProperty hasAlternative = mychoiceModel.createObjectProperty(mch + "hasAlternative");
        ObjectProperty hasArgument = mychoiceModel.createObjectProperty(mch + "hasArgument");
        ObjectProperty relatedToProduct = mychoiceModel.createObjectProperty(mch + "relatedToProduct");
        ObjectProperty relatedToArgument = mychoiceModel.createObjectProperty(mch + "relatedToArgument");
        
        // Requête SPARQL pour récupérer les produits avec leurs arguments
        String queryString = 
            "PREFIX ncl: <https://w3id.org/NCL/ontology/> \n" +
            "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n" +
            "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> \n" +
            "SELECT ?product ?productLabel ?argument ?argLabel ?argName ?assertion ?explanation \n" +
            "WHERE { \n" +
            "  ?product rdf:type ncl:Product . \n" +
            "  ?product skos:prefLabel ?productLabel . \n" +
            "  ?product ncl:hasArgument ?argument . \n" +
            "  ?argument rdf:type ncl:Argument . \n" +
            "  OPTIONAL { ?argument skos:prefLabel ?argLabel } \n" +
            "  OPTIONAL { ?argument ncl:nameProperty ?argName } \n" +
            "  OPTIONAL { ?argument ncl:hasAssertion ?assertion } \n" +
            "  OPTIONAL { ?argument ncl:hasExplanation ?explanation } \n" +
            "} \n" +
            "ORDER BY ?product";
        
        // Exécution de la requête
        QueryExecution qexec = QueryExecutionFactory.create(queryString, infModel);
        ResultSet results = qexec.execSelect();
        
        // Maps pour gérer les alternatives et arguments déjà créés
        Map<String, Individual> productToAlternative = new HashMap<>();
        Map<String, Individual> argumentToMychArgument = new HashMap<>();
        
        // Création d'un projet global (ou vous pouvez créer plusieurs projets selon vos besoins)
        Individual project = mychoiceModel.createIndividual(mch + "Project_NatclinnProducts", Project);
        project.addProperty(projectName, "Choix de produits Natclinn");
        project.addProperty(projectDescription, 
            "Projet de comparaison de produits alimentaires basé sur leurs caractéristiques et arguments");
        
        int alternativeCount = 0;
        int argumentCount = 0;
        
        System.out.println("   - Traitement des produits et leurs arguments...");
        
        // Parcours des résultats
        while (results.hasNext()) {
            QuerySolution solution = results.nextSolution();
            
            Resource productResource = solution.getResource("product");
            String productUri = productResource.getURI();
            Literal productLabel = solution.getLiteral("productLabel");
            
            Resource argumentResource = solution.getResource("argument");
            String argumentUri = argumentResource.getURI();
            Literal argLabel = solution.getLiteral("argLabel");
            Literal argName = solution.getLiteral("argName");
            Literal assertionLit = solution.getLiteral("assertion");
            Literal explanationLit = solution.getLiteral("explanation");
            
            // Création de l'alternative si elle n'existe pas encore
            if (!productToAlternative.containsKey(productUri)) {
                String alternativeUri = mch + "Alternative_" + productUri.substring(productUri.lastIndexOf("/") + 1);
                Individual alternative = mychoiceModel.createIndividual(alternativeUri, Alternative);
                
                alternative.addProperty(alternativeName, productLabel.getString());
                alternative.addProperty(alternativeDescription, "Produit : " + productLabel.getString());
                alternative.addProperty(relatedToProduct, productResource);
                
                // Lien projet -> alternative
                project.addProperty(hasAlternative, alternative);
                
                productToAlternative.put(productUri, alternative);
                alternativeCount++;
            }
            
            Individual alternative = productToAlternative.get(productUri);
            
            // Création de l'argument MyChoice si il n'existe pas encore
            if (!argumentToMychArgument.containsKey(argumentUri)) {
                String mychArgumentUri = mch + "Argument_" + argumentUri.substring(argumentUri.lastIndexOf("/") + 1);
                Individual mychArgument = mychoiceModel.createIndividual(mychArgumentUri, Argument);
                
                // Utilisation de argName ou argLabel pour l'assertion
                String assertionText = "";
                if (argName != null) {
                    assertionText = argName.getString();
                } else if (argLabel != null) {
                    assertionText = argLabel.getString();
                } else if (assertionLit != null) {
                    assertionText = assertionLit.getString();
                } else {
                    assertionText = "Argument lié au produit";
                }
                
                mychArgument.addProperty(assertionProp, assertionText);
                
                // Ajout de l'explication si disponible
                if (explanationLit != null) {
                    mychArgument.addProperty(explanationProp, explanationLit.getString());
                } else {
                    mychArgument.addProperty(explanationProp, "Argument inféré automatiquement depuis les caractéristiques du produit");
                }
                
                mychArgument.addProperty(relatedToArgument, argumentResource);
                
                argumentToMychArgument.put(argumentUri, mychArgument);
                argumentCount++;
            }
            
            Individual mychArgument = argumentToMychArgument.get(argumentUri);
            
            // Lien alternative -> argument
            alternative.addProperty(hasArgument, mychArgument);
        }
        
        qexec.close();
        
        System.out.println("   - Projet créé : " + project.getURI());
        System.out.println("   - Alternatives créées : " + alternativeCount);
        System.out.println("   - Arguments créés : " + argumentCount);
        
        return mychoiceModel;
    }
    
    /**
     * Sauvegarde le modèle dans un fichier
     */
    private static void saveModel(OntModel model, String fileName) throws IOException {
        String outputPath = NatclinnConf.folderForOntologies + "/" + fileName;
        FileOutputStream outStream = new FileOutputStream(outputPath);
        model.write(outStream, "RDF/XML");
        outStream.close();
        
        System.out.println("   - Fichier sauvegardé : " + outputPath);
    }
}
