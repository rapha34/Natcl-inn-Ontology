package natclinn.util;

import org.apache.jena.graph.Node;
import org.apache.jena.reasoner.rulesys.RuleContext;
import org.apache.jena.reasoner.rulesys.builtins.BaseBuiltin;
import org.apache.jena.sparql.expr.NodeValue;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONObject;

/**
 * Primitive pour interroger l'API Open Food Facts
 * Usage: getOFFProperty(?ingredient, 'nova_group', ?novaGroup)
 */
public class GetOFFProperty extends BaseBuiltin {
    
    @Override
    public String getName() {
        return "getOFFProperty";
    }

    @Override
    public int getArgLength() {
        return 3; // ingredient, propertyName, result
    }

    @Override
    public boolean bodyCall(Node[] args, int length, RuleContext context) {
        checkArgs(length, context);
        
        Node ingredientNode = getArg(0, args, context);
        Node propertyNameNode = getArg(1, args, context);
        
        if (ingredientNode.isURI() && propertyNameNode.isLiteral()) {
            String ingredientURI = ingredientNode.getURI();
            String propertyName = propertyNameNode.getLiteralLexicalForm();
            
            try {
                // Récupérer l'ID OFF de l'ingrédient depuis le modèle
                String offId = getOFFIdFromModel(ingredientURI, context);
                if (offId == null) return false;
                
                // Appeler l'API OFF
                new NatclinnConf();
                String apiBaseUrl = NatclinnConf.OFF_API_BASE_URL; // Charger depuis la configuration
                String apiUrl = apiBaseUrl + offId + ".json";
                JSONObject response = callAPI(apiUrl);
                
                if (response != null && response.has("product")) {
                    JSONObject product = response.getJSONObject("product");
                    
                    // Extraire la propriété demandée
                    Object value = extractProperty(product, propertyName);
                    if (value != null) {
                        Node resultNode = createLiteralNode(value);
                        return context.getEnv().bind(args[2], resultNode);
                    }
                }
            } catch (Exception e) {
                System.err.println("Erreur API OFF: " + e.getMessage());
            }
        }
        return false;
    }
    
    private String getOFFIdFromModel(String ingredientURI, RuleContext context) {
        // Récupérer ncl:hasIdIngredientOFF depuis le modèle
        //String query = "SELECT ?id WHERE { <" + ingredientURI + "> <https://w3id.org/NCL/ontology/hasIdIngredientOFF> ?id }";
        // Implémentation de la requête SPARQL sur context.getGraph()
        // ... (simplifié ici)
        return null; // À implémenter
    }
    
    private JSONObject callAPI(String apiUrl) throws Exception {
        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", NatclinnConf.OFF_API_USER_AGENT);
        conn.setConnectTimeout(Integer.parseInt(NatclinnConf.OFF_API_CONNECT_TIMEOUT));
        conn.setReadTimeout(Integer.parseInt(NatclinnConf.OFF_API_READ_TIMEOUT));
        
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
            response.append(line);
        }
        in.close();
        
        return new JSONObject(response.toString());
    }
    
    private Object extractProperty(JSONObject product, String propertyName) {
        // Navigation dans l'objet JSON selon le nom de la propriété
        if (product.has(propertyName)) {
            return product.get(propertyName);
        }
        return null;
    }
    
    private Node createLiteralNode(Object value) {
        return NodeValue.makeString(value.toString()).asNode();
    }
}