package inferencesAndQueries;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.fasterxml.jackson.databind.ObjectMapper;

import natclinn.util.NatclinnConf;
import natclinn.util.NatclinnQueryObject;
import natclinn.util.NatclinnUtil;

public class NatclinnQueryStatistics {

	public static void main(String[] args) throws Exception {

		// ============================================================
        // Vérification / définition de la propriété SIS_DATA
        // ============================================================

		// Initialisation de la configuration
		// Chemin d'accès, noms fichiers...
		new NatclinnConf();


		String UserLanguage = NatclinnConf.preferredLanguage;
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

		
		/////////////////////////////
		// Affichage des résultats //
		/////////////////////////////

		titleQuery = "NATCLINN - STATISTIQUES GLOBALES";
		typeQuery = "";
		stringQuery = "";
		listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));

		titleQuery = "Taille du_modèle après inférences (en triplets)";
		typeQuery = "SELECT";
		stringQuery = prefix + "SELECT (COUNT(*) AS ?Nombre_De_Triplets) WHERE { ?s ?p ?o. }";
		listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));

		titleQuery = "Instances de la classe Product ";
		typeQuery = "SELECT";
		stringQuery = prefix + "SELECT DISTINCT (?s AS ?Instance_Produit) WHERE { " +
			"?s ?p ?o." +
			"?s rdf:type ncl:Product." +
			"}";
		listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));

		titleQuery = "Instances avec le flag isProductIAA à true ";
		typeQuery = "SELECT";
		stringQuery = prefix + "SELECT DISTINCT (?s AS ?Instance_ProduitIAA) WHERE { " +
			"?s rdf:type ncl:Product. " +	
			"?s ncl:isProductIAA 'true'^^xsd:Boolean." +
			"}";
		listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));

		titleQuery = "Instances de la classe ProductIAA ";
		typeQuery = "SELECT";
		stringQuery = prefix + "SELECT DISTINCT (?product AS ?Instance_ProduitIAA) WHERE { " +
		"?product rdf:type ncl:Product. " +
		"FILTER NOT EXISTS { ?otherProduct ncl:hasComposedOf ?product } " +
		"}";
		listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));
		
		titleQuery = "Nombre de triplets (calcNumberOfTriples) ";
		typeQuery = "SELECT";
		stringQuery = prefix + "SELECT (?o AS ?numberOfTriples) WHERE { " +
			"?s ncl:numberOfTriples ?o." +
			"}";
		listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));


		titleQuery = "Test Résumé des fonctions dans un produit";
		typeQuery = "SELECT";
		stringQuery = prefix + "SELECT ?product (COUNT(DISTINCT ?ingredient) AS ?nbIngredients) " +
			"WHERE { " +
				"?product rdf:type ncl:Product . " +
				"?product ncl:hasIngredient ?ingredient . " +
			"} " +
			"GROUP BY ?product ";
		listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));

		titleQuery = "Test 2 Résumé des fonctions dans un produit";
		typeQuery = "SELECT";
		stringQuery = prefix + "SELECT ?ingredient ?label " +
			"WHERE { " +
				"?ingredient rdf:type ncl:Ingredient . " +
				"?ingredient skos:prefLabel ?label . " +
			"} " ;
		listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));


		titleQuery = "Résumé des fonctions dans un produit";
		typeQuery = "SELECT";
		stringQuery = prefix + "SELECT ?product (COUNT(DISTINCT ?ingredient) AS ?nbIngredients) " +
				"(GROUP_CONCAT(DISTINCT ?function; separator=\", \") AS ?functions) " +
			"WHERE { " +
				"?product rdf:type ncl:Product . " +
				"?product ncl:hasIngredient ?ingredient . " +
				"?ingredient ncl:hasFunction ?function . " +
			"} " +
			"GROUP BY ?product ";
		listQuery.add(new NatclinnQueryObject(titleQuery, commentQuery, typeQuery, stringQuery, idQuery));

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
		listRulesFileName.add("Natclinn_1.rules");
		listRulesFileName.add("Natclinn_additives.rules");
		listPrimitives.add("CalcNumberOfTriples");
		listPrimitives.add("GetOFFProperty");
		listPrimitives.add("GetCiqualProperty");
		listPrimitives.add("GetIngredientFunction");
		topSpatial = "true";
		
		// Récupération du nom du fichier contenant la liste des ontologies à traiter.
		Path pathOfTheListOntologies = Paths.get(NatclinnConf.mainFolderNatclinn, NatclinnConf.fileNameListOntologies);					
		// Récupération du nom des fichiers d'ontologies dans listOntologiesFileName
		listOntologiesFileName = new ArrayList<String>();	
		listOntologiesFileName = NatclinnUtil.makeListFileName(pathOfTheListOntologies.toString()); 

		NatclinnCreateInferredModelAndRunQueries.InferencesAndQuery(listOntologiesFileName, listRulesFileName, listPrimitives, topSpatial, listQuery);
		
		Instant end0 = Instant.now();
		System.out.println("Total running time : " + Duration.between(start0, end0).getSeconds() + " secondes");
	}  
}