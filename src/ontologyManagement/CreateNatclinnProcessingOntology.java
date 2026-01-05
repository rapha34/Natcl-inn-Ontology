package ontologyManagement;

import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.jena.ontology.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.vocabulary.DC;
import org.apache.jena.vocabulary.RDFS;

import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import natclinn.util.NatclinnConf;
import natclinn.util.NatclinnUtil;

/**
 * Crée l'ABox des transformations (degré de transformation) à partir du fichier Excel ProcessingAbox.xlsx.
 * Onglet "Transformation" : IDTransformation | Nom Transformation
 * Onglet "CritéreEtPropriété" : IDTransformation | Nom Transformation | Transformation Requise | Attribut | Description | Polarité | Nom critère | Propriété | Valeur propriété
 */
public class CreateNatclinnProcessingOntology {

    public static void main(String[] args) {
        new NatclinnConf();
        String excelFile = NatclinnConf.folderForData + "/ProcessingAbox.xlsx";
        OntModel om = createProcessingABox(excelFile);
        if (om == null) {
            System.out.println("Modèle vide – arrêt.");
            return;
        }

        String output = NatclinnConf.folderForOntologies + "/NatclinnProcessing.xml";
        try (FileOutputStream fos = new FileOutputStream(output);
             BufferedOutputStream bos = new BufferedOutputStream(fos, 8192 * 4)) {
            RDFDataMgr.write(bos, om, RDFFormat.RDFXML_PLAIN);
            bos.flush();
            System.out.println("Ontologie écrite : " + output);
        } catch (IOException e) {
            System.err.println("Erreur écriture RDF/XML: " + e.getMessage());
        }
    }

    public static OntModel createProcessingABox(String excelFile) {
        OntModel om = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);

        // Namespaces
        String ncl = NatclinnConf.ncl; om.setNsPrefix("ncl", ncl);
        String skos = NatclinnConf.skos; om.setNsPrefix("skos", skos);
        String rdfs = NatclinnConf.rdfs; om.setNsPrefix("rdfs", rdfs);
        String rdf = NatclinnConf.rdf; om.setNsPrefix("rdf", rdf);
        String dcterms = NatclinnConf.dcterms; om.setNsPrefix("dcterms", dcterms);

        // Ontology
        Ontology ont = om.createOntology(ncl + "NatclinnProcessingAbox");
        om.add(ont, RDFS.label, "Ontology of Natclinn Processing Transformations");
        om.add(ont, DC.description, "ABox for processing transformations and related criteria");

        // Classes
        OntClass NCL = om.createClass(ncl + "NCL");
        NCL.addComment("NCL is the set of product and arguments.", "en");
        NCL.addComment("NCL est l'ensemble des produits et des arguments.", "fr");

        OntClass NCLPR = om.createClass(ncl + "NCLPR");
        NCLPR.addComment("NCLPR is the set of processing transformations.", "en");
        NCLPR.addComment("NCLPR est l'ensemble des transformations.", "fr");
        NCLPR.addSuperClass(NCL);

        OntClass Processing = om.createClass(ncl + "Processing");
        OntClass ProcessingArgumentBinding = om.createClass(ncl + "ProcessingArgumentBinding");
        OntClass ProcessingAttribute = om.createClass(ncl + "ProcessingAttribute");

        Processing.addSuperClass(NCLPR);
        ProcessingArgumentBinding.addSuperClass(NCLPR);
        ProcessingAttribute.addSuperClass(NCLPR);

        // Properties
        ObjectProperty hasProcessingAttribute = om.createObjectProperty(ncl + "hasProcessingAttribute");
        ObjectProperty aboutProcessing = om.createObjectProperty(ncl + "aboutProcessing");
        DatatypeProperty processingRequired = om.createDatatypeProperty(ncl + "processingRequired");
        DatatypeProperty processingDescription = om.createDatatypeProperty(ncl + "processingDescription");
        DatatypeProperty processingPolarity = om.createDatatypeProperty(ncl + "processingPolarity");
        DatatypeProperty processingNameCriterion = om.createDatatypeProperty(ncl + "processingNameCriterion");
        DatatypeProperty processingNameProperty = om.createDatatypeProperty(ncl + "processingNameProperty");
        DatatypeProperty processingValueProperty = om.createDatatypeProperty(ncl + "processingValueProperty");
        AnnotationProperty prefLabel = om.createAnnotationProperty(skos + "prefLabel");

        Map<String, Individual> transformationMap = new HashMap<>();
        Map<String, Individual> attributeMap = new HashMap<>();
        Map<String, Integer> argCountersByLabel = new HashMap<>();

        try (FileInputStream fis = new FileInputStream(excelFile)) {
            ZipSecureFile.setMinInflateRatio(0.001);
            try (Workbook workbook = new XSSFWorkbook(fis)) {
                // Sheet Transformation
                Sheet transSheet = workbook.getSheet("Transformation");
                if (transSheet != null) {
                    for (Row row : transSheet) {
                        if (row.getRowNum() == 0) continue; // header
                        String idTransfo = getCellValue(row.getCell(0));
                        String nomTransfo = getCellValue(row.getCell(1));
                        if (idTransfo != null && !idTransfo.isEmpty()) {
                            String uri = NatclinnUtil.makeURI(ncl + "Transformation-", (nomTransfo != null && !nomTransfo.isEmpty()) ? nomTransfo : idTransfo);
                            Individual t = om.createIndividual(uri, Processing);
                            if (nomTransfo != null && !nomTransfo.isEmpty()) {
                                t.addProperty(prefLabel, nomTransfo);
                            }
                            transformationMap.put(idTransfo, t);
                        }
                    }
                }

                // Sheet CritéreEtPropriété
                Sheet critSheet = workbook.getSheet("CritéreEtPropriété");
                if (critSheet != null) {
                    for (Row row : critSheet) {
                        if (row.getRowNum() == 0) continue; // header
                        String idTransfo = getCellValue(row.getCell(0));
                        // ignore column 1 (Nom Transformation)
                        String transfoRequise = getCellValue(row.getCell(2));
                        String attribut = getCellValue(row.getCell(3));
                        String description = getCellValue(row.getCell(4));
                        String polarite = getCellValue(row.getCell(5));
                        String nomCritere = getCellValue(row.getCell(6));
                        String propriete = getCellValue(row.getCell(7));
                        String valeurPropriete = getCellValue(row.getCell(8));

                        if (idTransfo != null && !idTransfo.isEmpty()) {
                            boolean hasData = (propriete != null && !propriete.isEmpty()) ||
                                              (description != null && !description.isEmpty()) ||
                                              (transfoRequise != null && !transfoRequise.isEmpty()) ||
                                              (attribut != null && !attribut.isEmpty()) ||
                                              (nomCritere != null && !nomCritere.isEmpty()) ||
                                              (valeurPropriete != null && !valeurPropriete.isEmpty()) ||
                                              (polarite != null && !polarite.isEmpty());
                            if (hasData) {
                                String baseLabel = idTransfo;
                                if (transformationMap.containsKey(idTransfo)) {
                                    try {
                                        String lbl = transformationMap.get(idTransfo).getProperty(prefLabel).getString();
                                        if (lbl != null && !lbl.isEmpty()) {
                                            baseLabel = lbl;
                                        }
                                    } catch (Exception ignored) {}
                                }
                                String safeLabel = baseLabel.trim()
                                        .replaceAll("\\s+", "-")
                                        .replaceAll("[^A-Za-z0-9_-]", "");
                                int nextIndex = argCountersByLabel.getOrDefault(safeLabel, 0) + 1;
                                String argId = "ProcBind-" + safeLabel + "-" + nextIndex;
                                while (om.containsResource(om.createResource(ncl + argId))) {
                                    nextIndex++;
                                    argId = "ProcBind-" + safeLabel + "-" + nextIndex;
                                }
                                argCountersByLabel.put(safeLabel, nextIndex);

                                Individual arg = om.createIndividual(ncl + argId, ProcessingArgumentBinding);

                                if (description != null && !description.isEmpty()) arg.addProperty(processingDescription, description);
                                if (polarite != null && !polarite.isEmpty()) arg.addProperty(processingPolarity, polarite);
                                if (nomCritere != null && !nomCritere.isEmpty()) arg.addProperty(processingNameCriterion, nomCritere);
                                if (propriete != null && !propriete.isEmpty()) arg.addProperty(processingNameProperty, propriete);
                                if (valeurPropriete != null && !valeurPropriete.isEmpty()) arg.addProperty(processingValueProperty, valeurPropriete);
                                if (transfoRequise != null && !transfoRequise.isEmpty()) arg.addProperty(processingRequired, transfoRequise);

                                // Attribut (création si nécessaire)
                                if (attribut != null && !attribut.isEmpty()) {
                                    if (!attributeMap.containsKey(attribut)) {
                                        String uriAtt = NatclinnUtil.makeURI(ncl + "ProcessingAttribute-", attribut.replaceAll("\\s+", "-"));
                                        Individual att = om.createIndividual(uriAtt, ProcessingAttribute);
                                        att.addProperty(prefLabel, attribut);
                                        attributeMap.put(attribut, att);
                                    }
                                    arg.addProperty(hasProcessingAttribute, attributeMap.get(attribut));
                                }

                                // Lien vers la transformation
                                if (transformationMap.containsKey(idTransfo)) {
                                    arg.addProperty(aboutProcessing, transformationMap.get(idTransfo));
                                }
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Erreur lecture Excel: " + e.getMessage());
            return null;
        }

        System.out.println("Modèle construit : " + om.size() + " triplets");
        return om;
    }

    private static String getCellValue(Cell cell) {
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case STRING: return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) return cell.getDateCellValue().toString();
                double num = cell.getNumericCellValue();
                if (num == (long) num) return String.valueOf((long) num);
                return String.valueOf(num);
            case BOOLEAN: return String.valueOf(cell.getBooleanCellValue());
            case FORMULA: return cell.getCellFormula();
            case BLANK: return null;
            default: return null;
        }
    }
}
