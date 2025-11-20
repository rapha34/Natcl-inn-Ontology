package databaseManagement;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.util.ArrayList;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.shared.Lock;

import natclinn.util.NatclinnConf;
import natclinn.util.NatclinnUtil;
import natclinn.util.TDBUtil;

public class NatclinnTDBInitialisation {

	public static void main(final String[] args) throws Exception {

		initialisation();

	}
	public static void initialisation() throws Exception {

		// Forcer l'encodage UTF-8 pour la console afin d'éviter les problèmes d'accents
		try {
			System.setProperty("file.encoding", "UTF-8");
			System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
			System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8));
		} catch (Exception e) {
			// Ignorer silencieusement si non supporté
		}
		// Initialisation de la configuration
		// Chemin d'accès, noms fichiers...
		new NatclinnConf(); 

		// Récupération du nom du fichier contenant la liste des ontologies à traiter.
		Path pathOfTheListOntologies = Paths.get(NatclinnConf.mainFolderNatclinn, NatclinnConf.fileNameListOntologiesForInitTDB);
			
		// Récupération du nom du répertoire des ontologies à traiter dans la configuration
		Path pathOfTheDirectory = Paths.get(NatclinnConf.folderForOntologies);
	
		// Récupération du nom des fichiers d'ontologies dans listOntologiesFileName
		ArrayList<String> listOntologiesFileName = new ArrayList<String>();	
		listOntologiesFileName = NatclinnUtil.makeListFileName(pathOfTheListOntologies.toString()); 
		if (listOntologiesFileName.isEmpty()) {
			System.out.println("No ontologies to load in TDB");
			return;
		}
		System.out.println("The list of ontologies to load in TDB is : " + listOntologiesFileName.toString());

		long heapsize = Runtime.getRuntime().totalMemory();
		long maxSize = Runtime.getRuntime().maxMemory();
		int availableProcessors = Runtime.getRuntime().availableProcessors();

        System.out.println("Total memory is: " + heapsize);
		System.out.println("Max memory is: " + maxSize);
		System.out.println("The number of available processors is: " + availableProcessors);

		// Pour toutes les ontologies
		for (int i = 0; i < listOntologiesFileName.size(); i++) {
			String fileName = listOntologiesFileName.get(i);
			
			System.out.println("The ontology " + fileName + " is being loaded");   
			
			treatment(pathOfTheDirectory.toString(), fileName);
		}   
		System.out.println("End of the transfer of ontologies to TDB");
	}

	public static void treatment(String pathOfTheDirectory, String fileName) throws Exception {
		
		// Un nom pour la création du graphe TDB à partir du nom de l'ontologie
		String nameForGraphURI = fileName.replaceFirst("[.][^.]+$", "");
		Path pathFileDataset = Paths.get(pathOfTheDirectory, fileName);

		// Correction des erreurs du fichier
		modifyFile(pathFileDataset.toString());

		String typeOfSerialization = getFileSerializationType(fileName);

		Dataset dataset = null;
		try {
			dataset = TDBUtil.CreateTDBDataset();
			dataset.begin(ReadWrite.WRITE);

			// Effacement des statements contenus dans le graphe TDB
			dataset.removeNamedModel(nameForGraphURI);

			Model model = dataset.getNamedModel(nameForGraphURI);
			model.enterCriticalSection(Lock.WRITE);
			model.clearNsPrefixMap();

			// Lecture du fichier en fonction du type de sérialisation
			try (InputStream is = new FileInputStream(pathFileDataset.toString())) {
				model.read(is, "", typeOfSerialization);
			}

			System.out.println("Graph size " + nameForGraphURI + " : " + model.size());
			model.leaveCriticalSection();
			dataset.commit();

		} catch (FileNotFoundException e) {
			System.out.println("File not found: " + pathFileDataset + " : " + e);
		} catch (Exception e) {
			System.out.println("Error processing dataset: " + e.toString());
			e.printStackTrace();
		} finally {
			if (dataset != null) {
				dataset.end();
				dataset.close();
			}
		}
		
	}
	/**
	 * Détection du type de sérialisation
	 */
	private static String getFileSerializationType(String fileName) {
		if (fileName.matches("^.*json$")) {
			return "JSONLD";
		} else if (fileName.matches("^.*ttl$")) {
			return "TTL";
		} else {
			return "RDF/XML";  // Default to RDF/XML if no specific type is detected
		}
	}

	/**
	 * Correction des problémes de typo du fichier
	 */
	public static void modifyFile(String filePath) {
		try {
			// Ouverture du fichier d'entrée et du fichier temporaire de sortie
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8));
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filePath + ".tmp"), StandardCharsets.UTF_8));
	
			// Lecture et modification ligne par ligne
			String line;
			while ((line = reader.readLine()) != null) {
				// Modification des erreurs typographiques
				String modifiedLine = line.replaceAll("http:///", "http://")
					.replaceAll("https:///", "https://")
					.replaceAll("%5D%5(?!D)", "%5D%5D")
					.replaceAll("xmlns:ns3=\"http://dbkwik.webdatacommons.org/marvel.wikia.com/property/%3\"", "")
					.replaceAll("ns3:CdivAlign", "ns1:divAlign")
					.replaceAll("http://www.wikipedia.com:secrets_of_spiderman_revealed", "http://www.wikipedia.com/secrets_of_spiderman_revealed")
					.replaceAll("OntologyID\\(Anonymous-2\\)module1", "http://OntologyID/Anonymous_2/module1")
					.replaceAll("ِAlRay_AlAam", "AlRay_AlAam")
					.replaceAll("<dcterms:created rdf:datatype=\"http://www.w3.org/2001/XMLSchema#dateTime\"></dcterms:created>", "<dcterms:created rdf:datatype=\"http://www.w3.org/2001/XMLSchema#dateTime\">0001-01-01T00:00:00Z</dcterms:created>")
					.replaceAll("<dcterms:modified rdf:datatype=\"http://www.w3.org/2001/XMLSchema#dateTime\"></dcterms:modified>", "<dcterms:modified rdf:datatype=\"http://www.w3.org/2001/XMLSchema#dateTime\">0001-01-01T00:00:00Z</dcterms:modified>")
					.replaceAll("http:/php.net", "http://php.net")
					.replaceAll("http:/www.apple.com/jp/iphone", "http://www.apple.com/jp/iphone")
					.replaceAll("http:/www.apple.com/safari", "http://www.apple.com/safari")
					.replaceAll("http:/www.senate.gov", "http://www.senate.gov");
	
				// Encodage des caractères illégaux dans les IRIs
				String encodedLine = encodeInvalidCharactersInIRI(modifiedLine);
	
				// Unicode Normalization Form C
				String normalizedLine = Normalizer.normalize(encodedLine, Normalizer.Form.NFC);
				writer.write(normalizedLine + "\n");
			}
	
			// Fermeture des flux
			reader.close();
			writer.close();
	
			// Suppression du fichier d'origine
			Files.deleteIfExists(Paths.get(filePath));
	
			// Renommer le fichier temporaire en fichier d'origine
			Files.move(Paths.get(filePath + ".tmp"), Paths.get(filePath), StandardCopyOption.REPLACE_EXISTING);
	
			System.out.println("Le fichier a été modifié avec succès.");
		} catch (IOException e) {
			System.out.println("Une erreur s'est produite lors de la modification du fichier : " + e.getMessage());
		}
	}
	
	private static String encodeInvalidCharactersInIRI(String iri) {
		StringBuilder encodedIRI = new StringBuilder();
		for (char c : iri.toCharArray()) {
			// Vérifier si le caractère est illégal pour un IRI et doit être encodé
			if (isIllegalIRICharacter(c)) {
				try {
					// Encoder le caractère illégal
					encodedIRI.append(URLEncoder.encode(String.valueOf(c), StandardCharsets.UTF_8.toString()));
				} catch (Exception e) {
					// En cas d'erreur d'encodage
					encodedIRI.append(c); // Garder le caractère original si l'encodage échoue
				}
			} else {
				encodedIRI.append(c); // Caractère valide, on le garde tel quel
			}
		}
		return encodedIRI.toString();
	}
	
	private static boolean isIllegalIRICharacter(char c) {
		// Définir les caractères illégaux à encoder
		return (c >= 0xD800 && c <= 0xDFFF) // Paires de substitution UTF-16
				//|| c == ' ' // Les espaces sont également illégaux dans un IRI (mais là nous lisons la ligne entière)
				|| !Character.isValidCodePoint(c); // Tout autre caractère non valide
	}


}




