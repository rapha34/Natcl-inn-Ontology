package ontologyManagement;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

// utiliser le modele ontologique
import org.apache.jena.ontology.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.vocabulary.DC;
import org.apache.jena.vocabulary.RDFS;
import natclinn.util.NatclinnConf;
import natclinn.util.NatclinnUtil;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.openxml4j.util.ZipSecureFile;

// Ce programme lit un fichier Excel contenant des informations sur des produits et leurs composants,
//  construit une représentation OWL (ABox) de ces données en instanciant des individus et en établissant
//  leurs relations, puis exporte le modèle obtenu dans différents formats RDF.

public class CreateNatclinnProductAbox {
	
	public static <ValuesFromRestriction> void main( String[] args ) {

	new NatclinnConf();  
	// Passage du chemin complet du fichier Excel à traiter
    String excelFile = NatclinnConf.folderForData + "/NatclinnProductAbox.xlsx";
    String jsonString = CreationABox(excelFile);
    
    OntModel om = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
    
    RDFParser.create()
        .source(new StringReader(jsonString))
        .lang(Lang.JSONLD11)
        .parse(om);
    
    try {   
        FileOutputStream outStream = new FileOutputStream(NatclinnConf.folderForOntologies + "/NatclinnAbox.xml");
        om.write(outStream, "RDF/XML");
        outStream.close();
    }
    catch (FileNotFoundException e) {System.out.println("File not found");}
    catch (IOException e) {System.out.println("IO problem");}
}
	
// Méthode principale de création de l'ABox à partir du chemin d'un fichier Excel en paramètre d'entrée et 
// retourne une chaîne JSON-LD
public static String CreationABox(String excelFile) {

    String jsonString = null;
    OntModel om = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);

		///////////////////////////////
	    //Définition des namespaces  //
	    ///////////////////////////////

		String ncl = new String("https://w3id.org/NCL/ontology/");
	    om.setNsPrefix("ncl", ncl);
	    String dcat = new String("http://www.w3.org/ns/dcat#/");
	    om.setNsPrefix("dcat", dcat);
		String prov = new String("http://www.w3.org/ns/prov#");
	    om.setNsPrefix("prov", prov);
		String dct = new String("http://purl.org/dc/terms/"); 
	    om.setNsPrefix("dct", dct);
		String skos = new String("http://www.w3.org/2004/02/skos/core#");
	    om.setNsPrefix("skos", skos); 
	    String foaf = new String("http://xmlns.com/foaf/0.1/");
	    om.setNsPrefix("foaf", foaf);
	    String rdfs = new String("http://www.w3.org/2000/01/rdf-schema#");
	    om.setNsPrefix("rdfs", rdfs);
		String rdf = new String("http://www.w3.org/1999/02/22-rdf-syntax-ns#");
	    om.setNsPrefix("rdf", rdf);

		/////////////////////////////////////
	    //Description de l'ontologie       //
	    /////////////////////////////////////

		Ontology ont = om.createOntology(ncl + "NatclinnAbox");
		om.add(ont, RDFS.label,"Ontology of Natclinn");
		om.add(ont, DC.description,"Abox for the Natclinn ontology");
		om.add(ont, DC.creator,"Raphaël CONDE SALAZAR");	

		/////////////////////////////////////
	    // Classes RDF                     //
	    /////////////////////////////////////
        Resource CompositeProduct = om.createResource(ncl + "CompositeProduct");
		Resource SimpleProduct = om.createResource(ncl + "SimpleProduct");
        Resource Ingredient = om.createResource(ncl + "Ingredient");
		Resource QuantifiedElement = om.createResource(ncl + "QuantifiedElement");
        Resource CleanLabel = om.createResource(ncl + "CleanLabel");
        Resource Packaging = om.createResource(ncl + "Packaging");
		Resource ControlledOriginLabel = om.createResource(ncl + "ControlledOriginLabel");
		Resource ManufacturingProcess = om.createResource(ncl + "ManufacturingProcess");
        Resource NutriScore = om.createResource(ncl + "NutriScore");

        /////////////////////////////////////
	    // Propriétés RDF                  //
	    /////////////////////////////////////
		AnnotationProperty prefLabel = om.createAnnotationProperty(skos + "prefLabel");
        Property hasIngredient = om.createProperty(ncl, "hasIngredient");
		Property hasByProduct = om.createProperty(ncl, "hasByProduct");
		Property hasQuantifiedElement = om.createProperty(ncl, "hasQuantifiedElement");
        Property hasCleanLabel = om.createProperty(ncl, "hasCleanLabel");
        Property hasPackaging = om.createProperty(ncl, "hasPackaging");
		Property hasControlledOriginLabel = om.createProperty(ncl, "hasControlledOriginLabel");
		Property hasManufacturingProcess = om.createProperty(ncl, "hasManufacturingProcess");
        Property hasNutriScore = om.createProperty(ncl, "hasNutriScore");
		Property rdfType = om.createProperty(rdf, "type");
		Property refersTo = om.createProperty(ncl, "refersTo");
		Property quantity = om.createProperty(ncl, "quantity");
		Property unit = om.createProperty(ncl, "unit");
		Property percentage = om.createProperty(ncl, "percentage");
		Property rank = om.createProperty(ncl, "rank");

		//////////////////////////////
		// Définition des individus //
		////////////////////////////// 	


		try (FileInputStream fis = new FileInputStream(excelFile)) {
			// Autoriser les fichiers fortement compressés
    		ZipSecureFile.setMinInflateRatio(0.001);
			try (Workbook workbook = new XSSFWorkbook(fis)) {
				Map<String, Resource> products = new HashMap<>();
				Map<String, String> productsURI = new HashMap<>();
				Map<String, Resource> ingredients = new HashMap<>();
				Map<String, String> ingredientsURI = new HashMap<>();
				Map<String, Resource> cleanLabels = new HashMap<>();
				Map<String, Resource> nutriScores = new HashMap<>();
				Map<String, Resource> packagings = new HashMap<>();	
				Map<String, Resource> controlledOriginLabels = new HashMap<>();	
				Map<String, Resource> manufacturingProcesses = new HashMap<>();	
				
				
				Sheet productsSheet = workbook.getSheet("Products");
				for (Row row : productsSheet) {
					if (row.getRowNum() == 0) continue;
					String id = getCellValue(row.getCell(0));
				    String name = getCellValue(row.getCell(1));
					String code = getCellValue(row.getCell(2));
					String typeProduct = getCellValue(row.getCell(3));

					String productURI;
					if (typeProduct.equalsIgnoreCase("Additif")) {
						if (code == null || code.isEmpty()) {
							long counter = NatclinnUtil.getNextProductCounterValue();
							String paddedCounter = String.format("%013d", counter); // format 13 chiffres
							productURI = "A-" + paddedCounter;
						} else {
							productURI = "A-" + code;
						}
					} else {
						if (code == null || code.isEmpty()) {
							long counter = NatclinnUtil.getNextProductCounterValue();
							String paddedCounter = String.format("%013d", counter); // format 13 chiffres
							productURI = "P-" + paddedCounter;
						} else {
							productURI = "P-" + code;
						}
					}
					// System.out.println(productURI);

					Resource product = om.createResource(ncl + productURI)
				            .addProperty(prefLabel, name);
					products.put(id, product);	
					productsURI.put(id, productURI);	
				}

				Sheet ingredientsSheet = workbook.getSheet("Ingredients");
				for (Row row : ingredientsSheet) {
					if (row.getRowNum() == 0) continue;
					String id = getCellValue(row.getCell(0));
				    String name = getCellValue(row.getCell(1));

					String ingredientURI;
					long counter = NatclinnUtil.getNextIngredientCounterValue();
					String paddedCounter = String.format("%013d", counter); // format 13 chiffres
					ingredientURI = "I-" + paddedCounter;

					Resource ingredient = om.createResource(ncl + ingredientURI)
							.addProperty(rdfType, Ingredient)
							.addProperty(prefLabel, name);
					ingredients.put(id, ingredient);
					ingredientsURI.put(id, ingredientURI);	
				}


				Sheet packagingSheet = workbook.getSheet("Packaging");

				for (Row row : packagingSheet) {
					if (row.getRowNum() == 0) continue;

					String prodId = getCellValue(row.getCell(0));
				    String packaging = getCellValue(row.getCell(1));

					Resource product = products.get(prodId);
					
					if (product != null) {
						if (!packaging.isEmpty()) {
							String uri = NatclinnUtil.makeURI(ncl + "Packaging-", packaging);
							if (uri != null) {
								Resource pack = packagings.computeIfAbsent(packaging, val ->
									om.createResource(uri).addProperty(rdfType, Packaging));
								pack.addProperty(prefLabel, packaging);	
								product.addProperty(hasPackaging, pack);
							}
						}
					}

				}

				Sheet controlledOriginLabelSheet = workbook.getSheet("ControlledOriginLabel");

				for (Row row : controlledOriginLabelSheet) {
					if (row.getRowNum() == 0) continue;

					String prodId = getCellValue(row.getCell(0));
				    String controlledOriginLabel = getCellValue(row.getCell(1));

					Resource product = products.get(prodId);
					
					if (product != null) {
						if (!controlledOriginLabel.isEmpty()) {
							String uri = NatclinnUtil.makeURI(ncl + "ControlledOriginLabel-", controlledOriginLabel);
							if (uri != null) {
								Resource pack = controlledOriginLabels.computeIfAbsent(controlledOriginLabel, val ->
									om.createResource(uri).addProperty(rdfType, ControlledOriginLabel));
								pack.addProperty(prefLabel, controlledOriginLabel);
								product.addProperty(hasControlledOriginLabel, pack);
							}
						}
					}

				}

				Sheet cleanLabelSheet = workbook.getSheet("CleanLabel");

				for (Row row : cleanLabelSheet) {
					if (row.getRowNum() == 0) continue;

					String prodId = getCellValue(row.getCell(0));
				    String cleanLabel = getCellValue(row.getCell(1));

					Resource product = products.get(prodId);
					
					if (product != null) {
						if (!cleanLabel.isEmpty()) {
							String uri = NatclinnUtil.makeURI(ncl + "CleanLabel-", cleanLabel);
							if (uri != null) {
								Resource pack = cleanLabels.computeIfAbsent(cleanLabel, val ->
									om.createResource(uri).addProperty(rdfType, CleanLabel));
								pack.addProperty(prefLabel, cleanLabel);
								product.addProperty(hasCleanLabel, pack);
							}
						}
					}

				}

				Sheet manufacturingProcessSheet = workbook.getSheet("ManufacturingProcess");

				for (Row row : manufacturingProcessSheet) {
					if (row.getRowNum() == 0) continue;

					String prodId = getCellValue(row.getCell(0));
				    String manufacturingProcess = getCellValue(row.getCell(1));

					Resource product = products.get(prodId);
					
					if (product != null) {
						if (!manufacturingProcess.isEmpty()) {
							String uri = NatclinnUtil.makeURI(ncl + "ManufacturingProcess-", manufacturingProcess);
							if (uri != null) {
								Resource pack = manufacturingProcesses.computeIfAbsent(manufacturingProcess, val ->
									om.createResource(uri).addProperty(rdfType, ManufacturingProcess));
								pack.addProperty(prefLabel, manufacturingProcess);
								product.addProperty(hasManufacturingProcess, pack);
							}
						}
					}

				}

				Sheet nutriScoreSheet = workbook.getSheet("NutriScore");

				for (Row row : nutriScoreSheet) {
					if (row.getRowNum() == 0) continue;

					String prodId = getCellValue(row.getCell(0));
				    String nutriScore = getCellValue(row.getCell(1));

					Resource product = products.get(prodId);
					
					if (product != null) {
						if (!nutriScore.isEmpty()) {
							String uri = NatclinnUtil.makeURI(ncl + "NutriScore-", nutriScore);
							if (uri != null) {
								Resource pack = nutriScores.computeIfAbsent(nutriScore, val ->
									om.createResource(uri).addProperty(rdfType, NutriScore));
								pack.addProperty(prefLabel, "Nutri-Score " + nutriScore.toUpperCase());
								product.addProperty(hasNutriScore, pack);
							}
						}
					}

				}


				Sheet compositionsSheet = workbook.getSheet("Compositions");

				// Pour suivre si un produit a des composants qui sont d'autres produits
				Map<String, Boolean> isComposite = new HashMap<>();

				for (Row row : compositionsSheet) {
					if (row.getRowNum() == 0) continue;

					String prodId = getCellValue(row.getCell(0));
				    String type = getCellValue(row.getCell(1));
				    String compId = getCellValue(row.getCell(2));
				    Double dQuantity = getNumericCellValueSafe(row.getCell(3));
					String sUnit = getCellValue(row.getCell(4));
					Double dPercentage = getNumericCellValueSafe(row.getCell(5));
					Double rankVal = getNumericCellValueSafe(row.getCell(6));
					Integer iRank = (rankVal != null) ? rankVal.intValue() : null;

					Resource product = products.get(prodId);
					String productURI = productsURI.get(prodId);
					Resource composant = "Ingredient".equalsIgnoreCase(type) ? ingredients.get(compId) : products.get(compId);
					// String composantURI = "Ingredient".equalsIgnoreCase(type) ? ingredientsURI.get(compId) : productsURI.get(compId);

					if (product != null && composant != null) {

						// Ajout du composant la bonne propriété selon le type de composant
						if ("Ingredient".equalsIgnoreCase(type)) {
							product.addProperty(hasIngredient, composant);
							isComposite.putIfAbsent(prodId, false); // Pas un produit composite
						} else {
							product.addProperty(hasByProduct, composant);
							isComposite.put(prodId, true); // Produit composite
						}

						// Création d'une ressource nommée de type QuantifiedElement
						String qElemURI = ncl + "QuantifiedElement_" + productURI + "_" + iRank;
						Resource quantifiedElement = om.createResource(qElemURI)
							.addProperty(rdfType,QuantifiedElement)
							.addProperty(refersTo, composant);
							if (dQuantity != null) quantifiedElement.addLiteral(quantity, dQuantity);
							if (!sUnit.isEmpty()) quantifiedElement.addProperty(unit, sUnit);
							if (dPercentage != null) quantifiedElement.addLiteral(percentage, dPercentage);
							if (iRank != null) quantifiedElement.addLiteral(rank, iRank);

						// Ajout de la propriété générique
						product.addProperty(hasQuantifiedElement, quantifiedElement);
					}
				}

				// Affectation des types aux produits après l'analyse complète
				for (Map.Entry<String, Resource> entry : products.entrySet()) {
					String prodId = entry.getKey();
					Resource product = entry.getValue();

					boolean composite = isComposite.getOrDefault(prodId, false);
					Resource typeURI = composite ? CompositeProduct : SimpleProduct;
					product.addProperty(rdfType, typeURI);
				}
			}

		} catch (FileNotFoundException e) {
			System.out.println("File not found: " + excelFile);
			e.printStackTrace();
	
		
		} catch (IOException e) {
            e.printStackTrace();
        }
    	
				
		
		// Exporte le resultat dans un fichier au format JSON-LD
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		RDFDataMgr.write(out, om, RDFFormat.JSONLD11);
		try {
			jsonString = out.toString("UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
    
		System.out.println(jsonString);
		 
		return jsonString;
	}

	// Méthode utilitaire pour obtenir la valeur d'une cellule en tant que chaîne
    private static String getCellValue(Cell cell) {
		if (cell == null) return "";
		switch (cell.getCellType()) {
			case STRING:
				return cell.getStringCellValue().trim();
			case NUMERIC:
				 // Pour éviter la notation scientifique :
            	return new java.math.BigDecimal(cell.getNumericCellValue()).toPlainString();
			case BOOLEAN:
				return String.valueOf(cell.getBooleanCellValue());
			case FORMULA:
				return cell.getCellFormula();
			default:
				return "";
		}
	}

	private static Double getNumericCellValueSafe(Cell cell) {
		if (cell == null) return null;
		switch (cell.getCellType()) {
			case NUMERIC:
				return cell.getNumericCellValue();
			case STRING:
				try {
					return Double.parseDouble(cell.getStringCellValue().trim());
				} catch (NumberFormatException e) {
					return null;
				}
			default:
				return null;
		}
	}

}