package ontologyManagement;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import org.json.JSONObject;

import natclinn.util.NatclinnUtil;

public class IngredientTreePrinter {

    public static void main(String[] args) {

         // String ean = "3270160865826"; // EAN du produit
        String ean = "3259426071751"; // EAN du produit
        String apiUrl = "https://world.openfoodfacts.org/api/v0/product/" + ean + ".json";


        String input = null;
        JSONObject product = null;
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
            product = json.getJSONObject("product");
            input = product.getString("ingredients_text_fr");

           
        } catch (Exception e) {
            e.printStackTrace();
        }

       
        // String input = "Farine de blé CRC® 26%, huile de colza, œufs frais 17%, sucre, sirop de glucose-fructose, stabilisant : glycérol, chocolat (origine UE) 4% (sucre, pâte de cacao, beurre de cacao, cacao maigre en poudre, émulsifiant : lécithines (soja), arôme naturel de vanille), lait écrémé en poudre, cacao maigre en poudre 1%, poudre à lever (diphosphates, carbonates de sodium); amidon, émulsifiant : E471 (origine végétale), sel, arômes, épaississants : gomme xanthane (avoine) - gomme guar (agar-agar).";
        

        if (product != null) {
            System.out.println("Id : " + product.getString("_id"));
            System.out.println("Ean : " + product.getString("code"));
            System.out.println("Brands : " + product.getString("brands"));
            System.out.println("Name : " + product.getString("product_name_fr"));
            System.out.println("Category : " + product.getString("compared_to_category"));
            // Appelle le parseur de NatclinnUtil
            List<NatclinnUtil.Ingredient> ingredients = NatclinnUtil.parse(input);

            // Affiche l'arbre complet
            for (NatclinnUtil.Ingredient ing : ingredients) {
                printIngredientTree(ing, 0);
            }
        } else {
            System.out.println("Product data could not be retrieved.");
        }
    }

    private static void printIngredientTree(NatclinnUtil.Ingredient ing, int indent) {
        // Indentation visuelle
        for (int i = 0; i < indent; i++) System.out.print("  ");

        // Affichage des infos de l'ingrédient
        System.out.print("- ");
        if (ing.getType() != null) System.out.print(ing.getType() + ": ");
        System.out.print(ing.getName());
        if (ing.getPercentage() != null) System.out.print(" (" + ing.getPercentage() + "%)");
        if (!ing.getAnnotations().isEmpty()) System.out.print(" " + ing.getAnnotations());
        System.out.println();

        // Appel récursif pour les sous-ingrédients
        for (NatclinnUtil.Ingredient sub : ing.getSubIngredients()) {
            printIngredientTree(sub, indent + 1);
        }
    }
}
