package inferencesAndQueries;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.apache.jena.rdf.model.InfModel;


import natclinn.util.NatclinnConf;
import natclinn.util.NatclinnQueryObject;
import natclinn.util.NatclinnUtil;
import ontologyManagement.CreateMychoiceProjectFromPreliminaryProject;

public class NatclinnQueryStatistics {

	public static void main(String[] args) throws Exception {
		// Forcer l'encodage UTF-8 pour la console afin d'éviter les problèmes d'accents
		try {
			System.setProperty("file.encoding", "UTF-8");
			System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
			System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8));
		} catch (Exception e) {
			// Ignorer silencieusement si non supporté
		}

		// ============================================================
        // Vérification / définition de la propriété SIS_DATA
        // ============================================================

		// Initialisation de la configuration
		// Chemin d'accès, noms fichiers...
		new NatclinnConf();


		String prefix = NatclinnConf.queryPrefix;
		String titleQuery = "";
		String commentQuery = "";
		String typeQuery = "";
		String stringQuery = "";
		Integer idQuery = 0;
		ArrayList<String> listOntologiesFileName = new ArrayList<String>();
		ArrayList<String> listRulesFileName = new ArrayList<String>();
		ArrayList<String> listPrimitives = new ArrayList<String>();
		String topSpatial = "";
		ArrayList<NatclinnQueryObject> listQuery = new ArrayList<NatclinnQueryObject>();


		/////////////////////////////////////////////////////
		// Etude des parcelles                             //
		/////////////////////////////////////////////////////

		/////////////////////////////////////////////////////
		// Injection des données nécessaires pour l'étude  //
		/////////////////////////////////////////////////////
		listQuery.clear();
		titleQuery = "Insertion du namespace de l'ontologie dans le modèle";
		typeQuery = "INSERT";
	// Insertion dans le modèle du nameSpace de l'ontologie (pour les rules)
	stringQuery = prefix + "INSERT DATA {ncl:thisOntology ncl:hasNameSpace ncl:.}";
	listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
	idQuery++;		// /////////////////////////////////////////////////////
		// // TEST : Liens produits-arguments au hasard      //
		// /////////////////////////////////////////////////////
		// titleQuery = "TEST - Liens produits-arguments aléatoires";
		// commentQuery = "Crée des liens ncl:hasProductArgument arbitraires pour tester l'export";
		// typeQuery = "INSERT";
		// stringQuery = prefix + 
		// 	"INSERT DATA { " +
		// 	// Produit P-3564700423196 -> Arguments naturalité/additifs
		// 	"  ncl:P-3564700423196 ncl:hasProductArgument ncl:A-0000008 . " +  // Conservateurs naturels
		// 	"  ncl:P-3564700423196 ncl:hasProductArgument ncl:A-0000007 . " +  // Colorants naturels
		// 	"  ncl:P-3564700423196 ncl:hasProductArgument ncl:A-0000023 . " +  // Transformation minimale
		// 	// Produit P-3250392814908 -> Arguments bio/clean label
		// 	"  ncl:P-3250392814908 ncl:hasProductArgument ncl:A-0000045 . " +  // Certification biologique
		// 	"  ncl:P-3250392814908 ncl:hasProductArgument ncl:A-0000011 . " +  // Absence d'additifs
		// 	"  ncl:P-3250392814908 ncl:hasProductArgument ncl:A-0000043 . " +  // Allégations naturel
		// 	// Produit P-3302740044786 -> Arguments texture/fraîcheur
		// 	"  ncl:P-3302740044786 ncl:hasProductArgument ncl:A-0000041 . " +  // Texture de qualité
		// 	"  ncl:P-3302740044786 ncl:hasProductArgument ncl:A-0000039 . " +  // Fraîcheur perçue
		// 	"  ncl:P-3302740044786 ncl:hasProductArgument ncl:A-0000040 . " +  // Goût authentique
		// 	// Produit P-3564709163871 -> Arguments production
		// 	"  ncl:P-3564709163871 ncl:hasProductArgument ncl:A-0000032 . " +  // Production artisanale
		// 	"  ncl:P-3564709163871 ncl:hasProductArgument ncl:A-0000033 . " +  // Production locale
		// 	// Produit P-3178530410105 -> Arguments ingrédients
		// 	"  ncl:P-3178530410105 ncl:hasProductArgument ncl:A-0000009 . " +  // Ingrédients bruts
		// 	"  ncl:P-3178530410105 ncl:hasProductArgument ncl:A-0000021 . " +  // Longueur liste ingrédients
		// 	"  ncl:P-3178530410105 ncl:hasProductArgument ncl:A-0000014 . " +  // Absence de conservateurs
		// 	// Produit P-3245412343810 -> Arguments qualité/origine
		// 	"  ncl:P-3245412343810 ncl:hasProductArgument ncl:A-0000047 . " +  // Qualité nutritionnelle
		// 	"  ncl:P-3245412343810 ncl:hasProductArgument ncl:A-0000048 . " +  // Origine géographique
		// 	"  ncl:P-3245412343810 ncl:hasProductArgument ncl:A-0000002 . " +  // Naturalité perçue
		// 	"}";
		// listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));

		/////////////////////////////////////////////////////
		// Pré-création du projet MyChoice                //
		/////////////////////////////////////////////////////
		titleQuery = "Création du projet MyChoice Moussakas";
		commentQuery = "Toutes les propriétés du projet peuvent être personnalisées ici";
		typeQuery = "INSERT";
		stringQuery = prefix + 
			"INSERT DATA { " +
			"  mch:Project-Moussakas rdf:type mch:Project . " +
			// Propriétés obligatoires
			"  mch:Project-Moussakas mch:projectName \"Projet moussakas\"@fr . " +
			"  mch:Project-Moussakas mch:projectDescription \"Projet généré automatiquement à partir des produits et arguments inférés\"@fr . " +
			// Propriété optionnelle : image du projet (par défaut si non spécifiée)
			"  mch:Project-Moussakas mch:projectImage \"https://cdn.pixabay.com/photo/2022/01/18/16/30/vegetables-6947444_960_720.jpg\" . " +
			// Vous pouvez ajouter d'autres propriétés ici selon vos besoins
			"}";
		listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
		idQuery++;

		titleQuery = "Création des alternatives du projet Moussakas";
		commentQuery = "Chaque produit de la liste devient une alternative du projet MyChoice";
		typeQuery = "INSERT";
		stringQuery = prefix + 
			"INSERT { " +
			"  ?alternative rdf:type mch:Alternative . " +
			"  ?alternative mch:nameAlternative ?productName . " +
			"  ?alternative mch:alternativeDescription ?productDescription . " +
			"  ?alternative mch:imageAlternative ?imageAlternative . " +
			"  ?alternative mch:iconAlternative ?iconAlternative . " +
			"  ?alternative mch:relatedToProduct ?product . " +
			"  ?alternative mch:hasProject mch:Project-Moussakas . " + // Alternative -> Project
			"} WHERE { " +
			"  VALUES (?ean13 ?imageAlt ?iconAlt) { " +
			"    (\"3564700423196\" \"https://images.openfoodfacts.org/images/products/356/470/042/3196/front_fr.30.400.jpg\" \"numeric-1\") " +       
			"    (\"3250392814908\" \"https://images.openfoodfacts.org/images/products/325/039/281/4908/front_fr.3.400.jpg\" \"numeric-2\") " +        
			"    (\"3302740044786\" \"https://images.openfoodfacts.org/images/products/330/274/004/4786/front_fr.1123.400.jpg\" \"numeric-3\") " +       
			// Ajoutez ici d'autres codes EAN13 avec leurs images et icônes
			// Format: (\"EAN13\" \"URL_IMAGE\" \"ICON\")
			"  } " +
			"  ?product rdf:type ncl:Product . " +
			"  ?product ncl:hasEAN13 ?ean13 . " +
			"  ?product skos:prefLabel ?productName . " +
			"  OPTIONAL { ?product ncl:description ?productDescription } " +
			"  BIND(?imageAlt AS ?imageAlternative) " +
			"  BIND(?iconAlt AS ?iconAlternative) " +
		"  BIND(IRI(CONCAT(STR(mch:), \"Alternative-P-\", STR(?ean13))) AS ?alternative) " + // STR() assure compatibilité si ?ean13 est typé
		"}";
	listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
	idQuery++;

	/////////////////////////////////////////////////////
	// Pré-création d'un 2ème projet MyChoice         //
		/////////////////////////////////////////////////////
		titleQuery = "Création du projet Mychoice Madeleines";
		commentQuery = "Comparaison de Madeleines";
		typeQuery = "INSERT";
		stringQuery = prefix + 
			"INSERT DATA { " +
			"  mch:Project-Madeleines rdf:type mch:Project . " +
			"  mch:Project-Madeleines mch:projectName \"Project madeleines\"@fr . " +
			"  mch:Project-Madeleines mch:projectDescription \"Comparaison nutritionnelle de madeleines\"@fr . " +
		"  mch:Project-Madeleines mch:projectImage \"https://cdn.pixabay.com/photo/2022/01/18/16/30/vegetables-6947444_960_720.jpg\" . " +
		"}";
	listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
	idQuery++;

	titleQuery = "Création des alternatives du projet Madeleines";
		typeQuery = "INSERT";
		stringQuery = prefix + 
			"INSERT { " +
			"  ?alternative rdf:type mch:Alternative . " +
			"  ?alternative mch:nameAlternative ?productName . " +
			"  ?alternative mch:alternativeDescription ?productDescription . " +
			"  ?alternative mch:imageAlternative ?imageAlternative . " +
			"  ?alternative mch:iconAlternative ?iconAlternative . " +
			"  ?alternative mch:relatedToProduct ?product . " +
			"  ?alternative mch:hasProject mch:Project-Madeleines . " + // Alternative -> Project
			"} WHERE { " +
			"  VALUES (?ean13 ?imageAlt ?iconAlt) { " +
			"    (\"3564709163871\" \"\" \"\") " +
			"    (\"3178530410105\" \"\" \"\") " +
			"    (\"3245412343810\" \"\" \"\") " +
			"  } " +
			"  ?product rdf:type ncl:Product . " +
			"  ?product ncl:hasEAN13 ?ean13 . " +
			"  ?product skos:prefLabel ?productName . " +
			"  OPTIONAL { ?product ncl:description ?productDescription } " +
			"  BIND(?imageAlt AS ?imageAlternative) " +
			"  BIND(?iconAlt AS ?iconAlternative) " +
		"  BIND(IRI(CONCAT(STR(mch:), \"Alternative-P-\", STR(?ean13))) AS ?alternative) " + // Harmonisation avec Moussakas : STR() pour types littéraux
		"}";
	listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
	idQuery++;


	// // Vérifications pour le projet Moussakas
		// 		titleQuery = "Vérif: produits Moussakas par codes EAN13";
		// 		commentQuery = "Confirme l'existence des produits ciblés (ncl:hasEAN13)";
		// 		typeQuery = "SELECT";
		// 		stringQuery = prefix +
		// 			"SELECT ?product ?productName WHERE { " +
		// 			"  VALUES ?ean13 { \"3564700423196\" \"3250392814908\" \"3302740044786\" } " +
		// 			"  ?product rdf:type ncl:Product . " +
		// 			"  ?product ncl:hasEAN13 ?ean13 . " +
		// 			"  ?product skos:prefLabel ?productName . " +
		// 			"}";
		// 		listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));

		// 		titleQuery = "Vérif: alternatives du projet Moussakas";
		// 		commentQuery = "Liste des alternatives créées et leur produit lié";
		// 		typeQuery = "SELECT";
		// 		stringQuery = prefix +
		// 			"SELECT ?alt ?prod WHERE { " +
		// 			"  ?alt mch:hasProject mch:Project-Moussakas . " +
		// 			"  ?alt mch:relatedToProduct ?prod . " +
		// 			"}";
		// 		listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));



		// // Vérifications pour le projet Madeleines
		// titleQuery = "Vérif: produits Madeleines par codes EAN13";
		// commentQuery = "Confirme l'existence des produits ciblés (ncl:hasEAN13)";
		// typeQuery = "SELECT";
		// stringQuery = prefix +
		// 	"SELECT ?product ?productName WHERE { " +
		// 	"  VALUES ?ean13 { \"3564709163871\" \"3178530410105\" \"3245412343810\" } " +
		// 	"  ?product rdf:type ncl:Product . " +
		// 	"  ?product ncl:hasEAN13 ?ean13 . " +
		// 	"  ?product skos:prefLabel ?productName . " +
		// 	"}";
		// listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));

		titleQuery = "Vérif: alternatives du projet Madeleines";
		commentQuery = "Liste des alternatives créées et leur produit lié";
		typeQuery = "SELECT";
		stringQuery = prefix +
			"SELECT (?alt AS ?Alternative) (?prodName AS ?Produit) (?prodEAN AS ?EAN_Produit) WHERE { " +
			"  ?alt mch:hasProject mch:Project-Madeleines . " +
			"  ?alt mch:relatedToProduct ?prod . " +
			"  ?prod skos:prefLabel ?prodName . " +
		"  ?prod ncl:hasEAN13 ?prodEAN . " +	
		"}";
	listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
	idQuery++;

	titleQuery = "Vérif: alternatives du projet Moussakas";
		commentQuery = "Liste des alternatives créées et leur produit lié";
		typeQuery = "SELECT";
		stringQuery = prefix +
			"SELECT (?alt AS ?Alternative) (?prodName AS ?Produit) (?prodEAN AS ?EAN_Produit) WHERE { " +
			"  ?alt mch:hasProject mch:Project-Moussakas . " +
			"  ?alt mch:relatedToProduct ?prod . " +
			"  ?prod skos:prefLabel ?prodName . " +
			"  ?prod ncl:hasEAN13 ?prodEAN . " +	
			"}";
		listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
		
		 /////////////////////////////
		// Affichage des résultats //
		/////////////////////////////

	titleQuery = "NATCLINN - STATISTIQUES GLOBALES";
	typeQuery = "";
	stringQuery = "";
	listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
	idQuery++;

	titleQuery = "Taille du_modèle après inférences (en triplets)";
	typeQuery = "SELECT";
	stringQuery = prefix + "SELECT (COUNT(*) AS ?Nombre_De_Triplets) WHERE { ?s ?p ?o. }";
	listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
	idQuery++;

	titleQuery = "Instances de la classe Product ";
		typeQuery = "SELECT";
		stringQuery = prefix + "SELECT DISTINCT (?s AS ?Instance_Produit) (?prodName AS ?Nom_Produit) WHERE { " +
			"?s ?p ?o." +
			"?s rdf:type ncl:Product." +
		"?s skos:prefLabel ?prodName . " +
		"}";
	listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
	idQuery++;

	titleQuery = "Instances avec le flag isProductIAA à true ";
		typeQuery = "SELECT";
		stringQuery = prefix + "SELECT DISTINCT (?s AS ?Instance_ProduitIAA) (?prodName AS ?Nom_Produit) WHERE { " +
			"?s rdf:type ncl:Product. " +	
			"?s ncl:isProductIAA 'true'^^xsd:boolean." +
		"?s skos:prefLabel ?prodName . " +
		"}";
	listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
	idQuery++;

	titleQuery = "Tous les ingrédients avec leurs rôles associés pour un produit donné P-3564700423196";
	commentQuery = "Utilise les chemins de propriétés SPARQL 1.1 pour une récursivité complète";
	typeQuery = "SELECT";
	stringQuery = prefix + 
		"SELECT DISTINCT ?ingredientLabel " +
		"(GROUP_CONCAT(DISTINCT ?roleLabel; separator=\", \") AS ?roles) " +
		"WHERE { " +
		"    VALUES ?targetProduct { <https://w3id.org/NCL/ontology/P-3564700423196> } " +
		"    " +
		// Naviguer récursivement : Product -> (composedOf)* -> Product -> hasIngredient -> Ingredient
		// Naviguer récursivement : Product -> hasIngredient|composedOf -> Ingredient
			"?targetProduct (ncl:hasIngredient|ncl:hasComposedOf)* ?ingredient ." +
		"    " +
		// S'assurer que c'est bien un ingrédient et récupérer son label
		"    ?ingredient a ncl:Ingredient ; " +
		"                skos:prefLabel ?ingredientLabel . " +
		"    " +
		// Optionnel : récupérer les rôles des ingrédients
		"    OPTIONAL { ?ingredient ncl:hasRole ?role . ?role skos:prefLabel ?roleLabel . } " +
		"} " +
		"GROUP BY ?ingredient ?ingredientLabel " +
		"ORDER BY ?ingredient";
		// System.out.println(stringQuery);
	listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
	idQuery++;
	
	
	// titleQuery = "Instances de la classe ProductIAA ";
		// typeQuery = "SELECT";
		// stringQuery = prefix + "SELECT DISTINCT (?product AS ?Instance_ProduitIAA) WHERE { " +
		// "?product rdf:type ncl:Product. " +
		// "FILTER NOT EXISTS { ?otherProduct ncl:hasComposedOf ?product } " +
		// "}";
		// listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
		
		// titleQuery = "Nombre de triplets (calcNumberOfTriples) ";
		// typeQuery = "SELECT";
		// stringQuery = prefix + "SELECT (?o AS ?numberOfTriples) WHERE { " +
		// 	"?s ncl:numberOfTriples ?o." +
		// 	"}";
		// listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));

		titleQuery = "Résumé des rôles dans un produit par rapport aux ingrédients";
		typeQuery = "SELECT";
		stringQuery = prefix + "SELECT (?targetName AS ?Nom_Produit) (COUNT(DISTINCT ?ingredient) AS ?nbIngredients) " +
				"(GROUP_CONCAT(DISTINCT ?roleLabel; separator=\", \") AS ?Role_additif) " +
			"WHERE { " +
				"?targetProduct rdf:type ncl:Product . " +
				"?targetProduct skos:prefLabel ?targetName . " +
				"?targetProduct ncl:isProductIAA 'true'^^xsd:boolean." +
				// Naviguer récursivement : Product -> hasIngredient|hasComposedOf -> Ingredient
				"?targetProduct (ncl:hasIngredient|ncl:hasComposedOf)* ?ingredient ." +
				"    " +
				// S'assurer que c'est bien un ingrédient et récupérer son label et sa role
				"    ?ingredient a ncl:Ingredient ; " +
				"                skos:prefLabel ?ingredientLabel ; " +
				"                ncl:hasRole ?role . " +
				"    ?role   skos:prefLabel ?roleLabel . " +
				"    " +
				// EXCLURE les CompositeIngredient
        		"FILTER NOT EXISTS { ?ingredient a ncl:CompositeIngredient } " +
			"} " +
		"GROUP BY ?targetName " ;
	listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
	idQuery++;


	titleQuery = "Résumé des rôles dans un produit par rapport à l'absence d'ingrédients";
		typeQuery = "SELECT";
		stringQuery = prefix + "SELECT (?targetName AS ?Nom_Produit)  " +
				"(GROUP_CONCAT(DISTINCT ?roleLabel; separator=\", \") AS ?Role_additif) " +
			"WHERE { " +
				"?targetProduct rdf:type ncl:Product . " +
				"?targetProduct skos:prefLabel ?targetName . " +
				"?targetProduct ncl:isProductIAA 'true'^^xsd:boolean . " +
				"?targetProduct ncl:hasAdditiveRoleCheck ?role . " +
				"?role skos:prefLabel ?roleLabel . " +
			"} " +
		"GROUP BY ?targetName " ;
	listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
	idQuery++;	


	titleQuery = "Arguments disponibles (ncl:ProductArgument avec nameProperty)";
	commentQuery = "Liste tous les arguments qui peuvent être liés aux rôles";
	typeQuery = "SELECT";
	stringQuery = prefix + "SELECT ?argument ?nameProperty WHERE { " +
		"?argument rdf:type ncl:ProductArgument . " +
		"?argument ncl:nameProperty ?nameProperty . " +
		"} ORDER BY ?nameProperty";
	listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
	idQuery++;		
	
	
		// titleQuery = "Roles des additifs avec leurs labels";
		// commentQuery = "Vérifie comment les rôles sont étiquetées (skos:prefLabel, rdfs:label)";
		// typeQuery = "SELECT";
		// stringQuery = prefix + "SELECT DISTINCT ?role ?labelSkos ?labelRdfs WHERE { " +
		// 	"?ingredient ncl:hasRole ?role . " +
		// 	"OPTIONAL { ?role skos:prefLabel ?labelSkos } " +
		// 	"OPTIONAL { ?role rdfs:label ?labelRdfs } " +
		// 	"} ORDER BY ?role";
		// listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));

		// titleQuery = "Propriétés des rôles (toutes)";
		// commentQuery = "Montre toutes les propriétés des rôles pour comprendre leur structure";
		// typeQuery = "SELECT";
		// stringQuery = prefix + "SELECT DISTINCT ?role ?property ?value WHERE { " +
		// 	"?ingredient ncl:hasRole ?role . " +
		// 	"?role ?property ?value . " +
		// 	"} ORDER BY ?role ?property";
		// listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));

		// titleQuery = "test";
		// typeQuery = "SELECT";
		// stringQuery = prefix + "SELECT DISTINCT ?s  WHERE { " +
		// 	"?s rdf:type ncl:ComposedIngredient . " +
		// 	"}";
		// listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
		

		
		// titleQuery = "Test arguments";
		// typeQuery = "SELECT";
		// stringQuery = prefix + 
		// 	"SELECT ?arg " +
		// 	"WHERE { " +
		// 	"  ?product ncl:hasProductArgument ?arg . " +
		// 	"} " ;
		// listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
		
		titleQuery = "Produits liés à des arguments par inférence";
		typeQuery = "SELECT";
		stringQuery = prefix + 
			"SELECT (?nameProduct AS ?Nom_Produit) (COUNT(?arg) AS ?nbArguments) " +
			"(GROUP_CONCAT(DISTINCT ?argNameProperty; separator=\", \") AS ?arguments) " +
			"WHERE { " +
			"  ?product rdf:type ncl:Product . " +
			"  ?product skos:prefLabel ?nameProduct . " +
			"  ?product ncl:hasProductArgument ?arg . " +
			"  ?arg skos:prefLabel ?argLabel . " +
			"  ?arg ncl:nameProperty ?argNameProperty . " +
			"} " +
			"GROUP BY ?product ?nameProduct " +
			"ORDER BY DESC(?nbArguments)";
		listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
		idQuery++;


		/////////////////////////////////////////////////////
		// CONTRÔLES EMBALLAGES                            //
		/////////////////////////////////////////////////////

		titleQuery = "Types d'emballages détectés";
		commentQuery = "Liste tous les types d'emballages identifiés par la primitive getTypePackaging (compte les produits distincts)";
		typeQuery = "SELECT";
		stringQuery = prefix + 
			"SELECT DISTINCT ?packagingType (COUNT(DISTINCT ?product) AS ?nbProducts) " +
			"WHERE { " +
			"  ?product ncl:hasTypePackaging ?packagingType . " +
			"} " +
			"GROUP BY ?packagingType " +
			"ORDER BY DESC(?nbProducts)";
		listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
		idQuery++;

		titleQuery = "Nombre d'emballages par produit";
		commentQuery = "Montre combien de matériaux d'emballage différents chaque produit possède";
		typeQuery = "SELECT";
		stringQuery = prefix + 
			"SELECT ?product ?nameProduct (COUNT(?packaging) AS ?nbPackagings) " +
			"(GROUP_CONCAT(DISTINCT ?materialLabel; separator=\", \") AS ?materials) " +
			"WHERE { " +
			"  ?product rdf:type ncl:Product . " +
			"  ?product ncl:hasPackaging ?packaging . " +
			"  ?packaging ncl:hasMaterial ?material . " +
			"  OPTIONAL { ?product skos:prefLabel ?nameProduct } " +
			"  OPTIONAL { ?material skos:prefLabel ?materialLabel } " +
			"} " +
			"GROUP BY ?product ?nameProduct " +
			"ORDER BY DESC(?nbPackagings)";
		listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
		idQuery++;

		titleQuery = "Produits avec emballage plastique";
		commentQuery = "Liste les produits contenant du plastique dans leur emballage";
		typeQuery = "SELECT";
		stringQuery = prefix + 
			"SELECT ?product ?nameProduct " +
			"WHERE { " +
			"  ?product rdf:type ncl:Product . " +
			"  ?product ncl:hasTypePackaging ncl:emballage_plastique . " +
			"  OPTIONAL { ?product skos:prefLabel ?nameProduct } " +
			"} " +
			"ORDER BY ?nameProduct";
		listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
		idQuery++;

		titleQuery = "Produits sans plastique";
		commentQuery = "Liste les produits ayant reçu l'annotation ncl:sans_plastique";
		typeQuery = "SELECT";
		stringQuery = prefix + 
			"SELECT ?product ?nameProduct " +
			"WHERE { " +
			"  ?product rdf:type ncl:Product . " +
			"  ?product ncl:hasPackagingCheck ncl:sans_plastique . " +
			"  OPTIONAL { ?product skos:prefLabel ?nameProduct } " +
			"} " +
			"ORDER BY ?nameProduct";
		listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
		idQuery++;

		titleQuery = "Produits avec emballage naturel";
		commentQuery = "Produits avec emballages naturels (verre, carton, bois, métal)";
		typeQuery = "SELECT";
		stringQuery = prefix + 
			"SELECT ?product ?nameProduct (GROUP_CONCAT(DISTINCT ?packagingType; separator=\", \") AS ?types) " +
			"WHERE { " +
			"  ?product rdf:type ncl:Product . " +
			"  ?product ncl:hasPackagingCheck ncl:emballage_naturel . " +
			"  OPTIONAL { ?product skos:prefLabel ?nameProduct } " +
			"  OPTIONAL { ?product ncl:hasTypePackaging ?packagingType } " +
			"} " +
			"GROUP BY ?product ?nameProduct " +
			"ORDER BY ?nameProduct";
		listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
		idQuery++;

		titleQuery = "Produits avec emballage biodégradable/compostable";
		commentQuery = "Produits avec emballages biodégradables, compostables ou biosourcés";
		typeQuery = "SELECT";
		stringQuery = prefix + 
			"SELECT ?product ?nameProduct ?checkType " +
			"WHERE { " +
			"  ?product rdf:type ncl:Product . " +
			"  ?product ncl:hasPackagingCheck ?checkType . " +
			"  OPTIONAL { ?product skos:prefLabel ?nameProduct } " +
			"  FILTER(?checkType IN (ncl:emballage_biodegradable, ncl:emballage_compostable, ncl:emballage_biosource)) " +
			"} " +
			"ORDER BY ?nameProduct ?checkType";
		listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
		idQuery++;

		titleQuery = "Produits sans emballage (vrac)";
		commentQuery = "Produits n'ayant aucun emballage détecté";
		typeQuery = "SELECT";
		stringQuery = prefix + 
			"SELECT ?product ?nameProduct " +
			"WHERE { " +
			"  ?product rdf:type ncl:Product . " +
			"  ?product ncl:hasPackagingCheck ncl:sans_emballage . " +
			"  OPTIONAL { ?product skos:prefLabel ?nameProduct } " +
			"} " +
			"ORDER BY ?nameProduct";
		listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
		idQuery++;

		titleQuery = "Produits avec emballage composite";
		commentQuery = "Produits avec emballages composites (multi-matériaux, difficiles à recycler)";
		typeQuery = "SELECT";
		stringQuery = prefix + 
			"SELECT ?product ?nameProduct " +
			"WHERE { " +
			"  ?product rdf:type ncl:Product . " +
			"  ?product ncl:hasPackagingCheck ncl:emballage_composite . " +
			"  OPTIONAL { ?product skos:prefLabel ?nameProduct } " +
			"} " +
			"ORDER BY ?nameProduct";
		listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
		idQuery++;

		titleQuery = "Détail des emballages par produit";
		commentQuery = "Affiche tous les matériaux d'emballage et leurs types pour chaque produit";
		typeQuery = "SELECT";
		stringQuery = prefix + 
			"SELECT ?product ?nameProduct ?packaging ?material ?materialLabel ?packagingType " +
			"WHERE { " +
			"  ?product rdf:type ncl:Product . " +
			"  ?product ncl:hasPackaging ?packaging . " +
			"  ?packaging ncl:hasMaterial ?material . " +
			"  OPTIONAL { ?product skos:prefLabel ?nameProduct } " +
			"  OPTIONAL { ?material skos:prefLabel ?materialLabel } " +
			"  OPTIONAL { ?packaging ncl:hasTypePackaging ?packagingType } " +
			"} " +
			"ORDER BY ?nameProduct ?materialLabel";
		listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
		idQuery++;

		titleQuery = "Statistiques types d'emballages par catégorie";
		commentQuery = "Compte les occurrences de chaque type d'emballage détecté";
		typeQuery = "SELECT";
		stringQuery = prefix + 
			"SELECT ?packagingType (COUNT(DISTINCT ?product) AS ?nbProducts) " +
			"WHERE { " +
			"  ?product rdf:type ncl:Product . " +
			"  ?product ncl:hasTypePackaging ?packagingType . " +
			"} " +
			"GROUP BY ?packagingType " +
			"ORDER BY DESC(?nbProducts)";
		listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
		idQuery++;


        // titleQuery = "Test sel";
        // typeQuery = "SELECT";
        // stringQuery = prefix + 
        //     "SELECT ?ingredient ?prefLabel ?ciqual ?off ?hasIngredientR " +
        //     "WHERE { " +
        //     "  ncl:P-3564700423196 ncl:hasIngredient* ?ingredient . " +
        //     "  OPTIONAL { ?ingredient skos:prefLabel ?prefLabel } " +
        //     "  OPTIONAL { ?ingredient ncl:hasCiqualFoodCode ?ciqual } " +
        //     "  OPTIONAL { ?ingredient ncl:hasIdIngredientOFF ?off } " +
        //     "  OPTIONAL { ncl:P-3564700423196 ncl:hasIngredientR ?hasIngredientR . " +
        //     "             FILTER(?hasIngredientR = ?ingredient) } " +
        //     "  FILTER(CONTAINS(LCASE(STR(?prefLabel)), \"sel\") || " +
        //     "         ?ciqual = \"11058\" || " +
        //     "         ?off = \"en:salt\") " +
        //     "}";
        // listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
        // idQuery++;

        // titleQuery = "Test sel2";
        // typeQuery = "ASK";
        // stringQuery = prefix + 
        //     "ASK { " +
        //     "  ncl:P-3564700423196 ncl:hasIngredientR ?ing . " +
        //     "  ?ing skos:prefLabel \"sel\"^^xsd:string . " +
        //     "}";
        // listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
        // idQuery++;

        // titleQuery = "Test sel3 - containsSalt";
        // typeQuery = "ASK";
        // stringQuery = prefix + 
        //     "ASK { " +
        //     "  ncl:P-3564700423196 ncl:containsSalt 'true'^^xsd:boolean . " +
        //     "}";
        // listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
        // idQuery++;

        // titleQuery = "Test sel4 - toutes propriétés sel";
        // typeQuery = "SELECT";
        // stringQuery = prefix + 
        //     "SELECT ?p ?o " +
        //     "WHERE { " +
        //     "  ncl:P-3564700423196 ?p ?o . " +
        //     "  FILTER(CONTAINS(STR(?p), \"Salt\") || CONTAINS(STR(?o), \"sel\")) " +
        //     "}";
        // listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
        // idQuery++;

        // titleQuery = "Test sel5 - containsIngredientWithRole ncl:sel";
        // typeQuery = "ASK";
        // stringQuery = prefix + 
        //     "ASK { " +
        //     "  ncl:P-3564700423196 ncl:containsIngredientWithRole ncl:sel . " +
        //     "}";
        // listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
        // idQuery++;
		
		
		// titleQuery = "Périodes étudiées";
		// typeQuery = "SELECT";
		// stringQuery = prefix + "SELECT (?period AS ?Periode) (str(?year) AS ?AnneeDebut) (str(?nextYear) AS ?AnneeFin) WHERE { " + 
		// 		"?period rdf:type res:Period." +
		// 		"?period res:hasRank ?rank." +
		// 		"?period res:hasBegin ?year." +
		// 		"?period res:hasEnd ?nextYear." +
		// 		"} ORDER BY ?rank";
		// listQuery.add(new NatclinnQueryObject(titleQuery, typeQuery, stringQuery));

		// titleQuery = "Parcelles étudiées";
		// typeQuery = "SELECT";
		// stringQuery = prefix + "SELECT DISTINCT (?plot AS ?Parcelle) WHERE {?plot res:isPlot ?true." +
		// 		"} ORDER BY ?plot";
		// listQuery.add(new NatclinnQueryObject(titleQuery, typeQuery, stringQuery));

		// titleQuery = "Parcelles forestières";
		// typeQuery = "SELECT";
		// stringQuery = prefix + "SELECT DISTINCT (?plot AS ?Parcelle) WHERE {?plot res:isTFPlot ?true." +
		// 		"} ORDER BY ?plot";
		// listQuery.add(new NatclinnQueryObject(titleQuery, typeQuery, stringQuery));

		// titleQuery = "Propriétés étudiées";
		// typeQuery = "SELECT";
		// stringQuery = prefix + "SELECT DISTINCT (?property AS ?Propriété) WHERE { " +
		// 		"?property res:isPropertyToStudy 'true'^^xsd:Boolean." +
		// 		"} ORDER BY ?plot";
		// listQuery.add(new NatclinnQueryObject(titleQuery, typeQuery, stringQuery));

		// titleQuery = "ETUDES STATISTIQUES DES MESURES";
		// typeQuery = "";
		// stringQuery = "";
		// listQuery.add(new NatclinnQueryObject(titleQuery, typeQuery, stringQuery));

		// titleQuery = "Statistiques sur la croissance des arbres";
		// typeQuery = "";
		// stringQuery = "";
		// listQuery.add(new NatclinnQueryObject(titleQuery, typeQuery, stringQuery));

		// titleQuery = "Statistiques 2015";
		// typeQuery = "";
		// stringQuery = "";
		// listQuery.add(new NatclinnQueryObject(titleQuery, typeQuery, stringQuery));

		// titleQuery = "Statistiques 2015 diamètres des troncs";
		// typeQuery = "SELECT";
		// stringQuery = prefix + "SELECT DISTINCT (?plot AS ?Parcelle) " +
		// 		"(str(?valueDM) AS ?Moyenne_Diametres_Des_Troncs)"+ 
		// 		" WHERE {" +
		// 		"?plot res:hasTreeTrunkDiameterMean2015 ?valueDM." +
		// 		"} ORDER BY ?plot";
		// listQuery.add(new NatclinnQueryObject(titleQuery, typeQuery, stringQuery));


		// titleQuery = "Statistiques 2015 hauteurs des troncs";
		// typeQuery = "SELECT";
		// stringQuery = prefix + "SELECT DISTINCT (?plot AS ?Parcelle) " +
		// 		"(str(?valueHTM) AS ?Moyenne_Hauteurs_Des_Troncs)"+ 
		// 		" WHERE {" +
		// 		"?plot res:hasTreeHeightTrunkMean2015 ?valueHTM." +
		// 		"} ORDER BY ?plot";
		// listQuery.add(new NatclinnQueryObject(titleQuery, typeQuery, stringQuery));


		// titleQuery = "Statistiques 2016";
		// typeQuery = "";
		// stringQuery = "";
		// listQuery.add(new NatclinnQueryObject(titleQuery, typeQuery, stringQuery));

		// titleQuery = "Statistiques 2016 diamètres des troncs";
		// typeQuery = "SELECT";
		// stringQuery = prefix + "SELECT DISTINCT (?plot AS ?Parcelle) " +
		// 		"(str(?valueDM) AS ?Moyenne_Diametres_Des_Troncs)"+ 
		// 		" WHERE {" +
		// 		"?plot res:hasTreeTrunkDiameterMean2016 ?valueDM." +
		// 		"} ORDER BY ?plot";
		// listQuery.add(new NatclinnQueryObject(titleQuery, typeQuery, stringQuery));

		// titleQuery = "Statistiques 2016 hauteurs des troncs";
		// typeQuery = "SELECT";
		// stringQuery = prefix + "SELECT DISTINCT (?plot AS ?Parcelle) " +
		// 		"(str(?valueHM) AS ?Moyenne_Hauteurs_Des_Troncs)"+ 
		// 		" WHERE {" +
		// 		"?plot res:hasTreeHeightTrunkMean2016 ?valueHM." +
		// 		"} ORDER BY ?plot";
		// listQuery.add(new NatclinnQueryObject(titleQuery, typeQuery, stringQuery));

		// titleQuery = "Statistiques 2017";
		// typeQuery = "";
		// stringQuery = "";
		// listQuery.add(new NatclinnQueryObject(titleQuery, typeQuery, stringQuery));

		// titleQuery = "Statistiques 2017 diamètres des troncs";
		// typeQuery = "SELECT";
		// stringQuery = prefix + "SELECT DISTINCT (?plot AS ?Parcelle) " +
		// 		"(str(?valueDM) AS ?Moyenne_Diametres_Des_Troncs)"+ 
		// 		" WHERE {" +
		// 		"?plot res:hasTreeTrunkDiameterMean2017 ?valueDM." +
		// 		"} ORDER BY ?plot";
		// listQuery.add(new NatclinnQueryObject(titleQuery, typeQuery, stringQuery));

		// titleQuery = "Statistiques 2017 hauteurs des troncs";
		// typeQuery = "SELECT";
		// stringQuery = prefix + "SELECT DISTINCT (?plot AS ?Parcelle) " +
		// 		"(str(?valueHM) AS ?Moyenne_Hauteurs_Des_Troncs)"+ 
		// 		" WHERE {" +
		// 		"?plot res:hasTreeHeightTrunkMean2017 ?valueHM." +
		// 		"} ORDER BY ?plot";
		// listQuery.add(new NatclinnQueryObject(titleQuery, typeQuery, stringQuery));

		// titleQuery = "Quantiles";
		// typeQuery = "";
		// stringQuery = "";
		// listQuery.add(new NatclinnQueryObject(titleQuery, typeQuery, stringQuery));

		// titleQuery = "Statistiques 2015";
		// typeQuery = "";
		// stringQuery = "";
		// listQuery.add(new NatclinnQueryObject(titleQuery, typeQuery, stringQuery));

		// titleQuery = "Statistiques 2015 diamètres des troncs";
		// typeQuery = "SELECT";
		// stringQuery = prefix + "SELECT DISTINCT (?plot AS ?Parcelle) " +
		// 		"(str(?valueDQ1) AS ?Quantile_25)"+ 
		// 		"(str(?valueDQ2) AS ?Quantile_50)"+ 
		// 		"(str(?valueDQ3) AS ?Quantile_75)"+ 
		// 		" WHERE {" +
		// 		"?plot res:hasTreeTrunkDiameterQ12015 ?valueDQ1." +
		// 		"?plot res:hasTreeTrunkDiameterQ22015 ?valueDQ2." +
		// 		"?plot res:hasTreeTrunkDiameterQ32015 ?valueDQ3." +
		// 		"} ORDER BY ?plot";
		// listQuery.add(new NatclinnQueryObject(titleQuery, typeQuery, stringQuery));

		// titleQuery = "Statistiques 2015 hauteurs des troncs";
		// typeQuery = "SELECT";
		// stringQuery = prefix + "SELECT DISTINCT (?plot AS ?Parcelle) " +
		// 		"(str(?valueHTQ1) AS ?Quantile_25)"+ 
		// 		"(str(?valueHTQ2) AS ?Quantile_50)"+ 
		// 		"(str(?valueHTQ3) AS ?Quantile_75)"+ 
		// 		" WHERE {" +
		// 		"?plot res:hasTreeHeightTrunkQ12015 ?valueHTQ1." +
		// 		"?plot res:hasTreeHeightTrunkQ22015 ?valueHTQ2." +
		// 		"?plot res:hasTreeHeightTrunkQ32015 ?valueHTQ3." +
		// 		"} ORDER BY ?plot";
		// listQuery.add(new NatclinnQueryObject(titleQuery, typeQuery, stringQuery));

		// titleQuery = "Statistiques 2016";
		// typeQuery = "";
		// stringQuery = "";
		// listQuery.add(new NatclinnQueryObject(titleQuery, typeQuery, stringQuery));		

		// titleQuery = "Statistiques 2016 diamètres des troncs";
		// typeQuery = "SELECT";
		// stringQuery = prefix + "SELECT DISTINCT (?plot AS ?Parcelle) " +
		// 		"(str(?valueDQ1) AS ?Quantile_25)"+ 
		// 		"(str(?valueDQ2) AS ?Quantile_50)"+ 
		// 		"(str(?valueDQ3) AS ?Quantile_75)"+ 
		// 		" WHERE {" +
		// 		"?plot res:hasTreeTrunkDiameterQ12016 ?valueDQ1." +
		// 		"?plot res:hasTreeTrunkDiameterQ22016 ?valueDQ2." +
		// 		"?plot res:hasTreeTrunkDiameterQ32016 ?valueDQ3." +
		// 		"} ORDER BY ?plot";
		// listQuery.add(new NatclinnQueryObject(titleQuery, typeQuery, stringQuery));

		// titleQuery = "Statistiques 2016 hauteurs des troncs";
		// typeQuery = "SELECT";
		// stringQuery = prefix + "SELECT DISTINCT (?plot AS ?Parcelle) " +
		// 		"(str(?valueHQ1) AS ?Quantile_25)"+ 
		// 		"(str(?valueHQ2) AS ?Quantile_50)"+ 
		// 		"(str(?valueHQ3) AS ?Quantile_75)"+ 
		// 		" WHERE {" +
		// 		"?plot res:hasTreeHeightTrunkQ12016 ?valueHQ1." +
		// 		"?plot res:hasTreeHeightTrunkQ22016 ?valueHQ2." +
		// 		"?plot res:hasTreeHeightTrunkQ32016 ?valueHQ3." +

		// 		"} ORDER BY ?plot";
		// listQuery.add(new NatclinnQueryObject(titleQuery, typeQuery, stringQuery));


		// titleQuery = "Statistiques 2017";
		// typeQuery = "";
		// stringQuery = "";
		// listQuery.add(new NatclinnQueryObject(titleQuery, typeQuery, stringQuery));

		// titleQuery = "Statistiques 2017 diamètres des troncs";
		// typeQuery = "SELECT";
		// stringQuery = prefix + "SELECT DISTINCT (?plot AS ?Parcelle) " +
		// 		"(str(?valueDQ1) AS ?Quantile_25)"+ 
		// 		"(str(?valueDQ2) AS ?Quantile_50)"+ 
		// 		"(str(?valueDQ3) AS ?Quantile_75)"+ 
		// 		" WHERE {" +
		// 		"?plot res:hasTreeTrunkDiameterQ12017 ?valueDQ1." +
		// 		"?plot res:hasTreeTrunkDiameterQ22017 ?valueDQ2." +
		// 		"?plot res:hasTreeTrunkDiameterQ32017 ?valueDQ3." +
		// 		"} ORDER BY ?plot";
		// listQuery.add(new NatclinnQueryObject(titleQuery, typeQuery, stringQuery));

		// titleQuery = "Statistiques 2017 hauteur des troncs";
		// typeQuery = "SELECT";
		// stringQuery = prefix + "SELECT DISTINCT (?plot AS ?Parcelle) " +
		// 		"(str(?valueHQ1) AS ?Quantile_25)"+ 
		// 		"(str(?valueHQ2) AS ?Quantile_50)"+ 
		// 		"(str(?valueHQ3) AS ?Quantile_75)"+ 
		// 		" WHERE {" +
		// 		"?plot res:hasTreeHeightTrunkQ12017 ?valueHQ1." +
		// 		"?plot res:hasTreeHeightTrunkQ22017 ?valueHQ2." +
		// 		"?plot res:hasTreeHeightTrunkQ32017 ?valueHQ3." +

		// 		"} ORDER BY ?plot";
		// listQuery.add(new NatclinnQueryObject(titleQuery, typeQuery, stringQuery));

		// titleQuery = "Statistiques 2015 Biomasse des arbres";
		// typeQuery = "SELECT";
		// stringQuery = prefix + "SELECT DISTINCT (?plot AS ?Parcelle) " +
		// 		"(str(?value) AS ?Mediane)"+ 
		// 		" WHERE {" +
		// 		"?plot res:hasBiomassMedian2015 ?value." +
		// 		"} ORDER BY ?plot";
		// listQuery.add(new NatclinnQueryObject(titleQuery, typeQuery, stringQuery));

		// titleQuery = "ETUDES CATEGORIES";
		// typeQuery = "";
		// stringQuery = "";
		// listQuery.add(new NatclinnQueryObject(titleQuery, typeQuery, stringQuery));

		// titleQuery = "Nombres d'arbres par catégories";
		// typeQuery = "";
		// stringQuery = "";
		// listQuery.add(new NatclinnQueryObject(titleQuery, typeQuery, stringQuery));
		
		// titleQuery = "Nombres d'arbres par catégories pour l'année 2015";
		// typeQuery = "SELECT";
		// stringQuery = prefix + "SELECT DISTINCT (?plot AS ?Parcelle) (?categoryName AS ?Categorie) (COUNT(?categoryName) as ?nombre_d_arbres)" +
		// 		" WHERE {" +
		// 		"?plot res:isPlot ?true." +
		// 		"?structuralElement bfo:partOf ?plot." +
		// 		"?observation sosa:hasFeatureOfInterest ?structuralElement." +
		// 		"?observation sosa:observedProperty <http://www.afy.fr/Restinclieres/hasCategory>." +
		// 		"?observation sosa:hasResult ?category." +
		// 		"?category res:hasLocalName ?categoryName." +
		// 		"?observation sosa:phenomenonTime ?timeCategory ." +
		// 		"?timeCategory time:inXSDgYear \"2015\"^^<http://www.w3.org/2001/XMLSchema#gYear> ." +
		// 		"} GROUP BY ?plot ?categoryName ORDER BY ?plot ?categoryName";
		// listQuery.add(new NatclinnQueryObject(titleQuery, typeQuery, stringQuery));	
		
		// titleQuery = "Nombres d'arbres par catégories pour l'année 2016";
		// typeQuery = "SELECT";
		// stringQuery = prefix + "SELECT DISTINCT (?plot AS ?Parcelle) (?categoryName AS ?Categorie) (COUNT(?categoryName) as ?nombre_d_arbres)" +
		// 		" WHERE {" +
		// 		"?plot res:isPlot ?true." +
		// 		"?structuralElement bfo:partOf ?plot." +
		// 		"?observation sosa:hasFeatureOfInterest ?structuralElement." +
		// 		"?observation sosa:observedProperty <http://www.afy.fr/Restinclieres/hasCategory>." +
		// 		"?observation sosa:hasResult ?category." +
		// 		"?category res:hasLocalName ?categoryName." +
		// 		"?observation sosa:phenomenonTime ?timeCategory ." +
		// 		"?timeCategory time:inXSDgYear \"2016\"^^<http://www.w3.org/2001/XMLSchema#gYear> ." +
		// 		"} GROUP BY ?plot ?categoryName ORDER BY ?plot ?categoryName";
		// listQuery.add(new NatclinnQueryObject(titleQuery, typeQuery, stringQuery));	
		
		// titleQuery = "Nombres d'arbres par catégories pour l'année 2017";
		// typeQuery = "SELECT";
		// stringQuery = prefix + "SELECT DISTINCT (?plot AS ?Parcelle) (?categoryName AS ?Categorie) (COUNT(?categoryName) as ?nombre_d_arbres)" +
		// 		" WHERE {" +
		// 		"?plot res:isPlot ?true." +
		// 		"?structuralElement bfo:partOf ?plot." +
		// 		"?observation sosa:hasFeatureOfInterest ?structuralElement." +
		// 		"?observation sosa:observedProperty <http://www.afy.fr/Restinclieres/hasCategory>." +
		// 		"?observation sosa:hasResult ?category." +
		// 		"?category res:hasLocalName ?categoryName." +
		// 		"?observation sosa:phenomenonTime ?timeCategory ." +
		// 		"?timeCategory time:inXSDgYear \"2017\"^^<http://www.w3.org/2001/XMLSchema#gYear> ." +
		// 		"} GROUP BY ?plot ?categoryName ORDER BY ?plot ?categoryName";
		// listQuery.add(new NatclinnQueryObject(titleQuery, typeQuery, stringQuery));	
		
		// titleQuery = "ETUDE DE LA BIOMASSE";
		// typeQuery = "";
		// stringQuery = "";
		// listQuery.add(new NatclinnQueryObject(titleQuery, typeQuery, stringQuery));

		// titleQuery = "Ratio biomasse des arbres éligibles au négoce / biomasse totale par parcelle";
		// typeQuery = "";
		// stringQuery = "";
		// listQuery.add(new NatclinnQueryObject(titleQuery, typeQuery, stringQuery));

		// titleQuery = "Ratio pour le négoce de catégorie I";
		// typeQuery = "";
		// stringQuery = "";
		// listQuery.add(new NatclinnQueryObject(titleQuery, typeQuery, stringQuery));

		// titleQuery = "Ratio biomasse des arbres éligibles au négoce Cat I/ biomasse totale par parcelle pour l'année 2015";
		// typeQuery = "SELECT";
		// stringQuery = prefix + "SELECT DISTINCT (?plot AS ?Parcelle) (str(?value) AS ?Ratio)" +
		// 		" WHERE {" +
		// 		"?plot res:hasBiomassBycategoryIRatio2015 ?value." +
		// 		"} ORDER BY ?plot";
		// listQuery.add(new NatclinnQueryObject(titleQuery, typeQuery, stringQuery));

		// titleQuery = "Ratio biomasse des arbres éligibles au négoce Cat I / biomasse totale par parcelle pour l'année 2016";
		// typeQuery = "SELECT";
		// stringQuery = prefix + "SELECT DISTINCT (?plot AS ?Parcelle) (str(?value) AS ?Ratio) " +
		// 		" WHERE {" +
		// 		"?plot res:hasBiomassBycategoryIRatio2016 ?value." +
		// 		"} ORDER BY ?plot";
		// listQuery.add(new NatclinnQueryObject(titleQuery, typeQuery, stringQuery));

		// titleQuery = "Ratio biomasse des arbres éligibles au négoce Cat I / biomasse totale par parcelle pour l'année 2017";
		// typeQuery = "SELECT";
		// stringQuery = prefix + "SELECT DISTINCT (?plot AS ?Parcelle) (str(?value) AS ?Ratio) "+
		// 		" WHERE {" +
		// 		"?plot res:hasBiomassBycategoryIRatio2017 ?value." +
		// 		"} ORDER BY ?plot";
		// listQuery.add(new NatclinnQueryObject(titleQuery, typeQuery, stringQuery));


		// titleQuery = "Ratio pour le négoce de catégorie II";
		// typeQuery = "";
		// stringQuery = "";
		// listQuery.add(new NatclinnQueryObject(titleQuery, typeQuery, stringQuery));

		// titleQuery = "Ratio biomasse des arbres éligibles au négoce Cat II/ biomasse totale par parcelle pour l'année 2015";
		// typeQuery = "SELECT";
		// stringQuery = prefix + "SELECT DISTINCT (?plot AS ?Parcelle) (str(?value) AS ?Ratio) "+ 
		// 		" WHERE {" +
		// 		"?plot res:hasBiomassBycategoryIIRatio2015 ?value." +
		// 		"} ORDER BY ?plot";
		// listQuery.add(new NatclinnQueryObject(titleQuery, typeQuery, stringQuery));

		// titleQuery = "Ratio biomasse des arbres éligibles au négoce Cat II / biomasse totale par parcelle pour l'année 2016";
		// typeQuery = "SELECT";
		// stringQuery = prefix + "SELECT DISTINCT (?plot AS ?Parcelle) (str(?value) AS ?Ratio) "+
		// 		" WHERE {" +
		// 		"?plot res:hasBiomassBycategoryIIRatio2016 ?value." +
		// 		"} ORDER BY ?plot";
		// listQuery.add(new NatclinnQueryObject(titleQuery, typeQuery, stringQuery));

		// titleQuery = "Ratio biomasse des arbres éligibles au négoce Cat II/ biomasse totale par parcelle pour l'année 2017";
		// typeQuery = "SELECT";
		// stringQuery = prefix + "SELECT DISTINCT (?plot AS ?Parcelle) (str(?value) AS ?Ratio) " +
		// 		" WHERE {" +
		// 		"?plot res:hasBiomassBycategoryIIRatio2017 ?value." +
		// 		"} ORDER BY ?plot";
		// listQuery.add(new NatclinnQueryObject(titleQuery, typeQuery, stringQuery));


		// titleQuery = "Ratio pour le négoce de catégorie III";
		// typeQuery = "";
		// stringQuery = "";
		// listQuery.add(new NatclinnQueryObject(titleQuery, typeQuery, stringQuery));


		// titleQuery = "Ratio biomasse des arbres éligibles au négoce Cat III/ biomasse totale par parcelle pour l'année 2015";
		// typeQuery = "SELECT";
		// stringQuery = prefix + "SELECT DISTINCT (?plot AS ?Parcelle) (str(?value) AS ?Ratio) " +
		// 		" WHERE {" +
		// 		"?plot res:hasBiomassBycategoryIIIRatio2015 ?value." +
		// 		"} ORDER BY ?plot";
		// listQuery.add(new NatclinnQueryObject(titleQuery, typeQuery, stringQuery));

		// titleQuery = "Ratio biomasse des arbres éligibles au négoce Cat III / biomasse totale par parcelle pour l'année 2016";
		// typeQuery = "SELECT";
		// stringQuery = prefix + "SELECT DISTINCT (?plot AS ?Parcelle) (str(?value) AS ?Ratio) " +
		// 		" WHERE {" +
		// 		"?plot res:hasBiomassBycategoryIIIRatio2016 ?value." +
		// 		"} ORDER BY ?plot";
		// listQuery.add(new NatclinnQueryObject(titleQuery, typeQuery, stringQuery));

		// titleQuery = "Ratio biomasse des arbres éligibles au négoce Cat III / biomasse totale par parcelle pour l'année 2017";
		// typeQuery = "SELECT";
		// stringQuery = prefix + "SELECT DISTINCT (?plot AS ?Parcelle) (str(?value) AS ?Ratio) " +
		// 		" WHERE {" +
		// 		"?plot res:hasBiomassBycategoryIIIRatio2017 ?value." +
		// 		"} ORDER BY ?plot";
		// listQuery.add(new NatclinnQueryObject(titleQuery, typeQuery, stringQuery));


		// titleQuery = "ETUDES DU RENDEMENT DES CULTURES";
		// typeQuery = "";
		// stringQuery = "";
		// listQuery.add(new NatclinnQueryObject(titleQuery, typeQuery, stringQuery));

		// titleQuery = "Rendements des cultures";
		// typeQuery = "";
		// stringQuery = "";
		// listQuery.add(new NatclinnQueryObject(titleQuery, typeQuery, stringQuery));

		// titleQuery = "Rendement culture 2016";
		// typeQuery = "SELECT";
		// stringQuery = prefix + "SELECT DISTINCT (?sampling AS ?Echantillonnage)(?withNoTagSpeciesName AS ?NomCulture)(str(?value) AS ?Rendement)" +
	    //         "WHERE {?sampling rdf:type sosa:Sampling." +
		// 		"?sampling res:hasYieldPerHectareMean2016 ?value." +
		// 		"?sampling res:hasSpecies ?species." +
		// 		"?species skos:prefLabel ?speciesName." +
		// 		"FILTER (lang(?speciesName) = '" + UserLanguage + "')" +
		// 		"BIND (STR(?speciesName)  AS ?withNoTagSpeciesName)" +
		// 		"} ORDER BY ?sampling";
		// listQuery.add(new NatclinnQueryObject(titleQuery, typeQuery, stringQuery));

		// titleQuery = "Rendement culture 2017";
		// typeQuery = "SELECT";
		// stringQuery = prefix + "SELECT DISTINCT (?sampling AS ?Echantillonnage)(?withNoTagSpeciesName AS ?NomCulture)(str(?value) AS ?Rendement) WHERE {?sampling rdf:type sosa:Sampling." +
		// 		"?sampling res:hasYieldPerHectareMean2017 ?value." +
		// 		"?sampling res:hasSpecies ?species." +
		// 		"?species skos:prefLabel ?speciesName." +
		// 		"FILTER (lang(?speciesName) = '" + UserLanguage + "')" +
		// 		"BIND (STR(?speciesName)  AS ?withNoTagSpeciesName)" +
		// 		"} ORDER BY ?sampling";
		// listQuery.add(new NatclinnQueryObject(titleQuery, typeQuery, stringQuery));

		// titleQuery = "Rendement culture 2018";
		// typeQuery = "SELECT";
		// stringQuery = prefix + "SELECT DISTINCT (?sampling AS ?Echantillonnage)(?withNoTagSpeciesName AS ?NomCulture)(str(?value) AS ?Rendement) WHERE {?sampling rdf:type sosa:Sampling." +
		// 		"?sampling res:hasYieldPerHectareMean2018 ?value." +
		// 		"?sampling res:hasSpecies ?species." +
		// 		"?species skos:prefLabel ?speciesName." +
		// 		"FILTER (lang(?speciesName) = '" + UserLanguage + "')" +
		// 		"BIND (STR(?speciesName)  AS ?withNoTagSpeciesName)" +
		// 		"} ORDER BY ?sampling";
		// listQuery.add(new NatclinnQueryObject(titleQuery, typeQuery, stringQuery));

		// titleQuery = "Performance rendement culture 2016";
		// typeQuery = "SELECT";
		// stringQuery = prefix + "SELECT DISTINCT (?plot AS ?Parcelle) (str(?value) AS ?Ratio) WHERE {?plot res:isPlot ?true." +
		// 		"?plot res:hasYieldPerHectarePerformance2016 ?value" +
		// 		"} ORDER BY ?plot";
		// listQuery.add(new NatclinnQueryObject(titleQuery, typeQuery, stringQuery));

		// titleQuery = "Performance rendement culture 2017";
		// typeQuery = "SELECT";
		// stringQuery = prefix + "SELECT DISTINCT (?plot AS ?Parcelle) (str(?value) AS ?Ratio) WHERE {?plot res:isPlot ?true." +
		// 		"?plot res:hasYieldPerHectarePerformance2017 ?value" +
		// 		"} ORDER BY ?plot";
		// listQuery.add(new NatclinnQueryObject(titleQuery, typeQuery, stringQuery));

		// titleQuery = "Performance rendement culture 2018";
		// typeQuery = "SELECT";
		// stringQuery = prefix + "SELECT DISTINCT (?plot AS ?Parcelle) (str(round(?value)) AS ?Ratio) WHERE {?plot res:isPlot ?true." +
		// 		"?plot res:hasYieldPerHectarePerformance2018 ?value" +
		// 		"} ORDER BY ?plot";
		// listQuery.add(new NatclinnQueryObject(titleQuery, typeQuery, stringQuery));

		// titleQuery = "ETUDES PROXIMITE DU LEZ";
		// typeQuery = "";
		// stringQuery = "";
		// listQuery.add(new NatclinnQueryObject(titleQuery, typeQuery, stringQuery));

		// titleQuery = "Moyennes des distances au Lez par catégories";
		// typeQuery = "";
		// stringQuery = "";
		// listQuery.add(new NatclinnQueryObject(titleQuery, typeQuery, stringQuery));

		// titleQuery = "Moyennes des distances au Lez par catégories en 2015";
		// typeQuery = "SELECT";
		// stringQuery = prefix + "SELECT DISTINCT (?plot AS ?Parcelle) " +
		// 		"(str(?valueCat1) AS ?CategorieI )"+ 
		// 		"(str(?valueCat2) AS ?CategorieII )"+ 
		// 		"(str(?valueCat3) AS ?CategorieIII )"+ 
		// 		" WHERE {" +
		// 		"OPTIONAL {?plot res:hasDistanceTo_le_LezBycategoryIMean2015   ?valueCat1 }." +
		// 		"OPTIONAL {?plot res:hasDistanceTo_le_LezBycategoryIIMean2015  ?valueCat2 }." +
		// 		"OPTIONAL {?plot res:hasDistanceTo_le_LezBycategoryIIIMean2015 ?valueCat3 }." +
		// 		"} ORDER BY ?plot";
		// listQuery.add(new NatclinnQueryObject(titleQuery, typeQuery, stringQuery));
		
		// titleQuery = "Moyennes des distances au Lez par catégories en 2016";
		// typeQuery = "SELECT";
		// stringQuery = prefix + "SELECT DISTINCT (?plot AS ?Parcelle) " +
		// 		"(str(?valueCat1) AS ?CategorieI )"+ 
		// 		"(str(?valueCat2) AS ?CategorieII )"+ 
		// 		"(str(?valueCat3) AS ?CategorieIII )"+ 
		// 		" WHERE {" +
		// 		"OPTIONAL {?plot res:hasDistanceTo_le_LezBycategoryIMean2016   ?valueCat1 }." +
		// 		"OPTIONAL {?plot res:hasDistanceTo_le_LezBycategoryIIMean2016  ?valueCat2 }." +
		// 		"OPTIONAL {?plot res:hasDistanceTo_le_LezBycategoryIIIMean2016 ?valueCat3 }." +
		// 		"} ORDER BY ?plot";
		// listQuery.add(new NatclinnQueryObject(titleQuery, typeQuery, stringQuery));
		
		// titleQuery = "Moyennes des distances au Lez par catégories en 2017";
		// typeQuery = "SELECT";
		// stringQuery = prefix + "SELECT DISTINCT (?plot AS ?Parcelle) " +
		// 		"(str(?valueCat1) AS ?CategorieI )"+ 
		// 		"(str(?valueCat2) AS ?CategorieII )"+ 
		// 		"(str(?valueCat3) AS ?CategorieIII )"+ 
		// 		" WHERE {" +
		// 		"OPTIONAL {?plot res:hasDistanceTo_le_LezBycategoryIMean2017   ?valueCat1 }." +
		// 		"OPTIONAL {?plot res:hasDistanceTo_le_LezBycategoryIIMean2017  ?valueCat2 }." +
		// 		"OPTIONAL {?plot res:hasDistanceTo_le_LezBycategoryIIIMean2017 ?valueCat3 }." +
		// 		"} ORDER BY ?plot";
		// listQuery.add(new NatclinnQueryObject(titleQuery, typeQuery, stringQuery));
		
		/*
		 * titleQuery = "Test "; typeQuery = "SELECT"; stringQuery = prefix +
		 * "SELECT DISTINCT ?observation ?result" + " WHERE {" +
		 * "?observation sosa:hasFeatureOfInterest <http://www.afy.fr/Restinclieres/PB17AFL05A05>."
		 * + "?observation sosa:hasResult ?result." + "}  ORDER by ?observation ";
		 * listQuery.add(new NatclinnQueryObject(titleQuery, typeQuery,
		 * stringQuery));
		 * 
		 * titleQuery = "Test "; typeQuery = "SELECT"; stringQuery = prefix +
		 * "SELECT DISTINCT ?observation ?result" + " WHERE {" +
		 * "?observation sosa:hasFeatureOfInterest <http://www.afy.fr/Restinclieres/PA3AFL05A05>."
		 * + "?observation sosa:hasResult ?result." + "}  ORDER by ?observation ";
		 * listQuery.add(new NatclinnQueryObject(titleQuery, typeQuery,
		 * stringQuery));
		 * 
		 */
		// Pour mettre les queries dans un fichier JSON
		//Object to JSON in file
		// ObjectMapper objectMapper = new ObjectMapper();
		// objectMapper.writeValue(new File("C:\\var\\www\\natclinn\\queries\\queryNatclinn.json"), listQuery);
		
		Instant start0 = Instant.now();
		
		listRulesFileName.add("Natclinn.rules");
		listRulesFileName.add("Natclinn_additives.rules");
		listRulesFileName.add("Natclinn_packaging.rules");
		listPrimitives.add("CalcNumberOfTriples");
		listPrimitives.add("GetOFFProperty");
		listPrimitives.add("GetCiqualProperty");
		listPrimitives.add("GetIngredientRole");
		listPrimitives.add("CompareRoleProperty");
		listPrimitives.add("GetTypePackaging");
		listPrimitives.add("ComparePackagingTypeProperty");
		topSpatial = "false";
		
		// Récupération du nom du fichier contenant la liste des ontologies à traiter.
		Path pathOfTheListOntologies = Paths.get(NatclinnConf.mainFolderNatclinn, NatclinnConf.fileNameListOntologies);					
		// Récupération du nom des fichiers d'ontologies dans listOntologiesFileName
		listOntologiesFileName = new ArrayList<String>();	
		listOntologiesFileName = NatclinnUtil.makeListFileName(pathOfTheListOntologies.toString()); 

		InfModel infModel = NatclinnCreateInferredModelAndRunQueries.InferencesAndQueryWithModel(listOntologiesFileName, listRulesFileName, listPrimitives, topSpatial, listQuery);
		
		Instant end0 = Instant.now();
		System.out.println("Total running time : " + Duration.between(start0, end0).getSeconds() + " secondes");
		
		// Création des projets MyChoice à partir du modèle inféré
		CreateMychoiceProjectFromPreliminaryProject.createFromInferredModel(infModel);
	}  
}