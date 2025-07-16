package ontologyManagement;

import org.apache.jena.ontology.AnnotationProperty;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import natclinn.util.NatclinnConf;
import natclinn.util.NatclinnUtil;
import natclinn.util.NatclinnUtil.Ingredient;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Scanner;

public class OpenFoodFactsFetcher {

    public static void main(String[] args) throws Exception {

        String searchProperty = "categories_tags_en";
        String searchPropertyString = "madeleines";
        extractOFF(searchProperty, searchPropertyString);

    }

    public static void extractOFF(String searchProperty, String searchPropertyString) throws Exception {
        // Chemin d'accès, noms fichiers...	
		new NatclinnConf();
        String ncl = NatclinnConf.ncl;
        String skos = NatclinnConf.skos; 

        OntModel om = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
        om.setNsPrefix("ncl", ncl);
        om.setNsPrefix("skos", skos);

        Property hasNutriScore = om.createProperty(ncl, "hasNutriScore");
        Property hasBrand = om.createProperty(ncl, "hasBrand");
        AnnotationProperty prefLabel = om.createAnnotationProperty(skos + "prefLabel");
        Resource NutriScore = om.createResource(ncl + "NutriScore");
        Resource Product = om.createResource(ncl + "Product");

        int pageSize = 100;
        String OFF_API_URL = "https://world.openfoodfacts.net/api/v2/search?" + searchProperty + "=" + searchPropertyString + "&json=1&page_size=1&page=1&fields=product_name";
        // https://world.openfoodfacts.net/api/v2/search?categories_tags_en=madeleines&json=1&page_size=100&page=16

        // Récupération premier JSON
        String jsonResponse = getHttpContent(OFF_API_URL);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(jsonResponse);
        JsonNode count = root.path("count");
        if (count == null || count.asInt() == 0) {
            System.out.println("Aucun produit trouvé pour : " + searchProperty + "=" + searchPropertyString);
            return;
        }
        // System.out.println("count : " + count.asInt());
        int totalPages = (count.asInt() + pageSize - 1) / pageSize;  // Partie entière supérieure
        String field = "&fields=code,product_name,brands,nutriscore_grade,product_quantity,product_quantity_unit,serving_quantity,serving_quantity_unit,ingredients_text_fr,ingredients,ingredients_text,ingredients_text_fr";

        // Boucle sur toutes les pages
        for (int page = 1; page <= totalPages; page++) {
            System.out.println("Traitement de la page " + page + "/" + totalPages);
            OFF_API_URL = "https://world.openfoodfacts.net/api/v2/search?" + searchProperty + "=" + searchPropertyString + "&json=1&page_size=" + pageSize + "&page=" + page + field;
            jsonResponse = getHttpContent(OFF_API_URL);
            mapper = new ObjectMapper();
            root = mapper.readTree(jsonResponse);
            JsonNode products = root.path("products");

            for (int i = 0; i < products.size(); i++) {
                JsonNode p = products.get(i);
                String code = p.path("code").asText(); // code EAN du produit 
                String productName = p.path("product_name").asText("");
                String brand = p.path("brands").asText("");
                String nutri = p.path("nutriscore_grade").asText("");
                // String product_quantity = p.path("product_quantity").asText("");
                // String product_quantity_unit = p.path("product_quantity_unit").asText("");
                // String serving_quantity = p.path("serving_quantity").asText("");
                // String serving_quantity_unit = p.path("serving_quantity_unit").asText("");
                String ingredientsText = p.path("ingredients_text_fr").asText("");
                
                if (productName.isEmpty()) continue;
                if (!productName.equals("Madeleines coquilles aux oeufs")) continue;
                // if (!code.equals("3178530410105")) continue;

                    String productURI;
                    if (code == null || code.isEmpty()) {
                        long counter = NatclinnUtil.getNextProductCounterValue();
                        String paddedCounter = String.format("%013d", counter); // format 13 chiffres
                        productURI = "P-" + paddedCounter;
                    } else {
                        productURI = "P-" + code;
                    }

                Resource product = om.createResource(ncl + productURI);
                product.addProperty(RDF.type, Product);
                product.addProperty(prefLabel, productName);

                if (!brand.isEmpty()) {
                    Resource brandRes = om.createResource(ncl + "Brand-" + sanitize(brand));
                    product.addProperty(hasBrand, brandRes);
                }

                if (!nutri.isEmpty()) {
                    Resource nutriRes = om.createResource(ncl + "NutriScore-" + nutri.toUpperCase());
                    nutriRes.addProperty(RDF.type, NutriScore);
                    product.addProperty(hasNutriScore, nutriRes);
                }

                System.out.println("produit :" + productName);
                System.out.println("produit :" + i);

                
                System.out.println(ingredientsText);
                List<Ingredient> parsed = natclinn.util.NatclinnUtil.parse(ingredientsText);
                parsed.forEach(System.out::println);
            

                // Ajouter les ingrédients du produit
                JsonNode ingredients = p.path("ingredients");
                for (int j = 0; j < ingredients.size(); j++) {
                    JsonNode ing = ingredients.get(j);
                    String ingId = ing.path("id").asText();
                    // String ingLabel = ing.path("text").asText();
                    // String ciqual_food_code = ing.path("ciqual_food_code").asText("");
                    // String ciqual_proxy_food_code = ing.path("ciqual_proxy_food_code").asText("");
                    // String ingPercent = ing.path("percent").asText("");
                    // String ingPercentEstimate = ing.path("percent_estimate").asText("");
                    // String ingRank = ing.path("rank").asText("");
                    // String ingHasSubIng = ing.path("has_sub_ingredients").asText("");
                    
                    // Boolean isComposite = "yes".equalsIgnoreCase(ingHasSubIng);

                    if (ingId == null || ingId.isEmpty()) continue;
                    // Resource ingredient = om.createResource(ncl + "Ingredient-" + sanitize(ingId));
                    // System.out.println(ingId);
                    // System.out.println(isComposite);
                }
            }    

        }

        try {   

			//////////////////////////////
			// Sorties fichiers         //
			////////////////////////////// 
            
            FileOutputStream out = new FileOutputStream(NatclinnConf.folderForOntologies + "/MadeleineAbox.xml"); 
            om.write(out, "RDF/XML-ABBREV");

			//om.write(new FileOutputStream(NatclinnConf.folderForOntologies + "/output.n3"), "N3");
            // N3 N-TRIPLE RDF/XML RDF/XML-ABBREV
			om.write(System.out, "N3");

			// outStream.close();
            out.close();
		}
		catch (FileNotFoundException e) {System.out.println("File not found");}
		catch (IOException e) {System.out.println("IO problem");}
        

        System.out.println("Fichier MadeleineAbox.xml généré.");
    }

    private static String getHttpContent(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("User-Agent", "Java");

        try (Scanner scanner = new Scanner(conn.getInputStream(), "UTF-8")) {
            scanner.useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next() : "";
        }
    }

    private static String sanitize(String input) {
        return input.trim().replaceAll("[^a-zA-Z0-9]", "_");
    }

}
