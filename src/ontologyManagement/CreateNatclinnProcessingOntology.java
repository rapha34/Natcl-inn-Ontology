package ontologyManagement;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import org.apache.jena.ontology.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.DC;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFParser;

import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import natclinn.util.NatclinnConf;

/**
 * Programme de création de l'ABox des types d'emballage à partir
 * du fichier Excel ProcessingAbox.xlsx.
 *
 * Onglet "Processing" : IDProcessing | Nom Processing
 * Onglet "CritéreEtPropriété" : IDProcessing | Nom Processing | Processing Requise | Attribut | Description | Polarité | Nom critère | Propriété | Valeur propriété | Mots-clés
 *
 * Pour chaque type d'emballage on crée un individu de classe Processing.
 * Pour chaque ligne critère/propriété associée à un type d'emballage on crée un individu Argument
 * (réutilisation du modèle existant) et on le lie au type d'emballage via la propriété aboutProcessing.
 */
public class CreateNatclinnProcessingOntology {

    public static void main(String[] args) {
        new NatclinnConf();
        String excelFile = NatclinnConf.folderForData + "/ProcessingAbox.xlsx";
        String json = createProcessingABox(excelFile);
        if (json == null) {
            System.out.println("Modèle vide -> arrêt.");
            return;
        }
        OntModel om = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
        RDFParser.create().source(new StringReader(json)).lang(Lang.JSONLD).parse(om);
        try (FileOutputStream out = new FileOutputStream(NatclinnConf.folderForOntologies + "/NatclinnProcessingAbox.xml")) {
            om.write(out, "RDF/XML");
        } catch (IOException e) {
            System.err.println("Erreur écriture RDF/XML: " + e.getMessage());
        }
    }

    public static String createProcessingABox(String excelFile) {
        OntModel om = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);

        // Namespaces
        String ncl = NatclinnConf.ncl; om.setNsPrefix("ncl", ncl);
        String skos = NatclinnConf.skos; om.setNsPrefix("skos", skos);
        String rdfs = NatclinnConf.rdfs; om.setNsPrefix("rdfs", rdfs);
        String rdf = NatclinnConf.rdf; om.setNsPrefix("rdf", rdf);
        String dcterms = NatclinnConf.dcterms; om.setNsPrefix("dcterms", dcterms);

        Ontology ont = om.createOntology(ncl + "NatclinnProcessingAbox");
        om.add(ont, RDFS.label, "Ontology of Natclinn Packaging Types");
        om.add(ont, DC.description, "ABox for packaging types and related criteria");

        OntClass NCL = om.createClass(ncl + "NCL");
	    NCL.addComment("NCL is the set of product and arguments.", "en");
		NCL.addComment("NCL est l'ensemble des produits et des arguments.", "fr");

		// NCL est un ensemble d'atributs de classification
	    OntClass NCLPT = om.createClass(ncl + "NCLPT");
		NCLPT.addComment("NCLPT is the set of classification attributes.", "en");
		NCLPT.addComment("NCLPT est l'ensemble des attributs de classification.", "fr");
	    NCLPT.addSuperClass(NCL);

        // Classes
        OntClass Processing = om.createClass(ncl + "Processing");
        OntClass ProcessingArgumentBinding = om.createClass(ncl + "ProcessingArgumentBinding");
    
        Processing.addSuperClass(NCLPT);
        ProcessingArgumentBinding.addSuperClass(NCLPT);

        /////////////////////////////
	    // Inclusion de concepts   //
	    /////////////////////////////
	
	    // Note: Les relations de subsomption de classes ne sont plus créées avec Model

        // Properties
        ObjectProperty aboutProcessing = om.createObjectProperty(ncl + "aboutProcessing"); // lien ProcessingArgumentBinding -> Processing
        DatatypeProperty processingRequired = om.createDatatypeProperty(ncl + "processingRequired");
        DatatypeProperty keywordsProperty = om.createDatatypeProperty(ncl + "bindingAgentKeywords");
        // Nouvelles propriétés pour la configuration des règles de filtrage et sélection
        DatatypeProperty filteringRule = om.createDatatypeProperty(ncl + "processingFilteringRule");
        DatatypeProperty synonymLabels = om.createDatatypeProperty(ncl + "processingSynonymLabels");
        DatatypeProperty selectionRule = om.createDatatypeProperty(ncl + "processingSelectionRule");
        DatatypeProperty antinomyProperties = om.createDatatypeProperty(ncl + "processingAntinomyProperties");
        AnnotationProperty prefLabel = om.createAnnotationProperty(skos + "prefLabel");

        Map<String, Individual> processingMap = new HashMap<>();
        Map<String, Integer> argCountersByLabel = new HashMap<>();

        try (FileInputStream fis = new FileInputStream(excelFile)) {
            ZipSecureFile.setMinInflateRatio(0.001);
            try (Workbook workbook = new XSSFWorkbook(fis)) {
                // Sheet Processing
                Sheet processingsSheet = workbook.getSheet("Processing");
                if (processingsSheet != null) {
                    for (Row row : processingsSheet) {
                        if (row.getRowNum() == 0) continue; // header
                        String idProcessing = getCellValue(row.getCell(0));
                        String nomProcessing = getCellValue(row.getCell(1));
                        if (idProcessing != null && !idProcessing.isEmpty()) {
                            Individual pt = om.createIndividual(ncl + nomProcessing.replaceAll("\\s+", "_"), Processing);
                            if (nomProcessing != null && !nomProcessing.isEmpty()) {
                                pt.addProperty(prefLabel, nomProcessing);
                            }
                            processingMap.put(idProcessing, pt);
                        }
                    }
                }

                // Sheet CritéreEtPropriété
                Sheet critSheet = workbook.getSheet("CritéreEtPropriété");
                if (critSheet != null) {
                    for (Row row : critSheet) {
                        if (row.getRowNum() == 0) continue; // header
                        String idProcessing = getCellValue(row.getCell(0));
                        String processingRequis = getCellValue(row.getCell(2));
                        String motsCles = getCellValue(row.getCell(3));
                        // Nouvelles colonnes pour la configuration
                        String regleFiltrage = getCellValue(row.getCell(4));
                        String labelsSynonymes = getCellValue(row.getCell(5));
                        String regleSelection = getCellValue(row.getCell(6));
                        String proprietesAntinomie = getCellValue(row.getCell(7));

                            // Ne créer un ProcessingArgumentBinding que si IDProcessing est non vide
                        if (idProcessing != null && !idProcessing.isEmpty()) {
                            // Et s'il y a au moins une info utile
                            if ((motsCles != null && !motsCles.isEmpty()) || (regleFiltrage != null && !regleFiltrage.isEmpty())
                                    || (labelsSynonymes != null && !labelsSynonymes.isEmpty()) || (regleSelection != null && !regleSelection.isEmpty())
                                    || (proprietesAntinomie != null && !proprietesAntinomie.isEmpty())) {
                                String processingLabel = null;
                                if (processingMap.containsKey(idProcessing)) {
                                    try {
                                        processingLabel = processingMap.get(idProcessing).getProperty(prefLabel).getString();
                                    } catch (Exception ignored) {}
                                }
                                String baseLabel = (processingLabel != null && !processingLabel.isEmpty()) ? processingLabel : idProcessing;
                                String safeLabel = baseLabel
                                        .trim()
                                        .replaceAll("\\s+", "-")
                                        .replaceAll("[^A-Za-z0-9_-]", "");
                                int nextIndex = argCountersByLabel.getOrDefault(safeLabel, 0) + 1;
                                // Identifiant de l'individu ProcessingArgumentBinding (préfixe PkgBind-)
                                String argId = "PkgBind-" + safeLabel + "-" + nextIndex;
                                // Sécurité: éviter collision si précédemment créé
                                while (om.containsResource(om.createResource(ncl + argId))) {
                                    nextIndex++;
                                    // Re-génération de l'identifiant avec le préfixe PkgBind-
                                    argId = "PkgBind-" + safeLabel + "-" + nextIndex;
                                }
                                argCountersByLabel.put(safeLabel, nextIndex);
                                Individual arg = om.createIndividual(ncl + argId, ProcessingArgumentBinding);

                                if (motsCles != null && !motsCles.isEmpty()) arg.addProperty(keywordsProperty, motsCles);
                                // Nouvelles propriétés de configuration
                                if (regleFiltrage != null && !regleFiltrage.isEmpty()) arg.addProperty(filteringRule, regleFiltrage);
                                if (labelsSynonymes != null && !labelsSynonymes.isEmpty()) arg.addProperty(synonymLabels, labelsSynonymes);
                                if (regleSelection != null && !regleSelection.isEmpty()) arg.addProperty(selectionRule, regleSelection);
                                if (proprietesAntinomie != null && !proprietesAntinomie.isEmpty()) arg.addProperty(antinomyProperties, proprietesAntinomie);
                                if (processingRequis != null && !processingRequis.isEmpty()) arg.addProperty(processingRequired, processingRequis);

                                // Lien vers le type d'emballage (IDProcessing est non vide et attendu existant)
                                if (processingMap.containsKey(idProcessing)) {
                                    arg.addProperty(aboutProcessing, processingMap.get(idProcessing));
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

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        RDFDataMgr.write(out, om, RDFFormat.JSONLD11);
        try {
            return out.toString("UTF-8");
        } catch (UnsupportedEncodingException e) {
            return null;
        }
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
