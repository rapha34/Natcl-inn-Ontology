package ontologyManagement;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.jena.ontology.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;

import natclinn.util.NatclinnConf;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Extrait un projet MyChoice du graphe RDF et l'exporte vers un fichier Excel.
 * Programme inverse de CreateMychoiceAbox.
 * Parcours Project -> hasAlternative -> Alternative -> hasArgument -> Argument.
 */
public class ExtractMychoiceProjectToExcel {

    public static void main(String[] args) {
        new NatclinnConf();
        // String projectURI = null;
        // String aboxFile = null;

        String projectURI = "https://w3id.org/MCH/ontology/Project_NATCL%27INN";
        String aboxFile = NatclinnConf.folderForOntologies + "/MychoiceAbox.xml";

        if (args.length > 0) {
            projectURI = args[0];
            if (args.length > 1) {
                aboxFile = args[1];
            }
        }
        if (projectURI == null) {
            projectURI = "https://w3id.org/MCH/ontology/Project_NatclinnProducts";
            System.out.println("Utilisation du projet par défaut : " + projectURI);
        }
        if (aboxFile == null) {
            aboxFile = NatclinnConf.folderForOntologies + "/MychoiceProjectsFromProducts.xml";
            System.out.println("Utilisation du fichier : " + aboxFile);
        }
        String outputExcel = NatclinnConf.folderForResults + "/ExtractedProject.xlsx";
        try {
            extractProjectToExcel(aboxFile, projectURI, outputExcel);
            System.out.println("Extraction réussie : " + outputExcel);
        } catch (Exception e) {
            System.err.println("Erreur lors de l'extraction : " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void extractProjectToExcel(String aboxFile, String projectURI, String outputExcel) 
            throws FileNotFoundException, IOException {
        
        // Charger le modèle RDF
        OntModel om = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
        try (FileInputStream fis = new FileInputStream(aboxFile)) {
            om.read(fis, null, "RDF/XML");
        }
        
        String mch = "https://w3id.org/MCH/ontology/";
        
        // Récupérer le projet
        Resource projectRes = om.getResource(projectURI);
        if (projectRes == null || !om.contains(projectRes, RDF.type, om.getResource(mch + "Project"))) {
            throw new IllegalArgumentException("Projet non trouvé avec l'URI : " + projectURI);
        }
        
        // Structures pour stocker les données extraites
        Map<String, ProjectData> projects = new HashMap<>();
        Map<String, AlternativeData> alternatives = new HashMap<>();
        Map<String, ArgumentData> arguments = new HashMap<>();
        Set<StakeholderData> stakeholders = new HashSet<>();
        Set<CriterionData> criteria = new HashSet<>();
        Set<PropertyData> properties = new HashSet<>();
        Set<SourceData> sources = new HashSet<>();
        Set<TypeSourceData> typeSources = new HashSet<>();
        Set<HasExpertiseData> hasExpertises = new HashSet<>();
        
        // Extraire les données du projet
        ProjectData projData = extractProject(om, projectRes, mch);
        projects.put(projectURI, projData);
        
        // Récupérer toutes les alternatives liées au projet
        Property belongsToProject = om.getProperty(mch + "belongsToProject");
        StmtIterator altIter = om.listStatements(null, belongsToProject, projectRes);
        while (altIter.hasNext()) {
            Statement stmt = altIter.nextStatement();
            Resource altRes = stmt.getSubject();
            AlternativeData altData = extractAlternative(om, altRes, mch);
            alternatives.put(altRes.getURI(), altData);
        }
        
        // Récupérer tous les arguments liés aux alternatives
        Property hasAlternative = om.getProperty(mch + "hasAlternative");
        for (String altURI : alternatives.keySet()) {
            Resource altRes = om.getResource(altURI);
            StmtIterator argIter = om.listStatements(null, hasAlternative, altRes);
            while (argIter.hasNext()) {
                Statement stmt = argIter.nextStatement();
                Resource argRes = stmt.getSubject();
                ArgumentData argData = extractArgument(om, argRes, mch);
                arguments.put(argRes.getURI(), argData);
                
                // Extraire les entités référencées par l'argument
                if (argData.stakeholder != null) stakeholders.add(argData.stakeholder);
                if (argData.criterion != null) criteria.add(argData.criterion);
                if (argData.property != null) properties.add(argData.property);
                if (argData.source != null) sources.add(argData.source);
                if (argData.typeSource != null) typeSources.add(argData.typeSource);
            }
        }
        
        // Extraire les hasExpertise (stakeholder + criterion)
        StmtIterator heIter = om.listStatements(null, RDF.type, om.getResource(mch + "HasExpertise"));
        while (heIter.hasNext()) {
            Statement stmt = heIter.nextStatement();
            Resource heRes = stmt.getSubject();
            HasExpertiseData heData = extractHasExpertise(om, heRes, mch);
            if (heData != null) {
                hasExpertises.add(heData);
                if (heData.stakeholder != null) stakeholders.add(heData.stakeholder);
                if (heData.criterion != null) criteria.add(heData.criterion);
            }
        }
        
        // Créer le fichier Excel
        writeToExcel(outputExcel, projData, new ArrayList<>(alternatives.values()), 
                     new ArrayList<>(arguments.values()), new ArrayList<>(stakeholders),
                     new ArrayList<>(criteria), new ArrayList<>(properties),
                     new ArrayList<>(sources), new ArrayList<>(typeSources),
                     new ArrayList<>(hasExpertises));
    }
    
    private static ProjectData extractProject(OntModel om, Resource projRes, String mch) {
        ProjectData data = new ProjectData();
        data.uri = projRes.getURI();
        data.name = getStringProperty(om, projRes, mch + "projectName");
        data.description = getStringProperty(om, projRes, mch + "projectDescription");
        data.image = getStringProperty(om, projRes, mch + "projectImage");
        return data;
    }
    
    private static AlternativeData extractAlternative(OntModel om, Resource altRes, String mch) {
        AlternativeData data = new AlternativeData();
        data.uri = altRes.getURI();
        data.name = getStringProperty(om, altRes, mch + "alternativeName");
        data.description = getStringProperty(om, altRes, mch + "alternativeDescription");
        data.image = getStringProperty(om, altRes, mch + "projectImage"); // Réutilisation de projectImage
        return data;
    }
    
    private static ArgumentData extractArgument(OntModel om, Resource argRes, String mch) {
        ArgumentData data = new ArgumentData();
        data.idArgument = extractIdFromURI(argRes.getURI());
        data.assertion = getStringProperty(om, argRes, mch + "assertion");
        data.explanation = getStringProperty(om, argRes, mch + "explanation");
        data.typeProCon = getStringProperty(om, argRes, mch + "typeProCon");
        data.value = getStringProperty(om, argRes, mch + "value");
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
        
        Resource alternativeRes = getObjectProperty(om, argRes, mch + "hasAlternative");
        if (alternativeRes != null) {
            data.alternativeName = getStringProperty(om, alternativeRes, mch + "alternativeName");
        }
        
        Resource criterionRes = getObjectProperty(om, argRes, mch + "hasCriterion");
        if (criterionRes != null) {
            data.criterion = extractCriterion(om, criterionRes, mch);
        }
        
        Resource aimRes = getObjectProperty(om, argRes, mch + "hasAim");
        if (aimRes != null) {
            data.aim = getStringProperty(om, aimRes, mch + "aimDescription");
        }
        
        Resource propertyRes = getObjectProperty(om, argRes, mch + "hasProperty");
        if (propertyRes != null) {
            data.property = extractProperty(om, propertyRes, mch);
        }
        
        Resource sourceRes = getObjectProperty(om, argRes, mch + "hasSource");
        if (sourceRes != null) {
            data.source = extractSource(om, sourceRes, mch);
            
            // TypeSource lié à la source
            Resource typeSourceRes = getObjectProperty(om, sourceRes, mch + "hasTypeSource");
            if (typeSourceRes != null) {
                data.typeSource = extractTypeSource(om, typeSourceRes, mch);
            }
        }
        
        return data;
    }
    
    private static StakeholderData extractStakeholder(OntModel om, Resource shRes, String mch) {
        StakeholderData data = new StakeholderData();
        data.uri = shRes.getURI();
        data.name = getStringProperty(om, shRes, mch + "stakeholderName");
        return data;
    }
    
    private static CriterionData extractCriterion(OntModel om, Resource critRes, String mch) {
        CriterionData data = new CriterionData();
        data.uri = critRes.getURI();
        data.name = getStringProperty(om, critRes, mch + "criterionName");
        return data;
    }
    
    private static PropertyData extractProperty(OntModel om, Resource propRes, String mch) {
        PropertyData data = new PropertyData();
        data.uri = propRes.getURI();
        data.name = getStringProperty(om, propRes, mch + "propertyName");
        return data;
    }
    
    private static SourceData extractSource(OntModel om, Resource srcRes, String mch) {
        SourceData data = new SourceData();
        data.uri = srcRes.getURI();
        data.name = getStringProperty(om, srcRes, mch + "sourceName");
        return data;
    }
    
    private static TypeSourceData extractTypeSource(OntModel om, Resource tsRes, String mch) {
        TypeSourceData data = new TypeSourceData();
        data.uri = tsRes.getURI();
        data.name = getStringProperty(om, tsRes, mch + "sourceName");
        data.fiability = getStringProperty(om, tsRes, mch + "typeFiability");
        return data;
    }
    
    private static HasExpertiseData extractHasExpertise(OntModel om, Resource heRes, String mch) {
        HasExpertiseData data = new HasExpertiseData();
        
        Resource stakeholderRes = getObjectProperty(om, heRes, mch + "hasStakeholder");
        if (stakeholderRes != null) {
            data.stakeholder = extractStakeholder(om, stakeholderRes, mch);
        }
        
        Resource criterionRes = getObjectProperty(om, heRes, mch + "hasCriterion");
        if (criterionRes != null) {
            data.criterion = extractCriterion(om, criterionRes, mch);
        }
        
        return (data.stakeholder != null && data.criterion != null) ? data : null;
    }
    
    private static String getStringProperty(OntModel om, Resource res, String propURI) {
        Statement stmt = om.getProperty(res, om.getProperty(propURI));
        return (stmt != null) ? stmt.getString() : "";
    }
    
    private static Resource getObjectProperty(OntModel om, Resource res, String propURI) {
        Statement stmt = om.getProperty(res, om.getProperty(propURI));
        return (stmt != null && stmt.getObject().isResource()) ? stmt.getResource() : null;
    }
    
    private static String extractIdFromURI(String uri) {
        if (uri == null) return "";
        int lastUnderscore = uri.lastIndexOf('_');
        return (lastUnderscore >= 0) ? uri.substring(lastUnderscore + 1) : uri;
    }
    
    private static void writeToExcel(String outputFile, ProjectData project,
                                     List<AlternativeData> alternatives,
                                     List<ArgumentData> arguments,
                                     List<StakeholderData> stakeholders,
                                     List<CriterionData> criteria,
                                     List<PropertyData> properties,
                                     List<SourceData> sources,
                                     List<TypeSourceData> typeSources,
                                     List<HasExpertiseData> hasExpertises) throws IOException {
        
        try (Workbook workbook = new XSSFWorkbook()) {

            // Feuille Argument (d'abord)
            Sheet argSheet = workbook.createSheet("argument");
            Row headerRow = argSheet.createRow(0);
            headerRow.createCell(0).setCellValue("idArgument");
            headerRow.createCell(1).setCellValue("nameStakeHolder");
            headerRow.createCell(2).setCellValue("nameAlternative");
            headerRow.createCell(3).setCellValue("typeProCon");
            headerRow.createCell(4).setCellValue("nameCriterion");
            headerRow.createCell(5).setCellValue("aim");
            headerRow.createCell(6).setCellValue("nameProperty");
            headerRow.createCell(7).setCellValue("value");
            headerRow.createCell(8).setCellValue("condition");
            headerRow.createCell(9).setCellValue("infValue");
            headerRow.createCell(10).setCellValue("supValue");
            headerRow.createCell(11).setCellValue("unit");
            headerRow.createCell(12).setCellValue("assertion");
            headerRow.createCell(13).setCellValue("explanation");
            headerRow.createCell(14).setCellValue("isProspective");
            headerRow.createCell(15).setCellValue("date");
            headerRow.createCell(16).setCellValue("nameSource");
            headerRow.createCell(17).setCellValue("nameTypeSource");

            int rowIdx = 1;
            for (ArgumentData arg : arguments) {
                Row row = argSheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(arg.idArgument);
                row.createCell(1).setCellValue(arg.stakeholder != null ? arg.stakeholder.name : "");
                row.createCell(2).setCellValue(arg.alternativeName);
                row.createCell(3).setCellValue(arg.typeProCon);
                row.createCell(4).setCellValue(arg.criterion != null ? arg.criterion.name : "");
                row.createCell(5).setCellValue(arg.aim);
                row.createCell(6).setCellValue(arg.property != null ? arg.property.name : "");
                row.createCell(7).setCellValue(arg.value);
                row.createCell(8).setCellValue(arg.condition);
                row.createCell(9).setCellValue(arg.infValue);
                row.createCell(10).setCellValue(arg.supValue);
                row.createCell(11).setCellValue(arg.unit);
                row.createCell(12).setCellValue(arg.assertion);
                row.createCell(13).setCellValue(arg.explanation);
                row.createCell(14).setCellValue(arg.isProspective);
                row.createCell(15).setCellValue(arg.date);
                row.createCell(16).setCellValue(arg.source != null ? arg.source.name : "");
                row.createCell(17).setCellValue(arg.typeSource != null ? arg.typeSource.name : "");
            }

            // Feuille Project (ensuite)
            Sheet projectSheet = workbook.createSheet("project");
            headerRow = projectSheet.createRow(0);
            headerRow.createCell(0).setCellValue("nameProject");
            headerRow.createCell(1).setCellValue("description");
            headerRow.createCell(2).setCellValue("image");

            Row dataRow = projectSheet.createRow(1);
            dataRow.createCell(0).setCellValue(project.name);
            dataRow.createCell(1).setCellValue(project.description);
            dataRow.createCell(2).setCellValue(project.image);
            
            // Feuille Alternative
            Sheet altSheet = workbook.createSheet("alternative");
            headerRow = altSheet.createRow(0);
            headerRow.createCell(0).setCellValue("nameAlternative");
            headerRow.createCell(1).setCellValue("description");
            headerRow.createCell(2).setCellValue("imageAlternative");
            headerRow.createCell(3).setCellValue("iconAlternative");
            
            rowIdx = 1;
            for (AlternativeData alt : alternatives) {
                Row row = altSheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(alt.name);
                row.createCell(1).setCellValue(alt.description);
                row.createCell(2).setCellValue(alt.image);
                row.createCell(3).setCellValue(""); // iconAlternative
            }
            
            // Feuille TypeSource
            Sheet tsSheet = workbook.createSheet("typesource");
            headerRow = tsSheet.createRow(0);
            headerRow.createCell(0).setCellValue("nameTypeSource");
            headerRow.createCell(1).setCellValue("fiability");
            
            rowIdx = 1;
            for (TypeSourceData ts : typeSources) {
                Row row = tsSheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(ts.name);
                row.createCell(1).setCellValue(ts.fiability);
            }
            
            // Feuille HasExpertise
            Sheet heSheet = workbook.createSheet("hasexpertise");
            headerRow = heSheet.createRow(0);
            headerRow.createCell(0).setCellValue("nameStakeHolder");
            headerRow.createCell(1).setCellValue("nameCriterion");
            
            rowIdx = 1;
            for (HasExpertiseData he : hasExpertises) {
                Row row = heSheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(he.stakeholder != null ? he.stakeholder.name : "");
                row.createCell(1).setCellValue(he.criterion != null ? he.criterion.name : "");
            }
            
            // Assurer l'ordre final des onglets (argument, project, alternative, typesource, hasexpertise)
            workbook.setSheetOrder("argument", 0);
            workbook.setSheetOrder("project", 1);
            workbook.setSheetOrder("alternative", 2);
            workbook.setSheetOrder("typesource", 3);
            workbook.setSheetOrder("hasexpertise", 4);
            
            // Écrire le fichier
            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                workbook.write(fos);
            }
        }
    }
    
    // Classes de données internes
    static class ProjectData {
        String uri, name, description, image;
    }
    
    static class AlternativeData {
        String uri, name, description, image;
    }
    
    static class ArgumentData {
        String idArgument, alternativeName, typeProCon, value, condition;
        String infValue, supValue, unit, assertion, explanation;
        String isProspective, date, aim;
        StakeholderData stakeholder;
        CriterionData criterion;
        PropertyData property;
        SourceData source;
        TypeSourceData typeSource;
    }
    
    static class StakeholderData {
        String uri, name;
        @Override
        public int hashCode() { return uri != null ? uri.hashCode() : 0; }
        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof StakeholderData)) return false;
            return uri != null && uri.equals(((StakeholderData) obj).uri);
        }
    }
    
    static class CriterionData {
        String uri, name;
        @Override
        public int hashCode() { return uri != null ? uri.hashCode() : 0; }
        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof CriterionData)) return false;
            return uri != null && uri.equals(((CriterionData) obj).uri);
        }
    }
    
    static class PropertyData {
        String uri, name;
        @Override
        public int hashCode() { return uri != null ? uri.hashCode() : 0; }
        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof PropertyData)) return false;
            return uri != null && uri.equals(((PropertyData) obj).uri);
        }
    }
    
    static class SourceData {
        String uri, name;
        @Override
        public int hashCode() { return uri != null ? uri.hashCode() : 0; }
        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof SourceData)) return false;
            return uri != null && uri.equals(((SourceData) obj).uri);
        }
    }
    
    static class TypeSourceData {
        String uri, name, fiability;
        @Override
        public int hashCode() { return uri != null ? uri.hashCode() : 0; }
        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof TypeSourceData)) return false;
            return uri != null && uri.equals(((TypeSourceData) obj).uri);
        }
    }
    
    static class HasExpertiseData {
        StakeholderData stakeholder;
        CriterionData criterion;
        @Override
        public int hashCode() {
            return (stakeholder != null ? stakeholder.hashCode() : 0) 
                 + (criterion != null ? criterion.hashCode() : 0);
        }
        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof HasExpertiseData)) return false;
            HasExpertiseData other = (HasExpertiseData) obj;
            return (stakeholder != null && stakeholder.equals(other.stakeholder))
                && (criterion != null && criterion.equals(other.criterion));
        }
    }
}
