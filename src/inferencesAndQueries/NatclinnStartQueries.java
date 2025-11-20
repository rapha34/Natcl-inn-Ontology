package inferencesAndQueries;

import java.time.Duration;
import java.time.Instant;

import org.apache.jena.rdf.model.InfModel;

import ontologyManagement.CreateMychoiceProjectFromPreliminaryProject;

public class NatclinnStartQueries {

	// Lancement initial pour création modèle inferé et traitement des requêtes
	// Les fichiers déposés sur le serveur (\var\www\natclinn) servent à paramétrer le modéle
	//  à inférer (choix des ontologies, choix des régles à appliquer...) et les requêtes
	//  à executer
	public static void main(String[] args) throws Exception {

		Instant start0 = Instant.now();
		
		// Création du modèle inféré et exécution des requêtes
		// Retourne le modèle pour permettre la création des projets MyChoice
		InfModel infModel = NatclinnCreateInferredModelAndRunQueries.InferencesAndQueryWithModel();
		
		Instant end0 = Instant.now();
		System.out.println("Total running time : " + Duration.between(start0, end0).getSeconds() + " secondes");
		
		// Création des projets MyChoice à partir du modèle inféré
		if (infModel != null) {
			CreateMychoiceProjectFromPreliminaryProject.createFromInferredModel(infModel);
		}
	}
}