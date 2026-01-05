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

public class CreateNatclinnProductsAbox {
	
	public static <ValuesFromRestriction> void main( String[] args ) {

	new NatclinnConf();  
	// Passage du chemin complet du fichier Excel à traiter
    String excelFile = NatclinnConf.folderForData + "/NatclinnProductAbox.xlsx";
    String jsonString = CreationProductABox(excelFile);
    
    OntModel om = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
    
    RDFParser.create()
        .source(new StringReader(jsonString))
        .lang(Lang.JSONLD11)
        .parse(om);
    
    try {   
        FileOutputStream outStream = new FileOutputStream(NatclinnConf.folderForOntologies + "/NatclinnProductsAbox.xml");
        om.write(outStream, "RDF/XML");
        outStream.close();
    }
    catch (FileNotFoundException e) {System.out.println("File not found");}
    catch (IOException e) {System.out.println("IO problem");}
}
	
// Méthode principale de création de l'ABox à partir du chemin d'un fichier Excel en paramètre d'entrée et 
// retourne une chaîne JSON-LD
public static String CreationProductABox(String excelFile) {

    String jsonString = null;
    OntModel om = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);

		///////////////////////////////
	    //Définition des namespaces  //
	    ///////////////////////////////

		String ncl = NatclinnConf.ncl;
	    om.setNsPrefix("ncl", ncl);
	    String dcat = NatclinnConf.dcat;
	    om.setNsPrefix("dcat", dcat);
		String prov = NatclinnConf.prov;
	    om.setNsPrefix("prov", prov);
		String dcterms = NatclinnConf.dcterms; 
	    om.setNsPrefix("dcterms", dcterms);
		String skos = NatclinnConf.skos;
	    om.setNsPrefix("skos", skos); 
	    String foaf = NatclinnConf.foaf;
	    om.setNsPrefix("foaf", foaf);
	    String rdfs = NatclinnConf.rdfs;
	    om.setNsPrefix("rdfs", rdfs);
		String rdf = NatclinnConf.rdf;
	    om.setNsPrefix("rdf", rdf);

		/////////////////////////////////////
	    //Description de l'ontologie       //
	    /////////////////////////////////////

		Ontology ont = om.createOntology(ncl + "NatclinnProductAbox");
		om.add(ont, RDFS.label,"Ontology of Natclinn");
		om.add(ont, DC.description,"Abox for the Products of Natcl'inn ontology");
		om.add(ont, DC.creator,"Raphaël CONDE SALAZAR");	

		/////////////////////////////////////
	    // Classes RDF                     //
	    /////////////////////////////////////
		Resource Product = om.createResource(ncl + "Product");
        Resource CompositeProduct = om.createResource(ncl + "CompositeProduct");
		Resource SimpleProduct = om.createResource(ncl + "SimpleProduct");
		Resource Ingredient = om.createResource(ncl + "Ingredient");
		Resource CompositeIngredient = om.createResource(ncl + "CompositeIngredient");
		Resource SimpleIngredient = om.createResource(ncl + "SimpleIngredient");
		Resource QuantifiedElement = om.createResource(ncl + "QuantifiedElement");
        Resource CleanLabel = om.createResource(ncl + "CleanLabel");
        Resource Packaging = om.createResource(ncl + "Packaging");
		Resource Material = om.createResource(ncl + "Material");
		Resource Shape = om.createResource(ncl + "Shape");
		Resource ControlledOriginLabel = om.createResource(ncl + "ControlledOriginLabel");
		Resource ManufacturingProcess = om.createResource(ncl + "ManufacturingProcess");
		Resource NutriScore = om.createResource(ncl + "NutriScore");
		Resource NutriScoreAlpha = om.createResource(ncl + "NutriScoreAlpha");
		Resource AdditiveIngredient = om.createResource(ncl + "AdditiveIngredient");
		Resource NutriScoreDetail = om.createResource(ncl + "NutriScoreDetail");
		Resource NOVAgroupDetails = om.createResource(ncl + "NOVAgroupDetails");        /////////////////////////////////////
	    // Propriétés RDF                  //
	    /////////////////////////////////////
		AnnotationProperty prefLabel = om.createAnnotationProperty(skos + "prefLabel");
		AnnotationProperty hasEAN13 = om.createAnnotationProperty(ncl + "hasEAN13");
		AnnotationProperty hasTrademark = om.createAnnotationProperty(ncl + "hasTrademark");
		AnnotationProperty hasIdIngredientOFF = om.createAnnotationProperty(ncl + "hasIdIngredientOFF");
		Property hasRootCategory = om.createProperty(ncl + "hasRootCategory");
		Property hasLeafCategory = om.createProperty(ncl + "hasLeafCategory");
		Property hasCiqualFoodCode = om.createProperty(ncl + "hasCiqualFoodCode");
		Property hasCiqualProxyFoodCode = om.createProperty(ncl + "hasCiqualProxyFoodCode");
		Property hasComposedOf = om.createProperty(ncl, "hasComposedOf");
        Property hasIngredient = om.createProperty(ncl, "hasIngredient");
		Property hasQuantifiedElement = om.createProperty(ncl, "hasQuantifiedElement");
        Property hasCleanLabel = om.createProperty(ncl, "hasCleanLabel");
        Property hasPackaging = om.createProperty(ncl, "hasPackaging");
		Property hasMaterial = om.createProperty(ncl, "hasMaterial");
		Property hasShape = om.createProperty(ncl, "hasShape");
		Property hasControlledOriginLabel = om.createProperty(ncl, "hasControlledOriginLabel");
		Property hasManufacturingProcess = om.createProperty(ncl, "hasManufacturingProcess");
		Property hasNutriScore = om.createProperty(ncl, "hasNutriScore");
		Property hasNutriScoreAlpha = om.createProperty(ncl, "hasNutriScoreAlpha");
		Property hasNutriScoreNum = om.createProperty(ncl, "hasNutriScoreNum");
		// OFF: groupe NOVA (valeur 1..4) fourni par OFF
		Property hasNOVAgroup = om.createProperty(ncl, "hasNOVAgroup");
		Property hasNOVAgroupDetails = om.createProperty(ncl, "hasNOVAgroupDetails");
		Property groupe1 = om.createProperty(ncl, "groupe1");
		Property groupe2 = om.createProperty(ncl, "groupe2");
		Property groupe3 = om.createProperty(ncl, "groupe3");
		Property groupe4 = om.createProperty(ncl, "groupe4");
		Property hasFoodContact = om.createProperty(ncl, "hasFoodContact");
		Property hasNumberOfUnits = om.createProperty(ncl, "hasNumberOfUnits");
		Property hasWeightSpecified = om.createProperty(ncl, "hasWeightSpecified");
		Property rdfType = om.createProperty(rdf, "type");
		Property refersTo = om.createProperty(ncl, "refersTo");
		Property quantity = om.createProperty(ncl, "quantity");
		Property unit = om.createProperty(ncl, "unit");
		Property percentage = om.createProperty(ncl, "percentage");
		Property rank = om.createProperty(ncl, "rank");
		Property hasPolarityNSCompoment = om.createProperty(ncl, "hasPolarityNSCompoment");
		Property hasNSCompoment = om.createProperty(ncl, "hasNSCompoment");
		Property hasNutriScoreDetail = om.createProperty(ncl, "hasNutriScoreDetail");
		Property hasPoint = om.createProperty(ncl, "hasPoint");
		Property hasPointMax = om.createProperty(ncl, "hasPointMax");
		Property hasValue = om.createProperty(ncl, "hasValue");
		Property hasUnit = om.createProperty(ncl, "hasUnit");

		//////////////////////////////
		// Définition des individus //
		////////////////////////////// 	


		try (FileInputStream fis = new FileInputStream(excelFile)) {
			// Autoriser les fichiers fortement compressés
    		ZipSecureFile.setMinInflateRatio(0.001);
			try (Workbook workbook = new XSSFWorkbook(fis)) {
				Map<String, Resource> products = new HashMap<>();
				Map<String, Resource> ingredients = new HashMap<>();
				Map<String, Resource> cleanLabels = new HashMap<>();
				Map<String, Resource> nutriScoreNodes = new HashMap<>();
				Map<String, Resource> nutriScores = new HashMap<>();
				Map<String, Resource> materials = new HashMap<>();
				Map<String, Resource> shapes = new HashMap<>();				
				Map<String, Resource> controlledOriginLabels = new HashMap<>();	
				Map<String, Resource> manufacturingProcesses = new HashMap<>();	
				
				
				Sheet productsSheet = workbook.getSheet("Products");
				for (Row row : productsSheet) {
					if (row.getRowNum() == 0) continue;
					String id = getCellValue(row.getCell(0));
				    String name = getCellValue(row.getCell(1));
					// le code EAN peut être vide dans le cas d'un sous produit d'un produit composite
					// (ex: surgelé de poisson avec sa sauce holandaise)
					String code = getCellValue(row.getCell(2)); 
					String typeProduct = getCellValue(row.getCell(3));
					String marque = getCellValue(row.getCell(4));
					String rootCategory = getCellValue(row.getCell(8));
					String leafCategory = getCellValue(row.getCell(9));


					Resource product = om.createResource(ncl + id)
				            .addProperty(prefLabel, name);
					if (code != null && !code.isEmpty()) {
						product.addProperty(hasEAN13, code);
						product.addProperty(rdfType, Product);
					}
					if (typeProduct.equals("CompositeProduct") ) {
						product.addProperty(rdfType, CompositeProduct);
					}
					if (typeProduct.equals("SimpleProduct") ) {
						product.addProperty(rdfType, SimpleProduct);
					}
					if (marque != null && !marque.isEmpty()) {
						product.addProperty(hasTrademark, marque);
					}
					if (rootCategory != null && !rootCategory.isEmpty()) {
						product.addProperty(hasRootCategory, rootCategory	);
					}
					if (leafCategory != null && !leafCategory.isEmpty()) {
						product.addProperty(hasLeafCategory, leafCategory);
					}
					products.put(id, product);		
				}

				Sheet ingredientsSheet = workbook.getSheet("Ingredients");
				for (Row row : ingredientsSheet) {
					if (row.getRowNum() == 0) continue;
					String id = getCellValue(row.getCell(0));
				    String name = getCellValue(row.getCell(1));
					String CiqualFoodCode = getCellValue(row.getCell(2));
					String CiqualProxiFoodCode = getCellValue(row.getCell(3));
					String IdIngredientOFF = getCellValue(row.getCell(4));
					if (id == null || id.isEmpty()) {
						continue;
					}
					Resource ingredient = om.createResource(ncl + id)
							.addProperty(rdfType, Ingredient)
							.addProperty(prefLabel, name);
					if (CiqualFoodCode != null && !CiqualFoodCode.isEmpty()) {
						ingredient.addProperty(hasCiqualFoodCode, CiqualFoodCode);
					}
					if (CiqualProxiFoodCode != null && !CiqualProxiFoodCode.isEmpty()) {
						ingredient.addProperty(hasCiqualProxyFoodCode, CiqualProxiFoodCode);
					}
					if (IdIngredientOFF != null && !IdIngredientOFF.isEmpty()) {
						ingredient.addProperty(hasIdIngredientOFF, IdIngredientOFF);
					}
					ingredients.put(id, ingredient);
				}


				Sheet packagingSheet = workbook.getSheet("Packaging");

				for (Row row : packagingSheet) {
					if (row.getRowNum() == 0) continue;

					String prodId = getCellValue(row.getCell(0));
					Double rankPackaging = getNumericCellValueSafe(row.getCell(1));
					String material = getCellValue(row.getCell(2));
					String shape = getCellValue(row.getCell(3));
					String foodContact = getCellValue(row.getCell(4));    
					String numberOfUnits = getCellValue(row.getCell(5));
					String weightSpecified = getCellValue(row.getCell(6));
					Integer iRank = (rankPackaging != null) ? rankPackaging.intValue() : null;
					if (prodId == null || prodId.isEmpty()) {
						continue;
					}
					Resource product = products.get(prodId);
					
					if (product != null) {	
						String uri = NatclinnUtil.makeURI(ncl + "Packaging-", prodId + "-" + iRank);
						if (uri != null) {
							Resource packaging = om.createResource(uri);
							packaging.addProperty(rdfType, Packaging);
							if (material != null && !material.isEmpty()) {
							String uriMat = NatclinnUtil.makeURI(ncl + "Material-", material);
								if (uriMat != null) {
								Resource mat = materials.computeIfAbsent(material, val ->
									om.createResource(uriMat).addProperty(rdfType, Material));
								mat.addProperty(prefLabel, material);	
								packaging.addProperty(hasMaterial, mat);
								}
							}
							if (shape != null && !shape.isEmpty()) {
							String uriShap = NatclinnUtil.makeURI(ncl + "shape-", shape);
								if (uriShap != null) {
								Resource shap = shapes.computeIfAbsent(shape, val ->
									om.createResource(uriShap).addProperty(rdfType, Shape));
								shap.addProperty(prefLabel, shape);	
								packaging.addProperty(hasShape, shap);
								}
							}
							if (foodContact != null && !foodContact.isEmpty()) {
								packaging.addProperty(hasFoodContact, foodContact);
							}
							if (numberOfUnits != null && !numberOfUnits.isEmpty()) {
								packaging.addProperty(hasNumberOfUnits, numberOfUnits);
							}
							if (weightSpecified != null && !weightSpecified.isEmpty()) {
								packaging.addProperty(hasWeightSpecified, weightSpecified);
							}
							product.addProperty(hasPackaging, packaging);
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
								Resource cLabel = cleanLabels.computeIfAbsent(cleanLabel, val ->
									om.createResource(uri).addProperty(rdfType, CleanLabel));
								cLabel.addProperty(prefLabel, cleanLabel);
								product.addProperty(hasCleanLabel, cLabel);
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
					String Score = getCellValue(row.getCell(2));

					Resource product = products.get(prodId);
					
					if (product != null) {
						if (!nutriScore.isEmpty()) {
							String uri = NatclinnUtil.makeURI(ncl + "NutriScore-", prodId);
							if (uri != null) {
								Resource nutriscoreNode = nutriScoreNodes.computeIfAbsent(prodId, val ->
									om.createResource(uri).addProperty(rdfType, NutriScore));
								product.addProperty(hasNutriScore, nutriscoreNode);	
								String uriScoreAlpha = NatclinnUtil.makeURI(ncl + "NutriScore-", nutriScore);
								if (uriScoreAlpha != null) {
									Resource nutriscore = nutriScores.computeIfAbsent(nutriScore, val ->
										om.createResource(uriScoreAlpha).addProperty(rdfType, NutriScoreAlpha));
									nutriscore.addProperty(prefLabel, "Nutri-Score " + nutriScore.toUpperCase());
									nutriscoreNode.addProperty(hasNutriScoreAlpha, nutriscore);
									if (!Score.isEmpty()) {
										nutriscoreNode.addProperty(hasNutriScoreNum, Score);
									}	
								}
							}
						}
					}

				}

				Sheet nutriScoreDetailsSheet = workbook.getSheet("NutriScoreDetails");

				for (Row row : nutriScoreDetailsSheet) {
					if (row.getRowNum() == 0) continue;

					String prodId = getCellValue(row.getCell(0));
					String PolarityComponent = getCellValue(row.getCell(1));
					Double rankVal = getNumericCellValueSafe(row.getCell(2));
					Integer iRank = (rankVal != null) ? rankVal.intValue() : null;
					String IDNScomponent = getCellValue(row.getCell(3));
					Double points = getNumericCellValueSafe(row.getCell(4));
					Double points_max = getNumericCellValueSafe(row.getCell(5));
					Double value = getNumericCellValueSafe(row.getCell(6));
					String unitval = getCellValue(row.getCell(7));	

					Resource product = products.get(prodId);
					Resource nutriscoreNode = nutriScoreNodes.get(prodId);
					
					
					if (product != null && nutriscoreNode != null && iRank != null) {
						// Création d'une ressource nommée de type NutriscoreDetail
						String nutriscoreDetailURI = ncl + "NutriscoreDetail-" + prodId + "-" + PolarityComponent + "-"+ iRank;
						Resource nutriscoreDetail = om.createResource(nutriscoreDetailURI)
							.addProperty(rdfType,NutriScoreDetail);
						if (PolarityComponent != null && !PolarityComponent.isEmpty()) {
							nutriscoreDetail.addProperty(hasPolarityNSCompoment,
									om.createResource(ncl + PolarityComponent));
						}
						if (IDNScomponent != null && !IDNScomponent.isEmpty()) {
							nutriscoreDetail.addProperty(hasNSCompoment, om.createResource(ncl + IDNScomponent));
						}
						if (points != null) {
							nutriscoreDetail.addLiteral(hasPoint, points);
						}
						if (points_max != null) {
							nutriscoreDetail.addLiteral(hasPointMax, points_max);
						}
						if (value != null) {
							nutriscoreDetail.addLiteral(hasValue, value);
						}
						if (unitval != null && !unitval.isEmpty()) {
							nutriscoreDetail.addProperty(hasUnit, unitval);
						}

						nutriscoreNode.addProperty(hasNutriScoreDetail, nutriscoreDetail);
						
					}

				}

				// Lecture de l'onglet Nova (OFF) pour rattacher le groupe NOVA aux produits
				Sheet novaSheet = workbook.getSheet("Nova");

				if (novaSheet != null) {
					for (Row row : novaSheet) {
						if (row.getRowNum() == 0) continue; // entête

						String prodId = getCellValue(row.getCell(0)); // IDProduit
						// Le groupe NOVA peut être stocké comme numérique ou texte
						Double gNovaNum = getNumericCellValueSafe(row.getCell(1)); // GroupeNova
						String gNovaTxt = getCellValue(row.getCell(1));
						String g1 = getCellValue(row.getCell(2)); // Groupe1
						String g2 = getCellValue(row.getCell(3)); // Groupe2
						String g3 = getCellValue(row.getCell(4)); // Groupe3
						String g4 = getCellValue(row.getCell(5)); // Groupe4

						if (prodId == null || prodId.isEmpty()) {
							continue;
						}

						Resource product = products.get(prodId);
						if (product != null) {
							Integer group = null;
							if (gNovaNum != null) {
								group = gNovaNum.intValue();
							} else if (gNovaTxt != null && !gNovaTxt.isEmpty()) {
								try {
									// Autoriser d'éventuels formats "4" ou "4.0"
									double d = Double.parseDouble(gNovaTxt.trim());
									group = (int) Math.round(d);
								} catch (NumberFormatException e) {
									group = null;
								}
							}

							if (group != null && group >= 1 && group <= 4) {
								product.addLiteral(hasNOVAgroup, group);
								
								// Créer un nœud NOVAgroupDetails avec les listes groupe1..4
								String detailsURI = ncl + "NOVAgroupDetails-" + prodId;
								Resource novaDetails = om.createResource(detailsURI)
									.addProperty(rdfType, NOVAgroupDetails);
								
								if (g1 != null && !g1.isEmpty()) {
									novaDetails.addProperty(groupe1, g1);
								}
								if (g2 != null && !g2.isEmpty()) {
									novaDetails.addProperty(groupe2, g2);
								}
								if (g3 != null && !g3.isEmpty()) {
									novaDetails.addProperty(groupe3, g3);
								}
								if (g4 != null && !g4.isEmpty()) {
									novaDetails.addProperty(groupe4, g4);
								}
								
								product.addProperty(hasNOVAgroupDetails, novaDetails);
							}
						}
					}
				}


				Sheet compositionsSheet = workbook.getSheet("Compositions");

				// Pour suivre si un produit a des composants qui sont d'autres produits
				Map<String, Boolean> isComposite = new HashMap<>();

				for (Row row : compositionsSheet) {
					if (row.getRowNum() == 0) continue;

					String compoundId = getCellValue(row.getCell(0));
				    String type = getCellValue(row.getCell(1));
				    String componentId = getCellValue(row.getCell(2));
				    Double dQuantity = getNumericCellValueSafe(row.getCell(3));
					String sUnit = getCellValue(row.getCell(4));
					Double dPercentage = getNumericCellValueSafe(row.getCell(5));
					Double rankVal = getNumericCellValueSafe(row.getCell(6));
					Integer iRank = (rankVal != null) ? rankVal.intValue() : null;
					if (compoundId == null || compoundId.isEmpty()) {
						continue;
					}

					Resource compound = products.get(compoundId) != null ? products.get(compoundId) : ingredients.get(compoundId);
					Resource component = ingredients.get(componentId) != null ? ingredients.get(componentId) : products.get(componentId);
					Boolean compoundIsProduct = products.get(compoundId) != null ? true : false;
					Boolean componentIsProduct = products.get(componentId) != null ? true : false;

					if (compound != null && component != null) {

						// Création d'une ressource nommée de type QuantifiedElement
						String qElemURI = ncl + "QuantifiedElement-" + compound + "-" + iRank;
						Resource quantifiedElement = om.createResource(qElemURI)
							.addProperty(rdfType,QuantifiedElement)
							.addProperty(refersTo, component);
							if (dQuantity != null) quantifiedElement.addLiteral(quantity, dQuantity);
							if (!sUnit.isEmpty()) quantifiedElement.addProperty(unit, sUnit);
							if (dPercentage != null) quantifiedElement.addLiteral(percentage, dPercentage);
							if (iRank != null) quantifiedElement.addLiteral(rank, iRank);

						// Ajout de la propriété générique
						compound.addProperty(hasQuantifiedElement, quantifiedElement);
						// Ajout de la propriété spécifique selon le type
						if ("AdditiveIngredient".equalsIgnoreCase(type)) {
							component.addProperty(rdfType, AdditiveIngredient);
						}
						// Détermination de la nature de la relation entre le composé et le composant
						// selon qu'ils sont des produits ou des ingrédients
						if (compoundIsProduct && componentIsProduct) {
							compound.addProperty(hasComposedOf, component);
						} else if (compoundIsProduct && !componentIsProduct) {
							compound.addProperty(hasIngredient, component);
						} else if (!compoundIsProduct && !componentIsProduct) {
							compound.addProperty(hasComposedOf, component);
							isComposite.put(compoundId, true);
						}

					}
				}

				// Affectation des types aux ingrédients après l'analyse complète
				for (Map.Entry<String, Resource> entry : ingredients.entrySet()) {
					String ingredientId = entry.getKey();
					Resource ingredient = entry.getValue();

					boolean composite = isComposite.getOrDefault(ingredientId, false);
					Resource typeURI = composite ? CompositeIngredient : SimpleIngredient;
					ingredient.addProperty(rdfType, typeURI);
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
				// Obtenir la valeur calculée de la formule, pas la formule elle-même
				switch (cell.getCachedFormulaResultType()) {
					case STRING:
						return cell.getStringCellValue().trim();
					case NUMERIC:
						return new java.math.BigDecimal(cell.getNumericCellValue()).toPlainString();
					case BOOLEAN:
						return String.valueOf(cell.getBooleanCellValue());
					default:
						return "";
				}
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