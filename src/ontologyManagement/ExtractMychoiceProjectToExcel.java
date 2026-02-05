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

import org.apache.jena.base.Sys;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;

import natclinn.util.NatclinnConf;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Extrait un projet MyChoice du graphe RDF et l'exporte vers un fichier Excel.
 * Programme inverse de CreateMychoiceAbox.
 */
public class ExtractMychoiceProjectToExcel {

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
        
        // Générer le nom du fichier Excel à partir du nom du fichier d'entrée
        String inputFileName = new java.io.File(aboxFile).getName();
        String baseName = inputFileName.replaceFirst("[.][^.]+$", ""); // Retire l'extension
        String outputExcel = NatclinnConf.folderForResults + "/" + baseName + ".xlsx";
        
        try {
            extractProjectToExcel(aboxFile, outputExcel);
            System.out.println("Extraction réussie : " + outputExcel);
        } catch (Exception e) {
            System.err.println("Erreur lors de l'extraction : " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void extractProjectToExcel(String aboxFile, String outputExcel) 
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
            System.out.println("AVERTISSEMENT : Plusieurs projets trouvés dans le fichier. Seul le premier sera extrait.");
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
        Set<ProductData> products = new HashSet<>();
        List<CompositionData> compositions = new ArrayList<>();
        
        // Extraire les données du projet
        ProjectData projData = extractProject(om, projectRes, mch);
        projects.put(projectURI, projData);
        
        // Récupérer toutes les alternatives liées au projet (Alternative -> hasProject -> Project)
        Property hasProject = om.getProperty(mch + "hasProject");
        Property relatedToProduct = om.getProperty(mch + "relatedToProduct");
        String ncl = "https://w3id.org/NCL/ontology/";
        String skos = "http://www.w3.org/2004/02/skos/core#";
        Property hasTag = om.getProperty(ncl + "hasTag");
        Property hasTagCheck = om.getProperty(ncl + "hasTagCheck");
        Property hasRole = om.getProperty(ncl + "hasRole");
        Property hasQuantifiedElement = om.getProperty(ncl + "hasQuantifiedElement");
        Property refersTo = om.getProperty(ncl + "refersTo");
        Property rank = om.getProperty(ncl + "rank");
        Property prefLabel = om.getProperty(skos + "prefLabel");
        
        StmtIterator altIter = om.listStatements(null, hasProject, projectRes);
        while (altIter.hasNext()) {
            Statement stmt = altIter.nextStatement();
            Resource altRes = stmt.getSubject();
            AlternativeData altData = extractAlternative(om, altRes, mch);
            alternatives.put(altRes.getURI(), altData);
            
            // Extraire les produits et tags liés à cette alternative
            if (om.contains(altRes, relatedToProduct, (RDFNode)null)) {
                Resource productRes = getObjectProperty(om, altRes, mch + "relatedToProduct");
                if (productRes != null) {
                    String productUri = productRes.getURI();
                    String productName = "";
                    if (om.contains(productRes, prefLabel, (RDFNode)null)) {
                        productName = om.getProperty(productRes, prefLabel).getString();
                    }
                    
                    // Récupérer tous les tags du produit (hasTag et hasTagCheck)
                    StmtIterator tagIter = om.listStatements(productRes, hasTag, (RDFNode)null);
                    while (tagIter.hasNext()) {
                        Statement tagStmt = tagIter.nextStatement();
                        if (tagStmt.getObject().isResource()) {
                            Resource tagRes = tagStmt.getResource();
                            String tagLabel = "";
                            if (om.contains(tagRes, prefLabel, (RDFNode)null)) {
                                tagLabel = om.getProperty(tagRes, prefLabel).getString();
                            }
                            
                            ProductData prodData = new ProductData();
                            prodData.nameAlternative = altData.name;
                            prodData.productUri = productUri;
                            prodData.nameProduct = productName;
                            prodData.tagProduct = tagLabel;
                            products.add(prodData);
                        }
                    }
                    
                    // Récupérer aussi les tags via hasTagCheck
                    StmtIterator tagCheckIter = om.listStatements(productRes, hasTagCheck, (RDFNode)null);
                    while (tagCheckIter.hasNext()) {
                        Statement tagStmt = tagCheckIter.nextStatement();
                        if (tagStmt.getObject().isResource()) {
                            Resource tagRes = tagStmt.getResource();
                            String tagLabel = "";
                            if (om.contains(tagRes, prefLabel, (RDFNode)null)) {
                                tagLabel = om.getProperty(tagRes, prefLabel).getString();
                            }
                            
                            ProductData prodData = new ProductData();
                            prodData.nameAlternative = altData.name;
                            prodData.productUri = productUri;
                            prodData.nameProduct = productName;
                            prodData.tagProduct = tagLabel;
                            products.add(prodData);
                        }
                    }
                    
                    // Récupérer toute la composition via hasQuantifiedElement (produits + ingrédients)
                    collectCompositions(om, productRes, compositions, hasQuantifiedElement, refersTo, rank,
                                        hasTag, hasTagCheck, hasRole, prefLabel, ncl);
                }
            }
        }
        
        // Récupérer tous les arguments liés au projet (Argument -> belongsToProject -> Project)
        Property belongsToProject = om.getProperty(mch + "belongsToProject");
        StmtIterator argIter = om.listStatements(null, belongsToProject, projectRes);
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
                 new ArrayList<>(hasExpertises), new ArrayList<>(products),
                 compositions);
    }
    
    private static ProjectData extractProject(Model om, Resource projRes, String mch) {
        ProjectData data = new ProjectData();
        data.uri = projRes.getURI();
        data.name = getStringProperty(om, projRes, mch + "projectName");
        data.description = getStringProperty(om, projRes, mch + "projectDescription");
        data.image = getStringProperty(om, projRes, mch + "projectImage");
        return data;
    }
    
    private static AlternativeData extractAlternative(Model om, Resource altRes, String mch) {
        AlternativeData data = new AlternativeData();
        data.uri = altRes.getURI();
        data.name = getStringProperty(om, altRes, mch + "nameAlternative");
        data.description = getStringProperty(om, altRes, mch + "alternativeDescription");
        data.image = getStringProperty(om, altRes, mch + "imageAlternative");
        data.icon = getStringProperty(om, altRes, mch + "iconAlternative");
        return data;
    }
    
    private static ArgumentData extractArgument(Model om, Resource argRes, String mch) {
        ArgumentData data = new ArgumentData();
        data.idArgument = extractIdFromURI(argRes.getURI());
        data.assertion = getStringProperty(om, argRes, mch + "assertion");
        data.explanation = getStringProperty(om, argRes, mch + "explanation");
        
        // typeProCon = polarity (priorité à polarity)
        String polarity = getStringProperty(om, argRes, mch + "polarity");
        String typeProCon = getStringProperty(om, argRes, mch + "typeProCon");
        data.typeProCon = !polarity.isEmpty() ? polarity : typeProCon;
        
        // Normaliser les tirets : remplacer les tirets longs (–, —) par des tirets courts (-)
        if (!data.typeProCon.isEmpty()) {
            data.typeProCon = data.typeProCon.replace("–", "-").replace("—", "-");
        }
        
        // value = valueProperty (priorité à valueProperty)
        String valueProperty = getStringProperty(om, argRes, mch + "valueProperty");
        String value = getStringProperty(om, argRes, mch + "value");
        data.value = !valueProperty.isEmpty() ? valueProperty : value;
        
        data.condition = getStringProperty(om, argRes, mch + "condition");
        data.infValue = getStringProperty(om, argRes, mch + "infValue");
        data.supValue = getStringProperty(om, argRes, mch + "supValue");
        data.unit = getStringProperty(om, argRes, mch + "unit");
        data.isProspective = getStringProperty(om, argRes, mch + "isProspective");
        data.date = getStringProperty(om, argRes, mch + "evaluationDate");
        data.tagInitiator = getStringProperty(om, argRes, mch + "tagInitiator");
        
        // Relations
        Resource stakeholderRes = getObjectProperty(om, argRes, mch + "hasStakeholder");
        if (stakeholderRes != null) {
            data.stakeholder = extractStakeholder(om, stakeholderRes, mch);
        }
        
        // nameAlternatives : récupérer toutes les alternatives liées via hasAlternative
        Property hasAlternative = om.getProperty(mch + "hasAlternative");
        StmtIterator altStmtIter = om.listStatements(argRes, hasAlternative, (RDFNode) null);
        while (altStmtIter.hasNext()) {
            Statement altStmt = altStmtIter.nextStatement();
            if (altStmt.getObject().isResource()) {
                Resource alternativeRes = altStmt.getResource();
                String altName = getStringProperty(om, alternativeRes, mch + "nameAlternative");
                if (!altName.isEmpty()) {
                    data.nameAlternatives.add(altName);
                }
            }
        }
        
        // nameCriterion = nameCriterionStr (priorité à nameCriterionStr depuis mch:nameCriterion)
        String nameCriterionStr = getStringProperty(om, argRes, mch + "nameCriterion");
        Resource criterionRes = getObjectProperty(om, argRes, mch + "hasCriterion");
        if (!nameCriterionStr.isEmpty()) {
            data.nameCriterionStr = nameCriterionStr;
        }
        if (criterionRes != null) {
            data.criterion = extractCriterion(om, criterionRes, mch);
        }
        
        // aim = aimStr (priorité à aimStr depuis mch:aim)
        String aimStr = getStringProperty(om, argRes, mch + "aim");
        Resource aimRes = getObjectProperty(om, argRes, mch + "hasAim");
        if (!aimStr.isEmpty()) {
            data.aim = aimStr;
        } else if (aimRes != null) {
            data.aim = getStringProperty(om, aimRes, mch + "aimDescription");
        }
        
        // nameProperty = namePropertyStr (priorité à namePropertyStr depuis mch:nameProperty)
        String namePropertyStr = getStringProperty(om, argRes, mch + "nameProperty");
        Resource propertyRes = getObjectProperty(om, argRes, mch + "hasProperty");
        if (!namePropertyStr.isEmpty()) {
            data.nameProperty = namePropertyStr;
        }
        if (propertyRes != null) {
            data.property = extractProperty(om, propertyRes, mch);
        }
        
        Resource sourceRes = getObjectProperty(om, argRes, mch + "hasSource");
        if (sourceRes != null) {
            data.source = extractSource(om, sourceRes, mch);
            // Date liée à la source
            String dateSource = getStringProperty(om, sourceRes, mch + "date");
            if (!dateSource.isEmpty()) {
                data.date = dateSource;
            }
            // TypeSource lié à la source
            Resource typeSourceRes = getObjectProperty(om, sourceRes, mch + "hasTypeSource");
            if (typeSourceRes != null) {
                data.typeSource = extractTypeSource(om, typeSourceRes, mch);
            }
        }
        
        return data;
    }
    
    private static StakeholderData extractStakeholder(Model om, Resource shRes, String mch) {
        StakeholderData data = new StakeholderData();
        data.uri = shRes.getURI();
        data.name = getStringProperty(om, shRes, mch + "stakeholderName");
        return data;
    }
    
    private static CriterionData extractCriterion(Model om, Resource critRes, String mch) {
        CriterionData data = new CriterionData();
        data.uri = critRes.getURI();
        data.name = getStringProperty(om, critRes, mch + "criterionName");
        return data;
    }
    
    private static PropertyData extractProperty(Model om, Resource propRes, String mch) {
        PropertyData data = new PropertyData();
        data.uri = propRes.getURI();
        data.name = getStringProperty(om, propRes, mch + "propertyName");
        return data;
    }
    
    private static SourceData extractSource(Model om, Resource srcRes, String mch) {
        SourceData data = new SourceData();
        data.uri = srcRes.getURI();
        data.name = getStringProperty(om, srcRes, mch + "sourceName");
        data.date = getStringProperty(om, srcRes, mch + "date");
        return data;
    }
    
    private static TypeSourceData extractTypeSource(Model om, Resource tsRes, String mch) {
        TypeSourceData data = new TypeSourceData();
        data.uri = tsRes.getURI();
        // TypeSource : récupérer typeSourceName (pas de prefLabel nécessaire)
        data.name = getStringProperty(om, tsRes, mch + "typeSourceName");
        data.fiability = getStringProperty(om, tsRes, mch + "typeSourceFiability");
        return data;
    }
    
    private static HasExpertiseData extractHasExpertise(Model om, Resource heRes, String mch) {
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

    private static void collectCompositions(Model om, Resource startCompound, List<CompositionData> compositions,
                                            Property hasQuantifiedElement, Property refersTo, Property rank,
                                            Property hasTag, Property hasTagCheck, Property hasRole, Property prefLabel,
                                            String ncl) {
        if (startCompound == null) return;

        Set<String> visited = new HashSet<>();
        List<Resource> queue = new ArrayList<>();
        queue.add(startCompound);

        while (!queue.isEmpty()) {
            Resource compound = queue.remove(0);
            if (compound == null || compound.getURI() == null) continue;
            if (!visited.add(compound.getURI())) continue;

            String compoundUri = compound.getURI();
            String compoundName = getPrefLabel(om, compound, prefLabel);

            StmtIterator qIter = om.listStatements(compound, hasQuantifiedElement, (RDFNode) null);
            while (qIter.hasNext()) {
                Statement qStmt = qIter.nextStatement();
                if (!qStmt.getObject().isResource()) continue;
                Resource qRes = qStmt.getResource();

                Resource component = getObjectProperty(om, qRes, refersTo.getURI());
                if (component == null || component.getURI() == null) continue;

                String componentUri = component.getURI();
                String componentName = getPrefLabel(om, component, prefLabel);
                String componentType = getComponentType(om, component, ncl);
                String rankVal = getLiteralProperty(om, qRes, rank);

                List<String> tags = getComponentTags(om, component, componentType, hasTag, hasTagCheck, hasRole, prefLabel);
                if (tags.isEmpty()) {
                    CompositionData comp = new CompositionData();
                    comp.uriCompose = compoundUri;
                    comp.nameCompose = compoundName;
                    comp.typeComposant = componentType;
                    comp.uriComposant = componentUri;
                    comp.nameComposant = componentName;
                    comp.rank = rankVal;
                    comp.tagComposant = "";
                    compositions.add(comp);
                } else {
                    for (String tag : tags) {
                        CompositionData comp = new CompositionData();
                        comp.uriCompose = compoundUri;
                        comp.nameCompose = compoundName;
                        comp.typeComposant = componentType;
                        comp.uriComposant = componentUri;
                        comp.nameComposant = componentName;
                        comp.rank = rankVal;
                        comp.tagComposant = tag;
                        compositions.add(comp);
                    }
                }

                if (om.contains(component, hasQuantifiedElement, (RDFNode) null)) {
                    queue.add(component);
                }
            }
        }
    }

    private static String getComponentType(Model om, Resource res, String ncl) {
        if (hasType(om, res, ncl + "Product")
                || hasType(om, res, ncl + "SimpleProduct")
                || hasType(om, res, ncl + "CompositeProduct")) {
            return "Product";
        }
        if (hasType(om, res, ncl + "Ingredient")
                || hasType(om, res, ncl + "SimpleIngredient")
                || hasType(om, res, ncl + "CompositeIngredient")) {
            return "Ingredient";
        }
        return "";
    }

    private static boolean hasType(Model om, Resource res, String typeUri) {
        return om.contains(res, RDF.type, om.getResource(typeUri));
    }

    private static String getPrefLabel(Model om, Resource res, Property prefLabel) {
        if (res == null || prefLabel == null) return "";
        Statement stmt = om.getProperty(res, prefLabel);
        return (stmt != null) ? stmt.getString() : "";
    }

    private static String getLiteralProperty(Model om, Resource res, Property prop) {
        if (res == null || prop == null) return "";
        Statement stmt = om.getProperty(res, prop);
        if (stmt != null && stmt.getObject().isLiteral()) {
            Literal lit = stmt.getLiteral();
            return lit.getLexicalForm();
        }
        return "";
    }

    private static List<String> getComponentTags(Model om, Resource component, String componentType,
                                                 Property hasTag, Property hasTagCheck, Property hasRole,
                                                 Property prefLabel) {
        List<String> tags = new ArrayList<>();
        if (component == null) return tags;

        if ("Product".equals(componentType)) {
            StmtIterator tagIter = om.listStatements(component, hasTag, (RDFNode) null);
            while (tagIter.hasNext()) {
                Statement tagStmt = tagIter.nextStatement();
                if (tagStmt.getObject().isResource()) {
                    String tagLabel = getPrefLabel(om, tagStmt.getResource(), prefLabel);
                    if (!tagLabel.isEmpty()) tags.add(tagLabel);
                }
            }
            StmtIterator tagCheckIter = om.listStatements(component, hasTagCheck, (RDFNode) null);
            while (tagCheckIter.hasNext()) {
                Statement tagStmt = tagCheckIter.nextStatement();
                if (tagStmt.getObject().isResource()) {
                    String tagLabel = getPrefLabel(om, tagStmt.getResource(), prefLabel);
                    if (!tagLabel.isEmpty()) tags.add(tagLabel);
                }
            }
        } else if ("Ingredient".equals(componentType)) {
            StmtIterator roleIter = om.listStatements(component, hasRole, (RDFNode) null);
            while (roleIter.hasNext()) {
                Statement roleStmt = roleIter.nextStatement();
                if (roleStmt.getObject().isResource()) {
                    String tagLabel = getPrefLabel(om, roleStmt.getResource(), prefLabel);
                    if (!tagLabel.isEmpty()) tags.add(tagLabel);
                }
            }
        }

        return tags;
    }
    
    private static String getStringProperty(Model om, Resource res, String propURI) {
        Statement stmt = om.getProperty(res, om.getProperty(propURI));
        return (stmt != null) ? stmt.getString() : "";
    }
    
    private static Resource getObjectProperty(Model om, Resource res, String propURI) {
        Statement stmt = om.getProperty(res, om.getProperty(propURI));
        return (stmt != null && stmt.getObject().isResource()) ? stmt.getResource() : null;
    }
    
    private static String extractIdFromURI(String uri) {
        if (uri == null) return "";
        int lastUnderscore = uri.lastIndexOf('-');
        return (lastUnderscore >= 0) ? uri.substring(lastUnderscore + 1) : uri;
    }
    
    /**
     * Remplit une ligne d'argument avec toutes les cellules (sauf idArgument, nameStakeHolder, nameAlternative, typeProCon déjà remplies)
     */
    private static void fillArgumentRow(Row row, ArgumentData arg) {
        // nameCriterion : priorité à la string directe, sinon nom du Criterion objet
        String nameCriterion = !arg.nameCriterionStr.isEmpty() ? arg.nameCriterionStr : 
                       (arg.criterion != null ? arg.criterion.name : "");
        row.createCell(4).setCellValue(nameCriterion);
        row.createCell(5).setCellValue(arg.aim);
        // nameProperty : priorité à la string directe, sinon nom du Property objet
        String nameProperty = !arg.nameProperty.isEmpty() ? arg.nameProperty : 
                              (arg.property != null ? arg.property.name : "");
        row.createCell(6).setCellValue(nameProperty);
        row.createCell(7).setCellValue(arg.value);
        row.createCell(8).setCellValue(arg.condition);
        row.createCell(9).setCellValue(arg.infValue);
        row.createCell(10).setCellValue(arg.supValue);
        row.createCell(11).setCellValue(arg.unit);
        row.createCell(12).setCellValue(arg.assertion);
        row.createCell(13).setCellValue(arg.explanation);
        row.createCell(14).setCellValue(arg.isProspective);
        row.createCell(15).setCellValue(arg.source != null ? arg.source.date : "");
        row.createCell(16).setCellValue(arg.source != null ? arg.source.name : "");
        row.createCell(17).setCellValue(arg.typeSource != null ? arg.typeSource.name : "");
    }
    
    private static void writeToExcel(String outputFile, ProjectData project,
                                     List<AlternativeData> alternatives,
                                     List<ArgumentData> arguments,
                                     List<StakeholderData> stakeholders,
                                     List<CriterionData> criteria,
                                     List<PropertyData> properties,
                                     List<SourceData> sources,
                                     List<TypeSourceData> typeSources,
                                     List<HasExpertiseData> hasExpertises,
                                     List<ProductData> products,
                                     List<CompositionData> compositions) throws IOException {
        
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
            headerRow.createCell(18).setCellValue("tagInitiator");

            int rowIdx = 1;
            int idCounter = 1; // Compteur global, s'incrémente à chaque ligne écrite
            for (ArgumentData arg : arguments) {
                // Si l'argument a plusieurs alternatives, créer une ligne par alternative
                // Sinon créer une seule ligne (même si liste vide)
                if (arg.nameAlternatives.isEmpty()) {
                    // Pas d'alternative liée
                    Row row = argSheet.createRow(rowIdx++);
                    row.createCell(0).setCellValue(idCounter++);
                    row.createCell(1).setCellValue(arg.stakeholder != null ? arg.stakeholder.name : "");
                    row.createCell(2).setCellValue("");
                    row.createCell(3).setCellValue(arg.typeProCon);
                     row.createCell(18).setCellValue(arg.tagInitiator);
                    fillArgumentRow(row, arg);
                } else {
                    // Une ligne par alternative
                    for (String altName : arg.nameAlternatives) {
                        Row row = argSheet.createRow(rowIdx++);
                        row.createCell(0).setCellValue(idCounter++);
                        row.createCell(1).setCellValue(arg.stakeholder != null ? arg.stakeholder.name : "");
                        row.createCell(2).setCellValue(altName);
                        row.createCell(3).setCellValue(arg.typeProCon);
                        row.createCell(18).setCellValue(arg.tagInitiator);
                        fillArgumentRow(row, arg);
                    }
                }
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

            // Feuille Alternative (triée par iconAlternative croissant)
            Sheet altSheet = workbook.createSheet("alternative");
            headerRow = altSheet.createRow(0);
            headerRow.createCell(0).setCellValue("nameAlternative");
            headerRow.createCell(1).setCellValue("description");
            headerRow.createCell(2).setCellValue("imageAlternative");
            headerRow.createCell(3).setCellValue("iconAlternative");

            // Trier la liste des alternatives par iconAlternative croissant
            alternatives.sort((a, b) -> {
                if (a.icon == null && b.icon == null) return 0;
                if (a.icon == null) return -1;
                if (b.icon == null) return 1;
                return a.icon.compareTo(b.icon);
            });

            rowIdx = 1;
            for (AlternativeData alt : alternatives) {
                Row row = altSheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(alt.name);
                row.createCell(1).setCellValue(alt.description);
                row.createCell(2).setCellValue(alt.image);
                row.createCell(3).setCellValue(alt.icon);
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
                // Convertir fiability en entier
                try {
                    if (!ts.fiability.isEmpty()) {
                        double fiabilityValue = Double.parseDouble(ts.fiability);
                        row.createCell(1).setCellValue((int) fiabilityValue);
                    } else {
                        row.createCell(1).setCellValue("");
                    }
                } catch (NumberFormatException e) {
                    row.createCell(1).setCellValue(ts.fiability); // Garder la valeur string si conversion échoue
                }
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

            // Feuille Product (tags agrégés)
            Sheet productSheet = workbook.createSheet("product");
            headerRow = productSheet.createRow(0);
            headerRow.createCell(0).setCellValue("nameAlternative");
            headerRow.createCell(1).setCellValue("productUri");
            headerRow.createCell(2).setCellValue("nameProduct");
            headerRow.createCell(3).setCellValue("tagProduct");

            Map<String, ProductAgg> productAggMap = new HashMap<>();
            for (ProductData prod : products) {
                String key = (prod.nameAlternative != null ? prod.nameAlternative : "") + "||" +
                             (prod.productUri != null ? prod.productUri : "");
                ProductAgg agg = productAggMap.computeIfAbsent(key, k -> {
                    ProductAgg pa = new ProductAgg();
                    pa.nameAlternative = prod.nameAlternative;
                    pa.productUri = prod.productUri;
                    pa.nameProduct = prod.nameProduct;
                    return pa;
                });
                if (prod.tagProduct != null && !prod.tagProduct.isEmpty()) {
                    agg.tags.add(prod.tagProduct);
                }
            }

            rowIdx = 1;
            for (ProductAgg agg : productAggMap.values()) {
                Row row = productSheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(agg.nameAlternative);
                row.createCell(1).setCellValue(agg.productUri);
                row.createCell(2).setCellValue(agg.nameProduct);
                row.createCell(3).setCellValue(joinTags(agg.tags));
            }

            // Feuille Composition (tags agrégés)
            Sheet compositionSheet = workbook.createSheet("composition");
            headerRow = compositionSheet.createRow(0);
            headerRow.createCell(0).setCellValue("uriCompose");
            headerRow.createCell(1).setCellValue("nameCompose");
            headerRow.createCell(2).setCellValue("typeComposant");
            headerRow.createCell(3).setCellValue("uriComposant");
            headerRow.createCell(4).setCellValue("nameComposant");
            headerRow.createCell(5).setCellValue("Rang");
            headerRow.createCell(6).setCellValue("tagComposant");

            Map<String, CompositionAgg> compositionAggMap = new HashMap<>();
            for (CompositionData comp : compositions) {
                String key = (comp.uriCompose != null ? comp.uriCompose : "") + "||" +
                             (comp.uriComposant != null ? comp.uriComposant : "") + "||" +
                             (comp.rank != null ? comp.rank : "");
                CompositionAgg agg = compositionAggMap.computeIfAbsent(key, k -> {
                    CompositionAgg ca = new CompositionAgg();
                    ca.uriCompose = comp.uriCompose;
                    ca.nameCompose = comp.nameCompose;
                    ca.typeComposant = comp.typeComposant;
                    ca.uriComposant = comp.uriComposant;
                    ca.nameComposant = comp.nameComposant;
                    ca.rank = comp.rank;
                    return ca;
                });
                if (comp.tagComposant != null && !comp.tagComposant.isEmpty()) {
                    agg.tags.add(comp.tagComposant);
                }
            }

            List<CompositionAgg> orderedCompositions = orderCompositions(new ArrayList<>(compositionAggMap.values()));

            rowIdx = 1;
            for (CompositionAgg agg : orderedCompositions) {
                Row row = compositionSheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(agg.uriCompose);
                row.createCell(1).setCellValue(agg.nameCompose);
                row.createCell(2).setCellValue(agg.typeComposant);
                row.createCell(3).setCellValue(agg.uriComposant);
                row.createCell(4).setCellValue(agg.nameComposant);
                row.createCell(5).setCellValue(agg.rank);
                row.createCell(6).setCellValue(joinTags(agg.tags));
            }

            // Assurer l'ordre final des onglets (argument, project, alternative, typesource, hasexpertise, product, composition)
            workbook.setSheetOrder("argument", 0);
            workbook.setSheetOrder("project", 1);
            workbook.setSheetOrder("alternative", 2);
            workbook.setSheetOrder("typesource", 3);
            workbook.setSheetOrder("hasexpertise", 4);
            workbook.setSheetOrder("product", 5);
            workbook.setSheetOrder("composition", 6);

            // Auto-ajuster la largeur des colonnes pour toutes les feuilles
            autoSizeColumns(argSheet);
            autoSizeColumns(projectSheet);
            autoSizeColumns(altSheet);
            autoSizeColumns(tsSheet);
            autoSizeColumns(heSheet);
            autoSizeColumns(productSheet);
            autoSizeColumns(compositionSheet);

            // Écrire le fichier
            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                workbook.write(fos);
            }
        }
    }
    
    /**
     * Ajuste automatiquement la largeur de toutes les colonnes d'une feuille
     * en fonction du contenu, avec une largeur maximale pour éviter les colonnes trop larges.
     */
    private static void autoSizeColumns(Sheet sheet) {
        if (sheet.getPhysicalNumberOfRows() > 0) {
            Row headerRow = sheet.getRow(0);
            if (headerRow != null) {
                int numberOfColumns = headerRow.getPhysicalNumberOfCells();
                for (int i = 0; i < numberOfColumns; i++) {
                    sheet.autoSizeColumn(i);
                    // Limiter la largeur maximale à 100 caractères (environ 25600 unités)
                    int currentWidth = sheet.getColumnWidth(i);
                    int maxWidth = 25600; // ~100 caractères
                    if (currentWidth > maxWidth) {
                        sheet.setColumnWidth(i, maxWidth);
                    }
                    // Ajouter un peu de padding (5% de plus)
                    int newWidth = Math.min(sheet.getColumnWidth(i) + 512, maxWidth);
                    sheet.setColumnWidth(i, newWidth);
                }
            }
        }
    }

    private static String joinTags(Set<String> tags) {
        if (tags == null || tags.isEmpty()) return "";
        List<String> sorted = new ArrayList<>(tags);
        sorted.removeIf(t -> t == null || t.isEmpty());
        sorted.sort(String.CASE_INSENSITIVE_ORDER);
        StringBuilder sb = new StringBuilder();
        for (String tag : sorted) {
            if (tag == null || tag.isEmpty()) continue;
            if (sb.length() > 0) sb.append(", ");
            sb.append(tag);
        }
        return sb.toString();
    }

    private static List<CompositionAgg> orderCompositions(List<CompositionAgg> compositions) {
        Map<String, List<CompositionAgg>> byCompose = new HashMap<>();
        Set<String> componentUris = new HashSet<>();
        Set<String> composeUris = new HashSet<>();

        for (CompositionAgg comp : compositions) {
            if (comp.uriCompose != null) {
                composeUris.add(comp.uriCompose);
                byCompose.computeIfAbsent(comp.uriCompose, k -> new ArrayList<>()).add(comp);
            }
            // Enregistrer TOUS les composants (Product ET Ingredient) qui sont référencés
            if (comp.uriComposant != null) {
                componentUris.add(comp.uriComposant);
            }
        }

        // Trier chaque groupe par rang
        for (List<CompositionAgg> list : byCompose.values()) {
            list.sort((a, b) -> compareRankThenName(a, b));
        }

        // Racines = compose qui ne sont pas des composants d'un autre compose
        List<String> roots = new ArrayList<>();
        for (String uri : composeUris) {
            if (!componentUris.contains(uri)) {
                roots.add(uri);
            }
        }
        roots.sort(String.CASE_INSENSITIVE_ORDER);

        List<CompositionAgg> ordered = new ArrayList<>();
        Set<String> visitedCompose = new HashSet<>();

        for (String root : roots) {
            appendCompositionTree(root, byCompose, visitedCompose, ordered);
        }

        // Ajouter tout compose restant (si cycles ou racines non détectées)
        for (String uri : composeUris) {
            if (!visitedCompose.contains(uri)) {
                appendCompositionTree(uri, byCompose, visitedCompose, ordered);
            }
        }

        return ordered;
    }

    private static void appendCompositionTree(String composeUri,
                                              Map<String, List<CompositionAgg>> byCompose,
                                              Set<String> visitedCompose,
                                              List<CompositionAgg> ordered) {
        if (composeUri == null || visitedCompose.contains(composeUri)) return;
        visitedCompose.add(composeUri);

        List<CompositionAgg> children = byCompose.get(composeUri);
        if (children == null) return;

        for (CompositionAgg child : children) {
            ordered.add(child);
            // Vérifier si ce composant est lui-même un produit composite (a des enfants)
            if (child.uriComposant != null && byCompose.containsKey(child.uriComposant)) {
                appendCompositionTree(child.uriComposant, byCompose, visitedCompose, ordered);
            }
        }
    }

    private static int compareRankThenName(CompositionAgg a, CompositionAgg b) {
        int ra = parseRank(a.rank);
        int rb = parseRank(b.rank);
        if (ra != rb) return Integer.compare(ra, rb);
        String na = a.nameComposant != null ? a.nameComposant : "";
        String nb = b.nameComposant != null ? b.nameComposant : "";
        return na.compareToIgnoreCase(nb);
    }

    private static int parseRank(String rank) {
        if (rank == null || rank.trim().isEmpty()) return Integer.MAX_VALUE;
        try {
            double d = Double.parseDouble(rank.trim());
            return (int) Math.round(d);
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }
    
    // Classes de données internes
    static class ProjectData {
        String uri, name, description, image;
    }
    
    static class AlternativeData {
        String uri, name, description, image, icon;
    }
    
    static class ArgumentData {
        String idArgument = "", typeProCon = "", value = "", condition = "";
        String infValue = "", supValue = "", unit = "", assertion = "", explanation = "";
        String isProspective = "", date = "", aim = "";
        String tagInitiator = "";
        // Champs pour les strings directs (prioritaires sur les objets)
        String nameCriterionStr = "", nameProperty = "";
        // Liste des alternatives liées à cet argument
        List<String> nameAlternatives = new ArrayList<>();
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
        String uri, name, date;
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
    
    static class ProductData {
        String nameAlternative, productUri, nameProduct, tagProduct;
        @Override
        public int hashCode() {
            return (productUri != null ? productUri.hashCode() : 0)
                 + (tagProduct != null ? tagProduct.hashCode() : 0);
        }
        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ProductData)) return false;
            ProductData other = (ProductData) obj;
            return (productUri != null && productUri.equals(other.productUri))
                && (tagProduct != null && tagProduct.equals(other.tagProduct));
        }
    }

    static class ProductAgg {
        String nameAlternative, productUri, nameProduct;
        Set<String> tags = new HashSet<>();
    }

    static class CompositionData {
        String uriCompose, nameCompose, typeComposant;
        String uriComposant, nameComposant, rank, tagComposant;
    }

    static class CompositionAgg {
        String uriCompose, nameCompose, typeComposant;
        String uriComposant, nameComposant, rank;
        Set<String> tags = new HashSet<>();
    }
}
