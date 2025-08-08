package ontologyManagement;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;

// utiliser le modele ontologique
import org.apache.jena.ontology.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.vocabulary.DC;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.XSD;

import natclinn.util.NatclinnConf;


public class CreateNatclinnTbox {

	public static <ValuesFromRestriction> void main( String[] args ) {

		String jsonString = CreationTBox();
		
		OntModel om = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
		
		RDFParser.fromString(jsonString, Lang.JSONLD11).parse(om);

		try {   

			//////////////////////////////
			// Sorties fichiers         //
			////////////////////////////// 

			FileOutputStream outStream = new FileOutputStream(NatclinnConf.folderForOntologies + "/NatclinnTbox.xml");
			// exporte le resultat dans un fichier
			om.write(outStream, "RDF/XML");

			// N3 N-TRIPLE RDF/XML RDF/XML-ABBREV
			om.write(System.out, "N3");

			outStream.close();
		}
		catch (FileNotFoundException e) {System.out.println("File not found");}
		catch (IOException e) {System.out.println("IO problem");}
	}

	public static String CreationTBox() {

		String jsonString = null;

		// Initialisation de la configuration
		// Chemin d'accès, noms fichiers...	
		new NatclinnConf();  

		OntModel om = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
		
	    ///////////////////////////////
	    //Définition des namespaces  //
	    ///////////////////////////////
	    
	    String ncl = new String("https://w3id.org/NCL/ontology/");
	    om.setNsPrefix("ncl", ncl);
		String aif = new String("http://www.arg.dundee.ac.uk/aif#");
	    om.setNsPrefix("aif", aif);
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

		/////////////////////////////////////
	    //Description de l'ontologie       //
	    /////////////////////////////////////
	    
	    Ontology ont = om.createOntology(ncl + "NatclinnTbox");
		om.add(ont, RDFS.label,"Model Ontology of Natclinn");
		om.add(ont, DC.description,"Tbox for the Natclinn ontology");
		om.add(ont, DC.creator,"Pierre BISQUERT");	
		om.add(ont, DC.creator,"Raphaël CONDE SALAZAR");	
		om.add(ont, DC.creator,"Rallou THOMOPOULOS");	


	    //////////////////////////////////////////////////////////////////////
	    //														            //
	    //				TBOX = terminological box				            //				
	    //	ensemble de formules relatives aux informations terminologiques	//
	    //	(i.e. notions de bases et relations entre elles)                //
	    //       													        //
	    //////////////////////////////////////////////////////////////////////

//////////////////////////////////////////////////
//Axiomisation des propriétés et des relations	//
//terminologie DL: concepts et rôles	        // 
//////////////////////////////////////////////////

	    ////////////////////////////////////////////
	    // Définition des concepts atomiques      //
	    ////////////////////////////////////////////		

	    // Pour les éléments d'une structure décrivant un produit

	    // NCL est un ensemble de produits et d'arguments
	    OntClass NCL = om.createClass(ncl + "NCL");
	    NCL.addComment("NCL is the set of product and arguments.", "en");
		NCL.addComment("NCL est l'ensemble des produits et des arguments.", "fr");
	    
		OntClass Product = om.createClass(ncl + "Product");
		Product.addComment("A food industry product.", "en");
		Product.addComment("Un produit de l'industrie alimentaire.", "fr");

		OntClass CompositeProduct = om.createClass(ncl + "CompositeProduct");
		CompositeProduct.addComment("Product composed of at least one other product.", "en");
		CompositeProduct.addComment("Produit composé d'au moins un autre produit.", "fr"); 

		OntClass SimpleProduct = om.createClass(ncl + "SimpleProduct");
		SimpleProduct.addComment("Food industry product made up entirely of ingredients.", "en");
		SimpleProduct.addComment("Produit de l'industrie alimentaire composé uniquement d'ingrédients.", "fr"); 

		OntClass AdditiveProduct = om.createClass(ncl + "AdditiveProduct");
		SimpleProduct.addComment("Food industry product considered as an additive (stabilizer, raising agent, preservative, etc.). It may be composed of additive ingredients (sorbitol, E330, E520, etc.).", "en");
		SimpleProduct.addComment("Produit de l'industrie alimentaire considéré comme un additif (stabilisant, poudre à levée, conservateur, etc.). Il peut être composé d’ingrédient additif (sorbitol, E330, E520, etc.).", "fr");

		OntClass AromaProduct = om.createClass(ncl + "AromaProduct");
		SimpleProduct.addComment("Food industry product considered as a flavoring. It can be composed of ingredients.", "en");
		SimpleProduct.addComment("Produit de l'industrie alimentaire considéré comme un arôme. Il peut être composé d’ingrédient.", "fr");

		OntClass Ressource = om.createClass(ncl + "Ressource");
		Ressource.addComment("Abstract class Resource, from which Product and Ingredient inherit.", "en");
		Ressource.addComment("Classe abstraite Ressource, dont héritent Produit et Ingrédient.", "fr"); 

		// Pour les ingrédients 
		OntClass Ingredient = om.createClass(ncl + "Ingredient");
		Ingredient.addComment("An ingredient used in a product.", "en");
		Ingredient.addComment("Un ingrédient utilisé dans un produit.", "fr");

		OntClass IngredientByOrigin = om.createClass(ncl + "IngredientByOrigin");
		IngredientByOrigin.addSuperClass(Ingredient);
		IngredientByOrigin.addComment("Ingredient classified by its origin.", "en");
		IngredientByOrigin.addComment("Ingrédient classé selon son origine.", "fr");

		OntClass IngredientByFunction = om.createClass(ncl + "IngredientByFunction");
		IngredientByFunction.addSuperClass(Ingredient);
		IngredientByFunction.addComment("Ingredient classified by its function in the food product.", "en");
		IngredientByFunction.addComment("Ingrédient classé selon sa fonction dans le produit alimentaire.", "fr");

		OntClass IngredientByTransformationDegree = om.createClass(ncl + "IngredientByTransformationDegree");
		IngredientByTransformationDegree.addSuperClass(Ingredient);
		IngredientByTransformationDegree.addComment("Ingredient classified by its degree of processing.", "en");
		IngredientByTransformationDegree.addComment("Ingrédient classé selon son degré de transformation.", "fr");

		// Origine
		OntClass PlantOriginIngredient = om.createClass(ncl + "PlantOriginIngredient");
		PlantOriginIngredient.addSuperClass(IngredientByOrigin);
		PlantOriginIngredient.addComment("Ingredient of plant origin.", "en");
		PlantOriginIngredient.addComment("Ingrédient d'origine végétale.", "fr");

		OntClass AnimalOriginIngredient = om.createClass(ncl + "AnimalOriginIngredient");
		AnimalOriginIngredient.addSuperClass(IngredientByOrigin);
		AnimalOriginIngredient.addComment("Ingredient of animal origin.", "en");
		AnimalOriginIngredient.addComment("Ingrédient d'origine animale.", "fr");

		OntClass MineralOriginIngredient = om.createClass(ncl + "MineralOriginIngredient");
		MineralOriginIngredient.addSuperClass(IngredientByOrigin);
		MineralOriginIngredient.addComment("Ingredient of mineral origin.", "en");
		MineralOriginIngredient.addComment("Ingrédient d'origine minérale.", "fr");

		OntClass FungalOrMicrobialIngredient = om.createClass(ncl + "FungalOrMicrobialIngredient");
		FungalOrMicrobialIngredient.addSuperClass(IngredientByOrigin);
		FungalOrMicrobialIngredient.addComment("Ingredient of fungal or microbial origin.", "en");
		FungalOrMicrobialIngredient.addComment("Ingrédient d'origine fongique ou microbienne.", "fr");

		OntClass SyntheticOrBiotechIngredient = om.createClass(ncl + "SyntheticOrBiotechIngredient");
		SyntheticOrBiotechIngredient.addSuperClass(IngredientByOrigin);
		SyntheticOrBiotechIngredient.addComment("Ingredient of synthetic or biotechnological origin.", "en");
		SyntheticOrBiotechIngredient.addComment("Ingrédient d'origine synthétique ou biotechnologique.", "fr");

		// Fonction
		OntClass MainComponentIngredient = om.createClass(ncl + "MainComponentIngredient");
		MainComponentIngredient.addSuperClass(IngredientByFunction);
		MainComponentIngredient.addComment("Main ingredient providing structure or mass.", "en");
		MainComponentIngredient.addComment("Ingrédient principal apportant structure ou masse.", "fr");

		OntClass AdditiveIngredient = om.createClass(ncl + "AdditiveIngredient");
		AdditiveIngredient.addSuperClass(IngredientByFunction);
		AdditiveIngredient.addComment("Ingredient used as an additive (e.g., colorant, preservative).", "en");
		AdditiveIngredient.addComment("Ingrédient utilisé comme additif (colorant, conservateur…).", "fr");

		OntClass FlavorIngredient = om.createClass(ncl + "FlavorIngredient");
		FlavorIngredient.addSuperClass(IngredientByFunction);
		FlavorIngredient.addComment("Ingredient used for flavoring purposes.", "en");
		FlavorIngredient.addComment("Ingrédient utilisé pour aromatiser.", "fr");

		OntClass EnzymeIngredient = om.createClass(ncl + "EnzymeIngredient");
		EnzymeIngredient.addSuperClass(IngredientByFunction);
		EnzymeIngredient.addComment("Ingredient used as an enzyme in food processing.", "en");
		EnzymeIngredient.addComment("Ingrédient utilisé comme enzyme dans le procédé alimentaire.", "fr");

		OntClass FunctionalNutrientIngredient = om.createClass(ncl + "FunctionalNutrientIngredient");
		FunctionalNutrientIngredient.addSuperClass(IngredientByFunction);
		FunctionalNutrientIngredient.addComment("Ingredient providing nutritional or health benefits.", "en");
		FunctionalNutrientIngredient.addComment("Ingrédient apportant des bénéfices nutritionnels ou santé.", "fr");

		OntClass TechnologicalIngredient = om.createClass(ncl + "TechnologicalIngredient");
		TechnologicalIngredient.addSuperClass(IngredientByFunction);
		TechnologicalIngredient.addComment("Ingredient used for technological purposes (e.g., carrier, thickener).", "en");
		TechnologicalIngredient.addComment("Ingrédient utilisé pour des raisons technologiques (support, épaississant…).", "fr");

		// Degré de transformation
		OntClass RawIngredient = om.createClass(ncl + "RawIngredient");
		RawIngredient.addSuperClass(IngredientByTransformationDegree);
		RawIngredient.addComment("Unprocessed or minimally processed ingredient.", "en");
		RawIngredient.addComment("Ingrédient brut ou peu transformé.", "fr");

		OntClass ProcessedIngredient = om.createClass(ncl + "ProcessedIngredient");
		ProcessedIngredient.addSuperClass(IngredientByTransformationDegree);
		ProcessedIngredient.addComment("Ingredient that has been processed (e.g., flour, oil).", "en");
		ProcessedIngredient.addComment("Ingrédient ayant subi une transformation (farine, huile…).", "fr");

		OntClass UltraProcessedIngredient = om.createClass(ncl + "UltraProcessedIngredient");
		UltraProcessedIngredient.addSuperClass(IngredientByTransformationDegree);
		UltraProcessedIngredient.addComment("Ingredient that is highly processed or modified.", "en");
		UltraProcessedIngredient.addComment("Ingrédient hautement transformé ou modifié.", "fr");

		OntClass QuantifiedElement = om.createClass(ncl + "QuantifiedElement");
		QuantifiedElement.addComment("An element of a product composition that includes quantity, unit, percentage, and rank.", "en");
		QuantifiedElement.addComment("Un élément d'une composition de produit incluant quantité, unité, pourcentage et rang.", "fr");

		OntClass Packaging = om.createClass(ncl + "Packaging");
		Packaging.addComment("The packaging of a product.", "en");
		Packaging.addComment("L'emballage d'un produit.", "fr");

		OntClass Allegation = om.createClass(ncl + "Allegation");
		Packaging.addComment("Statement of facts about a product whose existence has yet to be proven.", "en");
		Packaging.addComment("Déclaration relative à des faits sur un produit dont l'existence reste à prouver.", "fr");

		OntClass ControlledOriginLabel = om.createClass(ncl + "ControlledOriginLabel");
		ControlledOriginLabel.addComment("The controlled origin label of a product.", "en");
		ControlledOriginLabel.addComment("Le label d'origine contrôlée d'un produit.", "fr");

		OntClass CleanLabel = om.createClass(ncl + "CleanLabel");
		CleanLabel.addComment("The clean label of a product.", "en");
		CleanLabel.addComment("Le 'clean label' d'un produit.", "fr");

		OntClass ManufacturingProcess = om.createClass(ncl + "ManufacturingProcess");
		ManufacturingProcess.addComment("The way the product is manufactured.", "en");
		ManufacturingProcess.addComment("La manière dont le produit est fabriqué.", "fr");

		OntClass FNI = om.createClass(ncl + "FNI");
		FNI.addComment("food naturalness index.", "en");
		FNI.addComment("Indice de naturalité d'un produit alimentaire.", "fr");
		 
		OntClass NutriScore = om.createClass(ncl + "NutriScore");
		NutriScore.addComment("Nutri-Score.", "en");
		NutriScore.addComment("Nutri-Score.", "fr");

		OntClass NaturalnessScore = om.createClass(ncl + "NaturalnessScore");
		NaturalnessScore.addComment("Naturalness-Score.", "en");
		NaturalnessScore.addComment("Score de naturalité.", "fr");

		OntClass Origin = om.createClass(ncl + "Origin");
		Origin.addComment("Ingredient origin (EU origin, Vietnam origin, etc.).", "en");
		Origin.addComment("Origine de l'ingrédient (origine UE, origine Vietnam, etc.).", "fr");
		// Pour les arguments 

		OntClass Argument = om.createClass(ncl + "Argument");
		Argument.addComment("An argument related to the naturalness of a product.", "en");
		Argument.addComment("Un argument lié à la naturalité d'un produit.", "fr");

		OntClass Source = om.createClass(ncl + "Source");
		Source.addComment("The source of an argument (consumer, scientific paper, etc.).", "en");
		Source.addComment("La source d'un argument (consommateur, article scientifique, etc.).", "fr");

		OntClass Context = om.createClass(ncl + "Context");
		Context.addComment("The context in which the argument is relevant.", "en");
		Context.addComment("Le contexte dans lequel l'argument est pertinent.", "fr");

		OntClass Attribute = om.createClass(ncl + "Attribute");
		Attribute.addComment("The attribute of naturalness in the context of the argument.", "en");
		Attribute.addComment("L'attribut de naturalité dans le contexte de l'argument.", "fr");

		OntClass ProductMatrix = om.createClass(ncl + "ProductMatrix");
		ProductMatrix.addComment("The product matrix in the context of the argument.", "en");
		ProductMatrix.addComment("La matrice produit dans le contexte de l'argument.", "fr");

		OntClass Verbatim = om.createClass(ncl + "Verbatim");
		Verbatim.addComment("Extract of a sentence supporting the argument.", "en");
		Verbatim.addComment("Extrait d'une phrase soutenant l'argument.", "fr");

		// Pour les arguments avec AIF

		// Abstract class Node
        OntClass Node = om.createClass(aif + "Node");
        Node.addComment("Abstract class for argumentation nodes.", "en");
		Node.addComment("Un nœud dans un graphe d'argumentation.", "fr");

		
	    ////////////////////////////////////////////
	    // Définition des disjonctions de classes //
	    ////////////////////////////////////////////

	    // add disjoint individuals axiom assertion:
	   
	    List<OntClass> classes = Arrays.asList(
			Argument,
			CleanLabel,
			Context,
			Attribute,
			ProductMatrix,
			ControlledOriginLabel,
			FNI,
			Ingredient,
			QuantifiedElement,
			ManufacturingProcess,
			NutriScore,
			NaturalnessScore,
			Allegation,
			Packaging,
			Origin,
			Product,
			Source,
			Verbatim
		);

		// Création de toutes les disjonctions possibles entre chaque paire de classes
		for (int i = 0; i < classes.size(); i++) {
			for (int j = i + 1; j < classes.size(); j++) {
				classes.get(i).addDisjointWith(classes.get(j));
			}
		}

	    //////////////////////////////////////////////////////////
	    // Définition des object property                       //
	    //////////////////////////////////////////////////////////
		
	    ObjectProperty identifier = om.createObjectProperty(ncl + "identifier");
		identifier.addDomain(Ressource);
		identifier.addRange(om.createResource(XSD.xstring.getURI()));
		
		ObjectProperty hasIngredient = om.createObjectProperty(ncl + "hasIngredient");
		hasIngredient.addDomain(Product);
		hasIngredient.addRange(Ingredient);

		ObjectProperty hasByProduct = om.createObjectProperty(ncl + "hasByProduct");
		hasByProduct.addDomain(Product);
		hasByProduct.addRange(Product);

		ObjectProperty hasPackaging = om.createObjectProperty(ncl + "hasPackaging");
		hasPackaging.addDomain(Product);
		hasPackaging.addRange(Packaging);

		ObjectProperty hasAllegation = om.createObjectProperty(ncl + "hasAllegation");
		hasAllegation.addDomain(Product);
		hasAllegation.addRange(Allegation);


		ObjectProperty hasCleanLabel = om.createObjectProperty(ncl + "hasCleanLabel");
		hasCleanLabel.addDomain(Product);
		hasCleanLabel.addRange(CleanLabel);

		ObjectProperty hasManufacturingProcess = om.createObjectProperty(ncl + "hasManufacturingProcess");
		hasManufacturingProcess.addDomain(Product);
		hasManufacturingProcess.addRange(ManufacturingProcess);

		ObjectProperty hasNutriScore = om.createObjectProperty(ncl + "hasNutriScore");
		hasNutriScore.addDomain(Product);
		hasNutriScore.addRange(NutriScore);

		ObjectProperty hasNaturalnessScore = om.createObjectProperty(ncl + "hasNaturalnessScore");
		hasNaturalnessScore.addDomain(Product);
		hasNaturalnessScore.addRange(NaturalnessScore);

		ObjectProperty hasFNI = om.createObjectProperty(ncl + "hasFNI");
		hasFNI.addDomain(Product);
		hasFNI.addRange(FNI);


		ObjectProperty hasQuantifiedElement = om.createObjectProperty(ncl + "hasQuantifiedElement");
		hasQuantifiedElement.addDomain(Product);
		hasQuantifiedElement.addRange(QuantifiedElement);

		ObjectProperty hasOrigin = om.createObjectProperty(ncl + "hasOrigin");
		hasOrigin.addDomain(Ingredient);
		hasOrigin.addRange(Origin);


		ObjectProperty hasControlledOrigin = om.createObjectProperty(ncl + "hasControlledOriginLabel");
		hasControlledOrigin.addDomain(Ingredient);
		hasControlledOrigin.addRange(ControlledOriginLabel);


		// anonymous class for unionOf
        RDFList unionListQuantifiedElement = om.createList(new RDFNode[] {Ingredient, Product});
        Resource unionClassQuantifiedElement = om.createResource()
            .addProperty(OWL.unionOf, unionListQuantifiedElement);
		ObjectProperty refersTo = om.createObjectProperty(ncl + "refersTo");
		refersTo.addDomain(QuantifiedElement);
		refersTo.addRange(unionClassQuantifiedElement);
		refersTo.addComment("Relates a product or ingredient to its quantification in a food industry product.", "en");
		refersTo.addComment("Relie un produit ou un ingrédient à sa quantification dans un produit de l'industrie agro-alimentaire.", "fr");

		ObjectProperty hasArgument = om.createObjectProperty(ncl + "hasArgument");
		hasArgument.addDomain(Product);
		hasArgument.addRange(Argument);
		hasArgument.addComment("Link a food industry product to an argument.", "en");
		hasArgument.addComment("Relie un produit de l'industrie agro-alimentaire à un argument.", "fr");

		RDFList unionListTarget = om.createList(new RDFNode[] { Ingredient, Product, Packaging, Allegation, CleanLabel, ManufacturingProcess, NutriScore, Origin, ControlledOriginLabel});
        Resource unionClassTarget = om.createResource()
		 .addProperty(OWL.unionOf, unionListTarget);
		ObjectProperty target = om.createObjectProperty(ncl + "target");
		target.addDomain(Argument);
		target.addRange(unionClassTarget);
		target.addComment("Link an argument to a component (product, ingredient, packaging, nutri-score, etc.) of a food industry product.", "en");
		target.addComment("Relie un argument à un composant (produit, ingrédient, packaging, nutri-score,etc.) d'un produit de l'industrie agro-alimentaire.", "fr");

		ObjectProperty hasContext = om.createObjectProperty(ncl + "hasContext");
		hasContext.addDomain(Argument);
		hasContext.addRange(Context);
		hasContext.addComment("Links an argument to its context", "en");
		hasContext.addComment("Relie un argument à son contexte", "fr");
		
		ObjectProperty hasVerbatim = om.createObjectProperty(ncl + "hasVerbatim");
		hasVerbatim.addDomain(Argument);
		hasVerbatim.addRange(Verbatim);
		hasVerbatim.addComment("Link an argument to its verbatim.", "en");
		hasVerbatim.addComment("Link an argument to its verbatim.", "fr");
		
		ObjectProperty hasSource = om.createObjectProperty(ncl + "hasSource");
		hasSource.addDomain(Argument);
		hasSource.addRange(Source);
		hasSource.addComment("Link an argument to its source (consumer survey, scientific journal, etc.).", "en");
		hasSource.addComment("Relie un argument à sa source (enquête consommateur, revue scientifique, etc.).", "fr");

		ObjectProperty hasAttribute = om.createObjectProperty(ncl + "hasAttribute");
		hasAttribute.addDomain(Context);
		hasAttribute.addRange(Attribute);
		hasAttribute.addComment("The naturalness attribute of a food industry product.", "en");
		hasAttribute.addComment("Attribut de la naturalité d'un produit de l'industrie agro-alimentaire.", "fr");

		ObjectProperty hasProductMatrix = om.createObjectProperty(ncl + "hasProductMatrix");
		hasProductMatrix.addDomain(Context);
		hasProductMatrix.addRange(ProductMatrix);
		hasProductMatrix.addComment("Matrix for a food industry product.", "en");
		hasProductMatrix.addComment("Matrice d'un produit de l'industrie agroalimentaire.", "fr");

	    
	    //////////////////////////////////////////////////////////
	    // Définition des data property                         //
	    //////////////////////////////////////////////////////////
	    DatatypeProperty quantity = om.createDatatypeProperty(ncl + "quantity");
		quantity.addDomain(QuantifiedElement);
		quantity.addComment("Quantity of a by-product or ingredient present in a food industry product.", "en");
		quantity.addComment("Quantité présente d'un sous-produit ou d'un ingrédient dans un produit de l'industrie agro-alimentaire.", "fr");
		
		
		DatatypeProperty unit = om.createDatatypeProperty(ncl + "unit");
		unit.addDomain(QuantifiedElement);
		unit.addComment("Unit of the quantity of a by-product or ingredient present in a food industry product.", "en");
		unit.addComment("Unité de la quantité présente d'un sous-produit ou d'un ingrédient dans un produit de l'industrie agro-alimentaire.", "fr");
		
		DatatypeProperty percentage = om.createDatatypeProperty(ncl + "percentage");
		percentage.addDomain(QuantifiedElement);
		percentage.addRange(om.createResource(XSD.xdouble.getURI()));
		percentage.addComment("Percentage (by weight) of a by-product or ingredient in a food industry product.", "en");
		percentage.addComment("Pourcentage (du poids) d'un sous-produit ou d'un ingrédient dans un produit de l'industrie agro-alimentaire.", "fr");
		
		
		DatatypeProperty rank = om.createDatatypeProperty(ncl + "rank");
		rank.addDomain(QuantifiedElement);
		rank.addRange(om.createResource(XSD.nonNegativeInteger.getURI()));
		rank.addComment("Classification of an ingredient or by-product in the textual statement of a product's composition.", "en");
		rank.addComment("Classement d'un ingrédient ou d'un sous-produit dans l’énoncé textuel de la composition d'un produit.", "fr");

		DatatypeProperty hasText = om.createDatatypeProperty(ncl + "hasText");
		hasText.addDomain(Verbatim);
		hasText.addRange(om.createResource(XSD.xstring.getURI()));
		hasText.addComment("Verbatim text.", "en");
		hasText.addComment("Texte du verbatim.", "fr");

		DatatypeProperty supportType = om.createDatatypeProperty(ncl + "supportType");
		supportType.addDomain(Argument);
		supportType.addRange(om.createResource(XSD.xstring.getURI()));
		supportType.addComment("Type of argument (positive or negative) in relation to its target.", "en");
		supportType.addComment("Type de l'argument (positif ou négatif) vis à vis de sa cible.", "fr");

		DatatypeProperty weightingIndex = om.createDatatypeProperty(ncl + "weightingIndex");
		weightingIndex.addDomain(Source);
		weightingIndex.addRange(om.createResource(XSD.nonNegativeInteger.getURI()));
		weightingIndex.addComment("Notoriety of the source of an argument.", "en");
		weightingIndex.addComment("Notoriété de la source d'un argument.", "fr");

		DatatypeProperty hasCiqualFoodCode = om.createDatatypeProperty(ncl + "hasCiqualFoodCode");
		hasCiqualFoodCode.addDomain(Ingredient);
		hasCiqualFoodCode.addRange(om.createResource(XSD.xstring.getURI()));
		hasCiqualFoodCode.addComment("Ciqual code for a food industry product.", "en");
		hasCiqualFoodCode.addComment("Code Ciqual d'un produit de l'industrie agroalimentaire.", "fr");
		
		DatatypeProperty hasCiqualProxyFoodCode = om.createDatatypeProperty(ncl + "hasCiqualProxyFoodCode");
		hasCiqualProxyFoodCode.addDomain(Ingredient);
		hasCiqualProxyFoodCode.addRange(om.createResource(XSD.xstring.getURI()));
		hasCiqualProxyFoodCode.addComment("Ciqual code for a similar food industry product.", "en");
		hasCiqualProxyFoodCode.addComment("Code Ciqual d'un produit similaire de l'industrie agroalimentaire.", "fr");
		



	
		//////////////////////////////////////////////////////////
	    // Définition des annotation property                   //
	    //////////////////////////////////////////////////////////
		om.createAnnotationProperty(skos + "prefLabel");
	    om.createAnnotationProperty(skos + "altLabel");
		om.createAnnotationProperty(skos + "definition");
		om.createAnnotationProperty(rdfs + "label");
		om.createAnnotationProperty(rdfs + "comment");
		om.createAnnotationProperty(dct + "created");


		
	//////////////////////////////////////////////////////////
	// Inclusion/Equivalence de concepts                    //
	//////////////////////////////////////////////////////////


	/////////////////////////////
	// Inclusion de concepts   //
	/////////////////////////////
	
		NCL.addSubClass(Ressource);
		NCL.addSubClass(Argument);
		NCL.addSubClass(CleanLabel);
		NCL.addSubClass(Context);
		NCL.addSubClass(ControlledOriginLabel);
		NCL.addSubClass(FNI);
		NCL.addSubClass(NutriScore);
		NCL.addSubClass(Packaging);	
		NCL.addSubClass(QuantifiedElement);	
		NCL.addSubClass(Source);
		NCL.addSubClass(Verbatim);
		NCL.addSubClass(ManufacturingProcess);
		NCL.addSubClass(NaturalnessScore);
		NCL.addSubClass(Allegation);
		NCL.addSubClass(Origin);
		NCL.addSubClass(ProductMatrix);
		NCL.addSubClass(Attribute);

	
		CompositeProduct.addSuperClass(Product);
		SimpleProduct.addSuperClass(Product);
		AdditiveProduct.addSuperClass(Product);
		AromaProduct.addSuperClass(Product);
		Product.addSuperClass(Ressource);
		Argument.addSuperClass(Node);
		Ingredient.addSuperClass(Ressource);
		// Sous-classes directes de Ingredient
		IngredientByOrigin.addSuperClass(Ingredient);
		IngredientByFunction.addSuperClass(Ingredient);
		IngredientByTransformationDegree.addSuperClass(Ingredient);

		// Origine (sous IngredientByOrigin)
		PlantOriginIngredient.addSuperClass(IngredientByOrigin);
		AnimalOriginIngredient.addSuperClass(IngredientByOrigin);
		MineralOriginIngredient.addSuperClass(IngredientByOrigin);
		FungalOrMicrobialIngredient.addSuperClass(IngredientByOrigin);
		SyntheticOrBiotechIngredient.addSuperClass(IngredientByOrigin);

		// Fonction (sous IngredientByFunction)
		MainComponentIngredient.addSuperClass(IngredientByFunction);
		AdditiveIngredient.addSuperClass(IngredientByFunction);
		FlavorIngredient.addSuperClass(IngredientByFunction);
		EnzymeIngredient.addSuperClass(IngredientByFunction);
		FunctionalNutrientIngredient.addSuperClass(IngredientByFunction);
		TechnologicalIngredient.addSuperClass(IngredientByFunction);

		// Degré de transformation (sous IngredientByTransformationDegree)
		RawIngredient.addSuperClass(IngredientByTransformationDegree);
		ProcessedIngredient.addSuperClass(IngredientByTransformationDegree);
		UltraProcessedIngredient.addSuperClass(IngredientByTransformationDegree);
	
		// exporte le resultat dans un fichier au format RDF/JSON
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		RDFDataMgr.write(out, om, RDFFormat.JSONLD11);
		try {
			jsonString = out.toString("UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		return jsonString;
	}
}