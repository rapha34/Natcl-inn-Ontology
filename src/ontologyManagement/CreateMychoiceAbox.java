package ontologyManagement;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import org.apache.jena.ontology.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.vocabulary.DC;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.RDF;

import natclinn.util.NatclinnConf;
import natclinn.util.NatclinnUtil;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.openxml4j.util.ZipSecureFile;

/**
 * Crée une ABox MyChoice à partir du fichier Excel `NatclinnMychoice.xlsx`.
 *
 * La classe suit le modèle de `CreateNatclinnProductsAbox` : lecture des feuilles
 * attendues (Projects, Stakeholders, Criteria, Aims, Alternatives, Arguments, Properties, QualValues, Sources, TypeSource)
 * et instanciation des individus selon la TBox décrite dans `CreateMychoiceTbox`.
 *
 * Le fichier Excel doit être placé dans le répertoire configuré par `NatclinnConf.folderForData`.
 */
public class CreateMychoiceAbox {

    public static void main(String[] args) {
        new NatclinnConf();
        String excelFile = NatclinnConf.folderForData + "/NatclinnMychoice.xlsx";
        String jsonString = CreationMychoiceABox(excelFile);

        OntModel om = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
        RDFParser.create()
            .source(new StringReader(jsonString))
            .lang(Lang.JSONLD11)
            .parse(om);

        try {
            FileOutputStream outStream = new FileOutputStream(NatclinnConf.folderForOntologies + "/MychoiceAbox.xml");
            om.write(outStream, "RDF/XML");
            outStream.close();
        } catch (FileNotFoundException e) {
            System.out.println("File not found");
        } catch (IOException e) {
            System.out.println("IO problem");
        }
    }

    public static String CreationMychoiceABox(String excelFile) {
        String jsonString = null;
        OntModel om = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);

        String mch = "https://w3id.org/MCH/ontology/";
        om.setNsPrefix("mch", mch);

        Ontology ont = om.createOntology(mch + "MyChoiceAbox");
        om.add(ont, RDFS.label, "ABox MyChoice");
        om.add(ont, DC.description, "Abox instanciée à partir d'un fichier MyChoice (NatclinnMychoice.xlsx)");

        // Classes et propriétés utilisés (minimaux, basés sur CreateMychoiceTbox)
        Resource Stakeholder = om.createResource(mch + "Stakeholder");
        Resource Project = om.createResource(mch + "Project");
        Resource Criterion = om.createResource(mch + "Criterion");
        Resource Aim = om.createResource(mch + "Aim");
        Resource Alternative = om.createResource(mch + "Alternative");
        Resource Argument = om.createResource(mch + "Argument");
        Resource Property = om.createResource(mch + "Property");
        Resource Source = om.createResource(mch + "Source");
        

        // Datatype properties (nom, description, etc.)
        DatatypeProperty stakeholderName = om.createDatatypeProperty(mch + "stakeholderName");
        DatatypeProperty projectName = om.createDatatypeProperty(mch + "projectName");
        DatatypeProperty projectDescription = om.createDatatypeProperty(mch + "projectDescription");
        DatatypeProperty projectImage = om.createDatatypeProperty(mch + "projectImage");
        DatatypeProperty criterionName = om.createDatatypeProperty(mch + "criterionName");
        DatatypeProperty aimDescription = om.createDatatypeProperty(mch + "aimDescription");
        DatatypeProperty alternativeName = om.createDatatypeProperty(mch + "alternativeName");
        DatatypeProperty alternativeDescription = om.createDatatypeProperty(mch + "alternativeDescription");
        DatatypeProperty assertion = om.createDatatypeProperty(mch + "assertion");
        DatatypeProperty explanation = om.createDatatypeProperty(mch + "explanation");
        DatatypeProperty propertyName = om.createDatatypeProperty(mch + "propertyName");
        DatatypeProperty sourceName = om.createDatatypeProperty(mch + "sourceName");

        // Object properties
        ObjectProperty hasStakeholder = om.createObjectProperty(mch + "hasStakeholder");
        ObjectProperty hasAlternative = om.createObjectProperty(mch + "hasAlternative");
        ObjectProperty hasProperty = om.createObjectProperty(mch + "hasProperty");
    ObjectProperty belongsToProject = om.createObjectProperty(mch + "belongsToProject");
        ObjectProperty hasSource = om.createObjectProperty(mch + "hasSource");
        ObjectProperty hasAim = om.createObjectProperty(mch + "hasAim");
        ObjectProperty hasCriterion = om.createObjectProperty(mch + "hasCriterion");

        try (FileInputStream fis = new FileInputStream(excelFile)) {
            ZipSecureFile.setMinInflateRatio(0.001);
            try (Workbook workbook = new XSSFWorkbook(fis)) {
                Map<String, Resource> projects = new HashMap<>();
                Map<String, Resource> stakeholders = new HashMap<>();
                Map<String, Resource> criteria = new HashMap<>();
                Map<String, Resource> aims = new HashMap<>();
                Map<String, Resource> alternatives = new HashMap<>();
                Map<String, Resource> arguments = new HashMap<>();
                Map<String, Resource> properties = new HashMap<>();
                
                Map<String, Resource> sources = new HashMap<>();

                // Helper to find sheets case-insensitively
                java.util.function.Function<String[], Sheet> findSheet = (names) -> {
                    for (String nm : names) {
                        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                            Sheet s = workbook.getSheetAt(i);
                            if (s.getSheetName().equalsIgnoreCase(nm)) return s;
                        }
                    }
                    return null;
                };

                // Projects sheet: expected columns: nameProject, description, image
                Sheet projectsSheet = findSheet.apply(new String[]{"project","projects","Project","Projects"});
                if (projectsSheet != null) {
                    for (Row row : projectsSheet) {
                        if (row.getRowNum() == 0) continue;
                        String nameProject = getCellValue(row.getCell(0));
                        String desc = getCellValue(row.getCell(1));
                        String image = getCellValue(row.getCell(2));
                        if (nameProject == null || nameProject.isEmpty()) continue;
                        String uri = NatclinnUtil.makeURI(mch + "Project_", nameProject); // nameProject est la clé du projet
                        Resource proj = om.createResource(uri).addProperty(projectName, nameProject);
                        if (desc != null && !desc.isEmpty()) proj.addProperty(projectDescription, desc);
                        if (image != null && !image.isEmpty()) proj.addProperty(projectImage, image);
                        proj.addProperty(RDF.type, Project);
                        projects.put(nameProject, proj);
                    }
                }

                // Alternatives sheet: nameAlternative, description, imageAlternative, iconAlternative
                Sheet alternativesSheet = findSheet.apply(new String[]{"alternative","alternatives","Alternative","Alternatives"});
                if (alternativesSheet != null) {
                    for (Row row : alternativesSheet) {
                        if (row.getRowNum() == 0) continue;
                        String nameAlt = getCellValue(row.getCell(0));
                        String desc = getCellValue(row.getCell(1));
                        String imageAlt = getCellValue(row.getCell(2));
                        String iconAlt = getCellValue(row.getCell(3));
                        if (nameAlt == null || nameAlt.isEmpty()) continue;
                        String uri = NatclinnUtil.makeURI(mch + "Alternative_", nameAlt); // nameAlt est le nom de l'alternative
                        Resource alt = om.createResource(uri).addProperty(alternativeName, nameAlt);
                        if (desc != null && !desc.isEmpty()) alt.addProperty(alternativeDescription, desc);
                        if (imageAlt != null && !imageAlt.isEmpty()) alt.addProperty(projectImage, imageAlt);
                        if (iconAlt != null && !iconAlt.isEmpty()) alt.addProperty(projectImage, iconAlt);
                        alt.addProperty(RDF.type, Alternative);
                        // The workbook defines a single project, link the alternative to that project
                        if (!projects.isEmpty()) {
                            Resource projRef = projects.values().iterator().next();
                            if (projRef != null) {
                                alt.addProperty(belongsToProject, projRef);
                            }
                        }
                        alternatives.put(nameAlt, alt);
                    }
                }

                // TypeSource sheet: nameTypeSource, fiability
                Sheet typeSourceSheet = findSheet.apply(new String[]{"typesource","TypeSource","typesources","TypeSources"});
                DatatypeProperty typeFiability = om.createDatatypeProperty(mch + "typeFiability");
                if (typeSourceSheet != null) {
                    for (Row row : typeSourceSheet) {
                        if (row.getRowNum() == 0) continue;
                        String nameType = getCellValue(row.getCell(0));
                        String fiability = getCellValue(row.getCell(1));
                        if (nameType == null || nameType.isEmpty()) continue;
                        String uri = NatclinnUtil.makeURI(mch + "TypeSource_", nameType); // nameType est le nom du type de source
                        Resource ts = om.createResource(uri).addProperty(sourceName, nameType);
                        if (fiability != null && !fiability.isEmpty()) ts.addProperty(typeFiability, fiability);
                        ts.addProperty(RDF.type, om.createResource(mch + "TypeSource"));
                        sources.put(nameType, ts);
                    }
                }

                // HasExpertise sheet: nameStakeHolder, nameCriterion
                Sheet hasExpertiseSheet = findSheet.apply(new String[]{"hasexpertise","HasExpertise","hasExpertise"});
                if (hasExpertiseSheet != null) {
                    for (Row row : hasExpertiseSheet) {
                        if (row.getRowNum() == 0) continue;
                        String nameStake = getCellValue(row.getCell(0));
                        String nameCrit = getCellValue(row.getCell(1));
                        if ((nameStake == null || nameStake.isEmpty()) || (nameCrit == null || nameCrit.isEmpty())) continue;
                        // create stakeholder and criterion if missing
                        Resource sh = stakeholders.computeIfAbsent(nameStake, k -> {
                            String u = NatclinnUtil.makeURI(mch + "Stakeholder_", k); // k est le nom du stakeholder
                            Resource r = om.createResource(u).addProperty(stakeholderName, k);
                            r.addProperty(RDF.type, Stakeholder);
                            return r;
                        });
                        Resource crit = criteria.computeIfAbsent(nameCrit, k -> {
                            String u = NatclinnUtil.makeURI(mch + "Criterion_", k); // k est le nom du critère
                            Resource r = om.createResource(u).addProperty(criterionName, k);
                            r.addProperty(RDF.type, Criterion);
                            return r;
                        });
                        String uri = NatclinnUtil.makeURI(mch + "HasExpertise_", nameStake + "_" + nameCrit); // Combinaison des noms pour l'expertise
                        Resource he = om.createResource(uri);
                        he.addProperty(RDF.type, om.createResource(mch + "HasExpertise"));
                        he.addProperty(hasStakeholder, sh);
                        he.addProperty(hasCriterion, crit);
                    }
                }

                // Arguments sheet: columns as provided by user
                Sheet argumentsSheet = findSheet.apply(new String[]{"argument","arguments","Argument","Arguments"});
                if (argumentsSheet != null) {
                    for (Row row : argumentsSheet) {
                        if (row.getRowNum() == 0) continue;
                        String idArg = getCellValue(row.getCell(0));
                        String nameStake = getCellValue(row.getCell(1));
                        String nameAlt = getCellValue(row.getCell(2));
                        String typeProConVal = getCellValue(row.getCell(3));
                        String nameCrit = getCellValue(row.getCell(4));
                        String aimVal = getCellValue(row.getCell(5));
                        String nameProp = getCellValue(row.getCell(6));
                        String value = getCellValue(row.getCell(7));
                        String condition = getCellValue(row.getCell(8));
                        String infValueVal = getCellValue(row.getCell(9));
                        String supValueVal = getCellValue(row.getCell(10));
                        String unitVal = getCellValue(row.getCell(11));
                        String assertionVal = getCellValue(row.getCell(12));
                        String explanationVal = getCellValue(row.getCell(13));
                        String isProspectiveVal = getCellValue(row.getCell(14));
                        String dateVal = getCellValue(row.getCell(15));
                        String nameSourceVal = getCellValue(row.getCell(16));
                        String nameTypeSourceVal = getCellValue(row.getCell(17));

                        if (idArg == null || idArg.isEmpty()) continue;

                        String uriArg = NatclinnUtil.makeURI(mch + "Argument_", idArg); // idArg est l'identifiant de l'argument
                        Resource arg = om.createResource(uriArg);
                        arg.addProperty(RDF.type, Argument);
                        if (assertionVal != null && !assertionVal.isEmpty()) arg.addProperty(assertion, assertionVal);
                        if (explanationVal != null && !explanationVal.isEmpty()) arg.addProperty(explanation, explanationVal);
                        // stakeholder
                        if (nameStake != null && !nameStake.isEmpty()) {
                            Resource sh = stakeholders.computeIfAbsent(nameStake, k -> {
                                String u = NatclinnUtil.makeURI(mch + "Stakeholder_", k);
                                if (u == null) u = mch + k.replaceAll("\\s+","_");
                                Resource r = om.createResource(u).addProperty(stakeholderName, k);
                                r.addProperty(RDF.type, Stakeholder);
                                return r;
                            });
                            arg.addProperty(hasStakeholder, sh);
                        }
                        // alternative
                        if (nameAlt != null && !nameAlt.isEmpty()) {
                            Resource alt = alternatives.computeIfAbsent(nameAlt, k -> {
                                String u = NatclinnUtil.makeURI(mch + "Alternative_", k);
                                if (u == null) u = mch + k.replaceAll("\\s+","_");
                                Resource r = om.createResource(u).addProperty(alternativeName, k);
                                r.addProperty(RDF.type, Alternative);
                                return r;
                            });
                            arg.addProperty(hasAlternative, alt);
                        }
                        // criterion
                        if (nameCrit != null && !nameCrit.isEmpty()) {
                            Resource crit = criteria.computeIfAbsent(nameCrit, k -> {
                                String u = NatclinnUtil.makeURI(mch + "Criterion_", k);
                                if (u == null) u = mch + k.replaceAll("\\s+","_");
                                Resource r = om.createResource(u).addProperty(criterionName, k);
                                r.addProperty(RDF.type, Criterion);
                                return r;
                            });
                            arg.addProperty(hasCriterion, crit);
                        }
                        // aim
                        if (aimVal != null && !aimVal.isEmpty()) {
                            Resource a = aims.computeIfAbsent(aimVal, k -> {
                                String u = NatclinnUtil.makeURI(mch + "Aim_", k); // k est le nom de l'objectif
                                Resource r = om.createResource(u).addProperty(aimDescription, k);
                                r.addProperty(RDF.type, Aim);
                                return r;
                            });
                            arg.addProperty(hasAim, a);
                        }
                        // property
                        if (nameProp != null && !nameProp.isEmpty()) {
                            Resource p = properties.computeIfAbsent(nameProp, k -> {
                                String u = NatclinnUtil.makeURI(mch + "Property_", k); // k est le nom de la propriété
                                Resource r = om.createResource(u).addProperty(propertyName, k);
                                r.addProperty(RDF.type, Property);
                                return r;
                            });
                            arg.addProperty(hasProperty, p);
                        }
                        // value/condition/inf/sup/unit/typeProCon/isProspective/date
                        DatatypeProperty typeProConProp = om.createDatatypeProperty(mch + "typeProCon");
                        DatatypeProperty valueProp = om.createDatatypeProperty(mch + "value");
                        DatatypeProperty conditionProp = om.createDatatypeProperty(mch + "condition");
                        DatatypeProperty infValueProp = om.createDatatypeProperty(mch + "infValue");
                        DatatypeProperty supValueProp = om.createDatatypeProperty(mch + "supValue");
                        DatatypeProperty unitProp = om.createDatatypeProperty(mch + "unit");
                        DatatypeProperty isProspectiveProp = om.createDatatypeProperty(mch + "isProspective");
                        DatatypeProperty evaluationDateProp = om.createDatatypeProperty(mch + "evaluationDate");
                        if (typeProConVal != null && !typeProConVal.isEmpty()) arg.addProperty(typeProConProp, typeProConVal);
                        if (value != null && !value.isEmpty()) arg.addProperty(valueProp, value);
                        if (condition != null && !condition.isEmpty()) arg.addProperty(conditionProp, condition);
                        if (infValueVal != null && !infValueVal.isEmpty()) arg.addProperty(infValueProp, infValueVal);
                        if (supValueVal != null && !supValueVal.isEmpty()) arg.addProperty(supValueProp, supValueVal);
                        if (unitVal != null && !unitVal.isEmpty()) arg.addProperty(unitProp, unitVal);
                        if (isProspectiveVal != null && !isProspectiveVal.isEmpty()) arg.addProperty(isProspectiveProp, isProspectiveVal);
                        if (dateVal != null && !dateVal.isEmpty()) arg.addProperty(evaluationDateProp, dateVal);
                        // source and typeSource
                        if (nameSourceVal != null && !nameSourceVal.isEmpty()) {
                            Resource src = sources.computeIfAbsent(nameSourceVal, k -> {
                                String u = NatclinnUtil.makeURI(mch + "Source_", k); // k est le nom de la source
                                Resource r = om.createResource(u).addProperty(sourceName, k);
                                r.addProperty(RDF.type, Source);
                                return r;
                            });
                            arg.addProperty(hasSource, src);
                            if (nameTypeSourceVal != null && !nameTypeSourceVal.isEmpty()) {
                                Resource ts = sources.get(nameTypeSourceVal);
                                if (ts == null) {
                                    String u = NatclinnUtil.makeURI(mch + "TypeSource_", nameTypeSourceVal); // nameTypeSourceVal est le nom du type de source
                                    ts = om.createResource(u).addProperty(sourceName, nameTypeSourceVal);
                                    ts.addProperty(RDF.type, om.createResource(mch + "TypeSource"));
                                    sources.put(nameTypeSourceVal, ts);
                                }
                                // link source -> typeSource (hasTypeSource)
                                ObjectProperty hasTypeSourceProp = om.createObjectProperty(mch + "hasTypeSource");
                                src.addProperty(hasTypeSourceProp, ts);
                            }
                        }
                        // store
                        arguments.put(idArg, arg);
                    }
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("File not found: " + excelFile);
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        RDFDataMgr.write(out, om, RDFFormat.JSONLD11);
        try {
            jsonString = out.toString("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        System.out.println("ABox MyChoice créé (aperçu JSON-LD) — longueur: " + (jsonString != null ? jsonString.length() : 0));
        return jsonString;
    }

    private static String getCellValue(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                return new java.math.BigDecimal(cell.getNumericCellValue()).toPlainString();
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue();
                } catch (Exception ex) {
                    return String.valueOf(cell.getNumericCellValue());
                }
            default:
                return "";
        }
    }

}
