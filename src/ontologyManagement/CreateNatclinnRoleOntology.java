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
 * Programme de création de l'ABox des rôles d'additifs à partir
 * du fichier Excel AdditiveRoleAbox.xlsx.
 *
 * Onglet "RoleAdditif" : IDRole | Nom Role
 * Onglet "CritéreEtPropriété" : IDRole | Nom Role | Role Requise | Attribut | Description | Polarité | Nom critère | Propriété | Valeur propriété
 *
 * Pour chaque rôle on crée un individu de classe AdditiveRole.
 * Pour chaque ligne critère/propriété associée à une rôle on crée un individu Argument
 * (réutilisation du modèle existant) et on le lie à la rôle via la propriété aboutRole.
 */
public class CreateNatclinnRoleOntology {

    public static void main(String[] args) {
        new NatclinnConf();
        String excelFile = NatclinnConf.folderForData + "/AdditiveRoleAbox.xlsx";
        String json = createRoleABox(excelFile);
        if (json == null) {
            System.out.println("Modèle vide – arrêt.");
            return;
        }
        OntModel om = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
        RDFParser.create().source(new StringReader(json)).lang(Lang.JSONLD).parse(om);
        try (FileOutputStream out = new FileOutputStream(NatclinnConf.folderForOntologies + "/NatclinnRoleAbox.xml")) {
            om.write(out, "RDF/XML");
        } catch (IOException e) {
            System.err.println("Erreur écriture RDF/XML: " + e.getMessage());
        }
    }

    public static String createRoleABox(String excelFile) {
        OntModel om = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);

        // Namespaces
        String ncl = NatclinnConf.ncl; om.setNsPrefix("ncl", ncl);
        String skos = NatclinnConf.skos; om.setNsPrefix("skos", skos);
        String rdfs = NatclinnConf.rdfs; om.setNsPrefix("rdfs", rdfs);
        String rdf = NatclinnConf.rdf; om.setNsPrefix("rdf", rdf);
        String dcterms = NatclinnConf.dcterms; om.setNsPrefix("dcterms", dcterms);

        Ontology ont = om.createOntology(ncl + "NatclinnRoleAbox");
        om.add(ont, RDFS.label, "Ontology of Natclinn Additive Roles");
        om.add(ont, DC.description, "ABox for additive roles and related criteria");

        OntClass NCL = om.createClass(ncl + "NCL");
	    NCL.addComment("NCL is the set of product and arguments.", "en");
		NCL.addComment("NCL est l'ensemble des produits et des arguments.", "fr");

		// NCLCOT est un ensemble d'atributs de classification
	    OntClass NCLRA = om.createClass(ncl + "NCLRA");
		NCLRA.addComment("NCLRA is the set of additive roles.", "en");
		NCLRA.addComment("NCLRA est l'ensemble des rôles d'additifs.", "fr");
	    NCLRA.addSuperClass(NCL);

        // Classes
        // Resource NCL = om.createResource(ncl + "NCL");
        OntClass AdditiveRole = om.createClass(ncl + "AdditiveRole");
        OntClass AdditiveRoleArgumentBinding = om.createClass(ncl + "AdditiveRoleArgumentBinding");
        OntClass Attribute = om.createClass(ncl + "Attribute");
        
        AdditiveRole.addSuperClass(NCLRA);
        AdditiveRoleArgumentBinding.addSuperClass(NCLRA);
        Attribute.addSuperClass(NCLRA);

        /////////////////////////////
	    // Inclusion de concepts   //
	    /////////////////////////////
	
	    // Note: Les relations de subsomption de classes ne sont plus créées avec Model
        // NCL.addSubClass(AdditiveRole);
        // NCL.addSubClass(AdditiveRoleArgumentBinding);
        // NCL.addSubClass(Attribute);

        // Properties
        Property rdfType = om.createProperty(rdf, "type");
        ObjectProperty hasAttribute = om.createObjectProperty(ncl + "hasBindingAgentAttribute");
        ObjectProperty aboutRole = om.createObjectProperty(ncl + "aboutRole"); // lien AdditiveRoleArgumentBinding -> Role
        DatatypeProperty roleRequired = om.createDatatypeProperty(ncl + "roleRequired");
        DatatypeProperty descriptionProp = om.createDatatypeProperty(ncl + "bindingAgentDescription");
        DatatypeProperty polarity = om.createDatatypeProperty(ncl + "bindingAgentPolarity");
        DatatypeProperty nameCriterion = om.createDatatypeProperty(ncl + "bindingAgentNameCriterion");
        DatatypeProperty nameProperty = om.createDatatypeProperty(ncl + "bindingAgentNameProperty");
        DatatypeProperty valueProperty = om.createDatatypeProperty(ncl + "bindingAgentValueProperty");
        AnnotationProperty prefLabel = om.createAnnotationProperty(skos + "prefLabel");

        Map<String, Individual> roleMap = new HashMap<>();
        Map<String, Individual> attributeMap = new HashMap<>();
        Map<String, Integer> argCountersByLabel = new HashMap<>();

        try (FileInputStream fis = new FileInputStream(excelFile)) {
            ZipSecureFile.setMinInflateRatio(0.001);
            try (Workbook workbook = new XSSFWorkbook(fis)) {
                // Sheet RoleAdditif
                Sheet rolesSheet = workbook.getSheet("RoleAdditif");
                if (rolesSheet != null) {
                    for (Row row : rolesSheet) {
                        if (row.getRowNum() == 0) continue; // header
                        String idRole = getCellValue(row.getCell(0));
                        String nomRole = getCellValue(row.getCell(1));
                        if (idRole != null && !idRole.isEmpty()) {
                            Individual f = om.createIndividual(ncl + nomRole.replaceAll("\\s+", "_"), AdditiveRole);
                            if (nomRole != null && !nomRole.isEmpty()) {
                                f.addProperty(prefLabel, nomRole);
                            }
                            roleMap.put(idRole, f);
                        }
                    }
                }

                // Sheet CritéreEtPropriété
                Sheet critSheet = workbook.getSheet("CritéreEtPropriété");
                if (critSheet != null) {
                    for (Row row : critSheet) {
                        if (row.getRowNum() == 0) continue; // header
                        String idRole = getCellValue(row.getCell(0));
                        // Ignorer la colonne B (Nom Role) pour cet onglet
                        String roleRequis = getCellValue(row.getCell(2));
                        String attribut = getCellValue(row.getCell(3));
                        String description = getCellValue(row.getCell(4));
                        String polarite = getCellValue(row.getCell(5));
                        String nomCritere = getCellValue(row.getCell(6));
                        String propriete = getCellValue(row.getCell(7));
                        String valeurPropriete = getCellValue(row.getCell(8));

                        // Ne créer un AdditiveRoleArgumentBinding que si IDRole est non vide
                        if (idRole != null && !idRole.isEmpty()) {
                            // Et s'il y a au moins une info utile
                            if ((propriete != null && !propriete.isEmpty()) || (description != null && !description.isEmpty())
                                    || (roleRequis != null && !roleRequis.isEmpty()) || (attribut != null && !attribut.isEmpty())
                                    || (nomCritere != null && !nomCritere.isEmpty()) || (valeurPropriete != null && !valeurPropriete.isEmpty())) {
                                String rolLabel = null;
                                if (roleMap.containsKey(idRole)) {
                                    try {
                                        rolLabel = roleMap.get(idRole).getProperty(prefLabel).getString();
                                    } catch (Exception ignored) {}
                                }
                                String baseLabel = (rolLabel != null && !rolLabel.isEmpty()) ? rolLabel : idRole;
                                String safeLabel = baseLabel
                                        .trim()
                                        .replaceAll("\\s+", "-")
                                        .replaceAll("[^A-Za-z0-9_-]", "");
                                int nextIndex = argCountersByLabel.getOrDefault(safeLabel, 0) + 1;
                                // Identifiant de l'individu AdditiveRoleArgumentBinding (ancien préfixe ArgDet- puis AFAB- remplacé par RolBind-)
                                String argId = "RolBind-" + safeLabel + "-" + nextIndex;
                                // Sécurité: éviter collision si précédemment créé
                                while (om.containsResource(om.createResource(ncl + argId))) {
                                    nextIndex++;
                                    // Re-génération de l'identifiant avec le nouveau préfixe RolBind-
                                    argId = "RolBind-" + safeLabel + "-" + nextIndex;
                                }
                                argCountersByLabel.put(safeLabel, nextIndex);
                                Individual arg = om.createIndividual(ncl + argId, AdditiveRoleArgumentBinding);

                                if (description != null && !description.isEmpty()) arg.addProperty(descriptionProp, description);;
                                if (polarite != null && !polarite.isEmpty()) arg.addProperty(polarity, polarite);
                                if (nomCritere != null && !nomCritere.isEmpty()) arg.addProperty(nameCriterion, nomCritere);
                                if (propriete != null && !propriete.isEmpty()) arg.addProperty(nameProperty, propriete);
                                if (valeurPropriete != null && !valeurPropriete.isEmpty()) arg.addProperty(valueProperty, valeurPropriete);
                                if (roleRequis != null && !roleRequis.isEmpty()) arg.addProperty(roleRequired, roleRequis);

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

                                // Lien vers la rôle (IDRole est non vide et attendu existant)
                                if (roleMap.containsKey(idRole)) {
                                    arg.addProperty(aboutRole, roleMap.get(idRole));
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
