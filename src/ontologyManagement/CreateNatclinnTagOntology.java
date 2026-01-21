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
 * Programme de création de l'ABox des Tags pour les liens produits arguments à partir
 * du fichier Excel ProductTagToLinkArgumentAbox.xlsx.
 *
 * Onglet "Tag Produit" : ID Tag | Nom Tag | Type Tag

 * Onglet "Paramètres Tag" : ID Tag | Nom Tag | Polarité | Catégorie | Nom critère | But | Propriété | Valeur propriété | Condition propriété | Valeur inférieur propriété | Valeur supérieure propriété | Unité propriété | Assertion | Explanation | Mots-clés | RègleFiltrage | LabelsSynonymes | RègleSelection | PropriétésAntinomie
 *
 */
public class CreateNatclinnTagOntology {

    public static void main(String[] args) {
        new NatclinnConf();
        String excelFile = NatclinnConf.folderForData + "/ProductTagToLinkArgumentAbox.xlsx";
        String json = createTagABox(excelFile);
        if (json == null) {
            System.out.println("Modèle vide -> arrêt.");
            return;
        }
        OntModel om = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
        RDFParser.create().source(new StringReader(json)).lang(Lang.JSONLD).parse(om);
        try (FileOutputStream out = new FileOutputStream(NatclinnConf.folderForOntologies + "/NatclinnTagAbox.xml")) {
            om.write(out, "RDF/XML");
        } catch (IOException e) {
            System.err.println("Erreur écriture RDF/XML: " + e.getMessage());
        }
    }

    public static String createTagABox(String excelFile) {
        OntModel om = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);

        // Namespaces
        String ncl = NatclinnConf.ncl; om.setNsPrefix("ncl", ncl);
        String skos = NatclinnConf.skos; om.setNsPrefix("skos", skos);
        String rdfs = NatclinnConf.rdfs; om.setNsPrefix("rdfs", rdfs);
        String rdf = NatclinnConf.rdf; om.setNsPrefix("rdf", rdf);
        String dcterms = NatclinnConf.dcterms; om.setNsPrefix("dcterms", dcterms);

        Ontology ont = om.createOntology(ncl + "NatclinnTagAbox");
        om.add(ont, RDFS.label, "Ontology of Natclinn Tags");
        om.add(ont, DC.description, "ABox for tags and related parameters to link product arguments in Natclinn.");

        OntClass NCL = om.createClass(ncl + "NCL");
	    NCL.addComment("NCL is the set of product and arguments.", "en");
		NCL.addComment("NCL est l'ensemble des produits et des arguments.", "fr");

		// NCL est un ensemble d'atributs de classification
	    OntClass NCLTAG = om.createClass(ncl + "NCLTAG");
		NCLTAG.addComment("NCLTAG is the set of tags.", "en");
		NCLTAG.addComment("NCLTAG est l'ensemble des tags.", "fr");
	    NCLTAG.addSuperClass(NCL);

        // Classes
        OntClass Tag = om.createClass(ncl + "Tag");
        OntClass TagArgumentBinding = om.createClass(ncl + "TagArgumentBinding");
    
        Tag.addSuperClass(NCLTAG);
        TagArgumentBinding.addSuperClass(NCLTAG);

        /////////////////////////////
	    // Inclusion de concepts   //
	    /////////////////////////////
	
	    // Note: Les relations de subsomption de classes ne sont plus créées avec Model

        // Properties
        ObjectProperty aboutTag = om.createObjectProperty(ncl + "aboutTag");
        // Propriétés de données pour Argument
		DatatypeProperty polarity = om.createDatatypeProperty(ncl + "tagPolarity");
		DatatypeProperty nameCriterion = om.createDatatypeProperty(ncl + "tagNameCriterion");
		DatatypeProperty aim = om.createDatatypeProperty(ncl + "tagAim");
		DatatypeProperty nameProperty = om.createDatatypeProperty(ncl + "tagNameProperty");
		DatatypeProperty valueProperty = om.createDatatypeProperty(ncl + "tagValueProperty");
		DatatypeProperty condition = om.createDatatypeProperty(ncl + "tagCondition");
		DatatypeProperty infValue = om.createDatatypeProperty(ncl + "tagInfValue");
		DatatypeProperty supValue = om.createDatatypeProperty(ncl + "tagSupValue");
		DatatypeProperty unit = om.createDatatypeProperty(ncl + "tagUnit");
        DatatypeProperty assertion = om.createDatatypeProperty(ncl + "tagAssertion");
        DatatypeProperty explanation = om.createDatatypeProperty(ncl + "tagExplanation");
        DatatypeProperty keywordsProperty = om.createDatatypeProperty(ncl + "tagBindingKeywords");
        // Propriétés pour la configuration des règles de filtrage et sélection
        DatatypeProperty filteringRule = om.createDatatypeProperty(ncl + "tagFilteringRule");
        DatatypeProperty synonymLabels = om.createDatatypeProperty(ncl + "tagSynonymLabels");
        DatatypeProperty selectionRule = om.createDatatypeProperty(ncl + "tagSelectionRule");
        DatatypeProperty antinomyProperties = om.createDatatypeProperty(ncl + "tagAntinomyProperties");
        DatatypeProperty tagType = om.createDatatypeProperty(ncl + "tagType");
        AnnotationProperty prefLabel = om.createAnnotationProperty(skos + "prefLabel");

        Map<String, Individual> tagMap = new HashMap<>();
        Map<String, Integer> argCountersByLabel = new HashMap<>();

        try (FileInputStream fis = new FileInputStream(excelFile)) {
            ZipSecureFile.setMinInflateRatio(0.001);
            try (Workbook workbook = new XSSFWorkbook(fis)) {
                // Sheet "Tag Produit" : ID Tag | Nom Tag | Type Tag
                Sheet tagSheet = workbook.getSheet("Tag Produit");
                if (tagSheet != null) {
                    for (Row row : tagSheet) {
                        if (row.getRowNum() == 0) continue; // header
                        String idTag = getCellValue(row.getCell(0));
                        String nomTag = getCellValue(row.getCell(1));
                        String typeTag = getCellValue(row.getCell(2));
                        
                        if (idTag != null && !idTag.isEmpty()) {
                            // String tagUri = ncl + "Tag-" + idTag;
                            String tagUri = ncl + nomTag;
                            Individual tag = om.createIndividual(tagUri, Tag);
                            
                            if (nomTag != null && !nomTag.isEmpty()) {
                                tag.addProperty(prefLabel, nomTag);
                            }
                            if (typeTag != null && !typeTag.isEmpty()) {
                                tag.addProperty(tagType, typeTag);
                            }
                            tagMap.put(idTag, tag);
                        }
                    }
                }

                // Sheet "Paramètres Tag" : ID Tag | Nom Tag | Polarité | Catégorie | Nom critère | But | Propriété | 
                // Valeur propriété | Condition propriété | Valeur inférieur propriété | Valeur supérieure propriété | 
                // Unité propriété | Assertion | Explanation | Mots-clés | RègleFiltrage | LabelsSynonymes | 
                // RègleSelection | PropriétésAntinomie
                Sheet paramsSheet = workbook.getSheet("Paramètres Tag");
                if (paramsSheet != null) {
                    for (Row row : paramsSheet) {
                        if (row.getRowNum() == 0) continue; // header
                        
                        String idTag = getCellValue(row.getCell(0));
                        String nomTag = getCellValue(row.getCell(1));
                        String polarite = getCellValue(row.getCell(2));
                        // String categorie = getCellValue(row.getCell(3));
                        String nomCritere = getCellValue(row.getCell(4));
                        String but = getCellValue(row.getCell(5));
                        String propriete = getCellValue(row.getCell(6));
                        String valeurPropriete = getCellValue(row.getCell(7));
                        String conditionPropriete = getCellValue(row.getCell(8));
                        String valeurInfPropriete = getCellValue(row.getCell(9));
                        String valeurSupPropriete = getCellValue(row.getCell(10));
                        String unitePropriete = getCellValue(row.getCell(11));
                        String assertionVal = getCellValue(row.getCell(12));
                        String explanationVal = getCellValue(row.getCell(13));
                        String motsCles = getCellValue(row.getCell(14));
                        String regleFiltrage = getCellValue(row.getCell(15));
                        String labelsSynonymes = getCellValue(row.getCell(16));
                        String regleSelection = getCellValue(row.getCell(17));
                        String proprietesAntinomie = getCellValue(row.getCell(18));

                        // Ne créer un TagArgumentBinding que si IDTag est non vide
                        if (idTag != null && !idTag.isEmpty()) {
                            // Vérifier qu'il y a au moins une info utile
                            boolean hasData = (polarite != null && !polarite.isEmpty())
                                    || (nomCritere != null && !nomCritere.isEmpty())
                                    || (but != null && !but.isEmpty())
                                    || (propriete != null && !propriete.isEmpty())
                                    || (valeurPropriete != null && !valeurPropriete.isEmpty())
                                    || (motsCles != null && !motsCles.isEmpty())
                                    || (regleFiltrage != null && !regleFiltrage.isEmpty())
                                    || (labelsSynonymes != null && !labelsSynonymes.isEmpty())
                                    || (regleSelection != null && !regleSelection.isEmpty())
                                    || (proprietesAntinomie != null && !proprietesAntinomie.isEmpty());

                            if (hasData) {
                                String tagLabel = null;
                                if (tagMap.containsKey(idTag)) {
                                    try {
                                        tagLabel = tagMap.get(idTag).getProperty(prefLabel).getString();
                                    } catch (Exception ignored) {}
                                }
                                if (tagLabel == null && nomTag != null && !nomTag.isEmpty()) {
                                    tagLabel = nomTag;
                                }
                                
                                String baseLabel = (tagLabel != null && !tagLabel.isEmpty()) ? tagLabel : idTag;
                                String safeLabel = baseLabel
                                        .trim()
                                        .replaceAll("\\s+", "-")
                                        .replaceAll("[^A-Za-z0-9_-]", "");
                                
                                int nextIndex = argCountersByLabel.getOrDefault(safeLabel, 0) + 1;
                                String argId = "TagBind-" + safeLabel + "-" + nextIndex;
                                
                                // Éviter collision
                                while (om.containsResource(om.createResource(ncl + argId))) {
                                    nextIndex++;
                                    argId = "TagBind-" + safeLabel + "-" + nextIndex;
                                }
                                argCountersByLabel.put(safeLabel, nextIndex);
                                
                                Individual binding = om.createIndividual(ncl + argId, TagArgumentBinding);

                                // Propriétés d'argument
                                if (polarite != null && !polarite.isEmpty()) binding.addProperty(polarity, polarite);
                                if (nomCritere != null && !nomCritere.isEmpty()) binding.addProperty(nameCriterion, nomCritere);
                                if (but != null && !but.isEmpty()) binding.addProperty(aim, but);
                                if (propriete != null && !propriete.isEmpty()) binding.addProperty(nameProperty, propriete);
                                if (valeurPropriete != null && !valeurPropriete.isEmpty()) binding.addProperty(valueProperty, valeurPropriete);
                                if (conditionPropriete != null && !conditionPropriete.isEmpty()) binding.addProperty(condition, conditionPropriete);
                                if (valeurInfPropriete != null && !valeurInfPropriete.isEmpty()) binding.addProperty(infValue, valeurInfPropriete);
                                if (valeurSupPropriete != null && !valeurSupPropriete.isEmpty()) binding.addProperty(supValue, valeurSupPropriete);
                                if (unitePropriete != null && !unitePropriete.isEmpty()) binding.addProperty(unit, unitePropriete);
                                if (assertionVal != null && !assertionVal.isEmpty()) binding.addProperty(assertion, assertionVal);
                                if (explanationVal != null && !explanationVal.isEmpty()) binding.addProperty(explanation, explanationVal);
                                if (motsCles != null && !motsCles.isEmpty()) binding.addProperty(keywordsProperty, motsCles);
                                
                                // Propriétés de configuration
                                if (regleFiltrage != null && !regleFiltrage.isEmpty()) binding.addProperty(filteringRule, regleFiltrage);
                                if (labelsSynonymes != null && !labelsSynonymes.isEmpty()) binding.addProperty(synonymLabels, labelsSynonymes);
                                if (regleSelection != null && !regleSelection.isEmpty()) binding.addProperty(selectionRule, regleSelection);
                                if (proprietesAntinomie != null && !proprietesAntinomie.isEmpty()) binding.addProperty(antinomyProperties, proprietesAntinomie);

                                // Lien vers le Tag
                                if (tagMap.containsKey(idTag)) {
                                    binding.addProperty(aboutTag, tagMap.get(idTag));
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
