package ontologyManagement;

import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
import natclinn.util.NatclinnConf;
import natclinn.util.NatclinnUtil;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Crée une ontologie OWL à partir de la taxonomie des catégories d'Open Food Facts
 * Lit le fichier Excel généré par ExtractOFFCategories
 */
public class CreateOFFCategoriesOntology {
	
	public static void main(String[] args) {
		OntModel om = CreationABox();
		
		if (om == null) {
			System.out.println("✗ Erreur lors de la création de l'ontologie");
			return;
		}
		
		try {   
			String outputFile = NatclinnConf.folderForOntologies + "/NatclinnOFFCategories.xml";
			
			System.out.println("Modèle OWL construit (" + om.size() + " triplets)");
			System.out.println("Écriture en RDF/XML avec buffer...");
			
			FileOutputStream fos = new FileOutputStream(outputFile);
			BufferedOutputStream bos = new BufferedOutputStream(fos, 8192 * 4); // 32KB buffer
			
			// RDFXML_PLAIN: format simplifié sans abbréviations, plus rapide pour grands modèles
			RDFDataMgr.write(bos, om, RDFFormat.RDFXML_PLAIN);
			bos.flush();
			bos.close();
			
			System.out.println("Ontologie OFF Categories générée: " + outputFile);
			
		} catch (FileNotFoundException e) {
			System.out.println("File not found");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("IO problem");
			e.printStackTrace();
		}
	}

	public static OntModel CreationABox() {

		// Initialisation de la configuration
		new NatclinnConf();  

		OntModel om = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);

		///////////////////////////////
	    // Définition des namespaces //
	    ///////////////////////////////

		String ncl = "https://w3id.org/NCL/ontology/";
	    om.setNsPrefix("ncl", ncl);
		String skos = "http://www.w3.org/2004/02/skos/core#";
	    om.setNsPrefix("skos", skos); 
	    String rdfs = "http://www.w3.org/2000/01/rdf-schema#";
	    om.setNsPrefix("rdfs", rdfs);
		String rdf = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	    om.setNsPrefix("rdf", rdf);
		String dc = "http://purl.org/dc/elements/1.1/";
	    om.setNsPrefix("dc", dc);

		/////////////////////////////////////
	    // Description de l'ontologie      //
	    /////////////////////////////////////

		Ontology ont = om.createOntology(ncl + "OFFCategories");
		om.add(ont, RDFS.label, "Open Food Facts Categories Taxonomy");
		om.add(ont, DC.description, "ABox for the OFF categories taxonomy with hierarchical relationships");
		om.add(ont, DC.creator, "Raphaël CONDE SALAZAR");	

        // NCL est un ensemble de produits et d'arguments
	    OntClass NCL = om.createClass(ncl + "NCL");
	    NCL.addComment("NCL is the set of product and arguments.", "en");
		NCL.addComment("NCL est l'ensemble des produits et des arguments.", "fr");

		// NCLOFFT est un ensemble d'éléments de catégorie OFF
	    OntClass NCLOFFC = om.createClass(ncl + "NCLOFFC");
		NCLOFFC.addComment("NCLOFFC is the set of OFF category elements.", "en");
		NCLOFFC.addComment("NCLOFFC est l'ensemble des éléments de catégorie OFF.", "fr");
	    NCLOFFC.addSuperClass(NCL);

		/////////////////////////////////////
	    // Classes de l'ontologie          //
	    /////////////////////////////////////

		// Classe pour la taxonomie OFF des catégories
	    OntClass OFFCategories = om.createClass(ncl + "OFFCategories");
	    OFFCategories.addLabel("Open Food Facts Categories", "en");
	    OFFCategories.addLabel("Catégories Open Food Facts", "fr");
	    OFFCategories.addComment("The Open Food Facts categories taxonomy", "en");
	    OFFCategories.addComment("La taxonomie des catégories Open Food Facts", "fr");
        OFFCategories.addSuperClass(NCLOFFC);

		// Classe pour les catégories de la taxonomie
	    OntClass TaxonomyCategory = om.createClass(ncl + "TaxonomyCategory");
	    TaxonomyCategory.addLabel("Taxonomy Category", "en");
	    TaxonomyCategory.addLabel("Catégorie de taxonomie", "fr");
	    TaxonomyCategory.addComment("A category in the OFF taxonomy", "en");
	    TaxonomyCategory.addComment("Une catégorie dans la taxonomie OFF", "fr");
        TaxonomyCategory.addSuperClass(NCLOFFC);

		/////////////////////////////////////
	    // Propriétés de l'ontologie       //
	    /////////////////////////////////////

		// Propriété : a pour parent (subClassOf équivalent)
		ObjectProperty hasParent = om.createObjectProperty(ncl + "hasParentCategory");
		hasParent.addLabel("has parent category", "en");
		hasParent.addLabel("a pour catégorie parente", "fr");
		hasParent.addDomain(TaxonomyCategory);
		hasParent.addRange(TaxonomyCategory);

		// Propriété : a pour enfant (inverse de hasParent)
		ObjectProperty hasChild = om.createObjectProperty(ncl + "hasChildCategory");
		hasChild.addLabel("has child category", "en");
		hasChild.addLabel("a pour catégorie enfant", "fr");
		hasChild.addDomain(TaxonomyCategory);
		hasChild.addRange(TaxonomyCategory);
		hasChild.addInverseOf(hasParent);

		// Propriété : identifiant OFF (ex: "en:meals")
		DatatypeProperty offId = om.createDatatypeProperty(ncl + "offCategoryId");
		offId.addLabel("OFF category ID", "en");
		offId.addLabel("identifiant OFF catégorie", "fr");
		offId.addDomain(TaxonomyCategory);

		//////////////////////////////
		// Lecture du fichier Excel //
		//////////////////////////////

		try (FileInputStream fis = new FileInputStream(NatclinnConf.folderForData + "/OFF_taxonomy_categories_fr.xlsx");
             Workbook workbook = new XSSFWorkbook(fis)) {
			
			// Map pour stocker les individus créés
			Map<String, Individual> categories = new HashMap<>();
			
			Sheet sheet = workbook.getSheet("Categories");
            if (sheet == null) {
                throw new IllegalArgumentException("Sheet 'Categories' not found in Excel file");
            }

			System.out.println("Lecture du fichier Excel...");
			
			// Première passe : créer tous les individus
			int count = 0;
            for (Row row : sheet) {
				// Ignorer l'en-tête
				if (row.getRowNum() == 0) continue;
				
				String categoryId = getCellValue(row.getCell(0));
				String nameEn = getCellValue(row.getCell(1));
				String nameFr = getCellValue(row.getCell(2));

				if (categoryId.isEmpty()) continue;
				
				// Filtrer uniquement les catégories en: ou fr:
				if (!categoryId.startsWith("en:") && !categoryId.startsWith("fr:")) {
					continue;
				}

				// Créer l'URI unique pour la catégorie
				String categoryUri = NatclinnUtil.makeURI(ncl + "TaxoCategory_", categoryId);

				// Créer l'individu
				Individual category = om.createIndividual(categoryUri, TaxonomyCategory);
				category.addProperty(offId, categoryId);
				
				// Ajouter les labels
				if (!nameEn.isEmpty()) {
					category.addLabel(nameEn, "en");
				}
				if (!nameFr.isEmpty()) {
					category.addLabel(nameFr, "fr");
				}
				
				categories.put(categoryId, category);
				count++;
				
				if (count % 500 == 0) {
					System.out.println("  " + count + " catégories traitées...");
				}
			}

			System.out.println(count + " catégories créées");
			
			// Deuxième passe : créer les relations hiérarchiques
			System.out.println("Création des relations hiérarchiques...");
			int relationCount = 0;
			
			for (Row row : sheet) {
				if (row.getRowNum() == 0) continue;
				
				String categoryId = getCellValue(row.getCell(0));
				String parentsStr = getCellValue(row.getCell(5));
				String childrenStr = getCellValue(row.getCell(6));
				
				if (categoryId.isEmpty()) continue;
				
				// Filtrer uniquement les catégories en: ou fr:
				if (!categoryId.startsWith("en:") && !categoryId.startsWith("fr:")) {
					continue;
				}
				
				Individual category = categories.get(categoryId);
				if (category == null) continue;
				
				// Ajouter les relations parent
				if (!parentsStr.isEmpty()) {
					String[] parents = parentsStr.split(",\\s*");
					for (String parentId : parents) {
						if (!parentId.isEmpty()) {
							Individual parent = categories.get(parentId);
							if (parent != null) {
								category.addProperty(hasParent, parent);
								relationCount++;
							}
						}
					}
				}
				
				// Ajouter les relations enfant
				if (!childrenStr.isEmpty()) {
					String[] children = childrenStr.split(",\\s*");
					for (String childId : children) {
						if (!childId.isEmpty()) {
							Individual child = categories.get(childId);
							if (child != null) {
								category.addProperty(hasChild, child);
								relationCount++;
							}
						}
					}
				}
			}
			
			System.out.println(relationCount + " relations hiérarchiques créées");
			
		} catch (FileNotFoundException e) {
			System.out.println("File not found: " + NatclinnConf.folderForData + "/OFF_taxonomy_categories_fr.xlsx");
			e.printStackTrace();
			return null;
	
		} catch (IOException e) {
            System.out.println("IO error reading Excel file");
            e.printStackTrace();
			return null;
        }
    	
		// Retourner directement le modèle OWL
		System.out.println("Modèle OWL construit avec succès");
		return om;
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
				return cell.getCellFormula();
			default:
				return "";
		}
	}
}
