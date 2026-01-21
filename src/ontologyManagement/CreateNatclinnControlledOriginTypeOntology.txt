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
 * Programme de création de l'ABox des origines d'ingrédients à partir
 * du fichier Excel OriginAbox.xlsx.
 *
 * Onglet "Origine" : IDOrigine | Nom Origine
 * Onglet "CritéreEtPropriété" : IDOrigine | Nom Origine | Origine Requise | Attribut | Description | Polarité | Nom critère | Propriété | Valeur propriété
 *
 * Pour chaque origine on crée un individu de classe ControlledOriginLabel.
 * Pour chaque ligne critère/propriété associée à une origine on crée un individu Argument
 * (réutilisation du modèle existant) et on le lie à l'origine via la propriété aboutOrigin.
 */
public class CreateNatclinnControlledOriginTypeOntology {

    public static void main(String[] args) {
        new NatclinnConf();
        String excelFile = NatclinnConf.folderForData + "/ControlledOriginTypeAbox.xlsx";
        String json = createOriginABox(excelFile);
        if (json == null) {
            System.out.println("Modèle vide – arrêt.");
            return;
        }
        OntModel om = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
        RDFParser.create().source(new StringReader(json)).lang(Lang.JSONLD).parse(om);
        try (FileOutputStream out = new FileOutputStream(NatclinnConf.folderForOntologies + "/NatclinnControlledOriginTypeAbox.xml")) {
            om.write(out, "RDF/XML");
        } catch (IOException e) {
            System.err.println("Erreur écriture RDF/XML: " + e.getMessage());
        }
    }

    public static String createOriginABox(String excelFile) {
        OntModel om = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);

        // Namespaces
        String ncl = NatclinnConf.ncl; om.setNsPrefix("ncl", ncl);
        String skos = NatclinnConf.skos; om.setNsPrefix("skos", skos);
        String rdfs = NatclinnConf.rdfs; om.setNsPrefix("rdfs", rdfs);
        String rdf = NatclinnConf.rdf; om.setNsPrefix("rdf", rdf);
        String dcterms = NatclinnConf.dcterms; om.setNsPrefix("dcterms", dcterms);

        Ontology ont = om.createOntology(ncl + "NatclinnControlledOriginTypeAbox");
        om.add(ont, RDFS.label, "Ontology of Natclinn Ingredient Origins");
        om.add(ont, DC.description, "ABox for controlled origin labels and related criteria");

        OntClass NCL = om.createClass(ncl + "NCL");
	    NCL.addComment("NCL is the set of product and arguments.", "en");
		NCL.addComment("NCL est l'ensemble des produits et des arguments.", "fr");

		// NCLCOT est un ensemble d'atributs de classification
	    OntClass NCLCOT = om.createClass(ncl + "NCLCOT");
		NCLCOT.addComment("NCLCOT is the set of Ingredient origins.", "en");
		NCLCOT.addComment("NCLCOT est l'ensemble des origines d'ingrédients.", "fr");
	    NCLCOT.addSuperClass(NCL);

        // Classes
        OntClass ControlledOriginType = om.createClass(ncl + "ControlledOriginType");
        OntClass ControlledOriginTypeArgumentBinding = om.createClass(ncl + "ControlledOriginTypeArgumentBinding");

        ControlledOriginType.addSuperClass(NCLCOT);
        ControlledOriginTypeArgumentBinding.addSuperClass(NCLCOT);

        /////////////////////////////
	    // Inclusion de concepts   //
	    /////////////////////////////
	
	    // Note: Les relations de subsomption de classes ne sont plus créées avec Model

        // adaptation du code pour la nouvelle structure du fichier Excel ControlledOriginTypeAbox.xlsx : IDOrigine	Nom Origine	Origine Requise	Mots-clés	RègleFiltrage	LabelsSynonymes	RègleSelection	PropriétésAntinomie


        // Properties
        ObjectProperty aboutOrigin = om.createObjectProperty(ncl + "aboutOrigin"); // lien OriginArgumentBinding -> ControlledOriginLabel
        DatatypeProperty originRequired = om.createDatatypeProperty(ncl + "originRequired");
         DatatypeProperty keywordsProperty = om.createDatatypeProperty(ncl + "bindingAgentKeywords");
        // Nouvelles propriétés pour la configuration des règles de filtrage et sélection
        DatatypeProperty filteringRule = om.createDatatypeProperty(ncl + "originFilteringRule");
        DatatypeProperty synonymLabels = om.createDatatypeProperty(ncl + "originSynonymLabels");
        DatatypeProperty selectionRule = om.createDatatypeProperty(ncl + "originSelectionRule");
        DatatypeProperty antinomyProperties = om.createDatatypeProperty(ncl + "originAntinomyProperties");
        AnnotationProperty prefLabel = om.createAnnotationProperty(skos + "prefLabel");

        Map<String, Individual> originMap = new HashMap<>();
        Map<String, Integer> argCountersByLabel = new HashMap<>();

        try (FileInputStream fis = new FileInputStream(excelFile)) {
            ZipSecureFile.setMinInflateRatio(0.001);
            try (Workbook workbook = new XSSFWorkbook(fis)) {
                // Sheet Origine
                Sheet originsSheet = workbook.getSheet("Origine");
                if (originsSheet != null) {
                    for (Row row : originsSheet) {
                        if (row.getRowNum() == 0) continue; // header
                        String idOrigin = getCellValue(row.getCell(0));
                        String nomOrigin = getCellValue(row.getCell(1));
                        if (idOrigin != null && !idOrigin.isEmpty()) {
                            Individual origin = om.createIndividual(ncl + nomOrigin.replaceAll("\\s+", "_"), ControlledOriginType);
                            if (nomOrigin != null && !nomOrigin.isEmpty()) {
                                origin.addProperty(prefLabel, nomOrigin);
                            }
                            originMap.put(idOrigin, origin);
                        }
                    }
                }

                // Sheet CritéreEtPropriété
                Sheet critSheet = workbook.getSheet("CritéreEtPropriété");
                if (critSheet != null) {
                    for (Row row : critSheet) {
                        if (row.getRowNum() == 0) continue; // header
                        String idOrigin = getCellValue(row.getCell(0));
                        String originRequis = getCellValue(row.getCell(2));
                        String motsCles = getCellValue(row.getCell(3));
                        // Nouvelles colonnes pour la configuration
                        String regleFiltrage = getCellValue(row.getCell(4));
                        String labelsSynonymes = getCellValue(row.getCell(5));
                        String regleSelection = getCellValue(row.getCell(6));
                        String proprietesAntinomie = getCellValue(row.getCell(7));

                        // Ne créer un OriginArgumentBinding que si IDOrigin est non vide
                        if (idOrigin != null && !idOrigin.isEmpty()) {
                            // Et s'il y a au moins une info utile
                            if ((motsCles != null && !motsCles.isEmpty()) || (regleFiltrage != null && !regleFiltrage.isEmpty())
                                    || (labelsSynonymes != null && !labelsSynonymes.isEmpty()) || (regleSelection != null && !regleSelection.isEmpty())
                                    || (proprietesAntinomie != null && !proprietesAntinomie.isEmpty())) {
                                String originLabel = null;
                                if (originMap.containsKey(idOrigin)) {
                                    try {
                                        originLabel = originMap.get(idOrigin).getProperty(prefLabel).getString();
                                    } catch (Exception ignored) {}
                                }
                                String baseLabel = (originLabel != null && !originLabel.isEmpty()) ? originLabel : idOrigin;
                                String safeLabel = baseLabel
                                        .trim()
                                        .replaceAll("\\s+", "-")
                                        .replaceAll("[^A-Za-z0-9_-]", "");
                                int nextIndex = argCountersByLabel.getOrDefault(safeLabel, 0) + 1;
                                // Identifiant de l'individu OriginArgumentBinding (préfixe PkgBind-)
                                String argId = "PkgBind-" + safeLabel + "-" + nextIndex;
                                // Sécurité: éviter collision si précédemment créé
                                while (om.containsResource(om.createResource(ncl + argId))) {
                                    nextIndex++;
                                    // Re-génération de l'identifiant avec le préfixe PkgBind-
                                    argId = "PkgBind-" + safeLabel + "-" + nextIndex;
                                }
                                argCountersByLabel.put(safeLabel, nextIndex);
                                Individual arg = om.createIndividual(ncl + argId, ControlledOriginTypeArgumentBinding);

                                if (motsCles != null && !motsCles.isEmpty()) arg.addProperty(keywordsProperty, motsCles);
                                // Nouvelles propriétés de configuration
                                if (regleFiltrage != null && !regleFiltrage.isEmpty()) arg.addProperty(filteringRule, regleFiltrage);
                                if (labelsSynonymes != null && !labelsSynonymes.isEmpty()) arg.addProperty(synonymLabels, labelsSynonymes);
                                if (regleSelection != null && !regleSelection.isEmpty()) arg.addProperty(selectionRule, regleSelection);
                                if (proprietesAntinomie != null && !proprietesAntinomie.isEmpty()) arg.addProperty(antinomyProperties, proprietesAntinomie);
                                if (originRequis != null && !originRequis.isEmpty()) arg.addProperty(originRequired, originRequis);

                                // Lien vers le type d'emballage (idOrigin est non vide et attendu existant)
                                if (originMap.containsKey(idOrigin)) {
                                    arg.addProperty(aboutOrigin, originMap.get(idOrigin));
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
