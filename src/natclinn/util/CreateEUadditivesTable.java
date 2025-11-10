package natclinn.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Programme pour récupérer automatiquement la liste officielle
 * des additifs alimentaires approuvés par l'Union Européenne.
 *
 * Source :
 *   API DG SANTE - https://api.datalake.sante.service.ec.europa.eu/food-additives
 *
 * Exemple d’appel :
 *   GET /food_additives_list?format=json
 *
 * Objectif :
 *   - Construire une table de correspondance (nom/additif → fonction)
 *   - Permettre de classer automatiquement les ingrédients dans Natclinn
 */
public class CreateEUadditivesTable {

    /** Table principale : nom ou code E → catégorie technologique (en français) */
    private static final Map<String, String> COMMON_INGREDIENTS = new LinkedHashMap<>();

    /** URL officielle de l’API */
    private static final String API_URL =
        "https://api.datalake.sante.service.ec.europa.eu/food-additives/food_additives_list?format=json";

    public static void main(String[] args) {
        try {
            System.out.println("Téléchargement de la liste officielle des additifs de l'UE...");
            String jsonData = fetch(API_URL);
            parseAdditives(jsonData);

            System.out.println("\n=== Exemple de sortie ===");
            COMMON_INGREDIENTS.entrySet().stream().limit(20)
                .forEach(e -> System.out.println(
                    "COMMON_INGREDIENTS.put(\"" + e.getKey() + "\", \"" + e.getValue() + "\");"));

            System.out.println("\nNombre total d’additifs extraits : " + COMMON_INGREDIENTS.size());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Télécharge le contenu JSON depuis l’API.
     */
    private static String fetch(String apiUrl) throws IOException {
        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.connect();

        if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
            throw new IOException("Erreur HTTP : " + conn.getResponseCode());
        }

        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }

    /**
     * Analyse le JSON pour extraire les champs intéressants :
     * nom, code E et fonction technologique.
     */
    private static void parseAdditives(String json) {
        JSONArray arr = new JSONArray(json);
        for (int i = 0; i < arr.length(); i++) {
            JSONObject obj = arr.getJSONObject(i);

            String eCode = obj.optString("additive_e_code", "").trim();
            String name = obj.optString("additive_name", "").trim().toLowerCase();
            String function = obj.optString("functional_class", "").trim();

            if (function.isEmpty() && obj.has("additive_function")) {
                function = obj.optString("additive_function", "").trim();
            }

            String normalizedFunction = normalizeFunction(function);
            if (normalizedFunction.isEmpty()) continue;

            if (!name.isEmpty()) COMMON_INGREDIENTS.put(name, normalizedFunction);
            if (!eCode.isEmpty()) COMMON_INGREDIENTS.put(eCode, normalizedFunction);
        }
    }

    /**
     * Normalise les fonctions officielles en catégories simplifiées (en français).
     */
    private static String normalizeFunction(String func) {
        func = func.toLowerCase();
        if (func.contains("preservative")) return "conservateur";
        if (func.contains("acid") || func.contains("acidity")) return "acidifiant";
        if (func.contains("antioxidant")) return "antioxydant";
        if (func.contains("colour")) return "colorant";
        if (func.contains("flavour enhancer")) return "exhausteur_gout";
        if (func.contains("emulsifier")) return "émulsifiant";
        if (func.contains("stabiliser")) return "stabilisant";
        if (func.contains("thickener")) return "épaississant";
        if (func.contains("sweetener")) return "édulcorant";
        if (func.contains("raising agent")) return "agent_levant";
        if (func.contains("humectant")) return "humectant";
        if (func.contains("glazing agent")) return "agent_de_glacage";
        return "";
    }
}

