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
 * Crée une ontologie OWL à partir de la taxonomie des ingrédients d'Open Food Facts
 * Lit le fichier Excel généré par ExtractOFFTaxonomy
 */
public class CreateOFFTaxonomyOntology {
	
	public static void main(String[] args) {
		OntModel om = CreationABox();
		
		if (om == null) {
			System.out.println("✗ Erreur lors de la création de l'ontologie");
			return;
		}
		
		try {   
			String outputFile = NatclinnConf.folderForOntologies + "/NatclinnOFFTaxonomy.xml";
			
			System.out.println("Modèle OWL construit (" + om.size() + " triplets)");
			System.out.println("Écriture en RDF/XML avec buffer...");
			
			FileOutputStream fos = new FileOutputStream(outputFile);
			BufferedOutputStream bos = new BufferedOutputStream(fos, 8192 * 4); // 32KB buffer
			
			// RDFXML_PLAIN: format simplifié sans abbréviations, plus rapide pour grands modèles
			RDFDataMgr.write(bos, om, RDFFormat.RDFXML_PLAIN);
			bos.flush();
			bos.close();
			
			System.out.println("Ontologie OFF Taxonomy générée: " + outputFile);
			
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

		Ontology ont = om.createOntology(ncl + "OFFTaxonomy");
		om.add(ont, RDFS.label, "Open Food Facts Ingredients Taxonomy");
		om.add(ont, DC.description, "ABox for the OFF ingredients taxonomy with hierarchical relationships");
		om.add(ont, DC.creator, "Raphaël CONDE SALAZAR");	

        // NCL est un ensemble de produits et d'arguments
	    OntClass NCL = om.createClass(ncl + "NCL");
	    NCL.addComment("NCL is the set of product and arguments.", "en");
		NCL.addComment("NCL est l'ensemble des produits et des arguments.", "fr");

		// NCLOFFT est un ensemble d'éléments de taxonomie OFF
	    OntClass NCLOFFT = om.createClass(ncl + "NCLOFFT");
		NCLOFFT.addComment("NCLOFFT is the set of OFF taxonomy elements.", "en");
		NCLOFFT.addComment("NCLOFFT est l'ensemble des éléments de taxonomie OFF.", "fr");
	    NCLOFFT.addSuperClass(NCL);

		/////////////////////////////////////
	    // Classes de l'ontologie          //
	    /////////////////////////////////////

		// Classe pour la taxonomie OFF
	    OntClass OFFTaxonomy = om.createClass(ncl + "OFFTaxonomy");
	    OFFTaxonomy.addLabel("Open Food Facts Taxonomy", "en");
	    OFFTaxonomy.addLabel("Taxonomie Open Food Facts", "fr");
	    OFFTaxonomy.addComment("The Open Food Facts ingredients taxonomy", "en");
	    OFFTaxonomy.addComment("La taxonomie des ingrédients Open Food Facts", "fr");
        OFFTaxonomy.addSuperClass(NCLOFFT);

		// Classe pour les ingrédients de la taxonomie
	    OntClass TaxonomyIngredient = om.createClass(ncl + "TaxonomyIngredient");
	    TaxonomyIngredient.addLabel("Taxonomy Ingredient", "en");
	    TaxonomyIngredient.addLabel("Ingrédient de taxonomie", "fr");
	    TaxonomyIngredient.addComment("An ingredient in the OFF taxonomy", "en");
	    TaxonomyIngredient.addComment("Un ingrédient dans la taxonomie OFF", "fr");
        TaxonomyIngredient.addSuperClass(NCLOFFT);

		/////////////////////////////////////
	    // Propriétés de l'ontologie       //
	    /////////////////////////////////////

		// Propriété : a pour parent (subClassOf équivalent)
		ObjectProperty hasParent = om.createObjectProperty(ncl + "hasParentIngredient");
		hasParent.addLabel("has parent ingredient", "en");
		hasParent.addLabel("a pour ingrédient parent", "fr");
		hasParent.addDomain(TaxonomyIngredient);
		hasParent.addRange(TaxonomyIngredient);

		// Propriété : a pour enfant (inverse de hasParent)
		ObjectProperty hasChild = om.createObjectProperty(ncl + "hasChildIngredient");
		hasChild.addLabel("has child ingredient", "en");
		hasChild.addLabel("a pour ingrédient enfant", "fr");
		hasChild.addDomain(TaxonomyIngredient);
		hasChild.addRange(TaxonomyIngredient);
		hasChild.addInverseOf(hasParent);

		// Propriété : identifiant OFF (ex: "en:emmental")
		DatatypeProperty offId = om.createDatatypeProperty(ncl + "offIngredientId");
		offId.addLabel("OFF ingredient ID", "en");
		offId.addLabel("identifiant OFF ingrédient", "fr");
		offId.addDomain(TaxonomyIngredient);

		//////////////////////////////
		// Lecture du fichier Excel //
		//////////////////////////////

		try (FileInputStream fis = new FileInputStream(NatclinnConf.folderForData + "/OFF_taxonomy_ingredients.xlsx");
             Workbook workbook = new XSSFWorkbook(fis)) {
			
			// Map pour stocker les individus créés
			Map<String, Individual> ingredients = new HashMap<>();
			
			Sheet sheet = workbook.getSheet("Ingredients");
            if (sheet == null) {
                throw new IllegalArgumentException("Sheet 'Ingredients' not found in Excel file");
            }

			System.out.println("Lecture du fichier Excel...");
			
			// Première passe : créer tous les individus
			int count = 0;
            for (Row row : sheet) {
				// Ignorer l'en-tête
				if (row.getRowNum() == 0) continue;
				
				String ingredientId = getCellValue(row.getCell(0));
				String nameEn = getCellValue(row.getCell(1));
				String nameFr = getCellValue(row.getCell(2));

				if (ingredientId.isEmpty()) continue;

				// Créer l'URI unique pour l'ingrédient
				String ingredientUri = NatclinnUtil.makeURI(ncl + "TaxoIngredient_", ingredientId);

				// Créer l'individu
				Individual ingredient = om.createIndividual(ingredientUri, TaxonomyIngredient);
				ingredient.addProperty(offId, ingredientId);
				
				// Ajouter les labels
				if (!nameEn.isEmpty()) {
					ingredient.addLabel(nameEn, "en");
				}
				if (!nameFr.isEmpty()) {
					ingredient.addLabel(nameFr, "fr");
				}
				
				ingredients.put(ingredientId, ingredient);
				count++;
				
				if (count % 1000 == 0) {
					System.out.println("  " + count + " ingrédients traités...");
				}
			}

			System.out.println(count + " ingrédients créés");
			
			// Deuxième passe : créer les relations hiérarchiques
			System.out.println("Création des relations hiérarchiques...");
			int relationCount = 0;
			
			for (Row row : sheet) {
				if (row.getRowNum() == 0) continue;
				
				String ingredientId = getCellValue(row.getCell(0));
				String parentsStr = getCellValue(row.getCell(5));
				String childrenStr = getCellValue(row.getCell(6));
				
				if (ingredientId.isEmpty()) continue;
				
				Individual ingredient = ingredients.get(ingredientId);
				if (ingredient == null) continue;
				
				// Ajouter les relations parent
				if (!parentsStr.isEmpty()) {
					String[] parents = parentsStr.split(",\\s*");
					for (String parentId : parents) {
						if (!parentId.isEmpty()) {
							Individual parent = ingredients.get(parentId);
							if (parent != null) {
								ingredient.addProperty(hasParent, parent);
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
							Individual child = ingredients.get(childId);
							if (child != null) {
								ingredient.addProperty(hasChild, child);
								relationCount++;
							}
						}
					}
				}
			}
			
			System.out.println(relationCount + " relations hiérarchiques créées");
			
		} catch (FileNotFoundException e) {
			System.out.println("File not found: " + NatclinnConf.folderForData + "/OFF_taxonomy_ingredients.xlsx");
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
