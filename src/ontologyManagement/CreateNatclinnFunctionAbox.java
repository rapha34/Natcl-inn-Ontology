package ontologyManagement;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import org.apache.jena.ontology.AnnotationProperty;
import org.apache.jena.ontology.DatatypeProperty;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.ontology.Ontology;
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
 * Programme de création de l'ABox des fonctions d'additifs à partir
 * du fichier Excel AdditiveFunctionAbox.xlsx.
 *
 * Onglet "FonctionAdditif" : IDFonction | Nom Fonction
 * Onglet "CritéreEtPropriété" : IDFonction | Nom Fonction | Fonction Requise | Attribut | Description | Polarité | Nom critère | Propriété | Valeur propriété
 *
 * Pour chaque fonction on crée un individu de classe AdditiveFunction.
 * Pour chaque ligne critère/propriété associée à une fonction on crée un individu Argument
 * (réutilisation du modèle existant) et on le lie à la fonction via la propriété aboutFunction.
 */
public class CreateNatclinnFunctionAbox {

    public static void main(String[] args) {
        new NatclinnConf();
        String excelFile = NatclinnConf.folderForData + "/AdditiveFunctionAbox.xlsx";
        String json = createFunctionABox(excelFile);
        if (json == null) {
            System.out.println("Modèle vide – arrêt.");
            return;
        }
        OntModel om = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
        RDFParser.create().source(new StringReader(json)).lang(Lang.JSONLD11).parse(om);
        try (FileOutputStream out = new FileOutputStream(NatclinnConf.folderForOntologies + "/NatclinnFunctionAbox.xml")) {
            om.write(out, "RDF/XML");
        } catch (IOException e) {
            System.err.println("Erreur écriture RDF/XML: " + e.getMessage());
        }
    }

    public static String createFunctionABox(String excelFile) {
        OntModel om = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);

        // Namespaces
        String ncl = NatclinnConf.ncl; om.setNsPrefix("ncl", ncl);
        String skos = NatclinnConf.skos; om.setNsPrefix("skos", skos);
        String rdfs = NatclinnConf.rdfs; om.setNsPrefix("rdfs", rdfs);
        String rdf = NatclinnConf.rdf; om.setNsPrefix("rdf", rdf);
        String dcterms = NatclinnConf.dcterms; om.setNsPrefix("dcterms", dcterms);

        Ontology ont = om.createOntology(ncl + "NatclinnFunctionAbox");
        om.add(ont, RDFS.label, "Ontology of Natclinn Additive Functions");
        om.add(ont, DC.description, "ABox for additive functions and related criteria");

        // Classes
        OntClass NCL = om.createClass(ncl + "NCL");
        OntClass AdditiveFunction = om.createClass(ncl + "AdditiveFunction");
        OntClass AdditiveFunctionArgumentBinding = om.createClass(ncl + "AdditiveFunctionArgumentBinding");
        OntClass Attribute = om.createClass(ncl + "Attribute");

        /////////////////////////////
	    // Inclusion de concepts   //
	    /////////////////////////////
	
	    NCL.addSubClass(AdditiveFunction);
        NCL.addSubClass(AdditiveFunctionArgumentBinding);
        NCL.addSubClass(Attribute);

        // Properties
        Property rdfType = om.createProperty(rdf, "type");
        Property hasAttribute = om.createProperty(ncl, "hasBindingAgentAttribute");
        Property aboutFunction = om.createProperty(ncl, "aboutFunction"); // lien AdditiveFunctionArgumentBinding -> Fonction
        DatatypeProperty functionRequired = om.createDatatypeProperty(ncl + "functionRequired");
        DatatypeProperty descriptionProp = om.createDatatypeProperty(ncl + "bindingAgentDescription");
        DatatypeProperty polarity = om.createDatatypeProperty(ncl + "bindingAgentPolarity");
        DatatypeProperty nameCriterion = om.createDatatypeProperty(ncl + "bindingAgentNameCriterion");
        DatatypeProperty nameProperty = om.createDatatypeProperty(ncl + "bindingAgentNameProperty");
        DatatypeProperty valueProperty = om.createDatatypeProperty(ncl + "bindingAgentValueProperty");
        AnnotationProperty prefLabel = om.createAnnotationProperty(skos + "prefLabel");

        Map<String, Resource> functionMap = new HashMap<>();
        Map<String, Resource> attributeMap = new HashMap<>();
        Map<String, Integer> argCountersByLabel = new HashMap<>();

        try (FileInputStream fis = new FileInputStream(excelFile)) {
            ZipSecureFile.setMinInflateRatio(0.001);
            try (Workbook workbook = new XSSFWorkbook(fis)) {
                // Sheet FonctionAdditif
                Sheet fonctionsSheet = workbook.getSheet("FonctionAdditif");
                if (fonctionsSheet != null) {
                    for (Row row : fonctionsSheet) {
                        if (row.getRowNum() == 0) continue; // header
                        String idFonction = getCellValue(row.getCell(0));
                        String nomFonction = getCellValue(row.getCell(1));
                        if (idFonction != null && !idFonction.isEmpty()) {
                            Resource f = om.createResource(ncl + nomFonction.replaceAll("\\s+", "_"))
                                    .addProperty(rdfType, AdditiveFunction);
                            if (nomFonction != null && !nomFonction.isEmpty()) {
                                f.addLiteral(prefLabel, nomFonction);
                            }
                            functionMap.put(idFonction, f);
                        }
                    }
                }

                // Sheet CritéreEtPropriété
                Sheet critSheet = workbook.getSheet("CritéreEtPropriété");
                if (critSheet != null) {
                    for (Row row : critSheet) {
                        if (row.getRowNum() == 0) continue; // header
                        String idFonction = getCellValue(row.getCell(0));
                        // Ignorer la colonne B (Nom Fonction) pour cet onglet
                        String fonctionRequise = getCellValue(row.getCell(2));
                        String attribut = getCellValue(row.getCell(3));
                        String description = getCellValue(row.getCell(4));
                        String polarite = getCellValue(row.getCell(5));
                        String nomCritere = getCellValue(row.getCell(6));
                        String propriete = getCellValue(row.getCell(7));
                        String valeurPropriete = getCellValue(row.getCell(8));

                        // Ne créer un AdditiveFunctionArgumentBinding que si IDFonction est non vide
                        if (idFonction != null && !idFonction.isEmpty()) {
                            // Et s'il y a au moins une info utile
                            if ((propriete != null && !propriete.isEmpty()) || (description != null && !description.isEmpty())
                                    || (fonctionRequise != null && !fonctionRequise.isEmpty()) || (attribut != null && !attribut.isEmpty())
                                    || (nomCritere != null && !nomCritere.isEmpty()) || (valeurPropriete != null && !valeurPropriete.isEmpty())) {
                                String funcLabel = null;
                                if (functionMap.containsKey(idFonction)) {
                                    try {
                                        funcLabel = functionMap.get(idFonction).getProperty(prefLabel).getString();
                                    } catch (Exception ignored) {}
                                }
                                String baseLabel = (funcLabel != null && !funcLabel.isEmpty()) ? funcLabel : idFonction;
                                String safeLabel = baseLabel
                                        .trim()
                                        .replaceAll("\\s+", "-")
                                        .replaceAll("[^A-Za-z0-9_-]", "");
                                int nextIndex = argCountersByLabel.getOrDefault(safeLabel, 0) + 1;
                                // Identifiant de l'individu AdditiveFunctionArgumentBinding (ancien préfixe ArgDet- puis AFAB- remplacé par FuncBind-)
                                String argId = "FuncBind-" + safeLabel + "-" + nextIndex;
                                // Sécurité: éviter collision si précédemment créé
                                while (om.containsResource(om.createResource(ncl + argId))) {
                                    nextIndex++;
                                    // Re-génération de l'identifiant avec le nouveau préfixe FuncBind-
                                    argId = "FuncBind-" + safeLabel + "-" + nextIndex;
                                }
                                argCountersByLabel.put(safeLabel, nextIndex);
                                Resource arg = om.createResource(ncl + argId).addProperty(rdfType, AdditiveFunctionArgumentBinding);

                                if (description != null && !description.isEmpty()) arg.addLiteral(descriptionProp, description);
                                if (polarite != null && !polarite.isEmpty()) arg.addLiteral(polarity, polarite);
                                if (nomCritere != null && !nomCritere.isEmpty()) arg.addLiteral(nameCriterion, nomCritere);
                                if (propriete != null && !propriete.isEmpty()) arg.addLiteral(nameProperty, propriete);
                                if (valeurPropriete != null && !valeurPropriete.isEmpty()) arg.addLiteral(valueProperty, valeurPropriete);
                                if (fonctionRequise != null && !fonctionRequise.isEmpty()) arg.addLiteral(functionRequired, fonctionRequise);

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

                                // Lien vers la fonction (IDFonction est non vide et attendu existant)
                                if (functionMap.containsKey(idFonction)) {
                                    arg.addProperty(aboutFunction, functionMap.get(idFonction));
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
