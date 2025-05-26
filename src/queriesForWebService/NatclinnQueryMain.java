package queriesForWebService;

import java.time.Duration;
import java.time.Instant;
import natclinn.util.NatclinnConf;


public class NatclinnQueryMain {

	public static void main(String[] args) throws Exception {
		Instant start = Instant.now();
		// Initialisation de la configuration
		// Chemin d'accès, noms fichiers...
		new NatclinnConf();
		
		NatclinnQueryCreationInfModel.creationModel();

		Instant end = Instant.now();
		System.out.println("Durée d'exécution : " + Duration.between(start, end).getSeconds() + " secondes");
		
		NatclinnQueryInfModel.main(null);
		NatclinnQueryInfModel.main(null);
		
		NatclinnQueryCreationInfModel.creationModel();
		
		NatclinnQueryInfModel.main(null);
		NatclinnQueryInfModel.main(null);
		
	}
}		