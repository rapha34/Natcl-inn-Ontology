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
import natclinn.util.NatclinnUtil;

/**
 * Programme de création de l'ABox des types d'emballage à partir
 * du fichier Excel PackagingTypeAbox.xlsx.
 *
 * Onglet "PackagingType" : IDPackagingType | Nom PackagingType
 * Onglet "CritéreEtPropriété" : IDPackagingType | Nom PackagingType | PackagingType Requise | Attribut | Description | Polarité | Nom critère | Propriété | Valeur propriété
 *
 * Pour chaque type d'emballage on crée un individu de classe PackagingType.
 * Pour chaque ligne critère/propriété associée à un type d'emballage on crée un individu Argument
 * (réutilisation du modèle existant) et on le lie au type d'emballage via la propriété aboutPackagingType.
 */
public class CreateNatclinnPackagingTypeOntology {

    public static void main(String[] args) {
        new NatclinnConf();
        String excelFile = NatclinnConf.folderForData + "/PackagingTypeAbox.xlsx";
        String json = createPackagingTypeABox(excelFile);
        if (json == null) {
            System.out.println("Modèle vide -> arrêt.");
            return;
        }
        OntModel om = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
        RDFParser.create().source(new StringReader(json)).lang(Lang.JSONLD).parse(om);
        try (FileOutputStream out = new FileOutputStream(NatclinnConf.folderForOntologies + "/NatclinnPackagingTypeAbox.xml")) {
            om.write(out, "RDF/XML");
        } catch (IOException e) {
            System.err.println("Erreur écriture RDF/XML: " + e.getMessage());
        }
    }

    public static String createPackagingTypeABox(String excelFile) {
        OntModel om = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);

        // Namespaces
        String ncl = NatclinnConf.ncl; om.setNsPrefix("ncl", ncl);
        String skos = NatclinnConf.skos; om.setNsPrefix("skos", skos);
        String rdfs = NatclinnConf.rdfs; om.setNsPrefix("rdfs", rdfs);
        String rdf = NatclinnConf.rdf; om.setNsPrefix("rdf", rdf);
        String dcterms = NatclinnConf.dcterms; om.setNsPrefix("dcterms", dcterms);

        Ontology ont = om.createOntology(ncl + "NatclinnPackagingTypeAbox");
        om.add(ont, RDFS.label, "Ontology of Natclinn Packaging Types");
        om.add(ont, DC.description, "ABox for packaging types and related criteria");

        OntClass NCL = om.createClass(ncl + "NCL");
	    NCL.addComment("NCL is the set of product and arguments.", "en");
		NCL.addComment("NCL est l'ensemble des produits et des arguments.", "fr");

		// NCLCOT est un ensemble d'atributs de classification
	    OntClass NCLPT = om.createClass(ncl + "NCLPT");
		NCLPT.addComment("NCLPT is the set of classification attributes.", "en");
		NCLPT.addComment("NCLPT est l'ensemble des attributs de classification.", "fr");
	    NCLPT.addSuperClass(NCL);

        // Classes
        OntClass PackagingType = om.createClass(ncl + "PackagingType");
        OntClass PackagingTypeArgumentBinding = om.createClass(ncl + "PackagingTypeArgumentBinding");
        OntClass Attribute = om.createClass(ncl + "Attribute");

        PackagingType.addSuperClass(NCLPT);
        PackagingTypeArgumentBinding.addSuperClass(NCLPT);
        Attribute.addSuperClass(NCLPT);

        /////////////////////////////
	    // Inclusion de concepts   //
	    /////////////////////////////
	
	    // Note: Les relations de subsomption de classes ne sont plus créées avec Model

        // Properties
        Property rdfType = om.createProperty(rdf, "type");
        ObjectProperty hasAttribute = om.createObjectProperty(ncl + "hasBindingAgentAttribute");
        ObjectProperty aboutPackagingType = om.createObjectProperty(ncl + "aboutPackagingType"); // lien PackagingTypeArgumentBinding -> PackagingType
        DatatypeProperty packagingTypeRequired = om.createDatatypeProperty(ncl + "packagingTypeRequired");
        DatatypeProperty descriptionProp = om.createDatatypeProperty(ncl + "bindingAgentDescription");
        DatatypeProperty polarity = om.createDatatypeProperty(ncl + "bindingAgentPolarity");
        DatatypeProperty nameCriterion = om.createDatatypeProperty(ncl + "bindingAgentNameCriterion");
        DatatypeProperty nameProperty = om.createDatatypeProperty(ncl + "bindingAgentNameProperty");
        DatatypeProperty valueProperty = om.createDatatypeProperty(ncl + "bindingAgentValueProperty");
        AnnotationProperty prefLabel = om.createAnnotationProperty(skos + "prefLabel");

        Map<String, Individual> packagingTypeMap = new HashMap<>();
        Map<String, Individual> attributeMap = new HashMap<>();
        Map<String, Integer> argCountersByLabel = new HashMap<>();

        try (FileInputStream fis = new FileInputStream(excelFile)) {
            ZipSecureFile.setMinInflateRatio(0.001);
            try (Workbook workbook = new XSSFWorkbook(fis)) {
                // Sheet PackagingType
                Sheet packagingTypesSheet = workbook.getSheet("PackagingType");
                if (packagingTypesSheet != null) {
                    for (Row row : packagingTypesSheet) {
                        if (row.getRowNum() == 0) continue; // header
                        String idPackagingType = getCellValue(row.getCell(0));
                        String nomPackagingType = getCellValue(row.getCell(1));
                        if (idPackagingType != null && !idPackagingType.isEmpty()) {
                            Individual pt = om.createIndividual(ncl + nomPackagingType.replaceAll("\\s+", "_"), PackagingType);
                            if (nomPackagingType != null && !nomPackagingType.isEmpty()) {
                                pt.addProperty(prefLabel, nomPackagingType);
                            }
                            packagingTypeMap.put(idPackagingType, pt);
                        }
                    }
                }

                // Sheet CritéreEtPropriété
                Sheet critSheet = workbook.getSheet("CritéreEtPropriété");
                if (critSheet != null) {
                    for (Row row : critSheet) {
                        if (row.getRowNum() == 0) continue; // header
                        String idPackagingType = getCellValue(row.getCell(0));
                        // Ignorer la colonne B (Nom PackagingType) pour cet onglet
                        String packagingTypeRequis = getCellValue(row.getCell(2));
                        String attribut = getCellValue(row.getCell(3));
                        String description = getCellValue(row.getCell(4));
                        String polarite = getCellValue(row.getCell(5));
                        String nomCritere = getCellValue(row.getCell(6));
                        String propriete = getCellValue(row.getCell(7));
                        String valeurPropriete = getCellValue(row.getCell(8));

                        // Ne créer un PackagingTypeArgumentBinding que si IDPackagingType est non vide
                        if (idPackagingType != null && !idPackagingType.isEmpty()) {
                            // Et s'il y a au moins une info utile
                            if ((propriete != null && !propriete.isEmpty()) || (description != null && !description.isEmpty())
                                    || (packagingTypeRequis != null && !packagingTypeRequis.isEmpty()) || (attribut != null && !attribut.isEmpty())
                                    || (nomCritere != null && !nomCritere.isEmpty()) || (valeurPropriete != null && !valeurPropriete.isEmpty())) {
                                String packagingTypeLabel = null;
                                if (packagingTypeMap.containsKey(idPackagingType)) {
                                    try {
                                        packagingTypeLabel = packagingTypeMap.get(idPackagingType).getProperty(prefLabel).getString();
                                    } catch (Exception ignored) {}
                                }
                                String baseLabel = (packagingTypeLabel != null && !packagingTypeLabel.isEmpty()) ? packagingTypeLabel : idPackagingType;
                                String safeLabel = baseLabel
                                        .trim()
                                        .replaceAll("\\s+", "-")
                                        .replaceAll("[^A-Za-z0-9_-]", "");
                                int nextIndex = argCountersByLabel.getOrDefault(safeLabel, 0) + 1;
                                // Identifiant de l'individu PackagingTypeArgumentBinding (préfixe PkgBind-)
                                String argId = "PkgBind-" + safeLabel + "-" + nextIndex;
                                // Sécurité: éviter collision si précédemment créé
                                while (om.containsResource(om.createResource(ncl + argId))) {
                                    nextIndex++;
                                    // Re-génération de l'identifiant avec le préfixe PkgBind-
                                    argId = "PkgBind-" + safeLabel + "-" + nextIndex;
                                }
                                argCountersByLabel.put(safeLabel, nextIndex);
                                Individual arg = om.createIndividual(ncl + argId, PackagingTypeArgumentBinding);

                                if (description != null && !description.isEmpty()) arg.addProperty(descriptionProp, description);;
                                if (polarite != null && !polarite.isEmpty()) arg.addProperty(polarity, polarite);
                                if (nomCritere != null && !nomCritere.isEmpty()) arg.addProperty(nameCriterion, nomCritere);
                                if (propriete != null && !propriete.isEmpty()) arg.addProperty(nameProperty, propriete);
                                if (valeurPropriete != null && !valeurPropriete.isEmpty()) arg.addProperty(valueProperty, valeurPropriete);
                                if (packagingTypeRequis != null && !packagingTypeRequis.isEmpty()) arg.addProperty(packagingTypeRequired, packagingTypeRequis);

                                // Attribut (création si nécessaire)
                                if (attribut != null && !attribut.isEmpty()) {
                                    if (!attributeMap.containsKey(attribut)) {
                                        String uriAtt = NatclinnUtil.makeURI(ncl + "Attribute-", attribut.replaceAll("\\s+", "-"));
                                        Individual att = om.createIndividual(uriAtt, Attribute);
                                        att.addProperty(prefLabel, attribut);
                                        attributeMap.put(attribut, att);
                                    }
                                    arg.addProperty(hasAttribute, attributeMap.get(attribut));
                                }

                                // Lien vers le type d'emballage (IDPackagingType est non vide et attendu existant)
                                if (packagingTypeMap.containsKey(idPackagingType)) {
                                    arg.addProperty(aboutPackagingType, packagingTypeMap.get(idPackagingType));
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
