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
        // Étape 4 : Raisonnement (Passe 1 : présence uniquement)
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

        // Séparation des règles par passe selon leur préfixe
        ArrayList<Rule> pass1Rules = new ArrayList<>();
        ArrayList<Rule> pass2Rules = new ArrayList<>();
        ArrayList<Rule> pass3Rules = new ArrayList<>();
        
        for (Rule r : allRules) {
            String rn = r.getName() == null ? "" : r.getName();
            if (rn.startsWith("Pass2")) {
                pass2Rules.add(r);
            } else if (rn.startsWith("Pass3")) {
                pass3Rules.add(r);
            } else {
                pass1Rules.add(r);
            }
        }
        System.out.println("Règles Pass1: " + pass1Rules.size() + " | Règles Pass2: " + pass2Rules.size() + " | Règles Pass3: " + pass3Rules.size());

        // ---------------------------------------------------
        // Étape 5 : Passe 1 - Règles du premier passage
        // ---------------------------------------------------
        System.out.println("=== Passe 1 démarrage ===");
        GenericRuleReasoner reasonerPass1 = new GenericRuleReasoner(pass1Rules);
        reasonerPass1.setMode(GenericRuleReasoner.FORWARD);
        reasonerPass1.setDerivationLogging(false);
        reasonerPass1.setTransitiveClosureCaching(true);

        InfModel infModelPass1 = ModelFactory.createInfModel(reasonerPass1, infOWL);
        infModelPass1.prepare();
        System.out.println("Passe 1 terminée. Taille modèle: " + infModelPass1.size());

        // ---------------------------------------------------
        // Étape 6 : Passe 2 si des règles Pass2 existent
        // ---------------------------------------------------
        InfModel infModelPass2 = infModelPass1;
        if (!pass2Rules.isEmpty()) {
            System.out.println("=== Passe 2 démarrage ===");
            new NatclinnConf();
            
            // Base = modèle Pass1 matérialisé
            Model basePass2 = ModelFactory.createDefaultModel().add(infModelPass1);

            GenericRuleReasoner reasonerPass2 = new GenericRuleReasoner(pass2Rules);
            reasonerPass2.setMode(GenericRuleReasoner.FORWARD);
            reasonerPass2.setDerivationLogging(false);
            reasonerPass2.setTransitiveClosureCaching(true);

            infModelPass2 = ModelFactory.createInfModel(reasonerPass2, basePass2);
            infModelPass2.prepare();
            System.out.println("Passe 2 terminée. Taille modèle: " + infModelPass2.size());
        } else {
            System.out.println("Aucune règle Pass2 détectée. Passage à Pass3 avec modèle Pass1.");
        }
        
        // ---------------------------------------------------
        // Étape 7 : Passe 3 si des règles Pass3 existent
        // ---------------------------------------------------
        InfModel infModelPass3 = infModelPass2;
        if (!pass3Rules.isEmpty()) {
            System.out.println("=== Passe 3 démarrage ===");
            new NatclinnConf();
            
            // Base = modèle Pass2 matérialisé
            Model basePass3 = ModelFactory.createDefaultModel().add(infModelPass2);

            GenericRuleReasoner reasonerPass3 = new GenericRuleReasoner(pass3Rules);
            reasonerPass3.setMode(GenericRuleReasoner.FORWARD);
            reasonerPass3.setDerivationLogging(false);
            reasonerPass3.setTransitiveClosureCaching(true);

            infModelPass3 = ModelFactory.createInfModel(reasonerPass3, basePass3);
            infModelPass3.prepare();
            System.out.println("Passe 3 terminée. Taille modèle finale: " + infModelPass3.size());
        } else {
            System.out.println("Aucune règle Pass3 détectée. Modèle final = Pass2.");
        }

        Instant endInfer = Instant.now();
        System.out.println("Durée création modèle inféré (3 passes) : " + Duration.between(startInfer, endInfer).toMillis() + " ms");

        System.out.printf(
            "=== Modèle inféré prêt ===%nOntologies : %d | Règles totales : %d | Primitives : %d | Taille finale : %d%n",
            listOntologiesFileName.size(), allRules.size(), listPrimitives.size(), infModelPass3.size()
        );

        return infModelPass3;
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
