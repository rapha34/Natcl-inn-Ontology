package natclinn.util;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.reasoner.rulesys.RuleContext;
import org.apache.jena.reasoner.rulesys.builtins.BaseBuiltin;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import org.json.JSONObject;
import org.json.JSONArray;
import java.util.HashMap;
import java.util.Map;

/**
 * Primitive pour identifier le rôle d'un ingrédient via Open Food Facts
 * Usage: getIngredientRole(?ingredientLabel, ?role)
 * Retourne: une ressource URI ncl:<rôle> (ex: ncl:conservateur, ncl:emulsifiant_stabilisant)
 */
public class GetIngredientRole extends BaseBuiltin {
    
    private static final String ncl;
    
    // Cache pour éviter les appels API répétés
    private static Map<String, String> cache = new HashMap<>();
    
    // Mapping des catégories E-numbers vers les rôles
    private static final Map<String, String> E_NUMBER_ROLES = new HashMap<>();
    
    static {
        // Initialisation de la configuration
        new NatclinnConf();
        ncl = NatclinnConf.ncl;
        
        // Conservateurs (E200-E299)
        for (int i = 200; i <= 299; i++) {
            E_NUMBER_ROLES.put("E" + i, "conservateur");
        }
        // Antioxydants (E300-E399)
        for (int i = 300; i <= 321; i++) {
            E_NUMBER_ROLES.put("E" + i, "antioxydant");
        }
        // Émulsifiants, stabilisants, épaississants (E400-E499)
        for (int i = 400; i <= 499; i++) {
            E_NUMBER_ROLES.put("E" + i, "emulsifiant_stabilisant");
        }
        // Régulateurs d'acidité (E500-E599)
        for (int i = 500; i <= 599; i++) {
            E_NUMBER_ROLES.put("E" + i, "regulateur_acidite");
        }
        // Exhausteurs de goût (E620-E640)
        for (int i = 620; i <= 640; i++) {
            E_NUMBER_ROLES.put("E" + i, "exhausteur_gout");
        }
        // Édulcorants (E950-E969)
        for (int i = 950; i <= 969; i++) {
            E_NUMBER_ROLES.put("E" + i, "edulcorant");
        }
        // Colorants (E100-E199)
        for (int i = 100; i <= 199; i++) {
            E_NUMBER_ROLES.put("E" + i, "colorant");
        }
        
        // Ajouts spécifiques connus
        E_NUMBER_ROLES.put("E330", "acidifiant"); // Acide citrique
        E_NUMBER_ROLES.put("E322", "emulsifiant"); // Lécithine
    }
    
    // Base de données locale des ingrédients courants
    private static final Map<String, String> COMMON_INGREDIENTS = new HashMap<>();
    
    static {
        // Conservateurs
        COMMON_INGREDIENTS.put("sel", "sel");
        COMMON_INGREDIENTS.put("sel de Guérande", "sel");
        COMMON_INGREDIENTS.put("sucre", "conservateur_naturel");
        COMMON_INGREDIENTS.put("vinaigre", "conservateur_naturel");
        COMMON_INGREDIENTS.put("acide citrique", "acidifiant");
        COMMON_INGREDIENTS.put("sorbate de potassium", "conservateur");
        COMMON_INGREDIENTS.put("benzoate de sodium", "conservateur");
        COMMON_INGREDIENTS.put("nitrite de sodium", "conservateur");
        COMMON_INGREDIENTS.put("sulfite", "conservateur");

        // Exhausteurs de goût
        COMMON_INGREDIENTS.put("glutamate monosodique", "exhausteur_gout");
        COMMON_INGREDIENTS.put("glutamate de sodium", "exhausteur_gout");
        COMMON_INGREDIENTS.put("msg", "exhausteur_gout");
        COMMON_INGREDIENTS.put("levure", "exhausteur_gout_naturel");
        COMMON_INGREDIENTS.put("extrait de levure", "exhausteur_gout_naturel");

        // Émulsifiants
        COMMON_INGREDIENTS.put("lécithine", "emulsifiant");
        COMMON_INGREDIENTS.put("lécithine de soja", "emulsifiant");
        COMMON_INGREDIENTS.put("lécithine de tournesol", "emulsifiant");
        COMMON_INGREDIENTS.put("mono- et diglycérides", "emulsifiant");
        COMMON_INGREDIENTS.put("gomme xanthane", "epaississant");
        COMMON_INGREDIENTS.put("xanthane", "epaississant");
        COMMON_INGREDIENTS.put("gomme de guar", "epaississant");
        COMMON_INGREDIENTS.put("carraghénane", "epaississant");
        COMMON_INGREDIENTS.put("carraghenane", "epaississant"); // variante orthographique

        // Colorants
        COMMON_INGREDIENTS.put("colorant_naturel", "colorant_naturel");
        COMMON_INGREDIENTS.put("colorant naturel", "colorant_naturel");
        COMMON_INGREDIENTS.put("caramel", "colorant_naturel");
        COMMON_INGREDIENTS.put("curcuma", "colorant_naturel");
        COMMON_INGREDIENTS.put("paprika", "colorant_naturel");
        COMMON_INGREDIENTS.put("betterave rouge", "colorant_naturel");
        COMMON_INGREDIENTS.put("charbon végétal", "colorant_naturel");
        COMMON_INGREDIENTS.put("rocou", "colorant_naturel");
        COMMON_INGREDIENTS.put("chlorophylle", "colorant_naturel");

        // Édulcorants
        COMMON_INGREDIENTS.put("edulcorant", "edulcorant");
        COMMON_INGREDIENTS.put("édulcorant", "edulcorant");
        COMMON_INGREDIENTS.put("aspartame", "edulcorant");
        COMMON_INGREDIENTS.put("sucralose", "edulcorant");
        COMMON_INGREDIENTS.put("stévia", "edulcorant_naturel");
        COMMON_INGREDIENTS.put("stevia", "edulcorant_naturel"); // variante
        COMMON_INGREDIENTS.put("sirop d'agave", "edulcorant_naturel");
        COMMON_INGREDIENTS.put("miel", "edulcorant_naturel");
        COMMON_INGREDIENTS.put("fructose", "edulcorant");
        COMMON_INGREDIENTS.put("sorbitol", "edulcorant");
        COMMON_INGREDIENTS.put("xylitol", "edulcorant");
        COMMON_INGREDIENTS.put("érythritol", "edulcorant");
        COMMON_INGREDIENTS.put("erythritol", "edulcorant"); // variante

        // Antioxydants
        COMMON_INGREDIENTS.put("antioxydant", "antioxydant");
        COMMON_INGREDIENTS.put("anti oxydant", "antioxydant");
        COMMON_INGREDIENTS.put("anti_oxydant", "antioxydant");
        COMMON_INGREDIENTS.put("anti-oxydant", "antioxydant");
        COMMON_INGREDIENTS.put("vitamine e", "antioxydant");
        COMMON_INGREDIENTS.put("vitamine c", "antioxydant");
        COMMON_INGREDIENTS.put("acide ascorbique", "antioxydant");
        COMMON_INGREDIENTS.put("tocophérol", "antioxydant");
        COMMON_INGREDIENTS.put("tocopherol", "antioxydant"); // variante
        COMMON_INGREDIENTS.put("acide citrique", "antioxydant_acidifiant");
        COMMON_INGREDIENTS.put("extrait de romarin", "antioxydant_naturel");

        // Agents de charge
        COMMON_INGREDIENTS.put("maltodextrine", "agent_charge");
        COMMON_INGREDIENTS.put("dextrose", "agent_charge");
        COMMON_INGREDIENTS.put("sirop de glucose", "agent_charge");
        COMMON_INGREDIENTS.put("glucose", "agent_charge");
        COMMON_INGREDIENTS.put("lactose", "agent_charge");
        COMMON_INGREDIENTS.put("polydextrose", "agent_charge");
        COMMON_INGREDIENTS.put("cellulose", "agent_charge");
        COMMON_INGREDIENTS.put("cellulose microcristalline", "agent_charge");
        COMMON_INGREDIENTS.put("inuline", "agent_charge_fibre");

        // Agents de texture (autres que charges)
        COMMON_INGREDIENTS.put("epaississant", "epaississant");
        COMMON_INGREDIENTS.put("épaississant", "epaississant");
        COMMON_INGREDIENTS.put("epaissaissant", "epaississant");
        COMMON_INGREDIENTS.put("épaissaissant", "epaississant");
        COMMON_INGREDIENTS.put("amidon", "agent_texture");
        COMMON_INGREDIENTS.put("amidon modifié", "agent_texture");
        COMMON_INGREDIENTS.put("fécule", "agent_texture");
        COMMON_INGREDIENTS.put("fecule", "agent_texture"); // variante
        COMMON_INGREDIENTS.put("gélatine", "gelifiant");
        COMMON_INGREDIENTS.put("gelatine", "gelifiant"); // variante
        COMMON_INGREDIENTS.put("agar-agar", "gelifiant_naturel");
        COMMON_INGREDIENTS.put("pectine", "gelifiant_naturel");
        COMMON_INGREDIENTS.put("alginate", "gelifiant");
        COMMON_INGREDIENTS.put("carboxyméthylcellulose", "epaississant");
        COMMON_INGREDIENTS.put("carboxymethylcellulose", "epaississant"); // variante

        // Acidifiants / Régulateurs d'acidité
        COMMON_INGREDIENTS.put("acidifiant", "acidifiant");
        COMMON_INGREDIENTS.put("acide lactique", "acidifiant");
        COMMON_INGREDIENTS.put("acide malique", "acidifiant");
        COMMON_INGREDIENTS.put("acide tartrique", "acidifiant");
        COMMON_INGREDIENTS.put("bicarbonate de sodium", "regulateur_acidite");
        COMMON_INGREDIENTS.put("citrate de sodium", "regulateur_acidite");

        // Anti-agglomérants
        COMMON_INGREDIENTS.put("antiagglomérant", "anti_agglomerant");
        COMMON_INGREDIENTS.put("antiagglomerant", "anti_agglomerant");
        COMMON_INGREDIENTS.put("anti_agglomérant", "anti_agglomerant");
        COMMON_INGREDIENTS.put("anti_agglomerant", "anti_agglomerant");
        COMMON_INGREDIENTS.put("dioxyde de silicium", "anti_agglomerant");
        COMMON_INGREDIENTS.put("silice", "anti_agglomerant");
        COMMON_INGREDIENTS.put("phosphate de calcium", "anti_agglomerant");
        COMMON_INGREDIENTS.put("carbonate de magnésium", "anti_agglomerant");

        // Agents levants
        COMMON_INGREDIENTS.put("agent_levant", "agent_levant");
        COMMON_INGREDIENTS.put("agent-levant", "agent_levant");
        COMMON_INGREDIENTS.put("levure chimique", "agent_levant");
        COMMON_INGREDIENTS.put("poudre à lever", "agent_levant");
        COMMON_INGREDIENTS.put("bicarbonate de soude", "agent_levant");
        COMMON_INGREDIENTS.put("pyrophosphate", "agent_levant");
    }
    
    @Override
    public String getName() {
        return "getIngredientRole";
    }

    @Override
    public int getArgLength() {
        return 2; // ingredientLabel, role
    }

    @Override
    public boolean bodyCall(Node[] args, int length, RuleContext context) {
        checkArgs(length, context);
        
        Node labelNode = getArg(0, args, context);
        
        if (labelNode.isLiteral()) {
            String label = labelNode.getLiteralLexicalForm().toLowerCase().trim();
            
            // Vérifier le cache
            if (cache.containsKey(label)) {
                // Créer une ressource URI au lieu d'un literal
                Node roleNode = NodeFactory.createURI(ncl + cache.get(label));
                return context.getEnv().bind(args[1], roleNode);
            }
            
            String role = identifyRole(label);
            
            if (role != null) {
                cache.put(label, role);
                // Créer une ressource URI au lieu d'un literal
                Node roleNode = NodeFactory.createURI(ncl + role);
                return context.getEnv().bind(args[1], roleNode);
            }
        }
        return false;
    }
    
    private String identifyRole(String label) {
        // 1. Vérifier si c'est un E-number
        String eNumber = extractENumber(label);
        if (eNumber != null && E_NUMBER_ROLES.containsKey(eNumber)) {
            return E_NUMBER_ROLES.get(eNumber);
        }
        
        // 2. Vérifier dans la base locale
        for (Map.Entry<String, String> entry : COMMON_INGREDIENTS.entrySet()) {
            if (label.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        
        // // 3. Appeler l'API Open Food Facts
        // try {
        //     String role = queryOFFIngredient(label);
        //     if (role != null) {
        //         return role;
        //     }
        // } catch (Exception e) {
        //     System.err.println("Erreur lors de la requête OFF pour: " + label + " - " + e.getMessage());
        // }
        
        // 4. Si rien trouvé, retourner null
        return null;
    }
    
    private String extractENumber(String label) {
        // Chercher un pattern E### dans le label
        if (label.matches(".*e\\s*\\d{3,4}.*")) {
            String[] parts = label.split("\\s+");
            for (String part : parts) {
                if (part.matches("e\\d{3,4}")) {
                    return part.toUpperCase().replaceAll("\\s", "");
                }
            }
        }
        return null;
    }
    
    private String queryOFFIngredient(String label) throws Exception {
        // Rechercher l'ingrédient dans la taxonomie OFF
        String encodedLabel = URLEncoder.encode(label, "UTF-8");
        String apiBaseUrl = NatclinnConf.OFF_API_BASE_URL; // Charger depuis la configuration
        String apiUrl = apiBaseUrl + "search_terms=" + 
                        encodedLabel + "&search_simple=1&action=process&json=1&tagtype_0=ingredients&tag_contains_0=contains";
        
        JSONObject response = callAPI(apiUrl);
        
        if (response != null && response.has("products")) {
            JSONArray products = response.getJSONArray("products");
            
            if (products.length() > 0) {
                // Analyser les ingrédients du premier produit trouvé
                JSONObject product = products.getJSONObject(0);
                
                if (product.has("ingredients")) {
                    JSONArray ingredients = product.getJSONArray("ingredients");
                    
                    for (int i = 0; i < ingredients.length(); i++) {
                        JSONObject ingredient = ingredients.getJSONObject(i);
                        
                        if (ingredient.has("text") && 
                            ingredient.getString("text").toLowerCase().contains(label)) {
                            
                            // Vérifier les propriétés de l'ingrédient
                            if (ingredient.has("vegan") && ingredient.getString("vegan").equals("no")) {
                                // Peut indiquer un additif d'origine animale
                            }
                            
                            // Analyser les propriétés additionnelles
                            String id = ingredient.optString("id", "");
                            
                            // Mapping basé sur les préfixes d'ID OFF
                            if (id.startsWith("en:e")) {
                                String eNum = id.substring(3).toUpperCase();
                                return E_NUMBER_ROLES.getOrDefault("E" + eNum, "additif");
                            }
                            
                            // Catégories spécifiques OFF
                            if (id.contains("preservative")) return "conservateur";
                            if (id.contains("flavour-enhancer")) return "exhausteur_gout";
                            if (id.contains("colour")) return "colorant";
                            if (id.contains("sweetener")) return "edulcorant";
                            if (id.contains("emulsifier")) return "emulsifiant";
                            if (id.contains("stabiliser") || id.contains("stabilizer")) return "stabilisant";
                            if (id.contains("thickener")) return "epaississant";
                            if (id.contains("antioxidant")) return "antioxydant";
                            if (id.contains("acidity-regulator")) return "regulateur_acidite";
                        }
                    }
                }
            }
        }
        
        return null;
    }
    
    private JSONObject callAPI(String apiUrl) throws Exception {
        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", NatclinnConf.OFF_API_USER_AGENT);
        conn.setConnectTimeout(Integer.parseInt(NatclinnConf.OFF_API_CONNECT_TIMEOUT));
        conn.setReadTimeout(Integer.parseInt(NatclinnConf.OFF_API_READ_TIMEOUT));
        
        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            return null;
        }
        
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
            response.append(line);
        }
        in.close();
        
        return new JSONObject(response.toString());
    }
}