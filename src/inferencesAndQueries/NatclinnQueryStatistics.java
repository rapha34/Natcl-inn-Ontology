package inferencesAndQueries;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.io.File;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFWriter;


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
		idQuery++;		
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

	/////////////////////////////////////////////////////



	// titleQuery = "Résumé des liens ";
	// commentQuery = "Afficher les valeurs de la propriété LinkToArgument par produits";
	// typeQuery = "SELECT";
	// stringQuery = prefix +
	// 	"SELECT ?product ?link " +
	// 	"WHERE { " +
	// 	"  ?product ncl:hasLinkToArgument ?link . " +
	// 	"} ORDER BY ?product";
	// listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
	// idQuery++;
	


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

	// 	titleQuery = "Vérif: alternatives du projet Madeleines";
	// 	commentQuery = "Liste des alternatives créées et leur produit lié";
	// 	typeQuery = "SELECT";
	// 	stringQuery = prefix +
	// 		"SELECT (?alt AS ?Alternative) (?prodName AS ?Produit) (?prodEAN AS ?EAN_Produit) WHERE { " +
	// 		"  ?alt mch:hasProject mch:Project-Madeleines . " +
	// 		"  ?alt mch:relatedToProduct ?prod . " +
	// 		"  ?prod skos:prefLabel ?prodName . " +
	// 	"  ?prod ncl:hasEAN13 ?prodEAN . " +	
	// 	"}";
	// listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
	// idQuery++;

	// titleQuery = "Vérif: alternatives du projet Moussakas";
	// 	commentQuery = "Liste des alternatives créées et leur produit lié";
	// 	typeQuery = "SELECT";
	// 	stringQuery = prefix +
	// 		"SELECT (?alt AS ?Alternative) (?prodName AS ?Produit) (?prodEAN AS ?EAN_Produit) WHERE { " +
	// 		"  ?alt mch:hasProject mch:Project-Moussakas . " +
	// 		"  ?alt mch:relatedToProduct ?prod . " +
	// 		"  ?prod skos:prefLabel ?prodName . " +
	// 		"  ?prod ncl:hasEAN13 ?prodEAN . " +	
	// 		"}";
	// 	listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
		
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

	// titleQuery = "Instances de la classe Product ";
	// 	typeQuery = "SELECT";
	// 	stringQuery = prefix + "SELECT DISTINCT (?s AS ?Instance_Produit) (?prodName AS ?Nom_Produit) WHERE { " +
	// 		"?s ?p ?o." +
	// 		"?s rdf:type ncl:Product." +
	// 	"?s skos:prefLabel ?prodName . " +
	// 	"}";
	// listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
	// idQuery++;

	titleQuery = "Instances avec le flag isProductIAA à true ";
		typeQuery = "SELECT";
		stringQuery = prefix + "SELECT DISTINCT (?s AS ?Instance_ProduitIAA) (?prodName AS ?Nom_Produit) WHERE { " +
			"?s rdf:type ncl:Product. " +	
			"?s ncl:isProductIAA 'true'^^xsd:boolean." +
		"?s skos:prefLabel ?prodName . " +
		"}";
	listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
	idQuery++;

	// titleQuery = "Tous les ingrédients d'un produit donné P-3178530410105";
	// commentQuery = "Utilise les chemins de propriétés SPARQL 1.1 pour une récursivité complète";
	// typeQuery = "SELECT";
	// stringQuery = prefix + 
	// 	"SELECT DISTINCT ?ingredientLabel " +
	// 	"(GROUP_CONCAT(DISTINCT ?tagLabel; separator=\", \") AS ?tags) " +
	// 	"WHERE { " +
	// 	"    VALUES ?targetProduct { <https://w3id.org/NCL/ontology/P-3178530410105> } " +
	// 	"    " +
	// 	// Naviguer récursivement : Product -> (composedOf)* -> Product -> hasIngredient -> Ingredient
	// 	// Naviguer récursivement : Product -> hasIngredient|composedOf -> Ingredient
	// 		"?targetProduct (ncl:hasIngredient|ncl:hasComposedOf)* ?ingredient ." +
	// 	"    " +
	// 	// S'assurer que c'est bien un ingrédient et récupérer son label
	// 	"    ?ingredient a ncl:Ingredient ; " +
	// 	"                skos:prefLabel ?ingredientLabel . " +
	// 	"    " +
	// 	// Optionnel : récupérer les rôles des ingrédients
	// 	"    OPTIONAL { ?ingredient ncl:hasRole ?tag . ?tag skos:prefLabel ?tagLabel . } " +
	// 	"} " +
	// 	"GROUP BY ?ingredient ?ingredientLabel " +
	// 	"ORDER BY ?ingredient";
	// 	// System.out.println(stringQuery);
	// listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
	// idQuery++;
	
	// titleQuery = "Tous le codes OFF des ingrédients d'un produit donné P-3178530410105";
	// commentQuery = "Utilise les chemins de propriétés SPARQL 1.1 pour une récursivité complète";
	// typeQuery = "SELECT";
	// stringQuery = prefix + 
	// 	"SELECT DISTINCT ?ingredientLabel " +
	// 	"(GROUP_CONCAT(DISTINCT ?iDOFF; separator=\", \") AS ?IDopenFoodFacts) " +
	// 	"WHERE { " +
	// 	"    VALUES ?targetProduct { <https://w3id.org/NCL/ontology/P-3178530410105> } " +
	// 	"    " +
	// 	// Naviguer récursivement : Product -> (composedOf)* -> Product -> hasIngredient -> Ingredient
	// 	// Naviguer récursivement : Product -> hasIngredient|composedOf -> Ingredient
	// 		"?targetProduct (ncl:hasIngredient|ncl:hasComposedOf)* ?ingredient ." +
	// 	"    " +
	// 	// S'assurer que c'est bien un ingrédient et récupérer son label
	// 	"    ?ingredient a ncl:Ingredient ; " +
	// 	"                skos:prefLabel ?ingredientLabel . " +
	// 	"    " +
	// 	// Optionnel : récupérer les rôles des ingrédients
	// 	"    OPTIONAL { ?ingredient ncl:hasIdIngredientOFF ?iDOFF. } " +
	// 	"} " +
	// 	"GROUP BY ?ingredient ?ingredientLabel " +
	// 	"ORDER BY ?ingredient";
	// 	// System.out.println(stringQuery);
	// listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
	// idQuery++;

	titleQuery = "Tous les tags d'un produit donné P-3178530410105";
	commentQuery = "Touts les tags associés au produit";
	typeQuery = "SELECT";
	stringQuery = prefix + 
		"SELECT DISTINCT ?tagLabel " +
		"WHERE { " +
		"    VALUES ?targetProduct { <https://w3id.org/NCL/ontology/P-3178530410105> } " +
		" ?targetProduct ncl:hasTag ?tag ." +
		" ?tag skos:prefLabel ?tagLabel . " + 
		"} " +
		"ORDER BY ?tagLabel ";
	listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
	idQuery++;

	titleQuery = "Tous les 'non' tags d'un produit donné P-3178530410105";
	commentQuery = "Touts les 'non' tags associés au produit";
	typeQuery = "SELECT";
	stringQuery = prefix + 
		"SELECT DISTINCT ?tagLabel " +
		"WHERE { " +
		"    VALUES ?targetProduct { <https://w3id.org/NCL/ontology/P-3178530410105> } " +
		" ?targetProduct ncl:hasTagCheck ?tag ." +
		" ?tag skos:prefLabel ?tagLabel . " +
		"} " +
		"ORDER BY ?tagLabel ";
	listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
	idQuery++;

	// titleQuery = "Vérification détail NOVA calculé";
	// 	typeQuery = "SELECT";
	// 	stringQuery = prefix + "SELECT DISTINCT (?s AS ?Instance_ProduitIAA) (?prodName AS ?Nom_Produit) (?uriDetails AS ?Calculed_Détails_NOVA) WHERE { " +
	// 		"?s rdf:type ncl:Product. " +	
	// 		"?s ncl:isProductIAA 'true'^^xsd:boolean." +
	// 		"?s skos:prefLabel ?prodName . " +
	// 		"?s ncl:hasCalculatedNOVAgroupDetails ?uriDetails . " +
	// 	"}";
	// listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
	// idQuery++;

	// titleQuery = "Tous les ingrédients avec leurs rôles associés pour un produit donné P-3564700423196";
	// commentQuery = "Utilise les chemins de propriétés SPARQL 1.1 pour une récursivité complète";
	// typeQuery = "SELECT";
	// stringQuery = prefix + 
	// 	"SELECT DISTINCT ?ingredientLabel " +
	// 	"(GROUP_CONCAT(DISTINCT ?roleLabel; separator=\", \") AS ?roles) " +
	// 	"WHERE { " +
	// 	"    VALUES ?targetProduct { <https://w3id.org/NCL/ontology/P-3564700423196> } " +
	// 	"    " +
	// 	// Naviguer récursivement : Product -> (composedOf)* -> Product -> hasIngredient -> Ingredient
	// 	// Naviguer récursivement : Product -> hasIngredient|composedOf -> Ingredient
	// 		"?targetProduct (ncl:hasIngredient|ncl:hasComposedOf)* ?ingredient ." +
	// 	"    " +
	// 	// S'assurer que c'est bien un ingrédient et récupérer son label
	// 	"    ?ingredient a ncl:Ingredient ; " +
	// 	"                skos:prefLabel ?ingredientLabel . " +
	// 	"    " +
	// 	// Optionnel : récupérer les rôles des ingrédients
	// 	"    OPTIONAL { ?ingredient ncl:hasRole ?role . ?role skos:prefLabel ?roleLabel . } " +
	// 	"} " +
	// 	"GROUP BY ?ingredient ?ingredientLabel " +
	// 	"ORDER BY ?ingredient";
	// 	// System.out.println(stringQuery);
	// listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
	// idQuery++;
	
	
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

	// 	titleQuery = "Résumé des rôles dans un produit par rapport aux ingrédients";
	// 	typeQuery = "SELECT";
	// 	stringQuery = prefix + "SELECT (?targetName AS ?Nom_Produit) (COUNT(DISTINCT ?ingredient) AS ?nbIngredients) " +
	// 			"(GROUP_CONCAT(DISTINCT ?roleLabel; separator=\", \") AS ?Role_additif) " +
	// 		"WHERE { " +
	// 			"?targetProduct rdf:type ncl:Product . " +
	// 			"?targetProduct skos:prefLabel ?targetName . " +
	// 			"?targetProduct ncl:isProductIAA 'true'^^xsd:boolean." +
	// 			// Naviguer récursivement : Product -> hasIngredient|hasComposedOf -> Ingredient
	// 			"?targetProduct (ncl:hasIngredient|ncl:hasComposedOf)* ?ingredient ." +
	// 			"    " +
	// 			// S'assurer que c'est bien un ingrédient et récupérer son label et sa role
	// 			"    ?ingredient a ncl:Ingredient ; " +
	// 			"                skos:prefLabel ?ingredientLabel ; " +
	// 			"                ncl:hasRole ?role . " +
	// 			"    ?role   skos:prefLabel ?roleLabel . " +
	// 			"    " +
	// 			// EXCLURE les CompositeIngredient
    //     		"FILTER NOT EXISTS { ?ingredient a ncl:CompositeIngredient } " +
	// 		"} " +
	// 	"GROUP BY ?targetName " ;
	// listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
	// idQuery++;


	// titleQuery = "Résumé des rôles dans un produit par rapport à l'absence d'ingrédients";
	// 	typeQuery = "SELECT";
	// 	stringQuery = prefix + "SELECT (?targetName AS ?Nom_Produit)  " +
	// 			"(GROUP_CONCAT(DISTINCT ?roleLabel; separator=\", \") AS ?Role_additif) " +
	// 		"WHERE { " +
	// 			"?targetProduct rdf:type ncl:Product . " +
	// 			"?targetProduct skos:prefLabel ?targetName . " +
	// 			"?targetProduct ncl:isProductIAA 'true'^^xsd:boolean . " +
	// 			"?targetProduct ncl:hasAdditiveRoleCheck ?role . " +
	// 			"?role skos:prefLabel ?roleLabel . " +
	// 		"} " +
	// 	"GROUP BY ?targetName " ;
	// listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
	// idQuery++;	


	// titleQuery = "Arguments disponibles (ncl:ProductArgument avec nameProperty)";
	// commentQuery = "Liste tous les arguments qui peuvent être liés aux rôles";
	// typeQuery = "SELECT";
	// stringQuery = prefix + "SELECT ?argument ?nameProperty WHERE { " +
	// 	"?argument rdf:type ncl:ProductArgument . " +
	// 	"?argument ncl:nameProperty ?nameProperty . " +
	// 	"} ORDER BY ?nameProperty";
	// listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
	// idQuery++;		
	
    // titleQuery = "Vérification des origines contrôlées sur les produits";
	// commentQuery = "Liste les produits avec leurs origines contrôlées";
	// typeQuery = "SELECT";
	// stringQuery = prefix + "SELECT (?product AS ?Produit) (?prodName AS ?Nom_Produit) " +
	// 	"(GROUP_CONCAT(DISTINCT ?controlledOriginLabelLabel; separator=\", \") AS ?Nom_Origine_Controlee) " +
	// 	"WHERE { " +
	// 	"?product rdf:type ncl:Product . " +
	// 	"?product skos:prefLabel ?prodName . " +
	// 	"?product ncl:hasControlledOriginLabel ?controlledOriginLabel . " +
	// 	"?controlledOriginLabel skos:prefLabel ?controlledOriginLabelLabel . " +
	// 	"} GROUP BY ?product ?prodName ORDER BY ?prodName";
	// listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
	// idQuery++;


	// titleQuery = "Vérification des types origines contrôlées détectées dans les produits";
	// commentQuery = "Liste les produits avec leurs origines contrôlées agrégées (containsControlledOriginType)";
	// typeQuery = "SELECT";
	// stringQuery = prefix + "SELECT (?product AS ?Produit) (?prodName AS ?Nom_Produit) " +
	// 	"(GROUP_CONCAT(DISTINCT ?originLabel; separator=\", \") AS ?Origines_Controlees) " +
	// 	"WHERE { " +
	// 	"?product rdf:type ncl:Product . " +
	// 	"?product skos:prefLabel ?prodName . " +
	// 	"?product ncl:containsControlledOriginType ?originType . " +
	// 	"?originType skos:prefLabel ?originLabel . " +
	// 	"} GROUP BY ?product ?prodName ORDER BY ?prodName";
	// listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
	// idQuery++;
	
	
	
	
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
		
		// titleQuery = "Produits liés à des arguments par inférence";
		// typeQuery = "SELECT";
		// stringQuery = prefix + 
		// 	"SELECT (?nameProduct AS ?Nom_Produit) (COUNT(?arg) AS ?nbArguments) " +
		// 	"(GROUP_CONCAT(DISTINCT ?argNameProperty; separator=\", \") AS ?arguments) " +
		// 	"WHERE { " +
		// 	"  ?product rdf:type ncl:Product . " +
		// 	"  ?product skos:prefLabel ?nameProduct . " +
		// 	"  ?product ncl:hasLinkToArgument ?link . " +
		// 	"  ?link ncl:hasReferenceProductArgument ?arg . " +
		// 	"  ?arg ncl:nameProperty ?argNameProperty . " +
		// 	"} " +
		// 	"GROUP BY ?product ?nameProduct " +
		// 	"ORDER BY DESC(?nbArguments)";
		// listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
		// idQuery++;

		// // --- NOVA Calculé (diagnostic) ---
		// titleQuery = "Produits avec NOVA calculé";
		// commentQuery = "Affiche le score NOVA calculé (ncl:hasCalculatedNOVAgroup) si présent pour les produits IAA";
		// typeQuery = "SELECT";
		// stringQuery = prefix +
		// 	"SELECT (?nameProduct AS ?Nom_Produit) ?score " +
		// 	"WHERE { " +
		// 	"  ?product rdf:type ncl:Product . " +
		// 	"  ?product ncl:isProductIAA 'true'^^xsd:boolean . " +
		// 	"  ?product skos:prefLabel ?nameProduct . " +
		// 	"  OPTIONAL { ?product ncl:hasCalculatedNOVAgroup ?score } " +
		// 	"} ORDER BY ?nameProduct";
		// listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
		// idQuery++;

// 	// --- Détails NOVA calculés (groupe et marqueurs) ---
// 	titleQuery = "Détails NOVA calculés par produit";
// 	commentQuery = "Affiche le groupe NOVA calculé et les marqueurs par groupe (hasNOVAmarker1..4) pour les produits IAA";
// 	typeQuery = "SELECT";
// 	stringQuery = prefix +
// 		"SELECT (?nameProduct AS ?Nom_Produit) " +
// 		"       (COALESCE(?novaCalcule, \"-\") AS ?NOVA_calculé) " +
// 		"       (COALESCE(GROUP_CONCAT(DISTINCT ?g1; separator=\", \"), \"-\") AS ?Groupe1_marqueurs) " +
// 		"       (COALESCE(GROUP_CONCAT(DISTINCT ?g2; separator=\", \"), \"-\") AS ?Groupe2_marqueurs) " +
// 		"       (COALESCE(GROUP_CONCAT(DISTINCT ?g3; separator=\", \"), \"-\") AS ?Groupe3_marqueurs) " +
// 		"       (COALESCE(GROUP_CONCAT(DISTINCT ?g4; separator=\", \"), \"-\") AS ?Groupe4_marqueurs) " +
// 		"WHERE { " +
// 		"  ?product rdf:type ncl:Product . " +
// 		"  ?product ncl:isProductIAA 'true'^^xsd:boolean . " +
// 		"  ?product skos:prefLabel ?nameProduct . " +
// 		"  OPTIONAL { ?product ncl:hasCalculatedNOVAgroup ?novaCalcule } " +
// 		"  OPTIONAL { ?product ncl:hasNOVAmarker1 ?g1 . FILTER(STRSTARTS(STR(?g1), \"Groupe1_\")) } " +
// 		"  OPTIONAL { ?product ncl:hasNOVAmarker2 ?g2 . FILTER(STRSTARTS(STR(?g2), \"Groupe2_\")) } " +
// 		"  OPTIONAL { ?product ncl:hasNOVAmarker3 ?g3 . FILTER(STRSTARTS(STR(?g3), \"Groupe3_\")) } " +
// 		"  OPTIONAL { ?product ncl:hasNOVAmarker4 ?g4 . FILTER(STRSTARTS(STR(?g4), \"Groupe4_\")) } " +
// 		"} " +
// 	"GROUP BY ?nameProduct ?novaCalcule " +
// 	"ORDER BY ?nameProduct";
// listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
// idQuery++;

	// // --- Contrôle détaillé NOVA : Calculé vs OFF pour tous les produits ---
	// titleQuery = "Contrôle NOVA détaillé : Calculé vs OFF (Tous les produits)";
	// commentQuery = "Compare les groupes NOVA calculés avec ceux natifs d'Open Food Facts pour tous les produits";
	// typeQuery = "SELECT";
	// stringQuery = prefix +
	// 	"SELECT ?product ?Nom_Produit ?NOVA_calculé ?NOVA_OFF " +
	// 	"       (COALESCE(?g1_calc, \"-\") AS ?Groupe1_calculé) " +
	// 	"       (COALESCE(?g2_calc, \"-\") AS ?Groupe2_calculé) " +
	// 	"       (COALESCE(?g3_calc, \"-\") AS ?Groupe3_calculé) " +
	// 	"       (COALESCE(?g4_calc, \"-\") AS ?Groupe4_calculé) " +
	// 	"       (COALESCE(?g1_off, \"-\") AS ?Groupe1_OFF) " +
	// 	"       (COALESCE(?g2_off, \"-\") AS ?Groupe2_OFF) " +
	// 	"       (COALESCE(?g3_off, \"-\") AS ?Groupe3_OFF) " +
	// 	"       (COALESCE(?g4_off, \"-\") AS ?Groupe4_OFF) " +
	// 	"WHERE { " +
	// 	"  ?product rdf:type ncl:Product . " +
	// 	"  ?product ncl:isProductIAA 'true'^^xsd:boolean." +
	// 	"  OPTIONAL { ?product skos:prefLabel ?Nom_Produit } . " +
	// 	"  OPTIONAL { ?product ncl:hasCalculatedNOVAgroup ?NOVA_calculé } " +
	// 	"  OPTIONAL { ?product ncl:hasNOVAgroup ?NOVA_OFF } " +
	// 	"  OPTIONAL { " +
	// 	"    ?product ncl:hasCalculatedNOVAgroupDetails ?details . " +
	// 	"    OPTIONAL { ?details ncl:groupe1 ?g1_calc } " +
	// 	"    OPTIONAL { ?details ncl:groupe2 ?g2_calc } " +
	// 	"    OPTIONAL { ?details ncl:groupe3 ?g3_calc } " +
	// 	"    OPTIONAL { ?details ncl:groupe4 ?g4_calc } " +
	// 	"  } " +
	// 	"  OPTIONAL { " +
	// 	"    ?product ncl:hasNOVAgroupDetails ?detailsOFF . " +
	// 	"    OPTIONAL { ?detailsOFF ncl:groupe1 ?g1_off } " +
	// 	"    OPTIONAL { ?detailsOFF ncl:groupe2 ?g2_off } " +
	// 	"    OPTIONAL { ?detailsOFF ncl:groupe3 ?g3_off } " +
	// 	"    OPTIONAL { ?detailsOFF ncl:groupe4 ?g4_off } " +
	// 	"  } " +
	// 	"} ORDER BY ?product";
	// listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
	// idQuery++;

	// // --- Audit détaillé : Différences NOVA Calculé vs OFF par marqueur ---
	// titleQuery = "Audit NOVA : Différences marqueurs par produit";
	// commentQuery = "Détail des marqueurs (ingrédients/additifs) présents en calculé vs OFF";
	// typeQuery = "SELECT";
	// stringQuery = prefix +
	// 	"SELECT ?product ?Nom_Produit ?NOVA_calc ?NOVA_off " +
	// 	"       ?groupe1_calc ?groupe2_calc ?groupe3_calc ?groupe4_calc " +
	// 	"       ?groupe1_off ?groupe2_off ?groupe3_off ?groupe4_off " +
	// 	"WHERE { " +
	// 	"  ?product rdf:type ncl:Product . " +
	// 	"  ?product ncl:isProductIAA 'true'^^xsd:boolean. " +
	// 	"  OPTIONAL { ?product skos:prefLabel ?Nom_Produit } " +
	// 	"  OPTIONAL { ?product ncl:hasCalculatedNOVAgroup ?NOVA_calc } " +
	// 	"  OPTIONAL { ?product ncl:hasNOVAgroup ?NOVA_off } " +
	// 	"  OPTIONAL { " +
	// 	"    ?product ncl:hasCalculatedNOVAgroupDetails ?detailsCalc . " +
	// 	"    OPTIONAL { ?detailsCalc ncl:groupe1 ?groupe1_calc } " +
	// 	"    OPTIONAL { ?detailsCalc ncl:groupe2 ?groupe2_calc } " +
	// 	"    OPTIONAL { ?detailsCalc ncl:groupe3 ?groupe3_calc } " +
	// 	"    OPTIONAL { ?detailsCalc ncl:groupe4 ?groupe4_calc } " +
	// 	"  } " +
	// 	"  OPTIONAL { " +
	// 	"    ?product ncl:hasNOVAgroupDetails ?detailsOFF . " +
	// 	"    OPTIONAL { ?detailsOFF ncl:groupe1 ?groupe1_off } " +
	// 	"    OPTIONAL { ?detailsOFF ncl:groupe2 ?groupe2_off } " +
	// 	"    OPTIONAL { ?detailsOFF ncl:groupe3 ?groupe3_off } " +
	// 	"    OPTIONAL { ?detailsOFF ncl:groupe4 ?groupe4_off } " +
	// 	"  } " +
	// 	"} ORDER BY ?product";
	// listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
	// idQuery++;

// titleQuery = "Diagnostic: détails NOVA présents ?";
// typeQuery = "SELECT";
// stringQuery = prefix +
//   "SELECT (COUNT(DISTINCT ?product) AS ?NbDetails) (COUNT(DISTINCT ?product) AS ?ProduitsAvecDetails) " +
//   "WHERE { ?product ncl:groupe1 ?g1 . }";
// listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
// idQuery++;// titleQuery = "Diagnostic: valeurs groupe1..4 existantes";
// typeQuery = "SELECT";
// stringQuery = prefix +
//   "SELECT ?product ?nameProduct ?g1 ?g2 ?g3 ?g4 " +
//   "WHERE { " +
//   "  ?product rdf:type ncl:Product . " +
//   "  OPTIONAL { ?product skos:prefLabel ?nameProduct } " +
//   "  OPTIONAL { ?product ncl:groupe1 ?g1 } " +
//   "  OPTIONAL { ?product ncl:groupe2 ?g2 } " +
//   "  OPTIONAL { ?product ncl:groupe3 ?g3 } " +
//   "  OPTIONAL { ?product ncl:groupe4 ?g4 } " +
//   "} ORDER BY ?product";
// listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
// idQuery++;

// titleQuery = "Diagnostic: ingrédients OFF présents dans les produits";
// commentQuery = "Liste tous les ingrédients avec leur hasIdIngredientOFF pour vérifier le matching avec NOVAMarker";
// typeQuery = "SELECT";
// stringQuery = prefix +
//   "SELECT ?product ?nameProduct ?ingredient ?ingredientLabel ?offId " +
//   "WHERE { " +
//   "  ?product rdf:type ncl:Product . " +
//   "  ?product ncl:isProductIAA 'true'^^xsd:boolean . " +
//   "  ?product skos:prefLabel ?nameProduct . " +
//   "  ?product (ncl:hasIngredient|ncl:hasComposedOf)* ?ingredient . " +
//   "  ?ingredient rdf:type ncl:Ingredient . " +
//   "  ?ingredient skos:prefLabel ?ingredientLabel . " +
//   "  ?ingredient ncl:hasIdIngredientOFF ?offId . " +
//   "} ORDER BY ?product ?ingredient LIMIT 50";
// listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
// idQuery++;

// titleQuery = "Diagnostic: marqueurs NOVA disponibles";
// commentQuery = "Liste quelques marqueurs NOVA avec leur markerValue pour vérifier la correspondance";
// typeQuery = "SELECT";
// stringQuery = prefix +
//   "SELECT ?marker ?markerLabel ?markerValue ?markerType ?groupNumber " +
//   "WHERE { " +
//   "  ?marker rdf:type ncl:NOVAMarker . " +
//   "  OPTIONAL { ?marker rdfs:label ?markerLabel } " +
//   "  ?marker ncl:markerValue ?markerValue . " +
//   "  ?marker ncl:markerType ?markerType . " +
//   "  ?marker ncl:belongsToNOVAGroup ?group . " +
//   "  ?group ncl:groupNumber ?groupNumber . " +
//   "} ORDER BY ?groupNumber ?markerType LIMIT 30";
// listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
// idQuery++;

// titleQuery = "Diagnostic: test match ingrédient ↔ marqueur NOVA";
// commentQuery = "Vérifie si des ingrédients OFF matchent des marqueurs NOVA (simulation de getNOVAgroup)";
// typeQuery = "SELECT";
// stringQuery = prefix +
//   "SELECT ?product ?nameProduct ?ingredient ?ingredientLabel ?offId ?markerValue ?markerType ?groupNumber " +
//   "WHERE { " +
//   "  ?product rdf:type ncl:Product . " +
//   "  ?product ncl:isProductIAA 'true'^^xsd:boolean . " +
//   "  ?product skos:prefLabel ?nameProduct . " +
//   "  ?product (ncl:hasIngredient|ncl:hasComposedOf)* ?ingredient . " +
//   "  ?ingredient rdf:type ncl:Ingredient . " +
//   "  ?ingredient skos:prefLabel ?ingredientLabel . " +
//   "  ?ingredient ncl:hasIdIngredientOFF ?offId . " +
//   "  ?marker rdf:type ncl:NOVAMarker . " +
//   "  ?marker ncl:markerValue ?markerValue . " +
//   "  ?marker ncl:markerType ?markerType . " +
//   "  ?marker ncl:belongsToNOVAGroup ?group . " +
//   "  ?group ncl:groupNumber ?groupNumber . " +
//   "  FILTER(CONTAINS(?offId, ?markerValue) || ?offId = ?markerValue) " +
//   "} ORDER BY ?product ?groupNumber LIMIT 50";
// listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
// idQuery++;

// titleQuery = "Diagnostic: hasNOVAmarkerInfo créé par Pass1 ?";
// commentQuery = "Vérifie si la règle Pass1 a bien appelé getNOVAgroup et créé hasNOVAmarkerInfo";
// typeQuery = "SELECT";
// stringQuery = prefix +
//   "SELECT ?ingredient ?ingredientLabel ?offId ?novaMarkerInfo " +
//   "WHERE { " +
//   "  ?ingredient rdf:type ncl:Ingredient . " +
//   "  ?ingredient skos:prefLabel ?ingredientLabel . " +
//   "  ?ingredient ncl:hasIdIngredientOFF ?offId . " +
//   "  OPTIONAL { ?ingredient ncl:hasNOVAmarkerInfo ?novaMarkerInfo } " +
//   "} ORDER BY ?ingredient LIMIT 50";
// listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
// idQuery++;

// titleQuery = "Diagnostic: hasIngredientR existe ?";
// commentQuery = "Vérifie si la propriété récursive hasIngredientR est bien matérialisée";
// typeQuery = "SELECT";
// stringQuery = prefix +
//   "SELECT ?product ?nameProduct (COUNT(?ingredient) AS ?nbIngredients) " +
//   "WHERE { " +
//   "  ?product rdf:type ncl:Product . " +
//   "  ?product ncl:isProductIAA 'true'^^xsd:boolean . " +
//   "  ?product skos:prefLabel ?nameProduct . " +
//   "  OPTIONAL { ?product ncl:hasIngredientR ?ingredient } " +
//   "} GROUP BY ?product ?nameProduct ORDER BY ?product";
// listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
// idQuery++;


// titleQuery = "Diagnostic: triplets groupe4 créés par Pass2 ?";
// commentQuery = "Compte les triplets (?product ncl:hasNOVAmarker4 ?value) créés par les règles Pass2 contenant Groupe4_";
// typeQuery = "SELECT";
// stringQuery = prefix +
//   "SELECT ?product (GROUP_CONCAT(DISTINCT ?g4; SEPARATOR=', ') AS ?all_g4) (COUNT(?g4) AS ?count_g4) " +
//   "WHERE { " +
//   "  ?product rdf:type ncl:Product . " +
//   "  ?product ncl:hasNOVAmarker4 ?g4 . " +
//   "  FILTER(STRSTARTS(STR(?g4), \"Groupe4_\")) " +
//   "} GROUP BY ?product";
// listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
// idQuery++;



		/////////////////////////////////////////////////////
		// CONTRÔLES EMBALLAGES                            //
		/////////////////////////////////////////////////////

		// titleQuery = "Types d'emballages détectés";
		// commentQuery = "Liste tous les types d'emballages identifiés par la primitive getPackagingType (compte les produits distincts)";
		// typeQuery = "SELECT";
		// stringQuery = prefix + 
		// 	"SELECT DISTINCT ?packagingType (COUNT(DISTINCT ?product) AS ?nbProducts) " +
		// 	"WHERE { " +
		// 	"  ?product ncl:hasTypePackaging ?packagingType . " +
		// 	"} " +
		// 	"GROUP BY ?packagingType " +
		// 	"ORDER BY DESC(?nbProducts)";
		// listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
		// idQuery++;

		// titleQuery = "Nombre d'emballages par produit";
		// commentQuery = "Montre combien de matériaux d'emballage différents chaque produit possède";
		// typeQuery = "SELECT";
		// stringQuery = prefix + 
		// 	"SELECT ?product ?nameProduct (COUNT(?packaging) AS ?nbPackagings) " +
		// 	"(GROUP_CONCAT(DISTINCT ?materialLabel; separator=\", \") AS ?materials) " +
		// 	"WHERE { " +
		// 	"  ?product rdf:type ncl:Product . " +
		// 	"  ?product ncl:hasPackaging ?packaging . " +
		// 	"  ?packaging ncl:hasMaterial ?material . " +
		// 	"  OPTIONAL { ?product skos:prefLabel ?nameProduct } " +
		// 	"  OPTIONAL { ?material skos:prefLabel ?materialLabel } " +
		// 	"} " +
		// 	"GROUP BY ?product ?nameProduct " +
		// 	"ORDER BY DESC(?nbPackagings)";
		// listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
		// idQuery++;

		// titleQuery = "Produits avec emballage plastique";
		// commentQuery = "Liste les produits contenant du plastique dans leur emballage";
		// typeQuery = "SELECT";
		// stringQuery = prefix + 
		// 	"SELECT ?product ?nameProduct " +
		// 	"WHERE { " +
		// 	"  ?product rdf:type ncl:Product . " +
		// 	"  ?product ncl:hasTypePackaging ncl:emballage_plastique . " +
		// 	"  OPTIONAL { ?product skos:prefLabel ?nameProduct } " +
		// 	"} " +
		// 	"ORDER BY ?nameProduct";
		// listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
		// idQuery++;

		// titleQuery = "Produits sans plastique";
		// commentQuery = "Liste les produits ayant reçu l'annotation ncl:sans_plastique";
		// typeQuery = "SELECT";
		// stringQuery = prefix + 
		// 	"SELECT ?product ?nameProduct " +
		// 	"WHERE { " +
		// 	"  ?product rdf:type ncl:Product . " +
		// 	"  ?product ncl:hasPackagingCheck ncl:sans_plastique . " +
		// 	"  OPTIONAL { ?product skos:prefLabel ?nameProduct } " +
		// 	"} " +
		// 	"ORDER BY ?nameProduct";
		// listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
		// idQuery++;

		// titleQuery = "Produits avec emballage naturel";
		// commentQuery = "Produits avec emballages naturels (verre, carton, bois, métal)";
		// typeQuery = "SELECT";
		// stringQuery = prefix + 
		// 	"SELECT ?product ?nameProduct (GROUP_CONCAT(DISTINCT ?packagingType; separator=\", \") AS ?types) " +
		// 	"WHERE { " +
		// 	"  ?product rdf:type ncl:Product . " +
		// 	"  ?product ncl:hasPackagingCheck ncl:emballage_naturel . " +
		// 	"  OPTIONAL { ?product skos:prefLabel ?nameProduct } " +
		// 	"  OPTIONAL { ?product ncl:hasTypePackaging ?packagingType } " +
		// 	"} " +
		// 	"GROUP BY ?product ?nameProduct " +
		// 	"ORDER BY ?nameProduct";
		// listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
		// idQuery++;

		// titleQuery = "Produits avec emballage biodégradable/compostable";
		// commentQuery = "Produits avec emballages biodégradables, compostables ou biosourcés";
		// typeQuery = "SELECT";
		// stringQuery = prefix + 
		// 	"SELECT ?product ?nameProduct ?checkType " +
		// 	"WHERE { " +
		// 	"  ?product rdf:type ncl:Product . " +
		// 	"  ?product ncl:hasPackagingCheck ?checkType . " +
		// 	"  OPTIONAL { ?product skos:prefLabel ?nameProduct } " +
		// 	"  FILTER(?checkType IN (ncl:emballage_biodegradable, ncl:emballage_compostable, ncl:emballage_biosource)) " +
		// 	"} " +
		// 	"ORDER BY ?nameProduct ?checkType";
		// listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
		// idQuery++;

		// titleQuery = "Produits sans emballage (vrac)";
		// commentQuery = "Produits n'ayant aucun emballage détecté";
		// typeQuery = "SELECT";
		// stringQuery = prefix + 
		// 	"SELECT ?product ?nameProduct " +
		// 	"WHERE { " +
		// 	"  ?product rdf:type ncl:Product . " +
		// 	"  ?product ncl:hasPackagingCheck ncl:sans_emballage . " +
		// 	"  OPTIONAL { ?product skos:prefLabel ?nameProduct } " +
		// 	"} " +
		// 	"ORDER BY ?nameProduct";
		// listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
		// idQuery++;

		// titleQuery = "Produits avec emballage composite";
		// commentQuery = "Produits avec emballages composites (multi-matériaux, difficiles à recycler)";
		// typeQuery = "SELECT";
		// stringQuery = prefix + 
		// 	"SELECT ?product ?nameProduct " +
		// 	"WHERE { " +
		// 	"  ?product rdf:type ncl:Product . " +
		// 	"  ?product ncl:hasPackagingCheck ncl:emballage_composite . " +
		// 	"  OPTIONAL { ?product skos:prefLabel ?nameProduct } " +
		// 	"} " +
		// 	"ORDER BY ?nameProduct";
		// listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
		// idQuery++;

		// titleQuery = "Détail des emballages par produit";
		// commentQuery = "Affiche tous les matériaux d'emballage et leurs types pour chaque produit";
		// typeQuery = "SELECT";
		// stringQuery = prefix + 
		// 	"SELECT ?product ?nameProduct ?packaging ?material ?materialLabel ?packagingType " +
		// 	"WHERE { " +
		// 	"  ?product rdf:type ncl:Product . " +
		// 	"  ?product ncl:hasPackaging ?packaging . " +
		// 	"  ?packaging ncl:hasMaterial ?material . " +
		// 	"  OPTIONAL { ?product skos:prefLabel ?nameProduct } " +
		// 	"  OPTIONAL { ?material skos:prefLabel ?materialLabel } " +
		// 	"  OPTIONAL { ?packaging ncl:hasTypePackaging ?packagingType } " +
		// 	"} " +
		// 	"ORDER BY ?nameProduct ?materialLabel";
		// listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
		// idQuery++;

		// titleQuery = "Statistiques types d'emballages par catégorie";
		// commentQuery = "Compte les occurrences de chaque type d'emballage détecté";
		// typeQuery = "SELECT";
		// stringQuery = prefix + 
		// 	"SELECT ?packagingType (COUNT(DISTINCT ?product) AS ?nbProducts) " +
		// 	"WHERE { " +
		// 	"  ?product rdf:type ncl:Product . " +
		// 	"  ?product ncl:hasTypePackaging ?packagingType . " +
		// 	"} " +
		// 	"GROUP BY ?packagingType " +
		// 	"ORDER BY DESC(?nbProducts)";
		// listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
		// idQuery++;


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

		// // ===================================================================
		// // Contrôle : Catégories intermédiaires rattachées aux produits
		// // ===================================================================
		// titleQuery = "Contrôle - Catégories intermédiaires par produit";
		// commentQuery = "Vérifie que les catégories intermédiaires sont correctement rattachées entre root et leaf (avec label).";
		// typeQuery = "SELECT";
		// stringQuery = prefix +
		// 	"SELECT ?product ?root ?intermediary ?interLabelFinal ?leaf " +
		// 	"WHERE { " +
		// 	"  ?product rdf:type ncl:Product . " +
		// 	"  ?product ncl:hasRootCategory ?root . " +
		// 	"  ?product ncl:hasLeafCategory ?leaf . " +
		// 	"  ?product ncl:hasIntermediaryCategory ?intermediary . " +
		// 	"  OPTIONAL { ?intermediary skos:prefLabel ?interLabel } " +
		// 	"  OPTIONAL { ?intermediary rdfs:label ?interLabelR } " +
		// 	"  BIND(COALESCE(?interLabel, ?interLabelR, REPLACE(STRAFTER(STR(?intermediary), \"TaxoCategory_\"), \"%3A\", \" :\")) AS ?tmp) " +
		// 	"  BIND(STR(?tmp) AS ?interLabelFinal) " +
		// 	"} " +
		// 	"LIMIT 20";
		// listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
		// idQuery++;

		// // Contrôle: nombre de produits avec root+leaf
		// titleQuery = "Contrôle - Nb produits (root+leaf)";
		// commentQuery = "Compter les produits ayant à la fois hasRootCategory et hasLeafCategory";
		// typeQuery = "SELECT";
		// stringQuery = prefix +
		// 	"SELECT (COUNT(*) AS ?nb) WHERE { " +
		// 	"  ?p rdf:type ncl:Product . " +
		// 	"  ?p ncl:hasRootCategory ?r . " +
		// 	"  ?p ncl:hasLeafCategory ?l . " +
		// 	"}";
		// listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
		// idQuery++;

		// Diagnostic: marqueurs NOVA des ingrédients
		// titleQuery = "Diagnostic - Marqueurs NOVA ingrédients (P-3564700423196)";
		// commentQuery = "Vérifie les marqueurs NOVA attachés aux ingrédients de la Moussaka";
		// typeQuery = "SELECT";
		// stringQuery = prefix +
		// 	"SELECT ?ingredient ?ingredientLabel ?novaInfo " +
		// 	"WHERE { " +
		// 	"  <https://w3id.org/NCL/ontology/P-3564700423196> ncl:hasIngredientR ?ingredient . " +
		// 	"  ?ingredient skos:prefLabel ?ingredientLabel . " +
		// 	"  OPTIONAL { ?ingredient ncl:hasNOVAmarkerInfo ?novaInfo } " +
		// 	"} ORDER BY ?ingredient";
		// listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
		// idQuery++;

		// // Contrôle: Ingrédients sans code OFF
		// titleQuery = "Contrôle - Ingrédients sans code OFF";
		// commentQuery = "Liste les ingrédients qui n'ont pas de relation hasIdIngredientOFF";
		// typeQuery = "SELECT";
		// stringQuery = prefix +
		// 	"SELECT ?ingredient ?ingredientLabel ?product ?productLabel " +
		// 	"WHERE { " +
		// 	"  ?ingredient rdf:type ncl:Ingredient . " +
		// 	"  OPTIONAL { ?ingredient skos:prefLabel ?ingredientLabel } " +
		// 	"  OPTIONAL { " +
		// 	"    ?product ncl:hasIngredientR ?ingredient . " +
		// 	"    OPTIONAL { ?product rdfs:label ?productLabel } " +
		// 	"  } " +
		// 	"  FILTER NOT EXISTS { ?ingredient ncl:hasIdIngredientOFF ?offId } " +
		// 	"} ORDER BY ?ingredientLabel";
		// listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
		// idQuery++;

		// // Test: Fuzzy matching des ingrédients sans code OFF
		// // NOTE: Le fuzzy matching fonctionne maintenant via la règle Pass2_MapMissingOFFIngredient_Fuzzy
		// // qui utilise le Builtin findIngredientOFFByLabel (voir natclinn.rules)
		// titleQuery = "Test - Fuzzy matching ingrédients (FindIngredientInTaxonomy)";
		// commentQuery = "Teste la primitive FindIngredientInTaxonomy pour matcher les labels d'ingrédients à la taxonomie";
		// typeQuery = "SELECT";
		// stringQuery = prefix +
		// 	"SELECT ?ingredient ?ingredientLabel ?matchedUri ?matchedLabel " +
		// 	"WHERE { " +
		// 	"  ?ingredient rdf:type ncl:Ingredient . " +
		// 	"  ?ingredient skos:prefLabel ?ingredientLabel . " +
		// 	"  FILTER NOT EXISTS { ?ingredient ncl:hasIdIngredientOFF ?offId } " +
		// 	"  BIND(natclinn:findIngredientMatch(?ingredientLabel) AS ?matchUri) " +
		// 	"  OPTIONAL { " +
		// 	"    ?matchedUri rdf:type ncl:TaxonomyIngredient . " +
		// 	"    ?matchedUri skos:prefLabel ?matchedLabel . " +
		// 	"    FILTER(STR(?matchUri) != '' && ?matchUri = STR(?matchedUri)) " +
		// 	"  } " +
		// 	"} LIMIT 100";
		// listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
		// idQuery++;

		// // Diagnostic: investigation starch et vegetable-oil manquants
		// titleQuery = "Diagnostic - Investigation starch & vegetable-oil";
		// commentQuery = "Recherche pourquoi starch et vegetable-oil ne sont pas détectés comme marqueurs NOVA";
		// typeQuery = "SELECT";
		// stringQuery = prefix +
		// 	"SELECT ?ingredient ?ingredientLabel ?offId ?novaMarker ?markerValue ?markerType ?groupNum ?taxonomyIng ?parentIng ?parentOffId " +
		// 	"WHERE { " +
		// 	"  <https://w3id.org/NCL/ontology/P-3250392814908> ncl:hasIngredientR ?ingredient . " +
		// 	"  ?ingredient skos:prefLabel ?ingredientLabel . " +
		// 	"  ?ingredient ncl:hasIdIngredientOFF ?offId . " +
		// 	"  FILTER(CONTAINS(?offId, 'starch') || CONTAINS(?offId, 'vegetable-oil')) " +
		// 	"  OPTIONAL { " +
		// 	"    ?novaMarker rdf:type ncl:NOVAMarker . " +
		// 	"    ?novaMarker ncl:markerValue ?markerValue . " +
		// 	"    ?novaMarker ncl:markerType ?markerType . " +
		// 	"    ?novaMarker ncl:belongsToNOVAGroup ?group . " +
		// 	"    ?group ncl:groupNumber ?groupNum . " +
		// 	"    FILTER(?offId = ?markerValue || CONTAINS(?offId, ?markerValue)) " +
		// 	"  } " +
		// 	"  OPTIONAL { " +
		// 	"    ?taxonomyIng rdf:type ncl:TaxonomyIngredient . " +
		// 	"    ?taxonomyIng ncl:offIngredientId ?offId . " +
		// 	"    OPTIONAL { " +
		// 	"      ?taxonomyIng ncl:hasParentIngredient ?parentIng . " +
		// 	"      ?parentIng ncl:offIngredientId ?parentOffId . " +
		// 	"    } " +
		// 	"  } " +
		// 	"}";
		// listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
		// idQuery++;

		// // Diagnostic: Liste complète des ingrédients de la Moussaka
		// titleQuery = "Diagnostic - Tous ingrédients Moussaka avec offId";
		// commentQuery = "Liste complète pour identifier les différences avec OFF";
		// typeQuery = "SELECT";
		// stringQuery = prefix +
		// 	"SELECT ?ingredient ?ingredientLabel ?offId " +
		// 	"WHERE { " +
		// 	"  <https://w3id.org/NCL/ontology/P-3250392814908> ncl:hasIngredientR ?ingredient . " +
		// 	"  ?ingredient skos:prefLabel ?ingredientLabel . " +
		// 	"  ?ingredient ncl:hasIdIngredientOFF ?offId . " +
		// 	"} ORDER BY ?ingredientLabel";
		// listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
		// idQuery++;

		// // Contrôle: arcs de hiérarchie OFF présents
		// titleQuery = "Contrôle - Nb arcs hasChildIngredient (OFF)";
		// commentQuery = "Vérifie que la taxonomie OFF est bien chargée (hiérarchie)";
		// typeQuery = "SELECT";
		// stringQuery = prefix +
		// 	"SELECT (COUNT(*) AS ?nb) WHERE { ?x <https://w3id.org/NCL/ontology/hasChildIngredient> ?y }";
		// listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
		// idQuery++;

		// // Contrôle: arcs de hiérarchie OFF catégories présents
		// titleQuery = "Contrôle - Nb arcs hasChildCategory (OFF)";
		// commentQuery = "Vérifie que la taxonomie OFF catégories est bien chargée (hiérarchie)";
		// typeQuery = "SELECT";
		// stringQuery = prefix +
		// 	"SELECT (COUNT(*) AS ?nb) WHERE { ?x <https://w3id.org/NCL/ontology/hasChildCategory> ?y }";
		// listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
		// idQuery++;
		

		// // ===================================================================
		// // Contrôle : Processing
		// // ===================================================================
		// titleQuery = "Contrôle - Processing par produit";
		// commentQuery = "Vérifie .";
		// typeQuery = "SELECT";
		// stringQuery = prefix +
		// 	"SELECT ?productName ?CalculatedNOVAgroup ?processingName " +
		// 	"WHERE { " +
		// 	"  ?product rdf:type ncl:Product . " +
		// 	"  ?product skos:prefLabel ?productName . " +
		// 	"  ?product ncl:hasCalculatedNOVAgroup ?CalculatedNOVAgroup . " +
		// 	"  ?product ncl:hasProcessing ?processing . " +
		// 	"  ?processing skos:prefLabel ?processingName . " +
		// 	"} " +
		// 	"LIMIT 20";
		// listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
		// idQuery++;


		

		// // ===================================================================
		// // Contrôle : Propriétés d'emballage par produit IAA
		// // ===================================================================
		// titleQuery = "Contrôle - Propriétés d'emballage par produit IAA";
		// commentQuery = "Affiche pour chaque produit IAA les valeurs de hasTag et hasTagCheck";
		// typeQuery = "SELECT";
		// stringQuery = prefix +
		// 	"SELECT DISTINCT ?productName ?tagUri ?tagCheckUri " +
		// 	"WHERE { " +
		// 	"  ?product rdf:type ncl:Product . " +
		// 	"  ?product ncl:hasEAN13 ?ean13 . " +
		// 	"  VALUES ?ean13 { \"3178530410105\"  } " +
		// 	"  ?product skos:prefLabel ?productName . " +
		// 	"  ?product ncl:isProductIAA 'true'^^xsd:boolean." +
		// 	"  OPTIONAL { " +
		// 	"    ?product ncl:hasTag ?tagUri . " +
		// 	"  } " +
		// 	"  OPTIONAL { " +
		// 	"    ?product ncl:hasTagCheck ?tagCheckUri . " +
		// 	"  } " +
		// 	"} " +
		// 	"ORDER BY ?productName";
		// listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
		// idQuery++;



			// // ===================================================================
			// // Contrôle : Propriétés d'emballage par produit IAA
			// // ===================================================================
			// titleQuery = "Contrôle - Propriétés d'emballage par produit IAA";
			// commentQuery = "Affiche pour chaque produit IAA les valeurs de hasTag et hasTagCheck";
			// typeQuery = "SELECT";
			// stringQuery = prefix +
			// 	"SELECT ?productName ?tag ?tagCheck " +
			// 	"WHERE { " +
			// 	"  ?product rdf:type ncl:Product . " +
			// 	"  ?product skos:prefLabel ?productName . " +
			// 	"  ?product ncl:isProductIAA 'true'^^xsd:boolean." +
			// 	"  OPTIONAL { " +
			// 	"    ?product ncl:hasTag ?tagUri . " +
			// 	"    ?tagUri skos:prefLabel ?tag . " +
			// 	"  } " +
			// 	"  OPTIONAL { " +
			// 	"    ?product ncl:hasTagCheck ?tagCheckUri . " +
			// 	"    ?tagCheckUri skos:prefLabel ?tagCheck . " +
			// 	"  } " +
			// 	"} " +
			// 	"ORDER BY ?productName";
			// listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
			// idQuery++;


		titleQuery = "Résumé des liens par initiator";
		commentQuery = "Compter les LinkToArgument par type d'initiateur";
		typeQuery = "SELECT";
		stringQuery = prefix +
			"SELECT ?tagLabel (COUNT(?link) AS ?nbLiens) " +
			"WHERE { " +
			"  ?product ncl:hasLinkToArgument ?link . " +
			"  ?link ncl:hasTagInitiator ?tag . " +
				"  OPTIONAL { ?tag skos:prefLabel ?tagLabel } " +
			"} GROUP BY ?tagLabel ORDER BY DESC(?nbLiens)";
		listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
		idQuery++;
	

		// ===================================================================
		// Contrôle : Tags
		// ===================================================================
		titleQuery = "Contrôle - Tags";
		commentQuery = "Affiche les tags pour le type Packaging";
		typeQuery = "SELECT";
		stringQuery = prefix +
			"SELECT DISTINCT ?tag ?tagType ?tagLabel " +
			"WHERE { " +
			"  ?tag rdf:type ncl:Tag . " +
			"  OPTIONAL { ?tag skos:prefLabel ?tagLabel . } " +
			"  OPTIONAL { ?tag ncl:tagType ?tagType . } " +
			"  FILTER (!bound(?tagType) || ?tagType = \"Packaging\") " +
			"} " +
			"ORDER BY ?tagType ?tagLabel";
		listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
		idQuery++;

		// ===================================================================
		// Contrôle : Tags processing
		// ===================================================================
		titleQuery = "Contrôle - Tags";
		commentQuery = "Affiche les tags pour le type ProcessingDegree";
		typeQuery = "SELECT";
		stringQuery = prefix +
			"SELECT DISTINCT ?tag ?tagType ?tagLabel " +
			"WHERE { " +
			"  ?tag rdf:type ncl:Tag . " +
			"  OPTIONAL { ?tag skos:prefLabel ?tagLabel . } " +
			"  OPTIONAL { ?tag ncl:tagType ?tagType . } " +
			"  FILTER (!bound(?tagType) || ?tagType = \"ProcessingDegree\") " +
			"} " +
			"ORDER BY ?tagType ?tagLabel";
		listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
		idQuery++;


		// ===================================================================
		// Contrôle : Bindings pour emballage_plastique
		// ===================================================================
		titleQuery = "Contrôle - Bindings pour emballage_plastique";
		commentQuery = "Affiche tous les PackagingTypeArgumentBinding liés à emballage_plastique";
		typeQuery = "SELECT";
		stringQuery = prefix +
			"SELECT ?binding ?nameProperty ?keywords " +
			"WHERE { " +
			"  ?binding rdf:type ncl:TagArgumentBinding . " +
			"  ?binding ncl:aboutTag ncl:emballage_plastique . " +
			"  OPTIONAL { ?binding ncl:tagNameProperty ?nameProperty } " +
			"  OPTIONAL { ?binding ncl:tagBindingKeywords ?keywords } " +
			"} " +
			"ORDER BY ?binding";
		listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
		idQuery++;


		// ===================================================================
		// Contrôle : Bindings pour emballage_sans_plastique
		// ===================================================================
		titleQuery = "Contrôle - Bindings pour emballage_sans_plastique";
		commentQuery = "Affiche tous les PackagingTypeArgumentBinding liés à emballage_sans_plastique";
		typeQuery = "SELECT";
		stringQuery = prefix +
			"SELECT ?binding ?nameProperty ?keywords " +
			"WHERE { " +
			"  ?binding rdf:type ncl:PackagingTypeArgumentBinding . " +
			"  ?binding ncl:aboutTag ncl:emballage_sans_plastique . " +
			"  OPTIONAL { ?binding ncl:bindingAgentNameProperty ?nameProperty } " +
			"  OPTIONAL { ?binding ncl:bindingAgentKeywords ?keywords } " +
			"} " +
			"ORDER BY ?binding";
		// ===================================================================
		// Contrôle : Arguments liés aux madeleines
		// ===================================================================
		titleQuery = "Contrôle - Arguments liés aux madeleines";
		commentQuery = "Affiche les arguments liés aux produits madeleines via LinkToArgument";
		typeQuery = "SELECT";
		stringQuery = prefix +
			"SELECT ?productName ?argumentName ?tagLabel " +
			"WHERE { " +
			"  ?product rdf:type ncl:Product . " +
			"  ?product skos:prefLabel ?productName . " +
			"  FILTER(CONTAINS(?productName, \"Madeleine\")) . " +
			"  ?product ncl:hasLinkToArgument ?link . " +
			"  ?link ncl:hasReferenceProductArgument ?argument . " +
			"  ?link ncl:hasTagInitiator ?tag . " +
			"  OPTIONAL { ?tag skos:prefLabel ?tagLabel } " +
			"  ?argument skos:prefLabel ?argumentName . " +
			"} " +
			"ORDER BY ?productName ?argumentName";
		listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
		idQuery++;

		// ===================================================================
		// Contrôle : Arguments liés aux moussakas
		// ===================================================================
		titleQuery = "Contrôle - Arguments liés aux moussakas";
		commentQuery = "Affiche les arguments liés aux produits moussakas via LinkToArgument";
		typeQuery = "SELECT";
		stringQuery = prefix +
			"SELECT ?productName ?argumentName ?tagLabel " +
			"WHERE { " +
			"  ?product rdf:type ncl:Product . " +
			"  ?product skos:prefLabel ?productName . " +
			"  FILTER(CONTAINS(?productName, \"Moussaka\")) . " +
			"  ?product ncl:hasLinkToArgument ?link . " +
			"  ?link ncl:hasReferenceProductArgument ?argument . " +
			"  ?link ncl:hasTagInitiator ?tag . " +
			"  OPTIONAL { ?tag skos:prefLabel ?tagLabel } " +
			"  ?argument skos:prefLabel ?argumentName . " +
			"} " +
			"ORDER BY ?productName ?argumentName";
		listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
		idQuery++;


		/////////////////////////////////////////////////////
		// FIN DES QUERIES                                 //
		/////////////////////////////////////////////////////
		
		// Pour mettre les queries dans un fichier JSON
		//Object to JSON in file
		// ObjectMapper objectMapper = new ObjectMapper();
		// objectMapper.writeValue(new File("C:\\var\\www\\natclinn\\queries\\queryNatclinn.json"), listQuery);
		
		Instant start0 = Instant.now();
		
		listRulesFileName.add("Natclinn.rules");
		listRulesFileName.add("Natclinn_additives.rules");
		listRulesFileName.add("Natclinn_packaging.rules");
		listRulesFileName.add("Natclinn_processing.rules");
		listRulesFileName.add("Natclinn_controlled_origin.rules");
		listRulesFileName.add("Natclinn_categories.rules");
		listRulesFileName.add("Natclinn_link_Product_To_Argument.rules");
		listPrimitives.add("CalcNumberOfTriples");
		listPrimitives.add("GetOFFProperty");
		listPrimitives.add("GetCiqualProperty");
		listPrimitives.add("GetIngredientRole");
		listPrimitives.add("GetPackagingType");
		//listPrimitives.add("ComparePackagingTypeProperty");
		//listPrimitives.add("CompareAdditiveRoleProperty");
		// listPrimitives.add("CompareProcessingDegreeProperty");
		// listPrimitives.add("CompareControlledOriginTypeProperty");
		listPrimitives.add("GetControlledOriginType");
		listPrimitives.add("GetNOVAgroup");
		listPrimitives.add("GetNOVAgroupInfoToProduct");
		listPrimitives.add("GetCategoriesToProduct");
		listPrimitives.add("GetNOVAgroupToProduct");
		// Primitive rules Builtin to map missing OFF codes by fuzzy matching
		listPrimitives.add("FindIngredientOFFByLabel");
		listPrimitives.add("CreateLinkProductToArgument");
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

		// Enregistrement du modèle inféré dans un fichier OWL avec diagnostics
		try {
			// 1) Vérifs dossier de sortie
			File outDir = new File(NatclinnConf.folderForOntologies);
			if (!outDir.exists()) {
				boolean created = outDir.mkdirs();
				System.out.println("Création du dossier d'ontologies: " + outDir + " => " + created);
			}

			// 2) Stats rapides sur le modèle
			long tripleCount = -1;
			try {
				tripleCount = infModel.size();
				System.out.println("Taille du modèle inféré (triplets): " + tripleCount);
			} catch (Throwable t) {
				System.out.println("Impossible d'obtenir la taille du modèle (size()).");
			}

			Runtime rt = Runtime.getRuntime();
			long maxMem = rt.maxMemory() / (1024 * 1024);
			long totalMem = rt.totalMemory() / (1024 * 1024);
			long freeMem = rt.freeMemory() / (1024 * 1024);
			System.out.println("Mémoire (Mo) => max:" + maxMem + ", total:" + totalMem + ", libre:" + freeMem);

			// 3) (Nettoyé) – suppression du smoke test d'écriture d'échantillon

			// 4) Écriture complète en RDF/XML simplifié (plus rapide que pretty)
			String inferedOntologyPath = NatclinnConf.folderForOntologies + File.separator + "NatclinnInferedOntology.owl";
			System.out.println("Écriture complète en RDF/XML (plain) vers: " + inferedOntologyPath);
			Instant writeStart = Instant.now();
			try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(inferedOntologyPath), 8192 * 4)) {
			org.apache.jena.riot.RDFDataMgr.write(bos, infModel, RDFFormat.RDFXML_PLAIN);
			}
			System.out.println("Écriture complète terminée en " + Duration.between(writeStart, Instant.now()).toSeconds() + "s");
			System.out.println("Modèle inféré enregistré dans: " + inferedOntologyPath);
		} catch (Exception e) {
			System.err.println("Erreur lors de l'enregistrement du modèle inféré:");
			e.printStackTrace();
		}
		
	}  
}