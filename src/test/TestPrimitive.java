package test;

import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.reasoner.rulesys.GenericRuleReasoner;
import org.apache.jena.reasoner.rulesys.Rule;

import natclinn.util.NatclinnConf;
import natclinn.util.NatclinnUtil;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class TestPrimitive {
    public static void main(String[] args) {
        try {
            System.out.println("=== Test de la primitive comparePackagingTypeProperty ===");

            // Initialisation
            new NatclinnConf();

            // Créer un modèle de base minimal pour le test
            Model baseModel = ModelFactory.createDefaultModel();

            // Ajouter quelques données de test minimales
            String ncl = "https://w3id.org/NCL/ontology/";
            baseModel.createResource(ncl + "Product1")
                    .addProperty(baseModel.createProperty(ncl + "rdf:type"), baseModel.createResource(ncl + "Product"))
                    .addProperty(baseModel.createProperty(ncl + "isProductIAA"), "true", "http://www.w3.org/2001/XMLSchema#boolean")
                    .addProperty(baseModel.createProperty(ncl + "hasTypePackaging"), baseModel.createResource(ncl + "emballage_plastique"));

            System.out.println("Modèle de test créé avec " + baseModel.size() + " triples");

            // Charger les règles de packaging
            Path pathFileRules = Paths.get(NatclinnConf.folderForRules, "natclinn_packaging.rules");
            System.out.println("Chargement du fichier de règles : " + pathFileRules);
            ArrayList<Rule> allRules = new ArrayList<>(Rule.rulesFromURL(pathFileRules.toUri().toString()));

            // Séparer les règles Pass3
            ArrayList<Rule> pass3Rules = new ArrayList<>();
            for (Rule r : allRules) {
                String rn = r.getName() == null ? "" : r.getName();
                if (rn.startsWith("Pass3")) {
                    pass3Rules.add(r);
                }
            }

            System.out.println("Règles Pass3 trouvées : " + pass3Rules.size());

            if (!pass3Rules.isEmpty()) {
                // Créer le reasoner pour Pass3
                GenericRuleReasoner reasonerPass3 = new GenericRuleReasoner(pass3Rules);
                reasonerPass3.setMode(GenericRuleReasoner.FORWARD);
                reasonerPass3.setDerivationLogging(false);
                reasonerPass3.setTransitiveClosureCaching(true);

                // Créer le modèle inféré pour Pass3
                System.out.println("=== Passe 3 démarrage ===");
                InfModel infModelPass3 = ModelFactory.createInfModel(reasonerPass3, baseModel);

                // Forcer la préparation (c'est là que l'exception se produisait avant)
                infModelPass3.prepare();

                System.out.println("=== Test réussi ! ===");
                System.out.println("Passe 3 terminée. Taille modèle: " + infModelPass3.size());
            } else {
                System.out.println("Aucune règle Pass3 trouvée");
            }

        } catch (Exception e) {
            System.err.println("Erreur: " + e.getMessage());
            e.printStackTrace();
        }
    }
}