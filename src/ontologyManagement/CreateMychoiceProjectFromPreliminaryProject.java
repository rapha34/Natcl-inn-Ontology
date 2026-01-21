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
 * 2. Récupère les produits et leurs arguments liés (via ncl:hasLinkToArgument)
 * 3. Crée un projet MyChoice (mch:Project) pour chaque groupe de produits
 * 4. Convertit chaque produit en alternative (mch:Alternative)
 * 5. Associe les arguments Natclinn aux alternatives MyChoice
 * 
 * @author Natclinn
 */
public class CreateMychoiceProjectFromPreliminaryProject {

    public static void main(String[] args) {
        try {
            new NatclinnConf();
            
            System.out.println("=== Création de projets MyChoice à partir des produits Natclinn ===");
            
            // Création du modèle inféré avec les règles product-argument
            System.out.println("\n1. Chargement du modèle inféré...");
            InfModel infModel = loadInferredModel();
            
            // Traitement de tous les projets MyChoice
            System.out.println("\n2. Traitement des projets MyChoice...");
            processAllProjects(infModel);
            
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
            
            // Traitement de tous les projets MyChoice trouvés dans le modèle
            System.out.println("Recherche des projets MyChoice à traiter...");
            processAllProjects(infModel);
            
            System.out.println("=== Tous les projets MyChoice traités avec succès ===\n");
            
        } catch (Exception e) {
            System.err.println("Erreur lors de la création des projets MyChoice : " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Traite tous les projets mch:Project trouvés dans le modèle inféré.
     * Génère un fichier XML par projet en utilisant le nom du projet pour le nom de fichier.
     * 
     * @param infModel Le modèle inféré contenant les projets
     */
    private static void processAllProjects(InfModel infModel) throws Exception {
        new NatclinnConf();
		String prefix = NatclinnConf.queryPrefix;
        
        // Requête pour récupérer tous les projets MyChoice
        String queryString = prefix +
            "SELECT ?project ?projectName \n" +
            "WHERE { \n" +
            "  ?project rdf:type mch:Project . \n" +
            "  ?project mch:projectName ?projectName . \n" +
            "}";
        
        QueryExecution qexec = QueryExecutionFactory.create(queryString, infModel);
        ResultSet results = qexec.execSelect();
        
        int projectCount = 0;
        
        while (results.hasNext()) {
            QuerySolution solution = results.nextSolution();
            Resource projectResource = solution.getResource("project");
            String projectUri = projectResource.getURI();
            String projectName = solution.getLiteral("projectName").getString();
            
            projectCount++;
            System.out.println("\n--- Traitement du projet " + projectCount + " : " + projectName + " ---");
            
            // Création du modèle enrichi pour ce projet
            OntModel mychoiceModel = createMychoiceProjectFromProducts(infModel, projectUri);
            
            // Génération du nom de fichier à partir du nom du projet
            // Normalisation : suppression des caractères spéciaux, espaces -> underscores
            String fileName = projectName
                .replaceAll("[^a-zA-Z0-9\\s-]", "")  // Garde lettres, chiffres, espaces, tirets
                .replaceAll("\\s+", "_")              // Espaces -> underscores
                .trim();
            
            if (fileName.isEmpty()) {
                fileName = "Project_" + projectCount;
            }
            
            fileName = fileName + ".xml";
            
            // Sauvegarde du modèle
            System.out.println("Sauvegarde du projet dans : " + fileName);
            saveModel(mychoiceModel, fileName);
            
            // Extraction automatique vers Excel
            String xmlFilePath = NatclinnConf.folderForOntologies + "/" + fileName;
            String excelFileName = fileName.replaceFirst("[.][^.]+$", ".xlsx");
            String excelFilePath = NatclinnConf.folderForResults + "/" + excelFileName;
            
            try {
                System.out.println("Extraction vers Excel : " + excelFileName);
                ExtractMychoiceProjectToExcel.extractProjectToExcel(xmlFilePath, excelFilePath);
                System.out.println("Fichier Excel généré : " + excelFilePath);
            } catch (Exception e) {
                System.err.println("Erreur lors de l'extraction Excel : " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        qexec.close();
        
        if (projectCount == 0) {
            System.out.println("Aucun projet MyChoice trouvé dans le modèle.");
        } else {
            System.out.println("\nNombre total de projets traités : " + projectCount);
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
     * Crée des projets MyChoice à partir des produits et arguments du modèle inféré.
     * Traite un projet spécifique identifié par son URI.
     * 
     * @param infModel Le modèle inféré
     * @param projectUri L'URI du projet à traiter
     * @return Le modèle MyChoice enrichi pour ce projet (contient uniquement ce projet et ses données)
     */
    private static OntModel createMychoiceProjectFromProducts(InfModel infModel, String projectUri) {
    // Création d'un modèle vide qui contiendra uniquement ce projet et ses données
    OntModel mychoiceModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
        
        // Définition des namespaces
        String prefix = NatclinnConf.queryPrefix;
        String mch = NatclinnConf.mch;
        mychoiceModel.setNsPrefix("mch", mch);
        String ncl = NatclinnConf.ncl;
        mychoiceModel.setNsPrefix("ncl", ncl);
        
        // Création de l'ontologie
        Ontology ont = mychoiceModel.createOntology(mch + "MyChoiceProjectsFromProducts");
        mychoiceModel.add(ont, RDFS.label, "Projet MyChoice générés depuis les produits et arguments Natclinn");
        mychoiceModel.add(ont, DC.description, 
            "Projet MyChoice créé automatiquement à partir des produits Natclinn et leurs arguments inférés");
        
    // Définition des classes et propriétés MyChoice
        OntClass Alternative = mychoiceModel.createClass(mch + "Alternative");
        OntClass Argument = mychoiceModel.createClass(mch + "Argument");
        OntClass Source = mychoiceModel.createClass(mch + "Source");
        OntClass TypeSource = mychoiceModel.createClass(mch + "TypeSource");
        OntClass Stakeholder = mychoiceModel.createClass(mch + "Stakeholder");
        
        DatatypeProperty projectName = mychoiceModel.createDatatypeProperty(mch + "projectName");
        DatatypeProperty projectDescription = mychoiceModel.createDatatypeProperty(mch + "projectDescription");
        DatatypeProperty nameAlternative = mychoiceModel.createDatatypeProperty(mch + "nameAlternative");
        DatatypeProperty alternativeDescription = mychoiceModel.createDatatypeProperty(mch + "alternativeDescription");
        DatatypeProperty imageAlternative = mychoiceModel.createDatatypeProperty(mch + "imageAlternative");
        DatatypeProperty iconAlternative = mychoiceModel.createDatatypeProperty(mch + "iconAlternative");
        DatatypeProperty assertionProp = mychoiceModel.createDatatypeProperty(mch + "assertion");
        DatatypeProperty explanationProp = mychoiceModel.createDatatypeProperty(mch + "explanation");
        
    // Relations de projet (respect des domaines/ranges de la TBox)
        ObjectProperty hasProject = mychoiceModel.createObjectProperty(mch + "hasProject"); // Alternative -> Project
        ObjectProperty belongsToProject = mychoiceModel.createObjectProperty(mch + "belongsToProject"); // Argument -> Project
        ObjectProperty hasAlternative = mychoiceModel.createObjectProperty(mch + "hasAlternative"); // Argument -> Alternative
        ObjectProperty hasArgument = mychoiceModel.createObjectProperty(mch + "hasArgument"); // Alternative -> Argument
        ObjectProperty relatedToProduct = mychoiceModel.createObjectProperty(mch + "relatedToProduct");
        DatatypeProperty relatedToNatclinnProductArgument = mychoiceModel.createDatatypeProperty(mch + "relatedToNatclinnProductArgument");
        ObjectProperty hasSource = mychoiceModel.createObjectProperty(mch + "hasSource"); // Argument -> Source
        ObjectProperty hasTypeSource = mychoiceModel.createObjectProperty(mch + "hasTypeSource"); // Source -> TypeSource
        DatatypeProperty dateSource = mychoiceModel.createDatatypeProperty(mch + "date"); // Source -> date
        ObjectProperty hasStakeholder = mychoiceModel.createObjectProperty(mch + "hasStakeholder"); // Argument -> Stakeholder
        
        // Vérification si le projet existe déjà (créé par les INSERT queries)
        Individual project;
        boolean projectExists;
        
        Resource existingProjectResource = infModel.getResource(projectUri);
        if (existingProjectResource != null && infModel.contains(existingProjectResource, null, (RDFNode)null)) {
            // Copier le projet depuis infModel vers mychoiceModel
            project = mychoiceModel.createIndividual(projectUri, mychoiceModel.createClass(mch + "Project"));
            
            // Copier toutes les propriétés du projet
            StmtIterator projectStmts = infModel.listStatements(existingProjectResource, null, (RDFNode)null);
            while (projectStmts.hasNext()) {
                Statement stmt = projectStmts.nextStatement();
                project.addProperty(stmt.getPredicate(), stmt.getObject());
            }
            projectExists = true;
        } else {
            // Le projet n'existe pas, on ne peut pas le traiter
            System.err.println("   ⚠ Projet non trouvé : " + projectUri);
            return mychoiceModel;
        }
        
        if (projectExists) {
            System.out.println("   - Projet MyChoice détecté : " + projectUri);
            
            // Affichage des propriétés du projet
            Statement nameStmt = project.getProperty(projectName);
            if (nameStmt != null) {
                System.out.println("   - Nom : " + nameStmt.getString());
            }
            
            Statement descStmt = project.getProperty(projectDescription);
            if (descStmt != null) {
                System.out.println("   - Description : " + descStmt.getString());
            }
            
            // Vérifier si le projet a une image, sinon ajouter l'image par défaut
            DatatypeProperty projectImageProp = mychoiceModel.createDatatypeProperty(mch + "projectImage");
            Statement imageStmt = project.getProperty(projectImageProp);
            if (imageStmt == null || imageStmt.getString().trim().isEmpty()) {
                System.out.println("   - Ajout de l'image par défaut du projet");
                if (imageStmt != null) {
                    project.removeAll(projectImageProp);
                }
                project.addProperty(projectImageProp, NatclinnConf.defaultProjectImage);
            }
            
            System.out.println("   - Enrichissement avec les arguments inférés...");
        }
        
        // Requête SPARQL pour récupérer les produits avec leurs arguments
        // Filtrée pour ce projet spécifique - récupère TOUTES les propriétés de ncl:ProductArgument
        // Mise à jour pour l'architecture V5 avec LinkToArgument
        String queryString = prefix +
            "SELECT ?product ?productLabel ?productDesc ?tag ?tagLabel ?argument ?argLabel \n" +
            "  ?assertion ?polarity ?nameCriterion ?aim ?nameProperty ?valueProperty \n" +
            "  ?condition ?infValue ?supValue ?unit ?verbatimText \n" +
            "  ?source ?sourceLabel ?dateSource ?typeSource ?typeSourceLabel ?typeFiability \n" +
            "  ?stakeholder ?stakeholderLabel \n" +
            "WHERE { \n" +
            // Récupérer les alternatives du projet
            "  ?alternative mch:hasProject <" + projectUri + "> . \n" +
            "  ?alternative mch:relatedToProduct ?product . \n" +
            // Récupérer les arguments inférés du produit via LinkToArgument
            "  ?product rdf:type ncl:Product . \n" +
            "  ?product skos:prefLabel ?productLabel . \n" +
            "  OPTIONAL { ?product ncl:description ?productDesc } \n" +
            "  ?product ncl:hasLinkToArgument ?link . \n" +
            "  ?link ncl:hasReferenceProductArgument ?argument . \n" +
            "  ?argument rdf:type ncl:ProductArgument . \n" +
            "  ?link ncl:hasTagInitiator ?tag . " +
            "  ?tag skos:prefLabel ?tagLabel . " +
            "  ?tagArgumentBinding ncl:aboutTag ?tag . " +
            // Toutes les propriétés de ncl:tagArgumentBinding
            "  OPTIONAL { ?tagArgumentBinding ncl:tagAssertion ?assertion } \n" +
            "  OPTIONAL { ?tagArgumentBinding ncl:tagPolarity ?polarity } \n" +
            "  OPTIONAL { ?tagArgumentBinding ncl:tagNameCriterion ?nameCriterion } \n" +
            "  OPTIONAL { ?tagArgumentBinding ncl:tagAim ?aim } \n" +
            "  OPTIONAL { ?tagArgumentBinding ncl:tagNameProperty ?nameProperty } \n" +
            "  OPTIONAL { ?tagArgumentBinding ncl:tagValueProperty ?valueProperty } \n" +
            "  OPTIONAL { ?tagArgumentBinding ncl:tagCondition ?condition } \n" +
            "  OPTIONAL { ?tagArgumentBinding ncl:tagInfValue ?infValue } \n" +
            "  OPTIONAL { ?tagArgumentBinding ncl:tagSupValue ?supValue } \n" +
            "  OPTIONAL { ?tagArgumentBinding ncl:tagUnit ?unit } \n" +
            // Toutes les propriétés de ncl:ProductArgument
            "  OPTIONAL { ?argument skos:prefLabel ?argLabel } \n" +
            "  OPTIONAL { ?argument ncl:verbatim ?verbatimText } \n" +
            // Source avec TypeSource et fiability
            "  OPTIONAL { ?argument ncl:hasSource ?source . \n" +
            "             OPTIONAL { ?source skos:prefLabel ?sourceLabel } \n" +
            "             OPTIONAL { ?source ncl:date ?dateSource } \n" +
            "             OPTIONAL { ?source ncl:hasTypeSource ?typeSource . \n" +
            "                        OPTIONAL { ?typeSource skos:prefLabel ?typeSourceLabel } \n" +
            "                        OPTIONAL { ?typeSource ncl:fiability ?typeFiability } \n" +
            "             } \n" +
            "  } \n" +
            // Stakeholder
            "  OPTIONAL { ?argument ncl:hasStakeholder ?stakeholder . \n" +
            "             OPTIONAL { ?stakeholder skos:prefLabel ?stakeholderLabel } \n" +
            "  } \n" +
            "} \n" +
            "ORDER BY ?product";
        
        // Exécution de la requête
        QueryExecution qexec = QueryExecutionFactory.create(queryString, infModel);
        ResultSet results = qexec.execSelect();
        //System.out.println("   - Nombre de produits/arguments récupérés : " + ResultSetFormatter.toList(results).size());
        
        // Maps pour gérer les alternatives et arguments déjà créés
        Map<String, Individual> productToAlternative = new HashMap<>();
        Map<String, Individual> argumentToMychArgument = new HashMap<>();
        Map<String, Individual> sourceToMychSource = new HashMap<>(); // Map pour les sources
        Map<String, Individual> typeSourceToMychTypeSource = new HashMap<>(); // Map pour les types de sources
        Map<String, Individual> stakeholderToMychStakeholder = new HashMap<>(); // Map pour les stakeholders
        
        // Si le projet existe déjà, charger les alternatives existantes
        int alternativeIndex = 0;
        if (projectExists) {
            // Recherche des alternatives: Alternative -> hasProject -> Project
            StmtIterator altIter = infModel.listStatements(null, hasProject, existingProjectResource);
            while (altIter.hasNext()) {
                Statement stmt = altIter.nextStatement();
                Resource alternativeResource = stmt.getSubject().asResource();
                
                // Copier l'alternative dans mychoiceModel
                Individual alternative = mychoiceModel.createIndividual(alternativeResource.getURI(), Alternative);
                
                // Copier toutes les propriétés de l'alternative
                StmtIterator altProps = infModel.listStatements(alternativeResource, null, (RDFNode)null);
                while (altProps.hasNext()) {
                    Statement altStmt = altProps.nextStatement();
                    alternative.addProperty(altStmt.getPredicate(), altStmt.getObject());
                    
                    // Si c'est la propriété relatedToProduct, copier aussi le produit
                    if (altStmt.getPredicate().equals(relatedToProduct)) {
                        Resource productResource = altStmt.getObject().asResource();
                        String productUri = productResource.getURI();
                        
                        // Copier toutes les propriétés du produit
                        StmtIterator productProps = infModel.listStatements(productResource, null, (RDFNode)null);
                        while (productProps.hasNext()) {
                            Statement prodStmt = productProps.nextStatement();
                            mychoiceModel.add(prodStmt);
                        }
                        
                        productToAlternative.put(productUri, alternative);
                    }
                }
                
                // Lier l'alternative au projet (Alternative -> Project)
                alternative.addProperty(hasProject, project);
                
                // Ajouter image par défaut si absente ou vide
                Statement imageStmt = alternative.getProperty(imageAlternative);
                if (imageStmt == null || imageStmt.getString().trim().isEmpty()) {
                    if (imageStmt != null) {
                        alternative.removeAll(imageAlternative);
                    }
                    alternative.addProperty(imageAlternative, NatclinnConf.defaultAlternativeImage);
                }
                
                // Ajouter icon numérique incrémenté si absent ou vide
                alternativeIndex++;
                Statement iconStmt = alternative.getProperty(iconAlternative);
                if (iconStmt == null || iconStmt.getString().trim().isEmpty()) {
                    if (iconStmt != null) {
                        alternative.removeAll(iconAlternative);
                    }
                    alternative.addProperty(iconAlternative, "numeric-" + alternativeIndex);
                }
            }
            System.out.println("   - Alternatives existantes chargées : " + productToAlternative.size());
        }
        
        int alternativeCount = 0;
        int argumentCount = 0;
        
        System.out.println("   - Traitement des produits et leurs arguments...");
        
        // Parcours des résultats
        while (results.hasNext()) {
            QuerySolution solution = results.nextSolution();
            
            Resource productResource = solution.getResource("product");
            String productUriStr = productResource.getURI();
            Literal productLabel = solution.getLiteral("productLabel");
            Literal productDesc = solution.getLiteral("productDesc");
            Resource tagResource = solution.getResource("tag");
            Literal tagLabel = solution.getLiteral("tagLabel");
            Resource argumentResource = solution.getResource("argument");
            String argumentUri = argumentResource.getURI();
            Literal argLabel = solution.getLiteral("argLabel");
            // Toutes les propriétés de ncl:ProductArgument
            Literal assertionLit = solution.getLiteral("assertion");
            Literal polarityLit = solution.getLiteral("polarity");
            Literal nameCriterionLit = solution.getLiteral("nameCriterion");
            Literal aimLit = solution.getLiteral("aim");
            Literal namePropertyLit = solution.getLiteral("nameProperty");
            Literal valuePropertyLit = solution.getLiteral("valueProperty");
            Literal conditionLit = solution.getLiteral("condition");
            Literal infValueLit = solution.getLiteral("infValue");
            Literal supValueLit = solution.getLiteral("supValue");
            Literal unitLit = solution.getLiteral("unit");
            // Verbatim/hasText devient explanation dans MyChoice
            Literal verbatimTextLit = solution.getLiteral("verbatimText");
            // Propriétés de la source Natclinn + TypeSource
            Resource sourceResource = solution.getResource("source");
            Literal sourceLabelLit = solution.getLiteral("sourceLabel");
            Resource typeSourceResource = solution.getResource("typeSource");
            Literal typeSourceLabelLit = solution.getLiteral("typeSourceLabel");
            Literal dateSourceLit = solution.getLiteral("dateSource");
            Literal typeFiabilityLit = solution.getLiteral("typeFiability");
            // Stakeholder Natclinn
            Resource stakeholderResource = solution.getResource("stakeholder");
            Literal stakeholderLabelLit = solution.getLiteral("stakeholderLabel");
            
            // Récupération ou création de l'alternative
            Individual alternative;
            if (productToAlternative.containsKey(productUriStr)) {
                // L'alternative existe déjà (créée par INSERT), on la récupère pour l'enrichir
                alternative = productToAlternative.get(productUriStr);
                
                // Vérifier et ajouter image par défaut si absente ou vide
                Statement imageStmt = alternative.getProperty(imageAlternative);
                if (imageStmt == null || imageStmt.getString().trim().isEmpty()) {
                    if (imageStmt != null) {
                        alternative.removeAll(imageAlternative);
                    }
                    alternative.addProperty(imageAlternative, NatclinnConf.defaultAlternativeImage);
                }
                
                // Vérifier et ajouter icon numérique incrémenté si absent ou vide
                Statement iconStmt = alternative.getProperty(iconAlternative);
                if (iconStmt == null || iconStmt.getString().trim().isEmpty()) {
                    alternativeIndex++;
                    if (iconStmt != null) {
                        alternative.removeAll(iconAlternative);
                    }
                    alternative.addProperty(iconAlternative, "numeric-" + alternativeIndex);
                }
            } else {
                // Cas exceptionnel : alternative non créée par INSERT, on la crée maintenant
                // (Ce cas ne devrait plus arriver avec le nouveau workflow INSERT)
                String alternativeUri = mch + "Alternative-" + productUriStr.substring(productUriStr.lastIndexOf("/") + 1);
                alternative = mychoiceModel.createIndividual(alternativeUri, Alternative);
                
                alternative.addProperty(nameAlternative, productLabel.getString());
                if (productDesc != null) {
                    alternative.addProperty(alternativeDescription, productDesc.getString());
                } else {
                    alternative.addProperty(alternativeDescription, "Produit : " + productLabel.getString());
                }
                alternative.addProperty(relatedToProduct, productResource);
                
                // Lien Alternative -> Project
                alternative.addProperty(hasProject, project);
                
                // Ajouter image par défaut
                alternative.addProperty(imageAlternative, NatclinnConf.defaultAlternativeImage);
                
                // Ajouter icon numérique incrémenté
                alternativeIndex++;
                alternative.addProperty(iconAlternative, "numeric-" + alternativeIndex);
                
                productToAlternative.put(productUriStr, alternative);
                alternativeCount++;
                System.out.println("   ⚠ Alternative créée à la volée (pas par INSERT) : " + productLabel.getString());
            }
            
            // Création de l'argument MyChoice si il n'existe pas encore
            // Clé = mychArgumentUri (URI complète incluant le tag) pour permettre plusieurs arguments
            // MyChoice différents basés sur le même ProductArgument mais avec des tags différents
            String mychArgumentUri = mch + "Argument-" + tagLabel + "_" + argumentUri.substring(argumentUri.lastIndexOf("/") + 1);
            if (!argumentToMychArgument.containsKey(mychArgumentUri)) {
                System.out.println("   - Création de l'argument MyChoice : " + mychArgumentUri);
                System.out.println("   - avec tag : " + tagResource.toString());
                Individual mychArgument = mychoiceModel.createIndividual(mychArgumentUri, Argument);
                
                // Copier TOUTES les propriétés de ncl:ProductArgument vers mch:Argument
                
                // assertion (priorité: assertion, sinon nameProperty, sinon argLabel)
                if (assertionLit != null) {
                    mychArgument.addProperty(assertionProp, assertionLit.getString());
                } else if (namePropertyLit != null) {
                    mychArgument.addProperty(assertionProp, namePropertyLit.getString());
                } else if (argLabel != null) {
                    mychArgument.addProperty(assertionProp, argLabel.getString());
                } else {
                    mychArgument.addProperty(assertionProp, "Argument lié au produit");
                }
                
                // explanation: Verbatim/hasText de Natclinn devient explanation dans MyChoice
                if (verbatimTextLit != null) {
                    mychArgument.addProperty(explanationProp, verbatimTextLit.getString());
                } else {
                    mychArgument.addProperty(explanationProp, "Argument inféré automatiquement");
                }
                
                // Autres propriétés de ncl:TagArgumentBinding -> mch:Argument
                DatatypeProperty polarityProp = mychoiceModel.createDatatypeProperty(mch + "polarity");
                DatatypeProperty nameCriterionProp = mychoiceModel.createDatatypeProperty(mch + "nameCriterion");
                DatatypeProperty aimProp = mychoiceModel.createDatatypeProperty(mch + "aim");
                DatatypeProperty namePropertyProp = mychoiceModel.createDatatypeProperty(mch + "nameProperty");
                DatatypeProperty valuePropertyProp = mychoiceModel.createDatatypeProperty(mch + "valueProperty");
                DatatypeProperty conditionProp = mychoiceModel.createDatatypeProperty(mch + "condition");
                DatatypeProperty infValueProp = mychoiceModel.createDatatypeProperty(mch + "infValue");
                DatatypeProperty supValueProp = mychoiceModel.createDatatypeProperty(mch + "supValue");
                DatatypeProperty unitProp = mychoiceModel.createDatatypeProperty(mch + "unit");
                
                if (polarityLit != null) mychArgument.addProperty(polarityProp, polarityLit.getString());
                if (nameCriterionLit != null) mychArgument.addProperty(nameCriterionProp, nameCriterionLit.getString());
                if (aimLit != null) mychArgument.addProperty(aimProp, aimLit.getString());
                if (namePropertyLit != null) mychArgument.addProperty(namePropertyProp, namePropertyLit.getString());
                if (valuePropertyLit != null) mychArgument.addProperty(valuePropertyProp, valuePropertyLit.getString());
                if (conditionLit != null) mychArgument.addProperty(conditionProp, conditionLit.getString());
                if (infValueLit != null) mychArgument.addProperty(infValueProp, infValueLit.getString());
                if (supValueLit != null) mychArgument.addProperty(supValueProp, supValueLit.getString());
                if (unitLit != null) mychArgument.addProperty(unitProp, unitLit.getString());
                
                // Créer ou récupérer la source MyChoice
                if (sourceResource != null) {
                    String sourceUri = sourceResource.getURI();
                    Individual mychSource;
                    
                    if (sourceToMychSource.containsKey(sourceUri)) {
                        // Source déjà créée, on la réutilise
                        mychSource = sourceToMychSource.get(sourceUri);
                    } else {
                        // Créer une nouvelle source MyChoice
                        String mychSourceUri = mch + "Source-" + sourceUri.substring(sourceUri.lastIndexOf("/") + 1);
                        mychSource = mychoiceModel.createIndividual(mychSourceUri, Source);
                        
                        // Propriétés de la source
                        DatatypeProperty sourceNameProp = mychoiceModel.createDatatypeProperty(mch + "sourceName");
                        DatatypeProperty dateSourceProp = mychoiceModel.createDatatypeProperty(mch + "date");
                        DatatypeProperty typeSourceNameProp = mychoiceModel.createDatatypeProperty(mch + "typeSourceName");
                        DatatypeProperty typeSourceFiabilityProp = mychoiceModel.createDatatypeProperty(mch + "typeSourceFiability");
                        
                        // Transfert des propriétés
                        if (sourceLabelLit != null) {
                            mychSource.addProperty(sourceNameProp, sourceLabelLit.getString());
                        }
                        if (dateSourceLit != null) {
                            mychSource.addProperty(dateSourceProp, dateSourceLit.getString());
                        }
                        // Création/Lien du TypeSource si présent
                        // System.out.println("typeSourceResource : " + typeSourceResource);
                        if (typeSourceResource != null) {
                            String tsUri = typeSourceResource.getURI();
                            Individual mychTypeSource;
                            if (typeSourceToMychTypeSource.containsKey(tsUri)) {
                                mychTypeSource = typeSourceToMychTypeSource.get(tsUri);
                            } else {
                                String mychTypeSourceUri = mch + "TypeSource-" + tsUri.substring(tsUri.lastIndexOf("/") + 1);
                                mychTypeSource = mychoiceModel.createIndividual(mychTypeSourceUri, TypeSource);
                                
                                if (typeSourceLabelLit != null) {
                                    mychTypeSource.addProperty(typeSourceNameProp, typeSourceLabelLit.getString());
                                }
                                
                                if (typeFiabilityLit != null) {
                                    mychTypeSource.addProperty(typeSourceFiabilityProp, typeFiabilityLit.getString());
                                }
                                typeSourceToMychTypeSource.put(tsUri, mychTypeSource);
                            // Lier la source au type de source
                            mychSource.addProperty(hasTypeSource, mychTypeSource);
                            }
                        }
                        
                        sourceToMychSource.put(sourceUri, mychSource);
                    }
                    
                    // Lier l'argument à la source
                    mychArgument.addProperty(hasSource, mychSource);
                }
                
                // Créer ou récupérer le stakeholder MyChoice
                if (stakeholderResource != null) {
                    String stakeholderUri = stakeholderResource.getURI();
                    Individual mychStakeholder;
                    
                    if (stakeholderToMychStakeholder.containsKey(stakeholderUri)) {
                        // Stakeholder déjà créé, on le réutilise
                        mychStakeholder = stakeholderToMychStakeholder.get(stakeholderUri);
                    } else {
                        // Créer un nouveau stakeholder MyChoice
                        String mychStakeholderUri = mch + "Stakeholder-" + stakeholderUri.substring(stakeholderUri.lastIndexOf("/") + 1);
                        mychStakeholder = mychoiceModel.createIndividual(mychStakeholderUri, Stakeholder);
                        
                        // Propriété stakeholderName sur le Stakeholder
                        DatatypeProperty stakeholderNameProp = mychoiceModel.createDatatypeProperty(mch + "stakeholderName");
                        
                        // Transfert du label
                        if (stakeholderLabelLit != null) {
                            mychStakeholder.addProperty(stakeholderNameProp, stakeholderLabelLit.getString());
                        }
                        
                        stakeholderToMychStakeholder.put(stakeholderUri, mychStakeholder);
                    }
                    
                    // Lier l'argument au stakeholder
                    mychArgument.addProperty(hasStakeholder, mychStakeholder);
                }
                
                // Lien vers l'argument Natclinn original (URI stockée comme string)
                mychArgument.addProperty(relatedToNatclinnProductArgument, argumentResource.getURI());
                
                // Stocker avec mychArgumentUri (incluant le tag) pour déduplication correcte
                argumentToMychArgument.put(mychArgumentUri, mychArgument);
                argumentCount++;
            }
            
            // Récupération de l'argument MyChoice avec la clé complète (incluant le tag)
            Individual mychArgument = argumentToMychArgument.get(mychArgumentUri);
            
            // Lien alternative -> argument
            alternative.addProperty(hasArgument, mychArgument);
            // Lien argument -> alternative (inverse pour faciliter les requêtes)
            mychArgument.addProperty(hasAlternative, alternative);
            // Lien Argument -> Project
            mychArgument.addProperty(belongsToProject, project);
        }
        
        qexec.close();
        
        if (projectExists) {
            System.out.println("   - Projet enrichi : " + project.getURI());
            System.out.println("   - Alternatives enrichies : " + productToAlternative.size());
            if (alternativeCount > 0) {
                System.out.println("   ⚠ Alternatives créées à la volée : " + alternativeCount);
            }
            System.out.println("   - Arguments ajoutés : " + argumentCount);
        } else {
            System.out.println("   - Projet créé : " + project.getURI());
            System.out.println("   - Alternatives créées : " + alternativeCount);
            System.out.println("   - Arguments créés : " + argumentCount);
        }
        
        return mychoiceModel;
    }
    
    /**
     * Sauvegarde le modèle dans un fichier en excluant les triplets owl:differentFrom,
     * ncl:hasIngredientR et en utilisant un format RDF/XML lisible sans blank nodes inutiles
     */
    private static void saveModel(OntModel model, String fileName) throws IOException {
        // Créer un modèle temporaire sans les triplets owl:differentFrom, hasIngredientR et sans blank nodes
        OntModel cleanModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
        
        // Copier tous les triplets sauf ceux avec owl:differentFrom, ncl:hasIngredientR
        // et ceux qui impliquent des blank nodes comme sujet ou objet
        String owl = NatclinnConf.owl;
        String rdf = NatclinnConf.rdf;
        String ncl = NatclinnConf.ncl;
        Property differentFrom = model.getProperty(owl + "differentFrom");
    Property rdfType = model.getProperty(rdf + "type");
    Property hasIngredientR = model.getProperty(ncl + "hasIngredientR");
    Property aboutFunction = model.getProperty(ncl + "aboutFunction");
    Property hasRole = model.getProperty(ncl + "hasRole");
        StmtIterator iter = model.listStatements();
        while (iter.hasNext()) {
            Statement stmt = iter.nextStatement();
            // Exclure owl:differentFrom et les triplets avec blank nodes
        boolean isBlankSubject = stmt.getSubject().isAnon();
        boolean isBlankObject = (!stmt.getObject().isLiteral() && stmt.getObject().isAnon());

        // Filtrer les IRIs "null" éventuels
        boolean subjectIsNullIRI = stmt.getSubject().isURIResource() &&
            "null".equalsIgnoreCase(stmt.getSubject().getURI());
        boolean objectIsNullIRI = stmt.getObject().isResource() &&
            stmt.getObject().asResource().isURIResource() &&
            "null".equalsIgnoreCase(stmt.getObject().asResource().getURI());

        // Filtrer les triplets du type ?s rdf:type rdfs:Resource
        boolean isTypeRdfsResource = stmt.getPredicate().equals(rdfType) &&
            stmt.getObject().isResource() && stmt.getObject().asResource().equals(RDFS.Resource);

        // Filtrer ncl:hasIngredientR
        boolean isHasIngredientR = stmt.getPredicate().equals(hasIngredientR);

        // Filtrer ncl:aboutFunction (lien AdditiveFunctionArgumentBinding -> AdditiveFunction)
        boolean isAboutFunction = stmt.getPredicate().equals(aboutFunction);

        // Filtrer ncl:hasRole (lien Ingredient -> AdditiveFunction)
        boolean isHasRole = stmt.getPredicate().equals(hasRole);

        if (!stmt.getPredicate().equals(differentFrom)
            && !isBlankSubject
            && (stmt.getObject().isLiteral() || !isBlankObject)
            && !subjectIsNullIRI
            && !objectIsNullIRI
            && !isTypeRdfsResource
            && !isHasIngredientR
            && !isAboutFunction
            && !isHasRole) {
        cleanModel.add(stmt);
        }
        }
        
        // Sauvegarder le modèle nettoyé en format RDF/XML-ABBREV (plus lisible)
        String outputPath = NatclinnConf.folderForOntologies + "/" + fileName;
        FileOutputStream outStream = new FileOutputStream(outputPath);
        cleanModel.write(outStream, "RDF/XML-ABBREV");
        outStream.close();
        
        System.out.println("   - Fichier sauvegardé : " + outputPath);
    }
}
