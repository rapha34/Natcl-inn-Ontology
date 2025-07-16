package ontologyManagement;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.HermiT.ReasonerFactory;
import org.semanticweb.owlapi.reasoner.*;

import natclinn.util.NatclinnConf;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;


public class OntologyChecker {
    public static void main(String[] args) throws Exception {

        final Logger natclinnChecker_logger = LogManager.getLogger(OntologyChecker.class);

        // 1. Créer un gestionnaire d'ontologie
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

        // 2. Charger l'ontologie TTL depuis un fichier
        // Initialisation de la configuration
		// Chemin d'accès, noms fichiers...
		new NatclinnConf(); 
			
		// Récupération du nom du répertoire des ontologies à traiter dans la configuration
		Path pathOfTheDirectory = Paths.get(NatclinnConf.folderForOntologies);
        Path pathFileDataset = Paths.get(pathOfTheDirectory.toString(), "NatclinnOntology.owl");


        File ontologyFile = new File(pathFileDataset.toString());
        if (!ontologyFile.exists()) {
            System.err.println("Ontology file not found: " + ontologyFile.getAbsolutePath());
            natclinnChecker_logger.error("Ontology file not found: " + ontologyFile.getAbsolutePath());
            return;
        }
        OWLOntology ontology = manager.loadOntologyFromOntologyDocument(ontologyFile);
        System.out.println("Ontology loaded: " + ontology.getOntologyID());
        natclinnChecker_logger.info("Ontology loaded: " + ontology.getOntologyID());

        System.out.println("Axioms count: " + ontology.getAxiomCount());
        natclinnChecker_logger.info("Axioms count: " + ontology.getAxiomCount()); 


        // 3. Créer un raisonneur HermiT
        OWLReasonerFactory reasonerFactory = new ReasonerFactory();
        OWLReasoner reasoner = reasonerFactory.createReasoner(ontology);

        // 4. Vérifier la cohérence
        if (reasoner.isConsistent()) {
            System.out.println("Ontology is consistent.");
            natclinnChecker_logger.info("Ontology is consistent.");
        } else {
            System.out.println("Ontology is NOT consistent.");
            natclinnChecker_logger.error("Ontology is NOT consistent.");
        }

        // 5. Vérifier les classes incohérentes
        try {
            Node<OWLClass> unsatisfiable = reasoner.getUnsatisfiableClasses();
            if (unsatisfiable.getEntitiesMinusBottom().isEmpty()) {
                System.out.println("No unsatisfiable classes.");
                natclinnChecker_logger.info("No unsatisfiable classes.");
            } else {
                System.out.println("Unsatisfiable classes:");
                unsatisfiable.getEntitiesMinusBottom().forEach(cls ->
                    System.out.println(" - " + cls));
                natclinnChecker_logger.error("Unsatisfiable classes: " + unsatisfiable.getEntitiesMinusBottom());
            }
        } catch (InconsistentOntologyException e) {
            System.err.println("Cannot compute unsatisfiable classes because ontology is inconsistent.");
            natclinnChecker_logger.error("Ontology is inconsistent: unable to get unsatisfiable classes.");
        }

        // 6. Nettoyage
        reasoner.dispose();
    }
}
