package ontologyManagement;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Ce programme interroge l'API Open Food Facts pour un EAN donné,
 * récupère les ingrédients et affiche pour chacun :
 * - le texte,
 * - le ciqual_food_code,
 * - le ciqual_proxy_food_code.
 */
public class OFFIngredientFetcher {

    public static void main(String[] args) {
        // String ean = "3270160865826"; // EAN du produit
        String ean = "3259426071751"; // EAN du produit
        String apiUrl = "https://world.openfoodfacts.org/api/v0/product/" + ean + ".json";
        // https://world.openfoodfacts.org/api/v0/product/3259426071751
        try {
            // Connexion HTTP à l'API OFF
            HttpURLConnection con = (HttpURLConnection) new URL(apiUrl).openConnection();
            con.setRequestMethod("GET");

            // Lecture de la réponse JSON
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            // Parsing du JSON
            JSONObject json = new JSONObject(response.toString());
            JSONObject product = json.getJSONObject("product");
            JSONArray ingredients = product.getJSONArray("ingredients");

            // Affichage des en-têtes du tableau
            System.out.printf("%-40s %-20s %-25s%n", "Text", "Ciqual_food_code", "Ciqual_proxy_food_code");
            System.out.println("---------------------------------------------------------------------------------------------");

            // Parcours des ingrédients
            for (int i = 0; i < ingredients.length(); i++) {
                JSONObject ing = ingredients.getJSONObject(i);
                String text = ing.optString("text", "N/A");
                String ciqualCode = ing.optString("ciqual_food_code", "N/A");
                String ciqualProxyCode = ing.optString("ciqual_proxy_food_code", "N/A");

                // Affichage formaté
                System.out.printf("%-40s %-20s %-25s%n", text, ciqualCode, ciqualProxyCode);
                // System.out.printf("%-40s %-20s %-25s%n", "", "", ciqualProxyCode);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
