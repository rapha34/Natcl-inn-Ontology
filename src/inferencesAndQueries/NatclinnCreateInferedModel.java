package inferencesAndQueries;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.ReasonerRegistry;
import org.apache.jena.reasoner.rulesys.Builtin;
import org.apache.jena.reasoner.rulesys.BuiltinRegistry;
import org.apache.jena.reasoner.rulesys.GenericRuleReasoner;
import org.apache.jena.reasoner.rulesys.Rule;

import io.github.galbiston.geosparql_jena.configuration.GeoSPARQLConfig;
import natclinn.util.*;

/**
 * Classe de création d'un modèle inféré à partir d'ontologies, de règles et de primitives personnalisées.
 * Utilise Apache Jena et les règles de type GenericRuleReasoner.
 */
public class NatclinnCreateInferedModel { 

    /**
     * Crée un modèle inféré à partir d'une liste d'ontologies, de règles et de primitives.
     *
     * @param listOntologiesFileName Liste des noms d'ontologies à charger depuis le TDB
     * @param listRulesFileName Liste des fichiers de règles (.rules)
     * @param listPrimitives Liste des primitives personnalisées à enregistrer
     * @param topSpatial Indique si GeoSPARQL doit être chargé ("true" / "false")
     * @return Un modèle inféré (InfModel)
     * @throws Exception si une étape échoue (chargement ou règles absentes)
     */
    public static InfModel createInferedModel(ArrayList<String> listOntologiesFileName,
                                              ArrayList<String> listRulesFileName,
                                              ArrayList<String> listPrimitives,
                                              String topSpatial) throws Exception {

        new NatclinnConf(); // Initialise les constantes et chemins

        Instant startLoad = Instant.now();

        // Chargement des ontologies depuis le TDB
		System.out.println("=== Chargement des ontologies ===");
		Map<String, Model> ontologiesModels = new HashMap<>();

		for (String nameOntology : listOntologiesFileName) {
			String graphName = nameOntology.replaceFirst("[.][^.]+$", "");

			Dataset dataset = null;
			try {
				dataset = TDBUtil.CreateTDBDataset();
				dataset.begin(ReadWrite.READ);
				Model model = ModelFactory.createDefaultModel()
						.add(dataset.getNamedModel(graphName));
				ontologiesModels.put(graphName, model);
				dataset.commit();
				System.out.println("Ontologie chargée : " + graphName);
			} catch (Exception e) {
				System.err.println("Erreur lors du chargement de l'ontologie : " + nameOntology);
				e.printStackTrace();
			} finally {
				if (dataset != null) {
					try {
						dataset.end();   // Termine proprement la transaction
					} catch (Exception ignored) {}
					dataset.close();    // Libère les ressources du Dataset
				}
			}
		}

        // Fusion des modèles en un seul modèle temporaire
        Model modelTemp = ModelFactory.createDefaultModel();
        Model modelImports = ModelFactory.createDefaultModel();

        // Chargement optionnel de GeoSPARQL
        if (topSpatial.equalsIgnoreCase("true")) {
            modelImports.read(NatclinnConf.geo);
            // modelImports.removeAll(
            //     modelImports.getResource("http://www.opengis.net/ont/geosparql"),
            //     modelImports.getProperty("http://purl.org/dc/elements/1.1/source"),
            //     null
            // );
            GeoSPARQLConfig.setupMemoryIndex();
            System.out.println("Ontologie GeoSPARQL chargée et configurée");
			// System.out.println("VBOX_MSI_INSTALL_PATH = "  + System.getenv("VBOX_MSI_INSTALL_PATH"));
			// System.out.println("SIS_DATA = "  + System.getenv("SIS_DATA"));
			// Pour définir la variable d’environnement avant le lancement
			// Pour supprimer définitivement le message, il faut que le système d’exploitation connaisse SIS_DATA avant le démarrage de Java.
			// Sous Windows :
			// Ouvre le menu Démarrer puis tape variables d’environnement.
			// Clique sur Modifier les variables d’environnement du système.
			// Clique sur Nouveau...
			// Nom : SIS_DATA
			// Valeur : C:\var\natclinn\sis_data (ou un dossier existant, par ex. C:\temp)
			// Clique sur OK partout et redémarre ton IDE / terminal.
			// Sous Linux/macOS :
			// Ajoute dans ~/.bashrc ou ~/.zshrc :
        }

        // Fusion finale
        for (Model m : ontologiesModels.values()) {
            modelTemp.add(m);
            m.close();
        }
        modelTemp.add(modelImports);
        modelImports.close();

        Instant endLoad = Instant.now();
        System.out.println("Durée chargement modèles : " + Duration.between(startLoad, endLoad).toMillis() + " ms");

        // ---------------------------------------------------
        // Étape 2 : Enregistrement des primitives
        // ---------------------------------------------------
        Instant startPrimitives = Instant.now();
        registerBuiltins(listPrimitives);
        Instant endPrimitives = Instant.now();
        System.out.println("Durée enregistrement primitives : " + Duration.between(startPrimitives, endPrimitives).toMillis() + " ms");

        // ---------------------------------------------------
        // Étape 3 : Chargement des règles
        // ---------------------------------------------------
        Instant startRules = Instant.now();
        ArrayList<Rule> allRules = new ArrayList<>();

        for (String ruleFile : listRulesFileName) {
            try {
                Path pathFileRules = Paths.get(NatclinnConf.folderForRules, ruleFile);
                System.out.println("Chargement du fichier de règles : " + pathFileRules);
                allRules.addAll(Rule.rulesFromURL(pathFileRules.toUri().toString()));
            } catch (Exception e) {
                System.err.println("Erreur lors du chargement du fichier de règles : " + ruleFile + " (" + e.getMessage() + ")");
            }
        }

        if (allRules.isEmpty()) {
            throw new Exception("Aucune règle valide chargée ! Vérifiez la liste des fichiers de règles.");
        }

        Instant endRules = Instant.now();
        System.out.println("Total des règles chargées : " + allRules.size());
        System.out.println("Durée chargement règles : " + Duration.between(startRules, endRules).toMillis() + " ms");

        // ---------------------------------------------------
        // Étape 4 : Raisonnement et création du modèle inféré
        // ---------------------------------------------------
        Instant startInfer = Instant.now();
		
		// // Création du modèle inféré avec RDFS + règles personnalisées
		// InfModel infRDFS = ModelFactory.createRDFSModel(modelTemp);
        // Reasoner reasonerRules = new GenericRuleReasoner(allRules);
        // InfModel infModel = ModelFactory.createInfModel(reasonerRules, infRDFS);

		// Étape 1 - RDFS : inférences de base (subClassOf, subPropertyOf, etc.)
		InfModel infRDFS = ModelFactory.createRDFSModel(modelTemp);

		// Étape 2 - OWL Mini : ajoute les inférences OWL légères (ex. sous-classes OWL)
		Reasoner reasonerOWL = ReasonerRegistry.getOWLMiniReasoner();
		reasonerOWL = reasonerOWL.bindSchema(infRDFS);  // important : lie le schéma RDFS déjà enrichi
		InfModel infOWL = ModelFactory.createInfModel(reasonerOWL, modelTemp);

		// Étape 3 - Règles personnalisées : complète avec tes propres règles Jena
		GenericRuleReasoner reasonerCustom = new GenericRuleReasoner(allRules);
        reasonerCustom.setMode(GenericRuleReasoner.FORWARD_RETE); // Utilisation de l'algorithme RETE pour de meilleures performances
		// reasonerCustom.setMode(GenericRuleReasoner.HYBRID); // Mode hybride obligatoire pour OWLTranslation
		reasonerCustom.setDerivationLogging(true); // utile pour debugger les inférences
		reasonerCustom.setOWLTranslation(false);    // permet d'utiliser OWL équivalences dans les règles
        // reasonerCustom.setOWLTranslation(true);    // permet d'utiliser OWL équivalences dans les règles
		reasonerCustom.setTransitiveClosureCaching(true);

		// Combine les inférences OWL avec les règles personnalisées
		InfModel infModel = ModelFactory.createInfModel(reasonerCustom, infOWL);

        // Rechargement et préparation du modèle inféré
        infModel.rebind();
        infModel.prepare();

        Instant endInfer = Instant.now();
        System.out.println("Durée création modèle inféré : " + Duration.between(startInfer, endInfer).toMillis() + " ms");

        System.out.printf(
            "=== Modèle inféré prêt ===%nOntologies : %d | Règles : %d | Primitives : %d%n",
            listOntologiesFileName.size(), allRules.size(), listPrimitives.size()
        );

        return infModel;
    }

    /**
     * Enregistre dynamiquement les primitives personnalisées dans le registre Jena.
     *
     * @param listPrimitives Liste des noms de classes de primitives à charger (dans natclinn.util)
     */
    public static void registerBuiltins(ArrayList<String> listPrimitives) {
        for (String primitiveName : listPrimitives) {
            primitiveName = primitiveName.trim();
            try {
                String fullClassName = "natclinn.util." + primitiveName;
                Class<?> clazz = Class.forName(fullClassName);
                Object instance = clazz.getDeclaredConstructor().newInstance();

                if (instance instanceof Builtin) {
                    BuiltinRegistry.theRegistry.register((Builtin) instance);
                    System.out.println("Primitive enregistrée : " + fullClassName);
                } else {
                    System.err.println("La classe " + fullClassName + " n'implémente pas org.apache.jena.reasoner.rulesys.Builtin");
                }
            } catch (ClassNotFoundException e) {
                System.err.println("Primitive non trouvée : " + primitiveName);
            } catch (Exception e) {
                System.err.println("Erreur lors du chargement de la primitive : " + primitiveName + " (" + e.getMessage() + ")");
            }
        }
    }
}
