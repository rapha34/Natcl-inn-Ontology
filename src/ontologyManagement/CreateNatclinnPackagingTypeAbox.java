package ontologyManagement;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
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
public class CreateNatclinnPackagingTypeAbox {

    public static void main(String[] args) {
        new NatclinnConf();
        String excelFile = NatclinnConf.folderForData + "/PackagingTypeAbox.xlsx";
        String json = createPackagingTypeABox(excelFile);
        if (json == null) {
            System.out.println("Modèle vide – arrêt.");
            return;
        }
        Model om = ModelFactory.createDefaultModel();
        RDFParser.create().source(new StringReader(json)).lang(Lang.JSONLD).parse(om);
        try (FileOutputStream out = new FileOutputStream(NatclinnConf.folderForOntologies + "/NatclinnPackagingTypeAbox.xml")) {
            om.write(out, "RDF/XML");
        } catch (IOException e) {
            System.err.println("Erreur écriture RDF/XML: " + e.getMessage());
        }
    }

    public static String createPackagingTypeABox(String excelFile) {
        Model om = ModelFactory.createDefaultModel();

        // Namespaces
        String ncl = NatclinnConf.ncl; om.setNsPrefix("ncl", ncl);
        String skos = NatclinnConf.skos; om.setNsPrefix("skos", skos);
        String rdfs = NatclinnConf.rdfs; om.setNsPrefix("rdfs", rdfs);
        String rdf = NatclinnConf.rdf; om.setNsPrefix("rdf", rdf);
        String dcterms = NatclinnConf.dcterms; om.setNsPrefix("dcterms", dcterms);

        Resource ont = om.createResource(ncl + "NatclinnPackagingTypeAbox");
        om.add(ont, RDFS.label, "Ontology of Natclinn Packaging Types");
        om.add(ont, DC.description, "ABox for packaging types and related criteria");

        // Classes
        Resource PackagingType = om.createResource(ncl + "PackagingType");
        Resource PackagingTypeArgumentBinding = om.createResource(ncl + "PackagingTypeArgumentBinding");
        Resource Attribute = om.createResource(ncl + "Attribute");

        /////////////////////////////
	    // Inclusion de concepts   //
	    /////////////////////////////
	
	    // Note: Les relations de subsomption de classes ne sont plus créées avec Model

        // Properties
        Property rdfType = om.createProperty(rdf, "type");
        Property hasAttribute = om.createProperty(ncl, "hasBindingAgentAttribute");
        Property aboutPackagingType = om.createProperty(ncl, "aboutPackagingType"); // lien PackagingTypeArgumentBinding -> PackagingType
        Property packagingTypeRequired = om.createProperty(ncl + "packagingTypeRequired");
        Property descriptionProp = om.createProperty(ncl + "bindingAgentDescription");
        Property polarity = om.createProperty(ncl + "bindingAgentPolarity");
        Property nameCriterion = om.createProperty(ncl + "bindingAgentNameCriterion");
        Property nameProperty = om.createProperty(ncl + "bindingAgentNameProperty");
        Property valueProperty = om.createProperty(ncl + "bindingAgentValueProperty");
        Property prefLabel = om.createProperty(skos + "prefLabel");

        Map<String, Resource> packagingTypeMap = new HashMap<>();
        Map<String, Resource> attributeMap = new HashMap<>();
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
                            Resource pt = om.createResource(ncl + nomPackagingType.replaceAll("\\s+", "_"))
                                    .addProperty(rdfType, PackagingType);
                            if (nomPackagingType != null && !nomPackagingType.isEmpty()) {
                                pt.addLiteral(prefLabel, nomPackagingType);
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
                                Resource arg = om.createResource(ncl + argId).addProperty(rdfType, PackagingTypeArgumentBinding);

                                if (description != null && !description.isEmpty()) arg.addLiteral(descriptionProp, description);
                                if (polarite != null && !polarite.isEmpty()) arg.addLiteral(polarity, polarite);
                                if (nomCritere != null && !nomCritere.isEmpty()) arg.addLiteral(nameCriterion, nomCritere);
                                if (propriete != null && !propriete.isEmpty()) arg.addLiteral(nameProperty, propriete);
                                if (valeurPropriete != null && !valeurPropriete.isEmpty()) arg.addLiteral(valueProperty, valeurPropriete);
                                if (packagingTypeRequis != null && !packagingTypeRequis.isEmpty()) arg.addLiteral(packagingTypeRequired, packagingTypeRequis);

                                // Attribut (création si nécessaire)
                                if (attribut != null && !attribut.isEmpty()) {
                                    if (!attributeMap.containsKey(attribut)) {
                                        String uriAtt = NatclinnUtil.makeURI(ncl + "Attribute-", attribut.replaceAll("\\s+", "-"));
                                        Resource att = om.createResource(uriAtt).addProperty(rdfType, Attribute)
                                                .addLiteral(prefLabel, attribut);
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
