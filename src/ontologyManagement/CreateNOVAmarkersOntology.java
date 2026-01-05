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
import java.util.Set;
import java.util.TreeMap;

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
import org.checkerframework.checker.units.qual.s;


/**
 * Crée une ontologie OWL à partir des marqueurs NOVA extraits d'Open Food Facts
 * Intègre le fichier Excel NOVA_markers_OFF.xlsx dans l'ontologie Natclinn
 */
public class CreateNOVAmarkersOntology {
	
	// Marqueurs à exclure par groupe : ingrédients de base qui ne sont pas des indicateurs de transformation
	// Vérifié via AnalyzeWaterInGroup4: ces ingrédients sont listés dans groupe 4 uniquement par corrélation
	// statistique (présents dans produits ultra-transformés) mais ne sont pas des marqueurs causals
	// Structure: Map<numéroGroupe, Set<marqueurasExclure>>
	// Permet de spécifier par groupe (ex: en:salt exclu uniquement du groupe 4, pas du groupe 1 ou 3)
	private static final Map<Integer, Set<String>> EXCLUDED_MARKERS_BY_GROUP = Map.ofEntries(
		Map.entry(1, Set.of(
			// Groupe 1: aliments peu ou non transformés
		)),
		Map.entry(2, Set.of(
			// Groupe 2: ingrédients culinaires transformés minimalement
		)),
		Map.entry(3, Set.of(
			// Groupe 3: aliments transformés
			// (pas d'exclusions pour ce groupe actuellement)
		)),
		Map.entry(4, Set.of(
			// Groupe 4: aliments ultratransformés - exclure les ingrédients de base
			"en:water",              // eau non transformée, présente dans tous les produits
			"en:salt",               // sel pur, ingrédient de base
			"en:beef",               // viande fraîche
			"en:pepper",             // poivre
			"en:cereal",             // céréales non transformées
			"en:vegetable",          // légumes frais génériques
			"en:wheat-flour",        // farine de blé transformée minimalement
			"en:olive-oil",          // huile d'olive vierge extra
			"en:potato-starch",      // amidon de pomme de terre
			"en:sunflower-oil",      // huile de tournesol vierge
			"en:wheat",              // blé entier ou grains de blé
			"en:egg-yolk",           // jaune d'œuf
			"en:milk",               // lait non transformé
			"en:carrot",             // carotte fraîche - NATURELLE
			"en:broad-bean",         // fève fraîche - NATURELLE
			"en:spinach",            // épinard - LÉGUME NATUREL
			"en:rice",               // riz grain - NATURELLE
			"en:potato",             // pomme de terre - TUBERCULE NATUREL
			"en:plant",              // plante générique - TROP VAGUE
			"en:oil-and-fat",        // PARENT GÉNÉRIQUE - force tous dérivés en G4
			"en:yeast",              // levure - Groupe 1 ou 2
			"en:chicken-egg-white",  // blanc d'œuf - Groupe 1
			"en:rice-flour",         // farine de riz - G3 max si non raffinée
			"en:rice-flakes",        // flocons de riz - G3 max
			// Huiles - discutables selon procédé de raffinage
			"en:walnut-oil",         // huile de noix
			"en:shea-oil",           // huile de karité
			"en:canola-oil",         // huile de colza (si non raffinée)
			"en:rice-bran-oil",      // huile de son de riz (si non raffinée)
			"en:peanut-oil",         // huile d'arachide (si non raffinée)
			"en:rapeseed-oil",       // huile de colza (si non raffinée)
			// Ingrédients discutables
			"en:cocoa-butter",       // beurre de cacao - G2 ou G3 selon extraction
			"en:natural-flavouring", // arôme naturel - N'EST PAS ultra-transformé
			"en:natural-cocoa",      // cacao naturel - devrait être G2 ou G3
			"en:emulsifier",         // générique - devrait pointer vers additifs spécifiques
			"en:gluten",             // gluten isolé - G3 si extrait
			"en:sunflower-protein",  // protéine de tournesol isolée
			"en:rapeseed-protein",   // protéine de colza isolée
			"en:firming-agent",      // raffermissant générique
			"en:sequestrant",        // séquestrant générique
			"en:vodka",              // alcool - G4 seulement si industrielle
			"en:honey",              // miel - DEVRAIT ÊTRE G2 ou G3, pas G4
			"en:pea-fiber",          // fibre de pois - à vérifier si isolat ou fibre brute
			"en:cream",
			"en:rice-starch",
			"en:colza-oil",
			"en:fruit-juice"
		))
	);
	
	// Futur candidats à exclure (à vérifier):
	// ingredients:en:vegetable
	// ingredients:en:egg-yolk
	// ingredients:en:pepper         OK 1 produit (groupe 4)
	// ingredients:en:sunflower-oil	 OK 52 produits (groupe 4)
	// ingredients:en:potato-starch  OK 26 produits (groupe 4)
	// ingredients:en:wheat		     OK 21 produits (groupe 4)
	// ingredients:en:olive-oil		 OK 1 produit (groupe 4)
	// ingredients:en:cereal         OK 1 produits (groupe 4)
	// ingredients:en:milk      	 OK 462 produits (groupe 4)


	
	// 	=== Aperçu des produits avec water en groupe 4 ===

	// Code: 2495070001701
	//   Produit: Schaschlik spezial mit Sauce
	//   Water types: ingredients
	//   Tous marqueurs groupe 4:
	//     - additives: en:e14xx
	//     - additives: en:e150d
	//     - additives: en:e14xx
	//     - additives: en:e150d
	//     - additives: en:e412
	//     - additives: en:e415
	//     - ingredients: en:colour
	//     - ingredients: en:dextrose
	//     - ingredients: en:flavouring
	//     - ingredients: en:glucose
	//     - ingredients: en:hydrogenated-oil
	//     - ingredients: en:thickener
	//     - ingredients: en:modified-starch
	//     - ingredients: en:water

	// Code: 7891515410315
	//   Produit: Claybom Cremosa com Sal - Margarina 50% de Gordura
	//   Water types: ingredients
	//   Tous marqueurs groupe 4:
	//     - additives: en:e471
	//     - ingredients: en:colour
	//     - ingredients: en:flavouring
	//     - ingredients: en:water

	// 	=== Aperçu des produits avec beef en groupe 4 ===

	// Code: 0045590247503
	//   Produit: unknown
	//   Beef types: ingredients
	//   Tous marqueurs groupe 4:
	//     - additives: en:e407
	//     - additives: en:e466
	//     - additives: en:e472b
	//     - additives: en:e941
	//     - additives: en:e950
	//     - additives: en:e955
	//     - ingredients: en:milk-proteins
	//     - ingredients: en:sweetener
	//     - ingredients: en:thickener
	//     - ingredients: en:beef

	// 	=== Produits trouvés avec salt en groupe 3: 148840 ===

	// === Aperçu des produits avec salt en groupe 3 ===

	// Code: 0000127534587
	// Produit: Lithuanian Rye Bread
	// Salt types: ingredients
	// Tous marqueurs groupe 3:
	// 	- ingredients: en:salt
	// 	- ingredients: en:sugar

	// Code: 0000197571406
	// Produit: Puppodums
	// Salt types: ingredients
	// Tous marqueurs groupe 3:
	// 	- ingredients: en:salt
	// 	- ingredients: en:vegetable-oil

	// Code: 0000433906023
	// Produit: Best Sweet-Potato Cookies
	// Salt types: ingredients
	// Tous marqueurs groupe 3:
	// 	- ingredients: en:butter
	// 	- ingredients: en:salt
	// 	- ingredients: en:sugar
	// 	- categories: en:sweet-snacks

	// Code: 0000433906030
	// Produit: Best Ginger Snap Cookies
	// Salt types: ingredients
	// Tous marqueurs groupe 3:
	// 	- ingredients: en:butter
	// 	- ingredients: en:salt
	// 	- ingredients: en:sugar

	// Code: 0000901000017
	// Produit: Yellow Corn Tortilla Chips
	// Salt types: ingredients
	// Tous marqueurs groupe 3:
	// 	- ingredients: en:salt
	// 	- ingredients: en:vegetable-oil

	// Code: 0000974300144
	// Produit: Caperberries In Vinegar
	// Salt types: ingredients
	// Tous marqueurs groupe 3:
	// 	- ingredients: en:salt

	// Code: 0000996284712
	// Produit: La Eur, 3 Milk Soft Ripened Cheese
	// Salt types: ingredients
	// Tous marqueurs groupe 3:
	// 	- categories: en:cheeses
	// 	- ingredients: en:salt
	// 	- ingredients: en:enzyme

	// Code: 0002000001780
	// Produit: Pizza Margherita
	// Salt types: ingredients
	// Tous marqueurs groupe 3:
	// 	- categories: en:meals
	// 	- ingredients: en:salt
	// 	- ingredients: en:starch
	// 	- ingredients: en:vegetable-oil
	// 	- ingredients: en:sauce
	// 	- ingredients: en:cheese

	// Code: 0002000010775
	// Produit: Escalope soja et blé
	// Salt types: ingredients
	// Tous marqueurs groupe 3:
	// 	- ingredients: en:salt
	// 	- ingredients: en:starch
	// 	- ingredients: en:sugar
	// 	- ingredients: en:vegetable-oil

	// Code: 0002124810206
	// Produit: Mediterranean Sea Salt
	// Salt types: ingredients
	// Tous marqueurs groupe 3:
	// 	- ingredients: en:salt

	// ... et 148830 autres produits

	// 	=== Produits trouvés avec wheatflour en groupe 4: 5 ===

	// Fichier Excel généré: C:\var\www\natclinn\data/WheatFlour_in_Group4_Analysis.xlsx

	// === Aperçu des produits avec wheatflour en groupe 4 ===
	// Code: 7616100756865
	//   Produit: cerelac HONEY & wheat
	//   WheatFlour types: ingredients
	//   Tous marqueurs groupe 4:
	//     - ingredients: en:flavouring
	//     - ingredients: en:whey
	//     - ingredients: en:wheat-flour

	// Code: 8410100181523
	//   Produit: Cerelac Infant Cereals With Milk Mixed fruits & wheat
	//   WheatFlour types: ingredients
	//   Tous marqueurs groupe 4:
	//     - additives: en:e322
	//     - ingredients: en:emulsifier
	//     - ingredients: en:flavouring
	//     - ingredients: en:maltodextrin
	//     - ingredients: en:wheat-flour

	// Code: 8801005198455
	//   Produit: Tteokbokki
	//   WheatFlour types: ingredients
	//   Tous marqueurs groupe 4:
	//     - additives: en:e14xx
	//     - additives: en:e414
	//     - additives: en:e955
	//     - ingredients: en:glucose
	//     - ingredients: en:gluten
	//     - ingredients: en:high-fructose-corn-syrup
	//     - ingredients: en:maltodextrin
	//     - ingredients: en:fructose
	//     - ingredients: en:modified-starch
	//     - ingredients: en:wheat-flour

	// Code: 0841112105623
	//   Produit: Spicy Chicken And Cheese, Spicy
	//   WheatFlour types: ingredients
	//   Tous marqueurs groupe 4:
	//     - additives: en:e471
	//     - additives: en:e472e
	//     - additives: en:e481
	//     - ingredients: en:dextrose
	//     - ingredients: en:flavouring
	//     - ingredients: en:glucose
	//     - ingredients: en:gluten
	//     - ingredients: en:maltodextrin
	//     - ingredients: en:modified-starch
	//     - ingredients: en:wheat-flour

	// Code: 4335619249493
	//   Produit: Vegan Nuggets
	//   WheatFlour types: ingredients
	//   Tous marqueurs groupe 4:
	//     - additives: en:e14xx
	//     - additives: en:e415
	//     - additives: en:e461
	//     - ingredients: en:maltodextrin
	//     - ingredients: en:thickener
	//     - ingredients: en:vegetable-fiber
	//     - ingredients: en:modified-starch
	//     - ingredients: en:soya-flour
	//     - ingredients: en:wheat-flour
	//     - ingredients: en:pea-fiber

	// 	=== Produits trouvés avec wheat en groupe 4: 21 ===

	// === Aperçu des produits avec wheat en groupe 4 ===

	// Code: 0024300041341
	//   Produit: Donut Sticks
	//   Wheat types: ingredients
	//   Tous marqueurs groupe 4:
	//     - additives: en:e171
	//     - additives: en:e322
	//     - additives: en:e406
	//     - additives: en:e415
	//     - additives: en:e450
	//     - additives: en:e471
	//     - ingredients: en:colour
	//     - ingredients: en:dextrose
	//     - ingredients: en:flavouring
	//     - ingredients: en:glucose
	//     - ingredients: en:whey
	//     - ingredients: en:wheat

	// Code: 0024300043345
	//   Produit: Glazed Donut Sticks
	//   Wheat types: ingredients
	//   Tous marqueurs groupe 4:
	//     - additives: en:e171
	//     - additives: en:e322
	//     - additives: en:e406
	//     - additives: en:e415
	//     - additives: en:e450
	//     - ingredients: en:colour
	//     - ingredients: en:dextrose
	//     - ingredients: en:emulsifier
	//     - ingredients: en:flavouring
	//     - ingredients: en:glucose
	//     - ingredients: en:whey
	//     - ingredients: en:wheat

	// Code: 0024300044670
	//   Produit: Mini Crunch Donuts
	//   Wheat types: ingredients
	//   Tous marqueurs groupe 4:
	//     - additives: en:e171
	//     - additives: en:e322
	//     - additives: en:e412
	//     - additives: en:e416
	//     - additives: en:e422
	//     - additives: en:e450
	//     - additives: en:e466
	//     - ingredients: en:colour
	//     - ingredients: en:dextrose
	//     - ingredients: en:emulsifier
	//     - ingredients: en:flavouring
	//     - ingredients: en:glucose
	//     - ingredients: en:high-fructose-corn-syrup
	//     - ingredients: en:whey
	//     - ingredients: en:fructose
	//     - ingredients: en:wheat

	// Code: 5060455517343
	//   Produit: Vegan Extreme Protein Chocolate Silk
	//   Wheat types: ingredients
	//   Tous marqueurs groupe 4:
	//     - additives: en:e450
	//     - additives: en:e957
	//     - ingredients: en:flavouring
	//     - ingredients: en:sweetener
	//     - ingredients: en:wheat

	// Code: 7613033536526
	//   Produit: Pensal farinha com cacau
	//   Wheat types: ingredients
	//   Tous marqueurs groupe 4:
	//     - additives: en:e170
	//     - ingredients: en:flavouring
	//     - ingredients: en:wheat

	// Code: 7622400950179
	//   Produit: Spaghetti Arrabbiata
	//   Wheat types: ingredients
	//   Tous marqueurs groupe 4:
	//     - additives: en:e14xx
	//     - ingredients: en:gluten
	//     - ingredients: en:modified-starch
	//     - ingredients: en:wheat

	// Code: 9310155530101
	//   Produit: Original Rice Crackers
	//   Wheat types: ingredients
	//   Tous marqueurs groupe 4:
	//     - additives: en:e627
	//     - additives: en:e631
	//     - ingredients: en:flavour-enhancer
	//     - ingredients: en:wheat

	// Code: 9310155540100
	//   Produit: Seaweed rice crackers
	//   Wheat types: ingredients
	//   Tous marqueurs groupe 4:
	//     - additives: en:e627
	//     - additives: en:e631
	//     - ingredients: en:flavour-enhancer
	//     - ingredients: en:flavouring
	//     - ingredients: en:wheat

	// Code: 0058336350306
	//   Produit: Giuseppe Pizzeria Thin Crust Pepperoni Pizza
	//   Wheat types: ingredients
	//   Tous marqueurs groupe 4:
	//     - ingredients: en:dextrose
	//     - ingredients: en:glucose
	//     - ingredients: en:wheat

	// Code: 9310155510103
	//   Produit: Barbeque Rice Crackers
	//   Wheat types: ingredients
	//   Tous marqueurs groupe 4:
	//     - additives: en:e160c
	//     - additives: en:e627
	//     - additives: en:e631
	//     - ingredients: en:colour
	//     - ingredients: en:flavour-enhancer
	//     - ingredients: en:flavouring
	//     - ingredients: en:wheat

	// ... et 11 autres produits

	// 	=== Produits trouvés avec oliveoil en groupe 4: 1 ===

	// === Aperçu des produits avec oliveoil en groupe 4 ===

	// Code: 5400113598770
	//   Produit: Carbonara
	//   OliveOil types: ingredients
	//   Tous marqueurs groupe 4:
	//     - ingredients: en:olive-oil


	// 	=== Produits trouvés avec potatostarch en groupe 4: 26 ===

	// === Aperçu des produits avec potatostarch en groupe 4 ===

	// Code: 0011110845894
	//   Produit: Crinkle Cut French Fried Potatoes
	//   PotatoStarch types: ingredients
	//   Tous marqueurs groupe 4:
	//     - additives: en:e1400
	//     - additives: en:e160b
	//     - additives: en:e415
	//     - additives: en:e450
	//     - ingredients: en:colour
	//     - ingredients: en:dextrose
	//     - ingredients: en:glucose
	//     - ingredients: en:modified-starch
	//     - ingredients: en:potato-starch
	//     - ingredients: en:tapioca

	// Code: 0041497087648
	//   Produit: Grade A Made With Real Sour Cream
	//   PotatoStarch types: ingredients
	//   Tous marqueurs groupe 4:
	//     - additives: en:e407
	//     - additives: en:e412
	//     - additives: en:e415
	//     - additives: en:e428
	//     - additives: en:e471
	//     - additives: en:e621
	//     - ingredients: en:flavouring
	//     - ingredients: en:maltodextrin
	//     - ingredients: en:modified-starch
	//     - ingredients: en:potato-starch

	// Code: 00673341
	//   Produit: Veggie Collin the Caterpillar
	//   PotatoStarch types: ingredients
	//   Tous marqueurs groupe 4:
	//     - additives: en:e901
	//     - ingredients: en:flavouring
	//     - ingredients: en:glazing-agent
	//     - ingredients: en:glucose
	//     - ingredients: en:glucose-syrup
	//     - ingredients: en:fructose
	//     - ingredients: en:potato-starch

	// Code: 0717854152099
	//   Produit: Pop'n chicken white chicken nugget patty fritters & fun formed mashed potatoes
	//   PotatoStarch types: ingredients
	//   Tous marqueurs groupe 4:
	//     - additives: en:e401
	//     - additives: en:e407
	//     - additives: en:e450
	//     - additives: en:e471
	//     - ingredients: en:dextrose
	//     - ingredients: en:flavouring
	//     - ingredients: en:glucose
	//     - ingredients: en:maltodextrin
	//     - ingredients: en:whey
	//     - ingredients: en:modified-starch
	//     - ingredients: en:potato-starch
	//     - ingredients: en:soya-flour

	// Code: 8431876121933
	//   Produit: Cocktail
	//   PotatoStarch types: ingredients
	//   Tous marqueurs groupe 4:
	//     - ingredients: en:colour
	//     - ingredients: en:flavouring
	//     - ingredients: en:potato-starch

	// Code: 7610491014351
	//   Produit: Bouillon de l�gumes instantan�
	//   PotatoStarch types: ingredients
	//   Tous marqueurs groupe 4:
	//     - ingredients: en:potato-starch

	// Code: 8414807502902
	//   Produit: Mega mix
	//   PotatoStarch types: ingredients
	//   Tous marqueurs groupe 4:
	//     - additives: en:e150d
	//     - additives: en:e160c
	//     - additives: en:e414
	//     - additives: en:e471
	//     - additives: en:e551
	//     - additives: en:e621
	//     - additives: en:e635
	//     - ingredients: en:colour
	//     - ingredients: en:emulsifier
	//     - ingredients: en:flavour-enhancer
	//     - ingredients: en:flavouring
	//     - ingredients: en:maltodextrin
	//     - ingredients: en:whey
	//     - ingredients: en:potato-starch

	// Code: 8001300670193
	//   Produit: Panzerotti pomodoro e mozzarella
	//   PotatoStarch types: ingredients
	//   Tous marqueurs groupe 4:
	//     - additives: en:e450
	//     - additives: en:e471
	//     - ingredients: en:emulsifier
	//     - ingredients: en:flavouring
	//     - ingredients: en:potato-starch

	// Code: 0075450016284
	//   Produit: Sweet potato fries french fried potatoes
	//   PotatoStarch types: ingredients
	//   Tous marqueurs groupe 4:
	//     - additives: en:e1400
	//     - additives: en:e415
	//     - additives: en:e450
	//     - ingredients: en:colour
	//     - ingredients: en:modified-starch
	//     - ingredients: en:vegetable-fiber
	//     - ingredients: en:potato-starch

	// Code: 4770190131605
	//   Produit: Gyoza de gambas
	//   PotatoStarch types: ingredients
	//   Tous marqueurs groupe 4:
	//     - ingredients: en:flavouring
	//     - ingredients: en:potato-starch

	// ... et 16 autres produits


	// 	=== Produits trouvés avec sunfloweroil en groupe 4: 52 ===

	// === Aperçu des produits avec sunfloweroil en groupe 4 ===

	// Code: 0200024015626
	//   Produit: Muesli mix
	//   SunflowerOil types: ingredients
	//   Tous marqueurs groupe 4:
	//     - ingredients: en:flavouring
	//     - ingredients: en:sunflower-oil

	// Code: 3229580005510
	//   Produit: La brioche des rois
	//   SunflowerOil types: ingredients
	//   Tous marqueurs groupe 4:
	//     - additives: en:e127
	//     - additives: en:e133
	//     - additives: en:e150a
	//     - ingredients: en:colour
	//     - ingredients: en:dextrose
	//     - ingredients: en:emulsifier
	//     - ingredients: en:flavouring
	//     - ingredients: en:glucose
	//     - ingredients: en:fructose
	//     - ingredients: en:sunflower-oil

	// Code: 3259426038938
	//   Produit: Galette Moelleuse Saveur Amande
	//   SunflowerOil types: ingredients
	//   Tous marqueurs groupe 4:
	//     - additives: en:e14xx
	//     - additives: en:e322
	//     - additives: en:e412
	//     - additives: en:e420
	//     - additives: en:e440
	//     - additives: en:e471
	//     - ingredients: en:dextrose
	//     - ingredients: en:emulsifier
	//     - ingredients: en:flavouring
	//     - ingredients: en:glucose
	//     - ingredients: en:glucose-syrup
	//     - ingredients: en:thickener
	//     - ingredients: en:fructose
	//     - ingredients: en:modified-starch
	//     - ingredients: en:sunflower-oil
	//     - ingredients: en:coconut-oil

	// Code: 3276550655008
	//   Produit: Madeleine saveur chocolat
	//   SunflowerOil types: ingredients
	//   Tous marqueurs groupe 4:
	//     - additives: en:e322
	//     - additives: en:e450
	//     - ingredients: en:emulsifier
	//     - ingredients: en:flavouring
	//     - ingredients: en:gluten
	//     - ingredients: en:invert-sugar
	//     - ingredients: en:sunflower-oil

	// Code: 3502790725127
	//   Produit: Canistrelli aux écorces d'oranges
	//   SunflowerOil types: ingredients
	//   Tous marqueurs groupe 4:
	//     - additives: en:e160
	//     - additives: en:e322
	//     - additives: en:e450
	//     - ingredients: en:colour
	//     - ingredients: en:emulsifier
	//     - ingredients: en:flavouring
	//     - ingredients: en:glucose
	//     - ingredients: en:fructose
	//     - ingredients: en:sunflower-oil

	// Code: 3502790725141
	//   Produit: Navettes Provençales arôme fleur d'oranger
	//   SunflowerOil types: ingredients
	//   Tous marqueurs groupe 4:
	//     - additives: en:e160
	//     - additives: en:e322
	//     - ingredients: en:colour
	//     - ingredients: en:emulsifier
	//     - ingredients: en:flavouring
	//     - ingredients: en:sunflower-oil

	// Code: 3502790725332
	//   Produit: Canistrelli orange
	//   SunflowerOil types: ingredients
	//   Tous marqueurs groupe 4:
	//     - additives: en:e160
	//     - additives: en:e322
	//     - additives: en:e450
	//     - additives: en:e471
	//     - ingredients: en:colour
	//     - ingredients: en:emulsifier
	//     - ingredients: en:flavouring
	//     - ingredients: en:glucose
	//     - ingredients: en:fructose
	//     - ingredients: en:sunflower-oil

	// Code: 3564700026342
	//   Produit: Biscuits apéritifs salés au bacon
	//   SunflowerOil types: ingredients
	//   Tous marqueurs groupe 4:
	//     - additives: en:e100
	//     - additives: en:e120
	//     - additives: en:e14xx
	//     - additives: en:e160b
	//     - additives: en:e160c
	//     - additives: en:e471
	//     - ingredients: en:colour
	//     - ingredients: en:emulsifier
	//     - ingredients: en:flavouring
	//     - ingredients: en:modified-starch
	//     - ingredients: en:sunflower-oil

	// Code: 3564700026359
	//   Produit: Biscuits Salés Tokapi Gouda, 85g
	//   SunflowerOil types: ingredients
	//   Tous marqueurs groupe 4:
	//     - additives: en:e100
	//     - additives: en:e14xx
	//     - additives: en:e160b
	//     - additives: en:e160c
	//     - additives: en:e471
	//     - ingredients: en:colour
	//     - ingredients: en:emulsifier
	//     - ingredients: en:flavouring
	//     - ingredients: en:modified-starch
	//     - ingredients: en:sunflower-oil

	// Code: 3700151861563
	//   Produit: Pave de lieu noir marine
	//   SunflowerOil types: ingredients
	//   Tous marqueurs groupe 4:
	//     - ingredients: en:flavouring
	//     - ingredients: en:sunflower-oil
	//     - ingredients: en:colza-oil

	// ... et 42 autres produits


	// 	=== Produits trouvés avec pepper en groupe 4: 1 ===

	// === Aperçu des produits avec pepper en groupe 4 ===

	// Code: 3700316212568
	//   Produit: Sauce au poivre
	//   Pepper types: ingredients
	//   Tous marqueurs groupe 4:
	//     - additives: en:e415
	//     - ingredients: en:flavouring
	//     - ingredients: en:pepper

	// === Produits trouvés avec cereal en groupe 4: 1 ===

	// Fichier Excel généré: C:\var\www\natclinn\data/Cereal_in_Group4_Analysis.xlsx

	// === Aperçu des produits avec cereal en groupe 4 ===
	// Code: 8445291137806
	//   Produit: GUIGOZ Croissance 5 Céréales 500ml dès 1 an
	//   Cereal types: ingredients
	//   Tous marqueurs groupe 4:
	//     - additives: en:e322
	//     - additives: en:e407
	//     - ingredients: en:emulsifier
	//     - ingredients: en:flavouring
	//     - ingredients: en:cereal
	// ...
	// 	=== Produits trouvés avec milk en groupe 4: 462 ===

	// Fichier Excel généré: C:\var\www\natclinn\data/Milk_in_Group4_Analysis.xlsx

	// === Aperçu des produits avec milk en groupe 4 ===
	// Code: 0036800413467
	//   Produit: Treat Bars
	//   Milk types: ingredients
	//   Tous marqueurs groupe 4:
	//     - additives: en:e965
	//     - ingredients: en:milk-proteins
	//     - ingredients: en:palm-kernel-oil
	//     - ingredients: en:milk

	// Code: 0055000697309
	//   Produit: Cappuccino Vanille
	//   Milk types: ingredients
	//   Tous marqueurs groupe 4:
	//     - additives: en:e452
	//     - ingredients: en:flavouring
	//     - ingredients: en:milk

	// Code: 0055577105313
	//   Produit: Harvest Crunch Granola Cereal Original
	//   Milk types: ingredients
	//   Tous marqueurs groupe 4:
	//     - ingredients: en:flavouring
	//     - ingredients: en:milk

	// Code: 0055577110218
	//   Produit: Barres tendres au quinoa chocolat et noix
	//   Milk types: ingredients
	//   Tous marqueurs groupe 4:
	//     - additives: en:e322
	//     - additives: en:e422
	//     - additives: en:e476
	//     - ingredients: en:flavouring
	//     - ingredients: en:glucose
	//     - ingredients: en:glucose-syrup
	//     - ingredients: en:invert-sugar
	//     - ingredients: en:palm-kernel-oil
	//     - ingredients: en:milk

	// Code: 0055577312551
	//   Produit: HARVEST CRUNCH ORIGINAL GRANOLA CEREAL
	//   Milk types: ingredients
	//   Tous marqueurs groupe 4:
	//     - ingredients: en:flavouring
	//     - ingredients: en:milk

	// Code: 0055653670308
	//   Produit: Breton basilic et huile d'olive
	//   Milk types: ingredients
	//   Tous marqueurs groupe 4:
	//     - ingredients: en:flavouring
	//     - ingredients: en:milk

	// Code: 0056800528015
	//   Produit: Creamy Strawberry
	//   Milk types: ingredients
	//   Tous marqueurs groupe 4:
	//     - additives: en:e440
	//     - ingredients: en:colour
	//     - ingredients: en:flavouring
	//     - ingredients: en:modified-starch
	//     - ingredients: en:milk

	// Code: 0056800528022
	//   Produit: Creamy Peach
	//   Milk types: ingredients
	//   Tous marqueurs groupe 4:
	//     - additives: en:e440
	//     - ingredients: en:colour
	//     - ingredients: en:flavouring
	//     - ingredients: en:modified-starch
	//     - ingredients: en:milk

	// Code: 0059800000116
	//   Produit: Aero
	//   Milk types: ingredients
	//   Tous marqueurs groupe 4:
	//     - additives: en:e322
	//     - additives: en:e476
	//     - ingredients: en:flavouring
	//     - ingredients: en:milk

	// Code: 0059800000215
	//   Produit: Wafer Bar
	//   Milk types: ingredients
	//   Tous marqueurs groupe 4:
	//     - additives: en:e322
	//     - additives: en:e476
	//     - ingredients: en:flavouring
	//     - ingredients: en:milk

	// ... et 452 autres produits
	public static void main(String[] args) {
		String jsonString = CreationABox();
		
		OntModel om = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
		
		RDFParser.create()
			.source(new StringReader(jsonString))
			.lang(Lang.JSONLD11)
			.parse(om);
		
		try {   
			//////////////////////////////
			// Sorties fichiers         //
			////////////////////////////// 

			FileOutputStream outStream = new FileOutputStream(NatclinnConf.folderForOntologies + "/NatclinnNOVAmarkers.xml");
			// exporte le resultat dans un fichier
			om.write(outStream, "RDF/XML");

			// N3 N-TRIPLE RDF/XML RDF/XML-ABBREV
			//om.write(System.out, "N3");

			outStream.close();
			System.out.println("Ontologie NOVA générée: " + NatclinnConf.folderForOntologies + "/NatclinnNOVAmarkers.xml");
			
		} catch (FileNotFoundException e) {
			System.out.println("File not found");
		} catch (IOException e) {
			System.out.println("IO problem");
		}
	}

	public static String CreationABox() {
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
		String skos = new String("http://www.w3.org/2004/02/skos/core#");
	    om.setNsPrefix("skos", skos); 
	    String rdfs = new String("http://www.w3.org/2000/01/rdf-schema#");
	    om.setNsPrefix("rdfs", rdfs);
		String rdf = new String("http://www.w3.org/1999/02/22-rdf-syntax-ns#");
	    om.setNsPrefix("rdf", rdf);
		String dc = new String("http://purl.org/dc/elements/1.1/");
	    om.setNsPrefix("dc", dc);

		/////////////////////////////////////
	    //Description de l'ontologie       //
	    /////////////////////////////////////

		Ontology ont = om.createOntology(ncl + "NOVAmarkers");
		om.add(ont, RDFS.label, "Ontology of NOVA Markers");
		om.add(ont, DC.description, "ABox for the NOVA markers from Open Food Facts");
		om.add(ont, DC.creator, "Raphaël CONDE SALAZAR");	

        // NCL est un ensemble de produits et d'arguments
	    OntClass NCL = om.createClass(ncl + "NCL");
	    NCL.addComment("NCL is the set of product and arguments.", "en");
		NCL.addComment("NCL est l'ensemble des produits et des arguments.", "fr");

		// NCLCA est un ensemble d'atributs de classification
	    OntClass NCLNM = om.createClass(ncl + "NCLNM");
		NCLNM.addComment("NCLNM is the set of NOVA markers.", "en");
		NCLNM.addComment("NCLNM est l'ensemble des marqueurs NOVA.", "fr");
	    NCLNM.addSuperClass(NCL);

		/////////////////////////////////////
	    // Classes de l'ontologie          //
	    /////////////////////////////////////

		// Classe pour la hiérarchie NOVA
	    OntClass NOVASystem = om.createClass(ncl + "NOVASystem");
	    NOVASystem.addLabel("NOVA Classification System", "en");
	    NOVASystem.addLabel("Système de classification NOVA", "fr");
	    NOVASystem.addComment("The NOVA food classification system developed by the University of São Paulo", "en");
	    NOVASystem.addComment("Le système de classification des aliments NOVA développé par l'Université de São Paulo", "fr");
        NOVASystem.addSuperClass(NCLNM);

		// Classe pour les groupes NOVA
	    OntClass NOVAGroup = om.createClass(ncl + "NOVAGroup");
	    NOVAGroup.addLabel("NOVA Group", "en");
	    NOVAGroup.addLabel("Groupe NOVA", "fr");
	    NOVAGroup.addComment("A group in the NOVA classification system", "en");
	    NOVAGroup.addComment("Un groupe du système de classification NOVA", "fr");
        NOVAGroup.addSuperClass(NCLNM);

		// Classe pour les marqueurs
	    OntClass NOVAMarker = om.createClass(ncl + "NOVAMarker");
	    NOVAMarker.addLabel("NOVA Marker", "en");
	    NOVAMarker.addLabel("Marqueur NOVA", "fr");
	    NOVAMarker.addComment("A marker or characteristic of a NOVA group", "en");
	    NOVAMarker.addComment("Un marqueur ou caractéristique d'un groupe NOVA", "fr");
        NOVAMarker.addSuperClass(NCLNM);

		/////////////////////////////////////
	    // Propriétés de l'ontologie       //
	    /////////////////////////////////////

		// Propriété : appartient au groupe
		ObjectProperty belongsToNOVAGroup = om.createObjectProperty(ncl + "belongsToNOVAGroup");
		belongsToNOVAGroup.addLabel("belongs to NOVA group", "en");
		belongsToNOVAGroup.addLabel("appartient au groupe NOVA", "fr");
		belongsToNOVAGroup.addDomain(NOVAMarker);
		belongsToNOVAGroup.addRange(NOVAGroup);

		// Propriété : type de marqueur
		DatatypeProperty markerType = om.createDatatypeProperty(ncl + "markerType");
		markerType.addLabel("marker type", "en");
		markerType.addLabel("type de marqueur", "fr");
		markerType.addDomain(NOVAMarker);

		// Propriété : valeur du marqueur
		DatatypeProperty markerValue = om.createDatatypeProperty(ncl + "markerValue");
		markerValue.addLabel("marker value", "en");
		markerValue.addLabel("valeur du marqueur", "fr");
		markerValue.addDomain(NOVAMarker);

		// Propriété : numéro du groupe NOVA
		DatatypeProperty groupNumber = om.createDatatypeProperty(ncl + "groupNumber");
		groupNumber.addLabel("group number", "en");
		groupNumber.addLabel("numéro du groupe", "fr");
		groupNumber.addDomain(NOVAGroup);

		//////////////////////////////
		// Création des individus   //
		//////////////////////////////

		// Créer les 4 groupes NOVA
		Map<Integer, Individual> novaGroups = new TreeMap<>();
		String[] groupLabels = {
			"Minimally processed or unprocessed foods / Aliments peu ou non transformés",
			"Cooking ingredients /  Ingrédients culinaires",
			"Processed foods  / Aliments transformés",
			"Ultra-processed foods / Aliments ultratransformés"
		};

		for (int i = 1; i <= 4; i++) {
			Individual group = om.createIndividual(ncl + "NOVAGroup_" + i, NOVAGroup);
			group.addProperty(groupNumber, om.createTypedLiteral(i));
			String[] labels = groupLabels[i - 1].split(" / ");
			group.addLabel(labels[0], "en");
			group.addLabel(labels[1], "fr");
			novaGroups.put(i, group);
		}

		//////////////////////////////
		// Lecture du fichier Excel  //
		//////////////////////////////

		try (FileInputStream fis = new FileInputStream(NatclinnConf.folderForData + "/NOVA_markers_OFF.xlsx");
             Workbook workbook = new XSSFWorkbook(fis)) {
			
			// Map pour stocker les marqueurs créés et éviter les doublons
			Map<String, Individual> markers = new HashMap<>();
			
			Sheet sheet = workbook.getSheet("NOVA Markers");
            if (sheet == null) {
                throw new IllegalArgumentException("Sheet 'NOVA Markers' not found in Excel file");
            }

			for (Row row : sheet) {
				// Ignorer l'en-tête
				if (row.getRowNum() == 0) continue;
				
				Integer groupNum = getNumericCellValue(row.getCell(0)).intValue();
				String markerTypeVal = getCellValue(row.getCell(1));
				String markerValueVal = getCellValue(row.getCell(2));

				if (markerTypeVal.isEmpty() || markerValueVal.isEmpty()) continue;

				// Spécifique: ne pas conserver en:enzyme en groupe 4 (conflit OFF groupe 3/4)
				if (groupNum == 4 && "en:enzyme".equals(markerValueVal)) {
					System.out.println("Filtré: Groupe " + groupNum + " | Marqueur exclu (enzyme groupe4): " + markerValueVal);
					continue;
				}

				// Appliquer le filtre des marqueurs non significatifs pour ce groupe spécifique
				Set<String> excludedForGroup = EXCLUDED_MARKERS_BY_GROUP.getOrDefault(groupNum, Set.of());
				if (excludedForGroup.contains(markerValueVal)) {
					System.out.println("Filtré: Groupe " + groupNum + " | Marqueur exclu pour ce groupe: " + markerValueVal);
					continue;
				}

				// System.out.println("Traitement: Groupe " + groupNum + " | Type: " + markerTypeVal + " | Valeur: " + markerValueVal);

				// Créer un URI unique pour le marqueur
				String markerUri = NatclinnUtil.makeURI(ncl + "Marker_Group" + groupNum + "_", 
					markerTypeVal + "_" + markerValueVal);

				// Créer le marqueur s'il n'existe pas déjà
				if (!markers.containsKey(markerUri)) {
					Individual marker = om.createIndividual(markerUri, NOVAMarker);
					marker.addProperty(markerType, markerTypeVal);
					marker.addProperty(markerValue, markerValueVal);
					marker.addProperty(belongsToNOVAGroup, novaGroups.get(groupNum));
					
					// Ajouter un label lisible
					String label = markerTypeVal + ": " + markerValueVal;
					marker.addLabel(label, "en");
					
					markers.put(markerUri, marker);
				}
			}

			System.out.println(markers.size() + " marqueurs NOVA créés");
			
		} catch (FileNotFoundException e) {
			System.out.println("File not found: " + NatclinnConf.folderForData + "/NOVA_markers_OFF.xlsx");
			e.printStackTrace();
	
		} catch (IOException e) {
            System.out.println("IO error reading Excel file");
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
    
		return jsonString;
	}

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

	private static Double getNumericCellValue(Cell cell) {
		if (cell == null) return 0.0;
		switch (cell.getCellType()) {
			case NUMERIC:
				return cell.getNumericCellValue();
			case STRING:
				try {
					return Double.parseDouble(cell.getStringCellValue().trim());
				} catch (NumberFormatException e) {
					return 0.0;
				}
			default:
				return 0.0;
		}
	}
}
