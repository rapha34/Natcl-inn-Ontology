package ontologyManagement;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;

import natclinn.util.NatclinnConf;

/**
 * Exporte un projet MyChoice du graphe RDF vers des fichiers DAMN (JSON et texte).
 * Inverse de CreateMychoiceAbox, génère les formats DAMN (Defeasible Reasoning Tool for Multi-Agent Reasoning).
 */
public class ExportMychoiceProjectToDamn {

    public static void main(String[] args) {
        new NatclinnConf();
        
        String aboxFile = null;

        if (args.length > 0) {
            aboxFile = args[0];
        }
        
        if (aboxFile == null) {
            aboxFile = NatclinnConf.folderForOntologies + "/Project_madeleines.xml";
            System.out.println("Utilisation du fichier par défaut : " + aboxFile);
        }
        
        // Générer les noms des fichiers de sortie à partir du nom du fichier d'entrée
        String inputFileName = new java.io.File(aboxFile).getName();
        String baseName = inputFileName.replaceFirst("[.][^.]+$", ""); // Retire l'extension
        String outputJson = NatclinnConf.folderForResults + "/" + baseName + "_damn.json";
        String outputText = NatclinnConf.folderForResults + "/" + baseName + "_damn.txt";
        
        try {
            exportProjectToDamn(aboxFile, outputJson, outputText);
            System.out.println("Export réussi :");
            System.out.println("  - JSON : " + outputJson);
            System.out.println("  - Texte : " + outputText);
        } catch (Exception e) {
            System.err.println("Erreur lors de l'export : " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Exporte un projet MyChoice vers les formats JSON et texte DAMN.
     * 
     * @param aboxFile Chemin du fichier RDF/XML contenant le projet MyChoice
     * @param outputJson Chemin du fichier JSON de sortie
     * @param outputText Chemin du fichier texte de sortie
     */
    public static void exportProjectToDamn(String aboxFile, String outputJson, String outputText) 
            throws FileNotFoundException, IOException {
        
        // Charger le modèle RDF
        Model om = ModelFactory.createDefaultModel();
        try (FileInputStream fis = new FileInputStream(aboxFile)) {
            om.read(fis, null, "RDF/XML");
        }
        
        String mch = "https://w3id.org/MCH/ontology/";
        
        // Détecter automatiquement le projet dans le fichier
        StmtIterator projectIter = om.listStatements(null, RDF.type, om.getResource(mch + "Project"));
        if (!projectIter.hasNext()) {
            throw new IllegalArgumentException("Aucun projet (mch:Project) trouvé dans le fichier : " + aboxFile);
        }
        
        Resource projectRes = projectIter.nextStatement().getSubject();
        String projectURI = projectRes.getURI();
        System.out.println("Projet détecté : " + projectURI);
        
        // Vérifier qu'il n'y a qu'un seul projet
        if (projectIter.hasNext()) {
            System.out.println("AVERTISSEMENT : Plusieurs projets trouvés dans le fichier. Seul le premier sera exporté.");
        }
        
        // Structures pour stocker les données extraites
        Map<String, AlternativeData> alternatives = new HashMap<>();
        Map<String, ArgumentData> arguments = new HashMap<>();
        
        // Récupérer toutes les alternatives liées au projet (Alternative -> hasProject -> Project)
        Property hasProject = om.getProperty(mch + "hasProject");
        StmtIterator altIter = om.listStatements((Resource) null, hasProject, projectRes);
        
        while (altIter.hasNext()) {
            Resource altRes = altIter.nextStatement().getSubject();
            AlternativeData altData = extractAlternative(om, altRes, mch);
            alternatives.put(altRes.getURI(), altData);
            
            // Récupérer tous les arguments liés à cette alternative
            Property hasArgument = om.getProperty(mch + "hasArgument");
            StmtIterator argIter = om.listStatements((Resource) altRes, (Property) hasArgument, (RDFNode) null);
            
            while (argIter.hasNext()) {
                Resource argRes = argIter.nextStatement().getObject().asResource();
                ArgumentData argData = extractArgument(om, argRes, mch);
                argData.alternativeName = altData.name;  // Associer le nom de l'alternative
                arguments.put(argRes.getURI(), argData);
                altData.arguments.add(argData);
            }
        }
        
        // Exporter en JSON
        ExportMychoiceProjectToDamnJson.exportToJson(alternatives, arguments, projectURI, outputJson);
        
        // Exporter en texte DAMN
        ExportMychoiceProjectToDamnText.exportToText(alternatives, arguments, projectURI, outputText);
    }
    
    /**
     * Extrait les données d'une alternative MyChoice.
     */
    private static AlternativeData extractAlternative(Model om, Resource altRes, String mch) {
        AlternativeData data = new AlternativeData();
        data.uri = altRes.getURI();
        data.name = getStringProperty(om, altRes, mch + "nameAlternative");
        data.description = getStringProperty(om, altRes, mch + "alternativeDescription");
        data.image = getStringProperty(om, altRes, mch + "imageAlternative");
        data.icon = getStringProperty(om, altRes, mch + "iconAlternative");
        data.arguments = new ArrayList<>();
        return data;
    }
    
    /**
     * Extrait les données d'un argument MyChoice.
     */
    private static ArgumentData extractArgument(Model om, Resource argRes, String mch) {
        ArgumentData data = new ArgumentData();
        data.uri = argRes.getURI();
        data.assertion = getStringProperty(om, argRes, mch + "assertion");
        data.explanation = getStringProperty(om, argRes, mch + "explanation");
        data.value = getStringProperty(om, argRes, mch + "value");
        data.nameCriterion = getStringProperty(om, argRes, mch + "nameCriterion");
        data.nameProperty = getStringProperty(om, argRes, mch + "nameProperty");
        data.condition = getStringProperty(om, argRes, mch + "condition");
        data.infValue = getStringProperty(om, argRes, mch + "infValue");
        data.supValue = getStringProperty(om, argRes, mch + "supValue");
        data.unit = getStringProperty(om, argRes, mch + "unit");
        data.isProspective = getStringProperty(om, argRes, mch + "isProspective");
        data.date = getStringProperty(om, argRes, mch + "evaluationDate");
        
        // Relations
        Resource stakeholderRes = getObjectProperty(om, argRes, mch + "hasStakeholder");
        if (stakeholderRes != null) {
            data.stakeholder = extractStakeholder(om, stakeholderRes, mch);
        }
        
        // Sources
        Property hasSource = om.getProperty(mch + "hasSource");
        StmtIterator sourceIter = om.listStatements((Resource) argRes, (Property) hasSource, (RDFNode) null);
        data.sources = new ArrayList<>();
        while (sourceIter.hasNext()) {
            Resource sourceRes = sourceIter.nextStatement().getObject().asResource();
            data.sources.add(extractSource(om, sourceRes, mch));
        }
        
        return data;
    }
    
    /**
     * Extrait les données d'une partie prenante.
     */
    private static StakeholderData extractStakeholder(Model om, Resource stakeholderRes, String mch) {
        StakeholderData data = new StakeholderData();
        data.uri = stakeholderRes.getURI();
        data.name = getStringProperty(om, stakeholderRes, mch + "nameStakeholder");
        return data;
    }
    
    /**
     * Extrait les données d'une source.
     */
    private static SourceData extractSource(Model om, Resource sourceRes, String mch) {
        SourceData data = new SourceData();
        data.uri = sourceRes.getURI();
        data.name = getStringProperty(om, sourceRes, mch + "nameSource");
        data.date = getStringProperty(om, sourceRes, mch + "date");
        
        Resource typeSourceRes = getObjectProperty(om, sourceRes, mch + "hasTypeSource");
        if (typeSourceRes != null) {
            data.typeSource = getStringProperty(om, typeSourceRes, mch + "nameTypeSource");
        }
        
        return data;
    }
    
    /**
     * Récupère une propriété chaîne de caractères depuis une ressource.
     */
    private static String getStringProperty(Model om, Resource res, String propertyUri) {
        Property prop = om.getProperty(propertyUri);
        if (prop == null) return "";
        
        Statement stmt = om.getProperty(res, prop);
        if (stmt == null) return "";
        
        RDFNode obj = stmt.getObject();
        if (obj.isLiteral()) {
            return obj.asLiteral().getString();
        } else if (obj.isResource()) {
            return obj.asResource().getURI();
        }
        return "";
    }
    
    /**
     * Récupère une propriété objet depuis une ressource.
     */
    private static Resource getObjectProperty(Model om, Resource res, String propertyUri) {
        Property prop = om.getProperty(propertyUri);
        if (prop == null) return null;
        
        Statement stmt = om.getProperty(res, prop);
        if (stmt == null) return null;
        
        RDFNode obj = stmt.getObject();
        if (obj.isResource()) {
            return obj.asResource();
        }
        return null;
    }
    
    // Classes internes pour représenter les données
    
    static class AlternativeData {
        String uri;
        String name;
        String description;
        String image;
        String icon;
        List<ArgumentData> arguments;
    }
    
    static class ArgumentData {
        String uri;
        String assertion;
        String explanation;
        String value;
        String nameCriterion;  // Critère de l'argument
        String nameProperty;   // Propriété de l'argument
        String condition;
        String infValue;
        String supValue;
        String unit;
        String isProspective;
        String date;
        String alternativeName;  // Nom de l'alternative associée
        StakeholderData stakeholder;
        List<SourceData> sources;
    }
    
    static class StakeholderData {
        String uri;
        String name;
    }
    
    static class SourceData {
        String uri;
        String name;
        String date;
        String typeSource;
    }
}
