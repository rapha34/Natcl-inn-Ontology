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

// Ce programme lit un fichier Excel contenant des informations sur des arguments,
//  construit une représentation OWL (ABox) de ces données en instanciant des individus et en établissant
//  leurs relations, puis exporte le modèle obtenu dans différents formats RDF.

public class CreateNatclinnArgumentsAbox {
	
	public static void main(String[] args) {
		new NatclinnConf();  
		// Passage du chemin complet du fichier Excel à traiter
		String excelFile = NatclinnConf.folderForData + "/NatclinnArgumentAbox.xlsx";
		String jsonString = CreationArgumentsABox(excelFile);
		
		OntModel om = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
		
		// Lecture du modèle à partir de la chaîne JSON-LD
		// Protection si jsonString est null
		if (jsonString == null) {
			System.out.println("Le modèle JSON-LD est vide. Impossible de créer l'ABox.");
			return;
		} 
		RDFParser.create()
			.source(new StringReader(jsonString))
			.lang(Lang.JSONLD11)
			.parse(om);
		
		try {   
			FileOutputStream outStream = new FileOutputStream(NatclinnConf.folderForOntologies + "/NatclinnArgumentsAbox.xml");
			om.write(outStream, "RDF/XML");
			outStream.close();
		}
		catch (FileNotFoundException e) {System.out.println("File not found");}
		catch (IOException e) {System.out.println("IO problem");}
	}
	
	// Méthode principale de création de l'ABox à partir du chemin d'un fichier Excel
	public static String CreationArgumentsABox(String excelFile) {

		String jsonString = null;
		OntModel om = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);

		///////////////////////////////
		// Définition des namespaces //
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
		// Description de l'ontologie      //
		/////////////////////////////////////

		Ontology ont = om.createOntology(ncl + "NatclinnArgumentAbox");
		om.add(ont, RDFS.label, "Ontology of Natclinn Arguments");
		om.add(ont, DC.description, "Abox for the Natclinn Arguments ontology");
		om.add(ont, DC.creator, "Raphaël CONDE SALAZAR");	

		/////////////////////////////////////
		// Classes RDF                     //
		/////////////////////////////////////
		Resource Argument = om.createResource(ncl + "Argument");
		Resource Attribute = om.createResource(ncl + "Attribute");
		Resource Verbatim = om.createResource(ncl + "Verbatim");
		Resource Source = om.createResource(ncl + "Source");
		Resource Context = om.createResource(ncl + "Context");
		Resource ContextIngredient = om.createResource(ncl + "ContextIngredient");
		Resource ContextProduct = om.createResource(ncl + "ContextProduct");

		/////////////////////////////////////
		// Propriétés RDF                  //
		/////////////////////////////////////
		// Propriétés de données pour les arguments
		AnnotationProperty prefLabel = om.createAnnotationProperty(skos + "prefLabel");
		Property rdfType = om.createProperty(rdf, "type");
		Property hasAttribute = om.createProperty(ncl, "hasAttribute");
		Property hasVerbatim = om.createProperty(ncl, "hasVerbatim");
		Property hasVerbatimKeyWord = om.createProperty(ncl, "hasVerbatimKeyWord");
		Property hasSource = om.createProperty(ncl, "hasSource");
		Property hasStakeholderName = om.createProperty(ncl, "hasStakeholderName");
		
		// Propriétés de données pour les contextes
		Property hasContext = om.createProperty(ncl, "hasContext");
		Property hasContextProduct = om.createProperty(ncl, "hasContextProduct");
		Property hasContextIngredient = om.createProperty(ncl, "hasContextIngredient");
		Property hasCategoryOFF = om.createProperty(ncl, "hasCategoryOFF");
		Property hasSubCategoryOFF = om.createProperty(ncl, "hasSubCategoryOFF");
		Property hasLabelsAndCertifications = om.createProperty(ncl, "hasLabelsAndCertifications");
		
		// Propriétés de données pour Argument
		DatatypeProperty assertion = om.createDatatypeProperty(ncl + "assertion");
		DatatypeProperty polarity = om.createDatatypeProperty(ncl + "polarity");
		DatatypeProperty nameCriterion = om.createDatatypeProperty(ncl + "nameCriterion");
		DatatypeProperty aim = om.createDatatypeProperty(ncl + "aim");
		DatatypeProperty nameProperty = om.createDatatypeProperty(ncl + "nameProperty");
		DatatypeProperty valueProperty = om.createDatatypeProperty(ncl + "valueProperty");
		DatatypeProperty condition = om.createDatatypeProperty(ncl + "condition");
		DatatypeProperty infValue = om.createDatatypeProperty(ncl + "infValue");
		DatatypeProperty supValue = om.createDatatypeProperty(ncl + "supValue");
		DatatypeProperty unit = om.createDatatypeProperty(ncl + "unit");
		
		// Propriété pour Verbatim
		DatatypeProperty hasText = om.createDatatypeProperty(ncl + "hasText");
		
		// Propriété pour Source
		DatatypeProperty weightingIndex = om.createDatatypeProperty(ncl + "weightingIndex");
		AnnotationProperty comment = om.createAnnotationProperty(rdfs + "comment");

		//////////////////////////////
		// Définition des individus //
		////////////////////////////// 	

		try (FileInputStream fis = new FileInputStream(excelFile)) {
			// Autoriser les fichiers fortement compressés
			ZipSecureFile.setMinInflateRatio(0.001);
			try (Workbook workbook = new XSSFWorkbook(fis)) {
				Map<String, Resource> arguments = new HashMap<>();
				Map<String, Resource> attributes = new HashMap<>();
				Map<String, Resource> sources = new HashMap<>();
				Map<String, Resource> contexts = new HashMap<>();
				Map<String, Resource> contextIngredients = new HashMap<>();
				Map<String, Resource> contextProducts = new HashMap<>();
				
				// Traitement de l'onglet Sources
				Sheet sourcesSheet = workbook.getSheet("Sources");
				if (sourcesSheet != null) {
					for (Row row : sourcesSheet) {
						if (row.getRowNum() == 0) continue;
						String idSource = getCellValue(row.getCell(0));
						String nomSource = getCellValue(row.getCell(1));
						String importanceSource = getCellValue(row.getCell(2));
						String partiePrenante = getCellValue(row.getCell(3));
						
						if (idSource != null && !idSource.isEmpty()) {
							Resource source = om.createResource(ncl + idSource)
								.addProperty(rdfType, Source)
								.addProperty(prefLabel, nomSource != null ? nomSource : idSource);
							if (importanceSource != null && !importanceSource.isEmpty()) {
								try {
									source.addLiteral(weightingIndex, Double.parseDouble(importanceSource));
								} catch (NumberFormatException e) {
									source.addLiteral(weightingIndex, importanceSource);
								}
							}
							if (partiePrenante != null && !partiePrenante.isEmpty()) {
								source.addProperty(hasStakeholderName, partiePrenante);
							}
							sources.put(idSource, source);
						}
					}
				}
				
				// Traitement de l'onglet ContextProduct
				Sheet contextProductSheet = workbook.getSheet("ContextesProduits");
				if (contextProductSheet != null) {
					for (Row row : contextProductSheet) {
						if (row.getRowNum() == 0) continue;
						String idContextProduct = getCellValue(row.getCell(0));
						String commentaireContexteProduit = getCellValue(row.getCell(1));
						String motClefVerbatimContexteProduct = getCellValue(row.getCell(2));
						String categoryOFF = getCellValue(row.getCell(3));
						String subCategoryOFF = getCellValue(row.getCell(4));
						String labelsAndCertifications = getCellValue(row.getCell(5));

						if ((idContextProduct != null && !idContextProduct.isEmpty()) && ((commentaireContexteProduit != null && !commentaireContexteProduit.isEmpty()) || (motClefVerbatimContexteProduct != null && !motClefVerbatimContexteProduct.isEmpty()) || (categoryOFF != null && !categoryOFF.isEmpty()) || (subCategoryOFF != null && !subCategoryOFF.isEmpty()) || (labelsAndCertifications != null && !labelsAndCertifications.isEmpty()))) {
							Resource contextProduct = om.createResource(ncl + idContextProduct)
								.addProperty(rdfType, ContextProduct)
								.addProperty(prefLabel, idContextProduct);
							if (commentaireContexteProduit != null && !commentaireContexteProduit.isEmpty()) {
								contextProduct.addProperty(comment, commentaireContexteProduit);
							}
							if (motClefVerbatimContexteProduct != null && !motClefVerbatimContexteProduct.isEmpty()) {
								contextProduct.addProperty(hasVerbatimKeyWord, motClefVerbatimContexteProduct);
							}
							if (categoryOFF != null && !categoryOFF.isEmpty()) {
								contextProduct.addProperty(hasCategoryOFF, categoryOFF);
							}
							if (subCategoryOFF != null && !subCategoryOFF.isEmpty()) {
								contextProduct.addProperty(hasSubCategoryOFF, subCategoryOFF);
							}
							if (labelsAndCertifications != null && !labelsAndCertifications.isEmpty()) {
								contextProduct.addProperty(hasLabelsAndCertifications, labelsAndCertifications);
							}


							contextProducts.put(idContextProduct, contextProduct);
						}
					}
				}
				
				// Traitement de l'onglet ContextIngredient
				Sheet contextIngredientSheet = workbook.getSheet("ContextesIngredients");
				if (contextIngredientSheet != null) {
					for (Row row : contextIngredientSheet) {
						if (row.getRowNum() == 0) continue;
						String idContextIngredient = getCellValue(row.getCell(0));
						String commentaireContexteIngredient = getCellValue(row.getCell(1));
						String motClefVerbatimContexteIngredient = getCellValue(row.getCell(2));

						if ((idContextIngredient != null && !idContextIngredient.isEmpty()) && ((commentaireContexteIngredient != null && !commentaireContexteIngredient.isEmpty()) || (motClefVerbatimContexteIngredient != null && !motClefVerbatimContexteIngredient.isEmpty()))) {
							Resource contextIngredient = om.createResource(ncl + idContextIngredient)
								.addProperty(rdfType, ContextIngredient)
								.addProperty(prefLabel, idContextIngredient);
							if (commentaireContexteIngredient != null && !commentaireContexteIngredient.isEmpty()) {
								contextIngredient.addProperty(comment, commentaireContexteIngredient);
							}
							if (motClefVerbatimContexteIngredient != null && !motClefVerbatimContexteIngredient.isEmpty()) {
								contextIngredient.addProperty(hasVerbatimKeyWord, motClefVerbatimContexteIngredient);
							}
							contextIngredients.put(idContextIngredient, contextIngredient);
						}
					}
				}
				
				// Traitement de l'onglet Contexts
				Sheet contextsSheet = workbook.getSheet("Contextes");
				if (contextsSheet != null) {
					for (Row row : contextsSheet) {
						if (row.getRowNum() == 0) continue;
						String idContext = getCellValue(row.getCell(1));
						String commentaireContext = getCellValue(row.getCell(2));
						String idContextProduct = getCellValue(row.getCell(3));
						String idContextIngredient = getCellValue(row.getCell(4));
						
						if (idContext != null && !idContext.isEmpty()) {
							Resource context = om.createResource(ncl + idContext)
								.addProperty(rdfType, Context);
							
							if (commentaireContext != null && !commentaireContext.isEmpty()) {
								context.addProperty(comment, commentaireContext);
							}
							
							// Lien vers contexte produit
							if (idContextProduct != null && !idContextProduct.isEmpty() 
								&& contextProducts.containsKey(idContextProduct)) {
								context.addProperty(hasContextProduct, contextProducts.get(idContextProduct));
							}
							
							// Lien vers contexte ingredient
							if (idContextIngredient != null && !idContextIngredient.isEmpty() 
								&& contextIngredients.containsKey(idContextIngredient)) {
								context.addProperty(hasContextIngredient, contextIngredients.get(idContextIngredient));
							}
							
							contexts.put(idContext, context);
						}
					}
				}
				
				// Traitement de l'onglet Arguments
				Sheet argumentsSheet = workbook.getSheet("Arguments");
				if (argumentsSheet != null) {
					for (Row row : argumentsSheet) {
						if (row.getRowNum() == 0) continue;
						
						String idArgument = getCellValue(row.getCell(0));
						String attribut = getCellValue(row.getCell(1));
						String description = getCellValue(row.getCell(2));
						String verbatimText = getCellValue(row.getCell(3));
						String assertionText = getCellValue(row.getCell(4));
						String polarite = getCellValue(row.getCell(5));
						String nomCritere = getCellValue(row.getCell(6));
						String but = getCellValue(row.getCell(7));
						String propriete = getCellValue(row.getCell(8));
						String valeurPropriete = getCellValue(row.getCell(9));
						String conditionPropriete = getCellValue(row.getCell(10));
						String valeurInferieure = getCellValue(row.getCell(11));
						String valeurSuperieure = getCellValue(row.getCell(12));
						String unitePropriete = getCellValue(row.getCell(13));
						String idSource = getCellValue(row.getCell(14));
						
						if (idArgument != null && !idArgument.isEmpty()) {
							// Créer l'argument
							Resource argument = om.createResource(ncl + idArgument)
								.addProperty(rdfType, Argument);
							
							if (description != null && !description.isEmpty()) {
								argument.addProperty(prefLabel, description);
							}
							
							// Attribut
							if (attribut != null && !attribut.isEmpty()) {
								if (!attributes.containsKey(attribut)) {
									String uriAtt = NatclinnUtil.makeURI(ncl + "Attribute_", attribut.replaceAll("\\s+", "_"));
									Resource att = om.createResource(uriAtt)
										.addProperty(rdfType, Attribute)
										.addProperty(prefLabel, attribut);
									attributes.put(attribut, att);
								}
								argument.addProperty(hasAttribute, attributes.get(attribut));
							}
							
	
							
							// Verbatim
							if (verbatimText != null && !verbatimText.isEmpty()) {
								String verbatimId = "Verbatim_" + idArgument;
								Resource verbatim = om.createResource(ncl + verbatimId)
									.addProperty(rdfType, Verbatim)
									.addProperty(hasText, verbatimText);
								argument.addProperty(hasVerbatim, verbatim);
							}
							
							// Propriétés de données
							if (assertionText != null && !assertionText.isEmpty()) {
								argument.addLiteral(assertion, assertionText);
							}
							if (polarite != null && !polarite.isEmpty()) {
								argument.addLiteral(polarity, polarite);
							}
							if (nomCritere != null && !nomCritere.isEmpty()) {
								argument.addLiteral(nameCriterion, nomCritere);
							}
							if (but != null && !but.isEmpty()) {
								argument.addLiteral(aim, but);
							}
							if (propriete != null && !propriete.isEmpty()) {
								argument.addLiteral(nameProperty, propriete);
							}
							if (valeurPropriete != null && !valeurPropriete.isEmpty()) {
								argument.addLiteral(valueProperty, valeurPropriete);
							}
							if (conditionPropriete != null && !conditionPropriete.isEmpty()) {
								argument.addLiteral(condition, conditionPropriete);
							}
							if (valeurInferieure != null && !valeurInferieure.isEmpty()) {
								try {
									argument.addLiteral(infValue, Double.parseDouble(valeurInferieure));
								} catch (NumberFormatException e) {
									argument.addLiteral(infValue, valeurInferieure);
								}
							}
							if (valeurSuperieure != null && !valeurSuperieure.isEmpty()) {
								try {
									argument.addLiteral(supValue, Double.parseDouble(valeurSuperieure));
								} catch (NumberFormatException e) {
									argument.addLiteral(supValue, valeurSuperieure);
								}
							}
							if (unitePropriete != null && !unitePropriete.isEmpty()) {
								argument.addLiteral(unit, unitePropriete);
							}
							
							// Source
							if (idSource != null && !idSource.isEmpty() && sources.containsKey(idSource)) {
								argument.addProperty(hasSource, sources.get(idSource));
							}
							
							arguments.put(idArgument, argument);
						}
					}
				}
				
				// Lier les arguments aux contextes
				if (contextsSheet != null) {
					for (Row row : contextsSheet) {
						if (row.getRowNum() == 0) continue;
						String idArgument = getCellValue(row.getCell(0));
						String idContext = getCellValue(row.getCell(1));
						
						if (idArgument != null && !idArgument.isEmpty() 
							&& idContext != null && !idContext.isEmpty()
							&& arguments.containsKey(idArgument) 
							&& contexts.containsKey(idContext)) {
							arguments.get(idArgument).addProperty(hasContext, contexts.get(idContext));
						}
					}
				}
			}

			// Exporte le resultat dans un fichier au format JSON-LD
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			RDFDataMgr.write(out, om, RDFFormat.JSONLD11);
			try {
				jsonString = out.toString("UTF-8");
				// System.out.println(jsonString);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			
		} catch (FileNotFoundException e) {
			System.out.println("File not found: " + excelFile);
		} catch (IOException e) {
			System.out.println("IO problem: " + e.getMessage());
		}
		 
		return jsonString;

	}
	
	// Méthode utilitaire pour extraire la valeur d'une cellule
	private static String getCellValue(Cell cell) {
		if (cell == null) {
			return null;
		}
		switch (cell.getCellType()) {
			case STRING:
				return cell.getStringCellValue().trim();
			case NUMERIC:
				if (DateUtil.isCellDateFormatted(cell)) {
					return cell.getDateCellValue().toString();
				} else {
					double numValue = cell.getNumericCellValue();
					if (numValue == (long) numValue) {
						return String.valueOf((long) numValue);
					} else {
						return String.valueOf(numValue);
					}
				}
			case BOOLEAN:
				return String.valueOf(cell.getBooleanCellValue());
			case FORMULA:
				return cell.getCellFormula();
			case BLANK:
				return null;
			default:
				return null;
		}
	}
}