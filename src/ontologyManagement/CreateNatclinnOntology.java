package ontologyManagement;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.jena.ontology.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.vocabulary.DC;
import org.apache.jena.vocabulary.OWL;
import natclinn.util.NatclinnConf;

/**
 * Programme Java permettant de construire l'ontologie complète Natcl'inn
 * en fusionnant les TBox et ABox existantes, et en ajoutant les métadonnées descriptives.
 * 
 * L'ontologie finale est exportée au format RDF/XML et JSON-LD.
 * 
 * Auteur : Raphaël CONDE SALAZAR
 * Date   : 2025
 */
public class CreateNatclinnOntology {

    public static void main(String[] args) {

        // Chargement de la configuration du projet
        new NatclinnConf();
        String folderForOntologies = NatclinnConf.folderForOntologies;

        // Fichiers sources d'ontologies partielles (TBox et ABox)
        String inputFileTbox = folderForOntologies + "/NatclinnTbox.xml";
        String inputFileTboxMyChoice = folderForOntologies + "/MyChoiceTbox.xml";
        String inputFileTboxAIF = folderForOntologies + "/AIFTbox.xml";
        String inputFileProductsAbox = folderForOntologies + "/NatclinnProductsAbox.xml";
        String inputFileArgumentsAbox = folderForOntologies + "/NatclinnArgumentsAbox.xml";
        String inputFileClassificationAttribute = folderForOntologies + "/NatclinnClassificationAttribute.xml";
        String inputFileRoleAbox = folderForOntologies + "/NatclinnRoleAbox.xml";
        String inputFilePackagingTypeAbox = folderForOntologies + "/NatclinnPackagingTypeAbox.xml";
        String inputFileControlledOriginTypeAbox = folderForOntologies + "/NatclinnControlledOriginTypeAbox.xml";
        String inputFileNOVAmarkersAbox = folderForOntologies + "/NatclinnNOVAmarkers.xml";
        String inputFileNatclinnOFFTaxonomy = folderForOntologies + "/NatclinnOFFTaxonomy.xml";



        // Modèle ontologique principal
        OntModel om = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
        OntModel modelTemp = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);

        // Définition des espaces de noms (préfixes)
        String ncl = "https://w3id.org/NCL/ontology/";
        om.setNsPrefix("ncl", ncl);
        String mch = "https://w3id.org/MCH/ontology/";
        om.setNsPrefix("mch", mch);
        String skos = "http://www.w3.org/2004/02/skos/core#";
        om.setNsPrefix("skos", skos);
        String aif = "http://www.arg.dundee.ac.uk/aif#";
        om.setNsPrefix("aif", aif);
        String foaf = "http://xmlns.com/foaf/0.1/";
        om.setNsPrefix("foaf", foaf);
        String rdfs = "http://www.w3.org/2000/01/rdf-schema#";
        om.setNsPrefix("rdfs", rdfs);
        String bibo = "http://purl.org/ontology/bibo/";
        om.setNsPrefix("bibo", bibo);
        String vann = "http://purl.org/vocab/vann/";
        om.setNsPrefix("vann", vann);
        String schema = "http://schema.org/";
        om.setNsPrefix("schema", schema);
        String org = "http://www.w3.org/ns/org#";
        om.setNsPrefix("org", org);
        String vocab = "https://w3id.org/afy/vocab#";
        om.setNsPrefix("vocab", vocab);
        String dct = "http://purl.org/dc/terms/";
        om.setNsPrefix("dct", dct);

        ////////////////////////////////////////////////
        // Définition de l’ontologie principale       //
        ////////////////////////////////////////////////

        Ontology ont = om.createOntology(ncl + "NatclinnOntology");
        ont.addLabel("Natcl'inn ontology", "en");
        ont.addLabel("Ontologie Natcl'inn", "fr");

        // Descriptions
        ont.addProperty(DC.description, "Ontology of Natcl'inn", "en");
        ont.addProperty(DC.description, "L'ontologie du projet Natcl'inn", "fr");

        // Créateurs
        ont.addProperty(DC.creator, "Raphael CONDE SALAZAR", "en");
        ont.addProperty(DC.creator, "Raphaël CONDE SALAZAR", "fr");
        ont.addProperty(DC.creator, "Pierre BISQUERT");
        ont.addProperty(DC.creator, "Rallou THOMOPOULOS");

        // Résumés (abstracts)
        ont.addProperty(om.createProperty(dct + "abstract"),
                "Le projet NATCL’INN vise à aider les entreprises agroalimentaires à mieux répondre aux attentes de naturalité des consommateurs, perçue comme gage de qualité, de santé et de goût. En s’appuyant sur la caractérisation des représentations de la naturalité et l’identification de marqueurs pertinents, il développe un outil d’arbitrage multicritère intégrant contraintes techniques, économiques et réglementaires. Porté par l’ADRIA, l’UBO-LEGO, l’INRAE et plusieurs partenaires industriels, ce projet collaboratif est labellisé par Valorial.",
                "fr");
        ont.addProperty(om.createProperty(dct + "abstract"),
                "The NATCL’INN project aims to help agri-food companies better meet consumers' expectations for naturalness, perceived as a guarantee of quality, health and taste. Based on the characterisation of representations of naturalness and the identification of relevant markers, it develops a multi-criteria arbitration tool integrating technical, economic and regulatory constraints. Led by ADRIA, UBO-LEGO, INRAE and several industrial partners, this collaborative project is certified by Valorial.",
                "en");

        // Licence et métadonnées
        ont.addProperty(om.createProperty(dct + "license"),
                om.createResource("http://creativecommons.org/licenses/by/4.0/"));
        ont.addProperty(OWL.versionInfo, "1.0.0");
        ont.addProperty(OWL.versionIRI, om.createResource(ncl + "1.0.0"));
        ont.addProperty(om.createProperty(vann + "preferredNamespacePrefix"), "ncl", "en");
        ont.addProperty(om.createProperty(vann + "preferredNamespaceUri"), ncl);

        // Commentaires
        ont.addComment(
                "Une ontologie conçue pour décrire les produits de l'industrie agroalimentaire et les arguments associés à leur naturalité perçue.",
                "fr");
        ont.addComment(
                "An ontology designed to describe agri-food products and the arguments associated with their perceived naturalness.",
                "en");

        // Financement et citation
        ont.addProperty(om.createProperty(foaf + "fundedBy"),
                om.createResource("https://www.pole-valorial.fr"));
        ont.addProperty(om.createProperty(schema + "citation"),
                "Cite this vocabulary as: Raphaël Conde Salazar, Pierre Bisquert, Rallou Thomopoulos; DOI: XXXXXXXXXXXXXXXX",
                "en");

        // Introduction (en FR et EN)
        ont.addProperty(om.createProperty(vocab + "introduction"),
                "NATCL’INN vise à proposer une solution aux entreprises de l’agroalimentaire qui doivent réaliser des arbitrages entre différents attributs produits relatifs à la naturalité afin de répondre aux nouvelles attentes des consommateurs. Le projet associe ADRIA, l’UBO-LEGO, l’INRAE et plusieurs partenaires industriels. Il est labellisé par le pôle VALORIAL et soutenu par la Région Bretagne, la Région Pays de la Loire et Quimper Bretagne Occidentale. Démarrage : janvier 2024 – fin prévue : décembre 2027.",
                "fr");
        ont.addProperty(om.createProperty(vocab + "introduction"),
                "NATCL’INN aims to provide a solution to agri-food companies needing to balance different product attributes related to naturalness. The project involves ADRIA, UBO-LEGO, INRAE and several industrial partners. It is certified by VALORIAL and supported by the Brittany and Pays de la Loire regions. Start: January 2024 – End: December 2027.",
                "en");

        ////////////////////////////////////////////////
        // Fusion des sous-ontologies (TBox / ABox)   //
        ////////////////////////////////////////////////

        try {
            mergeOntology(om, modelTemp, inputFileTbox, ncl + "NatclinnTbox");
            mergeOntology(om, modelTemp, inputFileTboxMyChoice, mch + "MyChoiceTbox");
            mergeOntology(om, modelTemp, inputFileTboxAIF, aif + "AIFTbox");
            mergeOntology(om, modelTemp, inputFileProductsAbox, ncl + "NatclinnProductAbox");
            mergeOntology(om, modelTemp, inputFileArgumentsAbox, ncl + "NatclinnArgumentAbox");
            mergeOntology(om, modelTemp, inputFileClassificationAttribute, ncl + "ClassificationAttribute");
            mergeOntology(om, modelTemp, inputFileRoleAbox, ncl + "NatclinnRoleAbox");
            mergeOntology(om, modelTemp, inputFilePackagingTypeAbox, ncl + "NatclinnPackagingTypeAbox");
            mergeOntology(om, modelTemp, inputFileControlledOriginTypeAbox, ncl + "NatclinnControlledOriginTypeAbox");
            mergeOntology(om, modelTemp, inputFileNOVAmarkersAbox, ncl + "NatclinnNOVAmarkers");
            mergeOntology(om, modelTemp, inputFileNatclinnOFFTaxonomy, ncl + "OFFTaxonomy");
        } catch (IOException e) {
            System.err.println("Erreur lors de la fusion des fichiers : " + e.getMessage());
        }

        ////////////////////////////////////////////////
        // Exportation des résultats                  //
        ////////////////////////////////////////////////

        try {
            System.out.println("Modèle fusionné (" + om.size() + " triplets)");
            
            // Export RDF/XML avec buffer
            System.out.println("Écriture RDF/XML avec buffer...");
            FileOutputStream fosXML = new FileOutputStream(folderForOntologies + "/NatclinnOntology.owl");
            BufferedOutputStream bosXML = new BufferedOutputStream(fosXML, 8192 * 4); // 32KB buffer
            RDFDataMgr.write(bosXML, om, RDFFormat.RDFXML_PLAIN);
            bosXML.flush();
            bosXML.close();
            System.out.println(" - RDF/XML : " + folderForOntologies + "/NatclinnOntology.owl");
            
            // Export JSON-LD avec buffer
            System.out.println("Écriture JSON-LD avec buffer...");
            FileOutputStream fosJSON = new FileOutputStream(folderForOntologies + "/NatclinnOntology.jsonld");
            BufferedOutputStream bosJSON = new BufferedOutputStream(fosJSON, 8192 * 4); // 32KB buffer
            RDFDataMgr.write(bosJSON, om, RDFFormat.JSONLD11);
            bosJSON.flush();
            bosJSON.close();
            System.out.println(" - JSON-LD : " + folderForOntologies + "/NatclinnOntology.jsonld");
            
            System.out.println("Ontologie Natcl'inn exportée avec succès !");

        } catch (FileNotFoundException e) {
            System.err.println("Erreur : fichier introuvable - " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Erreur d’écriture : " + e.getMessage());
        }

        // Affichage console en format N3
        om.write(System.out, "N3");
    }

    /**
     * Méthode utilitaire pour fusionner une sous-ontologie dans le modèle principal.
     * 
     * @param target      modèle de destination (ontologie principale)
     * @param temp        modèle temporaire utilisé pour la lecture
     * @param filePath    chemin du fichier RDF/XML à fusionner
     * @param resourceUri URI de la ressource décrivant la sous-ontologie à supprimer
     * @throws IOException en cas d'erreur d'accès fichier
     */
    private static void mergeOntology(OntModel target, OntModel temp, String filePath, String resourceUri)
            throws IOException {
        try (InputStream in = RDFDataMgr.open(filePath)) {
            if (in == null)
                throw new FileNotFoundException("Fichier non trouvé : " + filePath);

            // Lecture du fichier RDF/XML
            temp.read(in, "");

            // Suppression de la description de l'ontologie locale
            temp.removeAll(temp.getResource(resourceUri), null, null);

            // Fusion des triples dans le modèle principal
            target.add(temp);

            // Nettoyage du modèle temporaire
            temp.removeAll();
        }
    }
}
