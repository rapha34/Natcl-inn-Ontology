package ontologyManagement;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;

import org.apache.jena.datatypes.xsd.XSDDatatype;
// utiliser le modele ontologique
import org.apache.jena.ontology.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.vocabulary.DC;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
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
		String dc = new String("http://purl.org/dc/elements/1.1/"); 
	    om.setNsPrefix("dc", dc);
		String dct = new String("http://purl.org/dc/terms/"); 
	    om.setNsPrefix("dct", dct);
		String skos = new String("http://www.w3.org/2004/02/skos/core#");
	    om.setNsPrefix("skos", skos); 
	    String foaf = new String("http://xmlns.com/foaf/0.1/");
	    om.setNsPrefix("foaf", foaf);
	    String rdfs = new String("http://www.w3.org/2000/01/rdf-schema#");
	    om.setNsPrefix("rdfs", rdfs);
	    String bibo = new String("http://purl.org/ontology/bibo/");
	    om.setNsPrefix("bibo", bibo);
	    String vann = new String("http://purl.org/vocab/vann/");
	    om.setNsPrefix("vann", vann);
	    String schema = new String("http://schema.org/");
	    om.setNsPrefix("schema", schema);
	    String org = new String("http://www.w3.org/ns/org#");
	    om.setNsPrefix("org", org);
	    String vocab = new String("https://w3id.org/afy/vocab#");
	    om.setNsPrefix("vocab", vocab);

		/////////////////////////////////////
	    //Description de l'ontologie       //
	    /////////////////////////////////////
	    
	    Ontology ont = om.createOntology(ncl + "NatclinnTbox");
		ont.addProperty(RDFS.label, "Natcl'inn ontology", "en");
		ont.addProperty(RDFS.label, "Ontologie Natcl'inn", "fr");
		om.add(ont, DC.description,"Tbox for the Natcl'inn ontology", "en");
		om.add(ont, DC.description,"Tbox pour l'ontologie Natcl'inn", "fr");
		om.add(ont, DC.creator,"Pierre BISQUERT");	
		om.add(ont, DC.creator,"Raphaël CONDE SALAZAR");	
		om.add(ont, DC.creator,"Rallou THOMOPOULOS");	
        ont.addProperty(OWL.versionIRI, om.createResource(ncl + "/1.0.0"));
        //ont.addProperty(OWL.imports, om.createResource("http://www.w3.org/2000/01/rdf-schema"));

        // Titre
        ont.addProperty(om.createProperty(dc + "title"), "NCL", "en");

        // Abstracts
        ont.addProperty(om.createProperty(dct + "abstract"),
                "Le projet NATCL’INN vise à aider les entreprises agroalimentaires à mieux répondre aux attentes de naturalité des consommateurs, perçue comme gage de qualité, de santé et de goût. En s’appuyant sur la caractérisation des représentations de la naturalité et l’identification de marqueurs pertinents, il développe un outil d’arbitrage multicritère intégrant contraintes techniques, économiques et réglementaires. Porté par l’ADRIA, l’UBO-LEGO, l’INRAE et plusieurs partenaires industriels, ce projet collaboratif est labellisé par Valorial.", "fr");
        ont.addProperty(om.createProperty(dct + "abstract"),
                "The NATCL’INN project aims to help agri-food companies better meet consumers' expectations for naturalness, which is perceived as a guarantee of quality, health and taste. Based on the characterisation of representations of naturalness and the identification of relevant markers, it is developing a multi-criteria arbitration tool that integrates technical, economic and regulatory constraints. Led by ADRIA, UBO-LEGO, INRAE and several industrial partners, this collaborative project has been certified by Valorial.", "en");

        // Dates de création
        ont.addProperty(om.createProperty(dct + "created"), "Le 5 Octobre 2021", "fr");
        ont.addProperty(om.createProperty(dct + "created"), "October 5th, 2021", "en");

        // Licence et DOI
        ont.addProperty(om.createProperty(dct + "license"), om.createResource("http://creativecommons.org/licenses/by/4.0/"));
        ont.addProperty(om.createProperty(bibo + "doi"), om.createResource("https://doi.org/XXXXXXXXXXXXXXXX"));
        ont.addProperty(om.createProperty(bibo + "status"), om.createResource("http://purl.org/ontology/bibo/status/draft"));

        // Informations complémentaires
        ont.addProperty(om.createProperty(vann + "preferredNamespacePrefix"), "ncl", "en");
        ont.addProperty(om.createProperty(vann + "preferredNamespaceUri"), ncl);

        ont.addProperty(RDFS.comment, "Une ontologie conçue pour définir les différents produits de l'industrie agroalimentaire et les arguments qui leur sont associés en termes de naturalité perçue.", "fr");
        ont.addProperty(RDFS.comment, "An ontology designed to define the various products of the agri-food industry and the arguments associated with them in terms of their perceived naturalness.", "en");
        
        ont.addProperty(OWL.versionInfo, "1.0.0");

        ont.addProperty(om.createProperty(foaf + "fundedBy"), om.createResource("https://www.pole-valorial.fr"));

        ont.addProperty(om.createProperty(schema + "citation"),
                "Cite this vocabulary as: Raphaël Conde Salazar , Pierre Bisquert, Rallou Thomopoulos; DOI: XXXXXXXXXXXXXXXX", "en");

        // Introduction
        ont.addProperty(om.createProperty(vocab + "introduction"),
                "NATCL’INN vise à proposer une solution aux entreprises de l’agroalimentaire qui doivent réaliser des arbitrages entre différents attributs produits relatifs à la naturalité en vue de répondre aux nouvelles attentes et représentations des consommateurs en matière de naturalité alimentaire. Afin de proposer un prototype d’Outil d’Aide à la Décision, les équipes de R&D de l’ADRIA, le laboratoire LEGO de l’Université de Bretagne Occidentale (UBO), les unités de recherche INRAE et leurs partenaires industriels (Bridor, Paticeo, Charles Christ, La PAM, Ecomiam, Fleury Michon, Guyader Gastronomie) se sont associés pour mener un programme ambitieux de R&D collaborative. Le projet NATCL’INN a été labellisé par le pôle de compétitivité VALORIAL et a reçu le soutien de la Région Bretagne, de la Région Pays de la Loire et de Quimper Bretagne Occidentale. Le projet a démarré au 1er janvier 2024 et s'achèvera fin 2027.", "fr");
        ont.addProperty(om.createProperty(vocab + "introduction"),
                "NATCL’INN aims to offer a solution to agri-food companies that need to balance different product attributes related to naturalness in order to meet new consumer expectations and perceptions regarding food naturalness. In order to propose a prototype Decision Support Tool, the R&D teams at ADRIA, the LEGO laboratory at the University of Western Brittany (UBO), INRAE research units and their industrial partners (Bridor, Paticeo, Charles Christ, La PAM, Ecomiam, Fleury Michon, Guyader Gastronomie) have joined forces to carry out an ambitious collaborative R&D programme. The NATCL'INN project has been certified by the VALORIAL competitiveness cluster and has received support from the Brittany Region, the Pays de la Loire Region and Quimper Bretagne Occidentale. The project began on 1 January 2024 and will be completed at the end of 2027", "en");

        ont.addProperty(om.createProperty(vocab + "rdfxmlSerialization"),
                om.createTypedLiteral("https://w3id.org/NCL/ontology.xml",
                        XSDDatatype.XSDanyURI));

        // =====================
        // Définition des auteurs (noeuds anonymes)
        // =====================

        // Auteur 1
        Resource person1 = om.createResource();
        person1.addProperty(RDF.type, om.createResource(schema + "Person"));
        Resource org1 = om.createResource();
        org1.addProperty(RDF.type, om.createResource(foaf + "Organization"));
        org1.addProperty(om.createProperty(foaf + "name"), "University of Montpellier, INRAE, France");
        org1.addProperty(om.createProperty(schema + "url"), "https://www.umontpellier.fr/");
        person1.addProperty(om.createProperty(org + "memberOf"), org1);
        person1.addProperty(om.createProperty(schema + "familyName"), "Conde Salazar", "fr");
        person1.addProperty(om.createProperty(schema + "familyName"), "Conde Salazar", "en");
        person1.addProperty(om.createProperty(schema + "name"), "Raphaël Conde Salazar", "fr");
        person1.addProperty(om.createProperty(schema + "name"), "Raphael Conde Salazar", "en");
        person1.addProperty(om.createProperty(schema + "url"), om.createResource("https://orcid.org/0000-0002-6926-5299"));
        ont.addProperty(om.createProperty(schema + "creator"), person1);

        // Auteur 2
        Resource person2 = om.createResource();
        person2.addProperty(RDF.type, om.createResource(schema + "Person"));
        Resource org2 = om.createResource();
        org2.addProperty(RDF.type, om.createResource(foaf + "Organization"));
        org2.addProperty(om.createProperty(foaf + "name"), "University of Montpellier, INRAE, France");
        org2.addProperty(om.createProperty(schema + "url"), "https://www.umontpellier.fr/");
        person2.addProperty(om.createProperty(org + "memberOf"), org2);
        person2.addProperty(om.createProperty(schema + "name"), "Pierre Bisquert", "fr");
        person2.addProperty(om.createProperty(schema + "name"), "Pierre Bisquert", "en");
        person2.addProperty(om.createProperty(schema + "url"), om.createResource("https://orcid.org/0000-0001-9418-5330" + //
						""));
        ont.addProperty(om.createProperty(schema + "creator"), person2);

        // Auteur 3
        Resource person3 = om.createResource();
        person3.addProperty(RDF.type, om.createResource(schema + "Person"));
        Resource org3 = om.createResource();
        org3.addProperty(RDF.type, om.createResource(foaf + "Organization"));
        org3.addProperty(om.createProperty(foaf + "name"), "University of Montpellier, INRAE, France");
        org3.addProperty(om.createProperty(schema + "url"), "https://www.umontpellier.fr/");
        person3.addProperty(om.createProperty(org + "memberOf"), org3);
        person3.addProperty(om.createProperty(schema + "name"), "Rallou Thomopoulos", "fr");
        person3.addProperty(om.createProperty(schema + "name"), "Rallou Thomopoulos", "en");
        person3.addProperty(om.createProperty(schema + "url"), om.createResource("https://orcid.org/0000-0002-3218-9472"));
        ont.addProperty(om.createProperty(schema + "creator"), person3);

        // Financement
        ont.addProperty(om.createProperty(schema + "funding"),
                om.createResource("https://www.pole-valorial.fr"));



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

		// OntClass AdditiveProduct = om.createClass(ncl + "AdditiveProduct");
		// SimpleProduct.addComment("Food industry product considered as an additive (stabilizer, raising agent, preservative, etc.). It may be composed of additive ingredients (sorbitol, E330, E520, etc.).", "en");
		// SimpleProduct.addComment("Produit de l'industrie alimentaire considéré comme un additif (stabilisant, poudre à levée, conservateur, etc.). Il peut être composé d’ingrédient additif (sorbitol, E330, E520, etc.).", "fr");

		// OntClass AromaProduct = om.createClass(ncl + "AromaProduct");
		// SimpleProduct.addComment("Food industry product considered as a flavoring. It can be composed of ingredients.", "en");
		// SimpleProduct.addComment("Produit de l'industrie alimentaire considéré comme un arôme. Il peut être composé d’ingrédient.", "fr");

		OntClass Resource = om.createClass(ncl + "Resource");
		Resource.addComment("Abstract class Resource, from which Product and Ingredient inherit.", "en");
		Resource.addComment("Classe abstraite Resource, dont héritent Produit et Ingrédient.", "fr"); 

		// Pour les ingrédients 
		OntClass Ingredient = om.createClass(ncl + "Ingredient");
		Ingredient.addComment("An ingredient used in a product.", "en");
		Ingredient.addComment("Un ingrédient utilisé dans un produit.", "fr");

		OntClass CompositeIngredient = om.createClass(ncl + "CompositeIngredient");
		CompositeIngredient.addComment("Ingredient composed of at least one other Ingredient.", "en");
		CompositeIngredient.addComment("Un ingrédient composé d'au moins un autre ingrédient.", "fr"); 

		OntClass SimpleIngredient = om.createClass(ncl + "SimpleIngredient");
		SimpleIngredient.addComment("A simple ingredient.", "en");
		SimpleIngredient.addComment("Un ingrédient simple.", "fr"); 

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

		OntClass Shape = om.createClass(ncl + "Shape");
		Shape.addComment("The forme used for the product packaging.", "en");
		Shape.addComment("La forme de l'emballage du produit.", "fr");

		OntClass Material = om.createClass(ncl + "Material");
		Material.addComment("The material used for the product packaging.", "en");
		Material.addComment("La matière de l'emballage du produit.", "fr");

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

		OntClass NutriScoreAlpha = om.createClass(ncl + "NutriScoreAlpha");
		NutriScoreAlpha.addComment("Nutri-Score alpha	.", "en");
		NutriScoreAlpha.addComment("Nutri-Score alpha.", "fr");

		OntClass NutriScoreDetail = om.createClass(ncl + "NutriScoreDetail");
		NutriScoreDetail.addComment("Nutri-Score detail	.", "en");
		NutriScoreDetail.addComment("Nutri-Score detail.", "fr");

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

		OntClass ContextProduct = om.createClass(ncl + "ContextProduct");
		ContextProduct.addSuperClass(Context);
		ContextProduct.addComment("The product context in which an argument is relevant.", "en");
		ContextProduct.addComment("Le contexte produit dans lequel un argument est pertinent.", "fr");

		OntClass ContextIngredient = om.createClass(ncl + "ContextIngredient");
		ContextIngredient.addSuperClass(Context);
		ContextIngredient.addComment("The ingredient context in which an argument is relevant.", "en");
		ContextIngredient.addComment("Le contexte ingrédient dans lequel un argument est pertinent.", "fr");

		OntClass Attribute = om.createClass(ncl + "Attribute");
		Attribute.addComment("The attribute of naturalness in the context of the argument.", "en");
		Attribute.addComment("L'attribut de naturalité dans le contexte de l'argument.", "fr");

		OntClass Category = om.createClass(ncl + "Category");
		Category.addComment("Main category of the argument (e.g., 'Mode de culture et d'élevage').", "en");
		Category.addComment("Catégorie principale de l'argument (ex: 'Mode de culture et d'élevage').", "fr");

		OntClass Subcategory = om.createClass(ncl + "Subcategory");
		Subcategory.addComment("Subcategory of the argument.", "en");
		Subcategory.addComment("Sous-catégorie de l'argument.", "fr");

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
			Category,
    		Subcategory,
			ControlledOriginLabel,
			FNI,
			Ingredient,
			QuantifiedElement,
			ManufacturingProcess,
			NutriScore,
			NutriScoreDetail,
			NutriScoreAlpha,
			NaturalnessScore,
			Allegation,
			Packaging,
			Shape,
			Material,
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
		
		// anonymous class for unionOf
        RDFList unionListComposedOfDomain = om.createList(new RDFNode[] {CompositeIngredient, CompositeProduct});
        Resource unionClassComposedOfDomain = om.createResource()
            .addProperty(OWL.unionOf, unionListComposedOfDomain);
		RDFList unionListComposedOfRange = om.createList(new RDFNode[] {Ingredient, Product});
        Resource unionClassComposedOfRange = om.createResource()
            .addProperty(OWL.unionOf, unionListComposedOfRange);	
	    ObjectProperty composedOf = om.createObjectProperty(ncl + "composedOf");
		composedOf.addDomain(unionClassComposedOfDomain);
		composedOf.addRange(unionClassComposedOfRange);

		ObjectProperty identifier = om.createObjectProperty(ncl + "identifier");
		identifier.addDomain(Resource);
		identifier.addRange(om.createResource(XSD.xstring.getURI()));
		
		ObjectProperty hasIngredient = om.createObjectProperty(ncl + "hasIngredient");
		hasIngredient.addDomain(Product);
		hasIngredient.addRange(Ingredient);

		ObjectProperty hasPackaging = om.createObjectProperty(ncl + "hasPackaging");
		hasPackaging.addDomain(Product);
		hasPackaging.addRange(Packaging);

		ObjectProperty hasMaterial = om.createObjectProperty(ncl + "hasMaterial");
		hasMaterial.addDomain(Packaging);
		hasMaterial.addRange(Material);

		ObjectProperty hasShape = om.createObjectProperty(ncl + "hasShape");
		hasShape.addDomain(Packaging);
		hasShape.addRange(Shape);

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
		hasQuantifiedElement.addDomain(Resource);
		hasQuantifiedElement.addRange(QuantifiedElement);

		ObjectProperty hasOrigin = om.createObjectProperty(ncl + "hasOrigin");
		hasOrigin.addDomain(Ingredient);
		hasOrigin.addRange(Origin);


		ObjectProperty hasControlledOrigin = om.createObjectProperty(ncl + "hasControlledOriginLabel");
		hasControlledOrigin.addDomain(Product);
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

		ObjectProperty hasContextProduct = om.createObjectProperty(ncl + "hasContextProduct");
		hasContextProduct.addDomain(Context);
		hasContextProduct.addRange(ContextProduct);
		hasContextProduct.addComment("The product context of an argument.", "en");
		hasContextProduct.addComment("Le contexte produit d'un argument.", "fr");

		ObjectProperty hasContextIngredient = om.createObjectProperty(ncl + "hasContextIngredient");
		hasContextIngredient.addDomain(Context);
		hasContextIngredient.addRange(ContextIngredient);
		hasContextIngredient.addComment("The ingredient context of an argument.", "en");
		hasContextIngredient.addComment("Le contexte ingrédient d'un argument.", "fr");
		
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
		hasAttribute.addDomain(Argument);
		hasAttribute.addRange(Attribute);
		hasAttribute.addComment("The naturalness attribute of a food industry product.", "en");
		hasAttribute.addComment("Attribut de la naturalité d'un produit de l'industrie agro-alimentaire.", "fr");

		ObjectProperty hasCategory = om.createObjectProperty(ncl + "hasCategory");
		hasCategory.addDomain(Argument);
		hasCategory.addRange(Category);
		hasCategory.addComment("Links an argument to its main category.", "en");
		hasCategory.addComment("Relie un argument à sa catégorie principale.", "fr");

		ObjectProperty hasSubcategory = om.createObjectProperty(ncl + "hasSubcategory");
		hasSubcategory.addDomain(Argument);
		hasSubcategory.addRange(Subcategory);
		hasSubcategory.addComment("Links an argument to its subcategory.", "en");
		hasSubcategory.addComment("Relie un argument à sa sous-catégorie.", "fr");

		ObjectProperty containsIngredientWithFunction = om.createObjectProperty(ncl + "containsIngredientWithFunction");
		containsIngredientWithFunction.addDomain(Product);
		containsIngredientWithFunction.addRange(om.createResource(XSD.xstring.getURI()));
		containsIngredientWithFunction.addComment("Links a product to the function of its ingredients.", "en");
		containsIngredientWithFunction.addComment("Relie un produit à la fonction de ses ingrédients.", "fr");


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
		weightingIndex.addRange(om.createResource(XSD.xdouble.getURI()));
		weightingIndex.addComment("Notoriety of the source of an argument.", "en");
		weightingIndex.addComment("Notoriété de la source d'un argument.", "fr");

		DatatypeProperty assertion = om.createDatatypeProperty(ncl + "assertion");
		assertion.addDomain(Argument);
		assertion.addRange(om.createResource(XSD.xstring.getURI()));
		assertion.addComment("The assertion or claim made by the argument.", "en");
		assertion.addComment("L'assertion ou la déclaration faite par l'argument.", "fr");

		DatatypeProperty polarity = om.createDatatypeProperty(ncl + "polarity");
		polarity.addDomain(Argument);
		polarity.addRange(om.createResource(XSD.xstring.getURI()));
		polarity.addComment("Polarity of the argument (positive '+' or negative '-').", "en");
		polarity.addComment("Polarité de l'argument (positif '+' ou négatif '-').", "fr");

		DatatypeProperty nameCriterion = om.createDatatypeProperty(ncl + "nameCriterion");
		nameCriterion.addDomain(Argument);
		nameCriterion.addRange(om.createResource(XSD.xstring.getURI()));
		nameCriterion.addComment("Name of the criterion addressed by the argument.", "en");
		nameCriterion.addComment("Nom du critère abordé par l'argument.", "fr");

		DatatypeProperty aim = om.createDatatypeProperty(ncl + "aim");
		aim.addDomain(Argument);
		aim.addRange(om.createResource(XSD.xstring.getURI()));
		aim.addComment("Aim or objective related to the argument.", "en");
		aim.addComment("Objectif ou visée de l'argument.", "fr");

		DatatypeProperty nameProperty = om.createDatatypeProperty(ncl + "nameProperty");
		nameProperty.addDomain(Argument);
		nameProperty.addRange(om.createResource(XSD.xstring.getURI()));
		nameProperty.addComment("Name of the property evaluated in the argument.", "en");
		nameProperty.addComment("Nom de la propriété évaluée dans l'argument.", "fr");

		DatatypeProperty valueProperty = om.createDatatypeProperty(ncl + "valueProperty");
		valueProperty.addDomain(Argument);
		valueProperty.addRange(om.createResource(XSD.xstring.getURI()));
		valueProperty.addComment("Value associated with the argument property.", "en");
		valueProperty.addComment("Valeur associée à la propriété de l'argument.", "fr");

		DatatypeProperty condition = om.createDatatypeProperty(ncl + "condition");
		condition.addDomain(Argument);
		condition.addRange(om.createResource(XSD.xstring.getURI()));
		condition.addComment("Condition under which the argument applies.", "en");
		condition.addComment("Condition dans laquelle l'argument s'applique.", "fr");

		DatatypeProperty infValue = om.createDatatypeProperty(ncl + "infValue");
		infValue.addDomain(Argument);
		infValue.addRange(om.createResource(XSD.xdouble.getURI()));
		infValue.addComment("Lower bound value for the argument condition.", "en");
		infValue.addComment("Valeur minimale pour la condition de l'argument.", "fr");

		DatatypeProperty supValue = om.createDatatypeProperty(ncl + "supValue");
		supValue.addDomain(Argument);
		supValue.addRange(om.createResource(XSD.xdouble.getURI()));
		supValue.addComment("Upper bound value for the argument condition.", "en");
		supValue.addComment("Valeur maximale pour la condition de l'argument.", "fr");

		DatatypeProperty unitArg = om.createDatatypeProperty(ncl + "unit");
		unitArg.addDomain(Argument);
		unitArg.addRange(om.createResource(XSD.xstring.getURI()));
		unitArg.addComment("Unit of measurement for the argument values.", "en");
		unitArg.addComment("Unité de mesure pour les valeurs de l'argument.", "fr");

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
		
		DatatypeProperty hasFunction = om.createDatatypeProperty(ncl + "hasFunction");
		hasFunction.addDomain(Ingredient);
		hasFunction.addRange(om.createResource(XSD.xstring.getURI()));
		hasFunction.addComment("The technological or sensory function of an ingredient (preservative, flavor enhancer, etc.).", "en");
		hasFunction.addComment("La fonction technologique ou sensorielle d'un ingrédient (conservateur, exhausteur de goût, etc.).", "fr");

		DatatypeProperty containsAdditives = om.createDatatypeProperty(ncl + "containsAdditives");
		containsAdditives.addDomain(Product);
		containsAdditives.addRange(om.createResource(XSD.xboolean.getURI()));
		containsAdditives.addComment("Indicates whether the product contains additives.", "en");
		containsAdditives.addComment("Indique si le produit contient des additifs.", "fr");			
	
		//////////////////////////////////////////////////////////
	    // Définition des annotation property                   //
	    //////////////////////////////////////////////////////////
		om.createAnnotationProperty(ncl + "hasEAN13");
		om.createAnnotationProperty(ncl + "hasTrademark");
		om.createAnnotationProperty(ncl + "hasIdIngredientOFF");
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
	
		NCL.addSubClass(Resource);
		NCL.addSubClass(Argument);
		NCL.addSubClass(CleanLabel);
		NCL.addSubClass(Context);
		NCL.addSubClass(ContextIngredient);
		NCL.addSubClass(ContextProduct);
		NCL.addSubClass(ControlledOriginLabel);
		NCL.addSubClass(FNI);
		NCL.addSubClass(NutriScore);
		NCL.addSubClass(NutriScoreDetail);
		NCL.addSubClass(NutriScoreAlpha);
		NCL.addSubClass(Packaging);
		NCL.addSubClass(Shape);
		NCL.addSubClass(Material);
		NCL.addSubClass(QuantifiedElement);	
		NCL.addSubClass(Source);
		NCL.addSubClass(Verbatim);
		NCL.addSubClass(ManufacturingProcess);
		NCL.addSubClass(NaturalnessScore);
		NCL.addSubClass(Allegation);
		NCL.addSubClass(Origin);
		NCL.addSubClass(Attribute);
		NCL.addSubClass(Category);
		NCL.addSubClass(Subcategory);

	
		CompositeProduct.addSuperClass(Product);
		SimpleProduct.addSuperClass(Product);
		Product.addSuperClass(Resource);
		Argument.addSuperClass(Node);
		Ingredient.addSuperClass(Resource);
		CompositeIngredient.addSuperClass(Ingredient);
		SimpleIngredient.addSuperClass(Ingredient);
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